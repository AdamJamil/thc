package thc.villager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Custom deterministic trade tables for the 4 allowed professions.
 *
 * <p>Replaces vanilla random trade pools with curated, specification-defined
 * trades. All 37 trade slots are fully specified with exact items, quantities,
 * emerald costs, and 50/50 variant options.
 *
 * <p>Key design decisions (per CONTEXT.md):
 * <ul>
 *   <li>Unlimited trades - maxUses = Integer.MAX_VALUE</li>
 *   <li>No XP gain from trading - xp = 0 (manual leveling in Phase 69)</li>
 *   <li>50/50 variants reroll on each trade cycle (Phase 70)</li>
 *   <li>Existing villagers are grandfathered (trades unchanged)</li>
 * </ul>
 *
 * <p>Professions with custom trades:
 * <ul>
 *   <li>LIBRARIAN - 9 slots (enchanted books with 50/50 variants)</li>
 *   <li>BUTCHER - 8 slots (raw meat -> emeralds, emeralds -> cooked food)</li>
 *   <li>MASON - 10 slots (bulk building blocks, 64-stacks)</li>
 *   <li>CARTOGRAPHER - 10 slots (paper, maps, structure locators)</li>
 * </ul>
 */
public final class CustomTradeTables {

    private CustomTradeTables() {
        // Prevent instantiation
    }

    /**
     * Check if a profession has custom trade tables defined.
     *
     * @param profession the profession ResourceKey to check
     * @return true for LIBRARIAN, BUTCHER, MASON, CARTOGRAPHER
     */
    public static boolean hasCustomTrades(ResourceKey<VillagerProfession> profession) {
        if (profession == null) {
            return false;
        }
        // Compare location paths for the 4 allowed trade professions
        return profession.equals(VillagerProfession.LIBRARIAN) ||
               profession.equals(VillagerProfession.BUTCHER) ||
               profession.equals(VillagerProfession.MASON) ||
               profession.equals(VillagerProfession.CARTOGRAPHER);
    }

    /**
     * Get custom trades for a profession at a specific level.
     *
     * <p>Dispatcher method that calls profession-specific trade getters.
     * Returns empty list for levels without implementation (placeholder).
     *
     * @param profession the profession ResourceKey
     * @param level the villager level (1-5)
     * @param serverLevel the server level (for registry access)
     * @param random random source for 50/50 variant selection
     * @return list of MerchantOffers for this level, or empty list
     */
    public static List<MerchantOffer> getTradesFor(
            ResourceKey<VillagerProfession> profession,
            int level,
            ServerLevel serverLevel,
            RandomSource random) {

        // Placeholder - trade content will be added in subsequent plans
        // (68-02 through 68-05)
        return List.of();
    }

    // =====================================================================
    // Factory methods for trade creation
    // =====================================================================

    /**
     * Create a simple one-input trade.
     *
     * <p>Parameters per CONTEXT.md decisions:
     * <ul>
     *   <li>uses = 0 (starts unused)</li>
     *   <li>maxUses = Integer.MAX_VALUE (unlimited trades)</li>
     *   <li>xp = 0 (no XP gain, manual leveling in Phase 69)</li>
     *   <li>priceMultiplier = 0.05f (standard)</li>
     * </ul>
     *
     * @param costItem the input item
     * @param costCount the input item quantity
     * @param resultItem the output item
     * @param resultCount the output item quantity
     * @return a new MerchantOffer
     */
    public static MerchantOffer createSimpleTrade(
            Item costItem, int costCount,
            Item resultItem, int resultCount) {
        return new MerchantOffer(
            new ItemCost(costItem, costCount),
            Optional.empty(),                          // No secondary cost
            new ItemStack(resultItem, resultCount),
            0,                                         // uses (starts at 0)
            Integer.MAX_VALUE,                         // maxUses (unlimited)
            0,                                         // xp (no XP gain)
            0.05f                                      // priceMultiplier (standard)
        );
    }

    /**
     * Create a two-input trade (e.g., emeralds + book -> enchanted book).
     *
     * @param cost1Item the primary input item
     * @param cost1Count the primary input quantity
     * @param cost2Item the secondary input item
     * @param cost2Count the secondary input quantity
     * @param resultItem the output item
     * @param resultCount the output item quantity
     * @return a new MerchantOffer
     */
    public static MerchantOffer createTwoInputTrade(
            Item cost1Item, int cost1Count,
            Item cost2Item, int cost2Count,
            Item resultItem, int resultCount) {
        return new MerchantOffer(
            new ItemCost(cost1Item, cost1Count),
            Optional.of(new ItemCost(cost2Item, cost2Count)),
            new ItemStack(resultItem, resultCount),
            0,                                         // uses (starts at 0)
            Integer.MAX_VALUE,                         // maxUses (unlimited)
            0,                                         // xp (no XP gain)
            0.05f                                      // priceMultiplier (standard)
        );
    }

    /**
     * Select between two trade variants with 50/50 probability.
     *
     * <p>Used for trade slots that have two options (e.g., mending vs unbreaking,
     * polished granite vs polished diorite). The coin flip happens at trade
     * generation time, and rerolls on each trade cycle (Phase 70).
     *
     * @param random the random source
     * @param optionA supplier for first trade variant
     * @param optionB supplier for second trade variant
     * @return one of the two trade options based on random selection
     */
    public static MerchantOffer getVariantTrade(
            RandomSource random,
            Supplier<MerchantOffer> optionA,
            Supplier<MerchantOffer> optionB) {
        return random.nextBoolean() ? optionA.get() : optionB.get();
    }
}
