# OmniCraft

OmniCraft is a Paper plugin for MMO-style crafting stations. It opens category-based crafting menus, validates every craft from the server inventory, and keeps recipe configuration in readable YAML files.

## Status

This repository contains the first implementation pass for OmniCraft v1. The stable target is Paper 1.20-1.21.11 through the legacy module. A separate 26.x module is kept so adapter differences can be isolated when that API line is finalized.

## Modules

| Module | Purpose |
| --- | --- |
| `omnicraft-core` | Recipe models, matching, craft calculation, lock helpers |
| `omnicraft-paper-legacy` | Paper plugin implementation for 1.20-1.21.11 |
| `omnicraft-paper-26` | Paper 26.x adapter build |

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/omnicraft`, `/oc`, `/ocraft`, `/craft` | `omnicraft.use` | Open the main crafting menu |
| `/oc open <category>` | `omnicraft.category.<id>` or `omnicraft.open.<id>` | Open a category directly |
| `/oc settings` | `omnicraft.settings` | Open the settings menu |
| `/oc browse` | `omnicraft.admin` | Open the admin recipe browser |
| `/oc reload` | `omnicraft.reload` | Reload config and recipes |
| `/oc debug recipe <id>` | `omnicraft.debug` | Dry-run a recipe check |
| `/oc validate` | `omnicraft.validate` | Validate loaded categories and recipes |
| `/oc export <category>` | `omnicraft.admin` | Export category recipes to a zip |
| `/oc import <category> <file.zip>` | `omnicraft.admin` | Import category recipes from `plugins/OmniCraft/exports` |

## Configuration

Default files:

- `config.yml`: menu layout, craft amount, cooldown, countdown, anti-dupe settings, extraction defaults.
- `messages.yml`: all player-facing text and GUI labels.
- `category/<category>/<recipe>.yml`: one recipe per file.

The message loader also accepts `messange.yml` as a fallback for typo compatibility.

## API Notes

Core classes are in `com.salyvn.omnicraft.core`:

- `CraftRecipe`
- `CraftItem`
- `CraftIngredient`
- `CraftCalculator`
- `CraftMatcher`
- `CraftLocks`

Paper code should call `CraftService.craft(player, recipe, clickMode)` instead of removing items directly. The service locks the player and recipe, scans server-side inventory, removes ingredients, gives outputs, and rolls back when output cannot be added.

## Build

```powershell
.\gradlew.bat clean build
```

Artifacts:

- `omnicraft-paper-legacy/build/libs/OmniCraft-legacy.jar`
- `omnicraft-paper-26/build/libs/OmniCraft-26.jar`

## Wiki

- Static bilingual page: `docs/wiki/index.html`
- Markdown wiki: `docs/wiki/en/overview.md` and `docs/wiki/vi/overview.md`
- Research notes: `docs/research.md`
