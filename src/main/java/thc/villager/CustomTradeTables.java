package thc.villager;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import thc.enchant.EnchantmentEnforcement;
import thc.item.THCItems;

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
     * Returns the number of trades for a profession at a specific level.
     * Based on Phase 68 trade structure.
     *
     * @param profession the profession ResourceKey
     * @param level the villager level (1-5)
     * @return number of trades at that level, or 0 for unknown
     */
    public static int getTradeCount(ResourceKey<VillagerProfession> profession, int level) {
        if (profession == null) {
            return 0;
        }
        // LIBRARIAN: levels 1-4 = 2 trades, level 5 = 1 trade
        if (profession.equals(VillagerProfession.LIBRARIAN)) {
            return switch (level) {
                case 1, 2, 3, 4 -> 2;
                case 5 -> 1;
                default -> 0;
            };
        }
        // BUTCHER: levels 1-3 = 2 trades, levels 4-5 = 1 trade
        if (profession.equals(VillagerProfession.BUTCHER)) {
            return switch (level) {
                case 1, 2, 3 -> 2;
                case 4, 5 -> 1;
                default -> 0;
            };
        }
        // MASON: level 1 = 4 trades, levels 2-3 = 2 trades, levels 4-5 = 1 trade
        if (profession.equals(VillagerProfession.MASON)) {
            return switch (level) {
                case 1 -> 4;
                case 2, 3 -> 2;
                case 4, 5 -> 1;
                default -> 0;
            };
        }
        // CARTOGRAPHER: levels 1-2 = 3 trades, level 3 = 2 trades, levels 4-5 = 1 trade
        if (profession.equals(VillagerProfession.CARTOGRAPHER)) {
            return switch (level) {
                case 1, 2 -> 3;
                case 3 -> 2;
                case 4, 5 -> 1;
                default -> 0;
            };
        }
        return 0;
    }

    /**
     * Returns the trade pool size (number of distinct options) for cycling.
     * Pool > 1 means cycling produces different results (50/50 variants exist).
     * Pool = 1 means deterministic (cycling should be blocked).
     *
     * @param profession the profession ResourceKey
     * @param level the villager level (1-5)
     * @return pool size (2 for variants, 1 for deterministic)
     */
    public static int getTradePoolSize(ResourceKey<VillagerProfession> profession, int level) {
        if (profession == null) {
            return 0;
        }
        // LIBRARIAN: all levels have 50/50 variants
        if (profession.equals(VillagerProfession.LIBRARIAN)) {
            return 2;
        }
        // MASON: level 1 deterministic, levels 2-5 have 50/50 variants
        if (profession.equals(VillagerProfession.MASON)) {
            return level >= 2 ? 2 : 1;
        }
        // BUTCHER and CARTOGRAPHER: fully deterministic
        return 1;
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

        if (profession.equals(VillagerProfession.LIBRARIAN)) {
            return getLibrarianTrades(level, serverLevel, random);
        }
        if (profession.equals(VillagerProfession.BUTCHER)) {
            return getButcherTrades(level);
        }
        if (profession.equals(VillagerProfession.MASON)) {
            return getMasonTrades(level, random);
        }
        if (profession.equals(VillagerProfession.CARTOGRAPHER)) {
            return getCartographerTrades(level);
        }

        // All 4 professions implemented
        return List.of();
    }

    // =====================================================================
    // Librarian trades (TLIB-01 through TLIB-09)
    // =====================================================================

    /**
     * Get librarian trades for a specific level.
     * All slots have 50/50 variants per REQUIREMENTS.md TLIB-01 through TLIB-09.
     *
     * <p>Enchanted book trades use createEnchantedBookTrade() which looks up
     * internal levels from EnchantmentEnforcement.INTERNAL_LEVELS.
     */
    private static List<MerchantOffer> getLibrarianTrades(int level, ServerLevel serverLevel, RandomSource random) {
        return switch (level) {
            case 1 -> List.of(
                // TLIB-01: 24 paper -> 1e OR 1e -> 8 lanterns (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.PAPER, 24, Items.EMERALD, 1),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.LANTERN, 8)
                ),
                // TLIB-02: 5e + book -> mending OR 5e + book -> unbreaking (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(5, "mending", serverLevel),
                    () -> createEnchantedBookTrade(5, "unbreaking", serverLevel)
                )
            );
            case 2 -> List.of(
                // TLIB-03: 10e + book -> efficiency OR 10e + book -> fortune (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(10, "efficiency", serverLevel),
                    () -> createEnchantedBookTrade(10, "fortune", serverLevel)
                ),
                // TLIB-04: 10e + book -> silk touch OR 4 books -> 1e (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(10, "silk_touch", serverLevel),
                    () -> createSimpleTrade(Items.BOOK, 4, Items.EMERALD, 1)
                )
            );
            case 3 -> List.of(
                // TLIB-05: 15e + book -> protection OR 15e + book -> projectile_protection (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(15, "protection", serverLevel),
                    () -> createEnchantedBookTrade(15, "projectile_protection", serverLevel)
                ),
                // TLIB-06: 15e + book -> looting OR 9e -> 3 bookshelves (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(15, "looting", serverLevel),
                    () -> createSimpleTrade(Items.EMERALD, 9, Items.BOOKSHELF, 3)
                )
            );
            case 4 -> List.of(
                // TLIB-07: 20e + book -> sharpness OR 20e + book -> power (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(20, "sharpness", serverLevel),
                    () -> createEnchantedBookTrade(20, "power", serverLevel)
                ),
                // TLIB-08: 20e + book -> blast_protection OR 20e + book -> feather_falling (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(20, "blast_protection", serverLevel),
                    () -> createEnchantedBookTrade(20, "feather_falling", serverLevel)
                )
            );
            case 5 -> List.of(
                // TLIB-09: 30e + book -> breach OR 30e + book -> piercing (50/50)
                getVariantTrade(random,
                    () -> createEnchantedBookTrade(30, "breach", serverLevel),
                    () -> createEnchantedBookTrade(30, "piercing", serverLevel)
                )
            );
            default -> List.of();
        };
    }

    // =====================================================================
    // Butcher trades (TBUT-01 through TBUT-08)
    // =====================================================================

    /**
     * Get butcher trades for a specific level.
     * All butcher trades are deterministic (no 50/50 variants).
     * Per REQUIREMENTS.md TBUT-01 through TBUT-08.
     */
    private static List<MerchantOffer> getButcherTrades(int level) {
        return switch (level) {
            case 1 -> List.of(
                // TBUT-01: 4 raw chicken -> 1e
                createSimpleTrade(Items.CHICKEN, 4, Items.EMERALD, 1),
                // TBUT-02: 5 raw porkchop -> 1e
                createSimpleTrade(Items.PORKCHOP, 5, Items.EMERALD, 1)
            );
            case 2 -> List.of(
                // TBUT-03: 5 raw beef -> 1e
                createSimpleTrade(Items.BEEF, 5, Items.EMERALD, 1),
                // TBUT-04: 3 raw mutton -> 1e
                createSimpleTrade(Items.MUTTON, 3, Items.EMERALD, 1)
            );
            case 3 -> List.of(
                // TBUT-05: 1e -> 6 cooked porkchop
                createSimpleTrade(Items.EMERALD, 1, Items.COOKED_PORKCHOP, 6),
                // TBUT-06: 1e -> 5 steak
                createSimpleTrade(Items.EMERALD, 1, Items.COOKED_BEEF, 5)
            );
            case 4 -> List.of(
                // TBUT-07: 10 dried kelp blocks -> 1e
                createSimpleTrade(Items.DRIED_KELP_BLOCK, 10, Items.EMERALD, 1)
            );
            case 5 -> List.of(
                // TBUT-08: 10 sweet berries -> 1e
                createSimpleTrade(Items.SWEET_BERRIES, 10, Items.EMERALD, 1)
            );
            default -> List.of();
        };
    }

    // =====================================================================
    // Mason trades (TMAS-01 through TMAS-10)
    // =====================================================================

    /**
     * Get mason trades for a specific level.
     * Level 1 is deterministic, levels 2-5 have 50/50 variants.
     * Per REQUIREMENTS.md TMAS-01 through TMAS-10.
     */
    private static List<MerchantOffer> getMasonTrades(int level, RandomSource random) {
        return switch (level) {
            case 1 -> List.of(
                // TMAS-01: 1e -> 64 cobblestone (deterministic)
                createSimpleTrade(Items.EMERALD, 1, Items.COBBLESTONE, 64),
                // TMAS-02: 1e -> 64 stone bricks (deterministic)
                createSimpleTrade(Items.EMERALD, 1, Items.STONE_BRICKS, 64),
                // TMAS-03: 1e -> 64 bricks (deterministic)
                createSimpleTrade(Items.EMERALD, 1, Items.BRICKS, 64),
                // TMAS-04: 1e -> 64 polished andesite (deterministic)
                createSimpleTrade(Items.EMERALD, 1, Items.POLISHED_ANDESITE, 64)
            );
            case 2 -> List.of(
                // TMAS-05: 1e -> 64 polished granite OR 1e -> 64 polished diorite (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.POLISHED_GRANITE, 64),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.POLISHED_DIORITE, 64)
                ),
                // TMAS-06: 1e -> 64 smooth stone OR 1e -> 64 calcite (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.SMOOTH_STONE, 64),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.CALCITE, 64)
                )
            );
            case 3 -> List.of(
                // TMAS-07: 1e -> 64 tuff OR 1e -> 64 mud bricks (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.TUFF, 64),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.MUD_BRICKS, 64)
                ),
                // TMAS-08: 1e -> 32 deepslate bricks OR 1e -> 32 deepslate tiles (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.DEEPSLATE_BRICKS, 32),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.DEEPSLATE_TILES, 32)
                )
            );
            case 4 -> List.of(
                // TMAS-09: 1e -> 32 polished blackstone OR 1e -> 32 polished blackstone bricks (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.POLISHED_BLACKSTONE, 32),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.POLISHED_BLACKSTONE_BRICKS, 32)
                )
            );
            case 5 -> List.of(
                // TMAS-10: 1e -> 16 copper block OR 1e -> 16 quartz block (50/50)
                getVariantTrade(random,
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.COPPER_BLOCK, 16),
                    () -> createSimpleTrade(Items.EMERALD, 1, Items.QUARTZ_BLOCK, 16)
                )
            );
            default -> List.of();
        };
    }

    // =====================================================================
    // Cartographer trades (TCRT-01 through TCRT-10)
    // =====================================================================

    /**
     * Get cartographer trades for a specific level.
     * All cartographer trades are deterministic.
     * Structure locators from Phase 66 are sold at higher levels.
     * Per REQUIREMENTS.md TCRT-01 through TCRT-10.
     */
    private static List<MerchantOffer> getCartographerTrades(int level) {
        return switch (level) {
            case 1 -> List.of(
                // TCRT-01: 24 paper -> 1e (deterministic)
                createSimpleTrade(Items.PAPER, 24, Items.EMERALD, 1),
                // TCRT-02: 5e -> empty map (deterministic)
                createSimpleTrade(Items.EMERALD, 5, Items.MAP, 1),
                // TCRT-03: 10e -> trial chamber locator (deterministic)
                createLocatorTrade(10, THCItems.TRIAL_CHAMBER_LOCATOR)
            );
            case 2 -> List.of(
                // TCRT-04: 15e -> pillager outpost locator (deterministic)
                createLocatorTrade(15, THCItems.PILLAGER_OUTPOST_LOCATOR),
                // TCRT-05: 1e -> 8 glass panes (deterministic)
                createSimpleTrade(Items.EMERALD, 1, Items.GLASS_PANE, 8),
                // TCRT-06: 3e -> spyglass (deterministic)
                createSimpleTrade(Items.EMERALD, 3, Items.SPYGLASS, 1)
            );
            case 3 -> List.of(
                // TCRT-07: 20e -> nether fortress locator (deterministic)
                createLocatorTrade(20, THCItems.FORTRESS_LOCATOR),
                // TCRT-08: 20e -> bastion locator (deterministic)
                createLocatorTrade(20, THCItems.BASTION_LOCATOR)
            );
            case 4 -> List.of(
                // TCRT-09: 25e -> ancient city locator (deterministic)
                createLocatorTrade(25, THCItems.ANCIENT_CITY_LOCATOR)
            );
            case 5 -> List.of(
                // TCRT-10: 30e -> stronghold locator (deterministic)
                createLocatorTrade(30, THCItems.STRONGHOLD_LOCATOR)
            );
            default -> List.of();
        };
    }

    // =====================================================================
    // Factory methods for trade creation
    // =====================================================================

    /**
     * Creates a structure locator trade for cartographer.
     *
     * @param emeraldCost Emerald cost for the trade
     * @param locator Structure locator item from THCItems
     * @return MerchantOffer for the locator trade
     */
    public static MerchantOffer createLocatorTrade(int emeraldCost, Item locator) {
        return new MerchantOffer(
            new ItemCost(Items.EMERALD, emeraldCost),
            Optional.empty(),
            new ItemStack(locator, 1),
            0,                   // uses (starts at 0)
            Integer.MAX_VALUE,   // maxUses (unlimited)
            0,                   // xp (no XP gain)
            0.05f                // priceMultiplier (standard)
        );
    }

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

    /**
     * Creates an enchanted book trade for librarian.
     * Uses THC's internal enchantment levels from EnchantmentEnforcement.
     *
     * <p>The enchantment level is looked up from EnchantmentEnforcement.INTERNAL_LEVELS,
     * defaulting to level 1 if not specified.
     *
     * @param emeraldCost Emerald cost for the trade
     * @param enchantmentId Enchantment ID without namespace (e.g., "mending")
     * @param serverLevel Server level for registry access
     * @return MerchantOffer for the enchanted book trade
     */
    public static MerchantOffer createEnchantedBookTrade(
            int emeraldCost,
            String enchantmentId,
            ServerLevel serverLevel) {

        // Create enchanted book with specific enchantment
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

        // Get enchantment holder from registry
        Registry<Enchantment> registry = serverLevel.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT);
        var enchantKey = ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.withDefaultNamespace(enchantmentId));
        var enchantHolder = registry.get(enchantKey);

        if (enchantHolder.isPresent()) {
            // Get internal level from EnchantmentEnforcement
            String fullId = "minecraft:" + enchantmentId;
            int enchantLevel = EnchantmentEnforcement.INSTANCE.getINTERNAL_LEVELS()
                .getOrDefault(fullId, 1);

            // Build enchantments
            ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            builder.set(enchantHolder.get(), enchantLevel);
            book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
        }

        return new MerchantOffer(
            new ItemCost(Items.EMERALD, emeraldCost),
            Optional.of(new ItemCost(Items.BOOK, 1)),
            book,
            0,                   // uses (starts at 0)
            Integer.MAX_VALUE,   // maxUses (unlimited)
            0,                   // xp (no XP gain, manual leveling in Phase 69)
            0.05f                // priceMultiplier (standard)
        );
    }
}
