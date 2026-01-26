package thc.item

import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.HitResult

/**
 * Empty copper bucket that can only pick up water, not lava or other fluids.
 * Provides early-game water transport before iron is available.
 */
class CopperBucketItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)

        // Ray trace to find target fluid source block
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY)

        if (hitResult.type == HitResult.Type.MISS) {
            return InteractionResult.PASS
        }

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockPos = hitResult.blockPos
            val fluidState = level.getFluidState(blockPos)

            // Only allow picking up water source blocks
            if (fluidState.`is`(FluidTags.WATER) && fluidState.isSource) {
                if (!level.isClientSide()) {
                    // Remove water source block
                    level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11)

                    // Replace bucket with water bucket
                    stack.shrink(1)
                    val waterBucket = ItemStack(THCItems.COPPER_BUCKET_OF_WATER)
                    if (!player.inventory.add(waterBucket)) {
                        player.drop(waterBucket, false)
                    }

                    // Play fill sound
                    level.playSound(
                        null, player.x, player.y, player.z,
                        SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f
                    )
                }
                return InteractionResult.SUCCESS
            }

            // Lava or other fluids - silent fail (no animation)
            if (fluidState.`is`(FluidTags.LAVA) || !fluidState.isEmpty) {
                return InteractionResult.FAIL
            }
        }

        return InteractionResult.PASS
    }
}
