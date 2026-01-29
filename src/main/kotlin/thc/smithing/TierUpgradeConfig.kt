package thc.smithing

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Configuration for tier upgrades at smithing tables.
 * Defines material counts and valid upgrade paths.
 */
object TierUpgradeConfig {

    /**
     * Material count required for each armor piece type.
     * Based on vanilla crafting recipes.
     */
    val ARMOR_MATERIAL_COUNTS: Map<Item, Int> = mapOf(
        // Leather armor
        Items.LEATHER_HELMET to 5,
        Items.LEATHER_CHESTPLATE to 8,
        Items.LEATHER_LEGGINGS to 7,
        Items.LEATHER_BOOTS to 4,

        // Copper armor (requires copper ingots)
        Items.COPPER_HELMET to 5,
        Items.COPPER_CHESTPLATE to 8,
        Items.COPPER_LEGGINGS to 7,
        Items.COPPER_BOOTS to 4,

        // Iron armor
        Items.IRON_HELMET to 5,
        Items.IRON_CHESTPLATE to 8,
        Items.IRON_LEGGINGS to 7,
        Items.IRON_BOOTS to 4,

        // Diamond armor
        Items.DIAMOND_HELMET to 5,
        Items.DIAMOND_CHESTPLATE to 8,
        Items.DIAMOND_LEGGINGS to 7,
        Items.DIAMOND_BOOTS to 4
    )

    /**
     * Material count required for each tool type.
     * Based on vanilla crafting recipes.
     */
    val TOOL_MATERIAL_COUNTS: Map<Item, Int> = mapOf(
        // Pickaxes (3 material)
        Items.WOODEN_PICKAXE to 3,
        Items.STONE_PICKAXE to 3,
        Items.COPPER_PICKAXE to 3,
        Items.IRON_PICKAXE to 3,

        // Axes (3 material)
        Items.WOODEN_AXE to 3,
        Items.STONE_AXE to 3,
        Items.COPPER_AXE to 3,
        Items.IRON_AXE to 3,

        // Shovels (1 material)
        Items.WOODEN_SHOVEL to 1,
        Items.STONE_SHOVEL to 1,
        Items.COPPER_SHOVEL to 1,
        Items.IRON_SHOVEL to 1,

        // Hoes (2 material)
        Items.WOODEN_HOE to 2,
        Items.STONE_HOE to 2,
        Items.COPPER_HOE to 2,
        Items.IRON_HOE to 2,

        // Swords (2 material)
        Items.WOODEN_SWORD to 2,
        Items.STONE_SWORD to 2,
        Items.COPPER_SWORD to 2,
        Items.IRON_SWORD to 2
    )

