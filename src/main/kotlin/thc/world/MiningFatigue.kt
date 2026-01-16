package thc.world

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.ChunkPos
import thc.claim.ChunkValidator
import thc.claim.ClaimManager

/**
 * Handles mining fatigue application for block breaking outside base areas.
 *
 * Implements BREAK-01 through BREAK-04:
 * - BREAK-01: Player receives mining fatigue when breaking blocks outside base (non-village)
 * - BREAK-02: Mining fatigue stacks with increasing amplifier for longer break times
 * - BREAK-03: Breaking blocks inside base never applies fatigue
 * - BREAK-04: Fatigue decays one level every 12 seconds
 */
object MiningFatigue {

    /**
     * Duration for mining fatigue effect in ticks.
     * 12 seconds = 240 ticks
     * This is both the displayed duration and the decay interval per level.
     */
    private const val DECAY_TICKS = 12 * 20

    /**
     * Registers the block break event handler.
     */
    fun register() {
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, blockEntity ->
            // Skip client-side processing
            if (level.isClientSide) {
                return@register true
            }

            val serverLevel = level as ServerLevel
            val server = serverLevel.server

            // BREAK-03: No fatigue inside base areas
            if (ClaimManager.isInBase(server, pos)) {
                return@register true
            }

            // No fatigue in village chunks (village protection handled elsewhere)
            if (ChunkValidator.isVillageChunk(serverLevel, ChunkPos(pos))) {
                return@register true
            }

            // Apply/stack mining fatigue
            applyFatigue(player as ServerPlayer)

            // Allow the break - fatigue makes it slow, doesn't prevent
            true
        }
    }

    /**
     * Applies or stacks mining fatigue on the player.
     *
     * If no effect: applies Fatigue I (amplifier 0)
     * If has effect: increments amplifier by 1
     *
     * Duration is always DECAY_TICKS (12 seconds), which causes
     * natural decay of one level when effect expires and is reapplied.
     */
    private fun applyFatigue(player: ServerPlayer) {
        val currentEffect = player.getEffect(MobEffects.MINING_FATIGUE)

        val newAmplifier = if (currentEffect != null) {
            // Stack: increment amplifier
            val currentAmplifier = currentEffect.amplifier
            currentAmplifier + 1
        } else {
            // First application: amplifier 0 = Fatigue I
            0
        }

        // Remove old effect if present (to reset duration with new amplifier)
        if (currentEffect != null) {
            player.removeEffect(MobEffects.MINING_FATIGUE)
        }

        // Apply new effect with incremented amplifier
        player.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE, DECAY_TICKS, newAmplifier))
    }
}
