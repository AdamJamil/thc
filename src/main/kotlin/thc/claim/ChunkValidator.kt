package thc.claim

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.StructureTags
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.Heightmap

/**
 * Validates chunks for claiming eligibility.
 *
 * Enforces:
 * - CLAIM-02: Terrain flatness (max 10 block Y difference)
 * - CLAIM-08: Village detection (cannot claim village chunks)
 */
object ChunkValidator {

    private const val MAX_HEIGHT_DIFFERENCE = 10
    private val logger = org.slf4j.LoggerFactory.getLogger("thc.ChunkValidator")

    /**
     * Validates terrain flatness for chunk claiming.
     *
     * Algorithm:
     * 1. Iterate all 256 surface positions (16x16 chunk)
     * 2. Get surface Y for each position using heightmap
     * 3. Track min/max Y values
     * 4. If difference > 10: return Failure
     * 5. Otherwise: return Success with lowest surface Y (for base floor calculation)
     *
     * @param level The server level to check
     * @param chunkPos The chunk position to validate
     * @return ValidationResult.Success with lowestSurfaceY or ValidationResult.Failure with message
     */
    fun validateTerrain(level: ServerLevel, chunkPos: ChunkPos): ValidationResult {
        val startX = chunkPos.minBlockX
        val startZ = chunkPos.minBlockZ

        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE

        // Iterate all 256 surface positions in the chunk
        for (x in 0 until 16) {
            for (z in 0 until 16) {
                val worldX = startX + x
                val worldZ = startZ + z

                // getHeight returns Y of first air block, subtract 1 for actual surface
                val surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1

                if (surfaceY < minY) minY = surfaceY
                if (surfaceY > maxY) maxY = surfaceY
            }
        }

        val heightDifference = maxY - minY

        return if (heightDifference > MAX_HEIGHT_DIFFERENCE) {
            ValidationResult.Failure("The chunk's surface is not flat enough!")
        } else {
            ValidationResult.Success(lowestSurfaceY = minY)
        }
    }

    /**
     * Checks if a chunk contains any village structure.
     *
     * Uses comprehensive structure detection:
     * 1. Check if any village structure STARTS in this chunk
     * 2. Check multiple positions across the chunk for village structure PIECES
     *    (villages extend across multiple chunks)
     *
     * @param level The server level to check
     * @param chunkPos The chunk position to validate
     * @return true if the chunk contains or is part of a village structure
     */
    fun isVillageChunk(level: ServerLevel, chunkPos: ChunkPos): Boolean {
        val structureManager = level.structureManager()
        val chunk = level.getChunk(chunkPos.x, chunkPos.z)

        logger.info("Checking village for chunk ${chunkPos.x}, ${chunkPos.z}")

        // Method 1: Check structure starts in this chunk
        logger.info("  allStarts count: ${chunk.allStarts.size}")
        for ((structure, start) in chunk.allStarts) {
            val structureKey = level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getKey(structure)

            logger.info("  Found structure start: $structureKey")

            if (structureKey != null) {
                val holder = level.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(structureKey)

                if (holder.isPresent && holder.get().`is`(StructureTags.VILLAGE)) {
                    logger.info("  -> Is a village! (via allStarts)")
                    return true
                }
            }
        }

        // Method 2: Check structure references (structures that extend into this chunk)
        logger.info("  allReferences count: ${chunk.allReferences.size}")
        for ((structure, refs) in chunk.allReferences) {
            val structureKey = level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getKey(structure)

            logger.info("  Found structure reference: $structureKey (${refs.size} refs)")

            if (structureKey != null) {
                val holder = level.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(structureKey)

                if (holder.isPresent && holder.get().`is`(StructureTags.VILLAGE)) {
                    logger.info("  -> Is a village! (via allReferences)")
                    return true
                }
            }
        }

        // Method 3: Sample multiple positions across the chunk for structure pieces
        val yLevels = listOf(64, 70, 75, 80, 63, 60, 55)
        val xOffsets = listOf(2, 8, 14)
        val zOffsets = listOf(2, 8, 14)

        for (y in yLevels) {
            for (xOff in xOffsets) {
                for (zOff in zOffsets) {
                    val checkPos = BlockPos(chunkPos.minBlockX + xOff, y, chunkPos.minBlockZ + zOff)
                    val structureAt = structureManager.getStructureWithPieceAt(checkPos, StructureTags.VILLAGE)
                    if (structureAt.isValid) {
                        logger.info("  -> Is a village! (via getStructureWithPieceAt at $checkPos)")
                        return true
                    }
                }
            }
        }

        logger.info("  -> NOT a village chunk")
        return false
    }
}

/**
 * Result of chunk validation for claiming.
 */
sealed class ValidationResult {
    /**
     * Validation passed. Contains data needed for claim setup.
     * @param lowestSurfaceY The lowest surface Y coordinate in the chunk (for CLAIM-07 base floor)
     */
    data class Success(val lowestSurfaceY: Int) : ValidationResult()

    /**
     * Validation failed with reason.
     * @param reason Human-readable failure message to display to player
     */
    data class Failure(val reason: String) : ValidationResult()
}
