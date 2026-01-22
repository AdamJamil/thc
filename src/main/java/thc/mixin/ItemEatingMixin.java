package thc.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Modifies eating mechanics:
 * - All consumable items take 64 ticks (3.2 seconds) to consume
 *
 * This creates combat commitment for eating - the longer duration makes eating
 * mid-fight a real tactical decision rather than instant healing.
 */
@Mixin(Item.class)
public class ItemEatingMixin {

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
}
