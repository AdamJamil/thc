package thc.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevent undead mobs from burning in sunlight.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where
 * undead roam freely regardless of time of day. Zombies, skeletons,
 * and phantoms all inherit from Mob, so targeting this class covers
 * all undead entities.
 *
 * <p>This ONLY affects sun burning. Fire damage from other sources
 * (lava, fire aspect enchantment, flaming arrows) use different code
 * paths and remain unaffected.
 */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {

	/**
	 * Intercept sun burn check to always return false.
	 *
	 * <p>The isSunBurnTick method is called by undead mobs to check if they
	 * should take sun damage. By returning false, we prevent sun burning
	 * entirely while preserving all other fire damage mechanics.
	 */
	@Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
	private void thc$preventSunBurn(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
