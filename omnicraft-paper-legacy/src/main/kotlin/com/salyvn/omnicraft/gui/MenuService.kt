package com.salyvn.omnicraft.gui

import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.core.CraftIngredient
import com.salyvn.omnicraft.core.ExtractionMode
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
    private val craftableOnly = mutableSetOf<UUID>()
    private val favoritesOnly = mutableSetOf<UUID>()
    private val deleteMode = mutableSetOf<UUID>()

    companion object {
        val INGREDIENT_SLOTS = listOf(
            1, 2, 3, 4, 5,
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31, 32,
            37, 38, 39, 40, 41
        )
        const val OUTPUT_SLOT = 34
    }

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
        visibleRecipes(player, category.recipes).forEachIndexed { index, recipe ->
            val slot = 10 + index + (index / 7) * 2
            if (slot < 45) inv.setItem(slot, recipeIcon(player, recipe))
        }
        inv.setItem(45, named(Material.COMPASS, "#7cf5ffCraftable Only: ${if (craftableOnly.contains(player.uniqueId)) "ON" else "OFF"}", listOf("#8ea3b0Click to toggle.")))
        inv.setItem(46, named(Material.NAME_TAG, "#7cf5ffSearch", listOf("#8ea3b0Use /oc search <category> <text>.")))
        inv.setItem(47, named(Material.NETHER_STAR, "#ffd166Favorites: ${if (favoritesOnly.contains(player.uniqueId)) "ON" else "OFF"}", listOf("#8ea3b0Click to toggle. Right click recipes to favorite.")))
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to main menu.")))
        player.openInventory(inv)
    }

    fun openRecipe(player: Player, recipe: CraftRecipe) {
        val holder = OmniHolder(GuiType.RECIPE, recipe.categoryId, recipe.id)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ff${recipe.displayName}"))
        holder.attach(inv)
        fill(inv)

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
            inv.setItem(INGREDIENT_SLOTS[index], item)
        }

        inv.setItem(OUTPUT_SLOT, productIcon(player, recipe))
        inv.setItem(43, warningIcon(recipe))
        inv.setItem(45, named(Material.LIME_DYE, "#71f79fCraft x${recipe.craft.leftAmount}", listOf("#d6f7ffLeft click.")))
        inv.setItem(46, named(Material.EMERALD, "#71f79fCraft x${recipe.craft.rightAmount}", listOf("#d6f7ffRight click.")))
        inv.setItem(47, named(Material.NETHER_STAR, "#71f79fCraft Max", listOf("#d6f7ffShift click.")))
        inv.setItem(48, named(Material.BOOK, "#7cf5ffMissing Materials", missingLore(check.missing, recipe)))
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to category.")))
        player.openInventory(inv)
    }

    fun openSettings(player: Player) {
        val holder = OmniHolder(GuiType.SETTINGS)
        val inv = Bukkit.createInventory(holder, 27, Text.c("#7cf5ffOmniCraft Settings"))
        holder.attach(inv)
        fill(inv)
        inv.setItem(10, named(Material.CLOCK, "#7cf5ffCraft Time: ${state("craft-time.enabled")}", listOf("#d6f7ffClick to toggle global countdown.")))
        inv.setItem(12, named(Material.SHIELD, "#7cf5ffBlock Creative: ${state("anti-dupe.block-creative")}", listOf("#d6f7ffClick to toggle creative/spectator craft block.")))
        inv.setItem(14, named(Material.ENCHANTED_BOOK, "#7cf5ffStrict AE Hook: ${state("advanced-enchantments.missing-hook-disables-ae-recipes")}", listOf("#d6f7ffClick to fail AE recipes when hook is missing.")))
        inv.setItem(16, named(Material.CHEST, "#7cf5ffRecipes", listOf("#d6f7ffUse /oc browse to edit recipes.")))
        inv.setItem(22, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to main menu.")))
        player.openInventory(inv)
    }

    fun openBrowse(player: Player) {
        val holder = OmniHolder(GuiType.BROWSE)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffRecipe Browser"))
        holder.attach(inv)
        fill(inv)
        config.categories.forEachIndexed { index, category ->
            val slot = 10 + index + (index / 7) * 2
            if (slot < 45) inv.setItem(slot, named(Material.CHEST, "#7cf5ff${category.title}", listOf("#d6f7ffClick to browse/edit.")))
        }
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to main menu.")))
        inv.setItem(53, named(Material.BARRIER, "#ff6961Delete Mode: ${if (deleteMode.contains(player.uniqueId)) "ON" else "OFF"}", listOf("#8ea3b0Click to toggle recipe delete mode.")))
        player.openInventory(inv)
    }

    fun openAdminCategory(player: Player, categoryId: String) {
        val category = config.category(categoryId) ?: return
        val holder = OmniHolder(GuiType.ADMIN_CATEGORY, category.id)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffEdit ${category.title}"))
        holder.attach(inv)
        fill(inv)
        category.recipes.forEachIndexed { index, recipe ->
            val slot = 10 + index + (index / 7) * 2
            if (slot < 45) inv.setItem(slot, recipeIcon(player, recipe))
        }
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to browser.")))
        inv.setItem(53, named(Material.BARRIER, "#ff6961Delete Mode: ${if (deleteMode.contains(player.uniqueId)) "ON" else "OFF"}", listOf("#8ea3b0When ON, click a recipe to delete it.")))
        player.openInventory(inv)
    }

    fun openEditor(player: Player, recipe: CraftRecipe) {
        val holder = OmniHolder(GuiType.EDITOR, recipe.categoryId, recipe.id)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffEdit ${recipe.displayName}"))
        holder.attach(inv)
        fill(inv)
        recipe.ingredients.take(25).forEachIndexed { index, ingredient ->
            val item = ItemAdapter.fromCraftItem(ingredient.item)
            item.amount = ingredient.requiredAmount.coerceAtLeast(1)
            inv.setItem(INGREDIENT_SLOTS[index], item)
        }
        inv.setItem(OUTPUT_SLOT, ItemAdapter.fromCraftItem(recipe.output))
        inv.setItem(45, named(if (recipe.options.enabled) Material.LIME_DYE else Material.GRAY_DYE, "#7cf5ffEnabled: ${recipe.options.enabled}", listOf("#d6f7ffClick to toggle.")))
        inv.setItem(46, named(Material.ENCHANTED_BOOK, "#7cf5ffAE Extract: ${recipe.extraction.enchant}", listOf("#d6f7ffClick to cycle KEEP/DESTROY/EXTRACT.")))
        inv.setItem(47, named(Material.CLOCK, "#7cf5ffCraft Time: ${recipe.craftTime.enabled}", listOf("#d6f7ffClick to toggle recipe countdown.")))
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to admin category.")))
        inv.setItem(53, named(Material.BARRIER, "#ff6961Delete Recipe", listOf("#8ea3b0Sneak click to delete.")))
        player.openInventory(inv)
    }

    fun toggleCategoryCraftable(player: Player, categoryId: String) {
        if (!craftableOnly.add(player.uniqueId)) craftableOnly.remove(player.uniqueId)
        openCategory(player, categoryId)
    }

    fun toggleCategoryFavorites(player: Player, categoryId: String) {
        if (!favoritesOnly.add(player.uniqueId)) favoritesOnly.remove(player.uniqueId)
        openCategory(player, categoryId)
    }

    fun saveOutputFromCursor(player: Player, recipe: CraftRecipe, stack: ItemStack) {
        val saved = recipe.copy(output = ItemAdapter.fromStack(stack))
        config.saveRecipe(saved)
        openEditor(player, saved)
    }

    fun saveIngredientFromCursor(player: Player, recipe: CraftRecipe, slot: Int, stack: ItemStack) {
        val index = INGREDIENT_SLOTS.indexOf(slot)
        if (index < 0) return
        val ingredients = recipe.ingredients.toMutableList()
        val ingredient = CraftIngredient(
            id = ingredients.getOrNull(index)?.id ?: "ingredient_${index + 1}",
            item = ItemAdapter.fromStack(stack).copy(amount = 1),
            requiredAmount = stack.amount.coerceAtLeast(1)
        )
        if (index < ingredients.size) ingredients[index] = ingredient else ingredients += ingredient
        val saved = recipe.copy(ingredients = ingredients)
        config.saveRecipe(saved)
        openEditor(player, saved)
    }

    fun adjustIngredient(player: Player, recipe: CraftRecipe, slot: Int, delta: Int) {
        val index = INGREDIENT_SLOTS.indexOf(slot)
        val current = recipe.ingredients.getOrNull(index) ?: return
        val ingredients = recipe.ingredients.toMutableList()
        val next = (current.requiredAmount + delta).coerceAtLeast(1)
        ingredients[index] = current.copy(requiredAmount = next)
        val saved = recipe.copy(ingredients = ingredients)
        config.saveRecipe(saved)
        openEditor(player, saved)
    }

    fun toggleRecipeEnabled(player: Player, recipe: CraftRecipe) {
        val saved = recipe.copy(options = recipe.options.copy(enabled = !recipe.options.enabled))
        config.saveRecipe(saved)
        openEditor(player, saved)
    }

    fun cycleEnchantExtraction(player: Player, recipe: CraftRecipe) {
        val next = when (recipe.extraction.enchant) {
            ExtractionMode.KEEP -> ExtractionMode.DESTROY
            ExtractionMode.DESTROY -> ExtractionMode.EXTRACT
            ExtractionMode.EXTRACT -> ExtractionMode.KEEP
        }
        val saved = recipe.copy(extraction = recipe.extraction.copy(enchant = next))
        config.saveRecipe(saved)
        openEditor(player, saved)
    }

    fun toggleRecipeCraftTime(player: Player, recipe: CraftRecipe) {
        val saved = recipe.copy(craftTime = recipe.craftTime.copy(enabled = !recipe.craftTime.enabled))
        config.saveRecipe(saved)
        openEditor(player, saved)
    }

    fun deleteRecipe(player: Player, recipe: CraftRecipe) {
        config.deleteRecipe(recipe.categoryId, recipe.id)
        openAdminCategory(player, recipe.categoryId)
    }

    fun toggleDeleteMode(player: Player, categoryId: String?) {
        if (!deleteMode.add(player.uniqueId)) deleteMode.remove(player.uniqueId)
        if (categoryId == null) openBrowse(player) else openAdminCategory(player, categoryId)
    }

    fun isDeleteMode(player: Player): Boolean {
        return deleteMode.contains(player.uniqueId)
    }

    fun toggleFavorite(player: Player, recipe: CraftRecipe) {
        val key = "${recipe.categoryId}:${recipe.id}"
        val set = favoriteSet(player)
        if (!set.add(key)) set.remove(key)
        config.saveFavorites(player.uniqueId.toString(), set)
        player.sendMessage(Text.c("#ffd166Favorite updated: ${recipe.displayName}"))
    }

    fun search(player: Player, categoryId: String, query: String) {
        val category = config.category(categoryId) ?: return
        val holder = OmniHolder(GuiType.CATEGORY, category.id, searchQuery = query)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffSearch: $query"))
        holder.attach(inv)
        fill(inv)
        filteredRecipes(player, category.recipes, query)
            .forEachIndexed { index, recipe ->
                val slot = 10 + index + (index / 7) * 2
                if (slot < 45) inv.setItem(slot, recipeIcon(player, recipe))
            }
        inv.setItem(49, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to category.")))
        player.openInventory(inv)
    }

    fun visibleRecipeAt(player: Player, recipes: List<CraftRecipe>, index: Int): CraftRecipe? {
        if (index < 0) return null
        return visibleRecipes(player, recipes).getOrNull(index)
    }

    fun searchRecipeAt(player: Player, recipes: List<CraftRecipe>, query: String, index: Int): CraftRecipe? {
        if (index < 0) return null
        return filteredRecipes(player, recipes, query).getOrNull(index)
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

    private fun missingLore(missing: Map<String, Int>, recipe: CraftRecipe): List<String> {
        if (missing.isEmpty()) return listOf("#71f79fAll materials ready.")
        return missing.flatMap {
            val lines = mutableListOf("#ff6961${it.key}: missing ${it.value}")
            recipe.options.sourceHints[it.key]?.takeIf { hint -> hint.isNotBlank() }?.let { hint -> lines += "#8ea3b0$hint" }
            lines
        }
    }

    private fun visibleRecipes(player: Player, recipes: List<CraftRecipe>): List<CraftRecipe> {
        val favoriteKeys = favoriteSet(player)
        return recipes
            .filterNot { it.options.hidden }
            .filter { !craftableOnly.contains(player.uniqueId) || craftService.check(player, it).allowed }
            .filter { !favoritesOnly.contains(player.uniqueId) || favoriteKeys.contains("${it.categoryId}:${it.id}") }
    }

    private fun filteredRecipes(player: Player, recipes: List<CraftRecipe>, query: String): List<CraftRecipe> {
        return visibleRecipes(player, recipes)
            .filter {
                it.displayName.contains(query, true) ||
                    it.id.contains(query, true) ||
                    it.output.mmoType?.contains(query, true) == true ||
                    it.output.mmoId?.contains(query, true) == true ||
                    it.ingredients.any { ingredient ->
                        ingredient.item.material.contains(query, true) ||
                            ingredient.item.mmoType?.contains(query, true) == true ||
                            ingredient.item.mmoId?.contains(query, true) == true
                    }
            }
    }

    private fun favoriteSet(player: Player): MutableSet<String> {
        return favorites.getOrPut(player.uniqueId) { config.loadFavorites(player.uniqueId.toString()) }
    }

    private fun state(path: String): String = if (config.pluginConfigBoolean(path)) "ON" else "OFF"

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
