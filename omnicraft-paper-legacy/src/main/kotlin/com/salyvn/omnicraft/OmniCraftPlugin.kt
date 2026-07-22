package com.salyvn.omnicraft

import com.salyvn.omnicraft.command.OmniCraftCommand
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.craft.AuditService
import com.salyvn.omnicraft.craft.CraftService
import com.salyvn.omnicraft.craft.UsageService
import com.salyvn.omnicraft.craft.PendingRefundService
import com.salyvn.omnicraft.craft.PendingAuraXpService
import com.salyvn.omnicraft.craft.CraftQueueService
import com.salyvn.omnicraft.gui.GuiListener
import com.salyvn.omnicraft.gui.MenuService
import com.salyvn.omnicraft.hook.HookService
import com.salyvn.omnicraft.item.ItemAdapter
import org.bukkit.plugin.java.JavaPlugin

open class OmniCraftPlugin : JavaPlugin() {
    protected open val platformLine: String = "paper-legacy"

    lateinit var configService: ConfigService
        private set
    lateinit var craftService: CraftService
        private set
    lateinit var menuService: MenuService
        private set
    lateinit var hooks: HookService
        private set
    lateinit var usageService: UsageService
        private set
    lateinit var auditService: AuditService
        private set
    lateinit var pendingRefunds: PendingRefundService
        private set
    lateinit var pendingAuraXp: PendingAuraXpService
        private set
    lateinit var craftQueueService: CraftQueueService
        private set

    override fun onEnable() {
        saveDefaultConfig()
        saveResource("messages.yml", false)
        saveResource("category/weapons/steel_sword.yml", false)

        configService = ConfigService(this)
        configService.reload()
        hooks = HookService(this)
        ItemAdapter.hooks = hooks
        usageService = UsageService(this)
        usageService.load()
        auditService = AuditService(this)
        pendingRefunds = PendingRefundService(this, hooks)
        pendingRefunds.load()
        pendingAuraXp = PendingAuraXpService(this, hooks)
        pendingAuraXp.load()
        server.scheduler.runTaskTimer(this, Runnable {
            pendingRefunds.retryOnlinePlayers()
            pendingAuraXp.retryOnlinePlayers()
        }, 20L * 60, 20L * 60)
        craftService = CraftService(this, configService, hooks, usageService, auditService)
        craftQueueService = CraftQueueService(this, configService, craftService)
        menuService = MenuService(configService, craftService, hooks)

        val command = OmniCraftCommand(this, configService, menuService, craftService)
        listOf("omnicraft", "oc", "ocraft", "craft").forEach { name ->
            getCommand(name)?.setExecutor(command)
            getCommand(name)?.tabCompleter = command
        }

        server.pluginManager.registerEvents(GuiListener(this, menuService, craftService), this)
        logHookStatus()
        logger.info("OmniCraft enabled on $platformLine")
    }

    override fun onDisable() {
        if (::craftService.isInitialized) {
            craftService.shutdown()
        }
        if (::craftQueueService.isInitialized) craftQueueService.shutdown()
        logger.info("OmniCraft disabled")
    }

    fun reloadAll() {
        cancelActiveCrafting("cancelled on reload")
        reloadConfig()
        configService.reload()
    }

    fun cancelActiveCrafting(reason: String) {
        if (::craftQueueService.isInitialized) craftQueueService.cancelAll(reason)
        if (::craftService.isInitialized) craftService.cancelAll()
    }

    private fun logHookStatus() {
        val hooks = listOf(
            "MMOItems", "MythicLib", "AdvancedEnchantments", "Vault",
            "PlaceholderAPI", "AuraSkills", "OmniGemStone", "OmniEnchants", "OmniTooltips",
            "OmniMinMax", "OmniDelta", "OmniSet", "OmniLore", "OmniModifier",
            "OmniPopupPickup", "OmniTotalStats"
        )
        val status = hooks.joinToString(", ") {
            "$it=${server.pluginManager.isPluginEnabled(it)}"
        }
        logger.info("Hooks: $status")
    }
}
