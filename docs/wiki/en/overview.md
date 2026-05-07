# OmniCraft Wiki

[Tiếng Việt](../vi/overview.md)

## Overview

OmniCraft is a Paper plugin for MMO-style crafting stations. It provides category menus, recipe previews, material requirements, permission and level checks, optional craft countdowns, and server-side craft transactions.

## Install

1. Build the project.
2. Use `OmniCraft-legacy.jar` for Paper 1.20-1.21.11.
3. Use `OmniCraft-26.jar` for the 26.x adapter line.
4. Start the server once.
5. Edit `config.yml`, `messages.yml`, and files under `category/`.

```powershell
.\gradlew.bat clean build
```

## Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/oc` | `omnicraft.use` | Open main menu |
| `/oc open <category>` | `omnicraft.category.<id>` or `omnicraft.open.<id>` | Open a category |
| `/oc settings` | `omnicraft.settings` | Open settings menu |
| `/oc browse` | `omnicraft.admin` | Browse recipes |
| `/oc validate` | `omnicraft.validate` | Validate loaded recipes |
| `/oc export <category>` | `omnicraft.admin` | Export category recipes |
| `/oc import <category> <file.zip>` | `omnicraft.admin` | Import category recipes |

## Recipe Example

```yaml
output:
  mode: MMOITEMS
  material: DIAMOND_SWORD
  amount: 1
  type: SWORD
  id: STEEL_BLADE
ingredients:
  base:
    amount: 1
    item:
      mode: VANILLA
      material: IRON_SWORD
  essence:
    amount: 100
    item:
      mode: VANILLA
      material: AMETHYST_SHARD
requirements:
  permission: omnicraft.recipe.steel_blade
  level: 25
  money: 10000
  conditions:
    - "%player_world% == world"
craft-time:
  enabled: true
  seconds: 5
extraction:
  enchant: EXTRACT
  gemstone: EXTRACT
  level: DESTROY
  success-rate: 0.75
```

## Anti-dupe Model

OmniCraft cancels GUI clicks, ignores cursor items, blocks risky click types, locks player recipe transactions, scans inventory server-side, and rolls back if output cannot be delivered.
