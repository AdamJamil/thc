package thc.villager

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerProfession
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

        // At 0 XP - handle trade cycling
        if (currentXp == 0) {
            return handleTradeCycling(player, level, villager, stack)
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

    /**
     * Handle trade cycling at 0 XP.
     * Right-click with emerald rerolls current level trades if pool > 1.
     */
    private fun handleTradeCycling(
        player: ServerPlayer,
        level: ServerLevel,
        villager: Villager,
        stack: ItemStack
    ): InteractionResult {
        val data = villager.villagerData
        val currentLevel = data.level

        // Get profession key for trade table lookup
        val profKey = data.profession.unwrapKey().orElse(null)
            ?: return InteractionResult.PASS

        // Only handle professions with custom trades
        if (!CustomTradeTables.hasCustomTrades(profKey)) {
            return InteractionResult.PASS
        }

        // Check if cycling is possible (pool > 1)
        val poolSize = CustomTradeTables.getTradePoolSize(profKey, currentLevel)
        if (poolSize <= 1) {
            // Single-trade pool - cannot cycle, play failure sound
            playFailureEffects(villager, level)
            return InteractionResult.SUCCESS // Block GUI, no emerald consumed
        }

        // Success - consume emerald and cycle trades
        stack.shrink(1)
        cycleCurrentLevelTrades(villager, profKey, currentLevel, level)
        playSuccessEffects(villager, level)

        return InteractionResult.SUCCESS
    }

    /**
     * Remove current-level trades and regenerate from trade table.
     * Earlier level trades are preserved unchanged.
     */
    private fun cycleCurrentLevelTrades(
        villager: Villager,
        profKey: ResourceKey<VillagerProfession>,
        currentLevel: Int,
        level: ServerLevel
    ) {
        val offers = villager.offers

        // Calculate how many trades exist before current level
        val tradesBeforeCurrentLevel = (1 until currentLevel).sumOf {
            CustomTradeTables.getTradeCount(profKey, it)
        }

        // Remove current level trades (they're at the end of the list)
        while (offers.size > tradesBeforeCurrentLevel) {
            offers.removeAt(offers.size - 1)
        }

        // Regenerate current-level trades with fresh random
        val newTrades = CustomTradeTables.getTradesFor(
            profKey,
            currentLevel,
            level,
            level.random
        )
        offers.addAll(newTrades)
    }

    /**
     * Play success feedback: particles + sound
     */
    private fun playSuccessEffects(villager: Villager, level: ServerLevel) {
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
    }

    /**
     * Play failure feedback: sound only (villager head shake)
     */
    private fun playFailureEffects(villager: Villager, level: ServerLevel) {
        level.playSound(
            null,
            villager.blockPosition(),
            SoundEvents.VILLAGER_NO,
            SoundSource.NEUTRAL,
            1.0f,
            1.0f
        )
    }
}
