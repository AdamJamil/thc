---
phase: 76-boon-buckler-gate
plan: 01
subsystem: combat
tags: [buckler, bastion, class-gate, stage-gate, boon]

# Dependency graph
requires:
  - phase: 75-rename
    provides: PlayerClass.BASTION enum constant
provides:
  - Buckler usage restricted to Bastion class at Stage 2+
  - Gate pattern for class + stage boon restrictions
affects: [77-parry-sweep, 78-snowball-enhancement]

# Tech tracking
tech-stack:
  added: []
  patterns: [class-stage-gate-in-item-use]

key-files:
  created: []
  modified: [src/main/kotlin/thc/item/BucklerItem.kt]

key-decisions:
  - "Gate check placed before broken/poise checks for cleaner rejection flow"
  - "Single rejection message for all gate failures (class or stage)"

patterns-established:
  - "Class + stage gate: Check ClassManager.getClass() and StageManager.getBoonLevel() in item use()"
  - "Boon level >= 2 pattern for Stage 2+ requirements"

# Metrics
duration: 3min
completed: 2026-02-03
---

# Phase 76 Plan 01: Buckler Gate Summary

**Buckler raise restricted to Bastion class at Stage 2+ using ClassManager and StageManager gate checks**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-03
- **Completed:** 2026-02-03
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Buckler usage now restricted to Bastion class at Stage 2+
- Non-Bastion players see "Your wimpy arms cannot lift the buckler." in red
- Bastion at Stage 1 sees same rejection message
- Gate check happens before broken/poise checks for clean flow
- Existing buckler functionality preserved for authorized players

## Task Commits

Each task was committed atomically:

1. **Task 1: Add class and stage gate to BucklerItem.use()** - `949fbd6` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/item/BucklerItem.kt` - Added class + stage gate checks in use() method

## Decisions Made
- Gate check placed before broken/poise checks so non-Bastion players get class rejection message rather than confusing "buckler broken" state
- Single humorous rejection message ("wimpy arms") used for all gate failures regardless of whether player is wrong class or wrong stage

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Buckler gate complete, ready for Phase 77 (Parry Sweep)
- Pattern established for future class/stage-gated boons
- No blockers

---
*Phase: 76-boon-buckler-gate*
*Completed: 2026-02-03*
