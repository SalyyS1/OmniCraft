package com.salyvn.omnicraft.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class OmniHolder(
    val type: GuiType,
    val categoryId: String? = null,
    val recipeId: String? = null,
    val searchQuery: String? = null,
    val action: String? = null,
    val editorSlot: Int = -1,
    val page: Int = 0,
    val itemType: String? = null
) : InventoryHolder {
    private var inventory: Inventory? = null
    override fun getInventory(): Inventory = inventory ?: error("Inventory is not attached")
    fun attach(value: Inventory) {
        inventory = value
    }
}

enum class GuiType {
    MAIN,
    CATEGORY,
    RECIPE,
    SETTINGS,
    BROWSE,
    ADMIN_CATEGORY,
    EDITOR,
    ITEM_MODE,
    VANILLA_BROWSER,
    MMO_TYPE_BROWSER,
    MMO_ITEM_BROWSER
}
