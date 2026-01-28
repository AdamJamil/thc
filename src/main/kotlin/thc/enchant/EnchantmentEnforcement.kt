package thc.enchant

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.ItemEnchantments

/**
 * Utility for enforcing THC's enchantment rules:
 * - Removed enchantments are stripped from newly generated items
 * - All enchantment levels normalized to internal values (default 1)
 */
object EnchantmentEnforcement {

    /**
     * Set of enchantment IDs allowed on lecterns (Stage 1-2).
     * These are early-game, basic effect enchantments.
     * Fortune/Looting (economy), Mending/Respiration/Aqua/Depth/Frost (mob drops),
     * Flame/Fire Aspect (damage) are stage 3+ (enchanting table only).
     */
    val STAGE_1_2_ENCHANTMENTS = setOf(
        // Damage enchantments (basic)
        "minecraft:sharpness",
        "minecraft:power",
        "minecraft:smite",
        "minecraft:bane_of_arthropods",

        // Defense enchantments (all protection types)
        "minecraft:protection",
        "minecraft:fire_protection",
        "minecraft:blast_protection",
        "minecraft:projectile_protection",
        "minecraft:feather_falling",

        // Tool enchantments (basic utility)
        "minecraft:efficiency",
        "minecraft:unbreaking",
        "minecraft:silk_touch",

        // Crossbow basic
        "minecraft:piercing",

        // Fishing basic
        "minecraft:lure",
        "minecraft:luck_of_the_sea"
    )

    /**
     * Check if an enchantment ID is a stage 1-2 enchantment (lectern-compatible).
     */
    fun isStage12Enchantment(enchantId: String?): Boolean {
        return enchantId != null && STAGE_1_2_ENCHANTMENTS.contains(enchantId)
    }

    /**
     * Set of enchantment IDs that have been removed from THC.
     * These should never appear on newly generated loot.
     */
    val REMOVED_ENCHANTMENTS = setOf(
        "minecraft:loyalty",
        "minecraft:impaling",
        "minecraft:riptide",
        "minecraft:infinity",
        "minecraft:knockback",
        "minecraft:punch",
        "minecraft:quick_charge",
        "minecraft:lunge",
        "minecraft:thorns",
        "minecraft:wind_burst",
        "minecraft:multishot",
        "minecraft:density"
    )

    /**
     * Internal effect levels for enchantments.
     * Display shows no level suffix, but effects use these values.
     * Enchantments not in this map default to level 1.
     */
    val INTERNAL_LEVELS = mapOf(
        "minecraft:efficiency" to 3,
        "minecraft:sharpness" to 1,
        "minecraft:power" to 1,
        "minecraft:protection" to 1,
        "minecraft:fortune" to 3,
        "minecraft:looting" to 3,
        "minecraft:unbreaking" to 3,
        "minecraft:feather_falling" to 4
    )

    /**
     * Strips removed enchantments and normalizes levels to INTERNAL_LEVELS values.
     * Returns the original if no changes needed (avoids unnecessary object creation).
     * Preserves the showInTooltip flag from the original enchantments.
     */
    fun stripAndNormalize(enchantments: ItemEnchantments): ItemEnchantments {
        if (enchantments.isEmpty) {
            return enchantments
        }

        var modified = false
        val builder = ItemEnchantments.Mutable(enchantments)

        // Collect entries to remove (can't modify while iterating)
        val toRemove = mutableListOf<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>>()

        for (entry in enchantments.entrySet()) {
            val holder = entry.key
            val currentLevel = entry.intValue
            val enchantId = holder.unwrapKey().orElse(null)?.identifier()?.toString()

            if (enchantId != null && REMOVED_ENCHANTMENTS.contains(enchantId)) {
                toRemove.add(holder)
                modified = true
            } else {
                // Normalize level to internal value
                val targetLevel = if (enchantId != null) {
                    INTERNAL_LEVELS.getOrDefault(enchantId, 1)
                } else {
                    1
                }

                if (currentLevel != targetLevel) {
                    builder.set(holder, targetLevel)
                    modified = true
                }
            }
        }

        // Remove enchantments after iteration
        for (holder in toRemove) {
            builder.removeIf { it == holder }
        }

        return if (modified) builder.toImmutable() else enchantments
    }

    /**
     * Corrects enchantments on an ItemStack in-place.
     * Handles both ENCHANTMENTS (on tools/armor) and STORED_ENCHANTMENTS (on books).
     */
    fun correctStack(stack: ItemStack) {
        // Handle regular enchantments (on tools, weapons, armor)
        val enchantments = stack.get(DataComponents.ENCHANTMENTS)
        if (enchantments != null && !enchantments.isEmpty) {
            val corrected = stripAndNormalize(enchantments)
            if (corrected !== enchantments) {
                stack.set(DataComponents.ENCHANTMENTS, corrected)
            }
        }

        // Handle stored enchantments (on enchanted books)
        val storedEnchantments = stack.get(DataComponents.STORED_ENCHANTMENTS)
        if (storedEnchantments != null && !storedEnchantments.isEmpty) {
            val corrected = stripAndNormalize(storedEnchantments)
            if (corrected !== storedEnchantments) {
                stack.set(DataComponents.STORED_ENCHANTMENTS, corrected)
            }
        }
    }
}
