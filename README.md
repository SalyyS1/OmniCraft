# OmniCraft

OmniCraft is a Paper crafting station plugin for RPG/MMO servers. It gives players a clean category menu, shows exact material requirements, supports MMOItems and AdvancedEnchantments, and processes every craft with server-side anti-dupe checks.

## Server Use

Put the correct OmniCraft jar in `plugins/`, start the server once, then edit:

- `plugins/OmniCraft/config.yml`
- `plugins/OmniCraft/messages.yml`
- `plugins/OmniCraft/category/<category>/<recipe>.yml`

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/omnicraft`, `/oc`, `/ocraft`, `/craft` | `omnicraft.use` | Open main crafting menu |
| `/oc open <category>` | `omnicraft.category.<id>` or `omnicraft.open.<id>` | Open a category directly |
| `/oc settings` | `omnicraft.settings` | Open settings menu |
| `/oc browse` | `omnicraft.admin` | Browse admin recipe menu |
| `/oc search <category> <text>` | `omnicraft.use` | Search recipes by output, ingredient, MMOItems type or id |
| `/oc reload` | `omnicraft.reload` | Reload config, messages, recipes |
| `/oc debug recipe <id>` | `omnicraft.debug` | Dry-run a recipe check |
| `/oc validate` | `omnicraft.validate` | Validate loaded recipes |
| `/oc export <category>` | `omnicraft.admin` | Export a category zip |
| `/oc import <category> <file.zip>` | `omnicraft.admin` | Import a category zip from `exports/` |

## Main Features

- Category-based craft GUI.
- Direct category open commands.
- 5x5 ingredient grid.
- Requirement lines preserve original item name/lore and disable italic text.
- Material preview supports stack amounts above 64 inside GUI.
- Left/right/shift craft amounts.
- Optional title countdown before craft.
- MMOItems output and item matching.
- AdvancedEnchantments output enchant application.
- AdvancedEnchantments keep/destroy/extract behavior for consumed upgrade ingredients.
- Vault money requirement.
- PlaceholderAPI conditions.
- Daily and weekly craft limits.
- Rare craft broadcast.
- Craft history log.
- Config validation and category import/export.
- Admin settings, browse, recipe editor, cursor item serialization, and delete mode.
- Server-side anti-dupe transaction flow with rollback.

## Build Outputs

- `OmniCraft-Legacy.jar`: Paper `1.20` to `1.21.11`, Java 21 runtime.
- `OmniCraft-26.jar`: Paper `26.1.x`, Java 25 runtime, Paper API `26.1.2.build.+`.

## Recipe API Shape

```yaml
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
```

## Public Core Classes

- `CraftRecipe`
- `CraftItem`
- `CraftIngredient`
- `CraftCalculator`
- `CraftMatcher`
- `CraftLocks`

## Wiki

- Web wiki: `docs/wiki/index.html`
- English markdown: `docs/wiki/en/overview.md`
- Vietnamese markdown: `docs/wiki/vi/overview.md`
- Research notes: `docs/research.md`
