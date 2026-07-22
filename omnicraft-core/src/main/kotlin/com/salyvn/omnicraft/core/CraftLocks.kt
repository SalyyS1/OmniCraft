package com.salyvn.omnicraft.core

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CraftLocks(private val timeoutMillis: Long = 10_000, private val now: () -> Long = System::currentTimeMillis) {
    init { require(timeoutMillis > 0) { "timeoutMillis must be positive" } }

    private val locks = ConcurrentHashMap<Pair<UUID, RecipeKey>, Long>()
    private val lastClick = ConcurrentHashMap<UUID, Long>()

    fun tryLock(playerId: UUID, recipeId: String): Boolean {
        return tryLock(playerId, RecipeKey.of("legacy", recipeId))
    }

    fun tryLock(playerId: UUID, recipeKey: RecipeKey): Boolean {
        return locks.putIfAbsent(playerId to recipeKey, now()) == null
    }

    fun unlock(playerId: UUID, recipeId: String) {
        unlock(playerId, RecipeKey.of("legacy", recipeId))
    }

    fun unlock(playerId: UUID, recipeKey: RecipeKey) {
        locks.remove(playerId to recipeKey)
    }

    fun unlockPlayer(playerId: UUID) {
        locks.keys.removeIf { it.first == playerId }
    }

    fun throttle(playerId: UUID, cooldownMillis: Long): Boolean {
        val current = now()
        val previous = lastClick.put(playerId, current) ?: return true
        return current - previous >= cooldownMillis.coerceAtLeast(0)
    }

    fun clear() {
        locks.clear()
        lastClick.clear()
    }

}
