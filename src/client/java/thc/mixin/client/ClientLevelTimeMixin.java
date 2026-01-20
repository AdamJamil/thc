package thc.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces perpetual dusk sky rendering in the Overworld.
 * Targets Level to intercept getTimeOfDay, but only applies to ClientLevel.
 * Other dimensions (Nether, End) render their normal sky.
 */
@Mixin(Level.class)
public abstract class ClientLevelTimeMixin {

	// Dusk time: 13000 ticks / 24000 ticks = 0.541667
	private static final float DUSK_TIME = 0.541667F;

	@Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
	private void thc$forceDuskTime(float partialTick, CallbackInfoReturnable<Float> cir) {
		Level self = (Level) (Object) this;

		// Only modify client-side Overworld sky rendering
		if (self instanceof ClientLevel && self.dimension() == Level.OVERWORLD) {
			cir.setReturnValue(DUSK_TIME);
		}
		// Server levels and other dimensions pass through with vanilla values
	}
}
