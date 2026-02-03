package thc.client

import com.mojang.authlib.GameProfile
import com.mojang.math.Axis
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.world.phys.Vec3
import org.slf4j.LoggerFactory
import thc.mixin.client.access.CameraAccessor
import java.util.UUID

/**
 * Renders downed player bodies and spawns red particles at their locations.
 * Uses DummyDownedPlayer (extends RemotePlayer) for proper skin rendering.
 */
object DownedBodyRenderer {
    private val LOGGER = LoggerFactory.getLogger("thc.DownedBodyRenderer")
    private const val PARTICLE_INTERVAL_TICKS = 5

    private var tickCounter = 0
    private var debugCounter = 0

    // Cache of dummy player entities by UUID
    private val dummyCache = mutableMapOf<UUID, DummyDownedPlayer>()

    fun register() {
        LOGGER.info("[RENDERER] Registering DownedBodyRenderer")

        // Particle spawning on client tick
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.level != null && client.player != null) {
                tickCounter++
                debugCounter++

                if (debugCounter >= 100) {
                    debugCounter = 0
                    val downedPlayers = DownedPlayersClientState.getDownedPlayers()
                    LOGGER.info("[RENDERER] Tick check - downed players in cache: {}", downedPlayers.size)
                    for ((uuid, info) in downedPlayers) {
                        LOGGER.info("[RENDERER]   - {} at ({}, {}, {}) yaw={}", info.name, info.x, info.y, info.z, info.yaw)
                    }
                }

                if (tickCounter >= PARTICLE_INTERVAL_TICKS) {
                    tickCounter = 0
                    spawnDownedParticles()
                }

                // Clean up dummy cache for players no longer downed
                val currentDowned = DownedPlayersClientState.getDownedPlayers().keys
                dummyCache.keys.removeAll { it !in currentDowned }
            }
        }

        // Body rendering after entities
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val client = Minecraft.getInstance()
            val level = client.level ?: return@register
            val camera = client.gameRenderer.mainCamera
            val cameraPos = (camera as CameraAccessor).position

            val stack = context.matrices()
            val partialTick = client.deltaTracker.getGameTimeDeltaTicks()
            val gameTime = level.gameTime

            for ((uuid, info) in DownedPlayersClientState.getDownedPlayers()) {
                val dummy = getOrCreateDummy(level, uuid, info.name)

                // Position the dummy entity at the correct world location
                dummy.setPos(info.x, info.y, info.z)
                dummy.yRotO = info.yaw
                dummy.yRot = info.yaw

                stack.pushPose()

                // Translate to world position relative to camera
                stack.translate(info.x - cameraPos.x, info.y - cameraPos.y, info.z - cameraPos.z)

                // Rotate for facing direction
                stack.mulPose(Axis.YP.rotationDegrees(-info.yaw))

                // Rotate 90° to lie on back
                stack.mulPose(Axis.XP.rotationDegrees(-90f))

                // Offset for ground position (adjust so body lies on ground)
                stack.translate(0.0, -1.0, 2.01 / 16.0)

                // Render the entity using the Java helper
                try {
                    DownedBodyRendererHelper.renderDummy(
                        dummy,
                        stack,
                        context,
                        partialTick
                    )
                } catch (e: Exception) {
                    LOGGER.error("[RENDERER] Error rendering downed body for {}: {}", info.name, e.message, e)
                }

                stack.popPose()

                // Render beacon beam at downed player location (visible from distance)
                try {
                    BeaconBeamHelper.renderBeam(
                        stack,
                        info.x - cameraPos.x,
                        info.y - cameraPos.y,
                        info.z - cameraPos.z,
                        BeaconBeamHelper.DOWNED_RED,
                        gameTime
                    )
                } catch (e: Exception) {
                    LOGGER.error("[RENDERER] Error rendering beacon beam for {}: {}", info.name, e.message, e)
                }
            }
        }
    }

    private fun getOrCreateDummy(level: ClientLevel, uuid: UUID, name: String): DummyDownedPlayer {
        return dummyCache.getOrPut(uuid) {
            LOGGER.info("[RENDERER] Creating DummyDownedPlayer for {} ({})", name, uuid)
            DummyDownedPlayer(level, GameProfile(uuid, name))
        }
    }

    private fun spawnDownedParticles() {
        val client = Minecraft.getInstance()
        val level = client.level ?: return

        val downedPlayers = DownedPlayersClientState.getDownedPlayers()
        if (downedPlayers.isEmpty()) return

        for ((uuid, info) in downedPlayers) {
            val bodyPos = Vec3(info.x, info.y, info.z)

            LOGGER.info("[RENDERER] Spawning particles for {} at ({}, {}, {})",
                info.name, bodyPos.x, bodyPos.y, bodyPos.z)

            repeat(3) {
                val random = level.random
                val offsetX = (random.nextDouble() - 0.5) * 1.2
                val offsetY = random.nextDouble() * 0.8 + 0.2
                val offsetZ = (random.nextDouble() - 0.5) * 1.2

                val particle = DustParticleOptions(0xFFE61919.toInt(), 1.8f)

                level.addParticle(
                    particle,
                    bodyPos.x + offsetX,
                    bodyPos.y + offsetY,
                    bodyPos.z + offsetZ,
                    0.0, 0.03, 0.0
                )
            }
        }
    }
}
