package com.salyvn.omnicraft.core

import java.util.UUID

enum class CraftJobState { RUNNING, COMPLETED, CANCELLED }

enum class CraftJobStopReason { PLAYER_CANCELLED, LOGOUT, MOVED, SERVER_STOPPING, COMPLETED }

/** Immutable state exposed by the Paper job registry; it never owns Bukkit objects. */
data class CraftJob(
    val jobId: UUID,
    val playerId: UUID,
    val recipeKey: RecipeKey,
    val createdAtEpochMillis: Long,
    val dueAtMonotonicNanos: Long,
    val requestedAmount: Int,
    val durationSeconds: Int,
    val state: CraftJobState = CraftJobState.RUNNING,
    val stopReason: CraftJobStopReason? = null
)
