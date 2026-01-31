# Architecture Patterns: Villager Overhaul

**Domain:** Minecraft villager system modification
**Researched:** 2026-01-30
**Confidence:** HIGH

## System Integration Overview

The villager overhaul integrates with THC's existing architecture at four key points:

```
+------------------+     +-------------------+     +------------------+
|  StageManager    |     |   ClaimManager    |     |  ServerHolder    |
|  (Stage gates)   |     |   (Chunk checks)  |     |  (Server access) |
+--------+---------+     +---------+---------+     +--------+---------+
         |                         |                        |
         v                         v                        v
+--------+---------+     +---------+---------+     +--------+---------+
| VillagerLeveling |<--->| VillagerProfession|<--->|  TradeOverrides  |
| (UseEntityCB)    |     | Restriction (Mixin)|    |  (Mixin/Fabric)  |
+------------------+     +-------------------+     +------------------+
         |                         |                        |
         v                         v                        v
+------------------+     +-------------------+     +------------------+
|  UseEntityCallback    | Brain/POI Filtering|     | Custom ItemListing|
|  (emerald checks)     | (existing pattern) |     |  (trade tables)  |
+------------------+     +-------------------+     +------------------+
```

## Component Boundaries

| Component | Responsibility | Integrates With |
|-----------|---------------|-----------------|
| `VillagerProfessionRestriction` | Block profession changes to non-allowed types | Brain schedule mixin (existing), POI system |
| `VillagerLeveling` | Handle manual level-up via UseEntityCallback | StageManager, existing cow milking pattern |
| `VillagerTradeCycling` | Handle trade reroll via UseEntityCallback | Villager XP system, emerald economy |
| `VillagerTradeOverrides` | Replace vanilla ItemListings with custom tables | TradeOfferHelper (Fabric API), AbstractVillagerMixin |
| `JobBlockPOI` | Disable POI for non-allowed job blocks | ServerLevelPoiMixin (existing pattern) |

## Recommended Architecture

### Pattern 1: Profession Restriction via Mixin

**Where to intercept:** `Villager.setVillagerData()` or profession assignment in `VillagerGoalPackages`

**Why mixin over callback:** Profession changes happen through internal villager AI, not player interaction. The villager acquires a profession by standing near a job site block. This is not triggered by UseEntityCallback.

**Implementation approach:**

```java
@Mixin(Villager.class)
public abstract class VillagerProfessionMixin {

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void thc$restrictProfession(VillagerData data, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData current = self.getVillagerData();

        // Allow NONE -> allowed profession
        // Block NONE -> disallowed profession
        // Block profession -> different profession (job hopping)

        Holder<VillagerProfession> newProf = data.profession();
        String profId = newProf.unwrapKey().map(k -> k.identifier().toString()).orElse(null);

        if (!isAllowedProfession(profId)) {
            ci.cancel();
        }
    }

    @Unique
    private static final Set<String> ALLOWED = Set.of(
        "minecraft:mason",
        "minecraft:librarian",
        "minecraft:butcher",
        "minecraft:cartographer"
    );

    @Unique
    private boolean isAllowedProfession(String profId) {
        return profId == null  // NONE profession
            || profId.equals("minecraft:none")
            || ALLOWED.contains(profId);
    }
}
```

**Rationale:** Blocking at `setVillagerData` catches all profession changes - from AI job acquisition, from NBT loading, from commands. This is the single chokepoint.

### Pattern 2: Job Block POI Disabling

**Where to intercept:** Extend existing `ServerLevelPoiMixin.updatePOIOnBlockStateChange`

**Existing pattern (v2.6):**
```java
// ServerLevelPoiMixin.java
@Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"), cancellable = true)
private void thc$blockPoiInClaimedChunks(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
    if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
        ci.cancel();
    }
}
```

**Extension for job blocks:**
```java
// Same mixin, additional logic
private void thc$blockPoiInClaimedChunks(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
    // Existing: block all POI in claimed chunks
    if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
        ci.cancel();
        return;
    }

    // New: block non-allowed job site POI everywhere
    if (isDisallowedJobBlock(newState.getBlock())) {
        ci.cancel();
    }
}

@Unique
private static final Set<Block> DISALLOWED_JOB_BLOCKS = Set.of(
    Blocks.COMPOSTER,       // farmer
    Blocks.BREWING_STAND,   // cleric
    Blocks.SMITHING_TABLE,  // toolsmith
    Blocks.BLAST_FURNACE,   // armorer
    Blocks.FLETCHING_TABLE, // fletcher
    Blocks.CAULDRON,        // leatherworker
    Blocks.BARREL,          // fisherman
    Blocks.GRINDSTONE,      // weaponsmith
    Blocks.LOOM             // shepherd
);
```

**Rationale:** This reuses the exact pattern from v2.6 village deregistration. Same mixin, same injection point, just extended logic.

### Pattern 3: Manual Leveling via UseEntityCallback

**Where to intercept:** `UseEntityCallback.EVENT` (same as cow milking)

