package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.config.AtomicYamlFile
import com.salyvn.omnicraft.hook.HookService
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class PendingRefundService(private val plugin: JavaPlugin, private val hooks: HookService) {
    private val file = File(plugin.dataFolder, "data/pending-refunds.yml")
    private var yaml = YamlConfiguration()

    fun load() {
        file.parentFile.mkdirs()
        yaml = YamlConfiguration.loadConfiguration(file)
    }

    fun refundOrQueue(player: Player, amount: Double): Boolean {
        if (hooks.deposit(player, amount)) return true
        val path = "players.${player.uniqueId}"
        yaml.set(path, yaml.getDouble(path, 0.0) + amount)
        if (save()) {
            plugin.logger.severe("Queued pending craft refund player=${player.uniqueId} amount=$amount")
        } else {
            plugin.logger.severe("Could not persist pending craft refund player=${player.uniqueId} amount=$amount; retrying while server remains online")
        }
        return false
    }

    fun retry(player: Player) {
        val path = "players.${player.uniqueId}"
        val amount = yaml.getDouble(path, 0.0)
        if (amount <= 0.0) return

        val deliveryPath = "delivery-in-progress.${player.uniqueId}"
        if (yaml.contains(deliveryPath)) {
            plugin.logger.severe("Pending craft refund needs manual reconciliation player=${player.uniqueId} amount=$amount")
            return
        }

        // Persist the delivery intent before touching Vault. Vault provides no idempotency key, so after a
        // crash we prefer manual reconciliation to risking a second deposit and a currency dupe.
        yaml.set(deliveryPath, amount)
        if (!save()) return

        if (!hooks.deposit(player, amount)) {
            yaml.set(deliveryPath, null)
            save()
            return
        }

        yaml.set(path, null)
        yaml.set(deliveryPath, null)
        if (save()) {
            plugin.logger.info("Delivered pending craft refund player=${player.uniqueId} amount=$amount")
        } else {
            plugin.logger.severe("Refund was delivered but completion could not be persisted player=${player.uniqueId} amount=$amount; manual reconciliation required")
        }
    }

    fun retryOnlinePlayers() {
        plugin.server.onlinePlayers.forEach(::retry)
    }

    private fun save(): Boolean = runCatching {
        AtomicYamlFile.save(file, yaml)
    }.onFailure { error ->
        plugin.logger.severe("Unable to save pending craft refunds: ${error.message}")
    }.isSuccess
}
