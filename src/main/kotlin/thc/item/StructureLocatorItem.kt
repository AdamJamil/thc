package thc.item

import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.LodestoneTracker
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.Optional

/**
 * Compass-style item that points to specific structures.
 *
 * Uses lodestone_tracker component with tracked=false to leverage vanilla
 * compass rendering. Wrong dimension causes spinning needle (cleared target).
 *
 * @param properties Item properties
 * @param structureTag Tag for structure type to locate
 * @param expectedDimension Dimension where this locator works
 */
class StructureLocatorItem(
    properties: Properties,
    private val structureTag: TagKey<Structure>,
    private val expectedDimension: ResourceKey<Level>
) : Item(properties) {

    companion object {
        private const val SEARCH_INTERVAL_TICKS = 20L  // Search once per second
        private const val MAX_SEARCH_RADIUS = 100      // Max search radius in chunks
    }

    override fun inventoryTick(
        stack: ItemStack,
        serverLevel: ServerLevel,
        entity: Entity,
        slot: EquipmentSlot?
    ) {
        if (entity !is Player) return
        if (serverLevel.gameTime % SEARCH_INTERVAL_TICKS != 0L) return

        if (serverLevel.dimension() != expectedDimension) {
            clearTarget(stack)
            return
        }

        val found = findClosestStructure(serverLevel, entity.blockPosition())

        if (found != null) {
            setTarget(stack, serverLevel.dimension(), found)
        } else {
            clearTarget(stack)
        }
    }

    /**
     * Find the closest structure by searching at increasing radii.
     * This ensures we find truly nearby structures before distant ones.
     */
    private fun findClosestStructure(level: ServerLevel, origin: BlockPos): BlockPos? {
        val radii = listOf(16, 32, 64, MAX_SEARCH_RADIUS)
        var bestPos: BlockPos? = null
        var bestDistSq = Double.MAX_VALUE

        for (radius in radii) {
            val found = level.findNearestMapStructure(
                structureTag,
                origin,
                radius,
                false
            )

            if (found != null) {
                val distSq = origin.distSqr(found)
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestPos = found
                }
            }

            // If we found something, check one more tier to ensure closest
            if (bestPos != null && radius < MAX_SEARCH_RADIUS) {
                val nextRadius = radii.getOrNull(radii.indexOf(radius) + 1) ?: break
                val nextFound = level.findNearestMapStructure(
                    structureTag,
                    origin,
                    nextRadius,
                    false
                )
                if (nextFound != null) {
                    val nextDistSq = origin.distSqr(nextFound)
                    if (nextDistSq < bestDistSq) {
                        bestPos = nextFound
                    }
                }
                break
            }
        }

        return bestPos
    }

    private fun setTarget(stack: ItemStack, dimension: ResourceKey<Level>, pos: BlockPos) {
        val tracker = LodestoneTracker(
            Optional.of(GlobalPos.of(dimension, pos)),
            false
        )
        stack.set(DataComponents.LODESTONE_TRACKER, tracker)
    }

    private fun clearTarget(stack: ItemStack) {
        val tracker = LodestoneTracker(Optional.empty(), false)
        stack.set(DataComponents.LODESTONE_TRACKER, tracker)
    }
}
