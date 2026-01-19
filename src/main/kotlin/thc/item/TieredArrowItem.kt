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
        // Getter via accessor (field is private), setter is public in 1.21.11
        val currentDamage = (arrow as AbstractArrowAccessor).baseDamage
        arrow.setBaseDamage(currentDamage + damageBonus)
        return arrow
    }
}
