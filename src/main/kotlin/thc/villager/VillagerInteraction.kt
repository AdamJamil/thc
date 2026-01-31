package thc.villager

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import thc.mixin.access.VillagerAccessor
import thc.stage.StageManager

/**
 * Handles villager emerald interactions for manual level-up.
 *
 * Right-click a villager with an emerald when they have max XP for their
 * current level to promote them to the next level. Requires the server
 * stage to match or exceed the target level (Stage 2 for Apprentice, etc.).
 */
object VillagerInteraction {

    fun register() {
        UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
            // Only handle Villager entities
            if (entity !is Villager) {
                return@register InteractionResult.PASS
            }

            // Only process server-side
            if (level.isClientSide) {
                return@register InteractionResult.PASS
            }

            val stack = player.getItemInHand(hand)

            // Only trigger on emerald in hand
            if (!stack.`is`(Items.EMERALD)) {
                return@register InteractionResult.PASS
            }

            // Handle the level-up attempt
            handleLevelUp(player as ServerPlayer, level as ServerLevel, entity, stack)
        }
    }

    private fun handleLevelUp(
        player: ServerPlayer,
        level: ServerLevel,
        villager: Villager,
        stack: ItemStack
    ): InteractionResult {
        val data = villager.villagerData
        val currentLevel = data.level
        val accessor = villager as VillagerAccessor
        val currentXp = accessor.tradingXp

        // Already master - cannot level further
        if (currentLevel >= 5) {
            player.displayClientMessage(Component.literal("Already at master!"), true)
            return InteractionResult.FAIL
        }

        val maxXp = VillagerXpConfig.getMaxXpForLevel(currentLevel)

        // At 0 XP - reserved for Phase 70 cycling, pass through
        if (currentXp == 0) {
            return InteractionResult.PASS
        }

        // Not enough XP yet
        if (currentXp < maxXp) {
            player.displayClientMessage(Component.literal("Not enough experience to level up!"), true)
            return InteractionResult.FAIL
        }

        // Check stage gate: target level requires matching stage
        val targetLevel = currentLevel + 1
        val requiredStage = targetLevel
        val currentStage = StageManager.getCurrentStage(level.server)

        if (currentStage < requiredStage) {
            player.displayClientMessage(Component.literal("Complete the next trial!"), true)
            return InteractionResult.FAIL
        }

        // Success - consume emerald and level up
        stack.shrink(1)

        // Update villager level
        villager.villagerData = data.withLevel(targetLevel)

        // Reset XP to 0
        accessor.tradingXp = 0

        // Play success effects
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            villager.x,
            villager.y + 1.0,
            villager.z,
            10,    // count
            0.5,   // spread X
            0.5,   // spread Y
            0.5,   // spread Z
            0.0    // speed
        )

        level.playSound(
            null,
            villager.blockPosition(),
            SoundEvents.VILLAGER_YES,
            SoundSource.NEUTRAL,
            1.0f,
            1.0f
        )

        return InteractionResult.SUCCESS
    }
}
