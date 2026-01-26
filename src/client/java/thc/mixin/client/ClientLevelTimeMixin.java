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

	// Dusk sun angle in RADIANS (vanilla multiplies degrees by π/180)
	// Sunset is at 180 degrees = π radians ≈ 3.14159
	// Dusk (sun below horizon) is around 200 degrees = 3.49 radians
	private static final float DUSK_SUN_ANGLE = 3.49f;

	@Inject(
		method = "extractRenderState",
		at = @At("RETURN")
	)
	private void thc$forceDuskSky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
		// Only modify Overworld sky
		if (level.dimension() == Level.OVERWORLD) {
			state.sunAngle = DUSK_SUN_ANGLE;
			// Moon is opposite sun (π radians offset)
			state.moonAngle = DUSK_SUN_ANGLE + (float) Math.PI;
			state.starAngle = DUSK_SUN_ANGLE;
		}
	}
}
