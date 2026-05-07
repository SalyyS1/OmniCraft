package com.salyvn.omnicraft.core

data class CraftCategory(
    val id: String,
    val title: String,
    val icon: String,
    val slot: Int,
    val permission: String,
    val recipes: List<CraftRecipe>
)

data class CraftRecipe(
    val id: String,
    val categoryId: String,
    val displayName: String,
    val output: CraftItem,
    val ingredients: List<CraftIngredient>,
    val requirements: CraftRequirements,
    val craft: CraftBehavior,
    val craftTime: CraftTime,
    val extraction: ExtractionPolicy,
    val limits: CraftLimits
)

data class CraftItem(
    val mode: ItemMode,
    val material: String,
    val amount: Int,
    val uid: String? = null,
    val name: String? = null,
    val lore: List<String> = emptyList(),
    val mmoType: String? = null,
    val mmoId: String? = null
)

data class CraftIngredient(
    val id: String,
    val item: CraftItem,
    val requiredAmount: Int
)

data class CraftRequirements(
    val permission: String? = null,
    val level: Int = 0,
    val money: Double = 0.0,
    val papiConditions: List<String> = emptyList()
)

data class CraftBehavior(
    val leftAmount: Int = 1,
    val rightAmount: Int = 16,
    val shiftHardCap: Int = 512,
    val cooldownMillis: Long = 350
)

data class CraftTime(
    val enabled: Boolean = false,
    val seconds: Int = 0,
    val cancelOnMove: Boolean = false,
    val cancelOnLogout: Boolean = true
)

data class ExtractionPolicy(
    val enchant: ExtractionMode = ExtractionMode.DESTROY,
    val gemstone: ExtractionMode = ExtractionMode.DESTROY,
    val level: ExtractionMode = ExtractionMode.DESTROY,
    val successRate: Double = 1.0
)

data class CraftLimits(
    val daily: Int = -1,
    val weekly: Int = -1
)

enum class ItemMode {
    VANILLA,
    MMOITEMS
}

enum class ExtractionMode {
    KEEP,
    DESTROY,
    EXTRACT
}

enum class CraftClickMode {
    LEFT,
    RIGHT,
    SHIFT
}

data class InventoryEntry(
    val slot: Int,
    val key: ItemKey,
    val amount: Int,
    val risk: ItemRisk = ItemRisk()
)

data class ItemKey(
    val mode: ItemMode,
    val material: String,
    val uid: String? = null,
    val mmoType: String? = null,
    val mmoId: String? = null
)

data class ItemRisk(
    val enchantCount: Int = 0,
    val gemstoneCount: Int = 0,
    val upgradeLevel: Int = 0
)

data class CraftCheck(
    val craftableAmount: Int,
    val missing: Map<String, Int>,
    val permissionDenied: Boolean,
    val levelMissing: Int,
    val moneyMissing: Double,
    val conditionDenied: List<String>
) {
    val allowed: Boolean
        get() = craftableAmount > 0 && !permissionDenied && levelMissing <= 0 && moneyMissing <= 0.0 && conditionDenied.isEmpty()
}
