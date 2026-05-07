package com.salyvn.omnicraft.core

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
}
