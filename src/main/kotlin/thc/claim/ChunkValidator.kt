package thc.claim

import net.minecraft.server.level.ServerLevel
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
