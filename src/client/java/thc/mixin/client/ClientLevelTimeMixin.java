package thc.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces perpetual dusk sky rendering in the Overworld.
 *
 * <p>In Minecraft 1.21+, sky rendering uses a state-based system where
 * SkyRenderer.extractRenderState populates SkyRenderState with sun/moon angles.
 * The old Level.getTimeOfDay(float) method no longer exists.
 *
 * <p>We inject after extractRenderState completes and override sunAngle
 * for Overworld dimensions to create a perpetual dusk appearance.
 */
@Mixin(SkyRenderer.class)
public abstract class ClientLevelTimeMixin {

	// Dusk sun angle: ~11800 ticks equivalent
	// Sun angle formula: (dayTime / 24000.0 - 0.25) mod 1.0
	// For 11800 ticks: 11800/24000 - 0.25 = 0.241667
	private static final float DUSK_SUN_ANGLE = 0.241667F;

	@Inject(
		method = "extractRenderState",
		at = @At("RETURN")
	)
	private void thc$forceDuskSky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
		// Only modify Overworld sky
		if (level.dimension() == Level.OVERWORLD) {
			state.sunAngle = DUSK_SUN_ANGLE;
			// Moon and star angles follow sun
			state.moonAngle = DUSK_SUN_ANGLE + 0.5F;
			state.starAngle = DUSK_SUN_ANGLE;
		}
	}
}
