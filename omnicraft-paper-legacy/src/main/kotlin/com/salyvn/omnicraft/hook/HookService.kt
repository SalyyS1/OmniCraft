package com.salyvn.omnicraft.hook

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class HookService(private val plugin: JavaPlugin) {
    fun mmoItem(type: String?, id: String?, amount: Int): ItemStack? {
        if (!enabled("MMOItems") || type.isNullOrBlank() || id.isNullOrBlank()) return null
        return runCatching {
            val clazz = Class.forName("net.Indyuce.mmoitems.MMOItems")
            val pluginInstance = clazz.getField("plugin").get(null)
            val item = pluginInstance.javaClass.getMethod("getItem", String::class.java, String::class.java)
                .invoke(pluginInstance, type, id) as? ItemStack
            item?.also { it.amount = amount.coerceAtLeast(1) }
        }.getOrNull()
    }

    fun mmoKey(stack: ItemStack): Pair<String, String>? {
        if (!enabled("MMOItems")) return null
        return runCatching {
            val clazz = Class.forName("net.Indyuce.mmoitems.MMOItems")
            val type = clazz.getMethod("getTypeName", ItemStack::class.java).invoke(null, stack) as? String
            val id = clazz.getMethod("getID", ItemStack::class.java).invoke(null, stack) as? String
            if (type.isNullOrBlank() || id.isNullOrBlank()) null else type.uppercase() to id.uppercase()
        }.getOrNull()
    }

    fun applyAdvancedEnchant(stack: ItemStack, enchantId: String, level: Int): ItemStack {
        if (!enabled("AdvancedEnchantments")) return stack
        return runCatching {
            val clazz = Class.forName("net.advancedplugins.ae.api.AEAPI")
            clazz.getMethod("applyEnchant", String::class.java, Int::class.javaPrimitiveType, ItemStack::class.java)
                .invoke(null, enchantId, level, stack) as? ItemStack ?: stack
        }.getOrDefault(stack)
    }

    fun advancedEnchantments(stack: ItemStack): Map<String, Int> {
        if (!enabled("AdvancedEnchantments")) return emptyMap()
        return runCatching {
            val clazz = Class.forName("net.advancedplugins.ae.api.AEAPI")
            val methods = listOf("getEnchantments", "getEnchantmentsOnItem", "getEnchantmentsFromItem")
            for (methodName in methods) {
                val method = clazz.methods.firstOrNull { it.name == methodName && it.parameterTypes.contentEquals(arrayOf(ItemStack::class.java)) }
                val result = method?.invoke(null, stack) ?: continue
                val parsed = parseEnchantResult(result)
                if (parsed.isNotEmpty()) return@runCatching parsed
            }
            emptyMap()
        }.getOrDefault(emptyMap())
    }

    fun giveAdvancedEnchantBook(player: Player, enchantId: String, level: Int, successRate: Double, destroyRate: Double): Boolean {
        if (!enabled("AdvancedEnchantments")) return false
        val command = "ae givebook ${player.name} $enchantId ${level.coerceAtLeast(1)} 1 ${successRate.coerceIn(0.0, 100.0)} ${destroyRate.coerceIn(0.0, 100.0)}"
        return runCatching {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender() as CommandSender, command)
        }.getOrDefault(false)
    }

    fun balance(player: Player): Double {
        val economy = economyProvider() ?: return 0.0
        return runCatching { economy.javaClass.getMethod("getBalance", org.bukkit.OfflinePlayer::class.java).invoke(economy, player) as Double }
            .getOrDefault(0.0)
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        if (amount <= 0.0) return true
        val economy = economyProvider() ?: return false
        return runCatching {
            val response = economy.javaClass.getMethod("withdrawPlayer", org.bukkit.OfflinePlayer::class.java, Double::class.javaPrimitiveType)
                .invoke(economy, player, amount)
            response.javaClass.getMethod("transactionSuccess").invoke(response) as Boolean
        }.getOrDefault(false)
    }

    fun deposit(player: Player, amount: Double) {
        if (amount <= 0.0) return
        val economy = economyProvider() ?: return
        runCatching {
            economy.javaClass.getMethod("depositPlayer", org.bukkit.OfflinePlayer::class.java, Double::class.javaPrimitiveType)
                .invoke(economy, player, amount)
        }
    }

    fun deniedConditions(player: Player, conditions: List<String>): List<String> {
        if (conditions.isEmpty()) return emptyList()
        return conditions.filterNot { evaluateCondition(player, it) }
    }

    fun enabled(name: String): Boolean {
        return Bukkit.getPluginManager().isPluginEnabled(name)
    }

    private fun parseEnchantResult(result: Any): Map<String, Int> {
        if (result is Map<*, *>) {
            return result.mapNotNull { (key, value) ->
                val id = key?.toString() ?: return@mapNotNull null
                val level = when (value) {
                    is Number -> value.toInt()
                    else -> value?.toString()?.toIntOrNull() ?: 1
                }
                id to level
            }.toMap()
        }
        if (result is Iterable<*>) {
            return result.mapNotNull { value ->
                val text = value?.toString() ?: return@mapNotNull null
                val parts = text.split(":", " ", limit = 2)
                val id = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                id to (parts.getOrNull(1)?.toIntOrNull() ?: 1)
            }.toMap()
        }
        return emptyMap()
    }

    private fun economyProvider(): Any? {
        if (!enabled("Vault")) return null
        return runCatching {
            val economyClass = Class.forName("net.milkbowl.vault.economy.Economy")
            plugin.server.servicesManager.getRegistration(economyClass)?.provider
        }.getOrNull()
    }

    private fun evaluateCondition(player: Player, raw: String): Boolean {
        val parsed = if (enabled("PlaceholderAPI")) {
            runCatching {
                Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                    .getMethod("setPlaceholders", Player::class.java, String::class.java)
                    .invoke(null, player, raw) as String
            }.getOrDefault(raw)
        } else {
            raw
        }
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
