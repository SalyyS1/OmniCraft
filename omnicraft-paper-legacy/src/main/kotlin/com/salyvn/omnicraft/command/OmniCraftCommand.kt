package com.salyvn.omnicraft.command

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.config.RecipeArchiveService
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
    private val archive = RecipeArchiveService(plugin)

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
            "validate" -> {
                if (!sender.hasPermission("omnicraft.validate")) {
                    sender.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                val issues = config.validate()
                if (issues.isEmpty()) {
                    sender.sendMessage(Text.c("#71f79fOmniCraft config looks clean."))
                } else {
                    sender.sendMessage(Text.c("#ff6961OmniCraft found ${issues.size} config issue(s):"))
                    issues.take(10).forEach { sender.sendMessage(Text.c("#ffd166- $it")) }
                }
            }
            "export" -> {
                if (!sender.hasPermission("omnicraft.admin")) {
                    sender.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                val category = args.getOrNull(1)
                if (category == null || config.category(category) == null) {
                    sender.sendMessage(Text.c("#ff6961Usage: /$label export <category>"))
                    return true
                }
                val file = archive.exportCategory(category)
                sender.sendMessage(Text.c("#71f79fExported $category to ${file.name}."))
            }
            "import" -> {
                if (!sender.hasPermission("omnicraft.admin")) {
                    sender.sendMessage(Text.c(config.message("errors.no-permission", "#ff6961No permission.")))
                    return true
                }
                val category = args.getOrNull(1)
                val fileName = args.getOrNull(2)
                if (category == null || fileName == null) {
                    sender.sendMessage(Text.c("#ff6961Usage: /$label import <category> <file.zip>"))
                    return true
                }
                val file = java.io.File(plugin.dataFolder, "exports/$fileName")
                if (!file.exists()) {
                    sender.sendMessage(Text.c("#ff6961Archive not found in exports folder."))
                    return true
                }
                archive.importCategory(category, file)
                plugin.reloadAll()
                sender.sendMessage(Text.c("#71f79fImported $category from ${file.name}."))
            }
            else -> if (player != null) menus.openMain(player)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("open", "settings", "browse", "reload", "debug", "validate", "export", "import").filter { it.startsWith(args[0], true) }
            2 -> if (args[0].equals("open", true) || args[0].equals("export", true) || args[0].equals("import", true)) {
                config.categories.map { it.id }.filter { it.startsWith(args[1], true) }
            } else emptyList()
            else -> emptyList()
        }
    }
}
