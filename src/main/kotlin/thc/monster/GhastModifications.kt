package thc.monster

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball

/**
 * Ghast behavior modifications for v2.3 Monster Overhaul.
 *
 * Implements:
 * - FR-07: 50% velocity boost for Ghast fireballs
 *
 * Note: This boosts ALL LargeFireballs at spawn time.
 * Vanilla only spawns LargeFireball from Ghasts, so this is equivalent to
 * "boost Ghast fireballs". Deflected fireballs inherit the boost.
 */
object GhastModifications {

    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity !is LargeFireball) return@register

            // Scale velocity by 1.5 (50% faster)
            val velocity = entity.deltaMovement
            entity.setDeltaMovement(velocity.scale(1.5))
        }
    }
}
