package com.salyvn.omnicraft.core

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CraftLocks(private val timeoutMillis: Long = 10_000) {
    private val locks = ConcurrentHashMap<String, Long>()
    private val lastClick = ConcurrentHashMap<UUID, Long>()

    fun tryLock(playerId: UUID, recipeId: String): Boolean {
        cleanup()
        val key = "$playerId:$recipeId"
        return locks.putIfAbsent(key, System.currentTimeMillis()) == null
    }

    fun unlock(playerId: UUID, recipeId: String) {
        locks.remove("$playerId:$recipeId")
    }

    fun throttle(playerId: UUID, cooldownMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastClick.put(playerId, now) ?: return true
        return now - previous >= cooldownMillis
    }

    fun clear() {
        locks.clear()
        lastClick.clear()
    }

    private fun cleanup() {
        val now = System.currentTimeMillis()
        locks.entries.removeIf { now - it.value > timeoutMillis }
    }
}
