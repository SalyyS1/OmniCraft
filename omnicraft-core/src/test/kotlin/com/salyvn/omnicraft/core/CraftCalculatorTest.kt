package com.salyvn.omnicraft.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CraftCalculatorTest {
    private val calculator = CraftCalculator()
    private val recipe = CraftRecipe(
        id = "steel_sword",
        categoryId = "weapons",
        displayName = "Steel Sword",
        output = CraftItem(ItemMode.VANILLA, "IRON_SWORD", 1),
        ingredients = listOf(
            CraftIngredient("iron", CraftItem(ItemMode.VANILLA, "IRON_INGOT", 1), 24),
            CraftIngredient("stick", CraftItem(ItemMode.VANILLA, "STICK", 1), 4)
        ),
        requirements = CraftRequirements(permission = "omnicraft.recipe.steel_sword", level = 10, money = 100.0),
        craft = CraftBehavior(rightAmount = 16, shiftHardCap = 512),
        craftTime = CraftTime(),
        extraction = ExtractionPolicy(),
        limits = CraftLimits()
    )

    @Test
    fun `calculates missing materials and requirement failures`() {
        val check = calculator.check(
            recipe,
            inventory = listOf(
                InventoryEntry(0, ItemKey(ItemMode.VANILLA, "IRON_INGOT"), 5),
                InventoryEntry(1, ItemKey(ItemMode.VANILLA, "STICK"), 4)
            ),
            hasPermission = false,
            level = 3,
            money = 20.0,
            deniedConditions = listOf("%class%=warrior")
        )

        assertFalse(check.allowed)
        assertEquals(19, check.missing["iron"])
        assertEquals(7, check.levelMissing)
        assertEquals(80.0, check.moneyMissing)
        assertTrue(check.permissionDenied)
    }

    @Test
    fun `shift craft is capped by available materials and hard cap`() {
        val check = calculator.check(
            recipe.copy(requirements = CraftRequirements()),
            inventory = listOf(
                InventoryEntry(0, ItemKey(ItemMode.VANILLA, "IRON_INGOT"), 2400),
                InventoryEntry(1, ItemKey(ItemMode.VANILLA, "STICK"), 400)
            ),
            hasPermission = true,
            level = 0,
            money = 0.0,
            deniedConditions = emptyList()
        )

        assertEquals(100, check.craftableAmount)
        assertEquals(100, calculator.requestedAmount(recipe, CraftClickMode.SHIFT, check.craftableAmount))
    }

    @Test
    fun `selection prefers lower risk base items`() {
        val risky = InventoryEntry(0, ItemKey(ItemMode.VANILLA, "IRON_INGOT"), 24, ItemRisk(enchantCount = 2, gemstoneCount = 1))
        val clean = InventoryEntry(1, ItemKey(ItemMode.VANILLA, "IRON_INGOT"), 24)
        val plan = calculator.selectionPlan(recipe.copy(requirements = CraftRequirements()), listOf(risky, clean), 1)

        assertEquals(1, plan["iron"]?.first()?.slot)
    }
}
