# Phase 47: Saddle Removal - Research

**Researched:** 2026-01-25
**Domain:** Minecraft data pack loot table modification, recipe removal, villager trade filtering
**Confidence:** HIGH

## Summary

Saddle removal in Minecraft 1.21.11 requires coordinating three distinct systems: loot table overrides via data packs, recipe removal via mixin, and villager trade filtering via mixin. The project already has established patterns for all three approaches from previous item removals (shields, spears, bows).

Minecraft 1.21.6 made saddles craftable and removed them from several loot tables (dungeons, ancient cities, desert temples, jungle temples, stronghold altars). However, saddles still appear in: Bastion remnants (hoglin stable), End cities, Nether fortresses, three village chest types (weaponsmith, tannery, savanna house), fishing treasure loot, and as guaranteed drops from ravagers. Leatherworker villagers at Master level also sell saddles for 6 emeralds.

The codebase already overrides 8 of these loot tables with saddles present. Additional loot tables need creation/modification to remove all saddles. The established pattern is full loot table replacement via data pack files at `data/minecraft/loot_table/` - Minecraft does not support partial/patch-based modifications in version 1.21.11.

**Primary recommendation:** Follow existing patterns - add "saddle" to RecipeManagerMixin's REMOVED_RECIPE_PATHS, add saddle filtering to AbstractVillagerMixin's getOffers injection, and override remaining loot tables by copying vanilla JSON and removing saddle entries.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric Mixin | 0.15.11+ | Runtime class modification | Standard Fabric modding approach for behavior changes |
| Minecraft Data Packs | 1.21.11 | Static data overrides | Vanilla system for loot tables, recipes, advancements |
| Fabric API | 0.110.5+ | Mod foundation | Required for Fabric mods, provides base APIs |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| SpongePowered ASM | (via Mixin) | Bytecode manipulation | Mixins use this internally - no direct usage needed |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Full loot table override | Loot Table Modifier mod | Requires additional mod dependency, overkill for complete removal |
| Data packs | Fabric Loot API events | More code complexity, data packs sufficient for static overrides |
| Recipe mixin | Recipe data pack override | Mixin approach already established, consistent with project patterns |

**Installation:**
No new dependencies required - all functionality available in existing Fabric + Fabric API setup.

## Architecture Patterns

### Recommended File Structure
```
data/minecraft/
├── loot_table/
│   ├── chests/
│   │   ├── bastion_hoglin_stable.json    # Already exists, modify
│   │   ├── end_city_treasure.json        # Already exists, modify
│   │   ├── nether_bridge.json            # Already exists, modify
│   │   ├── desert_pyramid.json           # Create new (copy vanilla, remove saddle)
│   │   ├── jungle_temple.json            # Create new
│   │   ├── underwater_ruin_big.json      # Create new
│   │   ├── underwater_ruin_small.json    # Create new
│   │   └── village/
│   │       ├── village_weaponsmith.json  # Already exists, modify
│   │       ├── village_tannery.json      # Already exists, modify
│   │       └── village_savanna_house.json # Already exists, modify
│   ├── entities/
│   │   └── ravager.json                  # Already exists, modify
│   └── gameplay/
│       └── fishing/
│           └── treasure.json             # Already exists, modify
└── recipe/
    └── saddle.json                       # Already exists, leave as-is (mixin handles removal)

src/main/java/thc/mixin/
├── RecipeManagerMixin.java               # Modify: add "saddle" to REMOVED_RECIPE_PATHS
└── AbstractVillagerMixin.java            # Modify: add saddle filter to getOffers
```

### Pattern 1: Loot Table Override (Full Replacement)
**What:** Replace entire vanilla loot table JSON file in mod's data pack
**When to use:** Removing items from generated loot (chests, mobs, fishing)
**Example:**
```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:diamond"
        }
        // Saddle entry removed - rest of JSON preserved from vanilla
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "minecraft:chests/example"
}
```
**Source:** Existing project files like `data/minecraft/loot_table/chests/bastion_hoglin_stable.json`

### Pattern 2: Recipe Removal via Mixin
**What:** Filter recipes during RecipeManager preparation phase
**When to use:** Preventing crafting of specific items
**Example:**
```java
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Unique
    private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
        "shield",
        "saddle",  // Add this
        "wooden_spear"
        // ... other removed recipes
    );

    @Inject(
        method = "prepare",
        at = @At("RETURN"),
        cancellable = true
    )
    private void thc$removeDisabledRecipes(CallbackInfoReturnable<RecipeMap> cir) {
        // Filter logic already implemented
    }
}
```
**Source:** `/mnt/c/home/code/thc/src/main/java/thc/mixin/RecipeManagerMixin.java`

