package thc.bow

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item

object THCBows {
    private val combatTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("combat")
    )

    @JvmField
    val BREEZE_BOW: Item = register("breeze_bow") { key ->
        BreezeBowItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(384)
        )
    }

    fun init() {
        ItemGroupEvents.modifyEntriesEvent(combatTabKey).register { entries ->
            entries.accept(BREEZE_BOW)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
