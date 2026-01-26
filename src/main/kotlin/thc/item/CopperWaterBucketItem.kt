package thc.item

import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.HitResult

/**
 * Water-filled copper bucket that places flowing water (not source blocks).
 * Placed water flows and drains naturally, preventing infinite water exploits.
 * Returns empty copper bucket after use.
 */
class CopperWaterBucketItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)

        // Ray trace to find target block (not fluid)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE)

        if (hitResult.type == HitResult.Type.MISS) {
            return InteractionResult.PASS
        }

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockPos = hitResult.blockPos
            val targetPos = blockPos.relative(hitResult.direction)

            // Check permission to interact
            if (level.mayInteract(player, blockPos) && player.mayUseItemAt(targetPos, hitResult.direction, stack)) {
                if (!level.isClientSide()) {
                    // Place flowing water at level 8 (max height, spreads and drains)
                    val flowingWater = Fluids.FLOWING_WATER
                        .defaultFluidState()
                        .setValue(FlowingFluid.LEVEL, 8)
                        .createLegacyBlock()

                    level.setBlock(targetPos, flowingWater, 11)

                    // Schedule tick for water to start flowing
                    level.scheduleTick(targetPos, Fluids.FLOWING_WATER, Fluids.FLOWING_WATER.getTickDelay(level))

                    // Return empty copper bucket
                    stack.shrink(1)
                    val emptyBucket = ItemStack(THCItems.COPPER_BUCKET)
                    if (!player.inventory.add(emptyBucket)) {
                        player.drop(emptyBucket, false)
                    }

                    // Play empty sound
                    level.playSound(
                        null, player.x, player.y, player.z,
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f
                    )
                }
                return InteractionResult.SUCCESS
            }
        }

        return InteractionResult.PASS
    }
}
