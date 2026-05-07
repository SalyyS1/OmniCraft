package com.salyvn.omnicraft.config

import com.salyvn.omnicraft.OmniCraftPlugin
import com.salyvn.omnicraft.core.CraftBehavior
import com.salyvn.omnicraft.core.CraftCategory
import com.salyvn.omnicraft.core.CraftIngredient
import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.CraftLimits
import com.salyvn.omnicraft.core.CraftRecipe
import com.salyvn.omnicraft.core.CraftRequirements
import com.salyvn.omnicraft.core.CraftTime
import com.salyvn.omnicraft.core.ExtractionMode
import com.salyvn.omnicraft.core.ExtractionPolicy
import com.salyvn.omnicraft.core.ItemMode
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigService(private val plugin: OmniCraftPlugin) {
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
                cancelOnLogout = yaml.getBoolean("craft-time.cancel-on-logout", plugin.config.getBoolean("craft-time.cancel-on-logout", true))
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
            mmoId = yaml.getString("$path.id")
        )
    }

    private fun extractionMode(value: String?): ExtractionMode {
        return runCatching { ExtractionMode.valueOf((value ?: "DESTROY").uppercase()) }.getOrDefault(ExtractionMode.DESTROY)
    }
}
