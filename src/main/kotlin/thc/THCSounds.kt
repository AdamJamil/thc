package thc

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object THCSounds {
    @JvmField
    val BUCKLER_PARRY: SoundEvent = register("buckler_parry")

    fun init() {
        // Forces static init.
    }

    private fun register(name: String): SoundEvent {
        val id = Identifier.fromNamespaceAndPath("thc", name)
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
    }
}
