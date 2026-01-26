package thc.armor

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemAttributeModifiers

/**
 * Rebalances vanilla armor progression with copper tier insertion.
 *
 * Per-tier totals:
 * - Leather: 7 armor (early game)
 * - Copper: 10 armor (mid-tier upgrade)
 * - Iron: 15 armor (strong protection)
 * - Diamond: 18 armor + 4 toughness (endgame)
 * - Netherite: 20 armor + 6 toughness + 0.4 knockback resistance (ultimate)
 *
 * Each tier upgrade provides strictly more protection than the previous.
 * Half armor point values (1.5, 2.0, 3.0, etc.) create smooth progression.
 */
object ArmorRebalancing {
    fun register() {
        DefaultItemComponentEvents.MODIFY.register { context ->
            // ===== LEATHER ARMOR (7 total armor) =====
            context.modify(Items.LEATHER_HELMET) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "leather_helmet_armor"),
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .build())
            }
            context.modify(Items.LEATHER_CHESTPLATE) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "leather_chestplate_armor"),
                                3.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .build())
            }
            context.modify(Items.LEATHER_LEGGINGS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "leather_leggings_armor"),
                                2.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .build())
            }
            context.modify(Items.LEATHER_BOOTS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "leather_boots_armor"),
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .build())
            }

            // ===== COPPER ARMOR (10 total armor) =====
            // Note: Copper armor is added by THC mod, not vanilla
            // These entries are placeholders for when copper armor is added to the game

            // ===== IRON ARMOR (15 total armor) =====
            context.modify(Items.IRON_HELMET) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "iron_helmet_armor"),
                                2.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .build())
            }
            context.modify(Items.IRON_CHESTPLATE) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "iron_chestplate_armor"),
                                6.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .build())
            }
            context.modify(Items.IRON_LEGGINGS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "iron_leggings_armor"),
                                5.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .build())
            }
            context.modify(Items.IRON_BOOTS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "iron_boots_armor"),
                                2.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .build())
            }

            // ===== DIAMOND ARMOR (18 armor + 4 toughness) =====
            context.modify(Items.DIAMOND_HELMET) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_helmet_armor"),
                                3.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_helmet_toughness"),
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .build())
            }
            context.modify(Items.DIAMOND_CHESTPLATE) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_chestplate_armor"),
                                7.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_chestplate_toughness"),
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .build())
            }
            context.modify(Items.DIAMOND_LEGGINGS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_leggings_armor"),
                                5.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_leggings_toughness"),
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .build())
            }
            context.modify(Items.DIAMOND_BOOTS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_boots_armor"),
                                3.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "diamond_boots_toughness"),
                                1.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .build())
            }

            // ===== NETHERITE ARMOR (20 armor + 6 toughness + 0.4 knockback resistance) =====
            context.modify(Items.NETHERITE_HELMET) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_helmet_armor"),
                                3.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_helmet_toughness"),
                                1.5,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .add(Attributes.KNOCKBACK_RESISTANCE,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_helmet_knockback"),
                                0.1,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.HEAD)
                        .build())
            }
            context.modify(Items.NETHERITE_CHESTPLATE) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_chestplate_armor"),
                                8.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_chestplate_toughness"),
                                1.5,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .add(Attributes.KNOCKBACK_RESISTANCE,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_chestplate_knockback"),
                                0.1,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.CHEST)
                        .build())
            }
            context.modify(Items.NETHERITE_LEGGINGS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_leggings_armor"),
                                6.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_leggings_toughness"),
                                1.5,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .add(Attributes.KNOCKBACK_RESISTANCE,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_leggings_knockback"),
                                0.1,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.LEGS)
                        .build())
            }
            context.modify(Items.NETHERITE_BOOTS) { builder ->
                builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_boots_armor"),
                                3.0,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .add(Attributes.ARMOR_TOUGHNESS,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_boots_toughness"),
                                1.5,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .add(Attributes.KNOCKBACK_RESISTANCE,
                            AttributeModifier(
                                Identifier.fromNamespaceAndPath("thc", "netherite_boots_knockback"),
                                0.1,
                                AttributeModifier.Operation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.FEET)
                        .build())
            }
        }
    }
}
