package com.salyvn.omnicraft.hook

import me.clip.placeholderapi.PlaceholderAPI
import net.Indyuce.mmoitems.MMOItems
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class HookService(private val plugin: JavaPlugin) {
    private val economy: Economy? by lazy {
        if (!enabled("Vault")) return@lazy null
        plugin.server.servicesManager.getRegistration(Economy::class.java)?.provider
    }

    fun mmoItem(type: String?, id: String?, amount: Int): ItemStack? {
        if (!enabled("MMOItems") || type.isNullOrBlank() || id.isNullOrBlank()) return null
        return runCatching { MMOItems.plugin.getItem(type, id)?.also { it.amount = amount.coerceAtLeast(1) } }.getOrNull()
    }

    fun mmoKey(stack: ItemStack): Pair<String, String>? {
        if (!enabled("MMOItems")) return null
        return runCatching {
            val type = MMOItems.getTypeName(stack) ?: return@runCatching null
            val id = MMOItems.getID(stack) ?: return@runCatching null
            type.uppercase() to id.uppercase()
        }.getOrNull()
    }

    fun balance(player: Player): Double {
        return economy?.getBalance(player) ?: 0.0
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        if (amount <= 0.0) return true
        val eco = economy ?: return false
        val result = eco.withdrawPlayer(player, amount)
        return result.transactionSuccess()
    }

    fun deposit(player: Player, amount: Double) {
        if (amount > 0.0) economy?.depositPlayer(player, amount)
    }

    fun deniedConditions(player: Player, conditions: List<String>): List<String> {
        if (conditions.isEmpty()) return emptyList()
        return conditions.filterNot { evaluateCondition(player, it) }
    }

    fun enabled(name: String): Boolean {
        return Bukkit.getPluginManager().isPluginEnabled(name)
    }

    private fun evaluateCondition(player: Player, raw: String): Boolean {
        val parsed = if (enabled("PlaceholderAPI")) PlaceholderAPI.setPlaceholders(player, raw) else raw
        val trimmed = parsed.trim()
        if (trimmed.equals("true", true) || trimmed.equals("yes", true) || trimmed == "1") return true
        if (trimmed.equals("false", true) || trimmed.equals("no", true) || trimmed == "0") return false
        val match = Regex("""^(.+?)\s*(>=|<=|==|>|<|!=)\s*(.+)$""").matchEntire(trimmed) ?: return false
        val left = match.groupValues[1].trim()
        val op = match.groupValues[2]
        val right = match.groupValues[3].trim()
        val leftNumber = left.toDoubleOrNull()
        val rightNumber = right.toDoubleOrNull()
        if (leftNumber != null && rightNumber != null) {
            return when (op) {
                ">=" -> leftNumber >= rightNumber
                "<=" -> leftNumber <= rightNumber
                ">" -> leftNumber > rightNumber
                "<" -> leftNumber < rightNumber
                "==" -> leftNumber == rightNumber
                "!=" -> leftNumber != rightNumber
                else -> false
            }
        }
        return when (op) {
            "==" -> left.equals(right, true)
            "!=" -> !left.equals(right, true)
            else -> false
        }
    }
}
