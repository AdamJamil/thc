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
 * - Healing rate: 3/16 hearts/second (1 HP every 53 ticks)
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

		// CUSTOM HEALING (ignores naturalRegeneration gamerule)
		// Heal at 3/16 hearts/second = 1 HP every ~53 ticks when hunger >= 18
		if (this.foodLevel >= 18 && player.isHurt()) {
			int timer = accessor.getTickTimer();
			accessor.setTickTimer(timer + 1);
			if (timer >= 53) {
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
