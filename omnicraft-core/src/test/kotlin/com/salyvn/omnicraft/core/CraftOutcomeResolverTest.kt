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
        outcome = CraftOutcomePolicy(criticalChance = 100.0, criticalBonusCrafts = 1, byproduct = CraftItem(ItemMode.VANILLA, "DIRT", 1), byproductChance = 100.0, quality = CraftQualityPolicy("Masterwork", 100.0))
    )

    @Test
    fun `full chances produce bounded deterministic outcomes`() {
        val outcome = CraftOutcomeResolver().resolve(recipe, 3, 123L)
        assertEquals(6, outcome.outputCrafts)
        assertEquals(3, outcome.criticalCrafts)
        assertEquals(3, outcome.byproducts)
        assertEquals(3, outcome.qualityCrafts)
        assertEquals(outcome, CraftOutcomeResolver().resolve(recipe, 3, 123L))
    }

    @Test
    fun `maximum reserves every possible extra output`() {
        val maximum = CraftOutcomeResolver().maximum(recipe, 3)
        assertEquals(6, maximum.outputCrafts)
        assertEquals(3, maximum.byproducts)
        assertEquals(3, maximum.qualityCrafts)
    }

    @Test
    fun `quality name without a chance does not reserve quality stacks`() {
        val disabledQuality = recipe.copy(outcome = recipe.outcome.copy(quality = CraftQualityPolicy("Masterwork", 0.0)))
        assertEquals(0, CraftOutcomeResolver().maximum(disabledQuality, 3).qualityCrafts)
    }
}
