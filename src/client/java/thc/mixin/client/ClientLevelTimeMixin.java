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
 * SkyRenderer.extractRenderState populates SkyRenderState with visual properties.
 * The old Level.getTimeOfDay(float) method no longer exists.
 *
 * <p>We inject after extractRenderState and override ALL sky visual properties
 * to create perpetual dusk: sun/moon angles, sky color, star brightness, and
 * sunset glow.
 */
@Mixin(SkyRenderer.class)
public abstract class ClientLevelTimeMixin {

	// Sun angle at dusk (~13000 ticks)
	// Vanilla formula: celestialAngle * 360 degrees, then to radians
	// At 13000 ticks: celestialAngle = (13000/24000 - 0.25) mod 1 = 0.2917
	// Degrees: 0.2917 * 360 = 105 degrees (sun descending past zenith)
	// Radians: 105 * π/180 = 1.833
	private static final float DUSK_SUN_ANGLE = 1.833f;

	// Dusk sky color - twilight blue-gray RGB(90, 100, 140)
	// Darker than day (135, 206, 235) but not full night (0, 0, 0)
	private static final int DUSK_SKY_COLOR = (90 << 16) | (100 << 8) | 140;

	// Stars partially visible at dusk (0.0 = invisible, 1.0 = full night)
	private static final float DUSK_STAR_BRIGHTNESS = 0.3f;

	// Sunset orange glow ARGB - warm orange on horizon
	private static final int DUSK_SUNSET_COLOR = 0xFFE07020;

	@Inject(
		method = "extractRenderState",
		at = @At("RETURN")
	)
	private void thc$forceDuskSky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
		// Only modify Overworld sky
		if (level.dimension() == Level.OVERWORLD) {
			// Sun/moon/star positions
			state.sunAngle = DUSK_SUN_ANGLE;
			state.moonAngle = DUSK_SUN_ANGLE + (float) Math.PI;
			state.starAngle = DUSK_SUN_ANGLE;

			// Sky visual appearance - these control actual brightness
			state.skyColor = DUSK_SKY_COLOR;
			state.starBrightness = DUSK_STAR_BRIGHTNESS;
			state.sunriseAndSunsetColor = DUSK_SUNSET_COLOR;
		}
	}
}
