package thc.item

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.Level
import thc.buckler.BucklerState

class BucklerItem(properties: Properties) : Item(properties) {
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (hand != InteractionHand.OFF_HAND) {
            return InteractionResult.PASS
        }
        if (player is ServerPlayer) {
            if (BucklerState.isBroken(player)) {
                return InteractionResult.FAIL
            }
            if (BucklerState.getPoise(player) <= 0.0) {
                return InteractionResult.FAIL
            }
            BucklerState.setRaiseTick(player, player.level().gameTime)
        }
        player.startUsingItem(hand)
        return InteractionResult.CONSUME
    }

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation {
        return ItemUseAnimation.BLOCK
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return USE_DURATION
    }

    companion object {
        private const val USE_DURATION = 72000

        @JvmStatic
        fun isBuckler(stack: ItemStack): Boolean {
            return stack.item is BucklerItem
        }

        @JvmStatic
        fun isBucklerRaised(entity: LivingEntity): Boolean {
            if (!entity.isUsingItem) {
                return false
            }
            return entity.useItem.item is BucklerItem && entity.usedItemHand == InteractionHand.OFF_HAND
        }
    }
}
