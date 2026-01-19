package thc.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item

object THCArrows {
    private val combatTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("combat")
    )

    @JvmField
    val IRON_ARROW: Item = register("iron_arrow") { key ->
        TieredArrowItem(
            Item.Properties().setId(key),
            1.0  // +1 damage
        )
    }

    @JvmField
    val DIAMOND_ARROW: Item = register("diamond_arrow") { key ->
        TieredArrowItem(
            Item.Properties().setId(key),
            2.0  // +2 damage
        )
    }

    @JvmField
    val NETHERITE_ARROW: Item = register("netherite_arrow") { key ->
        TieredArrowItem(
            Item.Properties().setId(key),
            3.0  // +3 damage
        )
    }

    fun init() {
        ItemGroupEvents.modifyEntriesEvent(combatTabKey).register { entries ->
            entries.accept(IRON_ARROW)
            entries.accept(DIAMOND_ARROW)
            entries.accept(NETHERITE_ARROW)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
