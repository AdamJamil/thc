package thc.client

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.world.phys.Vec3

/**
 * Spawns red particles at downed player locations for visibility.
 * Particles allow spotting downed teammates from a distance.
 */
object DownedBodyRenderer {
    private const val PARTICLE_INTERVAL_TICKS = 5  // Spawn particles every 5 ticks

    private var tickCounter = 0

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.level != null && client.player != null) {
                tickCounter++
                if (tickCounter >= PARTICLE_INTERVAL_TICKS) {
                    tickCounter = 0
                    spawnDownedParticles()
                }
            }
        }
    }

    private fun spawnDownedParticles() {
        val client = Minecraft.getInstance()
        val level = client.level ?: return

        val downedPlayers = DownedPlayersClientState.getDownedPlayers()
        if (downedPlayers.isEmpty()) return

        for ((_, info) in downedPlayers) {
            val bodyPos = Vec3(info.x, info.y, info.z)

            // Spawn multiple red dust particles around the body
            repeat(3) {
                val random = level.random
                val offsetX = (random.nextDouble() - 0.5) * 1.2
                val offsetY = random.nextDouble() * 0.8 + 0.2
                val offsetZ = (random.nextDouble() - 0.5) * 1.2

                // Red color as ARGB int (0xFFE61919 = fully opaque red)
                val particle = DustParticleOptions(0xFFE61919.toInt(), 1.8f)

                level.addParticle(
                    particle,
                    bodyPos.x + offsetX,
                    bodyPos.y + offsetY,
                    bodyPos.z + offsetZ,
                    0.0, 0.03, 0.0  // Slight upward drift
                )
            }
        }
    }
}
