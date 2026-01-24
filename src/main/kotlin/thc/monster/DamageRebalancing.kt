package thc.monster

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes

/**
 * Damage rebalancing for melee mobs in v2.3 Monster Overhaul.
 *
 * Implements:
 * - Vex: 13.5 damage (Hard) -> ~4 damage (-70.4%)
 * - Vindicator: 19.5 damage (Hard) -> ~11.7 damage (-40%)
 * - Magma Cube: 9 damage (Hard large) -> ~4.7 damage (-47.8%)
 *
 * Uses ADD_MULTIPLIED_TOTAL to preserve difficulty scaling.
 */
object DamageRebalancing {
    private val VEX_DAMAGE_ID = Identifier.fromNamespaceAndPath("thc", "vex_damage_reduction")
    private val VINDICATOR_DAMAGE_ID = Identifier.fromNamespaceAndPath("thc", "vindicator_damage_reduction")
    private val MAGMA_CUBE_DAMAGE_ID = Identifier.fromNamespaceAndPath("thc", "magma_cube_damage_reduction")

    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity !is Mob) return@register
            applyVexDamage(entity)
            applyVindicatorDamage(entity)
            applyMagmaCubeDamage(entity)
        }
    }

    /**
     * Reduce Vex damage from 13.5 (Hard armed) to ~4.
     * Multiplier: 4 / 13.5 = 0.296, modifier = -0.704
     */
    private fun applyVexDamage(mob: Mob) {
        if (mob.type != EntityType.VEX) return

        val damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE) ?: return
        if (!damageAttr.hasModifier(VEX_DAMAGE_ID)) {
            damageAttr.addTransientModifier(
                AttributeModifier(VEX_DAMAGE_ID, -0.704, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            )
        }
    }

    /**
     * Reduce Vindicator damage from 19.5 (Hard armed) to ~11.7.
     * Multiplier: 11.7 / 19.5 = 0.6, modifier = -0.4
     */
    private fun applyVindicatorDamage(mob: Mob) {
        if (mob.type != EntityType.VINDICATOR) return

        val damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE) ?: return
        if (!damageAttr.hasModifier(VINDICATOR_DAMAGE_ID)) {
            damageAttr.addTransientModifier(
                AttributeModifier(VINDICATOR_DAMAGE_ID, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            )
        }
    }

    /**
     * Reduce Magma Cube damage from 9 (Hard large) to ~4.7.
     * Applied to all sizes - preserves ratio between large/medium/small.
     * Multiplier: 4.7 / 9 = 0.522, modifier = -0.478
     */
    private fun applyMagmaCubeDamage(mob: Mob) {
        if (mob.type != EntityType.MAGMA_CUBE) return

        val damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE) ?: return
        if (!damageAttr.hasModifier(MAGMA_CUBE_DAMAGE_ID)) {
            damageAttr.addTransientModifier(
                AttributeModifier(MAGMA_CUBE_DAMAGE_ID, -0.478, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            )
        }
    }
}
