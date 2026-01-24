package thc.monster

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Vex
import net.minecraft.world.item.ItemStack

/**
 * Simple entity behavior modifications for v2.3 Monster Overhaul.
 *
 * Implements:
 * - FR-12: Vex health reduction (14 HP -> 8 HP)
 * - FR-13: Vex sword removal
 */
object SimpleEntityBehaviors {
    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is Vex) {
                modifyVex(entity)
            }
        }
    }

    /**
     * Modify vex on spawn: reduce health to 8 HP and remove sword.
     *
     * FR-12: Health reduction makes vexes appropriately fragile as summoned minions.
     * FR-13: Sword removal reduces visual clutter and aligns with simplified aesthetics.
     */
    private fun modifyVex(vex: Vex) {
        // FR-12: Reduce max health to 8 HP (4 hearts)
        val healthAttr = vex.getAttribute(Attributes.MAX_HEALTH)
        if (healthAttr != null && healthAttr.baseValue != 8.0) {
            healthAttr.baseValue = 8.0
            // If current health exceeds new max, reduce it
            if (vex.health > 8.0f) {
                vex.health = 8.0f
            }
        }

        // FR-13: Remove sword from mainhand
        vex.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
    }
}
