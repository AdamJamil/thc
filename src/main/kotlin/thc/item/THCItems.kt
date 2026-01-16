package thc.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item

object THCItems {
    private val toolsTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("tools")
    )

    @JvmField
    val LAND_PLOT: Item = register("land_plot") { key ->
        LandPlotItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
        )
    }

    fun init() {
        ItemGroupEvents.modifyEntriesEvent(toolsTabKey).register { entries ->
            entries.accept(LAND_PLOT)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
