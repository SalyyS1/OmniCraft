package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.config.ConfigService
import com.salyvn.omnicraft.core.CraftCalculator
import com.salyvn.omnicraft.core.CraftClickMode
import com.salyvn.omnicraft.core.CraftDurationPolicy
import com.salyvn.omnicraft.core.CraftJobStopReason
import com.salyvn.omnicraft.core.CraftLocks
import com.salyvn.omnicraft.core.CraftMatcher
import com.salyvn.omnicraft.core.CraftOutcomeResolver
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.core.ExtractionMode
import com.salyvn.omnicraft.core.RecipeKey
import com.salyvn.omnicraft.hook.HookService
import com.salyvn.omnicraft.item.ItemAdapter
import com.salyvn.omnicraft.util.Text
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class CraftService(
    private val plugin: OmniCraftPlugin,
    private val config: ConfigService,
    private val hooks: HookService,
    private val usage: UsageService,
    private val audit: AuditService
) {
    private val calculator = CraftCalculator()
    private val speedProvider = CraftSpeedProvider(plugin, hooks)
    private val stations = CraftStationService()
    private val outcomes = CraftOutcomeResolver()
    private val jobs = CraftJobService(plugin)
    private val locks = CraftLocks(plugin.config.getLong("anti-dupe.lock-timeout-ms", 10_000).coerceAtLeast(1))

    fun isBusy(playerId: java.util.UUID): Boolean = jobs.activeJob(playerId) != null

    fun isServerLoadSafe(): Boolean {
        val minimumTps = plugin.config.getDouble("anti-dupe.minimum-tps", 16.0).coerceIn(1.0, 20.0)
        val tps = plugin.server.tps.firstOrNull() ?: return true
        return !tps.isFinite() || tps >= minimumTps
    }

    fun craft(player: Player, recipe: CraftRecipe, mode: CraftClickMode, reopen: () -> Unit = {}) {
        if (!isServerLoadSafe()) {
            player.sendMessage(Text.c(config.message("errors.server-busy", "#ff6961Crafting is paused while the server is busy.")))
            return
        }
        if (!locks.throttle(player.uniqueId, recipe.craft.cooldownMillis)) {
            player.sendMessage(Text.c(config.message("errors.too-fast", "#ff6961Please slow down.")))
            return
        }
        if (plugin.config.getBoolean("anti-dupe.block-creative", true) && player.gameMode in setOf(GameMode.CREATIVE, GameMode.SPECTATOR)) {
            player.sendMessage(Text.c(config.message("errors.blocked-gamemode", "#ff6961You cannot craft in this game mode.")))
            return
        }
        val recipeKey = RecipeKey.of(recipe)
        if (!locks.tryLock(player.uniqueId, recipeKey)) {
            player.sendMessage(Text.c(config.message("errors.locked", "#ff6961This recipe is already processing.")))
            return
        }
        if (jobs.activeJob(player.uniqueId) != null) {
            locks.unlock(player.uniqueId, recipeKey)
            player.sendMessage(Text.c(config.message("errors.locked", "#ff6961This recipe is already processing.")))
            return
        }
        if (recipe.craftTime.enabled && !player.hasPermission("omnicraft.bypass.craft-time")) {
            val initialCheck = check(player, recipe)
            val requested = calculator.requestedAmount(recipe, mode, initialCheck.craftableAmount)
            if (!recipe.options.enabled || !initialCheck.allowed || requested <= 0) {
                locks.unlock(player.uniqueId, recipeKey)
                player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                return
            }
            val duration = CraftDurationPolicy.calculate(recipe.craftTime, requested, speedProvider.modifiers(player, recipe))
            startCountdown(player, recipe, mode, duration.effectiveSeconds, reopen)
            return
        }
        runTransaction(player, recipe, mode, reopen)
    }

    fun shutdown() {
        jobs.shutdown()
        locks.clear()
    }

    fun cancelPlayer(playerId: java.util.UUID, reason: CraftJobStopReason = CraftJobStopReason.PLAYER_CANCELLED) {
        jobs.cancel(playerId, reason)
        locks.unlockPlayer(playerId)
    }

    fun cancelAll(reason: CraftJobStopReason = CraftJobStopReason.PLAYER_CANCELLED) {
        jobs.cancelAll(reason)
        locks.clear()
    }

    fun cancelOnMove(playerId: java.util.UUID) {
        val job = jobs.activeJob(playerId) ?: return
        val recipe = config.recipe(job.recipeKey.categoryId, job.recipeKey.recipeId) ?: return cancelPlayer(playerId, CraftJobStopReason.MOVED)
        if (recipe.craftTime.cancelOnMove) cancelPlayer(playerId, CraftJobStopReason.MOVED)
    }

    fun cancelOnLogout(playerId: java.util.UUID) {
        val job = jobs.activeJob(playerId) ?: return
        val recipe = config.recipe(job.recipeKey.categoryId, job.recipeKey.recipeId) ?: return cancelPlayer(playerId, CraftJobStopReason.LOGOUT)
        if (recipe.craftTime.cancelOnLogout) cancelPlayer(playerId, CraftJobStopReason.LOGOUT)
    }

    /** Runs one exact queue node through the same transaction coordinator as GUI crafting. */
    fun executeAutomated(player: Player, recipe: CraftRecipe, crafts: Int, completed: (Boolean) -> Unit): Boolean {
        if (!isServerLoadSafe()) return false
        if (plugin.config.getBoolean("anti-dupe.block-creative", true) && player.gameMode in setOf(GameMode.CREATIVE, GameMode.SPECTATOR)) return false
        if (crafts <= 0 || !locks.tryLock(player.uniqueId, RecipeKey.of(recipe))) return false
        if (recipe.craftTime.enabled && !player.hasPermission("omnicraft.bypass.craft-time")) {
            val check = check(player, recipe)
            if (!recipe.options.enabled || !check.allowed || check.craftableAmount < crafts) {
                locks.unlock(player.uniqueId, RecipeKey.of(recipe))
                completed(false)
                return true
            }
            val duration = CraftDurationPolicy.calculate(recipe.craftTime, crafts, speedProvider.modifiers(player, recipe))
            startCountdown(player, recipe, CraftClickMode.LEFT, duration.effectiveSeconds, {}, crafts, completed)
        } else {
            runTransaction(player, recipe, CraftClickMode.LEFT, {}, crafts, completed)
        }
        return true
    }

    private fun startCountdown(
        player: Player,
        recipe: CraftRecipe,
        mode: CraftClickMode,
        seconds: Int,
        reopen: () -> Unit,
        requestedCrafts: Int? = null,
        completed: (Boolean) -> Unit = {}
    ) {
        player.closeInventory()
        val requested = requestedCrafts ?: calculator.requestedAmount(recipe, mode, check(player, recipe).craftableAmount).coerceAtLeast(1)
        val job = jobs.start(player.uniqueId, RecipeKey.of(recipe), requested, seconds, tick@{ remaining ->
            if (!player.isOnline) {
                cancelPlayer(player.uniqueId, CraftJobStopReason.LOGOUT)
                return@tick
            }
            player.showTitle(net.kyori.adventure.title.Title.title(
                Text.c(config.message("titles.crafting", "#7cf5ffCrafting")),
                Text.c(config.message("titles.countdown", "#d6f7ffFinishing in {seconds}s").replace("{seconds}", remaining.toString()))
            ))
        }, {
            runTransaction(player, recipe, mode, reopen, requested, completed)
        }, {
            locks.unlock(player.uniqueId, RecipeKey.of(recipe))
            completed(false)
            player.sendMessage(Text.c(config.message("titles.cancelled", "#ff6961Craft cancelled")))
            plugin.server.scheduler.runTask(plugin, Runnable { reopen() })
        })
        if (job == null) {
            locks.unlock(player.uniqueId, RecipeKey.of(recipe))
            completed(false)
            player.sendMessage(Text.c(config.message("errors.locked", "#ff6961This recipe is already processing.")))
        }
    }

    private fun runTransaction(
        player: Player,
        recipe: CraftRecipe,
        mode: CraftClickMode,
        reopen: () -> Unit,
        requestedCrafts: Int? = null,
        completed: (Boolean) -> Unit = {}
    ) {
        var usageReservation: UsageService.Reservation? = null
        var reservedAmount = 0
        var moneyWithdrawn = false
        var committed = false
        var chargedMoney = 0.0
        var inventoryBeforeMutation: Array<ItemStack?>? = null
        try {
            if (!isServerLoadSafe()) {
                audit.record(player, recipe, 0, "fail", "server-busy")
                player.sendMessage(Text.c(config.message("errors.server-busy", "#ff6961Crafting is paused while the server is busy.")))
                return
            }
            val check = check(player, recipe)
            val crafts = requestedCrafts ?: calculator.requestedAmount(recipe, mode, check.craftableAmount)
            if (!recipe.options.enabled) {
                audit.record(player, recipe, 0, "fail", "disabled")
                player.sendMessage(Text.c(config.message("errors.recipe-disabled", "#ff6961This recipe is disabled.")))
                return
            }
            if (!check.allowed || crafts <= 0) {
                audit.record(player, recipe, 0, "fail", "requirements")
                player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                return
            }
            val reservation = usage.reserve(player, recipe, crafts) ?: run {
                audit.record(player, recipe, crafts, "fail", "limit")
                player.sendMessage(Text.c(config.message("errors.limit", "#ff6961Craft limit reached.")))
                return
            }
            usageReservation = reservation
            reservedAmount = crafts

            val inventory = ItemAdapter.inventoryEntries(player.inventory.storageContents)
            val quote = calculator.quote(recipe, inventory, crafts, hooks.balance(player))
            if (!quote.isExecutable || quote.allowedCrafts != crafts) {
                audit.record(player, recipe, crafts, "fail", "allocation")
                player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                usage.release(reservation)
                return
            }
            val plan = quote.allocation
            inventoryBeforeMutation = player.inventory.storageContents.map { it?.clone() }.toTypedArray()
            val removed = mutableListOf<Pair<Int, ItemStack>>()
            val extractedEnchants = mutableListOf<ExtractedAdvancedEnchant>()
            val keptEnchantGroups = mutableListOf<List<ExtractedAdvancedEnchant>>()
            chargedMoney = quote.totalMoney
            val maximumOutcome = outcomes.maximum(recipe, crafts)
            val maximumPrimaryOutput = outputStacks(recipe, maximumOutcome.outputCrafts - maximumOutcome.qualityCrafts, emptyList())
            val maximumQualityOutput = qualityOutputStacks(recipe, maximumOutcome.qualityCrafts)
            val maximumByproducts = byproductStacks(recipe, maximumOutcome.byproducts)
            val output = if (maximumPrimaryOutput == null || maximumQualityOutput == null || maximumByproducts == null) null
            else maximumPrimaryOutput + maximumQualityOutput + maximumByproducts
            val conservativeKeepSlots = 0
            val projectedStorage = player.inventory.storageContents.map { it?.clone() }.toTypedArray()
            plan.values.flatten().forEach { entry ->
                projectedStorage[entry.slot]?.let { stack ->
                    stack.amount -= entry.amount
                    if (stack.amount <= 0) projectedStorage[entry.slot] = null
                }
            }
            if (output == null || !hasOutputCapacity(projectedStorage, output, conservativeKeepSlots)) {
                audit.record(player, recipe, crafts, "fail", "inventory-full")
                player.sendMessage(Text.c(config.message("errors.inventory-full", "#ff6961Inventory full. Craft cancelled.")))
                usage.release(reservation)
                return
            }
            for ((ingredientId, entries) in plan) {
                val ingredient = recipe.consumedInputs().firstOrNull { it.id == ingredientId }
                if (ingredient == null) {
                    audit.record(player, recipe, crafts, "fail", "invalid-allocation")
                    usage.release(reservation)
                    return
                }
                for (entry in entries) {
                    val stack = player.inventory.getItem(entry.slot)
                    val currentEntry = stack?.let { ItemAdapter.inventoryEntries(arrayOf(it)).firstOrNull() }
                    if (stack == null || stack.amount < entry.amount || currentEntry == null ||
                        !CraftMatcher().matches(ingredient.item, currentEntry) ||
                        currentEntry.key != entry.key || currentEntry.risk != entry.risk
                    ) {
                        rollback(player, removed)
                        audit.record(player, recipe, crafts, "rollback", "stale-inventory")
                        player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                        usage.release(reservation)
                        return
                    }
                    val clone = stack.clone()
                    clone.amount = entry.amount
                    removed += entry.slot to clone
                    val groups = readExtractableAdvancedEnchantGroups(clone)
                    extractedEnchants += groups.flatten()
                    keptEnchantGroups += groups
                    stack.amount -= entry.amount
                    player.inventory.setItem(entry.slot, if (stack.amount <= 0) null else stack)
                }
            }
            if (!hooks.withdraw(player, chargedMoney)) {
                rollback(player, removed)
                audit.record(player, recipe, crafts, "rollback", "money")
                player.sendMessage(Text.c(config.message("errors.requirements", "#ff6961You do not meet the requirements.")))
                usage.release(reservation)
                return
            }
            moneyWithdrawn = true

            val outcome = outcomes.resolve(recipe, crafts, Random.nextLong())
            val primaryOutput = outputStacks(recipe, outcome.outputCrafts - outcome.qualityCrafts, keptEnchantGroups)
            val qualityOutput = qualityOutputStacks(recipe, outcome.qualityCrafts)
            val byproducts = byproductStacks(recipe, outcome.byproducts)
            val enrichedOutput = if (primaryOutput == null || qualityOutput == null || byproducts == null) null
            else primaryOutput + qualityOutput + byproducts
            if (enrichedOutput == null) {
                rollback(player, removed)
                plugin.pendingRefunds.refundOrQueue(player, chargedMoney)
                audit.record(player, recipe, crafts, "rollback", "output-hook")
                player.sendMessage(Text.c(config.message("errors.missing-hook", "#ff6961Required hook is missing: {hook}").replace("{hook}", "MMOItems")))
                usage.release(reservation)
                return
            }
            val leftover = player.inventory.addItem(*enrichedOutput.toTypedArray())
            if (leftover.isNotEmpty()) {
                player.inventory.storageContents = inventoryBeforeMutation
                plugin.pendingRefunds.refundOrQueue(player, chargedMoney)
                audit.record(player, recipe, crafts, "rollback", "inventory-full")
                player.sendMessage(Text.c(config.message("errors.inventory-full", "#ff6961Inventory full. Craft cancelled.")))
                usage.release(reservation)
                return
            }

            committed = true
            grantAuraSkillsXp(player, recipe, crafts)
            handleAdvancedEnchantExtraction(player, recipe, extractedEnchants)
            audit.record(player, recipe, crafts, "success", "seed=${outcome.seed},critical=${outcome.criticalCrafts},quality=${outcome.qualityCrafts},byproduct=${outcome.byproducts}")
            if (recipe.options.rareBroadcast && plugin.config.getBoolean("history.log-rare-recipes", true)) {
                plugin.server.broadcast(Text.c(config.message("broadcast.rare-craft", "#ffd166{player} crafted {amount}x {item}.")
                    .replace("{player}", player.name)
                    .replace("{amount}", crafts.toString())
                    .replace("{item}", recipe.displayName)))
            }
            plugin.logger.info("craft success player=${player.name} recipe=${recipe.categoryId}:${recipe.id} amount=$crafts")
            player.sendMessage(Text.c(config.message("success.crafted", "#71f79fCrafted {amount}x {item}.")
                .replace("{amount}", outcome.outputCrafts.toString())
                .replace("{item}", recipe.displayName)))
        } catch (exception: Exception) {
            if (!committed) {
                inventoryBeforeMutation?.let { player.inventory.storageContents = it }
                if (moneyWithdrawn) plugin.pendingRefunds.refundOrQueue(player, chargedMoney)
                usageReservation?.let { usage.release(it) }
                runCatching { audit.record(player, recipe, 0, "rollback", "exception") }
            }
            plugin.logger.severe("craft transaction exception player=${player.name} recipe=${recipe.categoryId}:${recipe.id}: ${exception.message}")
        } finally {
            runCatching { completed(committed) }
                .onFailure { plugin.logger.warning("craft completion callback failed: ${it.message}") }
            locks.unlock(player.uniqueId, RecipeKey.of(recipe))
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
            .plus(missingHookFailures(recipe))
            .plus(auraSkillsFailures(player, recipe))
            .plus(outcomeFailures(recipe))
            .plus(stations.failure(player, recipe)?.let(::listOf).orEmpty())
    )

    private fun auraSkillsFailures(player: Player, recipe: CraftRecipe): List<String> {
        val policy = recipe.auraSkills
        val skill = policy.skill ?: return emptyList()
        val level = hooks.auraSkillsLevel(player, skill) ?: return listOf("AuraSkills:$skill")
        return if (level < policy.minimumLevel) listOf("AuraSkills:$skill requires ${policy.minimumLevel}") else emptyList()
    }

    private fun grantAuraSkillsXp(player: Player, recipe: CraftRecipe, crafts: Int) {
        val policy = recipe.auraSkills
        val skill = policy.skill ?: return
        val amount = policy.experience * crafts
        if (amount > 0.0) plugin.pendingAuraXp.queue(player, "${recipe.categoryId}:${recipe.id}", skill, amount)
    }

    private fun missingHookFailures(recipe: CraftRecipe): List<String> {
        val requiresAe = recipe.output.advancedEnchantments.isNotEmpty() ||
            recipe.ingredients.any { it.item.advancedEnchantments.isNotEmpty() }
        val failures = mutableListOf<String>()
        if (recipe.output.mode == com.salyvn.omnicraft.core.ItemMode.MMOITEMS && !hooks.enabled("MMOItems")) failures += "MMOItems"
        val strict = plugin.config.getBoolean("advanced-enchantments.missing-hook-disables-ae-recipes", false)
        if (requiresAe && strict && !hooks.enabled("AdvancedEnchantments")) failures += "AdvancedEnchantments"
        return failures
    }

    private fun outcomeFailures(recipe: CraftRecipe): List<String> {
        val quality = recipe.outcome.quality
        if (!quality.name.isNullOrBlank() && recipe.extraction.enchant == ExtractionMode.KEEP) {
            return listOf("quality cannot use enchant KEEP")
        }
        return emptyList()
    }

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

    private fun hasOutputCapacity(storageContents: Array<ItemStack?>, output: List<ItemStack>, reservedSeparateSlots: Int = 0): Boolean {
        val free = storageContents.map { it?.clone() }.toMutableList()
        if (reservedSeparateSlots > 0) {
            val emptySlots = free.count { it == null || it.type.isAir }
            if (emptySlots < reservedSeparateSlots) return false
        }
        for (stack in output) {
            var remaining = stack.amount
            for (index in free.indices) {
                val current = free[index] ?: continue
                if (!current.isSimilar(stack)) continue
                val room = (current.maxStackSize - current.amount).coerceAtLeast(0)
                val moved = minOf(room, remaining)
                current.amount += moved
                remaining -= moved
                if (remaining == 0) break
            }
            for (index in free.indices) {
                if (remaining == 0) break
                if (free[index] != null && !free[index]!!.type.isAir) continue
                val moved = minOf(stack.maxStackSize.coerceAtLeast(1), remaining)
                free[index] = stack.clone().apply { amount = moved }
                remaining -= moved
            }
            if (remaining > 0) return false
        }
        return true
    }

    private fun outputStacks(recipe: CraftRecipe, crafts: Int, keptEnchantGroups: List<List<ExtractedAdvancedEnchant>>): List<ItemStack>? {
        var remaining = try {
            Math.multiplyExact(recipe.output.amount, crafts)
        } catch (_: ArithmeticException) {
            return null
        }
        val base = ItemAdapter.tryFromCraftItem(recipe.output, 1) ?: return null
        val maxStack = base.maxStackSize.coerceAtLeast(1)
        val stacks = mutableListOf<ItemStack>()
        var keepIndex = 0
        while (remaining > 0) {
            if (recipe.extraction.enchant == ExtractionMode.KEEP && keepIndex < keptEnchantGroups.size) {
                val group = keptEnchantGroups[keepIndex++]
                val stack = ItemAdapter.tryFromCraftItem(recipe.output, 1) ?: return null
                stacks += if (group.isEmpty()) stack else applyKeptAdvancedEnchants(stack, group)
                remaining--
                continue
            }
            val take = minOf(remaining, maxStack)
            stacks += ItemAdapter.tryFromCraftItem(recipe.output, take) ?: return null
            remaining -= take
        }
        return stacks
    }

    private fun byproductStacks(recipe: CraftRecipe, count: Int): List<ItemStack>? {
        if (count <= 0) return emptyList()
        val byproduct = recipe.outcome.byproduct ?: return emptyList()
        var remaining = try {
            Math.multiplyExact(byproduct.amount, count)
        } catch (_: ArithmeticException) {
            return null
        }
        val base = ItemAdapter.tryFromCraftItem(byproduct, 1) ?: return null
        val maxStack = base.maxStackSize.coerceAtLeast(1)
        val stacks = mutableListOf<ItemStack>()
        while (remaining > 0) {
            val take = minOf(remaining, maxStack)
            stacks += ItemAdapter.tryFromCraftItem(byproduct, take) ?: return null
            remaining -= take
        }
        return stacks
    }

    private fun qualityOutputStacks(recipe: CraftRecipe, crafts: Int): List<ItemStack>? {
        if (crafts <= 0) return emptyList()
        val quality = recipe.outcome.quality
        if (quality.name.isNullOrBlank()) return null
        var remaining = try {
            Math.multiplyExact(recipe.output.amount, crafts)
        } catch (_: ArithmeticException) {
            return null
        }
        val base = ItemAdapter.tryFromCraftItem(recipe.output, 1) ?: return null
        val maxStack = base.maxStackSize.coerceAtLeast(1)
        val stacks = mutableListOf<ItemStack>()
        while (remaining > 0) {
            val take = minOf(remaining, maxStack)
            val stack = ItemAdapter.tryFromCraftItem(recipe.output, take) ?: return null
            stacks += ItemAdapter.applyQuality(stack, quality)
            remaining -= take
        }
        return stacks
    }

    private fun readExtractableAdvancedEnchantGroups(item: ItemStack): List<List<ExtractedAdvancedEnchant>> {
        val enchants = hooks.advancedEnchantments(item)
        if (enchants.isEmpty()) return emptyList()
        return (1..item.amount.coerceAtLeast(1)).map {
            enchants.map { (id, level) -> ExtractedAdvancedEnchant(id, level) }
        }
    }

    private fun handleAdvancedEnchantExtraction(player: Player, recipe: CraftRecipe, enchants: List<ExtractedAdvancedEnchant>) {
        if (enchants.isEmpty()) return
        when (recipe.extraction.enchant) {
            ExtractionMode.DESTROY -> {
                player.sendMessage(Text.c(config.message("risk.advanced-destroyed", "#ff6961AdvancedEnchantments on consumed materials were destroyed.")))
            }
            ExtractionMode.KEEP -> {
                player.sendMessage(Text.c(config.message("risk.advanced-kept", "#71f79fAdvancedEnchantments were kept on crafted output.")))
            }
            ExtractionMode.EXTRACT -> {
                var success = 0
                var failed = 0
                for (enchant in enchants) {
                    val chance = extractionSuccessRate(enchant.id)
                    if (Random.nextDouble(100.0) <= chance && hooks.giveAdvancedEnchantBook(player, enchant.id, enchant.level, bookSuccessRate(enchant.id), bookDestroyRate(enchant.id))) {
                        success++
                    } else {
                        failed++
                    }
                }
                player.sendMessage(Text.c(config.message("risk.advanced-extracted", "#71f79fExtracted {success} AdvancedEnchantments. Failed {failed}.")
                    .replace("{success}", success.toString())
                    .replace("{failed}", failed.toString())))
            }
        }
    }

    private fun extractionSuccessRate(enchantId: String): Double {
        return plugin.config.getDouble("advanced-enchantments.extraction.per-enchant.$enchantId.extract-rate",
            plugin.config.getDouble("advanced-enchantments.extraction.extract-rate", 100.0)
        ).coerceIn(0.0, 100.0)
    }

    private fun bookSuccessRate(enchantId: String): Double {
        return plugin.config.getDouble("advanced-enchantments.extraction.per-enchant.$enchantId.book-success-rate",
            plugin.config.getDouble("advanced-enchantments.extraction.fixed-success-rate", 50.0)
        ).coerceIn(0.0, 100.0)
    }

    private fun bookDestroyRate(enchantId: String): Double {
        return plugin.config.getDouble("advanced-enchantments.extraction.per-enchant.$enchantId.book-destroy-rate",
            plugin.config.getDouble("advanced-enchantments.extraction.fixed-destroy-rate", 0.0)
        ).coerceIn(0.0, 100.0)
    }

    private fun applyKeptAdvancedEnchants(stack: ItemStack, enchants: List<ExtractedAdvancedEnchant>): ItemStack {
        var result = stack
        for (enchant in enchants) {
            result = hooks.applyAdvancedEnchant(result, enchant.id, enchant.level)
        }
        return result
    }

    private data class ExtractedAdvancedEnchant(val id: String, val level: Int)
}
