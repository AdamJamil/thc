package thc.villager

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory
import thc.mixin.access.VillagerAccessor

/**
 * Auto-assigns villager professions when placing job blocks.
 *
 * When a player places a stonecutter, smoker, cartography table, or lectern,
 * the nearest unemployed villager within 5 blocks claims that job site and
 * gets the corresponding profession.
 */
object JobBlockAssignment {

    private val logger = LoggerFactory.getLogger("THC-JobBlockAssignment")

    fun register() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            if (world.isClientSide) return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)
            val item = stack.item
            if (item !is BlockItem) return@register InteractionResult.PASS

            val block = item.block
            // Only process allowed job blocks
            val professionKey = AllowedProfessions.getProfessionForJobBlock(block)
                ?: return@register InteractionResult.PASS

            val level = world as? ServerLevel ?: return@register InteractionResult.PASS
            val placementPos = hitResult.blockPos.relative(hitResult.direction)

            // Schedule for next tick (after block is placed and POI registered)
            level.server.execute {
                assignNearestVillagerToJobSite(level, placementPos, professionKey)
            }

            InteractionResult.PASS
        }
    }

    private fun assignNearestVillagerToJobSite(
        level: ServerLevel,
        jobBlockPos: BlockPos,
        professionKey: net.minecraft.resources.ResourceKey<VillagerProfession>
    ) {
        val center = jobBlockPos.center
        val searchBox = AABB.ofSize(center, 10.0, 10.0, 10.0)

        // Find nearest unemployed villager
        val villager = level.getEntitiesOfClass(Villager::class.java, searchBox) { v ->
            val profKey = v.villagerData.profession.unwrapKey().orElse(null)
            profKey == VillagerProfession.NONE
        }.minByOrNull { it.position().distanceToSqr(center) }

        if (villager == null) {
            logger.debug("No unemployed villager found near {}", jobBlockPos)
            return
        }

        logger.info("Assigning villager at {} to job block at {}", villager.blockPosition(), jobBlockPos)

        // 1. Set profession and level 1
        val registry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
        val profHolder = registry.getOrThrow(professionKey)
        val oldData = villager.villagerData
        villager.villagerData = oldData
            .withProfession(profHolder)
            .withLevel(1)

        // Verify the data was set
        val newData = villager.villagerData
        logger.info("Data set: profession={}, level={} (was profession={}, level={})",
            newData.profession.unwrapKey().orElse(null),
            newData.level,
            oldData.profession.unwrapKey().orElse(null),
            oldData.level
        )

        // 2. Generate level 1 trades
        val offersBefore = villager.offers.size
        (villager as VillagerAccessor).invokeUpdateTrades(level)
        val offersAfter = villager.offers.size
        logger.info("Trades: {} before, {} after updateTrades()", offersBefore, offersAfter)

        // 3. Refresh brain for new profession AI behaviors
        villager.refreshBrain(level)

        logger.info("Assignment complete for {}", professionKey)
    }
}
