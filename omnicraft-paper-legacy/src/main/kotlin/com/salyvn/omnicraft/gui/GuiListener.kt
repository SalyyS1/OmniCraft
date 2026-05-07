package com.salyvn.omnicraft.gui

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.core.CraftClickMode
import com.salyvn.omnicraft.craft.CraftService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.entity.Player

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
                val category = plugin.configService.category(holder.categoryId ?: return) ?: return
                val recipe = category.recipes.firstOrNull { recipe ->
                    event.currentItem?.itemMeta?.displayName() == event.currentItem?.itemMeta?.displayName() &&
                        recipe.displayName.isNotBlank()
                } ?: category.recipes.getOrNull(categorySlotIndex(event.slot)) ?: return
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
            GuiType.SETTINGS, GuiType.BROWSE -> {
                if (event.slot == 49) menus.openMain(player)
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

    private fun categorySlotIndex(slot: Int): Int {
        val row = slot / 9
        val col = slot % 9
        if (row !in 1..4 || col !in 1..7) return -1
        return (row - 1) * 7 + (col - 1)
    }
}
