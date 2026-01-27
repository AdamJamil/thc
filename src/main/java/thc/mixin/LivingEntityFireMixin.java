package thc.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.THCAttachments;

/**
 * Custom fire damage rates for Flame and Fire Aspect enchantments.
 *
 * <p>LVL-04: Flame deals 1 damage per second for 6 seconds (6 HP total)
 * <p>LVL-05: Fire Aspect deals 1.5 damage per second for 6 seconds (9 HP total)
 *
 * <p>Implementation approach:
 * - Normal fire: 1 HP per second (vanilla behavior)
 * - Flame: 1 HP per second (same as vanilla, 6s duration handled elsewhere)
 * - Fire Aspect: 1.5 HP per second (needs accumulator for half-heart damage)
 *
 * <p>Fire damage occurs in baseTick() when entity has remainingFireTicks > 0.
 * Vanilla deals 1 HP every 20 ticks (1 second). We intercept and modify.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFireMixin {

	/**
	 * Accumulator for Fire Aspect half-damage ticks.
	 * Fire Aspect deals 1.5 dmg/s, meaning 1 HP one second, 2 HP next second, alternating.
	 * Track accumulated "half damage" to apply every other second.
	 */
	@Unique
	private float thc$fireAspectAccumulator = 0.0f;

	/**
	 * Tracks the last game tick when fire damage was processed.
	 * Used to detect when vanilla fire damage tick occurs.
	 */
	@Unique
	private long thc$lastFireDamageTick = -1;

	/**
	 * Intercept fire damage tick to apply custom damage rates.
	 *
	 * <p>Vanilla fire damage happens in Entity.baseTick() when:
	 * - remainingFireTicks > 0
	 * - remainingFireTicks % 20 == 0 (every second)
	 *
	 * <p>We inject at the start of baseTick to handle our custom logic.
	 */
	@Inject(method = "baseTick", at = @At("HEAD"))
	private void thc$customFireDamage(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;

		// Only process if on fire
		if (self.getRemainingFireTicks() <= 0) {
			thc$fireAspectAccumulator = 0.0f;
			return;
		}

		// Check fire source
		String fireSource = self.getAttached(THCAttachments.FIRE_SOURCE);

		// Only Fire Aspect needs custom handling (1.5 dmg/s)
		// Flame is 1 dmg/s which matches vanilla, no modification needed
		if (!"fire_aspect".equals(fireSource)) {
			return;
		}

		// Fire Aspect: deal 1.5 HP per second
		// This means alternating 1 HP and 2 HP each second
		// We accumulate 0.5 extra damage each second and deal it when >= 1.0
		long currentTick = self.level().getGameTime();

		// Detect if this is a fire damage tick (every 20 ticks while on fire)
		// Vanilla: remainingFireTicks % 20 == 0
		if (self.getRemainingFireTicks() % 20 == 0 && currentTick != thc$lastFireDamageTick) {
			thc$lastFireDamageTick = currentTick;

			// Accumulate the extra 0.5 damage per second
			thc$fireAspectAccumulator += 0.5f;

			// When we've accumulated a full point, deal extra damage
			if (thc$fireAspectAccumulator >= 1.0f) {
				thc$fireAspectAccumulator -= 1.0f;
				// Deal 1 extra damage (vanilla already deals 1, so total is 2 this tick)
				self.hurt(self.damageSources().onFire(), 1.0f);
			}
			// Otherwise vanilla deals its normal 1 damage
		}
	}
}
