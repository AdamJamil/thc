---
phase: 32-food-removals
plan: 01
subsystem: food
tags: [recipes, mixin, food-balance]

dependency-graph:
  requires: []
  provides:
    - removed-suspicious-stew-recipe
    - removed-mushroom-stew-recipe
    - removed-beetroot-soup-recipe
    - removed-sugar-from-sugarcane-recipe
  affects:
    - food progression
    - sugar availability (honey-only)

tech-stack:
  added: []
  patterns:
    - REMOVED_RECIPE_PATHS extension for food items

file-tracking:
  key-files:
    created: []
    modified:
      - src/main/java/thc/mixin/RecipeManagerMixin.java

decisions:
  - id: sugar-recipe-id
    choice: sugar_from_sugar_cane (not just sugar)
    rationale: Preserve honey-based sugar recipe while blocking sugarcane

metrics:
  duration: 1 min
  completed: 2026-01-22
---

# Phase 32 Plan 01: Food Recipe Removals Summary

Remove low-value food recipes (suspicious stew, mushroom stew, beetroot soup, sugarcane sugar) via REMOVED_RECIPE_PATHS filtering.

## What Was Built

### Recipe Filtering for 4 Low-Value Foods

Added to `REMOVED_RECIPE_PATHS` Set in RecipeManagerMixin:
- `suspicious_stew` - removes suspicious stew crafting
- `mushroom_stew` - removes mushroom stew crafting
- `beetroot_soup` - removes beetroot soup crafting
- `sugar_from_sugar_cane` - removes sugarcane-to-sugar recipe (preserves honey-based sugar)

## Key Implementation Details

**RecipeManagerMixin Extension:**
```java
private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
    // ... existing entries ...
    "suspicious_stew",
    "mushroom_stew",
    "beetroot_soup",
    "sugar_from_sugar_cane"
);
```

The existing `prepare()` injection intercepts all recipes and filters based on path matching, so adding paths to the Set is all that's needed.

## Commits

| Hash | Message |
|------|---------|
| 22cd2f2 | feat(32-01): remove low-value food recipes |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] `./gradlew build` succeeds without errors
- [x] grep finds all 4 recipe paths in RecipeManagerMixin.java

## Next Phase Readiness

Phase 32 complete. No blockers for subsequent phases.
