package thc.mixin;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.mixin.access.FoodDataAccessor;

/**
 * Overrides FoodData.tick() to implement custom exhaustion and healing mechanics:
 * - Saturation drains 21% faster (1.21 per 4.0 exhaustion instead of 1.0)
 * - Healing requires hunger >= 18 (9 full bars)
 * - Healing rate varies by saturation tier:
 *   - T5: saturation >= 6.36 -> +1 heart/s (20 ticks/HP)
 *   - T4: saturation >= 2.73 -> +0.5 heart/s (40 ticks/HP)
 *   - T3: saturation >= 1.36 -> +3/16 heart/s (53 ticks/HP, base rate)
 *   - T2: saturation >= 0.45 -> +1/8 heart/s (80 ticks/HP)
 *   - T1: saturation < 0.45 -> +1/16 heart/s (160 ticks/HP)
 * - naturalRegeneration gamerule is bypassed (always uses custom logic)
 * - Saturation boost (rapid healing at full hunger) is disabled
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

	@Shadow
	private int foodLevel;

	@Shadow
	public abstract float getSaturationLevel();

	@Shadow
	public abstract void addExhaustion(float amount);

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void thc$overrideTick(Player player, CallbackInfo ci) {
		FoodDataAccessor accessor = (FoodDataAccessor) (Object) this;
		Difficulty difficulty = player.level().getDifficulty();

		// EXHAUSTION PROCESSING (modified: 1.21 saturation drain instead of 1.0)
		float exhaustion = accessor.getExhaustionLevel();
		if (exhaustion > 4.0F) {
			accessor.setExhaustionLevel(exhaustion - 4.0F);
			float saturation = this.getSaturationLevel();
			if (saturation > 0.0F) {
				// THC: Drain 1.21 saturation instead of vanilla 1.0
				accessor.setSaturationLevel(Math.max(saturation - 1.21F, 0.0F));
			} else if (difficulty != Difficulty.PEACEFUL) {
				this.foodLevel = Math.max(this.foodLevel - 1, 0);
			}
		}

		// CUSTOM HEALING with saturation tiers (ignores naturalRegeneration gamerule)
		// Healing requires hunger >= 18; rate depends on saturation tier
		if (this.foodLevel >= 18 && player.isHurt()) {
			float saturation = this.getSaturationLevel();

			// Determine heal interval based on saturation tier
			// T5: 6.36+ = +1 heart/s (20 ticks)
			// T4: 2.73+ = +0.5 heart/s (40 ticks)
			// T3: 1.36+ = +3/16 heart/s (53 ticks, base rate)
			// T2: 0.45+ = +1/8 heart/s (80 ticks)
			// T1: 0-0.44 = +1/16 heart/s (160 ticks)
			int healInterval;
			if (saturation >= 6.36F) {
				healInterval = 20;
			} else if (saturation >= 2.73F) {
				healInterval = 40;
			} else if (saturation >= 1.36F) {
				healInterval = 53;
			} else if (saturation >= 0.45F) {
				healInterval = 80;
			} else {
				healInterval = 160;
			}

			int timer = accessor.getTickTimer();
			accessor.setTickTimer(timer + 1);
			if (timer >= healInterval) {
				player.heal(1.0F);
				this.addExhaustion(6.0F);  // Keep vanilla exhaustion cost
				accessor.setTickTimer(0);
			}
		}
		// STARVATION (keep vanilla behavior)
		else if (this.foodLevel <= 0) {
			int timer = accessor.getTickTimer();
			accessor.setTickTimer(timer + 1);
			if (timer >= 80) {
				if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD
					|| (player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL)) {
					player.hurt(player.damageSources().starve(), 1.0F);
				}
				accessor.setTickTimer(0);
			}
		}
		// RESET TIMER (no healing or starvation)
		else {
			accessor.setTickTimer(0);
		}

		ci.cancel();  // Skip vanilla tick entirely
	}
}
