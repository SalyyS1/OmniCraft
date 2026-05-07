package com.salyvn.omnicraft.command

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.craft.CraftService
import com.salyvn.omnicraft.gui.MenuService
import com.salyvn.omnicraft.util.Text
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class OmniCraftCommand(
    private val plugin: OmniCraftPlugin,
    private val config: ConfigService,
    private val menus: MenuService,
    private val craft: CraftService
) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender as? Player
        if (args.isEmpty()) {
            if (player == null) return true
            if (!player.hasPermission("omnicraft.use")) {
                player.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                return true
            }
            menus.openMain(player)
            return true
        }

        when (args[0].lowercase()) {
            "open" -> {
                if (player == null) return true
                val category = args.getOrNull(1)
                if (category == null) {
                    player.sendMessage(Text.c("#ff6961Usage: /$label open <category>"))
                    return true
                }
                menus.openCategory(player, category)
            }
            "settings" -> {
                if (player == null) return true
                if (!player.hasPermission("omnicraft.settings")) {
                    player.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                menus.openSettings(player)
            }
            "browse" -> {
                if (player == null) return true
                if (!player.hasPermission("omnicraft.admin")) {
                    player.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                menus.openBrowse(player)
            }
            "reload" -> {
                if (!sender.hasPermission("omnicraft.reload")) {
                    sender.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                plugin.reloadAll()
                sender.sendMessage(Text.c(config.message("success.reload", "#71f79fOmniCraft reloaded.")))
            }
            "debug" -> {
                if (player == null) return true
                if (!player.hasPermission("omnicraft.debug")) {
                    player.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                val recipe = config.recipe(args.getOrNull(2) ?: args.getOrNull(1) ?: "")
                if (recipe == null) {
                    player.sendMessage(Text.c("#ff6961Recipe not found."))
                    return true
                }
                val check = craft.check(player, recipe)
                player.sendMessage(Text.c("#7cf5ffRecipe ${recipe.categoryId}:${recipe.id} craftable=${check.craftableAmount} missing=${check.missing}"))
            }
            else -> if (player != null) menus.openMain(player)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("open", "settings", "browse", "reload", "debug").filter { it.startsWith(args[0], true) }
            2 -> if (args[0].equals("open", true)) config.categories.map { it.id }.filter { it.startsWith(args[1], true) } else emptyList()
            else -> emptyList()
        }
    }
}
