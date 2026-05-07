package com.salyvn.omnicraft.item

import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.InventoryEntry
import com.salyvn.omnicraft.core.ItemKey
import com.salyvn.omnicraft.core.ItemMode
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
        val amount = (amountOverride ?: item.amount).coerceAtLeast(1)
        if (item.mode == ItemMode.MMOITEMS) {
            hooks?.mmoItem(item.mmoType, item.mmoId, amount)?.let { return applyAdvancedEnchantments(it, item) }
        }
        val material = Material.matchMaterial(item.material) ?: Material.STONE
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
}
