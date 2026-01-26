# Phase 51: Brewing Removal - Research

**Researched:** 2026-01-25
**Domain:** Brewing stand removal, loot table modification, structure generation filtering
**Confidence:** HIGH

## Summary

This phase removes the brewing economy by eliminating brewing stands from all sources: structure spawns, crafting, and piglin bartering potions. The implementation uses three established patterns already in the codebase:

1. **Structure block filtering** via `StructureTemplateMixin` - add `Blocks.BREWING_STAND` to `FILTERED_STRUCTURE_BLOCKS`
2. **Recipe removal** via `RecipeManagerMixin` - add `"brewing_stand"` to `REMOVED_RECIPE_PATHS`
3. **Loot table override** via data pack - create `gameplay/piglin_bartering.json` without potion entries

All three patterns are proven and require minimal code changes. The phase is straightforward with high confidence.

**Primary recommendation:** Add brewing stand to existing StructureTemplateMixin and RecipeManagerMixin sets, then create piglin bartering loot table override with potion entries removed.

## Standard Stack

The established approach for this domain is already in the codebase:

### Core
| Component | Location | Purpose | Why Standard |
|-----------|----------|---------|--------------|
| StructureTemplateMixin | `src/main/java/thc/mixin/StructureTemplateMixin.java` | Block filtering from structure generation | Proven pattern for furnace/smoker removal |
| RecipeManagerMixin | `src/main/java/thc/mixin/RecipeManagerMixin.java` | Recipe removal via REMOVED_RECIPE_PATHS | Proven pattern for 10+ recipes |
| Loot table override | `src/main/resources/data/minecraft/loot_table/` | Complete table replacement | Proven pattern for saddle removal |

### Supporting
| Library | Purpose | When to Use |
|---------|---------|-------------|
| Fabric Loader 0.18.4+ | Mixin injection | All mixin modifications |
| Data pack JSON | Loot table overrides | Replacing vanilla loot tables |

### No Alternatives Needed
All approaches are established project patterns. No new patterns required.

**Installation:** No new dependencies needed.

## Architecture Patterns

### Pattern 1: Structure Block Filtering
**What:** Intercept `StructureTemplate.placeInWorld` setBlock calls and skip filtered blocks
**When to use:** Removing specific blocks from all structure generation
**Example:**
```java
// Source: src/main/java/thc/mixin/StructureTemplateMixin.java
@Unique
private static final Set<Block> FILTERED_STRUCTURE_BLOCKS = Set.of(
    Blocks.FURNACE,
    Blocks.BLAST_FURNACE,
    Blocks.SMOKER,
    Blocks.BREWING_STAND  // Add this
);

@Redirect(
    method = "placeInWorld",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
    )
)
private boolean thc$filterFurnaceBlocks(
        ServerLevelAccessor level,
        BlockPos pos,
        BlockState state,
        int flags) {
    if (FILTERED_STRUCTURE_BLOCKS.contains(state.getBlock())) {
        return true; // Pretend success, skip actual placement
    }
    return level.setBlock(pos, state, flags);
}
```

### Pattern 2: Recipe Removal
**What:** Filter recipes from RecipeMap after loading
**When to use:** Removing vanilla recipes without data pack files
**Example:**
```java
// Source: src/main/java/thc/mixin/RecipeManagerMixin.java
@Unique
private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
    "shield",
    "furnace",
    // ... existing entries ...
    "brewing_stand"  // Add this
);
```

### Pattern 3: Loot Table Override (Data Pack)
**What:** Complete JSON replacement of vanilla loot table
**When to use:** Removing specific items from loot pools
**Example location:** `src/main/resources/data/minecraft/loot_table/gameplay/piglin_bartering.json`
**Format:**
```json
{
  "type": "minecraft:barter",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        // All vanilla entries EXCEPT potions
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "minecraft:gameplay/piglin_bartering"
}
```

### Anti-Patterns to Avoid
- **Mixin for bartering:** Don't create a Piglin mixin to intercept bartering - loot table override is cleaner and maintains vanilla behavior
- **Partial loot table:** Don't try to "subtract" entries - copy full vanilla table, remove unwanted items
- **Block entity handling:** Don't worry about brewing stand block entity contents in structures - filtering the block prevents entity creation

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Structure block removal | Custom world gen hook | StructureTemplateMixin | Existing pattern handles all structures uniformly |
| Recipe hiding | Data pack empty recipe | RecipeManagerMixin | Cleaner, no extra files, existing pattern |
| Loot modification | Mixin on getLoot | Data pack override | Vanilla behavior preserved, simpler debugging |

**Key insight:** All three requirements map directly to existing patterns. No new code architecture needed.

## Common Pitfalls

### Pitfall 1: Forgetting End Ships
**What goes wrong:** Only filtering villages and igloos, missing brewing stands in End ships
**Why it happens:** End ships are less commonly thought of as brewing stand sources
**How to avoid:** StructureTemplateMixin filters ALL structures including End ships automatically
**Warning signs:** Finding brewing stands in End cities during testing

### Pitfall 2: Potion Entry Identification
**What goes wrong:** Removing wrong potion entries or missing some
**Why it happens:** Vanilla bartering has 3 potion entries with different purposes
**How to avoid:** Remove these specific entries from vanilla piglin_bartering.json:
  - Fire Resistance potion (weight 8, set_potion function)
  - Splash Fire Resistance potion (weight 8, set_potion function)
  - Water bottle (weight 10) - NOTE: This is NOT a potion but can be kept or removed
