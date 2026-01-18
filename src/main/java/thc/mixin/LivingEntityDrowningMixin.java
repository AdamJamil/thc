package thc.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Slows drowning damage tick rate from every 1 second to every 4 seconds.
 *
 * <p>Vanilla drowning mechanics:
 * <ul>
 *   <li>Air supply starts at 300 ticks (15 seconds)</li>
 *   <li>Air decreases by 1 each tick while underwater</li>
 *   <li>When air reaches -20, entity takes 2 damage (1 heart) and air resets to 0</li>
 *   <li>This creates a damage tick every ~20 ticks (1 second)</li>
 * </ul>
 *
 * <p>THC modification: Only apply drowning damage every 4th time it would occur,
 * effectively reducing damage rate to every 4 seconds instead of every 1 second.
 * This makes underwater exploration more forgiving while maintaining danger over time.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDrowningMixin {

	/**
	 * Counter to track drowning damage occurrences.
	 * Only every 4th damage event is actually applied.
	 * Resets when entity surfaces (tracked via positive air supply).
	 */
	@Unique
	private int thc$drowningDamageCounter = 0;

	/**
	 * Track the last air supply value to detect when entity surfaces.
	 */
	@Unique
	private int thc$lastAirSupply = 300;

	/**
	 * Intercepts damage application and blocks 3 out of 4 drowning damage ticks.
	 *
	 * @param source The damage source
	 * @param amount The damage amount
	 * @param cir Callback info for returning early
	 */
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void thc$slowDrowningDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!source.is(DamageTypes.DROWN)) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		int currentAir = self.getAirSupply();

		// Reset counter when entity surfaces (air supply becomes positive/normal)
		if (currentAir > 0 && thc$lastAirSupply <= 0) {
			thc$drowningDamageCounter = 0;
		}
		thc$lastAirSupply = currentAir;

		// Increment counter and only allow damage every 4th tick
		thc$drowningDamageCounter++;
		if (thc$drowningDamageCounter < 4) {
			// Block this drowning damage - not the 4th tick yet
			cir.setReturnValue(false);
			return;
		}

		// Reset counter and allow damage through (this is the 4th tick)
		thc$drowningDamageCounter = 0;
	}
}
