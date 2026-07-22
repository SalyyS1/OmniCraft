# OmniCraft

OmniCraft is a Paper crafting station plugin for RPG/MMO servers by SalyVn. It provides MMOItems-style recipe menus, server-side anti-dupe transactions, MMOItems support, AdvancedEnchantments extraction, Vault money checks, PlaceholderAPI conditions, admin recipe editing, and a bilingual wiki.

## Download

Built jars are committed in `dist/`.

| Jar | Server | Runtime |
| --- | --- | --- |
| `dist/OmniCraft-legacy.jar` | Paper 1.20 to 1.21.11 | Java 21 |
| `dist/OmniCraft-26.jar` | Paper 26.1.x | Java 25 |

## Install

1. Put the correct jar in your server `plugins/` folder.
2. Start the server once.
3. Edit `plugins/OmniCraft/config.yml`, `plugins/OmniCraft/messages.yml`, and recipe files under `plugins/OmniCraft/category/`.
4. Run `/oc reload` or restart.

Optional hooks: MMOItems, MythicLib, AdvancedEnchantments, Vault, PlaceholderAPI, AuraSkills, OmniGemStone, OmniEnchants, OmniTooltips, OmniMinMax, OmniDelta, OmniSet, OmniLore, OmniModifier, OmniPopupPickup, OmniTotalStats. AuraSkills is a soft-depend: recipes can require a default AuraSkills skill level and award XP after a committed craft; its PlaceholderAPI values can also drive Fast Craft.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/omnicraft`, `/oc`, `/ocraft`, `/craft` | `omnicraft.use` | Open main crafting menu |
| `/oc open <category>` | `omnicraft.category.<id>` or `omnicraft.open.<id>` | Open a category directly |
| `/oc browse` | `omnicraft.admin` | Browse, create, edit, and delete recipes |
| `/oc settings` | `omnicraft.settings` | Open global settings menu |
| `/oc search <category> <text>` | `omnicraft.use` | Search recipes |
| `/oc autocraft <category:recipe> [amount]` | `omnicraft.auto-craft` | Run an online-only recursive AutoCraft queue |
| `/oc autocraft-status` | `omnicraft.auto-craft` | Inspect the active queue |
| `/oc autocraft-resume` | `omnicraft.auto-craft` | Resume a queue safely paused before a restart |
| `/oc autocraft-cancel` | `omnicraft.auto-craft` | Cancel the active queue |
| `/oc reload` | `omnicraft.reload` | Reload plugin files |
| `/oc validate` | `omnicraft.validate` | Validate recipes and hooks |
| `/oc debug recipe <id>` | `omnicraft.debug` | Dry-run a recipe check |
| `/oc export <category>` | `omnicraft.admin` | Export category zip |
| `/oc import <category> <file.zip>` | `omnicraft.admin` | Import category zip |

## Main Features

- Category craft GUI with direct open commands.
- 4x4 ingredient grid and product-click crafting.
- Left click, right click, and shift click craft amounts.
- Original item name/lore preservation with non-italic display.
- Requirement lines with hex colors and stack previews above 64.
- Craftable-only filter, favorites, search, missing material summary, and source hints.
- Admin browse menu with continuous green create slots beside existing recipes.
- Editor with cursor item placement, Vanilla/MMOItems browser, ingredient amount controls, output replacement, enable toggle, craft-time toggle, and extraction mode toggle.
- Optional title countdown before final craft validation.
- Fast Craft duration policy with permission/PlaceholderAPI modifiers and clamped timing.
- Online-only AutoCraft: inventory-first recursive planning, deterministic source priority, cycle/depth caps, per-node revalidation, logout/reload cancellation, and one shared dispatcher. Queues paused before a restart can be resumed explicitly; an interrupted transaction is never replayed.
- MMOItems output and ingredient matching.
- AdvancedEnchantments output application and KEEP, DESTROY, EXTRACT handling for consumed upgrade items.
- Vault money requirements and PlaceholderAPI conditions.
- Daily and weekly craft limits.
- Rare craft broadcasts, craft history logs, category import/export, and config validation.
- Server-side transaction locks, click throttling, rollback, and GUI action hardening.

## Project Layout

| Path | Purpose |
| --- | --- |
| `omnicraft-core` | Shared recipe models, matching, locks, and calculators |
| `omnicraft-paper-legacy` | Paper 1.20 to 1.21.11 plugin module |
| `omnicraft-paper-26` | Paper 26.1.x plugin module |
| `docs` | GitHub Pages wiki |
| `dist` | Committed release jars |

## Public API Surface

- `CraftRecipe`
- `CraftItem`
- `CraftIngredient`
- `CraftRequirements`
- `CraftBehavior`
- `CraftTime`
- `ExtractionPolicy`
- `CraftCalculator`
- `CraftMatcher`
- `CraftLocks`

## Wiki

Open `docs/index.html` locally or use the GitHub Pages site once Pages is enabled for the repository.
