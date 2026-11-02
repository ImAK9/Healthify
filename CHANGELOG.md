## Healthify 1.2.0

### New
- **Redesigned config screen**: settings are grouped into tabs, with a panel that explains whatever setting you hover, plus Undo and Reset buttons. Styled to match Healthify, on both NeoForge and Fabric
- **Stop Shrinking Beyond** option (General): bars keep the size they have at this distance, so far-away bars stay readable. Set to 0 (default) to have bars shrink with distance like before

### Changed
- Level, Dynamic Difficulty and L2 Hostility settings are now together under the **Mod Support** tab

## Healthify 1.1.0

### New
- **Fabric support** for 1.21.1, 26.1.2 and 26.3
- **L2 Hostility support** (NeoForge 1.21.1): health bars show each mob's level, plus its traits as icons above the name
  - Levels are coloured by how they compare to your own difficulty: green (easier), yellow (around your level), orange and red (harder)
  - The number on an icon shows the trait's rank
  - L2 Hostility's own floating text is hidden so nothing shows twice (can be turned off)
- **Mob blacklist**: stop bars and damage numbers on specific mobs (`minecraft:bat`), every mob from a mod (`modid:*`) or an entity tag (`#minecraft:raiders`)
- **Show on Hostile Mobs** option, next to the existing passive, neutral and player options
- **New settings screen** with every category in a sidebar

### Changed
- Config reorganised into clearer sections: General, Mobs, Bar, Name, Health Text, Level, Damage Numbers and Mod Support
- Settings for supported mods are now together under **Mod Support**
- Dynamic Difficulty levels can be turned off on their own, under Mod Support
- Removed the test functions (`/function healthify:...`) from release builds

### Fixed
- Mob names no longer get hidden behind water and other fluids on 26.1.2 and 26.3
- The damage trail now animates in the theme preview when opened from the main menu

## Healthify 1.0.1

### New
- **API for mod developers**: other mods can add a name, prefix, header or subtitle to health bars

### Fixed
- Works with any NeoForge 21.1.x version on 1.21.1, not just 21.1.265 and newer
- Text shadows on bold names

## Healthify 1.0.0

- First release for NeoForge 1.21.1, 26.1.2 and 26.3
- RPG-style pixel health bars with the mob's name and health, RPG shading and a draining damage trail
- Floating damage numbers for hits, big hits, kills and heals
- Six built-in themes plus custom themes from simple JSON files
- **Dynamic Difficulty support**: mob levels in the bar, in difficulty colours
- Works with Iris, Sodium and shader packs