**Warning signs:** Piglins still giving fire resistance potions

### Pitfall 3: Loot Table Type Mismatch
**What goes wrong:** Using wrong "type" field causes silent failure
**Why it happens:** Different loot contexts need different types
**How to avoid:** Use `"type": "minecraft:barter"` for piglin bartering specifically
**Warning signs:** Piglins drop nothing when bartering

### Pitfall 4: Recipe Path Mismatch
**What goes wrong:** Recipe not removed because path string doesn't match
**Why it happens:** Recipe IDs use resource location paths without namespace
**How to avoid:** Use `"brewing_stand"` (not `"minecraft:brewing_stand"`)
**Warning signs:** Brewing stand still craftable in recipe book

## Code Examples

### Adding Brewing Stand to Structure Filter
```java
// In StructureTemplateMixin.java, modify the existing Set:
@Unique
private static final Set<Block> FILTERED_STRUCTURE_BLOCKS = Set.of(
    Blocks.FURNACE,
    Blocks.BLAST_FURNACE,
    Blocks.SMOKER,
    Blocks.BREWING_STAND  // <-- Add this line
);
```
Source: Existing pattern in StructureTemplateMixin.java

### Adding Brewing Stand Recipe Removal
```java
// In RecipeManagerMixin.java, add to existing Set:
@Unique
private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
    "shield",
    "wooden_spear",
    // ... existing entries ...
    "saddle",
    "brewing_stand"  // <-- Add this line
);
```
Source: Existing pattern in RecipeManagerMixin.java

### Piglin Bartering Loot Table (Potions Removed)
```json
{
  "type": "minecraft:barter",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "functions": [
            {
              "function": "minecraft:enchant_randomly",
              "options": "minecraft:soul_speed"
            }
          ],
          "name": "minecraft:book",
          "weight": 5
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "function": "minecraft:enchant_randomly",
              "options": "minecraft:soul_speed"
            }
          ],
          "name": "minecraft:iron_boots",
          "weight": 8
        },
        // REMOVED: Fire Resistance potion (weight 8)
        // REMOVED: Splash Fire Resistance potion (weight 8)
        // Keep or remove water bottle based on CONTEXT.md guidance
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 36.0,
                "min": 10.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:iron_nugget",
          "weight": 10
        }
        // ... rest of vanilla entries unchanged ...
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "minecraft:gameplay/piglin_bartering"
}
```
Source: data/minecraft/loot_table/gameplay/piglin_bartering.json (vanilla) with modifications

## State of the Art

| Aspect | Current Approach | Notes |
|--------|------------------|-------|
| Structure filtering | StructureTemplateMixin @Redirect | Established in project, works for MC 1.21.11 |
| Recipe removal | RecipeManagerMixin filter | Established in project, handles all recipes |
| Loot tables | Data pack override | Standard Minecraft pattern since 1.14+ |

**Deprecated/outdated:** None - all patterns are current

## Brewing Stand Spawn Locations

Brewing stands naturally generate in three structure types (all handled by StructureTemplateMixin):

| Structure | Location | Contents |
|-----------|----------|----------|
| Village | Church buildings | Empty brewing stand |
| Igloo | Basement (50% of igloos) | Brewing stand with Splash Weakness potion |
| End Ship | Main deck | Brewing stand with 2x Instant Health II |

**Note:** Witch huts do NOT contain brewing stands in vanilla Minecraft.

## Piglin Bartering Potion Entries

The vanilla piglin_bartering.json contains these potion-related entries to remove:

| Entry | Item | Potion Effect | Weight | Action |
|-------|------|---------------|--------|--------|
| 1 | minecraft:potion | fire_resistance | 8 | REMOVE |
| 2 | minecraft:splash_potion | fire_resistance | 8 | REMOVE |
| 3 | minecraft:potion | water | 10 | KEEP (not a real potion) |

Per CONTEXT.md, only Fire Resistance and Splash Fire Resistance are removed. Water bottles are technically "potions" but contain no effect - decision to keep maintains more interesting bartering without adding buff potential.

## Open Questions

None - all implementation details are clear from existing patterns.

## Sources

### Primary (HIGH confidence)
- `src/main/java/thc/mixin/StructureTemplateMixin.java` - existing block filtering pattern
- `src/main/java/thc/mixin/RecipeManagerMixin.java` - existing recipe removal pattern
- `data/minecraft/loot_table/gameplay/piglin_bartering.json` - vanilla loot table structure
- `src/main/resources/data/minecraft/loot_table/entities/ravager.json` - existing loot override pattern

### Secondary (MEDIUM confidence)
- https://minecraft.wiki/w/Brewing_Stand - structure spawn locations verified
- `.planning/phases/47-saddle-removal/47-01-PLAN.md` - loot table override pattern reference
- `.planning/phases/25-furnace-gating/25-02-PLAN.md` - structure filtering pattern reference

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all patterns exist in codebase
- Architecture: HIGH - direct reuse of existing mixins
- Pitfalls: HIGH - straightforward implementation with known patterns

**Research date:** 2026-01-25
**Valid until:** Indefinite - patterns are stable and version-independent within MC 1.21.x
