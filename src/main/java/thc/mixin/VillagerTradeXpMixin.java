package thc.mixin;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks XP orb spawning from villager trading.
 *
 * <p>THC enforces that XP should only come from combat (killing mobs) or
 * rare/expensive items (experience bottles). Trading with villagers no longer awards XP.
 *
 * <p>Uses HEAD injection with cancellation on the rewardTradeXp method to
 * completely prevent XP orb spawning when trades are completed.
 */
@Mixin(AbstractVillager.class)
public abstract class VillagerTradeXpMixin {

	/**
	 * Cancels XP reward from villager trading.
	 *
	 * <p>This intercepts the rewardTradeXp method and prevents it from executing,
	 * blocking any XP orbs from spawning when a trade is completed.
	 *
	 * @param offer The trade offer that was completed
	 * @param ci Callback info for cancellation
	 */
	@Inject(method = "rewardTradeXp", at = @At("HEAD"), cancellable = true)
	private void thc$blockTradeXp(MerchantOffer offer, CallbackInfo ci) {
		// Completely cancel XP orb spawning from trading
		ci.cancel();
	}
}
