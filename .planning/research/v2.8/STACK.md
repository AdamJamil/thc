# Technology Stack: Villager Overhaul

**Project:** THC v2.8 Villager Overhaul
**Researched:** 2026-01-30
**MC Version:** 1.21.11
**Confidence:** HIGH (verified against decompiled MC 1.21.11 sources)

## Executive Summary

The villager system in MC 1.21.11 uses a Brain-based AI system for job acquisition and a static trade table system (`VillagerTrades.TRADES`) for offer generation. THC already has foundational mixins for villager manipulation (`VillagerMixin`, `AbstractVillagerMixin`, `BrainPoiMemoryMixin`). The villager overhaul requires targeted mixins at specific interception points, with most complexity in trade table replacement and manual leveling.

---

## Core APIs and Classes

### VillagerData (Record)

**Package:** `net.minecraft.world.entity.npc.villager`

| Field | Type | Purpose |
|-------|------|---------|
| `type` | `Holder<VillagerType>` | Biome variant (plains, desert, etc.) |
| `profession` | `Holder<VillagerProfession>` | Job type (mason, librarian, etc.) |
| `level` | `int` | Trade tier (1-5) |

**Key Methods:**
```java
VillagerData withProfession(Holder<VillagerProfession>) // Create copy with new profession
VillagerData withLevel(int)                              // Create copy with new level
static int getMinXpPerLevel(int level)                   // XP threshold for level
static boolean canLevelUp(int level)                     // True if level < 5
```

**XP Thresholds (verified from bytecode):**
| Level | Min XP | Max XP |
|-------|--------|--------|
| 1 | 0 | 10 |
| 2 | 10 | 70 |
| 3 | 70 | 150 |
| 4 | 150 | 250 |
| 5 | 250 | - |

**Usage:**
```java
Villager villager = ...;
VillagerData data = villager.getVillagerData();
villager.setVillagerData(data.withProfession(newProfession));
```

### VillagerProfession (Record)

**Package:** `net.minecraft.world.entity.npc.villager`

**Static ResourceKeys (allowed professions for THC):**
```java
VillagerProfession.MASON        // Stonecutter
VillagerProfession.LIBRARIAN    // Lectern
VillagerProfession.BUTCHER      // Smoker
VillagerProfession.CARTOGRAPHER // Cartography table
VillagerProfession.NONE         // Unemployed
VillagerProfession.NITWIT       // Cannot work
```

**Key Fields:**
```java
Predicate<Holder<PoiType>> acquirableJobSite  // POI types that grant this profession
Predicate<Holder<PoiType>> heldJobSite        // POI types that maintain this profession
```

### VillagerTrades

**Package:** `net.minecraft.world.entity.npc.villager`

**Trade Table Structure:**
```java
// Static map: Profession -> Level -> ItemListing[]
Map<ResourceKey<VillagerProfession>, Int2ObjectMap<ItemListing[]>> TRADES

// ItemListing interface
interface ItemListing {
    MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random);
}
```

**Built-in ItemListing Implementations:**
| Class | Purpose |
|-------|---------|
| `EmeraldForItems` | Buy items for emeralds |
| `ItemsForEmeralds` | Sell items for emeralds |
| `ItemsAndEmeraldsToItems` | Trade items + emeralds for items |
| `EnchantBookForEmeralds` | Random enchanted book trades |
| `TreasureMapForEmeralds` | Explorer maps |
| `TippedArrowForItemsAndEmeralds` | Tipped arrows |

### MerchantOffer

**Package:** `net.minecraft.world.item.trading`

**Constructor (most flexible):**
```java
MerchantOffer(
    ItemCost baseCostA,           // Primary input
    Optional<ItemCost> costB,     // Secondary input (optional)
    ItemStack result,             // Output
    int uses,                     // Current uses
    int maxUses,                  // Max uses before restock
    int xp,                       // XP granted to villager
    float priceMultiplier         // Demand/reputation multiplier
)
```

**Key Methods:**
```java
void resetUses()          // Reset use count (for trade cycling)
void setToOutOfStock()    // Mark as depleted
boolean isOutOfStock()    // Check if depleted
ItemStack getResult()     // Get output item
```

### MerchantOffers

**Package:** `net.minecraft.world.item.trading`

