---
phase: 28-exhaustion-healing
plan: 01
subsystem: gameplay
tags: [mixin, food, exhaustion, healing, regeneration]

# Dependency graph
requires:
  - phase: 27
    provides: FoodDataAccessor base, eating mechanics
provides:
  - Custom exhaustion rate (1.21 saturation drain per cycle)
  - Custom healing gate (hunger >= 18)
  - Custom healing rate (3/16 hearts/second)
  - Bypassed naturalRegeneration gamerule
  - Disabled saturation boost
affects: [future healing-related mechanics]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - HEAD cancellation with full reimplementation for method override
    - Accessor interface expansion for private field modification

key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/FoodDataMixin.java
    - src/main/java/thc/mixin/access/FoodDataAccessor.java

key-decisions:
  - "Full tick() override via HEAD cancellation instead of multiple targeted injections"
  - "Remove lastFoodLevel accessors - field removed/renamed in 1.21"
  - "Keep vanilla exhaustion cost (6.0F) for healing"

patterns-established:
  - "HEAD cancellation with ci.cancel() for complete method replacement"
  - "Accessor interface for multiple private fields on same target class"

# Metrics
duration: 5min
completed: 2026-01-22
---

# Phase 28 Plan 01: Exhaustion and Healing Summary

**Complete FoodData.tick() override with 21% faster saturation drain, hunger >= 18 healing gate, and 53-tick healing rate**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-22T19:20:01Z
- **Completed:** 2026-01-22T19:25:26Z
- **Tasks:** 3
- **Files modified:** 2

## Accomplishments

- Saturation drains 21% faster (1.21 per 4.0 exhaustion vs vanilla 1.0)
- Healing requires hunger >= 18 (9 full hunger bars)
- Healing rate: 1 HP every 53 ticks (~2.65 seconds, 3/16 hearts/second)
- naturalRegeneration gamerule completely bypassed
- Saturation boost (rapid healing at full hunger) disabled

## Task Commits

Each task was committed atomically:

1. **Task 1: Expand FoodDataAccessor for tick() override** - `8d0adc8` (feat)
2. **Task 2: Replace FoodDataMixin with full tick() override** - `dc490ee` (feat)
3. **Task 3: Test and verify mechanics** - verification only, no commit

## Files Created/Modified

- `src/main/java/thc/mixin/FoodDataMixin.java` - Complete tick() override replacing old @ModifyArg
- `src/main/java/thc/mixin/access/FoodDataAccessor.java` - Added exhaustionLevel and tickTimer accessors

## Decisions Made

- **Full override vs targeted injections:** Chose HEAD cancellation to have complete control over all tick logic paths rather than chaining multiple redirects/modifies.
- **Remove lastFoodLevel:** Field does not exist in 1.21 FoodData (causes mixin remap warning). Not needed for our implementation since we don't track food level changes for events.
- **Keep vanilla exhaustion cost:** Retained 6.0F exhaustion cost per heal to maintain balance.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed lastFoodLevel accessors**
- **Found during:** Task 1
- **Issue:** `lastFoodLevel` field does not exist in 1.21 FoodData, causing "Cannot remap" warnings
- **Fix:** Removed getLastFoodLevel and setLastFoodLevel accessors from FoodDataAccessor
- **Impact:** None - field was only used for vanilla event tracking which we don't need
- **Files modified:** `src/main/java/thc/mixin/access/FoodDataAccessor.java`

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Manual Verification Points

To verify the mechanics in-game:
1. **Saturation drain:** With F3 debug, watch saturation drop faster than vanilla when sprinting/jumping
2. **Healing gate:** Damage yourself, verify no healing when hunger < 18, healing starts at 18+
3. **Healing rate:** Time the healing - should be noticeably slower than vanilla (~2.65s per HP)
4. **Gamerule bypass:** Set `/gamerule naturalRegeneration false`, verify healing still works
5. **No saturation boost:** At full hunger with saturation, healing should NOT be faster

## Next Phase Readiness

- Exhaustion and healing mechanics complete and ready for testing
- Build passes with no mixin injection warnings
- Ready for phase 29 (Custom Items)

---
*Phase: 28-exhaustion-healing*
*Completed: 2026-01-22*
