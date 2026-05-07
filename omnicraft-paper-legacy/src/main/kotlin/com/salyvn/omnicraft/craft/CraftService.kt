package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.core.CraftCalculator
import com.salyvn.omnicraft.core.CraftClickMode
import com.salyvn.omnicraft.core.CraftLocks
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.hook.HookService
import com.salyvn.omnicraft.item.ItemAdapter
import com.salyvn.omnicraft.util.Text
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class CraftService(private val plugin: OmniCraftPlugin, private val config: ConfigService, private val hooks: HookService) {
    private val calculator = CraftCalculator()
    private val locks = CraftLocks()
    private val countdowns = mutableMapOf<UUID, BukkitTask>()

    fun craft(player: Player, recipe: CraftRecipe, mode: CraftClickMode, reopen: () -> Unit = {}) {
        if (!locks.throttle(player.uniqueId, recipe.craft.cooldownMillis)) {
            player.sendMessage(Text.c(config.message("errors.too-fast", "#ff6961Please slow down.")))
            return
        }
        if (plugin.config.getBoolean("anti-dupe.block-creative", true) && player.gameMode in setOf(GameMode.CREATIVE, GameMode.SPECTATOR)) {
            player.sendMessage(Text.c(config.message("errors.blocked-gamemode", "#ff6961You cannot craft in this game mode.")))
            return
        }
        if (!locks.tryLock(player.uniqueId, recipe.id)) {
            player.sendMessage(Text.c(config.message("errors.locked", "#ff6961This recipe is already processing.")))
            return
        }
        if (recipe.craftTime.enabled && !player.hasPermission("omnicraft.bypass.craft-time")) {
            startCountdown(player, recipe, mode, reopen)
            return
        }
        runTransaction(player, recipe, mode, reopen)
    }

    fun shutdown() {
        countdowns.values.forEach { it.cancel() }
        countdowns.clear()
        locks.clear()
    }

    private fun startCountdown(player: Player, recipe: CraftRecipe, mode: CraftClickMode, reopen: () -> Unit) {
        var remaining = recipe.craftTime.seconds.coerceAtLeast(1)
        player.closeInventory()
        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) {
                cancelCountdown(player.uniqueId, recipe.id)
                return@Runnable
            }
            if (remaining <= 0) {
                countdowns.remove(player.uniqueId)?.cancel()
                runTransaction(player, recipe, mode, reopen)
                return@Runnable
            }
            player.showTitle(net.kyori.adventure.title.Title.title(
                Text.c(config.message("titles.crafting", "#7cf5ffCrafting")),
                Text.c(config.message("titles.countdown", "#d6f7ffFinishing in {seconds}s").replace("{seconds}", remaining.toString()))
            ))
            remaining--
        }, 0L, 20L)
        countdowns[player.uniqueId] = task
    }

    private fun cancelCountdown(playerId: UUID, recipeId: String) {
        countdowns.remove(playerId)?.cancel()
        locks.unlock(playerId, recipeId)
    }

    private fun runTransaction(player: Player, recipe: CraftRecipe, mode: CraftClickMode, reopen: () -> Unit) {
        try {
            val check = check(player, recipe)
            val crafts = calculator.requestedAmount(recipe, mode, check.craftableAmount)
            if (!check.allowed || crafts <= 0) {
                player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                return
            }

            val inventory = ItemAdapter.inventoryEntries(player.inventory.storageContents)
            val plan = calculator.selectionPlan(recipe, inventory, crafts)
            val removed = mutableListOf<Pair<Int, ItemStack>>()
            val chargedMoney = recipe.requirements.money * crafts
            for ((_, entries) in plan) {
                for (entry in entries) {
                    val stack = player.inventory.getItem(entry.slot) ?: continue
                    val clone = stack.clone()
                    clone.amount = entry.amount
                    removed += entry.slot to clone
                    stack.amount -= entry.amount
                    player.inventory.setItem(entry.slot, if (stack.amount <= 0) null else stack)
                }
            }
            if (!hooks.withdraw(player, chargedMoney)) {
                rollback(player, removed)
                player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                return
            }

            val output = outputStacks(recipe, crafts)
            val leftover = player.inventory.addItem(*output.toTypedArray())
            if (leftover.isNotEmpty()) {
                rollback(player, removed)
                hooks.deposit(player, chargedMoney)
                player.sendMessage(Text.c(config.message("errors.inventory-full", "#ff6961Inventory full. Craft cancelled.")))
                return
            }

            plugin.logger.info("craft success player=${player.name} recipe=${recipe.categoryId}:${recipe.id} amount=$crafts")
            player.sendMessage(Text.c(config.message("success.crafted", "#71f79fCrafted {amount}x {item}.")
                .replace("{amount}", crafts.toString())
                .replace("{item}", recipe.displayName)))
        } finally {
            locks.unlock(player.uniqueId, recipe.id)
            plugin.server.scheduler.runTask(plugin, Runnable { reopen() })
        }
    }

    fun check(player: Player, recipe: CraftRecipe) = calculator.check(
        recipe = recipe,
        inventory = ItemAdapter.inventoryEntries(player.inventory.storageContents),
        hasPermission = recipe.requirements.permission?.let { player.hasPermission(it) } ?: true,
        level = player.level,
        money = hooks.balance(player),
        deniedConditions = hooks.deniedConditions(player, recipe.requirements.papiConditions)
    )

    private fun rollback(player: Player, removed: List<Pair<Int, ItemStack>>) {
        for ((slot, item) in removed) {
            val current = player.inventory.getItem(slot)
            if (current == null || current.type.isAir) {
                player.inventory.setItem(slot, item)
            } else {
                player.inventory.addItem(item).values.forEach { player.world.dropItemNaturally(player.location, it) }
            }
        }
        plugin.logger.warning("craft rollback player=${player.name}")
    }

    private fun outputStacks(recipe: CraftRecipe, crafts: Int): List<ItemStack> {
        var remaining = recipe.output.amount * crafts
        val base = ItemAdapter.fromCraftItem(recipe.output, 1)
        val maxStack = base.maxStackSize.coerceAtLeast(1)
        val stacks = mutableListOf<ItemStack>()
        while (remaining > 0) {
            val take = minOf(remaining, maxStack)
            stacks += ItemAdapter.fromCraftItem(recipe.output, take)
            remaining -= take
        }
        return stacks
    }
}
