package thc

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import org.slf4j.LoggerFactory
import thc.client.BucklerClientState
import thc.client.BucklerHudRenderer
import thc.client.BucklerUseHandler
import thc.client.EffectsGuiConfig
import thc.client.EffectsHudRenderer
import thc.client.DownedBodyRenderer
import thc.client.DownedPlayersClientState
import thc.client.IronBoatRenderer
import thc.client.RevivalClientState
import thc.client.RevivalProgressRenderer
import thc.entity.THCEntities
import thc.network.BucklerStatePayload
import thc.network.DownedPlayersPayload
import thc.network.RevivalStatePayload

object THCClient : ClientModInitializer {
	private val logger = LoggerFactory.getLogger("thc")

	override fun onInitializeClient() {
		logger.info("THC client init")
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		// Load effects GUI scale config
		EffectsGuiConfig.load()

		// Entity renderers
		EntityRendererRegistry.register(THCEntities.IRON_BOAT) { context ->
			IronBoatRenderer(context)
		}

		BucklerUseHandler.register()
		ClientPlayNetworking.registerGlobalReceiver(BucklerStatePayload.TYPE) { payload, context ->
			context.client().execute {
				BucklerClientState.update(payload.poise, payload.maxPoise, payload.broken, payload.lastFullTick)
			}
		}
		ClientPlayNetworking.registerGlobalReceiver(RevivalStatePayload.TYPE) { payload, context ->
			context.client().execute {
				RevivalClientState.update(
					payload.downedPlayerUUID(),
					payload.downedX(),
					payload.downedY(),
					payload.downedZ(),
					payload.progress()
				)
			}
		}
		logger.info("[THCClient] Registering DownedPlayersPayload receiver")
		ClientPlayNetworking.registerGlobalReceiver(DownedPlayersPayload.TYPE) { payload, context ->
			logger.info("[THCClient] Received DownedPlayersPayload with {} entries", payload.entries().size)
			context.client().execute {
				logger.info("[THCClient] Executing update on client thread")
				DownedPlayersClientState.update(payload.entries())
			}
		}
		logger.info("[THCClient] Registering DownedBodyRenderer")
		DownedBodyRenderer.register()
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, RevivalProgressRenderer.REVIVAL_PROGRESS_ID) { guiGraphics, _ ->
			RevivalProgressRenderer.render(guiGraphics)
		}
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, BucklerHudRenderer.POISE_ID) { guiGraphics, _ ->
			BucklerHudRenderer.render(guiGraphics)
		}
		HudStatusBarHeightRegistry.addLeft(BucklerHudRenderer.POISE_ID) { player ->
			BucklerHudRenderer.getRenderHeight(player)
		}
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, EffectsHudRenderer.EFFECTS_HUD_ID) { guiGraphics, _ ->
			EffectsHudRenderer.render(guiGraphics)
		}
		// Remove vanilla status effects from HUD (inventory GUI effects remain)
		HudElementRegistry.removeElement(VanillaHudElements.STATUS_EFFECTS)
	}
}
