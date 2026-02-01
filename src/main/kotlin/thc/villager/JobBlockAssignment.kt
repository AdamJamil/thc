package thc.villager

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB

/**
 * Auto-assigns villager professions when placing job blocks.
 *
 * When a player places a stonecutter, smoker, cartography table, or lectern,
 * the nearest unemployed villager within 5 blocks is pointed to that job site
 * via brain memory. Vanilla AI then handles profession assignment naturally.
 */
object JobBlockAssignment {

    fun register() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            if (world.isClientSide) return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)
            val item = stack.item
            if (item !is BlockItem) return@register InteractionResult.PASS

            val block = item.block
            // Only process allowed job blocks
            AllowedProfessions.getProfessionForJobBlock(block)
                ?: return@register InteractionResult.PASS

            val level = world as? ServerLevel ?: return@register InteractionResult.PASS
            val placementPos = hitResult.blockPos.relative(hitResult.direction)

            // Schedule for next tick (after block is placed and POI registered)
            level.server.execute {
                assignNearestVillagerToJobSite(level, placementPos)
            }

            InteractionResult.PASS
        }
    }

    private fun assignNearestVillagerToJobSite(level: ServerLevel, jobBlockPos: BlockPos) {
        val center = jobBlockPos.center
        val searchBox = AABB.ofSize(center, 10.0, 10.0, 10.0)

        // Find nearest unemployed villager
        val villager = level.getEntitiesOfClass(Villager::class.java, searchBox) { v ->
            val profKey = v.villagerData.profession.unwrapKey().orElse(null)
            profKey == VillagerProfession.NONE
        }.minByOrNull { it.position().distanceToSqr(center) }
            ?: return

        // Set POTENTIAL_JOB_SITE memory - vanilla AI will handle the rest
        val globalPos = GlobalPos.of(level.dimension(), jobBlockPos)
        villager.brain.setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, globalPos)
    }
}
