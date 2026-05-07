# OmniCraft Research Notes

## Local MMOItems API

Source reviewed:

- `D:\Project\.1_PROJECT_SL_PLUGINS\.20_RPG_PLUGINS\mmoitems-master\mmoitems-master`

Useful API points:

- `net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, id)` builds an MMOItems item.
- `MMOItems.getTypeName(ItemStack)` reads an MMOItems type from an item stack.
- `MMOItems.getID(ItemStack)` reads an MMOItems id from an item stack.
- MMOItems has its own crafting station package, but OmniCraft keeps recipes independent so server owners can use one YAML format and the plugin can start even when MMOItems is missing.

## Local MythicLib API

Source reviewed:

- `D:\Project\.1_PROJECT_SL_PLUGINS\.20_RPG_PLUGINS\mythiclib-master`

Useful API points:

- MythicLib contains crafting recipe, ingredient, UI filter, and event types.
- OmniCraft currently uses MythicLib only through MMOItems compile-time requirements and leaves direct MythicLib station integration as an adapter boundary.

## Similar Plugins

- CustomCrafting focuses on custom recipes with GUI management and NBT-heavy options.
- MMOItems crafting stations support delayed crafting queues and MMO item outputs.
- OmniCraft differs by prioritizing server-side transaction safety, ingredient risk warnings, MMOItems-style browsing, and one-file-per-output recipes.

## AdvancedEnchantments API

Sources reviewed:

- `https://ae.advancedplugins.net/for-developers/plugin-api`
- `https://www.spigotmc.org/resources/advancedenchantments-api.76819/`

Useful API points:

- The API class is `net.advancedplugins.ae.api.AEAPI`.
- `AEAPI.applyEnchant(enchantName, enchantLevel, itemStack)` returns a modified `ItemStack`.
- Events are exposed under `net.advancedplugins.ae.api` and `net.advancedplugins.ae.impl.effects.api`.
- OmniCraft uses reflection for AE calls so `AdvancedEnchantments` remains a real softdepend.

OmniCraft integration:

- `output.enchantments.advanced` applies AE enchants to crafted outputs.
- AE enchants on player items count as risk when selecting which base item to consume first.
- `extraction.enchant: EXTRACT` splits AE enchants from consumed base items into AE books through `/ae givebook`.
- `advanced-enchantments.missing-hook-disables-ae-recipes` can force AE recipes to fail when the hook is missing.

## Safety Decisions

- GUI preview items are display only.
- Ingredient removal uses server inventory snapshots.
- Player recipe transactions are locked.
- Risky client clicks are cancelled before any craft logic runs.
- Money is charged after material removal and refunded if output delivery fails.
- MMOItems and Vault are optional hooks; missing hooks do not crash plugin startup.