Extends `ArrayList<MerchantOffer>`. The offers list for a villager.

**Key Pattern (already used in THC):**
```java
@Mixin(AbstractVillager.class)
private void onGetOffers(CallbackInfoReturnable<MerchantOffers> cir) {
    MerchantOffers offers = cir.getReturnValue();
    offers.removeIf(offer -> offer.getResult().is(Items.SHIELD));
}
```

---

## Mixin Targets

### 1. Profession Restriction

**Target:** `AssignProfessionFromJobSite.create()`

**Location:** `net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite`

**Method:** Static `create()` returns a `BehaviorControl<Villager>`

**Approach:** Mixin the behavior that assigns profession from job site to check if profession is allowed.

**Alternative (Recommended):** Intercept `Villager.setVillagerData()` - single point for all profession changes.

```java
@Mixin(Villager.class)
public class VillagerProfessionMixin {
    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void thc$restrictProfession(VillagerData data, CallbackInfo ci) {
        ResourceKey<VillagerProfession> profession = data.profession().unwrapKey().orElse(null);
        if (profession != null && !ALLOWED_PROFESSIONS.contains(profession)) {
            // Reset to NONE instead of disallowed profession
            VillagerData current = ((Villager)(Object)this).getVillagerData();
            ((Villager)(Object)this).setVillagerData(current.withProfession(
                /* NONE holder */
            ));
            ci.cancel();
        }
    }
}
```

### 2. POI Blocking for Disallowed Professions

**Target:** `AcquirePoi.create()`

**Location:** `net.minecraft.world.entity.ai.behavior.AcquirePoi`

**Existing THC Pattern:** `BrainPoiMemoryMixin` already blocks POI memories for JOB_SITE and POTENTIAL_JOB_SITE in claimed chunks.

**Extension:** Add profession-based filtering in addition to claim-based filtering.

```java
// In BrainPoiMemoryMixin, extend the check:
if (type == MemoryModuleType.JOB_SITE || type == MemoryModuleType.POTENTIAL_JOB_SITE) {
    // Existing claim check
    if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
        ci.cancel();
        return;
    }

    // New: Check if POI type maps to allowed profession
    if (!isAllowedProfessionPoi(globalPos, server)) {
        ci.cancel();
    }
}
```

### 3. Leveling Interception

**Target:** `Villager.increaseMerchantCareer(ServerLevel)`

**Why:** Called when villager has enough XP to level up. This is where we gate on stage and emerald cost.

```java
@Mixin(Villager.class)
public class VillagerLevelingMixin {
    @Inject(method = "increaseMerchantCareer", at = @At("HEAD"), cancellable = true)
    private void thc$gatedLeveling(ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager)(Object)this;
        int currentLevel = self.getVillagerData().level();
        int requiredStage = getRequiredStageForLevel(currentLevel + 1);

        if (StageManager.getCurrentStage(level.getServer()) < requiredStage) {
            ci.cancel(); // Block level-up until stage unlocked
        }
    }
}
```

**Alternative Target:** `Villager.shouldIncreaseLevel()` - returns boolean, can be redirected to always return false unless conditions met.

### 4. Trade Cycling (Reroll Current Rank)

**Target:** Custom interaction handler (not vanilla)

**Implementation:** Add a new interaction when player shift-right-clicks with emerald in hand.

**Pattern:** Use `UseEntityCallback` from Fabric API or inject into `Villager.mobInteract()`.

```java
@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
private void thc$handleTradeCycle(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    if (player.isShiftKeyDown() && player.getItemInHand(hand).is(Items.EMERALD)) {
        // Cycle trades for current level only
        rerollTradesForCurrentLevel();
        player.getItemInHand(hand).shrink(1);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
```

### 5. Trade Table Replacement

**Target:** `Villager.updateTrades(ServerLevel)`

**Why:** Called when villager gains a level to populate new trades. We replace with deterministic custom trades.

```java
@Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
private void thc$customTrades(ServerLevel level, CallbackInfo ci) {
    Villager self = (Villager)(Object)this;
    VillagerData data = self.getVillagerData();
    ResourceKey<VillagerProfession> profession = data.profession().unwrapKey().orElse(null);
    int villagerLevel = data.level();

    // Get custom trades for this profession and level
    List<MerchantOffer> customOffers = THCTrades.getTradesFor(profession, villagerLevel);

    MerchantOffers offers = self.getOffers();
    offers.addAll(customOffers);

    ci.cancel(); // Skip vanilla trade generation
}
```

