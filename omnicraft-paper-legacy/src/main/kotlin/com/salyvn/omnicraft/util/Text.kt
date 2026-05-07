package com.salyvn.omnicraft.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object Text {
    private val mini = MiniMessage.miniMessage()
    private val legacyAmpersand = LegacyComponentSerializer.legacyAmpersand()
    private val legacySection = LegacyComponentSerializer.legacySection()

    fun c(input: String): Component {
        val component = when {
            input.contains('§') -> legacySection.deserialize(input)
            input.contains('&') && !input.contains('#') -> legacyAmpersand.deserialize(input)
            else -> mini.deserialize(convertHex(input))
        }
        return component.decoration(TextDecoration.ITALIC, false)
    }

    private fun convertHex(input: String): String {
        return Regex("#[A-Fa-f0-9]{6}").replace(input) { "<${it.value}>" }
    }
}
