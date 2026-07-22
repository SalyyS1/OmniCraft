package com.salyvn.omnicraft.craft

import com.salyvn.omnicraft.core.CraftRecipe
import org.bukkit.Material
import org.bukkit.entity.Player

/** Resolves an optional recipe station from nearby loaded blocks on the server thread. */
class CraftStationService {
    fun failure(player: Player, recipe: CraftRecipe): String? {
        val requested = recipe.station.material ?: return null
        val material = Material.matchMaterial(requested) ?: return "station:$requested"
        val radius = recipe.station.radius
        val origin = player.location.block
        for (x in -radius..radius) for (y in -1..1) for (z in -radius..radius) {
            if (origin.getRelative(x, y, z).type == material) return null
        }
        return "station:$material"
    }
}
