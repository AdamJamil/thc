package thc.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.ArrowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import thc.mixin.access.AbstractArrowAccessor

class TieredArrowItem(
    properties: Properties,
    private val damageBonus: Double
) : ArrowItem(properties) {

    override fun createArrow(
        level: Level,
        ammo: ItemStack,
        shooter: LivingEntity,
        weapon: ItemStack?
    ): AbstractArrow {
        val arrow = super.createArrow(level, ammo, shooter, weapon)
        // Add damage bonus to vanilla base damage (2.0)
        val accessor = arrow as AbstractArrowAccessor
        accessor.setBaseDamage(accessor.baseDamage + damageBonus)
        return arrow
    }
}
