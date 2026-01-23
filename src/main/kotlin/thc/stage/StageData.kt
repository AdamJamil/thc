package thc.stage

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType

class StageData(currentStage: Int = 1) : SavedData() {

    var currentStage: Int = currentStage
        private set

    fun advanceStage(): Boolean {
        if (currentStage >= 5) return false
        currentStage++
        setDirty()
        return true
    }

    companion object {
        private const val DATA_NAME = "thc_stage"

        private val CODEC: Codec<StageData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("current_stage").forGetter { it.currentStage }
            ).apply(instance, ::StageData)
        }

        val TYPE: SavedDataType<StageData> = SavedDataType(
            DATA_NAME,
            ::StageData,
            CODEC,
            DataFixTypes.LEVEL
        )

        @JvmStatic
        fun getServerState(server: MinecraftServer): StageData {
            val overworld = server.getLevel(Level.OVERWORLD)
                ?: throw IllegalStateException("Overworld not loaded")
            return overworld.dataStorage.computeIfAbsent(TYPE)
        }
    }
}
