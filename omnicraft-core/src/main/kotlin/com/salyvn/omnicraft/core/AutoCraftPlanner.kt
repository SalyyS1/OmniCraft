package com.salyvn.omnicraft.core

import kotlin.math.ceil

data class AutoCraftPolicy(
    val maxDepth: Int = 8,
    val maxNodes: Int = 64,
    val maxCraftsPerNode: Int = 512
)

data class AutoCraftStep(val recipeKey: RecipeKey, val crafts: Int)

sealed interface AutoCraftPlanResult {
    data class Success(val steps: List<AutoCraftStep>) : AutoCraftPlanResult {
        val orderedRecipes: List<RecipeKey> get() = steps.map { it.recipeKey }
    }
    data class Failure(val reason: String) : AutoCraftPlanResult
}

/**
 * Creates a deterministic, inventory-first recipe plan. The returned steps are
 * prerequisites followed by the target; the caller must revalidate every step
 * against live Bukkit state before it commits a transaction.
 */
class AutoCraftPlanner(private val matcher: CraftMatcher = CraftMatcher()) {
    fun plan(
        target: CraftRecipe,
        targetCrafts: Int,
        recipes: List<CraftRecipe>,
        inventory: List<InventoryEntry>,
        policy: AutoCraftPolicy = AutoCraftPolicy()
    ): AutoCraftPlanResult {
        if (targetCrafts !in 1..policy.maxCraftsPerNode) return AutoCraftPlanResult.Failure("target-limit")
        val enabled = recipes.filter { it.options.enabled && it.options.sourceHints["auto-craft.enabled"]?.equals("true", true) == true }
        val sources = enabled.groupBy { matcher.keyOf(it.output) }
        val stock = inventory.groupBy { it.key }.mapValuesTo(mutableMapOf()) { (_, entries) -> entries.sumOf { it.amount.coerceAtLeast(0) }.toLong() }
        val steps = mutableListOf<AutoCraftStep>()
        val visiting = mutableSetOf<RecipeKey>()

        fun addStep(recipe: CraftRecipe, crafts: Int): String? {
            if (crafts !in 1..policy.maxCraftsPerNode) return "node-craft-limit"
            val key = RecipeKey.of(recipe)
            // Do not coalesce non-adjacent nodes: a later use of the same recipe can depend on
            // an intermediate source planned after its earlier use.
            steps += AutoCraftStep(key, crafts)
            return if (steps.size > policy.maxNodes) "node-limit" else null
        }

        fun ensure(required: CraftItem, amount: Long, depth: Int): String? {
            if (amount <= 0) return null
            val itemKey = matcher.keyOf(required)
            val available = stock[itemKey] ?: 0L
            if (available >= amount) {
                stock[itemKey] = available - amount
                return null
            }
            stock[itemKey] = 0L
            val missing = amount - available
            val source = sources[itemKey]
                ?.sortedWith(compareByDescending<CraftRecipe> { it.options.sourceHints["auto-craft.priority"]?.toIntOrNull() ?: 0 }
                    .thenBy { RecipeKey.of(it).toString() })
                ?.firstOrNull()
                ?: return "missing-source:$itemKey"
            if (depth >= policy.maxDepth) return "depth-limit"
            val sourceKey = RecipeKey.of(source)
            if (!visiting.add(sourceKey)) return "cycle:$sourceKey"
            val outputAmount = source.output.amount.coerceAtLeast(0)
            if (outputAmount == 0) return "invalid-output:$sourceKey"
            val crafts = ceil(missing.toDouble() / outputAmount.toDouble()).toLong()
            if (crafts !in 1..policy.maxCraftsPerNode.toLong()) return "node-craft-limit"
            for (ingredient in source.ingredients) {
                val requiredAmount = ingredient.requiredAmount.toLong() * crafts
                ensure(ingredient.item, requiredAmount, depth + 1)?.let { return it }
            }
            visiting.remove(sourceKey)
            addStep(source, crafts.toInt())?.let { return it }
            val produced = outputAmount.toLong() * crafts
            stock[itemKey] = produced - missing
            return null
        }

        val targetKey = RecipeKey.of(target)
        if (!visiting.add(targetKey)) return AutoCraftPlanResult.Failure("cycle:$targetKey")
        for (ingredient in target.ingredients) {
            ensure(ingredient.item, ingredient.requiredAmount.toLong() * targetCrafts, 0)?.let { return AutoCraftPlanResult.Failure(it) }
        }
        visiting.remove(targetKey)
        addStep(target, targetCrafts)?.let { return AutoCraftPlanResult.Failure(it) }
        return AutoCraftPlanResult.Success(steps)
    }

    /** Compatibility overload for callers that only need dependency validation. */
    fun plan(target: CraftRecipe, recipes: List<CraftRecipe>, policy: AutoCraftPolicy = AutoCraftPolicy()): AutoCraftPlanResult =
        plan(target, 1, recipes, emptyList(), policy)
}
