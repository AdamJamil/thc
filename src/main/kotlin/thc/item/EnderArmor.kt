package thc.item

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.TagKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.world.item.equipment.EquipmentAsset
import net.minecraft.world.item.equipment.EquipmentAssets

object EnderArmor {
    private val combatTabKey: ResourceKey<CreativeModeTab> = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.withDefaultNamespace("combat")
    )

    // Equipment asset keys for texture lookup
    private val ENDER_COPPER_ASSET: ResourceKey<EquipmentAsset> = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath("thc", "ender_copper")
    )
    private val ENDER_IRON_ASSET: ResourceKey<EquipmentAsset> = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath("thc", "ender_iron")
    )
    private val ENDER_DIAMOND_ASSET: ResourceKey<EquipmentAsset> = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath("thc", "ender_diamond")
    )
    private val ENDER_NETHERITE_ASSET: ResourceKey<EquipmentAsset> = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        Identifier.fromNamespaceAndPath("thc", "ender_netherite")
    )

    // Tag for repair ingredients (ender pearls)
    private val REPAIRS_ENDER_ARMOR: TagKey<Item> = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("thc", "repairs_ender_copper_armor")
    )

    // Ender Copper: between copper and iron stats
    private val ENDER_COPPER_MATERIAL: ArmorMaterial = ArmorMaterial(
        15,  // durability multiplier (iron-tier)
        mapOf(ArmorType.HELMET to 2, ArmorType.CHESTPLATE to 5, ArmorType.LEGGINGS to 4, ArmorType.BOOTS to 2),
        12,  // enchantability
        SoundEvents.ARMOR_EQUIP_IRON,
        1.0f,  // toughness
        0.0f,  // knockback resistance
        REPAIRS_ENDER_ARMOR,
        ENDER_COPPER_ASSET
    )

    // Ender Iron: iron-tier stats
    private val ENDER_IRON_MATERIAL: ArmorMaterial = ArmorMaterial(
        15,  // durability multiplier
        mapOf(ArmorType.HELMET to 2, ArmorType.CHESTPLATE to 6, ArmorType.LEGGINGS to 5, ArmorType.BOOTS to 2),
        9,   // enchantability
        SoundEvents.ARMOR_EQUIP_IRON,
        0.0f,
        0.0f,
        REPAIRS_ENDER_ARMOR,
        ENDER_IRON_ASSET
    )

    // Ender Diamond: diamond-tier stats
    private val ENDER_DIAMOND_MATERIAL: ArmorMaterial = ArmorMaterial(
        33,  // durability multiplier
        mapOf(ArmorType.HELMET to 3, ArmorType.CHESTPLATE to 8, ArmorType.LEGGINGS to 6, ArmorType.BOOTS to 3),
        10,  // enchantability
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.0f,  // toughness
        0.0f,
        REPAIRS_ENDER_ARMOR,
        ENDER_DIAMOND_ASSET
    )

    // Ender Netherite: netherite-tier stats
    private val ENDER_NETHERITE_MATERIAL: ArmorMaterial = ArmorMaterial(
        37,  // durability multiplier
        mapOf(ArmorType.HELMET to 3, ArmorType.CHESTPLATE to 8, ArmorType.LEGGINGS to 6, ArmorType.BOOTS to 3),
        15,  // enchantability
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0f,  // toughness
        0.1f,  // knockback resistance
        REPAIRS_ENDER_ARMOR,
        ENDER_NETHERITE_ASSET
    )

    @JvmField
    val ENDER_COPPER_HELMET: Item = register("ender_copper_helmet") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.HELMET.getDurability(15))
                .humanoidArmor(ENDER_COPPER_MATERIAL, ArmorType.HELMET)
                .repairable(Items.ENDER_PEARL)
        )
    }

    @JvmField
    val ENDER_COPPER_CHESTPLATE: Item = register("ender_copper_chestplate") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.CHESTPLATE.getDurability(15))
                .humanoidArmor(ENDER_COPPER_MATERIAL, ArmorType.CHESTPLATE)
                .repairable(Items.ENDER_PEARL)
        )
    }

    @JvmField
    val ENDER_COPPER_LEGGINGS: Item = register("ender_copper_leggings") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.LEGGINGS.getDurability(15))
                .humanoidArmor(ENDER_COPPER_MATERIAL, ArmorType.LEGGINGS)
                .repairable(Items.ENDER_PEARL)
        )
    }

    @JvmField
    val ENDER_COPPER_BOOTS: Item = register("ender_copper_boots") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.BOOTS.getDurability(15))
                .humanoidArmor(ENDER_COPPER_MATERIAL, ArmorType.BOOTS)
                .repairable(Items.ENDER_PEARL)
        )
    }

    // Ender Iron Helmet
    @JvmField
    val ENDER_IRON_HELMET: Item = register("ender_iron_helmet") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.HELMET.getDurability(15))
                .humanoidArmor(ENDER_IRON_MATERIAL, ArmorType.HELMET)
                .repairable(Items.ENDER_PEARL)
        )
    }

    // Ender Diamond Helmet
    @JvmField
    val ENDER_DIAMOND_HELMET: Item = register("ender_diamond_helmet") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.HELMET.getDurability(33))
                .humanoidArmor(ENDER_DIAMOND_MATERIAL, ArmorType.HELMET)
                .repairable(Items.ENDER_PEARL)
        )
    }

    // Ender Netherite Helmet
    @JvmField
    val ENDER_NETHERITE_HELMET: Item = register("ender_netherite_helmet") { key ->
        Item(
            Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(ArmorType.HELMET.getDurability(37))
                .humanoidArmor(ENDER_NETHERITE_MATERIAL, ArmorType.HELMET)
                .repairable(Items.ENDER_PEARL)
                .fireResistant()
        )
    }

    fun init() {
        ItemGroupEvents.modifyEntriesEvent(combatTabKey).register { entries ->
            entries.accept(ENDER_COPPER_HELMET)
            entries.accept(ENDER_COPPER_CHESTPLATE)
            entries.accept(ENDER_COPPER_LEGGINGS)
            entries.accept(ENDER_COPPER_BOOTS)
            entries.accept(ENDER_IRON_HELMET)
            entries.accept(ENDER_DIAMOND_HELMET)
            entries.accept(ENDER_NETHERITE_HELMET)
        }
    }

    private fun register(name: String, factory: (ResourceKey<Item>) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thc", name))
        val item = factory(key)
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
