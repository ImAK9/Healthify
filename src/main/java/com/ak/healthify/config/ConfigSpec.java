package com.ak.healthify.config;

import com.ak.healthify.Healthify;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ConfigSpec {
    private static final long CHECK_EVERY_MS = 1000;

    private final String fileName;
    private final List<ConfigValue<?>> values;
    private Path file;
    private long lastWritten = Long.MIN_VALUE;
    private static int revision;
    private long lastCheck;

    private ConfigSpec(String fileName, List<ConfigValue<?>> values) {
        this.fileName = fileName;
        this.values = List.copyOf(values);
        for (ConfigValue<?> value : this.values) value.spec = this;
    }

    public static int revision() {
        return revision;
    }

    public List<ConfigValue<?>> values() {
        return values;
    }

    public void load(Path folder) {
        file = folder.resolve(fileName);
        reload();
    }

    public void tick() {
        if (file == null) return;
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_EVERY_MS) return;
        lastCheck = now;
        if (modifiedTime() != lastWritten) reload();
    }

    public void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(fileName + ".tmp");
            Files.writeString(temp, TomlFile.write(values), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            lastWritten = modifiedTime();
        } catch (IOException e) {
            Healthify.LOGGER.warn("Healthify: couldn't save {}", file, e);
        }
    }

    private void reload() {
        revision++;
        boolean needsSave = false;
        Map<String, Object> data = Map.of();
        if (Files.exists(file)) {
            try {
                data = TomlFile.read(Files.readString(file, StandardCharsets.UTF_8));
            } catch (Exception e) {
                Healthify.LOGGER.warn("Healthify: couldn't read {}, using the default settings", file, e);
                needsSave = true;
            }
        } else {
            needsSave = true;
        }
        Set<String> known = new HashSet<>();
        for (ConfigValue<?> value : values) {
            String key = value.fullKey();
            known.add(key);
            Object raw = data.get(key);
            if (raw == null) {
                value.reset();
                needsSave = true;
            } else if (!value.load(raw)) {
                needsSave = true;
            }
        }
        for (String key : data.keySet()) {
            if (!known.contains(key)) needsSave = true;
        }
        if (needsSave) {
            save();
        } else {
            lastWritten = modifiedTime();
        }
    }

    private long modifiedTime() {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }

    public abstract static class ConfigValue<T> {
        private final List<String> path;
        private final String key;
        private final List<String> comment;
        private final String translationKey;
        private final T defaultValue;
        private ConfigSpec spec;
        protected T value;

        protected ConfigValue(List<String> path, String key, List<String> comment, String translationKey, T defaultValue) {
            this.path = List.copyOf(path);
            this.key = key;
            this.comment = List.copyOf(comment);
            this.translationKey = translationKey;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public void set(T newValue) {
            T checked = newValue == null ? null : read(toRaw(newValue));
            if (checked != null) {
                value = checked;
                revision++;
            }
        }

        public void save() {
            if (spec != null) spec.save();
        }

        public T getDefault() {
            return defaultValue;
        }

        public String key() {
            return key;
        }

        public List<String> path() {
            return path;
        }

        public List<String> comment() {
            return comment;
        }

        public String translationKey() {
            return translationKey;
        }

        public List<String> extraComment() {
            return List.of();
        }

        String fullKey() {
            return path.isEmpty() ? key : String.join(".", path) + "." + key;
        }

        void reset() {
            value = defaultValue;
        }

        boolean load(Object raw) {
            T parsed = read(raw);
            if (parsed == null) {
                value = defaultValue;
                return false;
            }
            value = parsed;
            return matches(raw, parsed);
        }

        protected boolean matches(Object raw, T parsed) {
            return true;
        }

        protected Object toRaw(T value) {
            return value;
        }

        protected abstract T read(Object raw);

        protected abstract String write();
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(List<String> path, String key, List<String> comment, String translationKey, boolean defaultValue) {
            super(path, key, comment, translationKey, defaultValue);
        }

        @Override
        protected Boolean read(Object raw) {
            return raw instanceof Boolean b ? b : null;
        }

        @Override
        protected String write() {
            return value.toString();
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        IntValue(List<String> path, String key, List<String> comment, String translationKey, int defaultValue, int min, int max) {
            super(path, key, comment, translationKey, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }

        @Override
        protected Integer read(Object raw) {
            if (!(raw instanceof Number n)) return null;
            double d = n.doubleValue();
            if (Double.isNaN(d) || d != Math.rint(d)) return null;
            return (int) Math.max(min, Math.min(max, Math.round(d)));
        }

        @Override
        protected boolean matches(Object raw, Integer parsed) {
            return raw instanceof Long l && l == parsed.longValue();
        }

        @Override
        protected String write() {
            return value.toString();
        }

        @Override
        public List<String> extraComment() {
            return List.of(" Default: " + getDefault(), " Range: " + min + " ~ " + max);
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(List<String> path, String key, List<String> comment, String translationKey, double defaultValue, double min, double max) {
            super(path, key, comment, translationKey, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        @Override
        protected Double read(Object raw) {
            if (!(raw instanceof Number n)) return null;
            double d = n.doubleValue();
            if (Double.isNaN(d)) return null;
            return Math.max(min, Math.min(max, d));
        }

        @Override
        protected boolean matches(Object raw, Double parsed) {
            return raw instanceof Double d && d.doubleValue() == parsed;
        }

        @Override
        protected String write() {
            return Double.toString(value);
        }

        @Override
        public List<String> extraComment() {
            return List.of(" Default: " + getDefault(), " Range: " + min + " ~ " + max);
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private final Class<E> type;

        EnumValue(List<String> path, String key, List<String> comment, String translationKey, E defaultValue) {
            super(path, key, comment, translationKey, defaultValue);
            this.type = defaultValue.getDeclaringClass();
        }

        public E[] options() {
            return type.getEnumConstants();
        }

        @Override
        protected E read(Object raw) {
            if (raw instanceof Enum<?> e && type.isInstance(e)) return type.cast(e);
            if (!(raw instanceof String s)) return null;
            for (E option : type.getEnumConstants()) {
                if (option.name().equalsIgnoreCase(s.trim())) return option;
            }
            return null;
        }

        @Override
        protected boolean matches(Object raw, E parsed) {
            return parsed.name().equals(raw);
        }

        @Override
        protected String write() {
            return TomlFile.quote(value.name());
        }

        @Override
        public List<String> extraComment() {
            return List.of("Allowed Values: " + String.join(", ", Arrays.stream(options()).map(Enum::name).toList()));
        }
    }

    public static final class StringValue extends ConfigValue<String> {
        private final Predicate<Object> validator;

        StringValue(List<String> path, String key, List<String> comment, String translationKey, String defaultValue, Predicate<Object> validator) {
            super(path, key, comment, translationKey, defaultValue);
            this.validator = validator;
        }

        public boolean isValid(String candidate) {
            return candidate != null && validator.test(candidate);
        }

        @Override
        protected String read(Object raw) {
            return raw instanceof String s && validator.test(s) ? s : null;
        }

        @Override
        protected String write() {
            return TomlFile.quote(value);
        }
    }

    public static final class ListValue extends ConfigValue<List<? extends String>> {
        private final Predicate<Object> elementValidator;

        ListValue(List<String> path, String key, List<String> comment, String translationKey, List<? extends String> defaultValue, Predicate<Object> elementValidator) {
            super(path, key, comment, translationKey, List.copyOf(defaultValue));
            this.elementValidator = elementValidator;
        }

        public boolean isValidElement(String candidate) {
            return candidate != null && elementValidator.test(candidate);
        }

        @Override
        protected List<? extends String> read(Object raw) {
            if (!(raw instanceof List<?> list)) return null;
            List<String> out = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof String s && elementValidator.test(s)) out.add(s);
            }
            return List.copyOf(out);
        }

        @Override
        protected boolean matches(Object raw, List<? extends String> parsed) {
            return raw instanceof List<?> list && list.size() == parsed.size();
        }

        @Override
        protected String write() {
            List<String> parts = new ArrayList<>();
            for (String element : value) parts.add(TomlFile.quote(element));
            return "[" + String.join(", ", parts) + "]";
        }
    }

    public static final class Builder {
        private final List<String> path = new ArrayList<>();
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private List<String> comment = List.of();
        private String translation;

        public Builder push(String name) {
            path.add(name);
            return this;
        }

        public Builder pop() {
            if (!path.isEmpty()) path.remove(path.size() - 1);
            return this;
        }

        public Builder comment(String... lines) {
            comment = List.of(lines);
            return this;
        }

        public Builder translation(String key) {
            translation = key;
            return this;
        }

        public BooleanValue define(String key, boolean defaultValue) {
            return add(new BooleanValue(path, key, takeComment(), takeTranslation(key), defaultValue));
        }

        public StringValue define(String key, String defaultValue) {
            return define(key, defaultValue, o -> o instanceof String);
        }

        public StringValue define(String key, String defaultValue, Predicate<Object> validator) {
            return add(new StringValue(path, key, takeComment(), takeTranslation(key), defaultValue, validator));
        }

        public IntValue defineInRange(String key, int defaultValue, int min, int max) {
            return add(new IntValue(path, key, takeComment(), takeTranslation(key), defaultValue, min, max));
        }

        public DoubleValue defineInRange(String key, double defaultValue, double min, double max) {
            return add(new DoubleValue(path, key, takeComment(), takeTranslation(key), defaultValue, min, max));
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(String key, E defaultValue) {
            return add(new EnumValue<>(path, key, takeComment(), takeTranslation(key), defaultValue));
        }

        public ListValue defineListAllowEmpty(String key, List<? extends String> defaultValue, Supplier<String> newElement, Predicate<Object> elementValidator) {
            return add(new ListValue(path, key, takeComment(), takeTranslation(key), defaultValue, elementValidator));
        }

        public ConfigSpec build(String fileName) {
            return new ConfigSpec(fileName, values);
        }

        private <V extends ConfigValue<?>> V add(V value) {
            values.add(value);
            return value;
        }

        private List<String> takeComment() {
            List<String> out = comment;
            comment = List.of();
            return out;
        }

        private String takeTranslation(String key) {
            String out = translation != null ? translation : "healthify.configuration." + key;
            translation = null;
            return out;
        }
    }
}
