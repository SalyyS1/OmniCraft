package com.salyvn.omnicraft.core

import java.util.Random

data class CraftOutcome(
    val seed: Long,
    val outputCrafts: Int,
    val criticalCrafts: Int,
    val byproducts: Int,
    val qualityCrafts: Int
)

/** Resolves random RPG outcomes from an auditable seed without touching Bukkit state. */
class CraftOutcomeResolver {
    fun resolve(recipe: CraftRecipe, crafts: Int, seed: Long): CraftOutcome {
        require(crafts > 0) { "crafts must be positive" }
        val policy = recipe.outcome
        val random = Random(seed)
        val criticalChance = policy.criticalChance.coerceIn(0.0, 100.0)
        val byproductChance = policy.byproductChance.coerceIn(0.0, 100.0)
        val qualityChance = if (policy.quality.name.isNullOrBlank()) 0.0 else policy.quality.chance.coerceIn(0.0, 100.0)
        val bonus = policy.criticalBonusCrafts.coerceIn(0, 64)
        var criticals = 0
        var byproducts = 0
        var qualityCrafts = 0
        repeat(crafts) {
            if (random.nextDouble() * 100.0 < criticalChance) criticals++
            if (random.nextDouble() * 100.0 < qualityChance) qualityCrafts++
            if (policy.byproduct != null && random.nextDouble() * 100.0 < byproductChance) byproducts++
        }
        val outputCrafts = Math.addExact(crafts, Math.multiplyExact(criticals, bonus))
        return CraftOutcome(seed, outputCrafts, criticals, byproducts, qualityCrafts)
    }

    fun maximum(recipe: CraftRecipe, crafts: Int): CraftOutcome {
        require(crafts > 0) { "crafts must be positive" }
        val bonus = recipe.outcome.criticalBonusCrafts.coerceIn(0, 64)
        return CraftOutcome(
            0L,
            Math.addExact(crafts, Math.multiplyExact(crafts, bonus)),
            crafts,
            if (recipe.outcome.byproduct == null) 0 else crafts,
            if (recipe.outcome.quality.name.isNullOrBlank() || recipe.outcome.quality.chance <= 0.0) 0 else crafts
        )
    }
}
