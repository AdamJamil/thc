package thc.item

import net.minecraft.core.BlockPos
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import thc.entity.IronBoat
import thc.entity.THCEntities

class IronBoatItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)

        // Ray trace to find where player is looking (includes lava and water surfaces)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY)

        if (hitResult.type == HitResult.Type.MISS) {
            return InteractionResult.PASS
        }

        if (hitResult.type == HitResult.Type.BLOCK) {
            val hitPos = hitResult.location

            // Create iron boat entity at hit location
            val boat = IronBoat(THCEntities.IRON_BOAT, level)
            boat.snapTo(hitPos.x, hitPos.y, hitPos.z)
            boat.yRot = player.yRot

            // Check if there's space for the boat
            if (!level.noCollision(boat, boat.boundingBox)) {
                return InteractionResult.FAIL
            }

            // Only spawn on server side
            if (!level.isClientSide()) {
                level.addFreshEntity(boat)
                level.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(hitPos))

                // Shrink stack if not in creative mode
                stack.consume(1, player)
            }

            // Update stats
            player.awardStat(Stats.ITEM_USED.get(this))
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }
}
