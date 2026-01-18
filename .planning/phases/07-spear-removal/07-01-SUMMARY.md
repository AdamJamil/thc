---
phase: 07-spear-removal
plan: 01
subsystem: gameplay
tags: [mixin, loot-table, recipe, spear, fabric-api]

# Dependency graph
requires:
  - phase: 05-crafting-tweaks
    provides: RecipeManagerMixin pattern for recipe filtering
provides:
  - Spear recipe removal (8 recipes)
  - Spear drop filtering (7 item types)
  - Extended REMOVED_RECIPE_PATHS set pattern
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Set-based recipe path filtering in RecipeManagerMixin
    - Multi-item drop filtering via LootTableEvents.MODIFY_DROPS

key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/RecipeManagerMixin.java
    - src/main/kotlin/thc/THC.kt

key-decisions:
  - "Use recipe path matching instead of full ResourceKey comparison for scalability"
  - "Combine shield and spear filtering into unified removedItems set"

patterns-established:
  - "REMOVED_RECIPE_PATHS: Static set of recipe IDs to filter in RecipeManagerMixin"
  - "removedItems: Combined set for multi-item loot table filtering"

# Metrics
duration: 4min
completed: 2026-01-18
---

# Phase 7 Plan 1: Spear Removal Summary

**8 spear recipes filtered from crafting, 7 spear item types filtered from all loot drops via RecipeManagerMixin and LootTableEvents.MODIFY_DROPS**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-18T13:10:00Z
- **Completed:** 2026-01-18T13:14:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Extended RecipeManagerMixin to filter 8 spear recipes (7 crafting + 1 smithing)
- Extended LootTableEvents.MODIFY_DROPS to filter 7 spear item types from all loot
- Unified filtering approach using Set-based lookup for efficiency

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend RecipeManagerMixin to filter spear recipes** - `50da704` (feat)
2. **Task 2: Extend drop filtering to remove spears from all loot** - `2dd3898` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/RecipeManagerMixin.java` - Added REMOVED_RECIPE_PATHS set with 8 recipe IDs, renamed method to thc$removeDisabledRecipes
- `src/main/kotlin/thc/THC.kt` - Added removedItems set with shield + 7 spears, extended MODIFY_DROPS handler

## Decisions Made
- Used recipe path string matching (holder.id().identifier().getPath()) instead of full ResourceKey comparison - simpler and scales better for multiple recipes
- Combined shield and spear filtering into single removedItems set rather than separate handlers

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed ResourceKey API method name**
- **Found during:** Task 1 (RecipeManagerMixin modification)
- **Issue:** Plan specified `location()` method but Minecraft 1.21.11 uses `identifier()` for ResourceKey
- **Fix:** Changed `holder.id().location().getPath()` to `holder.id().identifier().getPath()`
- **Files modified:** src/main/java/thc/mixin/RecipeManagerMixin.java
- **Verification:** Build passes successfully
- **Committed in:** 50da704 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** API name difference between vanilla versions. No scope change.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Spear removal complete
- All spear acquisition paths blocked (crafting, chest loot, mob drops)
- Ready for Phase 8 (projectile-modifications)

---
*Phase: 07-spear-removal*
*Completed: 2026-01-18*
