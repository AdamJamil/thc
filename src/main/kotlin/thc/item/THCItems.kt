package thc.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.StructureTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.DamageResistant
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.Structure

object THCItems {
    private val toolsTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("tools_and_utilities")
    )

    private val foodTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("food_and_drinks")
    )

    // Structure tags for locators (custom tags for structures without StructureTags constants)
    private val FORTRESS_TAG: TagKey<Structure> = TagKey.create(
        Registries.STRUCTURE,
        Identifier.withDefaultNamespace("fortress")
    )
    private val BASTION_TAG: TagKey<Structure> = TagKey.create(
        Registries.STRUCTURE,
        Identifier.withDefaultNamespace("bastion_remnant")
    )
    private val PILLAGER_OUTPOST_TAG: TagKey<Structure> = TagKey.create(
        Registries.STRUCTURE,
        Identifier.withDefaultNamespace("pillager_outpost")
    )
    private val ANCIENT_CITY_TAG: TagKey<Structure> = TagKey.create(
        Registries.STRUCTURE,
        Identifier.withDefaultNamespace("ancient_city")
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
                .food(FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(0.170625f)
                    .build())
        )
    }

    @JvmField
    val IRON_BOAT: Item = register("iron_boat") { key ->
        IronBoatItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .component(DataComponents.DAMAGE_RESISTANT, DamageResistant(DamageTypeTags.IS_FIRE))
        )
    }

    @JvmField
    val COPPER_BUCKET: Item = register("copper_bucket") { key ->
        CopperBucketItem(
            Item.Properties()
                .setId(key)
                .stacksTo(16)  // Empty buckets can stack
        )
    }

    @JvmField
    val COPPER_BUCKET_OF_WATER: Item = register("copper_bucket_of_water") { key ->
        CopperWaterBucketItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .craftRemainder(COPPER_BUCKET)  // Returns empty bucket in crafting
        )
    }

    @JvmField
    val COPPER_BUCKET_OF_MILK: Item = register("copper_bucket_of_milk") { key ->
        CopperMilkBucketItem(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .craftRemainder(COPPER_BUCKET)
        )
    }

    @JvmField
    val SOUL_DUST: Item = register("soul_dust") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(64)
        )
    }

    @JvmField
    val DOUGH: Item = register("dough") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(64)
        )
    }

    // Structure Locators - compass-style items that point to structures
    @JvmField
    val FORTRESS_LOCATOR: Item = register("fortress_locator") { key ->
        StructureLocatorItem(
            Item.Properties().setId(key).stacksTo(1),
            FORTRESS_TAG,
            Level.NETHER
        )
    }

    @JvmField
    val BASTION_LOCATOR: Item = register("bastion_locator") { key ->
        StructureLocatorItem(
            Item.Properties().setId(key).stacksTo(1),
            BASTION_TAG,
            Level.NETHER
        )
    }

    @JvmField
    val TRIAL_CHAMBER_LOCATOR: Item = register("trial_chamber_locator") { key ->
        StructureLocatorItem(
            Item.Properties().setId(key).stacksTo(1),
            StructureTags.ON_TRIAL_CHAMBERS_MAPS,
            Level.OVERWORLD
        )
    }

    @JvmField
    val PILLAGER_OUTPOST_LOCATOR: Item = register("pillager_outpost_locator") { key ->
        StructureLocatorItem(
            Item.Properties().setId(key).stacksTo(1),
            PILLAGER_OUTPOST_TAG,
            Level.OVERWORLD
        )
    }

    @JvmField
    val ANCIENT_CITY_LOCATOR: Item = register("ancient_city_locator") { key ->
        StructureLocatorItem(
            Item.Properties().setId(key).stacksTo(1),
            ANCIENT_CITY_TAG,
            Level.OVERWORLD
        )
    }

    @JvmField
    val STRONGHOLD_LOCATOR: Item = register("stronghold_locator") { key ->
        StructureLocatorItem(
            Item.Properties().setId(key).stacksTo(1),
            StructureTags.EYE_OF_ENDER_LOCATED,
            Level.OVERWORLD
        )
    }

    fun init() {
        // Set the drop item for IronBoat entity after items are initialized
        thc.entity.IronBoat.ironBoatDropItem = IRON_BOAT

        ItemGroupEvents.modifyEntriesEvent(toolsTabKey).register { entries ->
            entries.accept(LAND_PLOT)
            entries.accept(BLAST_TOTEM)
            entries.accept(SOUL_DUST)
            entries.accept(IRON_BOAT)
            entries.accept(COPPER_BUCKET)
            entries.accept(COPPER_BUCKET_OF_WATER)
            entries.accept(COPPER_BUCKET_OF_MILK)
            // Structure locators
            entries.accept(FORTRESS_LOCATOR)
            entries.accept(BASTION_LOCATOR)
            entries.accept(TRIAL_CHAMBER_LOCATOR)
            entries.accept(PILLAGER_OUTPOST_LOCATOR)
            entries.accept(ANCIENT_CITY_LOCATOR)
            entries.accept(STRONGHOLD_LOCATOR)
        }
        ItemGroupEvents.modifyEntriesEvent(foodTabKey).register { entries ->
            entries.accept(HONEY_APPLE)
            entries.accept(DOUGH)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
