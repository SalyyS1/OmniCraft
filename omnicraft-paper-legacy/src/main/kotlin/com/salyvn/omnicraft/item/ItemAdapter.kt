package com.salyvn.omnicraft.item

import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.ItemMode
import com.salyvn.omnicraft.core.CraftQualityPolicy
import com.salyvn.omnicraft.core.InventoryEntry
import com.salyvn.omnicraft.core.ItemKey
import com.salyvn.omnicraft.core.ItemRisk
import com.salyvn.omnicraft.hook.HookService
import com.salyvn.omnicraft.util.Text
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ItemAdapter {
    var hooks: HookService? = null
    private val uidKey = NamespacedKey("omnicraft", "uid")
    private val qualityKey = NamespacedKey("omnicraft", "quality")

    fun key(stack: ItemStack): ItemKey {
        val mmo = hooks?.mmoKey(stack)
        if (mmo != null) {
            return ItemKey(ItemMode.MMOITEMS, stack.type.name, mmoType = mmo.first, mmoId = mmo.second)
        }
        val meta = stack.itemMeta
        val uid = meta?.persistentDataContainer?.get(uidKey, PersistentDataType.STRING)
        return ItemKey(ItemMode.VANILLA, stack.type.name, uid)
    }

    fun inventoryEntries(contents: Array<ItemStack?>): List<InventoryEntry> {
        return contents.mapIndexedNotNull { slot, item ->
            if (item == null || item.type.isAir || item.amount <= 0) null
            else InventoryEntry(slot, key(item), item.amount, risk(item))
        }
    }

    fun fromCraftItem(item: CraftItem, amountOverride: Int? = null): ItemStack {
        return tryFromCraftItem(item, amountOverride) ?: missingItem(item, amountOverride)
    }

    fun tryFromCraftItem(item: CraftItem, amountOverride: Int? = null): ItemStack? {
        val amount = (amountOverride ?: item.amount).coerceAtLeast(1)
        if (item.mode == ItemMode.MMOITEMS) {
            return hooks?.mmoItem(item.mmoType, item.mmoId, amount)?.let { applyAdvancedEnchantments(it, item) }
        }
        val material = Material.matchMaterial(item.material) ?: return null
        val stack = ItemStack(material, amount)
        val meta = stack.itemMeta
        if (meta != null) {
            item.name?.let { meta.displayName(Text.c(it)) }
            if (item.lore.isNotEmpty()) meta.lore(item.lore.map { Text.c(it) })
            item.uid?.let { meta.persistentDataContainer.set(uidKey, PersistentDataType.STRING, it) }
            stack.itemMeta = meta
        }
        return applyAdvancedEnchantments(stack, item)
    }

    fun fromStack(stack: ItemStack): CraftItem {
        val mmo = hooks?.mmoKey(stack)
        val meta = stack.itemMeta
        return if (mmo != null) {
            CraftItem(
                mode = ItemMode.MMOITEMS,
                material = stack.type.name,
                amount = stack.amount.coerceAtLeast(1),
                name = meta?.displayName()?.let { Text.plain(it) },
                lore = meta?.lore()?.map { Text.plain(it) } ?: emptyList(),
                mmoType = mmo.first,
                mmoId = mmo.second
            )
        } else {
            CraftItem(
                mode = ItemMode.VANILLA,
                material = stack.type.name,
                amount = stack.amount.coerceAtLeast(1),
                uid = meta?.persistentDataContainer?.get(uidKey, PersistentDataType.STRING),
                name = meta?.displayName()?.let { Text.plain(it) },
                lore = meta?.lore()?.map { Text.plain(it) } ?: emptyList()
            )
        }
    }

    fun applyQuality(stack: ItemStack, quality: CraftQualityPolicy): ItemStack {
        val name = quality.name?.trim()?.takeIf { it.isNotEmpty() } ?: return stack
        return stack.clone().apply {
            editMeta { meta ->
                meta.persistentDataContainer.set(qualityKey, PersistentDataType.STRING, name)
                val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                lore += Text.c("#ffd166✦ Quality: $name")
                lore += quality.lore.map(Text::c)
                meta.lore(lore)
            }
        }
    }

    private fun risk(item: ItemStack): ItemRisk {
        val enchantCount = item.enchantments.size + (hooks?.advancedEnchantments(item)?.size ?: 0)
        val lore = item.itemMeta?.lore()?.size ?: 0
        return ItemRisk(enchantCount = enchantCount, gemstoneCount = 0, upgradeLevel = if (lore > 0) 1 else 0)
    }

    private fun applyAdvancedEnchantments(stack: ItemStack, item: CraftItem): ItemStack {
        var result = stack
        for (enchant in item.advancedEnchantments) {
            result = hooks?.applyAdvancedEnchant(result, enchant.id, enchant.level) ?: result
        }
        return result
    }

    private fun missingItem(item: CraftItem, amountOverride: Int?): ItemStack {
        val stack = ItemStack(Material.BARRIER, (amountOverride ?: item.amount).coerceAtLeast(1).coerceAtMost(64))
        stack.editMeta {
            it.displayName(Text.c("#ff6961Missing item hook"))
            it.lore(listOf(
                Text.c("#d6f7ffMode: ${item.mode}"),
                Text.c("#d6f7ffType: ${item.mmoType ?: "-"}"),
                Text.c("#d6f7ffId: ${item.mmoId ?: item.material}")
            ))
        }
        return stack
    }
}