### Pattern 3: Villager Trade Filtering
**What:** Remove trades by result item during offer retrieval
**When to use:** Preventing villagers from selling specific items
**Example:**
```java
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void thc$removeShieldTrades(CallbackInfoReturnable<MerchantOffers> cir) {
        MerchantOffers offers = cir.getReturnValue();
        if (offers == null) return;
        offers.removeIf(offer -> offer.getResult().is(Items.SHIELD));
        offers.removeIf(offer -> offer.getResult().is(Items.SADDLE));  // Add this
    }
}
```
**Source:** `/mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractVillagerMixin.java`

### Anti-Patterns to Avoid
- **Partial loot table modification:** Minecraft 1.21.11 does not support loot table patching - attempting `LootTable.Builder.modify()` or similar will fail. Must replace entire file.
- **Deleting vanilla recipe files:** Recipe files in `data/minecraft/recipe/` should remain (even if placeholder). RecipeManagerMixin handles removal at runtime.
- **Client-only mixins for server data:** Loot tables and recipes are server-side. Don't use `@Environment(EnvType.CLIENT)` for these mixins.
- **Modifying loot after generation:** Intercept at source (loot table files) not at container filling time (more complex, error-prone).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Loot table partial updates | Custom loot table merger/patcher | Full file replacement in data pack | Minecraft 1.21.11 has no patch API - full replacement is the vanilla-supported approach |
| Villager trade data packs | Custom trade JSON parser | Mixin filtering on `getOffers()` | Villager trades aren't data-driven until MC 26.1+ - must use mixins in 1.21.11 |
| Recipe removal via events | Fabric Lifecycle Events | RecipeManagerMixin filtering | Project pattern already established, events add unnecessary complexity |
| Item drop interception | LivingDropsEvent handlers | Entity loot table override | Loot tables are authoritative source, cleaner than event-based filtering |

**Key insight:** Minecraft 1.21.11 does not support partial data pack modifications. The "full replacement" pattern feels heavyweight but is the only vanilla-supported approach. Third-party mods (LootTableModifier, DataTrades) enable patching but add dependencies inappropriate for this project.

## Common Pitfalls

### Pitfall 1: Incomplete Loot Table Coverage
**What goes wrong:** Saddles remain obtainable from overlooked loot sources
**Why it happens:** Multiple loot table locations (9+ chest types, fishing, mob drops), easy to miss one
**How to avoid:**
- Cross-reference wiki "Generated loot" page for complete source list
- Grep existing loot tables for "saddle" to find already-overridden files
- Test in-game at each source (creative mode structure finding, fishing, mob spawning)
**Warning signs:** Players report finding saddles in chests after "complete" removal

### Pitfall 2: Version-Specific Loot Table Formats
**What goes wrong:** Loot table JSON uses wrong format, fails to load or gets replaced by vanilla
**Why it happens:** Format changed in 1.21 (loot_tables → loot_table singular, component format changes)
**How to avoid:**
- Copy from vanilla 1.21.11 files as baseline (use misode.github.io generator set to 1.21)
- Preserve `random_sequence` field exactly as vanilla (affects RNG, important for testing consistency)
- Use `"type": "minecraft:chest"` not `"type": "chest"` (namespace required in 1.21+)
**Warning signs:** Console warnings about malformed loot tables during world load

### Pitfall 3: Recipe Mixin Path Matching
**What goes wrong:** Recipe still appears in crafting book despite being in REMOVED_RECIPE_PATHS
**Why it happens:** Mixin checks `holder.id().identifier().getPath()` which returns filename without namespace/extension
**How to avoid:**
- Use exact filename without `.json`: `"saddle"` not `"minecraft:saddle"` or `"saddle.json"`
- Recipe ID path is just the filename part: `minecraft:saddle` → path is `"saddle"`
- Test in creative mode crafting book (JEI/REI if installed, vanilla recipe book if not)
**Warning signs:** Recipe visible in crafting book, crafting table allows saddle creation

