package com.salyvn.omnicraft.config

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.core.CraftBehavior
import com.salyvn.omnicraft.core.CraftCategory
import com.salyvn.omnicraft.core.CraftCatalyst
import com.salyvn.omnicraft.core.CraftStationPolicy
import com.salyvn.omnicraft.core.CraftOutcomePolicy
import com.salyvn.omnicraft.core.CraftQualityPolicy
import com.salyvn.omnicraft.core.CraftIngredient
import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.CraftLimits
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.core.CraftRequirements
import com.salyvn.omnicraft.core.CraftTime
import com.salyvn.omnicraft.core.CraftQuantityScaling
import com.salyvn.omnicraft.core.ExtractionMode
import com.salyvn.omnicraft.core.ExtractionPolicy
import com.salyvn.omnicraft.core.ItemMode
import com.salyvn.omnicraft.core.AdvancedEnchant
import com.salyvn.omnicraft.core.AuraSkillsPolicy
import com.salyvn.omnicraft.core.RecipeOptions
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigService(private val plugin: OmniCraftPlugin) {
    private val writer = RecipeWriter()
    var categories: List<CraftCategory> = emptyList()
        private set
    var messages: YamlConfiguration = YamlConfiguration()
        private set

    fun reload() {
        val messageFile = File(plugin.dataFolder, "messages.yml")
        val typoFile = File(plugin.dataFolder, "messange.yml")
        messages = YamlConfiguration.loadConfiguration(if (messageFile.exists()) messageFile else typoFile)
        categories = loadCategories()
    }

    fun message(path: String, fallback: String): String {
        return messages.getString(path, fallback) ?: fallback
    }

    fun category(id: String): CraftCategory? {
        return categories.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    fun recipe(categoryId: String, recipeId: String): CraftRecipe? {
        return category(categoryId)?.recipes?.firstOrNull { it.id.equals(recipeId, ignoreCase = true) }
    }

    fun recipe(anyId: String): CraftRecipe? {
        return categories.asSequence().flatMap { it.recipes.asSequence() }
            .firstOrNull { it.id.equals(anyId, ignoreCase = true) || "${it.categoryId}:${it.id}".equals(anyId, ignoreCase = true) }
    }

    fun recipeFile(categoryId: String, recipeId: String): File {
        return File(plugin.dataFolder, "category/$categoryId/$recipeId.yml")
    }

    fun saveRecipe(recipe: CraftRecipe) {
        plugin.cancelActiveCrafting("cancelled: recipe updated")
        writer.saveAtomic(recipeFile(recipe.categoryId, recipe.id), recipe)
        reload()
    }

    fun deleteRecipe(categoryId: String, recipeId: String): Boolean {
        plugin.cancelActiveCrafting("cancelled: recipe updated")
        val deleted = recipeFile(categoryId, recipeId).delete()
        reload()
        return deleted
    }

    fun setConfig(path: String, value: Any?) {
        plugin.config.set(path, value)
        plugin.saveConfig()
        plugin.reloadConfig()
    }

    fun pluginConfigBoolean(path: String): Boolean {
        return plugin.config.getBoolean(path)
    }

    fun loadFavorites(playerId: String): MutableSet<String> {
        val file = File(plugin.dataFolder, "favorites.yml")
        if (!file.exists()) return mutableSetOf()
        return YamlConfiguration.loadConfiguration(file).getStringList(playerId).toMutableSet()
    }

    fun saveFavorites(playerId: String, keys: Set<String>) {
        val file = File(plugin.dataFolder, "favorites.yml")
        val yaml = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()
        yaml.set(playerId, keys.sorted())
        writer.saveAtomic(file, yaml)
    }

    fun validate(): List<String> {
        val issues = mutableListOf<String>()
        val categoryIds = mutableSetOf<String>()
        for (category in categories) {
            if (!categoryIds.add(category.id.lowercase())) issues += "Duplicate category '${category.id}'"
            if (category.slot !in 0..53) issues += "Category '${category.id}' slot is outside 0..53"
            if (category.recipes.isEmpty()) issues += "Category '${category.id}' has no recipes"
            val recipeIds = mutableSetOf<String>()
            for (recipe in category.recipes) {
                if (!recipeIds.add(recipe.id.lowercase())) issues += "Duplicate recipe '${category.id}:${recipe.id}'"
                if (recipe.ingredients.isEmpty()) issues += "Recipe '${category.id}:${recipe.id}' has no ingredients"
                if (recipe.ingredients.any { it.id == "__catalyst" }) issues += "Recipe '${category.id}:${recipe.id}' uses reserved ingredient id '__catalyst'"
                if (recipe.ingredients.size > 16) issues += "Recipe '${category.id}:${recipe.id}' has more than 16 ingredients"
                if (recipe.output.amount <= 0) issues += "Recipe '${category.id}:${recipe.id}' output amount must be positive"
                if (!recipe.outcome.criticalChance.isFinite() || recipe.outcome.criticalChance !in 0.0..100.0) issues += "Recipe '${category.id}:${recipe.id}' critical chance must be 0..100"
                if (!recipe.outcome.byproductChance.isFinite() || recipe.outcome.byproductChance !in 0.0..100.0) issues += "Recipe '${category.id}:${recipe.id}' byproduct chance must be 0..100"
                if (!recipe.outcome.quality.chance.isFinite() || recipe.outcome.quality.chance !in 0.0..100.0) issues += "Recipe '${category.id}:${recipe.id}' quality chance must be 0..100"
                if (recipe.outcome.quality.chance > 0.0 && recipe.outcome.quality.name.isNullOrBlank()) issues += "Recipe '${category.id}:${recipe.id}' quality chance requires a quality name"
                if (!recipe.outcome.quality.name.isNullOrBlank() && recipe.extraction.enchant == ExtractionMode.KEEP) issues += "Recipe '${category.id}:${recipe.id}' quality cannot be combined with enchant KEEP"
                recipe.station.material?.let { material ->
                    if (Material.matchMaterial(material) == null) issues += "Recipe '${category.id}:${recipe.id}' station material '$material' is invalid"
                }
                recipe.catalyst?.let { catalyst ->
                    if (catalyst.item.mode == ItemMode.VANILLA && Material.matchMaterial(catalyst.item.material) == null) {
                        issues += "Recipe '${category.id}:${recipe.id}' catalyst material '${catalyst.item.material}' is invalid"
                    }
                }
                recipe.outcome.byproduct?.let { byproduct ->
                    if (byproduct.mode == ItemMode.VANILLA && Material.matchMaterial(byproduct.material) == null) {
                        issues += "Recipe '${category.id}:${recipe.id}' byproduct material '${byproduct.material}' is invalid"
                    }
                }
                if (recipe.extraction.successRate !in 0.0..1.0) issues += "Recipe '${category.id}:${recipe.id}' extraction success rate must be 0..1"
                if (recipe.output.advancedEnchantments.any { it.id.isBlank() }) issues += "Recipe '${category.id}:${recipe.id}' has blank AdvancedEnchantments id"
                recipe.auraSkills.skill?.let { skill ->
                    if (skill.uppercase() !in AuraSkillsPolicy.DEFAULT_SKILLS) {
                        issues += "Recipe '${category.id}:${recipe.id}' AuraSkills skill '$skill' is not a supported default skill"
                    }
                }
            }
        }
        return issues
    }

    private fun loadCategories(): List<CraftCategory> {
        val configCategories = plugin.config.getMapList("main-menu.categories")
        return configCategories.mapNotNull { raw ->
            val id = raw["id"]?.toString() ?: return@mapNotNull null
            CraftCategory(
                id = id,
                title = raw["title"]?.toString() ?: id,
                icon = raw["icon"]?.toString() ?: Material.CRAFTING_TABLE.name,
                slot = raw["slot"]?.toString()?.toIntOrNull() ?: 10,
                permission = raw["permission"]?.toString() ?: "omnicraft.category.$id",
                recipes = loadRecipes(id)
            )
        }
    }

    private fun loadRecipes(categoryId: String): List<CraftRecipe> {
        val folder = File(plugin.dataFolder, "category/$categoryId")
        if (!folder.exists()) return emptyList()
        return folder.listFiles { file -> file.isFile && file.extension.equals("yml", true) }
            ?.mapNotNull { loadRecipe(categoryId, it) }
            ?: emptyList()
    }

    private fun loadRecipe(categoryId: String, file: File): CraftRecipe? {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val id = yaml.getString("id") ?: file.nameWithoutExtension
        val output = loadItem(yaml, "output") ?: return null
        val ingredients = yaml.getConfigurationSection("ingredients")?.getKeys(false)?.mapNotNull { key ->
            val item = loadItem(yaml, "ingredients.$key.item") ?: return@mapNotNull null
            CraftIngredient(key, item, yaml.getInt("ingredients.$key.amount", item.amount).coerceAtLeast(1))
        } ?: emptyList()

        return CraftRecipe(
            id = id,
            categoryId = categoryId,
            displayName = yaml.getString("display.name") ?: output.name ?: id,
            output = output,
            ingredients = ingredients,
            requirements = CraftRequirements(
                permission = yaml.getString("requirements.permission"),
                level = yaml.getInt("requirements.level", 0),
                money = yaml.getDouble("requirements.money", 0.0),
                papiConditions = yaml.getStringList("requirements.conditions")
            ),
            craft = CraftBehavior(
                leftAmount = yaml.getInt("craft.left-amount", plugin.config.getInt("craft.left-amount", 1)),
                rightAmount = yaml.getInt("craft.right-amount", plugin.config.getInt("craft.right-amount", 16)),
                shiftHardCap = yaml.getInt("craft.shift-hard-cap", plugin.config.getInt("craft.shift-hard-cap", 512)),
                cooldownMillis = yaml.getLong("craft.cooldown-ms", plugin.config.getLong("craft.cooldown-ms", 350))
            ),
            craftTime = CraftTime(
                enabled = yaml.getBoolean("craft-time.enabled", plugin.config.getBoolean("craft-time.enabled", false)),
                seconds = yaml.getInt("craft-time.seconds", plugin.config.getInt("craft-time.seconds", 0)),
                cancelOnMove = yaml.getBoolean("craft-time.cancel-on-move", plugin.config.getBoolean("craft-time.cancel-on-move", false)),
                // Timed work is online-only; retaining this model field avoids breaking API consumers.
                cancelOnLogout = true,
                quantityScaling = runCatching {
                    CraftQuantityScaling.valueOf(yaml.getString("craft-time.quantity-scaling", "LINEAR")!!.uppercase())
                }.getOrDefault(CraftQuantityScaling.LINEAR),
                minimumSeconds = yaml.getInt("craft-time.minimum-seconds", plugin.config.getInt("craft-time.minimum-seconds", 1)).coerceAtLeast(0),
                maximumSeconds = yaml.getInt("craft-time.maximum-seconds", plugin.config.getInt("craft-time.maximum-seconds", 3600)).coerceAtLeast(0)
            ),
            extraction = ExtractionPolicy(
                enchant = extractionMode(yaml.getString("extraction.enchant", "DESTROY")),
                gemstone = extractionMode(yaml.getString("extraction.gemstone", "DESTROY")),
                level = extractionMode(yaml.getString("extraction.level", "DESTROY")),
                successRate = yaml.getDouble("extraction.success-rate", 1.0).coerceIn(0.0, 1.0)
            ),
            limits = CraftLimits(
                daily = yaml.getInt("limits.daily", -1),
                weekly = yaml.getInt("limits.weekly", -1)
            ),
            options = RecipeOptions(
                enabled = yaml.getBoolean("options.enabled", true),
                hidden = yaml.getBoolean("options.hidden", false),
                rareBroadcast = yaml.getBoolean("options.rare-broadcast", false),
                sourceHints = (yaml.getConfigurationSection("options.source-hints")?.getKeys(false)
                    ?.associateWith { key -> yaml.getString("options.source-hints.$key", "") ?: "" }
                    ?: emptyMap()).let { sourceHints ->
                    val legacyEnabled = yaml.getString("options.source-hints.auto-craft.enabled")
                        ?: sourceHints["auto-craft.enabled"]
                    val legacyPriority = yaml.getString("options.source-hints.auto-craft.priority")
                        ?: sourceHints["auto-craft.priority"]
                    sourceHints + buildMap {
                        if (yaml.contains("auto-craft.enabled")) put("auto-craft.enabled", yaml.getBoolean("auto-craft.enabled").toString())
                        else if (legacyEnabled != null) put("auto-craft.enabled", legacyEnabled)
                        if (yaml.contains("auto-craft.priority")) put("auto-craft.priority", yaml.getInt("auto-craft.priority").toString())
                        else if (legacyPriority != null) put("auto-craft.priority", legacyPriority)
                    }
                }
            ),
            auraSkills = AuraSkillsPolicy(
                skill = yaml.getString("auraskills.skill")?.trim()?.takeIf { it.isNotEmpty() },
                minimumLevel = yaml.getInt("auraskills.minimum-level", 0).coerceAtLeast(0),
                experience = yaml.getDouble("auraskills.experience", 0.0).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            ),
            catalyst = loadItem(yaml, "catalyst.item")?.let { CraftCatalyst(it, yaml.getInt("catalyst.amount", 1).coerceAtLeast(1)) },
            station = CraftStationPolicy(
                material = yaml.getString("station.material")?.trim()?.takeIf { it.isNotEmpty() },
                radius = yaml.getInt("station.radius", 0).coerceIn(0, 6)
            ),
            outcome = CraftOutcomePolicy(
                criticalChance = yaml.getDouble("outcome.critical.chance", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0, 100.0) ?: 0.0,
                criticalBonusCrafts = yaml.getInt("outcome.critical.bonus-crafts", 0).coerceIn(0, 64),
                byproduct = loadItem(yaml, "outcome.byproduct.item"),
                byproductChance = yaml.getDouble("outcome.byproduct.chance", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0, 100.0) ?: 0.0,
                quality = CraftQualityPolicy(
                    name = yaml.getString("outcome.quality.name")?.trim()?.takeIf { it.isNotEmpty() },
                    chance = yaml.getDouble("outcome.quality.chance", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0, 100.0) ?: 0.0,
                    lore = yaml.getStringList("outcome.quality.lore")
                )
            )
        )
    }

    private fun loadItem(yaml: YamlConfiguration, path: String): CraftItem? {
        val material = yaml.getString("$path.material") ?: yaml.getString("$path.MATERIAL") ?: return null
        val mode = runCatching { ItemMode.valueOf(yaml.getString("$path.mode", "VANILLA")!!.uppercase()) }.getOrDefault(ItemMode.VANILLA)
        return CraftItem(
            mode = mode,
            material = material,
            amount = yaml.getInt("$path.amount", 1).coerceAtLeast(1),
            uid = yaml.getString("$path.uid"),
            name = yaml.getString("$path.name"),
            lore = yaml.getStringList("$path.lore"),
            mmoType = yaml.getString("$path.type"),
            mmoId = yaml.getString("$path.id"),
            advancedEnchantments = loadAdvancedEnchantments(yaml, "$path.enchantments.advanced") +
                loadAdvancedEnchantments(yaml, "$path.advanced-enchantments")
        )
    }

    private fun loadAdvancedEnchantments(yaml: YamlConfiguration, path: String): List<AdvancedEnchant> {
        val section = yaml.getConfigurationSection(path)
        if (section != null) {
            return section.getKeys(false).mapNotNull { id ->
                val level = yaml.getInt("$path.$id.level", yaml.getInt("$path.$id", 1))
                AdvancedEnchant(
                    id = id,
                    level = level.coerceAtLeast(1),
                    successRate = yaml.getDouble("$path.$id.success-rate", 100.0).coerceIn(0.0, 100.0),
                    destroyRate = yaml.getDouble("$path.$id.destroy-rate", 0.0).coerceIn(0.0, 100.0),
                    tier = yaml.getString("$path.$id.tier")
                )
            }
        }
        return yaml.getStringList(path).mapNotNull { raw ->
            val parts = raw.split(":", limit = 4)
            val id = parts.getOrNull(0)?.trim().orEmpty()
            if (id.isBlank()) return@mapNotNull null
            AdvancedEnchant(
                id = id,
                level = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                successRate = parts.getOrNull(2)?.trim()?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 100.0,
                destroyRate = parts.getOrNull(3)?.trim()?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
            )
        }
    }

    private fun extractionMode(value: String?): ExtractionMode {
        return runCatching { ExtractionMode.valueOf((value ?: "DESTROY").uppercase()) }.getOrDefault(ExtractionMode.DESTROY)
    }
}
