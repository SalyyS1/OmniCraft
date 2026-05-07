package com.salyvn.omnicraft.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class CraftCalculator(private val matcher: CraftMatcher = CraftMatcher()) {
    fun check(
        recipe: CraftRecipe,
        inventory: List<InventoryEntry>,
        hasPermission: Boolean,
        level: Int,
        money: Double,
        deniedConditions: List<String>
    ): CraftCheck {
        val missing = linkedMapOf<String, Int>()
        var maxCrafts = Int.MAX_VALUE

        for (ingredient in recipe.ingredients) {
            val available = inventory
                .filter { matcher.matches(ingredient.item, it) }
                .sumOf { max(0, it.amount) }
            val possible = available / ingredient.requiredAmount
            maxCrafts = min(maxCrafts, possible)
            if (available < ingredient.requiredAmount) {
                missing[ingredient.id] = ingredient.requiredAmount - available
            }
        }

        if (recipe.ingredients.isEmpty()) {
            maxCrafts = 0
        }

        val levelMissing = max(0, recipe.requirements.level - level)
        val moneyMissing = max(0.0, recipe.requirements.money - money)
        val finalCrafts = if (hasPermission && levelMissing <= 0 && moneyMissing <= 0.0 && deniedConditions.isEmpty()) {
            max(0, maxCrafts)
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
        return min(requested, availableCrafts).coerceAtLeast(0)
    }

    fun selectionPlan(recipe: CraftRecipe, inventory: List<InventoryEntry>, crafts: Int): Map<String, List<InventoryEntry>> {
        val plan = linkedMapOf<String, List<InventoryEntry>>()
        for (ingredient in recipe.ingredients) {
            var remaining = ingredient.requiredAmount * crafts
            val picked = mutableListOf<InventoryEntry>()
            val candidates = inventory
                .filter { matcher.matches(ingredient.item, it) }
                .sortedWith(compareBy<InventoryEntry> { it.risk.enchantCount + it.risk.gemstoneCount + it.risk.upgradeLevel }
                    .thenBy { it.risk.enchantCount }
                    .thenBy { it.risk.gemstoneCount }
                    .thenBy { it.risk.upgradeLevel })

            for (entry in candidates) {
                if (remaining <= 0) break
                val take = min(entry.amount, remaining)
                picked += entry.copy(amount = take)
                remaining -= take
            }
            plan[ingredient.id] = picked
        }
        return plan
    }
}
