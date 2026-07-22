package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.config.AtomicYamlFile
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class UsageService(private val plugin: JavaPlugin) {
    private val file = File(plugin.dataFolder, "data/usage.yml")
    private var yaml = YamlConfiguration()

    fun load() {
        file.parentFile.mkdirs()
        yaml = YamlConfiguration.loadConfiguration(file)
    }

    fun allowed(player: Player, recipe: CraftRecipe, amount: Int): Boolean {
        return allowed(player, recipe, amount, "daily.${LocalDate.now()}", "weekly.${weekKey()}")
    }

    private fun allowed(player: Player, recipe: CraftRecipe, amount: Int, dailyBucket: String, weeklyBucket: String): Boolean {
        val daily = recipe.limits.daily
        val weekly = recipe.limits.weekly
        if (daily >= 0 && count(player, recipe, dailyBucket) + amount > daily) return false
        if (weekly >= 0 && count(player, recipe, weeklyBucket) + amount > weekly) return false
        return true
    }

    data class Reservation(val playerId: String, val categoryId: String, val recipeId: String, val dailyBucket: String, val weeklyBucket: String, val amount: Int)

    fun reserve(player: Player, recipe: CraftRecipe, amount: Int): Reservation? {
        val dailyBucket = "daily.${LocalDate.now()}"
        val weeklyBucket = "weekly.${weekKey()}"
        if (amount <= 0 || !allowed(player, recipe, amount, dailyBucket, weeklyBucket)) return null
        val reservation = Reservation(player.uniqueId.toString(), recipe.categoryId, recipe.id, dailyBucket, weeklyBucket, amount)
        add(reservation, amount)
        return try {
            AtomicYamlFile.save(file, yaml)
            reservation
        } catch (_: Exception) {
            add(reservation, -amount)
            null
        }
    }

    fun release(reservation: Reservation) {
        add(reservation, -reservation.amount)
        runCatching { AtomicYamlFile.save(file, yaml) }
    }

    private fun count(player: Player, recipe: CraftRecipe, bucket: String): Int {
        return yaml.getInt(path(player, recipe, bucket), 0)
    }

    private fun add(player: Player, recipe: CraftRecipe, bucket: String, amount: Int) {
        val path = path(player, recipe, bucket)
        yaml.set(path, yaml.getInt(path, 0) + amount)
    }

    private fun add(reservation: Reservation, amount: Int) {
        val base = "players.${reservation.playerId}.${reservation.categoryId}.${reservation.recipeId}"
        yaml.set("$base.${reservation.dailyBucket}", yaml.getInt("$base.${reservation.dailyBucket}", 0) + amount)
        yaml.set("$base.${reservation.weeklyBucket}", yaml.getInt("$base.${reservation.weeklyBucket}", 0) + amount)
    }

    private fun path(player: Player, recipe: CraftRecipe, bucket: String): String {
        return "players.${player.uniqueId}.${recipe.categoryId}.${recipe.id}.$bucket"
    }

    private fun weekKey(): String {
        val now = LocalDate.now()
        val fields = WeekFields.of(Locale.US)
        return "${now.get(fields.weekBasedYear())}-${now.get(fields.weekOfWeekBasedYear())}"
    }
}
