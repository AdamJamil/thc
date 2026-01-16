package thc

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Items
import net.minecraft.world.level.gamerules.GameRules
import org.slf4j.LoggerFactory
import thc.buckler.BucklerState
import thc.buckler.BucklerStatsRegistry
import thc.item.BucklerItem
import thc.item.THCBucklers
import thc.item.THCItems
import thc.network.BucklerSync
import thc.network.BucklerStatePayload

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
		THCBucklers.init()
		THCItems.init()
		THCSounds.init()
		PayloadTypeRegistry.playS2C().register(BucklerStatePayload.TYPE, BucklerStatePayload.STREAM_CODEC)

		ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
			updateBucklerState(server)
		})

		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			BucklerSync.clear(handler.player)
		})

		ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server ->
			server.allLevels.forEach { world ->
				lockWorldToNight(server, world)
			}
		})

		LootTableEvents.MODIFY_DROPS.register(LootTableEvents.ModifyDrops { _, _, drops ->
			drops.removeIf { stack -> stack.`is`(Items.SHIELD) }
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

	private fun updateBucklerState(server: MinecraftServer) {
		for (player in server.playerList.players) {
			val tick = player.level().gameTime
			val isRaised = BucklerItem.isBucklerRaised(player)
			val raiseTick = BucklerState.getRaiseTick(player)
			if (isRaised) {
				if (raiseTick < 0L) {
					BucklerState.setRaiseTick(player, tick)
				}
			} else if (raiseTick >= 0L) {
				BucklerState.setRaiseTick(player, -1L)
			}

			val stats = BucklerStatsRegistry.forStack(player.offhandItem)
			if (stats != null) {
				BucklerState.setMaxPoise(player, stats.maxPoiseHearts)
			}
			val maxPoise = if (stats != null) stats.maxPoiseHearts else BucklerState.getMaxPoise(player)
			if (maxPoise <= 0.0) {
				BucklerSync.sync(player)
				continue
			}

			var poise = BucklerState.getPoise(player)
			if (poise > maxPoise) {
				poise = maxPoise
				BucklerState.setPoise(player, poise)
			}

			if (isRaised) {
				val drainPerTick = 0.5 / 10.0
				if (drainPerTick > 0.0 && poise > 0.0) {
					val updatedPoise = kotlin.math.max(0.0, poise - drainPerTick)
					BucklerState.setPoise(player, updatedPoise)
					if (updatedPoise <= 0.0) {
						BucklerState.setBroken(player, true)
						player.stopUsingItem()
					}
				}
			} else {
				val regenPerTick = maxPoise / 4.0 / 20.0
				if (regenPerTick > 0.0 && poise < maxPoise) {
					val updatedPoise = kotlin.math.min(maxPoise, poise + regenPerTick)
					BucklerState.setPoise(player, updatedPoise)
					if (updatedPoise >= maxPoise) {
						BucklerState.setLastFullTick(player, tick)
						if (BucklerState.isBroken(player)) {
							BucklerState.setBroken(player, false)
						}
					}
				}
			}

			BucklerSync.sync(player)
		}
	}

	private fun lockWorldToNight(server: MinecraftServer, world: ServerLevel) {
		world.gameRules.set(GameRules.ADVANCE_TIME, false, server)
        world.dayTime = NIGHT_TIME
	}
}
