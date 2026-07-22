package com.salyvn.omnicraft.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CraftOutcomeResolverTest {
    private val recipe = CraftRecipe(
        id = "test", categoryId = "test", displayName = "Test",
        output = CraftItem(ItemMode.VANILLA, "STONE", 1),
        ingredients = listOf(CraftIngredient("stone", CraftItem(ItemMode.VANILLA, "COBBLESTONE", 1), 1)),
        requirements = CraftRequirements(), craft = CraftBehavior(), craftTime = CraftTime(),
        extraction = ExtractionPolicy(), limits = CraftLimits(),
        outcome = CraftOutcomePolicy(criticalChance = 100.0, criticalBonusCrafts = 1, byproduct = CraftItem(ItemMode.VANILLA, "DIRT", 1), byproductChance = 100.0)
    )

    @Test
    fun `full chances produce bounded deterministic outcomes`() {
        val outcome = CraftOutcomeResolver().resolve(recipe, 3, 123L)
        assertEquals(6, outcome.outputCrafts)
        assertEquals(3, outcome.criticalCrafts)
        assertEquals(3, outcome.byproducts)
        assertEquals(outcome, CraftOutcomeResolver().resolve(recipe, 3, 123L))
    }

    @Test
    fun `maximum reserves every possible extra output`() {
        val maximum = CraftOutcomeResolver().maximum(recipe, 3)
        assertEquals(6, maximum.outputCrafts)
        assertEquals(3, maximum.byproducts)
    }
}
