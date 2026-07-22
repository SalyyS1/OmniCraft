package com.salyvn.omnicraft.config

import com.salyvn.omnicraft.core.CraftItem
import com.salyvn.omnicraft.core.CraftRecipe
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class RecipeWriter {
    fun saveAtomic(file: File, yaml: YamlConfiguration) {
        AtomicYamlFile.save(file, yaml)
    }

    fun saveAtomic(file: File, recipe: CraftRecipe) {
        file.parentFile.mkdirs()
        val yaml = YamlConfiguration()
        yaml.set("id", recipe.id)
        yaml.set("display.name", recipe.displayName)
        writeItem(yaml, "output", recipe.output)
        for (ingredient in recipe.ingredients) {
            yaml.set("ingredients.${ingredient.id}.amount", ingredient.requiredAmount)
            writeItem(yaml, "ingredients.${ingredient.id}.item", ingredient.item)
        }
        yaml.set("requirements.permission", recipe.requirements.permission)
        yaml.set("requirements.level", recipe.requirements.level)
        yaml.set("requirements.money", recipe.requirements.money)
        yaml.set("requirements.conditions", recipe.requirements.papiConditions)
        yaml.set("craft.left-amount", recipe.craft.leftAmount)
        yaml.set("craft.right-amount", recipe.craft.rightAmount)
        yaml.set("craft.shift-hard-cap", recipe.craft.shiftHardCap)
        yaml.set("craft.cooldown-ms", recipe.craft.cooldownMillis)
        yaml.set("craft-time.enabled", recipe.craftTime.enabled)
        yaml.set("craft-time.seconds", recipe.craftTime.seconds)
        yaml.set("craft-time.cancel-on-move", recipe.craftTime.cancelOnMove)
        yaml.set("craft-time.cancel-on-logout", recipe.craftTime.cancelOnLogout)
        yaml.set("craft-time.quantity-scaling", recipe.craftTime.quantityScaling.name)
        yaml.set("craft-time.minimum-seconds", recipe.craftTime.minimumSeconds)
        yaml.set("craft-time.maximum-seconds", recipe.craftTime.maximumSeconds)
        yaml.set("extraction.enchant", recipe.extraction.enchant.name)
        yaml.set("extraction.gemstone", recipe.extraction.gemstone.name)
        yaml.set("extraction.level", recipe.extraction.level.name)
        yaml.set("extraction.success-rate", recipe.extraction.successRate)
        yaml.set("limits.daily", recipe.limits.daily)
        yaml.set("limits.weekly", recipe.limits.weekly)
        yaml.set("options.enabled", recipe.options.enabled)
        yaml.set("options.hidden", recipe.options.hidden)
        yaml.set("options.rare-broadcast", recipe.options.rareBroadcast)
        recipe.options.sourceHints.forEach { (key, value) -> yaml.set("options.source-hints.$key", value) }
        yaml.set("auto-craft.enabled", recipe.options.sourceHints["auto-craft.enabled"]?.toBooleanStrictOrNull() ?: false)
        yaml.set("auto-craft.priority", recipe.options.sourceHints["auto-craft.priority"]?.toIntOrNull() ?: 0)
        yaml.set("auraskills.skill", recipe.auraSkills.skill)
        yaml.set("auraskills.minimum-level", recipe.auraSkills.minimumLevel)
        yaml.set("auraskills.experience", recipe.auraSkills.experience)

        saveAtomic(file, yaml)
    }

    private fun writeItem(yaml: YamlConfiguration, path: String, item: CraftItem) {
        yaml.set("$path.mode", item.mode.name)
        yaml.set("$path.material", item.material)
        yaml.set("$path.amount", item.amount)
        yaml.set("$path.uid", item.uid)
        yaml.set("$path.name", item.name)
        yaml.set("$path.lore", item.lore)
        yaml.set("$path.type", item.mmoType)
        yaml.set("$path.id", item.mmoId)
        for (enchant in item.advancedEnchantments) {
            yaml.set("$path.enchantments.advanced.${enchant.id}.level", enchant.level)
            yaml.set("$path.enchantments.advanced.${enchant.id}.success-rate", enchant.successRate)
            yaml.set("$path.enchantments.advanced.${enchant.id}.destroy-rate", enchant.destroyRate)
            yaml.set("$path.enchantments.advanced.${enchant.id}.tier", enchant.tier)
        }
    }
}
