package thc.villager

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory
import thc.villager.JobAssignmentBypass

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
            val serverPlayer = player as? net.minecraft.server.level.ServerPlayer

            logger.info("Job block placed: {} at {}", block, placementPos)

            // Schedule for next tick (after block is placed and POI registered)
            level.server.execute {
                try {
                    assignNearestVillagerToJobSite(level, placementPos, professionKey, serverPlayer)
                } catch (e: Exception) {
                    logger.error("Error in job assignment", e)
                    serverPlayer?.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§cError: ${e.message}")
                    )
                }
            }

            InteractionResult.PASS
        }
    }

    private fun assignNearestVillagerToJobSite(
        level: ServerLevel,
        jobBlockPos: BlockPos,
        professionKey: net.minecraft.resources.ResourceKey<VillagerProfession>,
        player: net.minecraft.server.level.ServerPlayer?
    ) {
        fun msg(text: String) {
            logger.info(text)
            player?.sendSystemMessage(net.minecraft.network.chat.Component.literal(text))
        }

        val center = jobBlockPos.center
        val searchBox = AABB.ofSize(center, 10.0, 10.0, 10.0)

        // Find nearest unemployed villager
        val villager = level.getEntitiesOfClass(Villager::class.java, searchBox) { v ->
            val profKey = v.villagerData.profession.unwrapKey().orElse(null)
            profKey == VillagerProfession.NONE
        }.minByOrNull { it.position().distanceToSqr(center) }

        if (villager == null) {
            msg("§7No unemployed villager within 5 blocks")
            return
        }

        // Debug: what profession does this villager actually have?
        val foundProfKey = villager.villagerData.profession.unwrapKey().orElse(null)
        val foundLevel = villager.villagerData.level
        val foundOffers = villager.offers.size
        msg("§7Found: prof=$foundProfKey lv=$foundLevel offers=$foundOffers")

        // Double-check they're actually unemployed
        if (foundProfKey != VillagerProfession.NONE) {
            msg("§cERROR: Filter broken - villager is NOT unemployed!")
            return
        }

        msg("§aAssigning job...")

        // 1. Claim the POI in PoiManager so it's marked as "taken"
        val poiManager = level.poiManager
        val claimedPos = poiManager.take(
            { true },  // Accept any POI type
            { _, pos -> pos == jobBlockPos },  // Only this exact position
            jobBlockPos,
            1
        )
        if (claimedPos.isPresent) {
            msg("§7Claimed POI at ${claimedPos.get()}")
        } else {
            msg("§cFailed to claim POI - may not be registered")
        }

        // 2. Set profession and level 1
        val registry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
        val profHolder = registry.getOrThrow(professionKey)
        val oldData = villager.villagerData
        villager.villagerData = oldData
            .withProfession(profHolder)
            .withLevel(1)

        msg("§7Set: $professionKey lv${villager.villagerData.level}")

        // 3. Refresh brain FIRST to set up behaviors for new profession
        // This creates a NEW brain object, so any memories set before this are lost!
        villager.refreshBrain(level)

        // 4. NOW set JOB_SITE memory on the new brain (bypass claim check)
        val globalPos = GlobalPos.of(level.dimension(), jobBlockPos)
        JobAssignmentBypass.allowPosition(jobBlockPos)
        villager.brain.setMemory(MemoryModuleType.JOB_SITE, globalPos)

        // Verify memory was actually set
        val jobSiteMemory = villager.brain.getMemory(MemoryModuleType.JOB_SITE)
        if (jobSiteMemory.isPresent) {
            msg("§7JOB_SITE memory set: ${jobSiteMemory.get().pos()}")
        } else {
            msg("§cJOB_SITE memory NOT SET - bypass failed!")
        }

        // Debug: check final state
        val finalProf = villager.villagerData.profession.unwrapKey().orElse(null)
        val finalOffers = villager.offers.size
        msg("§7Final: prof=$finalProf lv${villager.villagerData.level} offers=$finalOffers")

        msg("§aJob assigned!")
    }
}
