package thc.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.villager.VillagerXpConfig;

/**
 * Block automatic villager leveling and cap XP at level thresholds.
 *
 * <p>THC replaces vanilla's automatic level-up-on-XP-threshold system with
 * manual emerald-payment advancement. This mixin:
 * <ul>
 *   <li>Blocks automatic leveling by making shouldIncreaseLevel always return false</li>
 *   <li>Caps XP at the max for current level to prevent overflow</li>
 * </ul>
 *
 * <p>Manual leveling is handled separately via UseEntityCallback (Phase 69-02).
 */
@Mixin(Villager.class)
public abstract class VillagerLevelingMixin {

    @Shadow
    private int tradingXp;

    @Shadow
    public abstract VillagerData getVillagerData();

    /**
     * Block automatic villager leveling (VLEV-01).
     *
     * <p>Vanilla calls shouldIncreaseLevel() after trading to check if the
     * villager should level up. By always returning false, we prevent
     * the vanilla increaseMerchantCareer() call entirely.
     *
     * <p>Level advancement is handled manually via emerald payment instead.
     */
    @Inject(method = "shouldIncreaseLevel", at = @At("HEAD"), cancellable = true)
    private void thc$blockAutoLeveling(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Cap villager XP at the maximum for their current level (VLEV-05).
     *
     * <p>After vanilla awards trade XP via rewardTradeXp(), we clamp the
     * tradingXp field to prevent overflow beyond the level threshold.
     * This ensures XP can't accumulate beyond what's needed for the next level.
     *
     * @param offer the trade offer that was completed (unused)
     * @param ci callback info
     */
    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void thc$capXpAtLevelMax(MerchantOffer offer, CallbackInfo ci) {
        int currentLevel = this.getVillagerData().level();
        int maxXp = VillagerXpConfig.getMaxXpForLevel(currentLevel);

        if (maxXp > 0 && this.tradingXp > maxXp) {
            this.tradingXp = maxXp;
        }
    }
}