### Pitfall 4: Villager Trade Timing
**What goes wrong:** Existing villagers keep saddle trades, only new villagers have trades removed
**Why it happens:** `getOffers()` populates trades on first interaction, then caches. Mixin runs but cached trades persist.
**How to avoid:**
- Trade filtering happens on retrieval - existing trades are already cached on villager entities
- Document that pre-existing leatherworkers retain saddle trades (working as designed)
- Alternative: Add mixin to repopulate trades on villager tick/level load (complex, high risk)
**Warning signs:** Bug reports "saddle removal doesn't work" from players with established villages

### Pitfall 5: Ravager Drop Confusion
**What goes wrong:** Confusion about whether ravagers should drop saddles (they naturally carry them)
**Why it happens:** Ravagers visually wear saddles as part of their model, ambiguous if this is "equipped" or "natural"
**How to avoid:**
- Ravager loot table currently shows saddle drop - this is intentional vanilla behavior (100% drop rate)
- Requirement SADL-02 says "Mobs no longer drop saddles" - interpret as "no guaranteed saddle drops"
- Override ravager.json to remove saddle entry (ravagers never naturally wore saddles in vanilla)
**Warning signs:** Players farming ravagers for guaranteed saddles

## Code Examples

Verified patterns from project codebase:

### Full Loot Table Override Example
```json
// File: data/minecraft/loot_table/chests/nether_bridge.json
// Source: /mnt/c/home/code/thc/data/minecraft/loot_table/chests/nether_bridge.json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:diamond"
        },
        // Saddle entry removed here - would be:
        // {
        //   "type": "minecraft:item",
        //   "name": "minecraft:saddle"
        // },
        {
          "type": "minecraft:item",
          "name": "minecraft:iron_ingot"
        }
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "minecraft:chests/nether_bridge"
}
```

### Recipe Removal Mixin Pattern
```java
// File: src/main/java/thc/mixin/RecipeManagerMixin.java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/RecipeManagerMixin.java
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Unique
    private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
        "shield",
        "wooden_spear",
        "saddle"  // Add this entry
    );

    @Inject(
        method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void thc$removeDisabledRecipes(ResourceManager rm, ProfilerFiller prof, CallbackInfoReturnable<RecipeMap> cir) {
        RecipeMap recipes = cir.getReturnValue();
        Collection<RecipeHolder<?>> values = recipes.values();
        List<RecipeHolder<?>> filtered = new ArrayList<>(values.size());
        for (RecipeHolder<?> holder : values) {
            if (!REMOVED_RECIPE_PATHS.contains(holder.id().identifier().getPath())) {
                filtered.add(holder);
            }
        }
        if (filtered.size() != values.size()) {
            cir.setReturnValue(RecipeMap.create(filtered));
        }
    }
}
```

### Villager Trade Filter Pattern
```java
// File: src/main/java/thc/mixin/AbstractVillagerMixin.java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractVillagerMixin.java
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void thc$removeShieldTrades(CallbackInfoReturnable<MerchantOffers> cir) {
        MerchantOffers offers = cir.getReturnValue();
        if (offers == null) {
            return;
        }
        offers.removeIf(offer -> offer.getResult().is(Items.SHIELD));
        offers.removeIf(offer -> offer.getResult().is(Items.BELL));
        offers.removeIf(offer -> offer.getResult().is(Items.SADDLE));  // Add this line
    }
}
```

