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
        if (event.click in setOf(ClickType.NUMBER_KEY, ClickType.SWAP_OFFHAND, ClickType.DROP, ClickType.CONTROL_DROP, ClickType.DOUBLE_CLICK, ClickType.CREATIVE)) return
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory != event.view.topInventory) {
            if (holder.type in setOf(GuiType.EDITOR, GuiType.ADMIN_CATEGORY) && player.hasPermission("omnicraft.admin") && event.click in setOf(ClickType.LEFT, ClickType.RIGHT)) {
                event.isCancelled = false
            }
            return
        }

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
                    MenuService.OUTPUT_SLOT -> craft.craft(player, recipe, craftMode(event.click)) { menus.openRecipe(player, recipe) }
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
                val createSlot = MenuService.ADMIN_RECIPE_SLOTS.getOrNull(category.recipes.size)
                if (event.slot == createSlot) {
                    val cursor = event.cursor
                    if (cursor.type != Material.AIR) menus.createRecipeFromCursor(player, category.id, cursor)
                    else menus.openItemModeBrowser(player, category.id, null, MenuService.ACTION_NEW_RECIPE)
                    return
                }
                val recipeIndex = MenuService.ADMIN_RECIPE_SLOTS.indexOf(event.slot)
                val recipe = category.recipes.getOrNull(recipeIndex) ?: return
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
                        val ingredientIndex = MenuService.INGREDIENT_SLOTS.indexOf(event.slot)
                        if (ingredientIndex >= recipe.ingredients.size) {
                            menus.openItemModeBrowser(player, recipe.categoryId, recipe.id, MenuService.ACTION_SET_INGREDIENT, event.slot)
                        } else if (event.click == ClickType.SHIFT_RIGHT) {
                            menus.removeIngredient(player, recipe, event.slot)
                        } else if (event.click == ClickType.SHIFT_LEFT) {
                            menus.adjustIngredient(player, recipe, event.slot, 16)
                        } else if (event.click == ClickType.RIGHT) {
                            menus.adjustIngredient(player, recipe, event.slot, -1)
                        } else {
                            menus.adjustIngredient(player, recipe, event.slot, 1)
                        }
                    }
                    MenuService.OUTPUT_SLOT -> menus.openItemModeBrowser(player, recipe.categoryId, recipe.id, MenuService.ACTION_SET_OUTPUT)
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
            GuiType.ITEM_MODE -> {
                when (event.slot) {
                    11 -> menus.openVanillaBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, 0)
                    15 -> menus.openMmoTypeBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, 0)
                    22 -> backFromBrowser(player, holder)
                }
            }
            GuiType.VANILLA_BROWSER -> {
                when (event.slot) {
                    45 -> menus.openVanillaBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, (holder.page - 1).coerceAtLeast(0))
                    49 -> menus.openItemModeBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot)
                    53 -> menus.openVanillaBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, holder.page + 1)
                    else -> menus.selectVanilla(player, holder, event.slot)
                }
            }
            GuiType.MMO_TYPE_BROWSER -> {
                when (event.slot) {
                    45 -> menus.openMmoTypeBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, (holder.page - 1).coerceAtLeast(0))
                    49 -> menus.openItemModeBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot)
                    53 -> menus.openMmoTypeBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, holder.page + 1)
                    else -> menus.selectMmoType(player, holder, event.slot)
                }
            }
            GuiType.MMO_ITEM_BROWSER -> {
                when (event.slot) {
                    45 -> menus.openMmoItemBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, holder.itemType ?: return, (holder.page - 1).coerceAtLeast(0))
                    49 -> menus.openMmoTypeBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, 0)
                    53 -> menus.openMmoItemBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, holder.itemType ?: return, holder.page + 1)
                    else -> menus.selectMmoItem(player, holder, event.slot)
                }
            }
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder as? OmniHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        val topSlots = event.rawSlots.filter { it < event.view.topInventory.size }
        val slot = topSlots.firstOrNull() ?: return
        val stack = event.oldCursor
        if (stack.type == Material.AIR) return
        when (holder.type) {
            GuiType.ADMIN_CATEGORY -> {
                val category = plugin.configService.category(holder.categoryId ?: return) ?: return
                if (slot == MenuService.ADMIN_RECIPE_SLOTS.getOrNull(category.recipes.size)) menus.createRecipeFromCursor(player, category.id, stack)
            }
            GuiType.EDITOR -> {
                val recipe = plugin.configService.recipe(holder.categoryId ?: return, holder.recipeId ?: return) ?: return
                when (slot) {
                    MenuService.OUTPUT_SLOT -> menus.saveOutputFromCursor(player, recipe, stack)
                    in MenuService.INGREDIENT_SLOTS -> menus.saveIngredientFromCursor(player, recipe, slot, stack)
                }
            }
            else -> {}
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

    private fun craftMode(click: ClickType): CraftClickMode {
        return when {
            click.isShiftClick -> CraftClickMode.SHIFT
            click == ClickType.RIGHT -> CraftClickMode.RIGHT
            else -> CraftClickMode.LEFT
        }
    }

    private fun backFromBrowser(player: Player, holder: OmniHolder) {
        val categoryId = holder.categoryId ?: return
        val recipeId = holder.recipeId
        if (recipeId == null) menus.openAdminCategory(player, categoryId)
        else plugin.configService.recipe(categoryId, recipeId)?.let { menus.openEditor(player, it) } ?: menus.openAdminCategory(player, categoryId)
    }
}
