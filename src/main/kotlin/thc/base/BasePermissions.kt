package thc.base

import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.CrossbowItem
import thc.claim.ClaimManager

/**
 * Handles base area permission enforcement.
 *
 * Bases are safe zones where no violence is permitted ("No violence indoors!").
 * This object registers event handlers that:
 * - Block player attacks against any entity while in base
 * - Block drawing bows and crossbows while in base
 */
object BasePermissions {

    private const val NO_VIOLENCE_MESSAGE = "No violence indoors!"

    /**
     * Register all base permission event handlers.
     * Should be called during mod initialization after BellHandler.register().
     */
    @JvmStatic
    fun register() {
        registerAttackBlocking()
        registerRangedWeaponBlocking()
    }

    /**
     * Block attacks against entities while player is in base area.
     */
    private fun registerAttackBlocking() {
        AttackEntityCallback.EVENT.register(AttackEntityCallback { player, world, _, _, _ ->
            // Skip on client side
            if (world.isClientSide) {
                return@AttackEntityCallback InteractionResult.PASS
            }

            // Get server from level
            val level = world as? ServerLevel ?: return@AttackEntityCallback InteractionResult.PASS
            val server = level.server

            // Check if player is in base area
            if (ClaimManager.isInBase(server, player.blockPosition())) {
                player.displayClientMessage(
                    Component.literal(NO_VIOLENCE_MESSAGE).withStyle(ChatFormatting.RED),
                    true  // action bar
                )
                return@AttackEntityCallback InteractionResult.FAIL
            }

            InteractionResult.PASS
        })
    }

    /**
     * Block drawing bows and crossbows while player is in base area.
     */
    private fun registerRangedWeaponBlocking() {
        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            // Skip on client side
            if (world.isClientSide) {
                return@UseItemCallback InteractionResult.PASS
            }

            // Get the item being used (check the hand parameter)
            val itemStack = player.getItemInHand(hand)
            val item = itemStack.item

            // Only block bows and crossbows
            if (item !is BowItem && item !is CrossbowItem) {
                return@UseItemCallback InteractionResult.PASS
            }

            // Get server from level
            val level = world as? ServerLevel ?: return@UseItemCallback InteractionResult.PASS
            val server = level.server

            // Check if player is in base area
            if (ClaimManager.isInBase(server, player.blockPosition())) {
                player.displayClientMessage(
                    Component.literal(NO_VIOLENCE_MESSAGE).withStyle(ChatFormatting.RED),
                    true  // action bar
                )
                return@UseItemCallback InteractionResult.FAIL
            }

            InteractionResult.PASS
        })
    }
}