---

## Integration Points with Existing THC Systems

### StageManager Integration

```java
// Check stage for leveling gates
int stage = StageManager.getCurrentStage(server);
int requiredStage = switch(targetLevel) {
    case 2 -> 1;  // Apprentice requires Stage 1
    case 3 -> 2;  // Journeyman requires Stage 2
    case 4 -> 3;  // Expert requires Stage 3
    case 5 -> 4;  // Master requires Stage 4
    default -> 0; // Novice always available
};
```

### POI Blocking Synergy

Existing `BrainPoiMemoryMixin` and `ServerLevelPoiMixin` handle POI blocking in claimed chunks. Profession restriction can share this pattern by:
1. Reusing the memory blocking approach
2. Adding a parallel check for profession allowlist

### Trade Filtering

Existing `AbstractVillagerMixin.getOffers()` removes shield/bell/saddle trades. Custom trade tables should simply not include disallowed items, making this mixin unnecessary for custom professions.

---

## Custom Trade Table Design

### Recommended Structure

```java
public class THCTrades {
    // Profession -> Level -> List of MerchantOffer
    private static final Map<ResourceKey<VillagerProfession>, Int2ObjectMap<List<MerchantOffer>>> TRADES;

    public static List<MerchantOffer> getTradesFor(
            ResourceKey<VillagerProfession> profession,
            int level) {
        Int2ObjectMap<List<MerchantOffer>> levelMap = TRADES.get(profession);
        if (levelMap == null) return List.of();
        return levelMap.getOrDefault(level, List.of());
    }
}
```

### Trade Creation Helper

```java
public static MerchantOffer createTrade(
        Item costItem, int costCount,
        Item resultItem, int resultCount,
        int maxUses, int xp) {
    return new MerchantOffer(
        new ItemCost(costItem, costCount),
        Optional.empty(),
        new ItemStack(resultItem, resultCount),
        0, maxUses, xp, 0.05f
    );
}
```

---

## Required Imports Summary

```java
// Villager system
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

// Trading
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;

// POI system
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

// Brain/AI
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;

// Registry
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
```

---

## Recommended New Files

| File | Purpose |
|------|---------|
| `VillagerProfessionMixin.java` | Restrict profession changes to allowed types |
| `VillagerLevelingMixin.java` | Gate leveling on stage + emerald cost |
| `VillagerTradesMixin.java` | Replace vanilla trades with deterministic custom trades |
| `THCTrades.java` | Custom trade table definitions |
| `VillagerConfig.java` | Configuration for allowed professions, stage gates, emerald costs |

---

## Verification Checklist

- [x] VillagerData structure verified (type, profession, level record)
- [x] XP thresholds extracted from bytecode: [0, 10, 70, 150, 250]
- [x] VillagerProfession ResourceKeys confirmed (MASON, LIBRARIAN, BUTCHER, CARTOGRAPHER)
- [x] MerchantOffer constructor parameters documented
- [x] updateTrades() is the correct interception point for trade table replacement
- [x] increaseMerchantCareer() is the correct point for level gating
- [x] setVillagerData() is single point for profession changes
- [x] Existing THC mixins compatible (VillagerMixin, AbstractVillagerMixin, BrainPoiMemoryMixin)
- [x] StageManager API verified for stage checks

---

## Confidence Assessment

| Component | Confidence | Verification |
|-----------|------------|--------------|
| VillagerData API | HIGH | Decompiled from MC 1.21.11 jar |
| XP Thresholds | HIGH | Extracted from bytecode static initializer |
| Trade Structure | HIGH | VillagerTrades.TRADES map verified |
| Mixin Targets | HIGH | Method signatures verified in decompiled classes |
| POI Integration | HIGH | Existing BrainPoiMemoryMixin pattern confirmed |
| Stage Integration | HIGH | StageManager.java reviewed |

---

## Sources

- MC 1.21.11 decompiled classes (gradle cache: `minecraft-common-1.21.11-loom.mappings...jar`)
- Existing THC mixin implementations (verified working patterns)
- Fabric Loom official Mojang mappings
