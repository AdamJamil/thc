package thc.bell

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import thc.item.THCItems

object BellHandler {
    @JvmStatic
    fun register() {
        UseBlockCallback.EVENT.register(UseBlockCallback { player, level, hand, hitResult ->
            val pos = hitResult.blockPos
            val blockState = level.getBlockState(pos)

            // Check if block is a bell
            if (!blockState.`is`(Blocks.BELL)) {
                return@UseBlockCallback InteractionResult.PASS
            }

            // Server-side only
            if (level.isClientSide) {
                return@UseBlockCallback InteractionResult.SUCCESS
            }

            // Check if already activated
            if (BellState.isActivated(level, pos)) {
                return@UseBlockCallback InteractionResult.SUCCESS
            }

            // Mark as activated
            BellState.setActivated(level, pos, true)

            // Drop land plot book
            val landPlot = ItemStack(THCItems.LAND_PLOT)
            val itemEntity = ItemEntity(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, landPlot)
            level.addFreshEntity(itemEntity)

            return@UseBlockCallback InteractionResult.SUCCESS
        })
    }
}
