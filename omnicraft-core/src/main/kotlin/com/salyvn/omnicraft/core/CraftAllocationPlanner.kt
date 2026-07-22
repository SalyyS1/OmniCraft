package com.salyvn.omnicraft.core

import kotlin.math.min

/** Allocates each physical inventory slot at most once across all ingredients. */
class CraftAllocationPlanner(private val matcher: CraftMatcher = CraftMatcher()) {
    fun allocate(recipe: CraftRecipe, inventory: List<InventoryEntry>, crafts: Int): CraftAllocationResult {
        if (crafts <= 0) return CraftAllocationResult.InvalidQuantity("craft count must be positive")
        if (inventory.any { it.amount < 0 }) return CraftAllocationResult.InvalidInventory("inventory amounts must not be negative")
        if (inventory.groupBy { it.slot }.any { (_, entries) -> entries.size > 1 }) {
            return CraftAllocationResult.InvalidInventory("inventory snapshot contains duplicate slots")
        }

        val remainingBySlot = inventory.associate { it.slot to it.amount }.toMutableMap()
        val allocations = linkedMapOf<String, MutableList<InventoryEntry>>()

        for (ingredient in recipe.ingredients) {
            if (ingredient.requiredAmount <= 0) {
                return CraftAllocationResult.InvalidQuantity("ingredient ${ingredient.id} must require a positive amount")
            }
            val required = try {
                Math.multiplyExact(ingredient.requiredAmount, crafts)
            } catch (_: ArithmeticException) {
                return CraftAllocationResult.InvalidQuantity("ingredient ${ingredient.id} amount overflows")
            }
            var outstanding = required
            val picked = mutableListOf<InventoryEntry>()
            val candidates = inventory
                .asSequence()
                .filter { matcher.matches(ingredient.item, it) }
                .sortedWith(riskOrder)
                .toList()

            for (entry in candidates) {
                if (outstanding == 0) break
                val available = remainingBySlot.getValue(entry.slot)
                val taken = min(available, outstanding)
                if (taken > 0) {
                    picked += entry.copy(amount = taken)
                    remainingBySlot[entry.slot] = available - taken
                    outstanding -= taken
                }
            }
            if (outstanding > 0) return CraftAllocationResult.Insufficient(ingredient.id, outstanding)
            allocations.getOrPut(ingredient.id) { mutableListOf() }.addAll(picked)
        }
        return CraftAllocationResult.Success(allocations.mapValues { (_, entries) -> entries.toList() })
    }

    private companion object {
        val riskOrder = compareBy<InventoryEntry> { it.risk.enchantCount + it.risk.gemstoneCount + it.risk.upgradeLevel }
            .thenBy { it.risk.enchantCount }
            .thenBy { it.risk.gemstoneCount }
            .thenBy { it.risk.upgradeLevel }
            .thenBy { it.slot }
    }
}

sealed interface CraftAllocationResult {
    data class Success(val allocations: Map<String, List<InventoryEntry>>) : CraftAllocationResult
    data class Insufficient(val ingredientId: String, val missingAmount: Int) : CraftAllocationResult
    data class InvalidQuantity(val reason: String) : CraftAllocationResult
    data class InvalidInventory(val reason: String) : CraftAllocationResult
}
