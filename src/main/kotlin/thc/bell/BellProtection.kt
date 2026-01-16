package thc.bell

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.world.level.block.Blocks

/**
 * Prevents players from breaking bells.
 *
 * Bells are critical infrastructure for obtaining land plots.
 * They must remain permanent world fixtures.
 */
object BellProtection {

    fun register() {
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, blockEntity ->
            // Allow break on client side (server will reject)
            if (level.isClientSide) {
                return@register true
            }

            // Block breaking bells
            if (state.`is`(Blocks.BELL)) {
                return@register false
            }

            // Allow all other blocks
            true
        }
    }
}
