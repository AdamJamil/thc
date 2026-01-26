---
phase: 51-brewing-removal
plan: 01
subsystem: economy-balance
status: complete
tags: [brewing, potions, bartering, structure-gen, recipes]

requires:
  - "Phase 40: RecipeManagerMixin pattern for recipe removal"
  - "Phase 45: StructureTemplateMixin pattern for structure block filtering"

provides:
  - brewing-stands-removed-from-structures
  - brewing-stand-recipe-disabled
  - fire-resistance-potions-removed-from-bartering
  - potion-economy-eliminated

affects:
  - "Phase 52: Armor crafting changes (complete economy rebalance)"

tech-stack:
  added: []
  patterns:
    - "Loot table data pack override for gameplay loot tables"
    - "Structure block filtering via Set.contains check"
    - "Recipe path filtering via Set.contains check"

key-files:
  created:
    - src/main/resources/data/minecraft/loot_table/gameplay/piglin_bartering.json
  modified:
    - src/main/java/thc/mixin/StructureTemplateMixin.java
    - src/main/java/thc/mixin/RecipeManagerMixin.java

decisions:
  - id: BREW-01
    choice: "Add Blocks.BREWING_STAND to FILTERED_STRUCTURE_BLOCKS"
    rationale: "Reuses existing structure filtering pattern to remove brewing stands from villages, igloos, and End ships"

  - id: BREW-02
    choice: "Add 'brewing_stand' to REMOVED_RECIPE_PATHS"
    rationale: "Reuses existing recipe removal pattern to disable brewing stand crafting"

  - id: BREW-03
    choice: "Create loot table override for piglin_bartering.json"
    rationale: "Data pack override is cleanest approach for selective loot table modification"

  - id: BREW-04
    choice: "Keep water bottle in piglin bartering"
    rationale: "Water bottles have no buff/healing effect, only utility for filling/cauldrons per CONTEXT.md"

metrics:
  duration: "3 minutes"
  completed: 2026-01-26
---

# Phase 51 Plan 01: Brewing Removal Summary

Remove brewing stands and potions from game economy to force reliance on food healing and skill-based combat.

## What Was Built

Eliminated brewing economy through three complementary changes:
1. **Structure filtering**: Brewing stands no longer generate in villages, igloos, or End ships
2. **Recipe removal**: Brewing stand crafting recipe disabled
3. **Bartering changes**: Fire Resistance potions removed from piglin bartering, water bottles retained

## Technical Approach

### Structure Block Filtering
Added `Blocks.BREWING_STAND` to existing `FILTERED_STRUCTURE_BLOCKS` set in StructureTemplateMixin. The existing @Redirect on `setBlock` in `placeInWorld` automatically prevents brewing stand placement in all structures.

### Recipe Removal
Added `"brewing_stand"` to existing `REMOVED_RECIPE_PATHS` set in RecipeManagerMixin. The existing @Inject on recipe loading filters out the brewing stand recipe.

### Piglin Bartering Override
Created data pack override at `data/minecraft/loot_table/gameplay/piglin_bartering.json`:
- Removed Fire Resistance potion (weight 8)
- Removed Splash Fire Resistance potion (weight 8)
- Retained water bottle (weight 10, no buff effect)
- Preserved all other vanilla bartering items (soul speed books, ender pearls, iron nuggets, etc.)

Critical format: `"type": "minecraft:barter"` with `"random_sequence": "minecraft:gameplay/piglin_bartering"`

## Decisions Made

**BREW-01: Brewing Stand Structure Filtering**
- Added to existing FILTERED_STRUCTURE_BLOCKS pattern
- Blocks generation in villages, igloos, End ships
- Updated class Javadoc to document brewing stand filtering

**BREW-02: Brewing Stand Recipe Removal**
- Added to existing REMOVED_RECIPE_PATHS pattern
- Prevents crafting via recipe book
- No special handling needed, path-based filtering works universally

**BREW-03: Loot Table Override Approach**
- Data pack override chosen over mixin interception
- Cleaner for selective item removal from loot tables
- Follows vanilla data pack override semantics

**BREW-04: Water Bottle Retention**
- Water bottles kept in bartering pool
- No buff/healing effect (per CONTEXT.md)
- Only utility value for filling containers/cauldrons

## Deviations from Plan

None - plan executed exactly as written.

## Implementation Notes

### Why Set-Based Filtering Works
Both StructureTemplateMixin and RecipeManagerMixin use Set.contains() checks in their injection points, making addition of new filtered items trivial (single line in Set.of()).

### Loot Table Override Location
File must be at `src/main/resources/data/minecraft/loot_table/gameplay/piglin_bartering.json` to override vanilla loot table at `minecraft:gameplay/piglin_bartering`.

### Fire Resistance Potion Identification
Vanilla bartering had two potion entries with `"function": "minecraft:set_potion", "id": "minecraft:fire_resistance"`:
- Regular potion (weight 8)
- Splash potion (weight 8)

Both removed. Water bottle entry uses `"id": "minecraft:water"` and was preserved.

## Files Changed

### Created
- `src/main/resources/data/minecraft/loot_table/gameplay/piglin_bartering.json` (243 lines)
  - Vanilla bartering loot table minus fire resistance potions
  - Type: "minecraft:barter"
  - Retained 18 vanilla bartering items

### Modified
- `src/main/java/thc/mixin/StructureTemplateMixin.java`
  - Added Blocks.BREWING_STAND to FILTERED_STRUCTURE_BLOCKS
  - Updated class Javadoc

- `src/main/java/thc/mixin/RecipeManagerMixin.java`
  - Added "brewing_stand" to REMOVED_RECIPE_PATHS

## Testing Strategy

**Structure Generation**: Create new world, locate village/igloo/End ship, verify no brewing stands
**Recipe Access**: Open recipe book, search "brewing", verify no recipe appears
**Piglin Bartering**: Trade gold ingots with piglins, verify no fire resistance potions in results
**Water Bottles**: Verify water bottles still obtainable from piglins (weight 10)

## Next Phase Readiness

**Phase 52 (Armor Crafting) Ready**
- Economy rebalance continues with armor crafting restrictions
- Brewing removal eliminates potion buffs, armor changes address defense
- Combined effect: players must rely on skill over gear/consumables

**No Blockers**
- All changes compile successfully
- No new dependencies introduced
- No architectural changes needed

## Requirements Satisfied

- ✅ BREW-01: Brewing stands filtered from structure generation
- ✅ BREW-02: Brewing stand recipe removed
- ✅ BREW-03: Fire Resistance potions removed from piglin bartering
- ✅ All changes compile successfully

## Commits

- 4b6371d: feat(51-01): filter brewing stands from structure generation
- 4ecfdf1: feat(51-01): remove brewing stand recipe
- 3d618f4: feat(51-01): remove fire resistance potions from piglin bartering
