package thc.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.villager.CustomTradeTables;

import java.util.List;

/**
 * Intercept trade generation to replace vanilla random pools with
 * THC deterministic trade tables for allowed professions.
 *
 * <p>This mixin hooks into {@link Villager#updateTrades()} which is called
 * when a villager gains a new level. For custom professions (librarian,
 * butcher, mason, cartographer), we add our curated trades and skip
 * vanilla generation.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Only affects the 4 allowed trade professions</li>
 *   <li>Existing villagers are grandfathered (this only fires on level-up)</li>
 *   <li>50/50 variant selection uses world random for proper distribution</li>
 *   <li>Cancels vanilla trade generation for custom professions</li>
 * </ul>
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

    @Shadow
    public abstract VillagerData getVillagerData();

    @Shadow
    public abstract MerchantOffers getOffers();

    /**
     * Intercept trade generation to replace vanilla random pools with
     * THC deterministic trade tables for allowed professions.
     *
     * <p>This method is called when a villager gains a new level.
     * For custom professions (librarian, butcher, mason, cartographer),
     * we add our curated trades and skip vanilla generation.
     */
    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void thc$customTrades(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData data = self.getVillagerData();

        ResourceKey<VillagerProfession> profKey = data.profession().unwrapKey().orElse(null);
        if (profKey == null) {
            return; // NONE profession, let vanilla handle
        }

        int villagerLevel = data.level();

        // Check if this profession has custom trades
        if (!CustomTradeTables.hasCustomTrades(profKey)) {
            return; // Let vanilla handle non-overridden professions
        }

        // Get server level for registry access (needed for enchanted books)
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return; // Client side, skip
        }

        // Get custom trades for this level
        List<MerchantOffer> customOffers = CustomTradeTables.getTradesFor(
            profKey,
            villagerLevel,
            serverLevel,
            serverLevel.getRandom()
        );

        // Add custom trades to the villager's offers
        MerchantOffers offers = self.getOffers();
        offers.addAll(customOffers);

        ci.cancel(); // Skip vanilla trade generation
    }
}