### Entity Loot Table Override Example
```json
// File: data/minecraft/loot_table/entities/ravager.json
// Source: /mnt/c/home/code/thc/data/minecraft/loot_table/entities/ravager.json (before modification)
// Current file shows saddle drop - remove the saddle pool entirely
{
  "type": "minecraft:entity",
  "pools": [
    // Remove this entire pool that drops saddle:
    // {
    //   "bonus_rolls": 0.0,
    //   "entries": [
    //     {
    //       "type": "minecraft:item",
    //       "functions": [
    //         {
    //           "add": false,
    //           "count": 1.0,
    //           "function": "minecraft:set_count"
    //         }
    //       ],
    //       "name": "minecraft:saddle"
    //     }
    //   ],
    //   "rolls": 1.0
    // }
  ],
  "random_sequence": "minecraft:entities/ravager"
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Saddles uncraftable | Saddles craftable (3 leather + 1 iron) | MC 1.21.6 (2025-06-17) | THC must actively remove recipe, not just block it |
| Saddles in dungeon/temple loot | Removed from dungeons, temples, ancient cities, strongholds | MC 1.21.6 | Fewer loot tables to override (5 fewer files) |
| Static villager trades | Data-driven trades (JSON) | MC 26.1+ (2026+) | THC on 1.21.11 still requires mixins for trades |
| Loot table patching mods common | Native patching still unavailable | Still current in 1.21.11 | Must use full replacement pattern |

**Deprecated/outdated:**
- **Loot table format (pre-1.21):** Used `loot_tables` (plural) directory, some function names changed
- **Recipe format (pre-1.21):** `recipes` (plural) directory, component format different
- **Villager trade events:** Some tutorials reference Forge events - Fabric uses different approach (direct mixin filtering cleaner)

## Open Questions

Things that couldn't be fully resolved:

1. **Strider jockey saddle drops**
   - What we know: Striders spawning with zombified piglin riders naturally have saddles equipped
   - What's unclear: Whether killing the strider (not the piglin) drops the saddle, and if this is controlled by strider loot table or equipment drop logic
   - Recommendation: Test in-game with strider jockey spawn egg. If saddles drop, override `data/minecraft/loot_table/entities/strider.json` to remove saddle entry. Low priority - strider jockeys rare in normal gameplay.

2. **Buried treasure loot table**
   - What we know: Wiki lists buried treasure as possible saddle source in older versions
   - What's unclear: Whether 1.21.11 still includes saddles in buried treasure (not in project's current overrides)
   - Recommendation: Check vanilla 1.21.11 `data/minecraft/loot_table/chests/buried_treasure.json` file. If saddles present, override. Medium confidence they were removed in 1.21.6 changes.

3. **Advancement references to saddles**
   - What we know: Project overrides many advancement files, some may reference saddle acquisition
   - What's unclear: Whether any advancements trigger on saddle crafting/obtaining, creating broken progression
   - Recommendation: Grep advancement files for "saddle" references, check if any criteria need updating. Low priority - broken advancements non-critical for hardcore mod.

## Sources

### Primary (HIGH confidence)
- Minecraft Wiki - Saddle: https://minecraft.wiki/w/Saddle
  - Comprehensive source list: chest loot tables, mob drops, fishing, trading
  - Version history: 1.21.6 changes (craftable recipe, loot removals)
- Minecraft Wiki - Trading: https://minecraft.wiki/w/Trading
  - Leatherworker Master level: 6 emeralds for 1 saddle
- Minecraft Wiki - Loot Table: https://minecraft.wiki/w/Loot_table
  - JSON structure, override mechanism via data packs
  - Full replacement required (no patching)
- Minecraft Wiki - Generated Loot: https://minecraft.wiki/w/Generated_loot
  - Loot table removal history (1.21.6 changes)
- Minecraft Wiki - Java Edition 1.21.6: Version-specific changes
  - Saddle crafting recipe addition
  - Loot table removals (5 locations)
  - Shears equipment removal mechanic

### Secondary (MEDIUM confidence)
- Project codebase files (verified patterns):
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/RecipeManagerMixin.java` - Recipe removal pattern
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractVillagerMixin.java` - Trade filtering pattern
  - `/mnt/c/home/code/thc/data/minecraft/loot_table/` - 8 existing overrides with saddles
- Fabric Wiki - Adding to Loot Tables: https://wiki.fabricmc.net/tutorial:adding_to_loot_tables
  - Data pack override mechanism (confirms full replacement approach)
- Minecraft Wiki - Data Pack: https://minecraft.wiki/w/Data_pack
  - Data pack structure, override priority rules

### Tertiary (LOW confidence, informational only)
- How to Get a Saddle in Minecraft (dathost.net): https://dathost.net/blog/how-to-get-a-saddle-in-minecraft-all-methods-drop-rates
  - General saddle sources (cross-verified with wiki)
- VillagerConfig mod (CurseForge/Modrinth): https://www.curseforge.com/minecraft/mc-mods/villagerconfig
  - Demonstrates trade modification complexity (confirms mixin approach appropriate)
- DataTrades mod: https://github.com/PssbleTrngle/DataTrades
  - Alternative JSON-based trade system (requires additional mod - not used)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Established patterns in codebase, no new dependencies needed
- Architecture: HIGH - All three patterns (loot tables, recipes, trades) already implemented for other items
- Pitfalls: MEDIUM - Common issues identified through code review and wiki research, but in-game testing needed for validation

**Research date:** 2026-01-25
**Valid until:** ~30 days (2026-02-25) - Minecraft 1.21.11 is stable release, no imminent format changes expected. MC 26.1+ will introduce breaking changes but outside project scope.
