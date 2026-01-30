---
phase: 64
plan: 01
subsystem: food-economy
tags: [dough, bread, recipes, crafting, smelting]
dependency-graph:
  requires: []
  provides: [dough-item, dough-recipes, bread-recipe-removal]
  affects: [64-02]
tech-stack:
  added: []
  patterns: [recipe-removal-via-mixin, shapeless-crafting, smelting-recipe]
key-files:
  created:
    - src/main/resources/assets/thc/models/item/dough.json
    - src/main/resources/data/thc/recipe/dough.json
    - src/main/resources/data/thc/recipe/dough_copper.json
    - src/main/resources/data/thc/recipe/dough_smelting.json
    - src/main/resources/data/thc/recipe/dough_smoking.json
  modified:
    - src/main/java/thc/mixin/RecipeManagerMixin.java
    - src/main/kotlin/thc/item/THCItems.kt
    - src/main/resources/assets/thc/lang/en_us.json
decisions:
  - id: FOOD-DOUGH-01
    description: Dough requires water bucket (iron or copper) not just wheat
    rationale: Adds processing step to bread creation
metrics:
  duration: 5 min
  completed: 2026-01-30
---

# Phase 64 Plan 01: Dough Item and Recipes Summary

Dough crafting system with wheat + water bucket recipes, furnace/smoker cooking to bread

## What Was Built

### Task 1: Dough Item Registration
- Added "bread" to `REMOVED_RECIPE_PATHS` in RecipeManagerMixin to block vanilla bread crafting
- Registered `DOUGH` item in THCItems following SOUL_DUST pattern (64 stack size)
- Added DOUGH to food creative tab
- Created dough.json item model using existing dough.png texture
- Added "Dough" translation to en_us.json

### Task 2: Dough Recipes
- **dough.json**: 3 wheat + iron water bucket = 1 dough (bucket returned via vanilla craftRemainder)
- **dough_copper.json**: 3 wheat + copper water bucket = 1 dough (bucket returned via craftRemainder set on COPPER_BUCKET_OF_WATER)
- **dough_smelting.json**: dough -> bread in furnace (200 ticks = 10 seconds, 0.35 XP)
- **dough_smoking.json**: dough -> bread in smoker (100 ticks = 5 seconds, 0.35 XP)

## Key Implementation Details

**Recipe Removal Pattern**: Uses existing `REMOVED_RECIPE_PATHS` Set in RecipeManagerMixin - simply add "bread" to the set.

**Bucket Preservation**: Iron water bucket uses vanilla craftRemainder (returns empty bucket automatically). Copper water bucket has `.craftRemainder(COPPER_BUCKET)` set in THCItems.kt registration.

**Cooking Times**: Smoker cooks 2x faster than furnace (vanilla behavior for smoking recipes).

## Commits

| Hash | Message |
|------|---------|
| 01ce15a | feat(64-01): add dough item and remove bread recipe |
| 703d467 | feat(64-01): add dough crafting and cooking recipes |

## Verification

- [x] Build succeeds with all files
- [x] REMOVED_RECIPE_PATHS contains "bread"
- [x] DOUGH item registered in THCItems
- [x] All 4 recipe files are valid JSON
- [x] Smelting recipe: 200 ticks (10 seconds)
- [x] Smoking recipe: 100 ticks (5 seconds)

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

Plan 64-02 (cake recipe modification) can proceed. Dough item is available for use in cake recipe.
