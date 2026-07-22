package com.salyvn.omnicraft.core

/** Immutable preflight result. Paper adapters must revalidate allocations before mutation. */
data class CraftQuote(
    val recipeKey: RecipeKey,
    val requestedCrafts: Int,
    val allowedCrafts: Int,
    val totalMoney: Double,
    val allocation: Map<String, List<InventoryEntry>>,
    val failure: CraftQuoteFailure? = null
) {
    val isExecutable: Boolean get() = failure == null && allowedCrafts > 0
}

enum class CraftQuoteFailure {
    INVALID_RECIPE,
    INVALID_QUANTITY,
    MATERIALS,
    MONEY,
    REQUIREMENTS,
    CAPACITY
}