**Existing pattern (THC.kt):**
```kotlin
UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
    val stack = player.getItemInHand(hand)

    if (stack.item == THCItems.COPPER_BUCKET && entity is Cow && !entity.isBaby) {
        // Handle interaction
        return@register InteractionResult.SUCCESS
    }

    InteractionResult.PASS
}
```

**Extension for villager leveling:**
```kotlin
// New file: VillagerInteraction.kt
object VillagerInteraction {
    fun register() {
        UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
            if (entity !is Villager) return@register InteractionResult.PASS
            if (level.isClientSide) return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)
            if (!stack.`is`(Items.EMERALD)) return@register InteractionResult.PASS

            // Shift+emerald = level up
            // Non-shift+emerald = cycle trades
            if (player.isShiftKeyDown) {
                return@register handleLevelUp(player, entity, stack, level)
            } else {
                return@register handleTradeCycle(player, entity, stack, level)
            }
        }
    }
}
```

**Rationale:** UseEntityCallback is already used for cow milking. Same event, same pattern, just different entity type check.

### Pattern 4: Stage-Gated Leveling

**Where to intercept:** Within UseEntityCallback handler

**Integration with StageManager:**
```kotlin
private fun handleLevelUp(player: ServerPlayer, villager: Villager, stack: ItemStack, level: Level): InteractionResult {
    val currentLevel = villager.villagerData.level
    val currentStage = StageManager.getCurrentStage(player.server)

    // Level gates: level 2 needs stage 2, level 3 needs stage 3, etc.
    val requiredStage = currentLevel + 1
    if (currentStage < requiredStage) {
        player.displayClientMessage(
            Component.literal("Stage $requiredStage required for level ${currentLevel + 1}"),
            true
        )
        return InteractionResult.FAIL
    }

    // ... emerald cost check, level up logic
}
```

**Existing StageManager API:**
```java
public static int getCurrentStage(MinecraftServer server) {
    return StageData.getServerState(server).getCurrentStage();
}
```

**Rationale:** Direct reuse of StageManager pattern from PatrolSpawnerMixin. Same API, same gate structure.

### Pattern 5: Custom Trade Tables

**Where to intercept:** Two options

**Option A: TradeOfferHelper (Fabric API)**
```kotlin
// Register at mod init
TradeOfferHelper.registerVillagerOffers(
    VillagerProfession.MASON,
    1,  // level
    factories -> {
        factories.clear()  // Remove vanilla trades
        factories.add(CustomMasonTrades.LEVEL_1)
    }
)
```

**Option B: AbstractVillagerMixin.getOffers() (Existing)**
```java
// Already exists for removing shield trades
@Inject(method = "getOffers", at = @At("RETURN"))
private void thc$removeShieldTrades(CallbackInfoReturnable<MerchantOffers> cir) {
    MerchantOffers offers = cir.getReturnValue();
    offers.removeIf(offer -> offer.getResult().is(Items.SHIELD));
}
```

**Recommendation:** Use TradeOfferHelper for adding custom trades (cleaner API), keep AbstractVillagerMixin for removal/filtering (already exists).

**Trade table structure:**
```kotlin
object VillagerTrades {
    // Organized by profession and level
    val MASON: Map<Int, List<ItemListing>> = mapOf(
        1 to listOf(
            ItemsForEmeralds(Items.STONE, 1, 16, 12, 2),
            // ...
        ),
        2 to listOf(/* stage 2+ trades */),
        // ...
    )
}
```

### Pattern 6: Trade Cycling

**Where to intercept:** UseEntityCallback (same handler as leveling)

**Data flow:**
```
Player right-clicks villager with emerald (no shift)
    |
    v
Check: villager has enough XP to reroll?
    |
    +-- No XP -> show "Not enough experience" message
    |
    v
Consume emerald, reset current-level trades
    |
    v
Call villager.updateTrades() or manually populate from trade table
```

**Villager XP access:**
```java
// Villager has public tradingXp field (int)
int xp = villager.tradingXp;

// XP thresholds per level (vanilla constants)
// Level 2: 10 XP, Level 3: 70 XP, Level 4: 150 XP, Level 5: 250 XP
```

## Data Flow Diagrams

### Leveling Flow

```
Player shift+right-click villager with emerald
           |
           v
    +------+------+
    | Is villager?|
    +------+------+
           | Yes
           v
    +------+------+
    | Has emerald?|
    +------+------+
           | Yes
           v
    +------+------+
    | Shift held? |
    +------+------+
           | Yes
           v
    +------+------+
    | Check stage |<-- StageManager.getCurrentStage()
    +------+------+
           | Pass
           v
    +------+------+
    | Consume     |
    | emerald(s)  |
    +------+------+
           |
           v
    +------+------+
    | Increment   |<-- villager.setVillagerData(data.withLevel(level+1))
    | level       |
    +------+------+
           |
           v
    +------+------+
    | Apply new   |<-- VillagerTrades.PROFESSION[level+1]
    | trade tier  |
    +------+------+
```

### Profession Restriction Flow

