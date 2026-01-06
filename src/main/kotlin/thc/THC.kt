package thc

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.gamerules.GameRules
import org.slf4j.LoggerFactory

object THC : ModInitializer {
	private val logger = LoggerFactory.getLogger("thc")
	private const val NIGHT_TIME = 18000L

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		logger.info("Hello Fabric world!")

		ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server ->
			server.allLevels.forEach { world ->
				lockWorldToNight(server, world)
			}
		})
	}

	private fun lockWorldToNight(server: MinecraftServer, world: ServerLevel) {
		world.gameRules.set(GameRules.ADVANCE_TIME, false, server)
        world.dayTime = NIGHT_TIME
	}
}
