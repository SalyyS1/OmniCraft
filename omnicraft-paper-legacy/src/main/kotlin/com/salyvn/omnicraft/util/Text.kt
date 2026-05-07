package com.salyvn.omnicraft.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.ChatColor

object Text {
    private val mini = MiniMessage.miniMessage()

    fun c(input: String): Component {
        val normalized = input.replace('&', '§')
        val legacy = ChatColor.translateAlternateColorCodes('§', normalized)
        return mini.deserialize(convertHex(legacy)).decoration(TextDecoration.ITALIC, false)
    }

    private fun convertHex(input: String): String {
        return Regex("#[A-Fa-f0-9]{6}").replace(input) { "<${it.value}>" }
    }
}
