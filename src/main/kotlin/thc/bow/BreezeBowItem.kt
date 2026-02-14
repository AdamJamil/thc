package thc.bow

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import thc.playerclass.ClassManager
import thc.stage.StageManager

class BreezeBowItem(properties: Properties) : BowItem(properties) {

    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResult {
        if (player is ServerPlayer) {
            // Breeze Bow gate: Support class at Stage 2+ only
            val boonLevel = StageManager.getBoonLevel(player)
            if (!ClassManager.isSupport(player) || boonLevel < 2) {
                player.displayClientMessage(
                    Component.literal("The bow gusts are beyond your control.")
                        .withStyle(ChatFormatting.RED),
                    true
                )
                return InteractionResult.FAIL
            }
        }
        return super.use(world, player, hand)
    }

    override fun releaseUsing(stack: ItemStack, level: Level, entity: LivingEntity, timeLeft: Int): Boolean {
        // Scale charge time by 1/0.75 = 1.333x to make draw 0.75x faster
        // actualCharge = useDuration - timeLeft (ticks since draw started)
        // scaledCharge = actualCharge / 0.75 (faster progression through power curve)
        // At 15 real ticks, scaledCharge = 20 -> full power (vanilla full draw)
        // adjustedTimeLeft = useDuration - scaledCharge (what vanilla sees)
        val useDuration = getUseDuration(stack, entity)
        val actualCharge = useDuration - timeLeft
        val scaledCharge = (actualCharge / 0.75f).toInt()
        val adjustedTimeLeft = useDuration - scaledCharge
        return super.releaseUsing(stack, level, entity, adjustedTimeLeft)
    }
}
