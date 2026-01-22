package thc.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.mixin.access.FoodDataAccessor;

/**
 * Modifies eating mechanics:
 * - All consumable items take 64 ticks (3.2 seconds) to consume
 * - Saturation cap: eating preserves max(current_saturation, food_saturation)
 *
 * This creates combat commitment for eating - the longer duration makes eating
 * mid-fight a real tactical decision rather than instant healing.
 * The saturation cap prevents wasting high-saturation foods when already well-fed.
 */
@Mixin(Item.class)
public class ItemEatingMixin {

	/**
	 * Thread-local storage for saturation level before eating.
	 * Used to implement saturation cap behavior.
	 */
	@Unique
	private static final ThreadLocal<Float> thc$priorSaturation = new ThreadLocal<>();

	/**
	 * Override eating duration to 64 ticks for all consumable items.
	 * Vanilla default is typically 32 ticks (1.6 seconds).
	 */
	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void thc$extendEatingDuration(ItemStack itemStack, LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
		Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
		if (consumable != null) {
			cir.setReturnValue(64);
		}
		// Non-consumable items fall through to vanilla handling
	}

	/**
	 * Capture saturation level before eating for saturation cap logic.
	 */
	@Inject(method = "finishUsingItem", at = @At("HEAD"))
	private void thc$captureSaturationBeforeEating(ItemStack itemStack, Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
		if (livingEntity instanceof Player player && itemStack.get(DataComponents.CONSUMABLE) != null) {
			thc$priorSaturation.set(player.getFoodData().getSaturationLevel());
		}
	}

	/**
	 * Apply saturation cap after eating: max(prior_saturation, new_saturation).
	 * This prevents wasting high-saturation foods when already well-fed.
	 */
	@Inject(method = "finishUsingItem", at = @At("RETURN"))
	private void thc$applySaturationCap(ItemStack itemStack, Level level, LivingEntity livingEntity, CallbackInfoReturnable<ItemStack> cir) {
		Float priorSaturation = thc$priorSaturation.get();
		if (priorSaturation != null && livingEntity instanceof Player player) {
			FoodData foodData = player.getFoodData();
			float newSaturation = foodData.getSaturationLevel();
			float maxSaturation = Math.max(priorSaturation, newSaturation);
			((FoodDataAccessor) foodData).setSaturationLevel(maxSaturation);
			thc$priorSaturation.remove(); // Clean up thread-local
		}
	}
}
