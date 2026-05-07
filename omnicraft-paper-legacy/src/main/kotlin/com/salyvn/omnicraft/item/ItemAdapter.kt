package com.salyvn.omnicraft.item

import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.InventoryEntry
import com.salyvn.omnicraft.core.ItemKey
import com.salyvn.omnicraft.core.ItemMode
import com.salyvn.omnicraft.core.ItemRisk
import com.salyvn.omnicraft.util.Text
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemAdapter {
    fun key(stack: ItemStack): ItemKey {
        val meta = stack.itemMeta
        val uid = meta?.persistentDataContainer?.keys?.firstOrNull { it.key.equals("omnicraft_uid", true) }?.let {
            meta.persistentDataContainer.get(it, org.bukkit.persistence.PersistentDataType.STRING)
        }
        return ItemKey(ItemMode.VANILLA, stack.type.name, uid)
    }

    fun inventoryEntries(contents: Array<ItemStack?>): List<InventoryEntry> {
        return contents.mapIndexedNotNull { slot, item ->
            if (item == null || item.type.isAir || item.amount <= 0) null
            else InventoryEntry(slot, key(item), item.amount, risk(item))
        }
    }

    fun fromCraftItem(item: CraftItem, amountOverride: Int? = null): ItemStack {
        val material = Material.matchMaterial(item.material) ?: Material.STONE
        val stack = ItemStack(material, (amountOverride ?: item.amount).coerceAtLeast(1))
        val meta = stack.itemMeta
        if (meta != null) {
            item.name?.let { meta.displayName(Text.c(it)) }
            if (item.lore.isNotEmpty()) meta.lore(item.lore.map { Text.c(it) })
            stack.itemMeta = meta
        }
        return stack
    }

    private fun risk(item: ItemStack): ItemRisk {
        val enchantCount = item.enchantments.size
        val lore = item.itemMeta?.lore()?.size ?: 0
        return ItemRisk(enchantCount = enchantCount, gemstoneCount = 0, upgradeLevel = if (lore > 0) 1 else 0)
    }
}