    /**
     * Valid tier upgrade paths: (base, addition) -> result
     */
    val VALID_TIER_UPGRADES: Map<Pair<Item, Item>, Item> = mapOf(
        // ARMOR UPGRADES
        // Leather -> Copper (using copper ingots)
        Pair(Items.LEATHER_HELMET, Items.COPPER_INGOT) to Items.COPPER_HELMET,
        Pair(Items.LEATHER_CHESTPLATE, Items.COPPER_INGOT) to Items.COPPER_CHESTPLATE,
        Pair(Items.LEATHER_LEGGINGS, Items.COPPER_INGOT) to Items.COPPER_LEGGINGS,
        Pair(Items.LEATHER_BOOTS, Items.COPPER_INGOT) to Items.COPPER_BOOTS,

        // Copper -> Iron (using iron ingots)
        Pair(Items.COPPER_HELMET, Items.IRON_INGOT) to Items.IRON_HELMET,
        Pair(Items.COPPER_CHESTPLATE, Items.IRON_INGOT) to Items.IRON_CHESTPLATE,
        Pair(Items.COPPER_LEGGINGS, Items.IRON_INGOT) to Items.IRON_LEGGINGS,
        Pair(Items.COPPER_BOOTS, Items.IRON_INGOT) to Items.IRON_BOOTS,

        // Iron -> Diamond (using diamonds)
        Pair(Items.IRON_HELMET, Items.DIAMOND) to Items.DIAMOND_HELMET,
        Pair(Items.IRON_CHESTPLATE, Items.DIAMOND) to Items.DIAMOND_CHESTPLATE,
        Pair(Items.IRON_LEGGINGS, Items.DIAMOND) to Items.DIAMOND_LEGGINGS,
        Pair(Items.IRON_BOOTS, Items.DIAMOND) to Items.DIAMOND_BOOTS,

        // TOOL UPGRADES
        // Wooden -> Stone (using smooth stone)
        Pair(Items.WOODEN_PICKAXE, Items.SMOOTH_STONE) to Items.STONE_PICKAXE,
        Pair(Items.WOODEN_AXE, Items.SMOOTH_STONE) to Items.STONE_AXE,
        Pair(Items.WOODEN_SHOVEL, Items.SMOOTH_STONE) to Items.STONE_SHOVEL,
        Pair(Items.WOODEN_HOE, Items.SMOOTH_STONE) to Items.STONE_HOE,
        Pair(Items.WOODEN_SWORD, Items.SMOOTH_STONE) to Items.STONE_SWORD,

        // Stone -> Copper (using copper ingots)
        Pair(Items.STONE_PICKAXE, Items.COPPER_INGOT) to Items.COPPER_PICKAXE,
        Pair(Items.STONE_AXE, Items.COPPER_INGOT) to Items.COPPER_AXE,
        Pair(Items.STONE_SHOVEL, Items.COPPER_INGOT) to Items.COPPER_SHOVEL,
        Pair(Items.STONE_HOE, Items.COPPER_INGOT) to Items.COPPER_HOE,
        Pair(Items.STONE_SWORD, Items.COPPER_INGOT) to Items.COPPER_SWORD,

        // Copper -> Iron (using iron ingots)
        Pair(Items.COPPER_PICKAXE, Items.IRON_INGOT) to Items.IRON_PICKAXE,
        Pair(Items.COPPER_AXE, Items.IRON_INGOT) to Items.IRON_AXE,
        Pair(Items.COPPER_SHOVEL, Items.IRON_INGOT) to Items.IRON_SHOVEL,
        Pair(Items.COPPER_HOE, Items.IRON_INGOT) to Items.IRON_HOE,
        Pair(Items.COPPER_SWORD, Items.IRON_INGOT) to Items.IRON_SWORD,

        // Iron -> Diamond (using diamonds)
        Pair(Items.IRON_PICKAXE, Items.DIAMOND) to Items.DIAMOND_PICKAXE,
        Pair(Items.IRON_AXE, Items.DIAMOND) to Items.DIAMOND_AXE,
        Pair(Items.IRON_SHOVEL, Items.DIAMOND) to Items.DIAMOND_SHOVEL,
        Pair(Items.IRON_HOE, Items.DIAMOND) to Items.DIAMOND_HOE,
        Pair(Items.IRON_SWORD, Items.DIAMOND) to Items.DIAMOND_SWORD
    )

    /**
     * Gets the required material count for upgrading a base item.
     * Returns the material count based on the base item type.
     *
     * @param baseItem The base armor or tool being upgraded
     * @return Required material count, or 0 if not upgradeable
     */
    fun getRequiredMaterialCount(baseItem: Item): Int {
        return ARMOR_MATERIAL_COUNTS[baseItem] ?: TOOL_MATERIAL_COUNTS[baseItem] ?: 0
    }

    /**
     * Gets the upgrade result for a base item + addition item combination.
     *
     * @param baseItem The base armor piece
     * @param additionItem The upgrade material
     * @return The upgraded item, or null if not a valid upgrade
     */
    fun getUpgradeResult(baseItem: Item, additionItem: Item): Item? {
        return VALID_TIER_UPGRADES[Pair(baseItem, additionItem)]
    }

    /**
     * Checks if a base + addition combination is a valid tier upgrade.
     *
     * @param baseItem The base armor piece
     * @param additionItem The upgrade material
     * @return True if this is a valid tier upgrade
     */
    fun isValidTierUpgrade(baseItem: Item, additionItem: Item): Boolean {
        return VALID_TIER_UPGRADES.containsKey(Pair(baseItem, additionItem))
    }
}
