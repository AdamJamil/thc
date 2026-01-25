package thc

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import org.slf4j.LoggerFactory
import thc.client.BucklerClientState
import thc.client.BucklerHudRenderer
import thc.client.BucklerUseHandler
import thc.network.BucklerStatePayload

object THCClient : ClientModInitializer {
	private val logger = LoggerFactory.getLogger("thc")

	override fun onInitializeClient() {
		logger.info("THC client init")
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		BucklerUseHandler.register()
		ClientPlayNetworking.registerGlobalReceiver(BucklerStatePayload.TYPE) { payload, context ->
			context.client().execute {
				BucklerClientState.update(payload.poise, payload.maxPoise, payload.broken, payload.lastFullTick)
			}
		}
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, BucklerHudRenderer.POISE_ID) { guiGraphics, _ ->
			BucklerHudRenderer.render(guiGraphics)
		}
		HudStatusBarHeightRegistry.addLeft(BucklerHudRenderer.POISE_ID) { player ->
			BucklerHudRenderer.getRenderHeight(player)
		}
	}
}