```
Villager AI detects nearby job block
           |
           v
    +------+------+
    | POI lookup  |<-- already blocked for non-allowed blocks
    +------+------+
           | If allowed block found
           v
    +------+------+
    | Call        |
    | setVillagerData
    +------+------+
           |
           v
    +------+------+
    | Mixin check |<-- isAllowedProfession()
    +------+------+
           |
    +------+------+
    | Cancel if   |
    | not allowed |
    +------+------+
```

## Existing THC Patterns to Reuse

| Pattern | Used In | Reuse For |
|---------|---------|-----------|
| UseEntityCallback + item check | Cow milking (THC.kt:78-99) | Villager leveling/cycling |
| HEAD cancellation mixin | PatrolSpawnerMixin, BrainPoiMemoryMixin | Profession restriction |
| ServerLevelPoiMixin extension | Village deregistration | Job block POI disable |
| StageManager.getCurrentStage() | PatrolSpawnerMixin | Level-up stage gates |
| InteractionResult.SUCCESS/FAIL | LecternEnchanting, BellHandler | Villager interaction feedback |
| Component.literal + displayClientMessage | LecternEnchanting, ClassManager | Error messages |
| ServerHolder for server access | BrainPoiMemoryMixin | Profession mixin server access |

## Implementation Order

Based on dependencies:

1. **Job block POI disabling** (extends existing mixin, no new files)
   - Prerequisite: None
   - Enables: Villagers can only find allowed job blocks

2. **Profession restriction mixin** (new mixin)
   - Prerequisite: None (but POI blocking helps)
   - Enables: Existing villagers with wrong professions stay that way

3. **Custom trade tables** (new data structures)
   - Prerequisite: None
   - Enables: Leveling/cycling have trades to apply

4. **Manual leveling handler** (UseEntityCallback)
   - Prerequisite: Trade tables, StageManager
   - Enables: Players can level villagers

5. **Trade cycling handler** (same UseEntityCallback)
   - Prerequisite: Trade tables
   - Enables: Players can reroll trades

6. **Rail recipes** (data generation)
   - Prerequisite: None
   - Independent of villager changes

## Anti-Patterns to Avoid

### Anti-Pattern 1: Modifying Villager Brain Directly
**What:** Adding custom goals/behaviors to villager brain
**Why bad:** Brain API is complex, easy to break other behaviors
**Instead:** Intercept at data level (VillagerData, trades) not AI level

### Anti-Pattern 2: Replacing Entire Trade System
**What:** Custom merchant implementation instead of modifying vanilla
**Why bad:** Breaks compatibility, massive scope creep
**Instead:** Modify trades at ItemListing level, keep vanilla merchant logic

### Anti-Pattern 3: Multiple UseEntityCallback Registrations
**What:** Separate registrations for leveling, cycling, etc.
**Why bad:** Order issues, duplicated entity type checks
**Instead:** Single VillagerInteraction handler that dispatches internally

### Anti-Pattern 4: Storing Villager State in Attachments
**What:** Using THCAttachments for villager trade/level data
**Why bad:** VillagerData already has persistent level, trades are in MerchantOffers
**Instead:** Use vanilla data structures, only override behavior

## Scalability Considerations

| Concern | At 10 villagers | At 100 villagers | At 1000 villagers |
|---------|-----------------|------------------|-------------------|
| POI filtering | Negligible | Negligible | Negligible (per-block, not per-villager) |
| Profession mixin | Called on profession change only | Same | Same |
| UseEntityCallback | Per-interaction only | Same | Same |
| Trade table lookup | O(1) map access | Same | Same |

No performance concerns identified. All interceptions are event-driven, not tick-based.

## Component File Structure

```
src/main/
  java/thc/
    mixin/
      VillagerProfessionMixin.java      # Profession restriction
      ServerLevelPoiMixin.java          # Extend for job block POI
    villager/
      VillagerTrades.java               # Custom trade ItemListings

  kotlin/thc/
    villager/
      VillagerInteraction.kt            # UseEntityCallback handler
      AllowedProfessions.kt             # Constants + validation
```

## Sources

- THC codebase analysis: StageManager.java, THC.kt (UseEntityCallback), ServerLevelPoiMixin.java, BrainPoiMemoryMixin.java, AbstractVillagerMixin.java
- Existing patterns: LecternEnchanting.kt, PatrolSpawnerMixin.java
- Minecraft 1.21.11: Villager class structure (VillagerData, tradingXp, MerchantOffers)

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Profession restriction | HIGH | setVillagerData is clear chokepoint, similar to other data mixins |
| POI blocking | HIGH | Exact reuse of existing ServerLevelPoiMixin pattern |
| UseEntityCallback | HIGH | Exact reuse of cow milking pattern |
| Stage integration | HIGH | Exact reuse of PatrolSpawnerMixin pattern |
| Trade tables | MEDIUM | TradeOfferHelper API needs verification for clearing existing trades |
| Trade cycling | MEDIUM | Villager.updateTrades() behavior needs verification |
