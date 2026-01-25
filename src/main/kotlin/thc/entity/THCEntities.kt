package thc.entity

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.level.Level

object THCEntities {
    @JvmField
    val IRON_BOAT: EntityType<IronBoat> = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("thc", "iron_boat"),
        EntityType.Builder.of({ type: EntityType<IronBoat>, level: Level ->
            IronBoat(type, level)
        }, MobCategory.MISC)
            .sized(1.375f, 0.5625f)  // Same as vanilla boat
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("thc", "iron_boat")))
    )

    fun init() {
        // Registration happens at object init, this just forces class loading
    }
}
