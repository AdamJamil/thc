package thc

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.gamerules.GameRules
import org.slf4j.LoggerFactory

object THC : ModInitializer {
	private val logger = LoggerFactory.getLogger("thc")
	private const val NIGHT_TIME = 18000L
	private const val SMOKE_TEST_PROPERTY = "thc.smokeTest"
	private const val SMOKE_TEST_TICKS = 100

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		logger.info("Hello Fabric world!")
		THCAttachments.init()

		ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server ->
			server.allLevels.forEach { world ->
				lockWorldToNight(server, world)
			}
		})

		if (java.lang.Boolean.getBoolean(SMOKE_TEST_PROPERTY)) {
			var tickCount = 0
			var stopRequested = false
			ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
				if (stopRequested) {
					return@EndTick
				}

				tickCount++
				if (tickCount >= SMOKE_TEST_TICKS) {
					stopRequested = true
					logger.info("Smoke test complete, stopping server.")
					server.halt(false)
				}
			})
		}
	}

	private fun lockWorldToNight(server: MinecraftServer, world: ServerLevel) {
		world.gameRules.set(GameRules.ADVANCE_TIME, false, server)
        world.dayTime = NIGHT_TIME
	}
}
