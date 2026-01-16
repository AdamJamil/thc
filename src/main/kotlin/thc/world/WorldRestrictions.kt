package thc.world

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Blocks
import thc.claim.ClaimManager

/**
 * Handles block placement restrictions outside base areas.
 *
 * Rules:
 * 1. Inside base areas: all blocks can be placed (delegated to BasePermissions)
 * 2. Outside base areas: only allowlist blocks can be placed
 * 3. Allowlist blocks (except torches and ladders) cannot be placed within 26 coordinates of each other
 */
object WorldRestrictions {

    /**
     * Blocks that can be placed outside base areas.
     * Includes crafting/utility stations and some building blocks.
     */
    private val ALLOWED_BLOCKS = setOf(
        // Anvils
        Blocks.ANVIL,
        Blocks.CHIPPED_ANVIL,
        Blocks.DAMAGED_ANVIL,
        // Workstations
        Blocks.BLAST_FURNACE,
        Blocks.BREWING_STAND,
        Blocks.CARTOGRAPHY_TABLE,
        Blocks.CRAFTING_TABLE,
        Blocks.ENCHANTING_TABLE,
        Blocks.FURNACE,
        Blocks.GRINDSTONE,
        Blocks.LECTERN,
        Blocks.LOOM,
        Blocks.SMITHING_TABLE,
        Blocks.SMOKER,
        Blocks.STONECUTTER,
        // Storage
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        // Misc utility
        Blocks.CAULDRON,
        Blocks.WATER_CAULDRON,
        Blocks.LAVA_CAULDRON,
        Blocks.POWDER_SNOW_CAULDRON,
        Blocks.COMPOSTER,
        Blocks.LODESTONE,
        Blocks.TNT,
        // Lighting and navigation
        Blocks.TORCH,
        Blocks.WALL_TORCH,
        Blocks.SOUL_TORCH,
        Blocks.SOUL_WALL_TORCH,
        Blocks.LADDER
    )

    /**
     * Blocks exempt from adjacency restrictions.
     * Torches and ladders are utility blocks used for navigation and can be placed close together.
     */
    private val ADJACENCY_EXEMPT_BLOCKS = setOf(
        Blocks.TORCH,
        Blocks.WALL_TORCH,
        Blocks.SOUL_TORCH,
        Blocks.SOUL_WALL_TORCH,
        Blocks.LADDER
    )

    /**
     * Register the block placement restriction handler.
     * Should be called during mod initialization.
     */
    @JvmStatic
    fun register() {
        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            // Skip on client side
            if (world.isClientSide) {
                return@UseBlockCallback InteractionResult.PASS
            }

            // Get the item being held
            val itemStack = player.getItemInHand(hand)
            val item = itemStack.item

            // Only process BlockItem placements
            if (item !is BlockItem) {
                return@UseBlockCallback InteractionResult.PASS
            }

            // Get the block being placed
            val block = item.block

            // Get server from level
            val level = world as? ServerLevel ?: return@UseBlockCallback InteractionResult.PASS
            val server = level.server

            // Get placement position (the face that was clicked)
            val placementPos = hitResult.blockPos.relative(hitResult.direction)

            // Allow all blocks inside base areas
            if (ClaimManager.isInBase(server, placementPos)) {
                return@UseBlockCallback InteractionResult.PASS
            }

            // Outside base: check if block is in allowlist
            if (block !in ALLOWED_BLOCKS) {
                return@UseBlockCallback InteractionResult.FAIL
            }

            // Block is allowed: check adjacency for non-exempt blocks
            if (block !in ADJACENCY_EXEMPT_BLOCKS) {
                if (!checkAdjacency(level, placementPos)) {
                    return@UseBlockCallback InteractionResult.FAIL
                }
            }

            InteractionResult.PASS
        })
    }

    /**
     * Check if a block placement would violate adjacency restrictions.
     *
     * Returns true if placement is allowed (no nearby restricted allowlist blocks).
     * Returns false if placement should be blocked (too close to another restricted allowlist block).
     *
     * Uses Chebyshev distance (max of |dx|,|dy|,|dz| <= 26).
     *
     * @param level The server level
     * @param placementPos The position where the block would be placed
     * @return true if placement is allowed, false if blocked by adjacency rule
     */
    private fun checkAdjacency(level: ServerLevel, placementPos: BlockPos): Boolean {
        // Check all blocks within Chebyshev distance of 26
        for (dx in -26..26) {
            for (dy in -26..26) {
                for (dz in -26..26) {
                    // Skip the placement position itself
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue
                    }

                    // Check the block at this offset
                    val checkPos = placementPos.offset(dx, dy, dz)
                    val blockState = level.getBlockState(checkPos)
                    val existingBlock = blockState.block

                    // If there's a restricted allowlist block nearby, block the placement
                    if (existingBlock in ALLOWED_BLOCKS && existingBlock !in ADJACENCY_EXEMPT_BLOCKS) {
                        return false
                    }
                }
            }
        }

        return true
    }
}
