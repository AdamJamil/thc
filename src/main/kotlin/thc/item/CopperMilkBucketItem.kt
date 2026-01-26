package thc.item

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.Level

/**
 * Milk-filled copper bucket that can be consumed to clear all status effects.
 * Returns empty copper bucket after drinking.
 */
class CopperMilkBucketItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        player.startUsingItem(hand)
        return InteractionResult.CONSUME
    }

    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        if (entity is ServerPlayer) {
            // Remove all status effects (vanilla milk behavior)
            entity.removeAllEffects()
        }

        // Return empty copper bucket
        return if (entity is Player && !entity.abilities.instabuild) {
            stack.shrink(1)
            if (stack.isEmpty) {
                ItemStack(THCItems.COPPER_BUCKET)
            } else {
                if (!entity.inventory.add(ItemStack(THCItems.COPPER_BUCKET))) {
                    entity.drop(ItemStack(THCItems.COPPER_BUCKET), false)
                }
                stack
            }
        } else {
            stack
        }
    }

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation {
        return ItemUseAnimation.DRINK
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return 32  // Same as vanilla milk bucket
    }
}
