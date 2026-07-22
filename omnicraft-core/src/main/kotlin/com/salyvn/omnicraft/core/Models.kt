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
    val limits: CraftLimits,
    val options: RecipeOptions = RecipeOptions(),
    val auraSkills: AuraSkillsPolicy = AuraSkillsPolicy()
)

/**
 * Stable identity for a recipe outside configuration and GUI display objects.
 * IDs are normalized so lock and future job keys cannot diverge only by case.
 */
data class RecipeKey private constructor(
    val categoryId: String,
    val recipeId: String
) {
    override fun toString(): String = "$categoryId:$recipeId"

    companion object {
        fun of(categoryId: String, recipeId: String): RecipeKey {
            val canonicalCategory = categoryId.trim().lowercase()
            val canonicalRecipe = recipeId.trim().lowercase()
            require(canonicalCategory.isNotEmpty()) { "categoryId must not be blank" }
            require(canonicalRecipe.isNotEmpty()) { "recipeId must not be blank" }
            return RecipeKey(canonicalCategory, canonicalRecipe)
        }

        fun of(recipe: CraftRecipe): RecipeKey = of(recipe.categoryId, recipe.id)
    }
}

data class CraftItem(
    val mode: ItemMode,
    val material: String,
    val amount: Int,
    val uid: String? = null,
    val name: String? = null,
    val lore: List<String> = emptyList(),
    val mmoType: String? = null,
    val mmoId: String? = null,
    val advancedEnchantments: List<AdvancedEnchant> = emptyList()
)

data class AdvancedEnchant(
    val id: String,
    val level: Int,
    val successRate: Double = 100.0,
    val destroyRate: Double = 0.0,
    val tier: String? = null
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
    val cancelOnLogout: Boolean = true,
    val quantityScaling: CraftQuantityScaling = CraftQuantityScaling.LINEAR,
    val minimumSeconds: Int = 1,
    val maximumSeconds: Int = 3_600
)

enum class CraftQuantityScaling {
    FIXED,
    LINEAR
}

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

/** Optional AuraSkills contract. Empty skill means AuraSkills is not required for this recipe. */
data class AuraSkillsPolicy(
    val skill: String? = null,
    val minimumLevel: Int = 0,
    val experience: Double = 0.0
) {
    companion object {
        val DEFAULT_SKILLS = setOf(
            "FARMING", "FORAGING", "MINING", "FISHING", "EXCAVATION", "ARCHERY", "FIGHTING", "DEFENSE",
            "AGILITY", "ENDURANCE", "ALCHEMY", "ENCHANTING", "SORCERY", "HEALING", "FORGING"
        )
    }
}

data class RecipeOptions(
    val enabled: Boolean = true,
    val hidden: Boolean = false,
    val rareBroadcast: Boolean = false,
    val sourceHints: Map<String, String> = emptyMap()
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
