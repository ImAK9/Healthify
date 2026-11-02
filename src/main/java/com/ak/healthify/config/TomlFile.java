package com.ak.healthify.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TomlFile {
    private TomlFile() {}

    static String write(List<ConfigSpec.ConfigValue<?>> values) {
        StringBuilder out = new StringBuilder();
        List<String> section = null;
        for (ConfigSpec.ConfigValue<?> value : values) {
            if (!value.path().equals(section)) {
                section = value.path();
                if (out.length() > 0) out.append('\n');
                if (!section.isEmpty()) {
                    out.append('[').append(String.join(".", section)).append("]\n");
                }
            }
            String indent = section.isEmpty() ? "" : "\t";
            for (String line : value.comment()) out.append(indent).append('#').append(line).append('\n');
            for (String line : value.extraComment()) out.append(indent).append('#').append(line).append('\n');
            out.append(indent).append(value.key()).append(" = ").append(value.write()).append('\n');
        }
        return out.toString();
    }

    static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    static Map<String, Object> read(String text) {
        Map<String, Object> out = new LinkedHashMap<>();
        String prefix = "";
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[")) {
                int end = line.lastIndexOf(']');
                if (end < 0) continue;
                String name = line.substring(1, end).replace("[", "").replace("]", "").trim();
                prefix = name.isEmpty() ? "" : cleanKey(name) + ".";
                continue;
            }
            int equals = indexOutsideStrings(line, '=');
            if (equals < 0) continue;
            String key = cleanKey(line.substring(0, equals).trim());
            String rest = line.substring(equals + 1).trim();
            while (rest.startsWith("[") && depth(rest) > 0 && i + 1 < lines.length) {
                rest = rest + " " + stripComment(lines[++i]).trim();
            }
            try {
                Parser parser = new Parser(rest);
                Object value = parser.value();
                if (value != null) out.put(prefix + key, value);
            } catch (RuntimeException ignored) {
            }
        }
        return out;
    }

    private static String cleanKey(String key) {
        StringBuilder out = new StringBuilder();
        for (String part : key.split("\\.")) {
            String p = part.trim();
            if (p.length() >= 2 && (p.startsWith("\"") && p.endsWith("\"") || p.startsWith("'") && p.endsWith("'"))) {
                p = p.substring(1, p.length() - 1);
            }
            if (out.length() > 0) out.append('.');
            out.append(p);
        }
        return out.toString();
    }

    private static String stripComment(String line) {
        int hash = indexOutsideStrings(line, '#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    private static int indexOutsideStrings(String line, char target) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (quote == '"' && c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == target) {
                return i;
            }
        }
        return -1;
    }

    private static int depth(String text) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (quote == '"' && c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            }
        }
        return depth;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        Object value() {
            skipSpace();
            if (pos >= s.length()) return null;
            char c = s.charAt(pos);
            if (c == '"') return basicString();
            if (c == '\'') return literalString();
            if (c == '[') return list();
            String token = token();
            if (token.equals("true")) return Boolean.TRUE;
            if (token.equals("false")) return Boolean.FALSE;
            return number(token);
        }

        private List<Object> list() {
            pos++;
            List<Object> out = new ArrayList<>();
            while (true) {
                skipSpace();
                if (pos >= s.length()) return out;
                if (s.charAt(pos) == ']') {
                    pos++;
                    return out;
                }
                Object element = value();
                if (element != null) out.add(element);
                skipSpace();
                if (pos < s.length() && s.charAt(pos) == ',') pos++;
                else if (pos < s.length() && s.charAt(pos) != ']') pos++;
            }
        }

        private String basicString() {
            pos++;
            StringBuilder out = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return out.toString();
                if (c == '\\' && pos < s.length()) {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case 'n' -> out.append('\n');
                        case 't' -> out.append('\t');
                        case 'r' -> out.append('\r');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'u' -> {
                            if (pos + 4 <= s.length()) {
                                out.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                                pos += 4;
                            }
                        }
                        default -> out.append(e);
                    }
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private String literalString() {
            pos++;
            int end = s.indexOf('\'', pos);
            if (end < 0) end = s.length();
            String out = s.substring(pos, end);
            pos = Math.min(s.length(), end + 1);
            return out;
        }

        private String token() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ',' || c == ']' || Character.isWhitespace(c)) break;
                pos++;
            }
            return s.substring(start, pos);
        }

        private Object number(String token) {
            String t = token.replace("_", "");
            if (t.isEmpty()) return null;
            try {
                if (t.matches("[+-]?\\d+")) return Long.parseLong(t.startsWith("+") ? t.substring(1) : t);
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }
    }
}
