package thc.food

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents
import net.minecraft.core.component.DataComponents
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Items

object FoodStatsModifier {
    fun register() {
        DefaultItemComponentEvents.MODIFY.register { context ->
            // ===== RAW MEATS (0 saturation - worthless for healing) =====
            context.modify(Items.RAW_BEEF) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.RAW_PORKCHOP) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0f)
                    .build())
            }
            // Raw chicken retains Hunger effect (30% chance)
            context.modify(Items.RAW_CHICKEN) { builder ->
                val original = Items.RAW_CHICKEN.components().get(DataComponents.FOOD)
                val foodBuilder = FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                original?.effects()?.forEach { effect ->
                    foodBuilder.effect(effect.effectSupplier(), effect.probability())
                }
                builder.set(DataComponents.FOOD, foodBuilder.build())
            }
            context.modify(Items.RAW_RABBIT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.RAW_MUTTON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.RAW_COD) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.RAW_SALMON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0f)
                    .build())
            }

            // ===== COOKED MEATS (1.6-1.8 saturation - basic healing) =====
            context.modify(Items.COOKED_BEEF) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(1.8f)
                    .build())
            }
            context.modify(Items.COOKED_PORKCHOP) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(1.8f)
                    .build())
            }
            context.modify(Items.COOKED_CHICKEN) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(1.6f)
                    .build())
            }
            context.modify(Items.COOKED_RABBIT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(1.6f)
                    .build())
            }
            context.modify(Items.COOKED_MUTTON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(1.6f)
                    .build())
            }
            context.modify(Items.COOKED_COD) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(1.6f)
                    .build())
            }
            context.modify(Items.COOKED_SALMON) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(1.6f)
                    .build())
            }

            // ===== CROPS/VEGETABLES (0-0.7 saturation - minimal healing) =====
            context.modify(Items.BREAD) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.7f)
                    .build())
            }
            context.modify(Items.CARROT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.5f)
                    .build())
            }
            context.modify(Items.POTATO) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.BAKED_POTATO) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.7f)
                    .build())
            }
            context.modify(Items.MELON_SLICE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2f)
                    .build())
            }
            context.modify(Items.APPLE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.5f)
                    .build())
            }
            context.modify(Items.COOKIE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2f)
                    .build())
            }
            context.modify(Items.PUMPKIN_PIE) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.7f)
                    .build())
            }
            context.modify(Items.DRIED_KELP) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .build())
            }
            context.modify(Items.SWEET_BERRIES) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2f)
                    .build())
            }
            context.modify(Items.GLOW_BERRIES) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2f)
                    .build())
            }
            context.modify(Items.BEETROOT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .build())
            }

            // ===== GOLDEN FOODS (10 saturation - premium healing) =====
            // Golden apple - preserve Regeneration and Absorption effects
            context.modify(Items.GOLDEN_APPLE) { builder ->
                val original = Items.GOLDEN_APPLE.components().get(DataComponents.FOOD)
                val foodBuilder = FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(10f)
                    .alwaysEdible()
                original?.effects()?.forEach { effect ->
                    foodBuilder.effect(effect.effectSupplier(), effect.probability())
                }
                builder.set(DataComponents.FOOD, foodBuilder.build())
            }
            // Enchanted golden apple - preserve all effects
            context.modify(Items.ENCHANTED_GOLDEN_APPLE) { builder ->
                val original = Items.ENCHANTED_GOLDEN_APPLE.components().get(DataComponents.FOOD)
                val foodBuilder = FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(10f)
                    .alwaysEdible()
                original?.effects()?.forEach { effect ->
                    foodBuilder.effect(effect.effectSupplier(), effect.probability())
                }
                builder.set(DataComponents.FOOD, foodBuilder.build())
            }
            context.modify(Items.GOLDEN_CARROT) { builder ->
                builder.set(DataComponents.FOOD, FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(10f)
                    .build())
            }
        }
    }
}
