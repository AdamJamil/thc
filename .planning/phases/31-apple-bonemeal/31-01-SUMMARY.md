---
phase: 31-apple-bonemeal
plan: 01
subsystem: food-farming
tags: [loot-tables, mixin, recipes, farming, food]

dependency-graph:
  requires: []
  provides:
    - universal-apple-drops
    - instant-crop-maturation
    - doubled-bonemeal-yield
  affects:
    - future farming mechanics
    - food balance

tech-stack:
  added: []
  patterns:
    - Loot table override for leaf blocks
    - HEAD injection with cancel for complete method replacement
    - Recipe filtering via REMOVED_RECIPE_PATHS pattern

file-tracking:
  key-files:
    created:
      - src/main/resources/data/minecraft/loot_table/blocks/oak_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/dark_oak_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/birch_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/spruce_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/jungle_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/acacia_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/cherry_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/azalea_leaves.json
      - src/main/resources/data/minecraft/loot_table/blocks/mangrove_leaves.json
      - src/main/java/thc/mixin/CropBlockMixin.java
      - src/main/resources/data/minecraft/recipe/bone_meal.json
    modified:
      - src/main/resources/thc.mixins.json
      - src/main/java/thc/mixin/RecipeManagerMixin.java

decisions:
  - id: apple-drop-rate
    choice: 5x vanilla (2.5% base)
    rationale: Reliable early food without being excessive

metrics:
  duration: 4 min
  completed: 2026-01-22
---

# Phase 31 Plan 01: Apple and Bonemeal Improvements Summary

Universal apple drops from all leaves, 5x drop rate, instant crop maturation, doubled bonemeal yield from bones.

## What Was Built

### 1. Universal Apple Drops (9 leaf types)

All leaf types now drop apples when broken (not just oak/dark oak):
- Oak, dark oak, birch, spruce, jungle, acacia, cherry, azalea, mangrove
- 5x vanilla drop rate: 2.5% base (vs 0.5% vanilla)
- Fortune bonus scales: 2.78% / 3.125% / 4.17% / 12.5%
- Preserves vanilla sapling and stick drops
- Correct sapling mappings (azalea → azalea, mangrove → propagule)

### 2. Instant Crop Maturation

CropBlockMixin intercepts `performBonemeal` method:
- HEAD injection cancels vanilla random growth behavior
- Sets crop directly to max age via `getStateForAge(getMaxAge())`
- One bonemeal = fully grown crop

### 3. Doubled Bonemeal Yield

Recipe override: 1 bone → 6 bonemeal (vs vanilla 3):
- Custom recipe at `data/minecraft/recipe/bone_meal.json`
- Vanilla recipe filtered via `REMOVED_RECIPE_PATHS`

## Key Implementation Details

**Loot Table Structure:**
Each leaf loot table has 3 pools:
1. Silk touch/shears → drop the leaf block
2. Normal break → sapling with fortune bonus
3. Normal break → apple with 5x fortune bonus

**CropBlockMixin Pattern:**
```java
@Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
private void thc$instantGrowth(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
    level.setBlock(pos, this.getStateForAge(this.getMaxAge()), 2);
    ci.cancel();
}
```

## Commits

| Hash | Message |
|------|---------|
| 8425451 | feat(31-01): add 5x apple drops to all leaf types |
| 2ef97a0 | feat(31-01): instant crop maturation on bonemeal |
| 870a919 | feat(31-01): double bonemeal yield from bones (6 instead of 3) |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] All 9 leaf loot table JSON files exist and are valid
- [x] CropBlockMixin compiles without errors
- [x] bone_meal.json recipe file exists
- [x] RecipeManagerMixin updated with "bone_meal" filter
- [x] `./gradlew build` succeeds

## Next Phase Readiness

Phase 31 complete. No blockers for subsequent phases.
