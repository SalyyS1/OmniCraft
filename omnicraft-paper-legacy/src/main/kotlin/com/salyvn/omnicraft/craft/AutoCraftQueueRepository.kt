package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.config.AtomicYamlFile
import com.salyvn.omnicraft.core.AutoCraftStep
import com.salyvn.omnicraft.core.RecipeKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID

/** Bounded, atomically-written queue persistence. IN_FLIGHT work is never replayed. */
class AutoCraftQueueRepository(private val plugin: JavaPlugin) : AutoCloseable {
    data class PersistedQueue(val playerId: UUID, val target: RecipeKey, val steps: List<AutoCraftStep>)

    private val file = File(plugin.dataFolder, "data/autocraft-queues.yml")
    private var yaml = YamlConfiguration()

    init {
        file.parentFile.mkdirs()
        yaml = YamlConfiguration.loadConfiguration(file)
    }

    fun createPaused(playerId: UUID, target: RecipeKey, steps: List<AutoCraftStep>): Boolean {
        val base = base(playerId)
        yaml.set(base, null)
        yaml.set("$base.target.category", target.categoryId)
        yaml.set("$base.target.recipe", target.recipeId)
        yaml.set("$base.state", "PAUSED")
        writeSteps(base, steps)
        return save()
    }

    fun loadPaused(playerId: UUID): PersistedQueue? {
        val base = base(playerId)
        if (yaml.getString("$base.state") != "PAUSED") return null
        val category = yaml.getString("$base.target.category") ?: return null
        val recipe = yaml.getString("$base.target.recipe") ?: return null
        val section = yaml.getConfigurationSection("$base.steps") ?: return null
        val steps = section.getKeys(false).mapNotNull { index ->
            val step = section.getConfigurationSection(index) ?: return@mapNotNull null
            val stepCategory = step.getString("category") ?: return@mapNotNull null
            val stepRecipe = step.getString("recipe") ?: return@mapNotNull null
            val crafts = step.getInt("remaining")
            if (crafts <= 0) null else index.toIntOrNull()?.let { it to AutoCraftStep(RecipeKey.of(stepCategory, stepRecipe), crafts) }
        }.sortedBy { it.first }.map { it.second }
        return PersistedQueue(playerId, RecipeKey.of(category, recipe), steps)
    }

    fun markInFlight(playerId: UUID): Boolean = transition(playerId, "PAUSED", "IN_FLIGHT")

    fun completeAttempt(playerId: UUID, steps: List<AutoCraftStep>): Boolean {
        val base = base(playerId)
        if (yaml.getString("$base.state") != "IN_FLIGHT") return false
        if (steps.isEmpty()) {
            yaml.set(base, null)
        } else {
            yaml.set("$base.state", "PAUSED")
            yaml.set("$base.last-reason", null)
            writeSteps(base, steps)
        }
        return save()
    }

    fun abandon(playerId: UUID, reason: String): Boolean {
        val base = base(playerId)
        if (!yaml.contains(base)) return true
        yaml.set("$base.state", "ABANDONED")
        yaml.set("$base.last-reason", reason)
        return save()
    }

    fun cancelPaused(playerId: UUID): Boolean {
        val base = base(playerId)
        if (yaml.getString("$base.state") != "PAUSED") return true
        yaml.set(base, null)
        return save()
    }

    fun recoverInterruptedRuns(): Int {
        var recovered = 0
        yaml.getConfigurationSection("queues")?.getKeys(false)?.forEach { id ->
            val base = "queues.$id"
            if (yaml.getString("$base.state") == "IN_FLIGHT") {
                yaml.set("$base.state", "ABANDONED")
                yaml.set("$base.last-reason", "server-restart-during-attempt")
                recovered++
            }
        }
        return if (recovered == 0 || save()) recovered else 0
    }

    override fun close() = Unit

    private fun transition(playerId: UUID, expected: String, next: String): Boolean {
        val base = base(playerId)
        if (yaml.getString("$base.state") != expected) return false
        yaml.set("$base.state", next)
        return save()
    }

    private fun writeSteps(base: String, steps: List<AutoCraftStep>) {
        yaml.set("$base.steps", null)
        steps.forEachIndexed { index, step ->
            val stepBase = "$base.steps.$index"
            yaml.set("$stepBase.category", step.recipeKey.categoryId)
            yaml.set("$stepBase.recipe", step.recipeKey.recipeId)
            yaml.set("$stepBase.remaining", step.crafts)
        }
    }

    private fun save(): Boolean = try {
        AtomicYamlFile.save(file, yaml)
        true
    } catch (exception: Exception) {
        // Keep RAM aligned with the last durable state: dispatch must fail closed after a write failure.
        yaml = YamlConfiguration.loadConfiguration(file)
        plugin.logger.severe("Could not persist AutoCraft queues: ${exception.message}")
        false
    }

    private fun base(playerId: UUID) = "queues.$playerId"
}
