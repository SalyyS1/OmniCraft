package com.salyvn.omnicraft.gui

import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.craft.CraftService
import com.salyvn.omnicraft.item.ItemAdapter
import com.salyvn.omnicraft.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID

class MenuService(
    private val config: ConfigService,
    private val craftService: CraftService
) {
    private val favorites = mutableMapOf<UUID, MutableSet<String>>()

    fun openMain(player: Player) {
        val holder = OmniHolder(GuiType.MAIN)
        val inv = Bukkit.createInventory(holder, 54, Text.c(config.message("gui.main-title", "#7cf5ffOmniCraft")))
        holder.attach(inv)
        fill(inv)
        for (category in config.categories) {
            val item = named(Material.matchMaterial(category.icon) ?: Material.CRAFTING_TABLE, "#7cf5ff${category.title}", listOf(
                "#d6f7ffClick to open.",
                "#8ea3b0Permission: ${category.permission}"
            ))
            inv.setItem(category.slot.coerceIn(0, 53), item)
        }
        player.openInventory(inv)
    }

    fun openCategory(player: Player, categoryId: String) {
        val category = config.category(categoryId)
        if (category == null) {
            player.sendMessage(Text.c(config.message("errors.category-not-found", "#ff6961Category not found.")))
            return
        }
        if (!player.hasPermission(category.permission) && !player.hasPermission("omnicraft.open.${category.id}")) {
            player.sendMessage(Text.c(config.message("errors.no-category-permission", "#ff6961You cannot open this category.")))
            return
        }

        val holder = OmniHolder(GuiType.CATEGORY, category.id)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ff${category.title}"))
        holder.attach(inv)
        fill(inv)
        category.recipes.forEachIndexed { index, recipe ->
            val slot = 10 + index + (index / 7) * 2
            if (slot < 45) inv.setItem(slot, recipeIcon(player, recipe))
        }
        inv.setItem(45, named(Material.COMPASS, "#7cf5ffCraftable Only", listOf("#8ea3b0Filter placeholder.")))
        inv.setItem(46, named(Material.NAME_TAG, "#7cf5ffSearch", listOf("#8ea3b0Search placeholder.")))
        inv.setItem(47, named(Material.NETHER_STAR, "#ffd166Favorites", listOf("#8ea3b0Right click recipes to favorite.")))
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to main menu.")))
        player.openInventory(inv)
    }

    fun openRecipe(player: Player, recipe: CraftRecipe) {
        val holder = OmniHolder(GuiType.RECIPE, recipe.categoryId, recipe.id)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ff${recipe.displayName}"))
        holder.attach(inv)
        fill(inv)

        val ingredientSlots = listOf(
            1, 2, 3, 4, 5,
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31, 32,
            37, 38, 39, 40, 41
        )
        val check = craftService.check(player, recipe)
        recipe.ingredients.take(25).forEachIndexed { index, ingredient ->
            val item = ItemAdapter.fromCraftItem(ingredient.item)
            item.amount = ingredient.requiredAmount.coerceAtLeast(1)
            item.editMeta {
                val lore = it.lore()?.toMutableList() ?: mutableListOf()
                val have = ingredient.requiredAmount - (check.missing[ingredient.id] ?: 0)
                val color = if (have >= ingredient.requiredAmount) "#71f79f" else "#ff6961"
                lore += Text.c("$color$have/${ingredient.requiredAmount}")
                it.lore(lore)
            }
            inv.setItem(ingredientSlots[index], item)
        }

        inv.setItem(34, productIcon(player, recipe))
        inv.setItem(43, warningIcon(recipe))
        inv.setItem(45, named(Material.LIME_DYE, "#71f79fCraft x${recipe.craft.leftAmount}", listOf("#d6f7ffLeft click.")))
        inv.setItem(46, named(Material.EMERALD, "#71f79fCraft x${recipe.craft.rightAmount}", listOf("#d6f7ffRight click.")))
        inv.setItem(47, named(Material.NETHER_STAR, "#71f79fCraft Max", listOf("#d6f7ffShift click.")))
        inv.setItem(48, named(Material.BOOK, "#7cf5ffMissing Materials", missingLore(check.missing)))
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to category.")))
        player.openInventory(inv)
    }

    fun openSettings(player: Player) {
        val holder = OmniHolder(GuiType.SETTINGS)
        val inv = Bukkit.createInventory(holder, 27, Text.c("#7cf5ffOmniCraft Settings"))
        holder.attach(inv)
        fill(inv)
        inv.setItem(10, named(Material.COMPARATOR, "#7cf5ffGlobal Config", listOf("#d6f7ffEdit config.yml.")))
        inv.setItem(12, named(Material.BOOK, "#7cf5ffMessages", listOf("#d6f7ffEdit messages.yml.")))
        inv.setItem(14, named(Material.CLOCK, "#7cf5ffCraft Time", listOf("#d6f7ffGlobal and per recipe.")))
        inv.setItem(16, named(Material.CHEST, "#7cf5ffRecipes", listOf("#d6f7ffUse /oc browse.")))
        player.openInventory(inv)
    }

    fun openBrowse(player: Player) {
        val holder = OmniHolder(GuiType.BROWSE)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffRecipe Browser"))
        holder.attach(inv)
        fill(inv)
        config.categories.forEachIndexed { index, category ->
            inv.setItem(10 + index, named(Material.CHEST, "#7cf5ff${category.title}", listOf("#d6f7ffClick to browse/edit.")))
        }
        inv.setItem(49, named(Material.BARRIER, "#ff6961Delete Mode: OFF", listOf("#8ea3b0Confirm before deleting recipes.")))
        player.openInventory(inv)
    }

    fun toggleFavorite(player: Player, recipe: CraftRecipe) {
        val key = "${recipe.categoryId}:${recipe.id}"
        val set = favorites.getOrPut(player.uniqueId) { mutableSetOf() }
        if (!set.add(key)) set.remove(key)
        player.sendMessage(Text.c("#ffd166Favorite updated: ${recipe.displayName}"))
    }

    private fun recipeIcon(player: Player, recipe: CraftRecipe): ItemStack {
        val item = ItemAdapter.fromCraftItem(recipe.output)
        val check = craftService.check(player, recipe)
        item.editMeta {
            val lore = it.lore()?.toMutableList() ?: mutableListOf()
            lore += Text.c(if (check.allowed) "#71f79fOK Craftable" else "#ff6961X Missing requirements")
            lore += Text.c("#d6f7ffLeft click to open.")
            lore += Text.c("#d6f7ffRight click to favorite.")
            it.lore(lore)
        }
        return item
    }

    private fun productIcon(player: Player, recipe: CraftRecipe): ItemStack {
        val item = ItemAdapter.fromCraftItem(recipe.output)
        val check = craftService.check(player, recipe)
        item.editMeta {
            val lore = it.lore()?.toMutableList() ?: mutableListOf()
            lore += Text.c(if (check.missing.isEmpty()) "#71f79fOK Materials" else "#ff6961X Materials")
            if (check.permissionDenied) lore += Text.c("#ff6961You don't have permission to craft ${recipe.displayName}")
            if (check.levelMissing > 0) lore += Text.c("#ff6961Need ${check.levelMissing} more levels")
            if (check.moneyMissing > 0.0) lore += Text.c("#ff6961Need ${check.moneyMissing} money")
            lore += Text.c("#8ea3b0Available crafts: ${check.craftableAmount}")
            it.lore(lore)
        }
        return item
    }

    private fun warningIcon(recipe: CraftRecipe): ItemStack {
        return named(Material.PAPER, "#ffd166Risk Warning", listOf(
            "#d6f7ffEnchant: ${recipe.extraction.enchant}",
            "#d6f7ffGemstone: ${recipe.extraction.gemstone}",
            "#d6f7ffLevel: ${recipe.extraction.level}",
            "#8ea3b0Items with fewer risks are consumed first."
        ))
    }

    private fun missingLore(missing: Map<String, Int>): List<String> {
        if (missing.isEmpty()) return listOf("#71f79fAll materials ready.")
        return missing.map { "#ff6961${it.key}: missing ${it.value}" }
    }

    private fun fill(inv: Inventory) {
        val pane = named(Material.GRAY_STAINED_GLASS_PANE, " ", emptyList())
        for (i in 0 until inv.size) inv.setItem(i, pane)
    }

    private fun named(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        item.editMeta {
            it.displayName(Text.c(name))
            it.lore(lore.map { line -> Text.c(line) })
        }
        return item
    }
}
