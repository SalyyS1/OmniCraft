# OmniCraft — Operations guide

OmniCraft is an RPG/MMO crafting station for Paper. The plugin validates materials, money, limits, and hooks on the server before mutating inventory.

## Quick start

1. Pick the matching JAR: `legacy` for Paper 1.20–1.21.11 / Java 21; `26` for Paper 26.1+ / Java 25.
2. Copy the JAR to `plugins/` and start the server once.
3. Edit `plugins/OmniCraft/config.yml` and recipes in `category/<id>/`.
4. Run `/oc validate`, then `/oc reload`.

## Players

- `/oc` opens the main menu.
- Left click crafts the standard amount; right click crafts a batch; Shift crafts the largest amount permitted by inventory and recipe limits.
- Timed recipes revalidate materials and requirements when they finish.
- Favorites, search, and the craftable-only filter are available in category menus.

## Administrators

- `/oc browse`: create and edit recipes in the GUI.
- `/oc validate`: find invalid recipes before publishing them.
- `/oc debug recipe <id>`: inspect craftability without consuming materials.
- `/oc export` and `/oc import`: move a category through a zip file.

## Integrations

- **MMOItems 6.10.1-SNAPSHOT** and **MythicLib 1.7.1-SNAPSHOT** are soft dependencies: OmniCraft still starts without them, while recipes requiring MMOItems fail safely.
- Vault handles money, PlaceholderAPI provides conditions and speed modifiers, and AdvancedEnchantments supports KEEP/DESTROY/EXTRACT.
- AuraSkills is a soft dependency. AuraSkills placeholders through PlaceholderAPI can drive Fast Craft; recipes may also require a default AuraSkills skill level and award XP after a committed craft.

## Anti-dupe safety

- No inventory slot can be allocated to multiple ingredients.
- If output cannot be delivered, the inventory snapshot is restored and a safe refund is created.
- Daily/weekly quota is reserved, then correctly released on rollback.
- Countdown and crafting allow one active job per player.
- Failed Vault refunds are persisted for retry while the player is online; retries are idempotent.

## Example recipe

```yaml
# Enable AutoCraft only for intermediary recipes.
id: steel_sword
display:
  name: "#7cf5ffSteel Sword"
output:
  mode: VANILLA
  material: IRON_SWORD
  amount: 1
ingredients:
  iron:
    amount: 24
    item:
      mode: VANILLA
      material: IRON_INGOT
      amount: 1
requirements:
  level: 5
  money: 250.0
craft-time:
  enabled: true
  seconds: 5
  quantity-scaling: LINEAR
  minimum-seconds: 1
  maximum-seconds: 3600
auto-craft:
  enabled: false
  priority: 0
auraskills:
  # Default AuraSkills skill, for example FORGING. Omit this block when the recipe does not require AuraSkills.
  skill: FORGING
  minimum-level: 10
  experience: 12.5
```

AutoCraft is globally enabled by default, but can be disabled with `features.auto-craft: false`. Mark **only intermediary recipes** with `auto-craft.enabled: true`, then use the **AutoCraft** button on the recipe screen or `/oc autocraft <recipe> [amount]`. The system prefers materials already in inventory, chooses sources by `priority` then key, detects cycles and depth limits, and cancels the queue on logout or reload. Every node uses the same transaction, quota, Vault, and craft-time safeguards as a normal craft. `auto-craft.max-active-runs` and `max-target-crafts` keep server load within fixed limits.

AuraSkills is a soft dependency: without it, OmniCraft starts normally. Only recipes with an `auraskills` block require it, and XP is awarded only after the crafting transaction commits.

## Built-in RPG progression

Recipes can use `station.material` and `station.radius` to require the player to remain near a specific station block. This check runs again at commit, so a countdown or AutoCraft cannot consume materials after the player leaves the station. Invalid block materials appear in `/oc validate`.

`catalyst.item` is consumed by `catalyst.amount` per craft. Catalysts share the ingredient allocation transaction, so a single inventory stack cannot be used twice.

`outcome.critical` and `outcome.byproduct` are both opt-in. Critical adds `bonus-crafts` output when it succeeds; a byproduct creates extra output by chance. Results have audit seeds, chance is limited to 0–100, and capacity for the largest possible result is reserved before any item is consumed.

`outcome.quality` creates output with `omnicraft:quality` metadata and tier-specific lore. Quality rolls also have audit seeds and reserve output capacity before the transaction. Quality cannot be combined with AdvancedEnchantments `KEEP`, which preserves enchantment metadata from an ingredient under a different contract.

Timed crafting and AutoCraft are online-only: logout, reload, or recipe edits cancel a job before a new transaction starts. There is no offline crafting or offline claim. If the server restarts while a queue is waiting for its next node, the player can resume it online with `/oc autocraft-resume`; permission and recipe are checked again. Any node committing during a restart is isolated and is never replayed.
