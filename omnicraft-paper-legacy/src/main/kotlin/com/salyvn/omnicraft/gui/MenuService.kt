package com.salyvn.omnicraft.gui

import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.core.CraftBehavior
import com.salyvn.omnicraft.core.CraftLimits
import com.salyvn.omnicraft.core.CraftIngredient
import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.ExtractionMode
import com.salyvn.omnicraft.core.ExtractionPolicy
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.core.CraftRequirements
import com.salyvn.omnicraft.core.CraftTime
import com.salyvn.omnicraft.core.ItemMode
import com.salyvn.omnicraft.core.RecipeOptions
import com.salyvn.omnicraft.craft.CraftService
import com.salyvn.omnicraft.hook.HookService
import com.salyvn.omnicraft.item.ItemAdapter
import com.salyvn.omnicraft.util.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.Locale
import java.util.UUID

class MenuService(
    private val config: ConfigService,
    private val craftService: CraftService,
    private val hooks: HookService
) {
    private val favorites = mutableMapOf<UUID, MutableSet<String>>()
    private val craftableOnly = mutableSetOf<UUID>()
    private val favoritesOnly = mutableSetOf<UUID>()
    private val deleteMode = mutableSetOf<UUID>()

    companion object {
        val INGREDIENT_SLOTS = listOf(
            10, 11, 12, 13,
            19, 20, 21, 22,
            28, 29, 30, 31,
            37, 38, 39, 40
        )
        const val OUTPUT_SLOT = 25
        const val ACTION_NEW_RECIPE = "new_recipe"
        const val ACTION_SET_OUTPUT = "set_output"
        const val ACTION_SET_INGREDIENT = "set_ingredient"
        val ADMIN_RECIPE_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
        val BROWSER_ITEM_SLOTS = (10..16) + (19..25) + (28..34) + (37..43)
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
        recipe.ingredients.take(INGREDIENT_SLOTS.size).forEachIndexed { index, ingredient ->
            val item = ItemAdapter.fromCraftItem(ingredient.item)
            item.amount = ingredient.requiredAmount.coerceAtLeast(1)
            item.editMeta {
                val lore = it.lore()?.toMutableList() ?: mutableListOf()
                val have = ingredient.requiredAmount - (check.missing[ingredient.id] ?: 0)
                val color = if (have >= ingredient.requiredAmount) "#71f79f" else "#ff6961"
                lore += Text.c("$color• Required: $have/${ingredient.requiredAmount}")
                it.lore(lore)
            }
            inv.setItem(INGREDIENT_SLOTS[index], item)
        }

        inv.setItem(OUTPUT_SLOT, productIcon(player, recipe))
        inv.setItem(43, warningIcon(recipe))
        inv.setItem(45, named(Material.CRAFTING_TABLE, "#7cf5ffCraft From Output", listOf(
            "#d6f7ff› Click the product preview.",
            "#d6f7ff• Left: craft x${recipe.craft.leftAmount}",
            "#d6f7ff• Right: craft x${recipe.craft.rightAmount}",
            "#d6f7ff• Shift: craft max"
        )))
        inv.setItem(47, named(Material.HOPPER, "#7cf5ffAutoCraft", listOf(
            "#d6f7ff• Left: queue x${recipe.craft.leftAmount}",
            "#d6f7ff• Right: queue x${recipe.craft.rightAmount}",
            "#d6f7ff• Shift: queue up to the server cap",
            "#8ea3b0› Uses inventory first, then enabled intermediate recipes."
        )))
        inv.setItem(48, named(Material.HOPPER, "#7cf5ffMissing Materials", missingLore(check.missing, recipe)))
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
        category.recipes.take(ADMIN_RECIPE_SLOTS.size).forEachIndexed { index, recipe ->
            inv.setItem(ADMIN_RECIPE_SLOTS[index], recipeIcon(player, recipe))
        }
        if (category.recipes.size < ADMIN_RECIPE_SLOTS.size) {
            inv.setItem(ADMIN_RECIPE_SLOTS[category.recipes.size], createRecipeIcon())
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
        recipe.ingredients.take(INGREDIENT_SLOTS.size).forEachIndexed { index, ingredient ->
            val item = ItemAdapter.fromCraftItem(ingredient.item)
            item.amount = ingredient.requiredAmount.coerceAtLeast(1)
            inv.setItem(INGREDIENT_SLOTS[index], item)
        }
        INGREDIENT_SLOTS.drop(recipe.ingredients.size).forEach { slot ->
            inv.setItem(slot, named(Material.LIME_STAINED_GLASS_PANE, "#71f79fAdd Ingredient", listOf(
                "#d6f7ffClick to browse items.",
                "#8ea3b0Or hold an item on cursor and click."
            )))
        }
        inv.setItem(OUTPUT_SLOT, ItemAdapter.fromCraftItem(recipe.output))
        inv.setItem(24, named(Material.COMPARATOR, "#ffd166Amount Controls", listOf(
            "#d6f7ff• Left click: +1",
            "#d6f7ff• Right click: -1",
            "#d6f7ff• Shift left: +16",
            "#d6f7ff• Shift right: remove",
            "#8ea3b0› Cursor item: replace/add"
        )))
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

    fun setIngredientFromItem(player: Player, recipe: CraftRecipe, slot: Int, item: CraftItem) {
        val index = INGREDIENT_SLOTS.indexOf(slot)
        if (index < 0) return
        val ingredients = recipe.ingredients.toMutableList()
        val ingredient = CraftIngredient(
            id = ingredients.getOrNull(index)?.id ?: "ingredient_${index + 1}",
            item = item.copy(amount = 1),
            requiredAmount = ingredients.getOrNull(index)?.requiredAmount ?: item.amount.coerceAtLeast(1)
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

    fun removeIngredient(player: Player, recipe: CraftRecipe, slot: Int) {
        val index = INGREDIENT_SLOTS.indexOf(slot)
        if (index < 0 || index >= recipe.ingredients.size) return
        val ingredients = recipe.ingredients.toMutableList()
        ingredients.removeAt(index)
        val saved = recipe.copy(ingredients = ingredients.mapIndexed { i, ingredient -> ingredient.copy(id = "ingredient_${i + 1}") })
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

    fun createRecipeFromCursor(player: Player, categoryId: String, stack: ItemStack) {
        createRecipeFromItem(player, categoryId, ItemAdapter.fromStack(stack))
    }

    fun createRecipeFromItem(player: Player, categoryId: String, output: CraftItem) {
        val category = config.category(categoryId) ?: return
        val id = uniqueRecipeId(category.id, output)
        val saved = CraftRecipe(
            id = id,
            categoryId = category.id,
            displayName = output.name ?: output.mmoId ?: output.material,
            output = output.copy(amount = output.amount.coerceAtLeast(1)),
            ingredients = emptyList(),
            requirements = CraftRequirements(),
            craft = CraftBehavior(),
            craftTime = CraftTime(),
            extraction = ExtractionPolicy(),
            limits = CraftLimits(),
            options = RecipeOptions(enabled = false)
        )
        config.saveRecipe(saved)
        player.sendMessage(Text.c("#71f79fCreated recipe ${saved.id}. Add ingredients, then enable it."))
        openEditor(player, saved)
    }

    fun openItemModeBrowser(player: Player, categoryId: String, recipeId: String?, action: String, editorSlot: Int = -1) {
        val holder = OmniHolder(GuiType.ITEM_MODE, categoryId, recipeId, action = action, editorSlot = editorSlot)
        val inv = Bukkit.createInventory(holder, 27, Text.c("#7cf5ffSelect Item Source"))
        holder.attach(inv)
        fill(inv)
        inv.setItem(11, named(Material.GRASS_BLOCK, "#71f79fVanilla Items", listOf("#d6f7ffBrowse Minecraft materials.")))
        inv.setItem(15, named(Material.DIAMOND_SWORD, "#7cf5ffMMOItems", listOf(
            if (hooks.enabled("MMOItems")) "#d6f7ffBrowse MMOItems types." else "#ff6961MMOItems is not enabled."
        )))
        inv.setItem(22, named(Material.ARROW, "#d6f7ffBack", listOf("#8ea3b0Return to editor.")))
        player.openInventory(inv)
    }

    fun openVanillaBrowser(player: Player, categoryId: String, recipeId: String?, action: String, editorSlot: Int, page: Int) {
        val materials = vanillaMaterials()
        val holder = OmniHolder(GuiType.VANILLA_BROWSER, categoryId, recipeId, action = action, editorSlot = editorSlot, page = page.coerceAtLeast(0))
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffVanilla Items"))
        holder.attach(inv)
        fill(inv)
        pageItems(materials, page).forEachIndexed { index, material ->
            inv.setItem(BROWSER_ITEM_SLOTS[index], named(material, "#d6f7ff${material.name}", listOf("#8ea3b0Click to select.")))
        }
        browserNav(inv, page, materials.size)
        player.openInventory(inv)
    }

    fun openMmoTypeBrowser(player: Player, categoryId: String, recipeId: String?, action: String, editorSlot: Int, page: Int) {
        val types = hooks.mmoTypes()
        val holder = OmniHolder(GuiType.MMO_TYPE_BROWSER, categoryId, recipeId, action = action, editorSlot = editorSlot, page = page.coerceAtLeast(0))
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ffMMOItems Types"))
        holder.attach(inv)
        fill(inv)
        if (types.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "#ff6961No MMOItems types", listOf("#8ea3b0Install MMOItems or reload its items.")))
        } else {
            pageItems(types, page).forEachIndexed { index, type ->
                inv.setItem(BROWSER_ITEM_SLOTS[index], named(Material.CHEST, "#7cf5ff$type", listOf("#d6f7ffClick to browse items.")))
            }
        }
        browserNav(inv, page, types.size)
        player.openInventory(inv)
    }

    fun openMmoItemBrowser(player: Player, categoryId: String, recipeId: String?, action: String, editorSlot: Int, type: String, page: Int) {
        val ids = hooks.mmoItemIds(type)
        val holder = OmniHolder(GuiType.MMO_ITEM_BROWSER, categoryId, recipeId, action = action, editorSlot = editorSlot, page = page.coerceAtLeast(0), itemType = type)
        val inv = Bukkit.createInventory(holder, 54, Text.c("#7cf5ff$type Items"))
        holder.attach(inv)
        fill(inv)
        if (ids.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "#ff6961No items in $type", listOf("#8ea3b0Create items in MMOItems first.")))
        } else {
            pageItems(ids, page).forEachIndexed { index, id ->
                val preview = hooks.mmoItem(type, id, 1) ?: named(Material.BOOK, "#d6f7ff$id", listOf("#8ea3b0Click to select."))
                inv.setItem(BROWSER_ITEM_SLOTS[index], preview)
            }
        }
        browserNav(inv, page, ids.size)
        player.openInventory(inv)
    }

    fun applyBrowserSelection(player: Player, holder: OmniHolder, item: CraftItem) {
        when (holder.action) {
            ACTION_NEW_RECIPE -> createRecipeFromItem(player, holder.categoryId ?: return, item)
            ACTION_SET_OUTPUT -> {
                val recipe = config.recipe(holder.categoryId ?: return, holder.recipeId ?: return) ?: return
                val saved = recipe.copy(output = item, displayName = item.name ?: item.mmoId ?: item.material)
                config.saveRecipe(saved)
                openEditor(player, saved)
            }
            ACTION_SET_INGREDIENT -> {
                val recipe = config.recipe(holder.categoryId ?: return, holder.recipeId ?: return) ?: return
                setIngredientFromItem(player, recipe, holder.editorSlot, item)
            }
        }
    }

    fun selectVanilla(player: Player, holder: OmniHolder, slot: Int) {
        val index = BROWSER_ITEM_SLOTS.indexOf(slot)
        if (index < 0) return
        val material = pageItems(vanillaMaterials(), holder.page).getOrNull(index) ?: return
        applyBrowserSelection(player, holder, CraftItem(ItemMode.VANILLA, material.name, 1))
    }

    fun selectMmoType(player: Player, holder: OmniHolder, slot: Int) {
        val index = BROWSER_ITEM_SLOTS.indexOf(slot)
        if (index < 0) return
        val type = pageItems(hooks.mmoTypes(), holder.page).getOrNull(index) ?: return
        openMmoItemBrowser(player, holder.categoryId ?: return, holder.recipeId, holder.action ?: return, holder.editorSlot, type, 0)
    }

    fun selectMmoItem(player: Player, holder: OmniHolder, slot: Int) {
        val type = holder.itemType ?: return
        val index = BROWSER_ITEM_SLOTS.indexOf(slot)
        if (index < 0) return
        val id = pageItems(hooks.mmoItemIds(type), holder.page).getOrNull(index) ?: return
        applyBrowserSelection(player, holder, CraftItem(ItemMode.MMOITEMS, Material.STONE.name, 1, mmoType = type, mmoId = id))
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
            lore += Text.c(if (check.allowed) "#71f79f✓ Craftable" else "#ff6961✕ Missing requirements")
            lore += Text.c("#d6f7ff› Left click: open")
            lore += Text.c("#ffd166★ Right click: favorite")
            it.lore(lore)
        }
        return item
    }

    private fun productIcon(player: Player, recipe: CraftRecipe): ItemStack {
        val item = ItemAdapter.fromCraftItem(recipe.output)
        val check = craftService.check(player, recipe)
        item.editMeta {
            val lore = it.lore()?.toMutableList() ?: mutableListOf()
            lore += Text.c(if (check.missing.isEmpty()) "#71f79f✓ Materials ready" else "#ff6961✕ Missing materials")
            if (check.permissionDenied) lore += Text.c("#ff6961• Permission: denied")
            if (check.levelMissing > 0) lore += Text.c("#ff6961• Level: need ${check.levelMissing} more")
            if (check.moneyMissing > 0.0) lore += Text.c("#ff6961• Money: need ${check.moneyMissing}")
            check.conditionDenied.forEach { condition -> lore += Text.c("#ff6961• Requirement: $condition") }
            recipe.catalyst?.let { lore += Text.c("#ffd166• Catalyst: ${it.amount}x ${it.item.mmoId ?: it.item.material}") }
            recipe.station.material?.let { lore += Text.c("#7cf5ff• Station: $it (r${recipe.station.radius})") }
            if (recipe.outcome.criticalChance > 0.0) lore += Text.c("#ffd166• Critical: ${recipe.outcome.criticalChance}% (+${recipe.outcome.criticalBonusCrafts} craft)")
            recipe.outcome.byproduct?.let { lore += Text.c("#d6f7ff• Byproduct: ${recipe.outcome.byproductChance}% ${it.mmoId ?: it.material}") }
            lore += Text.c("#8ea3b0• Available crafts: ${check.craftableAmount}")
            lore += Text.c("#d6f7ff› Left click: craft x${recipe.craft.leftAmount}")
            lore += Text.c("#d6f7ff› Right click: craft x${recipe.craft.rightAmount}")
            lore += Text.c("#d6f7ff› Shift click: craft max")
            it.lore(lore)
        }
        return item
    }

    private fun warningIcon(recipe: CraftRecipe): ItemStack {
        return named(Material.BELL, "#ffd166⚠ Risk Warning", listOf(
            "#d6f7ff• Enchant: ${recipe.extraction.enchant}",
            "#d6f7ff• Gemstone: ${recipe.extraction.gemstone}",
            "#d6f7ff• Level: ${recipe.extraction.level}",
            "#7cf5ff• Station: ${recipe.station.material ?: "none"}",
            "#ffd166• Catalyst: ${recipe.catalyst?.item?.mmoId ?: recipe.catalyst?.item?.material ?: "none"}",
            "#8ea3b0› Items with fewer risks are consumed first."
        ))
    }

    private fun missingLore(missing: Map<String, Int>, recipe: CraftRecipe): List<String> {
        if (missing.isEmpty()) return listOf("#71f79f✓ All materials ready.")
        return missing.flatMap {
            val lines = mutableListOf("#ff6961✕ ${it.key}: missing ${it.value}")
            recipe.options.sourceHints[it.key]?.takeIf { hint -> hint.isNotBlank() }?.let { hint -> lines += "#8ea3b0› $hint" }
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

    private fun createRecipeIcon(): ItemStack {
        return named(Material.LIME_STAINED_GLASS_PANE, "#71f79f+ Create Recipe", listOf(
            "#d6f7ff› Click to browse an output item.",
            "#8ea3b0› Or hold an item and click this slot."
        ))
    }

    private fun uniqueRecipeId(categoryId: String, item: CraftItem): String {
        val base = (item.mmoId ?: item.name ?: item.material)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .ifBlank { "recipe" }
        var candidate = base
        var index = 2
        while (config.recipe(categoryId, candidate) != null) {
            candidate = "${base}_$index"
            index++
        }
        return candidate
    }

    private fun vanillaMaterials(): List<Material> {
        return Material.entries.filter { it.isItem && !it.isAir }.sortedBy { it.name }
    }

    private fun <T> pageItems(values: List<T>, page: Int): List<T> {
        val from = (page.coerceAtLeast(0) * BROWSER_ITEM_SLOTS.size).coerceAtMost(values.size)
        val to = (from + BROWSER_ITEM_SLOTS.size).coerceAtMost(values.size)
        return values.subList(from, to)
    }

    private fun browserNav(inv: Inventory, page: Int, total: Int) {
        inv.setItem(45, named(Material.ARROW, "#d6f7ffPrevious", listOf("#8ea3b0Page ${page + 1}")))
        inv.setItem(49, named(Material.BARRIER, "#ff6961Back", listOf("#8ea3b0Return to source selection.")))
        inv.setItem(53, named(Material.ARROW, "#d6f7ffNext", listOf("#8ea3b0${total} entries")))
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
