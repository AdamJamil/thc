package thc.claim

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType

/**
 * Persistent storage for claimed chunk data at the world level.
 *
 * Stores:
 * - Set of claimed chunks (as longs from ChunkPos.toLong())
 * - Base floor Y for each claimed chunk (lowest surface Y - 10)
 */
class ClaimData(
    claimedChunks: List<Long> = emptyList(),
    baseFloors: Map<String, Int> = emptyMap()
) : SavedData() {

    val claimedChunks: MutableSet<Long> = claimedChunks.toMutableSet()
    val baseFloors: MutableMap<Long, Int> = baseFloors
        .mapKeys { (k, _) -> k.toLong() }
        .toMutableMap()

    companion object {
        private const val DATA_NAME = "thc_claims"

        private val CODEC: Codec<ClaimData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.LONG.listOf().fieldOf("claimed_chunks").forGetter { it.claimedChunks.toList() },
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("base_floors").forGetter {
                    it.baseFloors.mapKeys { (k, _) -> k.toString() }
                }
            ).apply(instance, ::ClaimData)
        }

        val TYPE: SavedDataType<ClaimData> = SavedDataType(
            DATA_NAME,
            ::ClaimData,
            CODEC,
            DataFixTypes.LEVEL
        )

        @JvmStatic
        fun getServerState(server: MinecraftServer): ClaimData {
            val overworld = server.getLevel(Level.OVERWORLD)
                ?: throw IllegalStateException("Overworld not loaded")
            return overworld.dataStorage.computeIfAbsent(TYPE)
        }
    }
}
