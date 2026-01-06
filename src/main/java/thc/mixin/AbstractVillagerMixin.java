package thc.mixin;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
	@Inject(method = "getOffers", at = @At("RETURN"))
	private void thc$removeShieldTrades(CallbackInfoReturnable<MerchantOffers> cir) {
		MerchantOffers offers = cir.getReturnValue();
		if (offers == null) {
			return;
		}
		offers.removeIf(offer -> offer.getResult().is(Items.SHIELD));
	}
}
