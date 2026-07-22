package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.core.CraftDurationModifiers
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.hook.HookService
import org.bukkit.entity.Player

class CraftSpeedProvider(private val plugin: OmniCraftPlugin, private val hooks: HookService) {
    fun modifiers(player: Player, recipe: CraftRecipe): CraftDurationModifiers {
        val candidates = mutableListOf(1.0 to "base")
        plugin.config.getConfigurationSection("fast-craft.permission-tiers")?.getKeys(false)?.forEach { permission ->
            val multiplier = plugin.config.getDouble("fast-craft.permission-tiers.$permission", 1.0)
            if (player.hasPermission(permission) && multiplier.isFinite()) candidates += multiplier to "permission:$permission"
        }
        val placeholder = plugin.config.getString("fast-craft.placeholder-multiplier", "")?.trim().orEmpty()
        if (placeholder.isNotEmpty() && hooks.enabled("PlaceholderAPI")) {
            hooks.resolvePlaceholders(player, placeholder).toDoubleOrNull()?.takeIf(Double::isFinite)?.let {
                candidates += it to "placeholder"
            }
        }
        val fastest = candidates.minBy { it.first }
        return CraftDurationModifiers(durationMultiplier = fastest.first, sources = listOf(fastest.second))
    }
}
