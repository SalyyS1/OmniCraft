package com.salyvn.omnicraft.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CraftDurationPolicyTest {
    @Test
    fun `linear quantity and fast craft multiplier reduce duration`() {
        val duration = CraftDurationPolicy.calculate(
            CraftTime(enabled = true, seconds = 10, minimumSeconds = 2, maximumSeconds = 120),
            crafts = 3,
            modifiers = CraftDurationModifiers(durationMultiplier = 0.5, flatReductionSeconds = 1.0, sources = listOf("vip"))
        )

        assertEquals(10, duration.baseSeconds)
        assertEquals(14, duration.effectiveSeconds)
        assertEquals(listOf("vip"), duration.modifierSources)
    }

    @Test
    fun `duration clamps invalid provider values and recipe bounds`() {
        val craftTime = CraftTime(enabled = true, seconds = 1, minimumSeconds = 3, maximumSeconds = 8)
        assertEquals(3, CraftDurationPolicy.calculate(craftTime, 1, CraftDurationModifiers(durationMultiplier = 0.0)).effectiveSeconds)
        assertEquals(8, CraftDurationPolicy.calculate(craftTime, 1, CraftDurationModifiers(durationMultiplier = 100.0)).effectiveSeconds)
    }

    @Test
    fun `fixed quantity ignores batch size`() {
        val craftTime = CraftTime(enabled = true, seconds = 7, quantityScaling = CraftQuantityScaling.FIXED)
        assertEquals(7, CraftDurationPolicy.calculate(craftTime, 50).effectiveSeconds)
    }
}
