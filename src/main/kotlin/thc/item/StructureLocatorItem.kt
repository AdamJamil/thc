package thc.item

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
        private const val SEARCH_RADIUS_CHUNKS = 100   // 100 chunk radius
    }

    override fun inventoryTick(
        stack: ItemStack,
        serverLevel: ServerLevel,
        entity: Entity,
        slot: EquipmentSlot?
    ) {
        // Players only
        if (entity !is Player) return

        // Throttle: search once per second
        if (serverLevel.gameTime % SEARCH_INTERVAL_TICKS != 0L) return

        // Dimension check - wrong dimension = spinning needle
        if (serverLevel.dimension() != expectedDimension) {
            clearTarget(stack)
            return
        }

        // Search for nearest structure within radius
        val found = serverLevel.findNearestMapStructure(
            structureTag,
            entity.blockPosition(),
            SEARCH_RADIUS_CHUNKS,
            false  // skipKnownStructures
        )

        if (found != null) {
            setTarget(stack, serverLevel.dimension(), found)
        } else {
            clearTarget(stack)
        }
    }

    /**
     * Set lodestone tracker target position.
     */
    private fun setTarget(stack: ItemStack, dimension: ResourceKey<Level>, pos: net.minecraft.core.BlockPos) {
        val tracker = LodestoneTracker(
            Optional.of(GlobalPos.of(dimension, pos)),
            false  // tracked=false: keep component even without lodestone
        )
        stack.set(DataComponents.LODESTONE_TRACKER, tracker)
    }

    /**
     * Clear lodestone tracker target (causes random spin).
     */
    private fun clearTarget(stack: ItemStack) {
        val tracker = LodestoneTracker(Optional.empty(), false)
        stack.set(DataComponents.LODESTONE_TRACKER, tracker)
    }
}
