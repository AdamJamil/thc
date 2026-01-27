package thc.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.THCAttachments;

/**
 * Track Fire Aspect enchantment fire source and set 6-second duration.
 *
 * <p>LVL-05: Fire Aspect deals 1.5 dmg/s for 6 seconds (9 HP total).
 * Vanilla Fire Aspect sets 4 seconds. THC sets 7 seconds (140 ticks)
 * for 6 damage ticks, with 1.5 dmg/s handled in LivingEntityFireMixin.
 */
@Mixin(Player.class)
public abstract class FireAspectIgniteMixin {

	/**
	 * After player attack, check if target was set on fire and mark source.
	 *
	 * <p>Fire Aspect ignition happens in Player.attack() after damage is dealt.
	 * We inject at TAIL to detect when fire was applied.
	 */
	@Inject(
		method = "attack",
		at = @At("TAIL")
	)
	private void thc$trackFireAspectSource(Entity target, CallbackInfo ci) {
		if (!(target instanceof LivingEntity living)) {
			return;
		}

		// Check if target is now on fire (Fire Aspect was applied)
		if (living.getRemainingFireTicks() > 0) {
			// Check if this was from Fire Aspect (player's weapon has enchantment)
			// We can't easily check the enchantment here, but if target is on fire
			// after melee attack, assume Fire Aspect (other fire sources tracked separately)
			String currentSource = living.getAttached(THCAttachments.FIRE_SOURCE);

			// Only override if not already set (e.g., from Flame arrow)
			if (currentSource == null) {
				living.setAttached(THCAttachments.FIRE_SOURCE, "fire_aspect");
				// Set 7 second duration for 6 damage ticks
				living.setRemainingFireTicks(140);
			}
		}
	}
}
