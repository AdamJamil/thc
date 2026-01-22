---
phase: 27-eating-mechanics
plan: 01
subsystem: gameplay
tags: [mixin, food, eating, saturation, combat]

# Dependency graph
requires:
  - phase: none
    provides: Standalone feature
provides:
  - Extended eating duration (64 ticks / 3.2 seconds)
  - Saturation cap behavior (max of current vs new)
  - FoodDataAccessor for saturation modification
affects: [future food-related mechanics]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ThreadLocal for cross-injection state transfer
    - Paired HEAD/RETURN injections for value comparison

key-files:
  created:
    - src/main/java/thc/mixin/ItemEatingMixin.java
    - src/main/java/thc/mixin/access/FoodDataAccessor.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use CONSUMABLE component check (not FOOD) for eating detection"
  - "ThreadLocal for thread-safe pre/post saturation storage"

patterns-established:
  - "ThreadLocal for state across paired injections: store in HEAD, use in RETURN"
  - "Accessor mixin pattern for private field modification (FoodDataAccessor)"

# Metrics
duration: 4min
completed: 2026-01-22
---

# Phase 27 Plan 01: Eating Mechanics Summary

**Extended eating duration to 64 ticks (3.2s) with saturation cap preserving max(current, food_value)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-22T17:28:25Z
- **Completed:** 2026-01-22T17:32:47Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- All consumable items now take 3.2 seconds to eat (vs vanilla 1.6s)
- Saturation never decreases from eating - preserves maximum
- Thread-safe implementation using ThreadLocal pattern

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ItemEatingMixin with eating duration modification** - `7b30efa` (feat)
2. **Task 2: Add saturation cap behavior to finishUsingItem** - `1f0bf63` (feat)
3. **Task 3: Register mixins and verify** - `3ed7d81` (chore)

## Files Created/Modified

- `src/main/java/thc/mixin/ItemEatingMixin.java` - Eating duration + saturation cap mixins
- `src/main/java/thc/mixin/access/FoodDataAccessor.java` - Accessor for saturation level
- `src/main/resources/thc.mixins.json` - Mixin registration

## Decisions Made

- **CONSUMABLE over FOOD component:** Used CONSUMABLE component check because that's what determines eating behavior. All foods have CONSUMABLE component.
- **ThreadLocal for state transfer:** Used ThreadLocal<Float> to safely pass pre-eating saturation value from HEAD injection to RETURN injection.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Eating mechanics complete and ready for testing
- Build passes with no mixin warnings
- Ready for phase 28 (Custom Items)

---
*Phase: 27-eating-mechanics*
*Completed: 2026-01-22*
