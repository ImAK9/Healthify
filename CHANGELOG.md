## 1.5.0

- feat: **Minecraft 26.2 support** on NeoForge and Fabric
- perf: Bars are drawn together in batches on 1.21.1 instead of one at a time, a big speedup with many mobs around (especially with Sodium or Iris)
- perf: Fewer GPU uploads on 1.21.1 when switching between textures
- perf: Bars are only rebuilt when something on them changes, and mod support data is read once per tick instead of every frame

## 1.4.0

- feat: **Dungeon Difficulty support** (with Dynamic Difficulty installed): mobs scaled by Dungeon Difficulty show their area and level under the bar, like "Dungeon 5", with Dungeon Difficulty's own icons and a colour from green to crimson as it gets harder (can be turned off under Mod Support)
- change: Bars now show for mobs up to 16 blocks away by default, up from 8 (existing configs keep their own setting)
- change: Champions: names stay white by default and only the stars use the rank colour, so a yellow name no longer sits under yellow stars (turn Rank Coloured Name back on under Mod Support if you liked it)
- change: The small line above the name (like Champions affixes) now has a thin dark outline, so it stays readable against bright skies
- change: Champions affixes are now light grey, so they stand apart from the white name
- change: Bars move up a little when a mob has a line under its bar (like the Dungeon Difficulty level), so the line no longer covers the mob's head
- fix: Husk bars no longer sit low on their heads, because husks are drawn bigger than their hitbox

## 1.3.0

- feat: **Champions support** (NeoForge): champions show their rank as stars above the name (one per tier, in the rank's colour), their name in the rank's colour and their affixes listed above it. Rank Style can also show the rank name (like "Elite Zombie"), both or neither, and each part can be turned off under Mod Support.
- feat: **Punchier damage numbers**: big hits and killing blows get an exclamation mark and a small word above them, like "Crit!" or "Slain!" (both can be turned off, and themes can set their own words)
- feat: **Outline Thickness** (1 or 2 pixels) and **Decimals** (0, 1 or 2) options for damage numbers
- feat: Themes can set the damage number outline colour
- change: Softer damage number colours in the default theme: cream hits, amber big hits, deep red kills and soft green heals, with a dark brown outline
- change: Unedited copies of the built-in themes in your themes folder now update automatically when Healthify updates them
- fix: Names from mods that colour them with old-style colour codes (like some boss names) no longer turn into a blurry blob, because their shadow now stays dark

## 1.2.0

- feat: **Redesigned config screen**: settings are grouped into tabs, with a panel that explains whatever setting you hover, plus Undo and Reset buttons. Styled to match Healthify, on both NeoForge and Fabric
- feat: **Stop Shrinking Beyond** option (General): bars keep the size they have at this distance, so far-away bars stay readable. Set to 0 (default) to have bars shrink with distance like before
- change: Level, Dynamic Difficulty and L2 Hostility settings are now together under the **Mod Support** tab

## 1.1.0

- feat: **Fabric support** for 1.21.1, 26.1.2 and 26.3
- feat: **L2 Hostility support** (NeoForge 1.21.1): health bars show each mob's level, plus its traits as icons above the name
  - Levels are coloured by how they compare to your own difficulty: green (easier), yellow (around your level), orange and red (harder)
  - The number on an icon shows the trait's rank
  - L2 Hostility's own floating text is hidden so nothing shows twice (can be turned off)
- feat: **Mob blacklist**: stop bars and damage numbers on specific mobs (`minecraft:bat`), every mob from a mod (`modid:*`) or an entity tag (`#minecraft:raiders`)
- feat: **Show on Hostile Mobs** option, next to the existing passive, neutral and player options
- feat: **New settings screen** with every category in a sidebar
- change: Config reorganised into clearer sections: General, Mobs, Bar, Name, Health Text, Level, Damage Numbers and Mod Support
- change: Settings for supported mods are now together under **Mod Support**
- change: Dynamic Difficulty levels can be turned off on their own, under Mod Support
- change: Removed the test functions (`/function healthify:...`) from release builds
- fix: Mob names no longer get hidden behind water and other fluids on 26.1.2 and 26.3
- fix: The damage trail now animates in the theme preview when opened from the main menu

## 1.0.1

- feat: **API for mod developers**: other mods can add a name, prefix, header or subtitle to health bars
- fix: Works with any NeoForge 21.1.x version on 1.21.1, not just 21.1.265 and newer
- fix: Text shadows on bold names

## 1.0.0

- First release for NeoForge 1.21.1, 26.1.2 and 26.3
- RPG-style pixel health bars with the mob's name and health, RPG shading and a draining damage trail
- Floating damage numbers for hits, big hits, kills and heals
- Six built-in themes plus custom themes from simple JSON files
- **Dynamic Difficulty support**: mob levels in the bar, in difficulty colours
- Works with Iris, Sodium and shader packs
