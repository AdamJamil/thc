package thc.food

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Items
import thc.item.THCItems

/**
 * Rebalances vanilla food items' nutrition and saturation values.
 *
 * Saturation formula: nutrition * saturationModifier * 2.0
 * To get target saturation, use: modifier = targetSat / (nutrition * 2.0)
 *
 * Categories:
 * - Raw meats: 0 saturation (worthless for healing)
 * - Cooked meats: 1.6-1.8 saturation (basic healing - T2)
 * - Crops/vegetables: 0-0.7 saturation (minimal healing - T1)
 * - Golden foods: 10 saturation (premium healing - T4)
 *
 * Note: Effects like Hunger on raw chicken are in CONSUMABLE component, not FOOD.
 * We only modify the FOOD component for nutrition/saturation.
 */
object FoodStatsModifier {
    fun register() {
        DefaultItemComponentEvents.MODIFY.register { context ->
            // ===== RAW MEATS (0 saturation - worthless for healing) =====
            // Modifier = 0 / (nutrition * 2) = 0
            context.modify(Items.BEEF) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.PORKCHOP) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.CHICKEN) { builder ->
                // Hunger effect is in CONSUMABLE component, not affected
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.RABBIT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.MUTTON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.COD) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.SALMON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }

            // ===== COOKED MEATS (1.6-1.8 saturation - basic healing) =====
            // COOKED_BEEF: 8 hunger, 1.8 sat -> modifier = 1.8 / 16 = 0.1125
            context.modify(Items.COOKED_BEEF) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(0.1125f)
                    .build())
            }
            // COOKED_PORKCHOP: 8 hunger, 1.8 sat -> modifier = 1.8 / 16 = 0.1125
            context.modify(Items.COOKED_PORKCHOP) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(0.1125f)
                    .build())
            }
            // COOKED_CHICKEN: 6 hunger, 1.6 sat -> modifier = 1.6 / 12 = 0.1333
            context.modify(Items.COOKED_CHICKEN) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.1333f)
                    .build())
            }
            // COOKED_RABBIT: 5 hunger, 1.6 sat -> modifier = 1.6 / 10 = 0.16
            context.modify(Items.COOKED_RABBIT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.16f)
                    .build())
            }
            // COOKED_MUTTON: 6 hunger, 1.6 sat -> modifier = 1.6 / 12 = 0.1333
            context.modify(Items.COOKED_MUTTON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.1333f)
                    .build())
            }
            // COOKED_COD: 5 hunger, 1.6 sat -> modifier = 1.6 / 10 = 0.16
            context.modify(Items.COOKED_COD) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.16f)
                    .build())
            }
            // COOKED_SALMON: 6 hunger, 1.6 sat -> modifier = 1.6 / 12 = 0.1333
            context.modify(Items.COOKED_SALMON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.1333f)
                    .build())
            }

            // ===== CROPS/VEGETABLES (0-0.7 saturation - minimal healing) =====
            // BREAD: 5 hunger, 0.7 sat -> modifier = 0.7 / 10 = 0.07
            context.modify(Items.BREAD) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.07f)
                    .build())
            }
            // CARROT: 3 hunger, 0.5 sat -> modifier = 0.5 / 6 = 0.0833
            context.modify(Items.CARROT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.0833f)
                    .build())
            }
            // POTATO: 1 hunger, 0 sat -> modifier = 0
            context.modify(Items.POTATO) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .build())
            }
            // BAKED_POTATO: 5 hunger, 0.7 sat -> modifier = 0.7 / 10 = 0.07
            context.modify(Items.BAKED_POTATO) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.07f)
                    .build())
            }
            // MELON_SLICE: 2 hunger, 0.2 sat -> modifier = 0.2 / 4 = 0.05
            context.modify(Items.MELON_SLICE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.05f)
                    .build())
            }
            // APPLE: 4 hunger, 0.5 sat -> modifier = 0.5 / 8 = 0.0625
            context.modify(Items.APPLE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.0625f)
                    .build())
            }
            // COOKIE: 2 hunger, 0.2 sat -> modifier = 0.2 / 4 = 0.05
            context.modify(Items.COOKIE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.05f)
                    .build())
            }
            // PUMPKIN_PIE: 5 hunger, 0.7 sat -> modifier = 0.7 / 10 = 0.07
            context.modify(Items.PUMPKIN_PIE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.07f)
                    .build())
            }
            // DRIED_KELP: 1 hunger, 0 sat -> modifier = 0
            context.modify(Items.DRIED_KELP) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .build())
            }
            // SWEET_BERRIES: 2 hunger, 0.2 sat -> modifier = 0.2 / 4 = 0.05
            context.modify(Items.SWEET_BERRIES) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.05f)
                    .build())
            }
            // GLOW_BERRIES: 2 hunger, 0.2 sat -> modifier = 0.2 / 4 = 0.05
            context.modify(Items.GLOW_BERRIES) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.05f)
                    .build())
            }
            // BEETROOT: 1 hunger, 0 sat -> modifier = 0
            context.modify(Items.BEETROOT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .build())
            }

            // ===== GOLDEN FOODS (10 saturation - premium healing) =====
            // Effects are in CONSUMABLE component, not FOOD - we only modify FOOD.
            // GOLDEN_APPLE: 4 hunger, 10 sat -> modifier = 10 / 8 = 1.25
            context.modify(Items.GOLDEN_APPLE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(1.25f)
                    .alwaysEdible()
                    .build())
            }
            // ENCHANTED_GOLDEN_APPLE: 4 hunger, 10 sat -> modifier = 10 / 8 = 1.25
            context.modify(Items.ENCHANTED_GOLDEN_APPLE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(1.25f)
                    .alwaysEdible()
                    .build())
            }
            // GOLDEN_CARROT: 6 hunger, 10 sat -> modifier = 10 / 12 = 0.8333
            context.modify(Items.GOLDEN_CARROT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.8333f)
                    .build())
            }

            // ===== HEARTY STEW (rabbit stew renamed) - premium food =====
            // 10 hunger, 6.36 sat -> modifier = 6.36 / 20 = 0.318
            context.modify(Items.RABBIT_STEW) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(10)
                    .saturationModifier(0.318f)
                    .build())
            }

            // ===== HONEY APPLE - mid-tier healing food =====
            // 8 hunger, 2.73 sat -> modifier = 2.73 / 16 = 0.170625
            context.modify(THCItems.HONEY_APPLE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(0.170625f)
                    .build())
            }
        }
    }
}
