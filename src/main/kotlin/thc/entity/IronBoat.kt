package thc.entity

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.boat.Boat
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.function.Supplier

class IronBoat(type: EntityType<out Boat>, level: Level) : Boat(type, level, Supplier { Items.AIR }) {
    companion object {
        // Will be set after THCItems initialization
        @JvmStatic
        var ironBoatDropItem: Item = Items.AIR
    }

    // Fire immunity - boat won't burn
    override fun fireImmune(): Boolean = true

    override fun isOnFire(): Boolean = false

    // Damage filtering - only allow player attacks
    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, amount: Float): Boolean {
        // Only allow damage from player attacks
        if (damageSource.entity is Player) {
            return super.hurtServer(serverLevel, damageSource, amount)
        }
        // Block all other damage: lava, fire, mob attacks, cacti, collisions
        return false
    }

    // Override getWaterLevelAbove to include lava - this is public and can be overridden
    override fun getWaterLevelAbove(): Float {
        // Get vanilla water level
        val waterLevel = super.getWaterLevelAbove()

        // Also check lava level
        val lavaLevel = getLavaLevelAbove()

        // Return the higher of water level or lava level
        return waterLevel.coerceAtLeast(lavaLevel)
    }

    // Helper method to get lava level above boat
    private fun getLavaLevelAbove(): Float {
        val aabb = boundingBox
        val minX = net.minecraft.util.Mth.floor(aabb.minX)
        val maxX = net.minecraft.util.Mth.ceil(aabb.maxX)
        val minZ = net.minecraft.util.Mth.floor(aabb.minZ)
        val maxZ = net.minecraft.util.Mth.ceil(aabb.maxZ)
        val y = net.minecraft.util.Mth.floor(aabb.maxY)

        val blockPos = net.minecraft.core.BlockPos.MutableBlockPos()
        var maxLavaLevel = Float.NEGATIVE_INFINITY

        for (x in minX until maxX) {
            for (z in minZ until maxZ) {
                blockPos.set(x, y, z)
                val fluidState = level().getFluidState(blockPos)
                if (fluidState.`is`(net.minecraft.tags.FluidTags.LAVA)) {
                    val lavaHeight = y.toFloat() + fluidState.getHeight(level(), blockPos)
                    maxLavaLevel = maxLavaLevel.coerceAtLeast(lavaHeight)
                }
            }
        }

        return maxLavaLevel
    }

    // Override destroy to drop item with velocity toward attacker
    // MC 1.21.11 has a different signature
    override fun destroy(serverLevel: ServerLevel, damageSource: DamageSource) {
        // Don't call super - we want custom drop behavior
        if (!level().isClientSide()) {
            // Create item entity
            val itemStack = ItemStack(ironBoatDropItem)
            val itemEntity = ItemEntity(
                level(),
                x,
                y,
                z,
                itemStack
            )

            // Calculate velocity toward attacker if one exists
            val attacker = damageSource.entity
            if (attacker != null) {
                // Direction from boat to attacker
                val direction = attacker.position().subtract(position()).normalize()
                // Apply velocity: horizontal speed + small upward component
                itemEntity.setDeltaMovement(
                    direction.x * 0.5,
                    0.3,  // Upward component
                    direction.z * 0.5
                )
                // Mark for client sync
                itemEntity.hurtMarked = true
            }

            level().addFreshEntity(itemEntity)
        }

        // Remove the boat entity
        kill(serverLevel)
    }
}
