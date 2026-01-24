package thc.monster

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.Creeper

/**
 * Global monster modifications for v2.3 Monster Overhaul.
 *
 * Implements:
 * - FR-01: 20% speed increase for hostile mobs (with exclusions)
 * - FR-04: Baby zombie speed normalization
 */
object MonsterModifications {
    private val SPEED_BOOST_ID = Identifier.fromNamespaceAndPath("thc", "monster_speed_boost")
    private val BABY_NORMALIZE_ID = Identifier.fromNamespaceAndPath("thc", "baby_zombie_normalize")

    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity !is Mob) return@register
            if (entity.type.category != MobCategory.MONSTER) return@register

            applySpeedBoost(entity)
            normalizeBabyZombieSpeed(entity)
        }
    }

    /**
     * Apply 20% speed boost to hostile mobs.
     *
     * Exclusions:
     * - Creepers (unchanged speed)
     * - Baby zombies (handled separately)
     * - Bosses (EnderDragon, WitherBoss - hardcoded behaviors break)
     */
    private fun applySpeedBoost(mob: Mob) {
        // Exclusions
        if (mob is Creeper) return
        if (mob.type == EntityType.ZOMBIE && mob.isBaby) return
        if (mob is EnderDragon || mob is WitherBoss) return

        val speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        if (!speedAttr.hasModifier(SPEED_BOOST_ID)) {
            speedAttr.addTransientModifier(
                AttributeModifier(SPEED_BOOST_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            )
        }
    }

    /**
     * Normalize baby zombie speed to match adult zombies.
     *
     * Applies -0.5 counter-modifier to negate vanilla's +0.5 BABY_SPEED_BONUS.
     * Result: baby zombies move at same speed as adult zombies.
     */
    private fun normalizeBabyZombieSpeed(mob: Mob) {
        if (mob.type != EntityType.ZOMBIE || !mob.isBaby) return

        val speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        if (!speedAttr.hasModifier(BABY_NORMALIZE_ID)) {
            speedAttr.addTransientModifier(
                AttributeModifier(BABY_NORMALIZE_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            )
        }
    }
}
