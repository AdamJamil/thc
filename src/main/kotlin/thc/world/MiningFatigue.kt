package thc.world

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import thc.claim.ChunkValidator
import thc.claim.ClaimManager
import thc.world.WorldRestrictions
import java.util.UUID

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
     * Maximum amplifier for mining fatigue (level 10 display = amplifier 9).
     * Caps stacking to prevent extreme mining slowdown.
     */
    private const val MAX_AMPLIFIER = 9

    /**
     * Tracks players with mining fatigue.
     * Maps player UUID to their current fatigue amplifier when effect was applied.
     * Used for decay logic to know when to reapply at lower level.
     */
    private val trackedPlayers = mutableMapOf<UUID, Int>()

    /**
     * Registers the block break event handler and tick handler for decay.
     */
    fun register() {
        // Register tick handler for fatigue decay
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            for (player in server.playerList.players) {
                val effect = player.getEffect(MobEffects.MINING_FATIGUE)

                if (effect == null) {
                    // No effect, clean up tracking
                    trackedPlayers.remove(player.uuid)
                    continue
                }

                // Check if effect is about to expire (duration <= 1 tick)
                if (effect.duration <= 1) {
                    val currentAmplifier = effect.amplifier

                    if (currentAmplifier > 0) {
                        // Decay to lower level: remove and reapply at amplifier-1
                        player.removeEffect(MobEffects.MINING_FATIGUE)
                        val newAmplifier = currentAmplifier - 1
                        player.addEffect(MobEffectInstance(MobEffects.MINING_FATIGUE, DECAY_TICKS, newAmplifier))
                        trackedPlayers[player.uuid] = newAmplifier
                    } else {
                        // Amplifier 0 (Fatigue I) - let it expire naturally
                        // Clean up tracking on next tick when effect is gone
                    }
                }
            }
        })

        // Register block break handler for fatigue application
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

            // No fatigue for mining ores - allows resource gathering without penalty
            if (isOre(state)) {
                return@register true
            }

            // No fatigue for exempt block categories (flowers, grass, glass, beds, gravel)
            if (isExemptBlock(state)) {
                return@register true
            }

            // No fatigue for placeable-anywhere blocks (torches, chests, crafting tables, etc.)
            if (WorldRestrictions.ALLOWED_BLOCKS.contains(state.block)) {
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
            // Stack: increment amplifier, cap at MAX_AMPLIFIER (level 10)
            minOf(currentEffect.amplifier + 1, MAX_AMPLIFIER)
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

        // Track for decay logic
        trackedPlayers[player.uuid] = newAmplifier
    }

    /**
     * Checks if a block state is an ore block.
     * Ores are exempt from mining fatigue to allow resource gathering.
     */
    private fun isOre(state: BlockState): Boolean {
        return state.`is`(BlockTags.COAL_ORES) ||
            state.`is`(BlockTags.IRON_ORES) ||
            state.`is`(BlockTags.COPPER_ORES) ||
            state.`is`(BlockTags.GOLD_ORES) ||
            state.`is`(BlockTags.REDSTONE_ORES) ||
            state.`is`(BlockTags.LAPIS_ORES) ||
            state.`is`(BlockTags.DIAMOND_ORES) ||
            state.`is`(BlockTags.EMERALD_ORES) ||
            state.`is`(Blocks.NETHER_QUARTZ_ORE)
    }

    /**
     * Checks if a block state is exempt from mining fatigue.
     * Includes flowers, grass-type blocks, glass, beds, and gravel.
     */
    private fun isExemptBlock(state: BlockState): Boolean {
        return state.`is`(BlockTags.FLOWERS) ||     // All flower variants (17+)
            state.`is`(BlockTags.DIRT) ||           // Grass blocks, podzol, mycelium, moss, mud
            state.`is`(BlockTags.IMPERMEABLE) ||    // All glass variants (plain, 16 stained, tinted, barrier)
            state.`is`(BlockTags.BEDS) ||           // All 16 bed colors
            state.`is`(Blocks.GRAVEL)               // Gravel blocks
    }
}
