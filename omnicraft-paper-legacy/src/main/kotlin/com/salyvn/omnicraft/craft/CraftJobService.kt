package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.core.CraftJob
import com.salyvn.omnicraft.core.CraftJobState
import com.salyvn.omnicraft.core.CraftJobStopReason
import com.salyvn.omnicraft.core.RecipeKey
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.ceil

class CraftJobService(private val plugin: OmniCraftPlugin) {
    private data class ActiveJob(val job: CraftJob, val task: BukkitTask, val onCancel: (CraftJobStopReason) -> Unit)
    private val active = mutableMapOf<UUID, ActiveJob>()

    fun activeJob(playerId: UUID): CraftJob? = active[playerId]?.job

    fun start(playerId: UUID, recipeKey: RecipeKey, requestedAmount: Int, durationSeconds: Int, onTick: (Int) -> Unit, onComplete: () -> Unit, onCancel: (CraftJobStopReason) -> Unit): CraftJob? {
        if (active.containsKey(playerId) || requestedAmount <= 0) return null
        val duration = durationSeconds.coerceAtLeast(1)
        val now = System.nanoTime()
        val job = CraftJob(UUID.randomUUID(), playerId, recipeKey, System.currentTimeMillis(), now + duration * 1_000_000_000L, requestedAmount, duration)
        lateinit var task: BukkitTask
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val current = active[playerId] ?: return@Runnable
            if (current.job.jobId != job.jobId) return@Runnable
            val remainingNanos = job.dueAtMonotonicNanos - System.nanoTime()
            if (remainingNanos <= 0L) {
                active.remove(playerId)?.task?.cancel()
                onComplete()
                return@Runnable
            }
            onTick(ceil(remainingNanos / 1_000_000_000.0).toInt().coerceAtLeast(1))
        }, 0L, 20L)
        active[playerId] = ActiveJob(job, task, onCancel)
        return job
    }

    fun cancel(playerId: UUID, reason: CraftJobStopReason): CraftJob? {
        val removed = active.remove(playerId) ?: return null
        removed.task.cancel()
        removed.onCancel(reason)
        return removed.job.copy(state = CraftJobState.CANCELLED, stopReason = reason)
    }

    fun shutdown() {
        active.keys.toList().forEach { cancel(it, CraftJobStopReason.SERVER_STOPPING) }
    }
}
