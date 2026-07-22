package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.core.AutoCraftPlanResult
import com.salyvn.omnicraft.core.AutoCraftPlanner
import com.salyvn.omnicraft.core.AutoCraftStep
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.core.CraftJobStopReason
import com.salyvn.omnicraft.core.ItemMode
import com.salyvn.omnicraft.item.ItemAdapter
import com.salyvn.omnicraft.util.Text
import org.bukkit.entity.Player
import java.util.UUID

/** One bounded main-thread dispatcher for all online AutoCraft runs. */
class CraftQueueService(
    private val plugin: OmniCraftPlugin,
    private val config: ConfigService,
    private val craft: CraftService,
    private val repository: AutoCraftQueueRepository
) {
    data class Status(val target: com.salyvn.omnicraft.core.RecipeKey, val pendingCrafts: Int, val committing: Boolean)
    private data class PendingStep(val key: com.salyvn.omnicraft.core.RecipeKey, var remaining: Int)
    private data class Run(
        val target: com.salyvn.omnicraft.core.RecipeKey,
        val steps: ArrayDeque<PendingStep>,
        var committing: Boolean = false,
        var nextDispatchNanos: Long = 0L
    )
    private val runs = mutableMapOf<UUID, Run>()

    init {
        val interval = plugin.config.getLong("auto-craft.dispatch-interval-ticks", 2L).coerceIn(1L, 20L)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, interval, interval)
    }

    fun start(player: Player, target: CraftRecipe, crafts: Int): String? {
        if (!plugin.config.getBoolean("features.auto-craft", false)) return "disabled"
        if (!player.hasPermission("omnicraft.use")) return "no-use-permission"
        if (!player.hasPermission("omnicraft.auto-craft")) return "no-permission"
        if (!target.options.enabled) return "target-disabled"
        val category = config.category(target.categoryId) ?: return "category-missing"
        if (!player.hasPermission(category.permission) && !player.hasPermission("omnicraft.open.${category.id}")) return "category-permission"
        if (crafts !in 1..plugin.config.getInt("auto-craft.max-target-crafts", 64).coerceAtLeast(1)) return "target-limit"
        if (runs.containsKey(player.uniqueId) || craft.isBusy(player.uniqueId)) return "busy"
        if (runs.size >= plugin.config.getInt("auto-craft.max-active-runs", 64).coerceAtLeast(1)) return "queue-full"
        val plan = AutoCraftPlanner().plan(target, crafts, config.categories.flatMap { it.recipes }, ItemAdapter.inventoryEntries(player.inventory.storageContents))
        val steps = (plan as? AutoCraftPlanResult.Success)?.steps ?: return (plan as AutoCraftPlanResult.Failure).reason
        val targetKey = com.salyvn.omnicraft.core.RecipeKey.of(target)
        if (!repository.createPaused(player.uniqueId, targetKey, steps)) return "persistence-unavailable"
        runs[player.uniqueId] = Run(targetKey, ArrayDeque(steps.map { PendingStep(it.recipeKey, it.crafts) }))
        player.sendMessage(Text.c("#7cf5ffAutoCraft started: ${target.displayName} x$crafts"))
        return null
    }

    fun cancel(playerId: UUID, reason: String = "cancelled") {
        val run = runs.remove(playerId) ?: return
        if (run.committing) repository.abandon(playerId, reason) else repository.cancelPaused(playerId)
        craft.cancelPlayer(playerId, CraftJobStopReason.PLAYER_CANCELLED)
        plugin.server.getPlayer(playerId)?.sendMessage(Text.c("#ffd166AutoCraft ${run.target} $reason."))
    }

    fun isActive(playerId: UUID): Boolean = runs.containsKey(playerId)

    fun status(playerId: UUID): Status? = runs[playerId]?.let { run ->
        Status(run.target, run.steps.sumOf { it.remaining }, run.committing)
    }

    fun shutdown() {
        runs.entries.toList().forEach { (playerId, run) ->
            if (run.committing) repository.abandon(playerId, "server-stopping-during-attempt")
        }
        runs.clear()
        repository.close()
    }

    fun cancelAll(reason: String) {
        runs.keys.toList().forEach { cancel(it, reason) }
    }

    fun resume(player: Player): String? {
        if (!plugin.config.getBoolean("features.auto-craft", false)) return "disabled"
        if (!player.hasPermission("omnicraft.use")) return "no-use-permission"
        if (!player.hasPermission("omnicraft.auto-craft")) return "no-permission"
        if (runs.containsKey(player.uniqueId) || craft.isBusy(player.uniqueId)) return "busy"
        val persisted = repository.loadPaused(player.uniqueId) ?: return "no-paused-queue"
        if (persisted.steps.isEmpty()) {
            repository.cancelPaused(player.uniqueId)
            return "queue-invalid"
        }
        val target = config.recipe(persisted.target.categoryId, persisted.target.recipeId)
        val category = target?.let { config.category(it.categoryId) }
        if (target == null || category == null || !target.options.enabled ||
            (!player.hasPermission(category.permission) && !player.hasPermission("omnicraft.open.${category.id}"))
        ) {
            repository.abandon(player.uniqueId, "resume-validation-failed")
            return "queue-unavailable"
        }
        runs[player.uniqueId] = Run(persisted.target, ArrayDeque(persisted.steps.map { PendingStep(it.recipeKey, it.crafts) }))
        return null
    }

    private fun tick() {
        runs.entries.toList().forEach { (playerId, run) ->
            val player = plugin.server.getPlayer(playerId)
            if (player == null || !player.isOnline) {
                cancel(playerId, "cancelled on logout")
                return@forEach
            }
            if (run.committing || craft.isBusy(playerId) || !craft.isServerLoadSafe() || System.nanoTime() < run.nextDispatchNanos) return@forEach
            val pending = run.steps.firstOrNull() ?: kotlin.run {
                runs.remove(playerId)
                player.sendMessage(Text.c("#71f79fAutoCraft completed: ${run.target}"))
                return@forEach
            }
            val recipe = config.recipe(pending.key.categoryId, pending.key.recipeId) ?: kotlin.run {
                cancel(playerId, "stopped: recipe changed")
                return@forEach
            }
            val category = config.category(recipe.categoryId)
            if (category == null || (!player.hasPermission(category.permission) && !player.hasPermission("omnicraft.open.${category.id}"))) {
                cancel(playerId, "stopped: category permission changed")
                return@forEach
            }
            val isTarget = pending.key == run.target
            val sourceEnabled = recipe.options.sourceHints["auto-craft.enabled"]?.equals("true", true) == true
            if (!recipe.options.enabled || (!isTarget && !sourceEnabled) || (recipe.output.mode == ItemMode.MMOITEMS && !plugin.hooks.enabled("MMOItems"))) {
                cancel(playerId, "stopped: recipe unavailable")
                return@forEach
            }
            run.committing = true
            if (!repository.markInFlight(playerId)) {
                run.committing = false
                return@forEach
            }
            if (!craft.executeAutomated(player, recipe, 1) { committed ->
                    run.committing = false
                    if (!committed) {
                        if (!craft.isServerLoadSafe()) {
                            run.nextDispatchNanos = System.nanoTime() + 1_000_000_000L
                        } else {
                            repository.abandon(playerId, "attempt-failed")
                            cancel(playerId, "stopped: requirements changed")
                        }
                    } else if (--pending.remaining <= 0) {
                        run.steps.removeFirst()
                        run.nextDispatchNanos = System.nanoTime() + recipe.craft.cooldownMillis.coerceAtLeast(0) * 1_000_000L
                    } else {
                        run.nextDispatchNanos = System.nanoTime() + recipe.craft.cooldownMillis.coerceAtLeast(0) * 1_000_000L
                    }
                    val persistedSteps = run.steps.map { AutoCraftStep(it.key, it.remaining) }
                    if (!repository.completeAttempt(playerId, persistedSteps)) runs.remove(playerId)
                }) {
                run.committing = false
                repository.abandon(playerId, "dispatch-failed")
                cancel(playerId, "stopped: busy")
            }
        }
    }
}
