package thc.world

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.ChunkPos
import thc.claim.ChunkValidator

/**
 * Handles block break protection in village chunks.
 *
 * Implements:
 * - BREAK-05: Player cannot break blocks in village chunks
 * - BREAK-06: Player CAN break ores in village chunks (exception)
 * - BREAK-07: Player CAN break allowlist blocks in village chunks (exception)
 *
 * This handler should be registered BEFORE MiningFatigue so that blocked breaks
 * don't trigger fatigue application.
 */
object VillageProtection {

    /**
     * Registers the village block break protection handler.
     */
    fun register() {
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, blockEntity ->
            // Skip client-side processing
            if (level.isClientSide) {
                return@register true
            }

            val serverLevel = level as ServerLevel
            val chunkPos = ChunkPos(pos)

            // Only apply protection in village chunks
            if (!ChunkValidator.isVillageChunk(serverLevel, chunkPos)) {
                return@register true
            }

            // BREAK-06: Allow breaking ores in village chunks
            if (isOre(state)) {
                return@register true
            }

            // BREAK-07: Allow breaking allowlist blocks in village chunks
            if (WorldRestrictions.ALLOWED_BLOCKS.contains(state.block)) {
                return@register true
            }

            // BREAK-05: Block breaking in village chunks
            false
        }
    }

    /**
     * Checks if a block state is an ore block.
     *
     * Uses vanilla BlockTags for comprehensive ore detection covering:
     * - Coal, Iron, Copper, Gold ores
     * - Redstone, Lapis, Diamond, Emerald ores
     * - Both regular and deepslate variants
     * - Nether gold ore and nether quartz ore
     */
    private fun isOre(state: net.minecraft.world.level.block.state.BlockState): Boolean {
        return state.`is`(BlockTags.COAL_ORES) ||
            state.`is`(BlockTags.IRON_ORES) ||
            state.`is`(BlockTags.COPPER_ORES) ||
            state.`is`(BlockTags.GOLD_ORES) ||
            state.`is`(BlockTags.REDSTONE_ORES) ||
            state.`is`(BlockTags.LAPIS_ORES) ||
            state.`is`(BlockTags.DIAMOND_ORES) ||
            state.`is`(BlockTags.EMERALD_ORES)
    }
}
