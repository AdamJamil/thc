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

    private val foodTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("food_and_drinks")
    )

    @JvmField
    val LAND_PLOT: Item = register("land_plot") { key ->
        LandPlotItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
        )
    }

    @JvmField
    val BLAST_TOTEM: Item = register("blast_totem") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
        )
    }

    @JvmField
    val HONEY_APPLE: Item = register("honey_apple") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(64)
        )
    }

    fun init() {
        ItemGroupEvents.modifyEntriesEvent(toolsTabKey).register { entries ->
            entries.accept(LAND_PLOT)
            entries.accept(BLAST_TOTEM)
        }
        ItemGroupEvents.modifyEntriesEvent(foodTabKey).register { entries ->
            entries.accept(HONEY_APPLE)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
