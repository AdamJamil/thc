package thc.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

object THCBucklers {
    private val combatTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("combat")
    )

    @JvmField
    val STONE_BUCKLER: Item = register("stone_buckler") { key ->
        BucklerItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(150)
                .repairable(Items.STONE)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
        )
    }

    @JvmField
    val IRON_BUCKLER: Item = register("iron_buckler") { key ->
        BucklerItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(300)
                .repairable(Items.IRON_INGOT)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
        )
    }

    @JvmField
    val GOLD_BUCKLER: Item = register("gold_buckler") { key ->
        BucklerItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(140)
                .repairable(Items.GOLD_INGOT)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
        )
    }

    @JvmField
    val DIAMOND_BUCKLER: Item = register("diamond_buckler") { key ->
        BucklerItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(660)
                .repairable(Items.DIAMOND)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
        )
    }

    @JvmField
    val NETHERITE_BUCKLER: Item = register("netherite_buckler") { key ->
        BucklerItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(740)
                .repairable(Items.NETHERITE_INGOT)
                .equippableUnswappable(EquipmentSlot.OFFHAND)
        )
    }

    fun init() {
        ItemGroupEvents.modifyEntriesEvent(combatTabKey).register { entries ->
            entries.accept(STONE_BUCKLER)
            entries.accept(IRON_BUCKLER)
            entries.accept(GOLD_BUCKLER)
            entries.accept(DIAMOND_BUCKLER)
            entries.accept(NETHERITE_BUCKLER)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }

}
