package com.salyvn.omnicraft.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class CraftCalculator(private val matcher: CraftMatcher = CraftMatcher()) {
    private val allocationPlanner = CraftAllocationPlanner(matcher)
    fun check(
        recipe: CraftRecipe,
        inventory: List<InventoryEntry>,
        hasPermission: Boolean,
        level: Int,
        money: Double,
        deniedConditions: List<String>
    ): CraftCheck {
        val missing = missingForOneCraft(recipe, inventory)
        val maxCrafts = maximumMaterialCrafts(recipe, inventory)

        val levelMissing = max(0, recipe.requirements.level - level)
        val unitMoney = recipe.requirements.money
        val validMoney = unitMoney.isFinite() && unitMoney >= 0.0
        val moneyMissing = if (validMoney) max(0.0, unitMoney - money) else Double.POSITIVE_INFINITY
        val moneyCrafts = when {
            !validMoney || !money.isFinite() || money < 0.0 -> 0
            unitMoney == 0.0 -> Int.MAX_VALUE
            else -> floor(money / unitMoney).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt()
        }
        val finalCrafts = if (hasPermission && levelMissing <= 0 && moneyMissing <= 0.0 && deniedConditions.isEmpty()) {
            min(maxCrafts, moneyCrafts).coerceAtLeast(0)
        } else {
            0
        }

        return CraftCheck(
            craftableAmount = finalCrafts,
            missing = missing,
            permissionDenied = !hasPermission,
            levelMissing = levelMissing,
            moneyMissing = floor(moneyMissing * 100.0) / 100.0,
            conditionDenied = deniedConditions
        )
    }

    fun requestedAmount(recipe: CraftRecipe, mode: CraftClickMode, availableCrafts: Int): Int {
        val requested = when (mode) {
            CraftClickMode.LEFT -> recipe.craft.leftAmount
            CraftClickMode.RIGHT -> recipe.craft.rightAmount
            CraftClickMode.SHIFT -> min(availableCrafts, recipe.craft.shiftHardCap)
        }
        return min(requested.coerceAtLeast(0), availableCrafts.coerceAtLeast(0))
    }

    fun selectionPlan(recipe: CraftRecipe, inventory: List<InventoryEntry>, crafts: Int): Map<String, List<InventoryEntry>> {
        return (allocationPlanner.allocate(recipe, inventory, crafts) as? CraftAllocationResult.Success)
            ?.allocations
            ?: emptyMap()
    }

    fun quote(recipe: CraftRecipe, inventory: List<InventoryEntry>, requestedCrafts: Int, money: Double): CraftQuote {
        val key = RecipeKey.of(recipe)
        if (requestedCrafts <= 0) return CraftQuote(key, requestedCrafts, 0, 0.0, emptyMap(), CraftQuoteFailure.INVALID_QUANTITY)
        val check = check(recipe, inventory, true, Int.MAX_VALUE, money, emptyList())
        val allowed = min(requestedCrafts, check.craftableAmount)
        val allocation = allocationPlanner.allocate(recipe, inventory, allowed)
        val totalMoney = recipe.requirements.money * allowed
        val failure = when {
            !totalMoney.isFinite() || totalMoney < 0.0 -> CraftQuoteFailure.INVALID_RECIPE
            allocation !is CraftAllocationResult.Success -> CraftQuoteFailure.MATERIALS
            allowed == 0 && recipe.requirements.money > money -> CraftQuoteFailure.MONEY
            allowed == 0 -> CraftQuoteFailure.MATERIALS
            else -> null
        }
        return CraftQuote(key, requestedCrafts, allowed, totalMoney, (allocation as? CraftAllocationResult.Success)?.allocations.orEmpty(), failure)
    }

    private fun missingForOneCraft(recipe: CraftRecipe, inventory: List<InventoryEntry>): Map<String, Int> {
        return when (val result = allocationPlanner.allocate(recipe, inventory, 1)) {
            is CraftAllocationResult.Insufficient -> mapOf(result.ingredientId to result.missingAmount)
            else -> emptyMap()
        }
    }

    private fun maximumMaterialCrafts(recipe: CraftRecipe, inventory: List<InventoryEntry>): Int {
        val inputs = recipe.consumedInputs()
        if (inputs.isEmpty() || recipe.output.amount <= 0 || inventory.any { it.amount < 0 }) return 0
        val roughUpperBound = inputs.minOfOrNull { ingredient ->
            if (ingredient.requiredAmount <= 0) return 0
            inventory.asSequence().filter { matcher.matches(ingredient.item, it) }.sumOf { it.amount.coerceAtLeast(0) } / ingredient.requiredAmount
        } ?: return 0
        var low = 0
        var high = roughUpperBound
        while (low < high) {
            val candidate = low + (high - low + 1) / 2
            if (allocationPlanner.allocate(recipe, inventory, candidate) is CraftAllocationResult.Success) low = candidate else high = candidate - 1
        }
        return low
    }
}
