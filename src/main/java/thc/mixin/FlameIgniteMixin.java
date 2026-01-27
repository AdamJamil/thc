package thc.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.THCAttachments;

/**
 * Track Flame enchantment fire source and set 6-second duration.
 *
 * <p>LVL-04: Flame deals 1 dmg/s for 6 seconds (6 HP total).
 * Vanilla Flame sets 5 seconds (100 ticks). THC sets 7 seconds (140 ticks)
 * to achieve 6 damage ticks after first-tick immunity.
 */
@Mixin(AbstractArrow.class)
public abstract class FlameIgniteMixin {

	/**
	 * When arrow sets target on fire, override duration and mark source.
	 * Flame arrows call setRemainingFireTicks in onHitEntity.
	 */
	@Inject(
		method = "doPostHurtEffects",
		at = @At("TAIL")
	)
	private void thc$trackFlameFireSource(LivingEntity target, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;

		// Check if arrow is on fire (Flame enchantment or lit by other means)
		if (self.isOnFire() && target.getRemainingFireTicks() > 0) {
			// Mark fire source as flame
			target.setAttached(THCAttachments.FIRE_SOURCE, "flame");
			// Set 7 second duration for 6 damage ticks (first tick immune)
			target.setRemainingFireTicks(140);
		}
	}
}
