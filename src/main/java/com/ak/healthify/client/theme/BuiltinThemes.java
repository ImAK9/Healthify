package com.ak.healthify.client.theme;

import java.util.LinkedHashMap;
import java.util.Map;

final class BuiltinThemes {
    private BuiltinThemes() {}

    static final String DEFAULT_ID = "healthify";

    static final Map<String, String> FILES = new LinkedHashMap<>();

    static {
        FILES.put("healthify", """
                {
                  "name": "Healthify",
                  "author": "Healthify",
                  "bar": {
                    "height": 10,
                    "ends": "pointed",
                    "shading": true,
                    "fill": "#A3170B",
                    "empty": "#3A0B07",
                    "frame": "#7A7A7A",
                    "outline": "#1B1B1B",
                    "inner": "#2E2E2E",
                    "damageTrail": "#E8B23A"
                  },
                  "text": {
                    "name": "#FFFFFF",
                    "health": "#FFFFFF",
                    "shadow": true
                  },
                  "damageNumbers": {
                    "damage": "#FFE14D",
                    "bigHit": "#FF8A1F",
                    "kill": "#FF3B30",
                    "heal": "#5CFF5C"
                  }
                }
                """);
        FILES.put("classic", """
                {
                  "name": "Classic",
                  "author": "Healthify",
                  "bar": {
                    "ends": "pointed",
                    "shading": false,
                    "fill": "#A3170B",
                    "empty": "#3A0B07",
                    "frame": "#7A7A7A",
                    "outline": "#1B1B1B",
                    "inner": "#2E2E2E",
                    "damageTrail": "#E8B23A"
                  }
                }
                """);
        FILES.put("frost", """
                {
                  "name": "Frost",
                  "author": "Healthify",
                  "bar": {
                    "ends": "round",
                    "fill": "#3B8FD9",
                    "empty": "#0D2238",
                    "frame": "#A9C6DA",
                    "outline": "#0F1820",
                    "inner": "#26384A",
                    "damageTrail": "#EAF8FF"
                  },
                  "text": {
                    "name": "#D8F1FF"
                  }
                }
                """);
        FILES.put("venom", """
                {
                  "name": "Venom",
                  "author": "Healthify",
                  "bar": {
                    "ends": "pointed",
                    "fill": "#4CAF2A",
                    "empty": "#13300B",
                    "frame": "#6E7D5A",
                    "outline": "#141A0F",
                    "inner": "#26301C",
                    "damageTrail": "#D7F25A",
                    "lowHealthFill": "#C9B21E",
                    "lowHealth": 0.3
                  },
                  "text": {
                    "name": "#C8F5A8"
                  }
                }
                """);
        FILES.put("royal", """
                {
                  "name": "Royal",
                  "author": "Healthify",
                  "bar": {
                    "ends": "square",
                    "fill": "#7B3FC4",
                    "empty": "#241038",
                    "frame": "#D2A43A",
                    "outline": "#2A1A06",
                    "inner": "#4A3410",
                    "damageTrail": "#F2D27A"
                  },
                  "text": {
                    "name": "#F4D98B"
                  }
                }
                """);
        FILES.put("ember", """
                {
                  "name": "Ember",
                  "author": "Healthify",
                  "bar": {
                    "ends": "round",
                    "fill": "#E8691E",
                    "empty": "#3A1406",
                    "frame": "#8C5A3C",
                    "outline": "#1E0E06",
                    "inner": "#3A2214",
                    "damageTrail": "#FFE08A",
                    "lowHealthFill": "#C42A12",
                    "lowHealth": 0.25
                  },
                  "text": {
                    "name": "#FFD9B0"
                  }
                }
                """);
    }

    static final String README = """
            HEALTHIFY THEMES
            ================

            Every .json file in this folder is a health bar theme. Pick one in game:
            Mods > Healthify > Config, then < Previous / Next >.

            To make your own: copy one of the files here, rename it (lowercase, no spaces,
            e.g. my_theme.json) and change the colours. Healthify notices changes on its own,
            so you can edit a theme while the game is running and watch the bar update.
            To share a theme, just send someone the .json file.

            Everything is optional. Anything you leave out uses the default Healthify theme.

            {
              "name": "My Theme",              Shown in the theme picker
              "author": "Your Name",           Shown in the theme picker
              "bar": {
                "height": 10,                  Bar height in pixels, 7 to 20
                "ends": "pointed",             End caps: "pointed", "round" or "square"
                "endInsets": [4,3,2,1,0,0,1,2,3,4],
                                               Or draw your own end caps: one number per pixel row
                                               (as many numbers as "height"), how far that row
                                               is pulled in at both ends. Overrides "ends".
                "shading": true,               RPG shading (glossy fill, bevelled frame)
                "fill": "#A3170B",             Remaining health
                "empty": "#3A0B07",            Missing health
                "frame": "#7A7A7A",            The frame around the bar
                "outline": "#1B1B1B",          Thin line around the outside
                "inner": "#2E2E2E",            Thin line between the frame and the health
                "damageTrail": "#E8B23A",      Health just lost, before it drains away
                "lowHealthFill": "#C42A12",    Fill colour when health is low (leave out to never change)
                "lowHealth": 0.25              "Low" means this share of max health or less (0.25 = 25%)
              },
              "text": {
                "name": "#FFFFFF",             Mob name (mobs with coloured name tags keep their colour)
                "health": "#FFFFFF",           Health numbers in the bar
                "shadow": true                 Drop shadow behind the text
              },
              "damageNumbers": {
                "damage": "#FFE14D",           Normal hits
                "bigHit": "#FF8A1F",           Big hits
                "kill": "#FF3B30",             Killing blows
                "heal": "#5CFF5C"              Healing
              }
            }

            Colours are "#RRGGBB", or "#AARRGGBB" to make them see-through (AA: 00 = invisible, FF = solid).

            If a theme file has a mistake, it won't show up in the picker. The game log
            (logs/latest.log) says which file and what's wrong; search it for "Healthify".

            Deleted one of the built-in themes by accident? Delete this whole folder and
            restart the game: Healthify recreates it with all built-in themes.
            """;
}
