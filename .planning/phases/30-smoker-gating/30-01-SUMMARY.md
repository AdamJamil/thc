---
phase: 30-smoker-gating
plan: 01
subsystem: crafting
tags: [recipe-gating, progression, iron-requirement, structure-generation]

dependency-graph:
  requires: [25-furnace-gating]
  provides: [smoker-gating]
  affects: []

tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - src/main/resources/data/minecraft/recipe/smoker.json
  modified:
    - src/main/java/thc/mixin/RecipeManagerMixin.java
    - src/main/java/thc/mixin/StructureTemplateMixin.java

decisions: []

metrics:
  duration: 3 min
  completed: 2026-01-22
---

# Phase 30 Plan 01: Smoker Gating Summary

**Gate smoker behind iron acquisition via recipe removal, custom recipe with iron ingots, and structure filtering.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-22T23:15:41Z
- **Completed:** 2026-01-22T23:18:54Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Vanilla smoker recipe removed from recipe loading
- Custom smoker recipe requiring 2 iron ingots + 4 logs
- Smokers filtered from village structure generation

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove vanilla smoker recipe** - `5dd9cfb` (feat)
2. **Task 2: Create custom smoker recipe with iron** - `29a08f7` (feat)
3. **Task 3: Filter smoker from village structures** - `9debf02` (feat)

## Files Created/Modified

- `src/main/java/thc/mixin/RecipeManagerMixin.java` - Added "smoker" to REMOVED_RECIPE_PATHS set
- `src/main/resources/data/minecraft/recipe/smoker.json` - Custom shaped recipe with iron ingots
- `src/main/java/thc/mixin/StructureTemplateMixin.java` - Added Blocks.SMOKER to FILTERED_STRUCTURE_BLOCKS set

## Decisions Made

None - followed established patterns from Phase 25 (furnace gating) exactly.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Smoker gating complete. The smoker is now gated behind iron acquisition:
- Players cannot craft vanilla smoker (recipe filtered)
- Players cannot find smokers in villages (structure filtered)
- Players must craft smoker using 2 iron ingots + 4 logs

Follows same pattern as furnace gating (Phase 25). No blockers for future phases.

---
*Phase: 30-smoker-gating*
*Completed: 2026-01-22*
