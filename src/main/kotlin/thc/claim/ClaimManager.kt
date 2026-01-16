package thc.claim

import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.ChunkPos

/**
 * Singleton providing the claim query and mutation API.
 *
 * All methods retrieve ClaimData via lazy getOrCreate pattern,
 * no initialization required.
 */
object ClaimManager {

    /**
     * Check if a chunk is claimed.
     *
     * @param server The Minecraft server instance
     * @param chunkPos The chunk position to check
     * @return true if the chunk is claimed
     */
    fun isClaimed(server: MinecraftServer, chunkPos: ChunkPos): Boolean {
        val state = ClaimData.getServerState(server)
        return state.claimedChunks.contains(chunkPos.toLong())
    }

    /**
     * Add a claim for a chunk.
     *
     * @param server The Minecraft server instance
     * @param chunkPos The chunk position to claim
     * @param baseFloorY The base floor Y level for this claim (lowest surface Y - 10)
     * @return true if the claim was added, false if already claimed
     */
    fun addClaim(server: MinecraftServer, chunkPos: ChunkPos, baseFloorY: Int): Boolean {
        val state = ClaimData.getServerState(server)
        val chunkKey = chunkPos.toLong()

        if (state.claimedChunks.contains(chunkKey)) {
            return false
        }

        state.claimedChunks.add(chunkKey)
        state.baseFloors[chunkKey] = baseFloorY
        state.setDirty()
        return true
    }

    /**
     * Get the base floor Y level for a claimed chunk.
     *
     * @param server The Minecraft server instance
     * @param chunkPos The chunk position to query
     * @return The base floor Y level, or null if the chunk is not claimed
     */
    fun getBaseFloorY(server: MinecraftServer, chunkPos: ChunkPos): Int? {
        val state = ClaimData.getServerState(server)
        return state.baseFloors[chunkPos.toLong()]
    }

    /**
     * Check if a block position is within a base area.
     *
     * A position is within a base if:
     * 1. The chunk containing the position is claimed
     * 2. The Y coordinate is at or above the base floor Y for that chunk
     *
     * @param server The Minecraft server instance
     * @param pos The block position to check
     * @return true if the position is within a base area
     */
    fun isInBase(server: MinecraftServer, pos: BlockPos): Boolean {
        val chunkPos = ChunkPos(pos)
        val baseFloorY = getBaseFloorY(server, chunkPos) ?: return false
        return pos.y >= baseFloorY
    }
}
