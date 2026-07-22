package com.salyvn.omnicraft.core

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CraftLocksTest {
    @Test
    fun `lock rejects duplicate recipe transaction`() {
        val locks = CraftLocks()
        val player = UUID.randomUUID()

        assertTrue(locks.tryLock(player, "recipe"))
        assertFalse(locks.tryLock(player, "recipe"))
        locks.unlock(player, "recipe")
        assertTrue(locks.tryLock(player, "recipe"))
    }

    @Test
    fun `throttle rejects rapid clicks`() {
        val locks = CraftLocks()
        val player = UUID.randomUUID()

        assertTrue(locks.throttle(player, 500))
        assertFalse(locks.throttle(player, 500))
    }

    @Test
    fun `category is part of canonical lock identity and active locks do not expire`() {
        var time = 1_000L
        val locks = CraftLocks(timeoutMillis = 10_000) { time }
        val player = UUID.randomUUID()

        assertTrue(locks.tryLock(player, RecipeKey.of("weapons", "sword")))
        assertTrue(locks.tryLock(player, RecipeKey.of("tools", "sword")))
        time += 10_000
        assertFalse(locks.tryLock(player, RecipeKey.of("weapons", "sword")))
        assertEquals("weapons:sword", RecipeKey.of(" Weapons ", "SWORD").toString())
    }
}
