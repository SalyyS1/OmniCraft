package com.salyvn.omnicraft.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AutoCraftPlannerTest {
    @Test fun `rejects recursive recipe cycles`() {
        val a = CraftRecipe("a", "x", "a", CraftItem(ItemMode.VANILLA, "STONE", 1), listOf(CraftIngredient("b", CraftItem(ItemMode.VANILLA, "DIRT", 1), 1)), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits(), RecipeOptions(sourceHints = mapOf("auto-craft.enabled" to "true")))
        val b = a.copy(id = "b", output = CraftItem(ItemMode.VANILLA, "DIRT", 1), ingredients = listOf(CraftIngredient("a", CraftItem(ItemMode.VANILLA, "STONE", 1), 1)))
        assertIs<AutoCraftPlanResult.Failure>(AutoCraftPlanner().plan(a, listOf(a, b)))
    }

    @Test fun `uses inventory before crafting enabled intermediate`() {
        val ingot = CraftItem(ItemMode.VANILLA, "IRON_INGOT", 1)
        val plate = CraftItem(ItemMode.VANILLA, "IRON_BLOCK", 1)
        val target = CraftRecipe("sword", "weapons", "Sword", CraftItem(ItemMode.VANILLA, "IRON_SWORD", 1), listOf(CraftIngredient("plate", plate, 2)), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits())
        val intermediate = CraftRecipe("plate", "parts", "Plate", plate, listOf(CraftIngredient("ingot", ingot, 3)), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits(), RecipeOptions(sourceHints = mapOf("auto-craft.enabled" to "true", "auto-craft.priority" to "10")))
        val inventory = listOf(InventoryEntry(0, ItemKey(ItemMode.VANILLA, "IRON_BLOCK"), 1), InventoryEntry(1, ItemKey(ItemMode.VANILLA, "IRON_INGOT"), 3))

        val result = AutoCraftPlanner().plan(target, 1, listOf(target, intermediate), inventory)

        val success = assertIs<AutoCraftPlanResult.Success>(result)
        assertEquals(listOf(AutoCraftStep(RecipeKey.of(intermediate), 1), AutoCraftStep(RecipeKey.of(target), 1)), success.steps)
    }

    @Test fun `fails when a missing ingredient has no enabled source`() {
        val target = CraftRecipe("target", "x", "Target", CraftItem(ItemMode.VANILLA, "STONE", 1), listOf(CraftIngredient("missing", CraftItem(ItemMode.VANILLA, "DIAMOND", 1), 1)), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits())
        val result = AutoCraftPlanner().plan(target, 1, listOf(target), emptyList())
        assertEquals(AutoCraftPlanResult.Failure("missing-source:ItemKey(mode=VANILLA, material=DIAMOND, uid=null, mmoType=null, mmoId=null)"), result)
    }

    @Test fun `chooses highest priority source deterministically`() {
        val gem = CraftItem(ItemMode.VANILLA, "EMERALD", 1)
        val target = CraftRecipe("target", "x", "Target", CraftItem(ItemMode.VANILLA, "STONE", 1), listOf(CraftIngredient("gem", gem, 1)), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits())
        val low = CraftRecipe("low", "x", "Low", gem, emptyList(), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits(), RecipeOptions(sourceHints = mapOf("auto-craft.enabled" to "true", "auto-craft.priority" to "1")))
        val high = low.copy(id = "high", options = RecipeOptions(sourceHints = mapOf("auto-craft.enabled" to "true", "auto-craft.priority" to "5")))

        val result = assertIs<AutoCraftPlanResult.Success>(AutoCraftPlanner().plan(target, 1, listOf(target, low, high), emptyList()))

        assertEquals(RecipeKey.of(high), result.steps.first().recipeKey)
    }

    @Test fun `preserves repeated source ordering when a later use gains a new prerequisite`() {
        val raw = CraftItem(ItemMode.VANILLA, "COBBLESTONE", 1)
        val intermediate = CraftItem(ItemMode.VANILLA, "STONE", 1)
        val target = CraftRecipe("target", "x", "Target", CraftItem(ItemMode.VANILLA, "DIAMOND", 1), listOf(
            CraftIngredient("first", intermediate, 1), CraftIngredient("second", intermediate, 1)
        ), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits())
        val source = CraftRecipe("source", "x", "Source", intermediate, listOf(CraftIngredient("raw", raw, 1)), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits(), RecipeOptions(sourceHints = mapOf("auto-craft.enabled" to "true")))
        val rawSource = CraftRecipe("raw-source", "x", "Raw source", raw, emptyList(), CraftRequirements(), CraftBehavior(), CraftTime(), ExtractionPolicy(), CraftLimits(), RecipeOptions(sourceHints = mapOf("auto-craft.enabled" to "true")))
        val inventory = listOf(InventoryEntry(0, ItemKey(ItemMode.VANILLA, "COBBLESTONE"), 1))

        val result = assertIs<AutoCraftPlanResult.Success>(AutoCraftPlanner().plan(target, 1, listOf(target, source, rawSource), inventory))

        assertEquals(listOf(RecipeKey.of(source), RecipeKey.of(rawSource), RecipeKey.of(source), RecipeKey.of(target)), result.steps.map { it.recipeKey })
    }
}
