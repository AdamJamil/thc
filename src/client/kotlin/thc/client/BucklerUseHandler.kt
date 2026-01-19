package thc.client

import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import thc.item.BucklerItem

object BucklerUseHandler {
    @JvmStatic
    fun register() {
        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            // Only run on client side
            if (!world.isClientSide) {
                return@UseItemCallback InteractionResult.PASS
            }

            // Only check bucklers in offhand
            if (hand != InteractionHand.OFF_HAND) {
                return@UseItemCallback InteractionResult.PASS
            }

            val itemStack = player.getItemInHand(hand)
            if (!BucklerItem.isBuckler(itemStack)) {
                return@UseItemCallback InteractionResult.PASS
            }

            // Block use if buckler is broken or poise is 0
            if (BucklerClientState.isBroken()) {
                return@UseItemCallback InteractionResult.FAIL
            }
            if (BucklerClientState.getPoise() <= 0.0) {
                return@UseItemCallback InteractionResult.FAIL
            }

            InteractionResult.PASS
        })
    }
}
