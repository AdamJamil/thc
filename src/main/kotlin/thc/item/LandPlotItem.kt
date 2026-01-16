package thc.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ChunkPos
import thc.claim.ChunkValidator
import thc.claim.ClaimManager
import thc.claim.ValidationResult

/**
 * Land plot book item used as currency for claiming chunks.
 *
 * This item represents a land plot claim book. Players must find bells in the world
 * and use them to acquire these books, which can then be used to claim chunks.
 *
 * Non-stackable to maintain scarcity and value.
 */
class LandPlotItem(properties: Properties) : Item(properties) {

    /**
     * Handles using the land plot book on a block to claim the chunk.
     *
     * Claiming flow:
     * 1. Check if chunk is already claimed (CLAIM-05)
     * 2. Check if chunk is a village chunk (CLAIM-04)
     * 3. Validate terrain flatness (CLAIM-02, CLAIM-03)
     * 4. Calculate base floor Y from lowest surface Y - 10 (CLAIM-07)
     * 5. Register the claim (CLAIM-01)
     * 6. Consume the item (CLAIM-06)
     * 7. Send success message
     */
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val player = context.player ?: return InteractionResult.PASS

        // Server-side only - return SUCCESS on client to show arm swing
        if (level.isClientSide || level !is ServerLevel) {
            return InteractionResult.SUCCESS
        }

        val server = level.server
        val chunkPos = ChunkPos(pos)

        // Check if already claimed (CLAIM-05)
        if (ClaimManager.isClaimed(server, chunkPos)) {
            sendFailureMessage(player, "This chunk is already claimed!")
            return InteractionResult.FAIL
        }

        // Check for village (CLAIM-04)
        if (ChunkValidator.isVillageChunk(level, chunkPos)) {
            sendFailureMessage(player, "Cannot claim village chunks!")
            return InteractionResult.FAIL
        }

        // Validate terrain flatness (CLAIM-02, CLAIM-03)
        when (val result = ChunkValidator.validateTerrain(level, chunkPos)) {
            is ValidationResult.Failure -> {
                sendFailureMessage(player, result.reason)
                return InteractionResult.FAIL
            }
            is ValidationResult.Success -> {
                // Calculate base floor Y (CLAIM-07): lowest surface Y - 10
                val baseFloorY = result.lowestSurfaceY - 10

                // Register claim (CLAIM-01)
                ClaimManager.addClaim(server, chunkPos, baseFloorY)

                // Consume item only on success (CLAIM-06)
                context.itemInHand.shrink(1)

                // Send success message
                sendSuccessMessage(player, chunkPos)

                return InteractionResult.SUCCESS
            }
        }
    }

    private fun sendFailureMessage(player: Player, message: String) {
        player.displayClientMessage(
            Component.literal(message).withStyle(ChatFormatting.RED),
            true  // actionBar = true for less intrusive display
        )
    }

    private fun sendSuccessMessage(player: Player, chunkPos: ChunkPos) {
        player.displayClientMessage(
            Component.literal("Claimed chunk at (${chunkPos.x}, ${chunkPos.z})!")
                .withStyle(ChatFormatting.GREEN),
            true
        )
    }
}
