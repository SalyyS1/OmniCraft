package com.salyvn.omnicraft.core

data class CraftDurationModifiers(
    /** A duration multiplier below 1.0 is faster; above 1.0 is slower. */
    val durationMultiplier: Double = 1.0,
    val flatReductionSeconds: Double = 0.0,
    val sources: List<String> = emptyList()
)

data class CraftDuration(
    val baseSeconds: Int,
    val effectiveSeconds: Int,
    val modifierSources: List<String>
)

object CraftDurationPolicy {
    fun calculate(craftTime: CraftTime, crafts: Int, modifiers: CraftDurationModifiers = CraftDurationModifiers()): CraftDuration {
        require(crafts > 0) { "craft count must be positive" }
        val base = craftTime.seconds.coerceAtLeast(0)
        val quantity = if (craftTime.quantityScaling == CraftQuantityScaling.LINEAR) crafts.toDouble() else 1.0
        val minimum = craftTime.minimumSeconds.coerceAtLeast(0)
        val maximum = craftTime.maximumSeconds.coerceAtLeast(minimum)
        val multiplier = modifiers.durationMultiplier.takeIf { it.isFinite() }?.coerceIn(0.05, 10.0) ?: 1.0
        val reduction = modifiers.flatReductionSeconds.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val effective = ((base * quantity * multiplier) - reduction)
            .coerceIn(minimum.toDouble(), maximum.toDouble())
            .toInt()
        return CraftDuration(base, effective, modifiers.sources)
    }
}
