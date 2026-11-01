![Healthify](https://i.imgur.com/zq0TMG1.png)

**Healthify** adds clean, RPG-style health bars and floating damage numbers to Minecraft. Bars show when mobs are near, damaged or looked at, hide behind walls, and everything is configurable in game.

## Bar styles

Pixel-art bars with the mob's name and exact health. Choose a classic flat look or **RPG shading**, and watch the **damage trail** drain away when a mob gets hit.

![Bar styles](https://i.imgur.com/wufhmrm.png)

## Damage numbers

Floating numbers pop out of mobs when they take damage: gold for hits, orange for big hits, red for killing blows and green for heals.

![Damage numbers](https://i.imgur.com/d2O01D2.png)

## Themes

Six themes are included. Pick one in game under **Mods → Healthify → Config**.

![Themes](https://i.imgur.com/O4wH1Af.png)

### Custom themes

Make your own look in a few minutes:

1.  In the theme picker, click **Open Themes Folder** (`config/healthify/themes`).
2.  Copy one of the theme files and give it a new name, like `my_theme.json`.
3.  Change the colours. The bar updates in game as you save, no restart needed.

```
{
  "name": "My Theme",
  "author": "You",
  "bar": {
    "ends": "round",
    "fill": "#3B8FD9",
    "empty": "#0D2238",
    "frame": "#A9C6DA",
    "damageTrail": "#EAF8FF",
    "lowHealthFill": "#D83A2E"
  },
  "text": { "name": "#D8F1FF" },
  "damageNumbers": { "damage": "#FFE14D" }
}
```

Anything you leave out uses the default theme. Themes can also set the bar's height, its end caps (pointed, round, square or your own pixel shape) and shading; every option is explained in the `README.txt` in the themes folder. To share a theme, just send the `.json` file.

## Dynamic Difficulty

With **[Dynamic Difficulty](https://www.curseforge.com/minecraft/mc-mods/dynamic-difficulty-mod)** installed, each mob's level shows right in its health bar, in difficulty colours.

![Dynamic Difficulty levels](https://i.imgur.com/2CwH6Hb.png)

## For mod developers

Other mods can add to a mob's health bar: a symbol before its name and a small line under the bar. The API only uses
Java and Minecraft types, so you can call it by reflection without depending on Healthify:

```java
Class.forName("com.ak.healthify.api.HealthifyApi")
        .getMethod("addDecorator", java.util.function.Function.class)
        .invoke(null, (java.util.function.Function<LivingEntity, Map<String, Object>>) entity ->
                isElite(entity) ? Map.of("prefix", Component.literal("★ ").withStyle(ChatFormatting.GOLD),
                                         "subtitle", Component.literal("Vampiric · Swift").withStyle(ChatFormatting.GOLD))
                                : null);
```

Return `null` for nothing, or a map with any of `"name"` (shown instead of the mob's name), `"prefix"` (starts the
name line, before the level when it's shown first), `"header"` (a small line above the name) and `"subtitle"` (a small line under the bar), all `Component`s. It's called every frame
on the client for each mob with a bar, so keep it cheap.

## Compatibility

|            |                                   |
| ---------- |---------------------------------- |
| <strong>Minecraft</strong> |1.21.1, 26.1.2, 26.3               |
| <strong>Mod loader</strong> |NeoForge                           |
| <strong>Side</strong> |Client only: servers don't need it |
| <strong>Works with</strong> |Iris, Sodium and shader packs      |
| <strong>Optional</strong> |Dynamic Difficulty, for mob levels |
