package thc.villager

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB

/**
 * Auto-assigns villager professions when placing job blocks.
 *
 * When a player places a stonecutter, smoker, cartography table, or lectern,
 * the nearest unemployed villager within 5 blocks is automatically assigned
 * the corresponding profession.
 */
object JobBlockAssignment {

    fun register() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            // Skip client side
            if (world.isClientSide) {
                return@register InteractionResult.PASS
            }

            val stack = player.getItemInHand(hand)
            val item = stack.item

            // Only process BlockItem placements
            if (item !is BlockItem) {
                return@register InteractionResult.PASS
            }

            val block = item.block
            val professionKey = AllowedProfessions.getProfessionForJobBlock(block)
                ?: return@register InteractionResult.PASS

            val level = world as? ServerLevel ?: return@register InteractionResult.PASS

            // Get placement position
            val placementPos = hitResult.blockPos.relative(hitResult.direction)
            val center = placementPos.center

            // Find nearest unemployed villager within 5 blocks
            val searchBox = AABB.ofSize(center, 10.0, 10.0, 10.0)
            val villagers = level.getEntitiesOfClass(Villager::class.java, searchBox) { villager ->
                // Filter to unemployed villagers (profession = NONE)
                val profKey = villager.villagerData.profession.unwrapKey().orElse(null)
                profKey == VillagerProfession.NONE
            }

            // Sort by distance and get nearest
            val nearestVillager = villagers
                .sortedBy { it.position().distanceToSqr(center) }
                .firstOrNull()
                ?: return@register InteractionResult.PASS

            // Assign profession
            val registry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
            val profHolder = registry.getOrThrow(professionKey)
            nearestVillager.villagerData = nearestVillager.villagerData.withProfession(profHolder)

            // Play feedback
            level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                nearestVillager.x,
                nearestVillager.y + 1.0,
                nearestVillager.z,
                10,    // count
                0.5,   // spread X
                0.5,   // spread Y
                0.5,   // spread Z
                0.0    // speed
            )

            level.playSound(
                null,
                nearestVillager.blockPosition(),
                SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL,
                1.0f,
                1.0f
            )

            // Return PASS to let vanilla block placement proceed
            InteractionResult.PASS
        }
    }
}
