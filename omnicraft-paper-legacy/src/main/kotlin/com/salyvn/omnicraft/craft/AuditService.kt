package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.core.CraftRecipe
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.time.Instant

class AuditService(private val plugin: JavaPlugin) {
    private val file = File(plugin.dataFolder, "logs/craft-history.log")

    fun record(player: Player, recipe: CraftRecipe, amount: Int, result: String, reason: String = "") {
        if (!plugin.config.getBoolean("history.enabled", true)) return
        file.parentFile.mkdirs()
        val line = listOf(
            Instant.now().toString(),
            "player=${player.name}",
            "uuid=${player.uniqueId}",
            "recipe=${recipe.categoryId}:${recipe.id}",
            "amount=$amount",
            "result=$result",
            "reason=${reason.replace('|', '/')}"
        ).joinToString(" | ")
        file.appendText(line + System.lineSeparator())
    }
}
