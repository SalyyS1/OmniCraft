package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.config.AtomicYamlFile
import com.salyvn.omnicraft.hook.HookService
import org.bukkit.entity.Player
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.UUID

/** Durable XP delivery queue. A crash after external delivery fails closed for manual reconciliation. */
class PendingAuraXpService(private val plugin: JavaPlugin, private val hooks: HookService) {
    private val file = File(plugin.dataFolder, "data/pending-aura-xp.yml")
    private var yaml = YamlConfiguration()

    fun load() {
        file.parentFile.mkdirs()
        yaml = YamlConfiguration.loadConfiguration(file)
    }

    fun queue(player: Player, recipeKey: String, skill: String, amount: Double) {
        if (amount <= 0.0 || !amount.isFinite()) return
        val id = UUID.randomUUID().toString()
        val base = "entries.$id"
        yaml.set("$base.player", player.uniqueId.toString())
        yaml.set("$base.recipe", recipeKey)
        yaml.set("$base.skill", skill.uppercase())
        yaml.set("$base.amount", amount)
        if (!save()) {
            plugin.logger.severe("Could not persist AuraSkills XP player=${player.uniqueId} recipe=$recipeKey")
            return
        }
        retry(player)
    }

    fun retry(player: Player) {
        yaml.getConfigurationSection("entries")?.getKeys(false)?.toList()?.forEach { id ->
            val base = "entries.$id"
            if (yaml.getString("$base.player") != player.uniqueId.toString()) return@forEach
            val delivery = "delivery-in-progress.$id"
            if (yaml.contains(delivery)) {
                plugin.logger.severe("AuraSkills XP entry needs manual reconciliation id=$id player=${player.uniqueId}")
                return@forEach
            }
            val skill = yaml.getString("$base.skill") ?: return@forEach
            val amount = yaml.getDouble("$base.amount")
            yaml.set(delivery, true)
            if (!save()) return@forEach
            if (!hooks.addAuraSkillsXp(player, skill, amount)) {
                yaml.set(delivery, null)
                save()
                return@forEach
            }
            yaml.set(base, null)
            yaml.set(delivery, null)
            if (!save()) plugin.logger.severe("AuraSkills XP delivered but completion was not persisted id=$id; manual reconciliation required")
        }
    }

    fun retryOnlinePlayers() = plugin.server.onlinePlayers.forEach(::retry)

    private fun save(): Boolean = runCatching { AtomicYamlFile.save(file, yaml) }
        .onFailure { plugin.logger.severe("Unable to save pending AuraSkills XP: ${it.message}") }
        .isSuccess
}
