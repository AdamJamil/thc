package thc.world

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.tags.StructureTags

/**
 * Handles block break protection inside village structures.
 *
 * Implements:
 * - BREAK-05: Player cannot break blocks inside village structure bounding boxes
 * - BREAK-06: Player CAN break ores inside village structures (exception)
 * - BREAK-07: Player CAN break allowlist blocks inside village structures (exception)
 *
 * Uses position-based structure detection via getStructureWithPieceAt() which
 * checks if a block position falls within any village structure piece's bounding box.
 * This allows underground traversal below villages while protecting actual structures.
 *
 * This handler should be registered BEFORE MiningFatigue so that blocked breaks
 * don't trigger fatigue application.
 */
private val logger = org.slf4j.LoggerFactory.getLogger("thc.VillageProtection")

object VillageProtection {

    /**
     * Registers the village block break protection handler.
     */
    fun register() {
        logger.info("VillageProtection handler registered!")
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, blockEntity ->
            // Skip client-side processing
            if (level.isClientSide) {
                return@register true
            }

            val serverLevel = level as ServerLevel

            logger.info("VillageProtection checking block break at $pos")

            // Only apply protection inside village structures
            val isVillage = isInsideVillageStructure(serverLevel, pos)
            logger.info("  isInsideVillageStructure result: $isVillage")

            if (!isVillage) {
                logger.info("  -> ALLOWING (not inside village structure)")
                return@register true
            }

            // BREAK-06: Allow breaking ores in village structures
            if (isOre(state)) {
                logger.info("  -> ALLOWING (is ore)")
                return@register true
            }

            // BREAK-07: Allow breaking allowlist blocks in village structures
            if (WorldRestrictions.ALLOWED_BLOCKS.contains(state.block)) {
                logger.info("  -> ALLOWING (is allowlist block)")
                return@register true
            }

            // BREAK-05: Block breaking inside village structures
            logger.info("  -> BLOCKING break inside village structure!")
            false
        }
    }

    /**
     * Checks if a position is inside a village structure piece bounding box.
     *
     * Uses StructureManager.getStructureWithPieceAt() which internally checks
     * if the position falls within any structure piece's BoundingBox for
     * villages in the chunk.
     *
     * @param level The server level to check
     * @param pos The block position to check
     * @return true if the position is inside any village structure piece
     */
    private fun isInsideVillageStructure(level: ServerLevel, pos: BlockPos): Boolean {
        val structureManager = level.structureManager()
        val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
        return structureAt.isValid
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
