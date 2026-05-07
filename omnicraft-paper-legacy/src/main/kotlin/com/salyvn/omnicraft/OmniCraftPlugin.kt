package com.salyvn.omnicraft

import com.salyvn.omnicraft.command.OmniCraftCommand
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.craft.CraftService
import com.salyvn.omnicraft.gui.GuiListener
import com.salyvn.omnicraft.gui.MenuService
import org.bukkit.plugin.java.JavaPlugin

class OmniCraftPlugin : JavaPlugin() {
    lateinit var configService: ConfigService
        private set
    lateinit var craftService: CraftService
        private set
    lateinit var menuService: MenuService
        private set

    override fun onEnable() {
        saveDefaultConfig()
        saveResource("messages.yml", false)
        saveResource("category/weapons/steel_sword.yml", false)

        configService = ConfigService(this)
        configService.reload()
        craftService = CraftService(this, configService)
        menuService = MenuService(configService, craftService)

        val command = OmniCraftCommand(this, configService, menuService, craftService)
        listOf("omnicraft", "oc", "ocraft", "craft").forEach { name ->
            getCommand(name)?.setExecutor(command)
            getCommand(name)?.tabCompleter = command
        }

        server.pluginManager.registerEvents(GuiListener(this, menuService, craftService), this)
        logHookStatus()
        logger.info("OmniCraft enabled")
    }

    override fun onDisable() {
        if (::craftService.isInitialized) {
            craftService.shutdown()
        }
        logger.info("OmniCraft disabled")
    }

    fun reloadAll() {
        reloadConfig()
        configService.reload()
    }

    private fun logHookStatus() {
        val hooks = listOf(
            "MMOItems", "MythicLib", "AdvancedEnchantments", "Vault",
            "PlaceholderAPI", "OmniGemStone", "OmniEnchants", "OmniTooltips",
            "OmniMinMax", "OmniDelta", "OmniSet", "OmniLore", "OmniModifier",
            "OmniPopupPickup", "OmniTotalStats"
        )
        val status = hooks.joinToString(", ") {
            "$it=${server.pluginManager.isPluginEnabled(it)}"
        }
        logger.info("Hooks: $status")
    }
}
