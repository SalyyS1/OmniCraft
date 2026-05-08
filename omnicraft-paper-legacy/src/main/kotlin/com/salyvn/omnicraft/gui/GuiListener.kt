package com.salyvn.omnicraft.gui

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.core.CraftClickMode
import com.salyvn.omnicraft.craft.CraftService
import com.salyvn.omnicraft.util.Text
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

class GuiListener(
    private val plugin: OmniCraftPlugin,
    private val menus: MenuService,
    private val craft: CraftService
) : Listener {
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? OmniHolder ?: return
        event.isCancelled = true
        if (event.clickedInventory != event.view.topInventory) return
        if (event.click in setOf(ClickType.NUMBER_KEY, ClickType.SWAP_OFFHAND, ClickType.DROP, ClickType.CONTROL_DROP, ClickType.DOUBLE_CLICK, ClickType.CREATIVE)) return
        val player = event.whoClicked as? Player ?: return

        when (holder.type) {
            GuiType.MAIN -> {
                val category = plugin.configService.categories.firstOrNull { it.slot == event.slot } ?: return
                menus.openCategory(player, category.id)
            }
            GuiType.CATEGORY -> {
                if (event.slot == 49) {
                    menus.openMain(player)
                    return
                }
                if (event.slot == 45) {
                    menus.toggleCategoryCraftable(player, holder.categoryId ?: return)
                    return
                }
                if (event.slot == 46) {
                    player.sendMessage(Text.c("#7cf5ffUse /oc search ${holder.categoryId ?: "<category>"} <text>."))
                    return
                }
                if (event.slot == 47) {
                    menus.toggleCategoryFavorites(player, holder.categoryId ?: return)
                    return
                }
                val category = plugin.configService.category(holder.categoryId ?: return) ?: return
                val index = categorySlotIndex(event.slot)
                val recipe = holder.searchQuery?.let { menus.searchRecipeAt(player, category.recipes, it, index) }
                    ?: menus.visibleRecipeAt(player, category.recipes, index)
                    ?: return
                if (event.click == ClickType.RIGHT) menus.toggleFavorite(player, recipe) else menus.openRecipe(player, recipe)
            }
            GuiType.RECIPE -> {
                val recipe = plugin.configService.recipe(holder.categoryId ?: return, holder.recipeId ?: return) ?: return
                when (event.slot) {
                    45 -> craft.craft(player, recipe, CraftClickMode.LEFT) { menus.openRecipe(player, recipe) }
                    46 -> craft.craft(player, recipe, CraftClickMode.RIGHT) { menus.openRecipe(player, recipe) }
                    47 -> craft.craft(player, recipe, CraftClickMode.SHIFT) { menus.openRecipe(player, recipe) }
                    49 -> menus.openCategory(player, recipe.categoryId)
                }
            }
            GuiType.SETTINGS -> {
                when (event.slot) {
                    10 -> toggleSetting("craft-time.enabled", player)
                    12 -> toggleSetting("anti-dupe.block-creative", player)
                    14 -> toggleSetting("advanced-enchantments.missing-hook-disables-ae-recipes", player)
                    16 -> menus.openBrowse(player)
                    22 -> menus.openMain(player)
                }
            }
            GuiType.BROWSE -> {
                if (event.slot == 49) {
                    menus.openMain(player)
                    return
                }
                if (event.slot == 53) {
                    menus.toggleDeleteMode(player, null)
                    return
                }
                val category = plugin.configService.categories.getOrNull(categorySlotIndex(event.slot)) ?: return
                menus.openAdminCategory(player, category.id)
            }
            GuiType.ADMIN_CATEGORY -> {
                if (event.slot == 49) {
                    menus.openBrowse(player)
                    return
                }
                if (event.slot == 53) {
                    menus.toggleDeleteMode(player, holder.categoryId)
                    return
                }
                val category = plugin.configService.category(holder.categoryId ?: return) ?: return
                val recipe = category.recipes.getOrNull(categorySlotIndex(event.slot)) ?: return
                if (menus.isDeleteMode(player)) menus.deleteRecipe(player, recipe) else menus.openEditor(player, recipe)
            }
            GuiType.EDITOR -> {
                val recipe = plugin.configService.recipe(holder.categoryId ?: return, holder.recipeId ?: return) ?: return
                val cursor = event.cursor
                if (cursor.type != Material.AIR) {
                    when (event.slot) {
                        MenuService.OUTPUT_SLOT -> menus.saveOutputFromCursor(player, recipe, cursor)
                        in MenuService.INGREDIENT_SLOTS -> menus.saveIngredientFromCursor(player, recipe, event.slot, cursor)
                    }
                    return
                }

                when (event.slot) {
                    in MenuService.INGREDIENT_SLOTS -> {
                        if (event.click == ClickType.RIGHT) menus.adjustIngredient(player, recipe, event.slot, -1)
                        else menus.adjustIngredient(player, recipe, event.slot, 1)
                    }
                    45 -> menus.toggleRecipeEnabled(player, recipe)
                    46 -> menus.cycleEnchantExtraction(player, recipe)
                    47 -> menus.toggleRecipeCraftTime(player, recipe)
                    49 -> menus.openAdminCategory(player, recipe.categoryId)
                    53 -> {
                        if (player.isSneaking) menus.deleteRecipe(player, recipe)
                        else player.sendMessage(Text.c("#ff6961Sneak click to delete this recipe."))
                    }
                }
            }
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is OmniHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (event.player.openInventory.topInventory.holder is OmniHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        craft.cancelPlayer(event.player.uniqueId)
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (event.from.blockX == event.to.blockX && event.from.blockY == event.to.blockY && event.from.blockZ == event.to.blockZ) return
        if (plugin.config.getBoolean("craft-time.cancel-on-move", false)) {
            craft.cancelPlayer(event.player.uniqueId)
        }
    }

    private fun categorySlotIndex(slot: Int): Int {
        val row = slot / 9
        val col = slot % 9
        if (row !in 1..4 || col !in 1..7) return -1
        return (row - 1) * 7 + (col - 1)
    }

    private fun toggleSetting(path: String, player: Player) {
        plugin.configService.setConfig(path, !plugin.config.getBoolean(path))
        menus.openSettings(player)
    }
}
