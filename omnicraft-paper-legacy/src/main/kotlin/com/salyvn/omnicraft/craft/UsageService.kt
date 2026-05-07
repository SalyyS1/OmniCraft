package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.core.CraftRecipe
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
        val daily = recipe.limits.daily
        val weekly = recipe.limits.weekly
        if (daily >= 0 && count(player, recipe, "daily.${LocalDate.now()}") + amount > daily) return false
        if (weekly >= 0 && count(player, recipe, "weekly.${weekKey()}") + amount > weekly) return false
        return true
    }

    fun record(player: Player, recipe: CraftRecipe, amount: Int) {
        add(player, recipe, "daily.${LocalDate.now()}", amount)
        add(player, recipe, "weekly.${weekKey()}", amount)
        yaml.save(file)
    }

    private fun count(player: Player, recipe: CraftRecipe, bucket: String): Int {
        return yaml.getInt(path(player, recipe, bucket), 0)
    }

    private fun add(player: Player, recipe: CraftRecipe, bucket: String, amount: Int) {
        val path = path(player, recipe, bucket)
        yaml.set(path, yaml.getInt(path, 0) + amount)
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
