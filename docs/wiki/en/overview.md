# OmniCraft User Wiki

[Tiếng Việt](../vi/overview.md)

## What OmniCraft Is

OmniCraft is a crafting station plugin for Paper RPG servers. Players open a menu, choose a category, inspect the required materials, then craft with left click, right click, or shift click.

## Player Guide

Use one of these commands:

| Command | Use |
| --- | --- |
| `/oc` | Open main menu |
| `/ocraft` | Open main menu |
| `/craft` | Open main menu |
| `/oc open <category>` | Open one category directly |

Material items show the original name and lore. OmniCraft only adds one requirement line. Red means missing. Green means enough.

## Admin Commands

| Command | Use |
| --- | --- |
| `/oc settings` | Open settings GUI |
| `/oc browse` | Browse recipes |
| `/oc reload` | Reload files |
| `/oc validate` | Check recipe errors |
| `/oc debug recipe <id>` | Dry-run a recipe for your player |
| `/oc export <category>` | Export category recipes |
| `/oc import <category> <file.zip>` | Import category recipes |

## Permissions

| Permission | Use |
| --- | --- |
| `omnicraft.use` | Open menu |
| `omnicraft.category.<id>` | Access category |
| `omnicraft.open.<id>` | Direct open category |
| `omnicraft.admin` | Admin browser, import, export |
| `omnicraft.settings` | Settings menu |
| `omnicraft.reload` | Reload files |
| `omnicraft.debug` | Debug recipe checks |
| `omnicraft.validate` | Validate config |

## File Layout

```text
plugins/OmniCraft/
  config.yml
  messages.yml
  category/
    weapons/
      steel_sword.yml
  data/
    usage.yml
  logs/
    craft-history.log
  exports/
```

## Config Files

`config.yml` controls menus, click amounts, countdown, anti-dupe settings, AdvancedEnchantments behavior, history logs, editor behavior, and optional player features.

`messages.yml` controls every GUI line, title, warning, error, success message, and broadcast.

Each recipe file controls one output item.

## Recipe Example

```yaml
id: steel_blade
display:
  name: "#7cf5ffSteel Blade"
output:
  mode: MMOITEMS
  material: DIAMOND_SWORD
  amount: 1
  type: SWORD
  id: STEEL_BLADE
  enchantments:
    advanced:
      telepathy:
        level: 1
        success-rate: 100.0
        destroy-rate: 0.0
        tier: COMMON
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
  success-rate: 1.0
limits:
  daily: 5
  weekly: 20
options:
  enabled: true
  hidden: false
  rare-broadcast: true
  source-hints:
    essence: "Drops from dungeon bosses."
```

## AdvancedEnchantments

OmniCraft can apply custom enchants to crafted outputs through AdvancedEnchantments. Use `output.enchantments.advanced`. If AdvancedEnchantments is not installed, OmniCraft can either skip the custom enchants or disable those recipes, depending on `advanced-enchantments.missing-hook-disables-ae-recipes`.

## Anti-dupe Safety

OmniCraft does not trust client clicks. GUI clicks are cancelled, risky click types are ignored, inventory is scanned server-side, one player recipe transaction is locked, materials are removed before output is given, and rollback runs if output cannot be delivered.

## Troubleshooting

Run `/oc validate` after editing recipes. Run `/oc reload` after fixing files. Check `logs/craft-history.log` for craft results and rollback reasons.
