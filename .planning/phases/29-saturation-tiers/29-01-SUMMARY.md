---
phase: 29-saturation-tiers
plan: 01
subsystem: gameplay
tags: [mixin, food, saturation, healing, tiers]

# Dependency graph
requires:
  - phase: 28
    provides: FoodDataMixin tick() override, healing gate, exhaustion mechanics
provides:
  - Saturation-tiered healing rate calculation
  - T5 high saturation fast healing
  - T1 low saturation slow healing
  - Skill-based food management rewards
affects: [future food/healing balancing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Saturation-based tier calculation with descending threshold checks

key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/FoodDataMixin.java

key-decisions:
  - "Descending threshold checks (highest tier first) for cleaner if-else chain"
  - "Base rate (53 ticks) becomes T3, not floor - lower saturation gives SLOWER healing"

patterns-established:
  - "getSaturationLevel() tier mapping for variable healing rates"

# Metrics
duration: 3min
completed: 2026-01-22
---

# Phase 29 Plan 01: Saturation Tiers Summary

**Saturation-tiered healing rates: high saturation rewards faster recovery, low saturation penalizes with slower healing**

## Performance

- **Duration:** 3 min
- **Tasks:** 2 (1 implementation, 1 verification)
- **Files modified:** 1

## Accomplishments

- T5: saturation >= 6.36 heals at 20 ticks/HP (+1 heart/s)
- T4: saturation >= 2.73 heals at 40 ticks/HP (+0.5 heart/s)
- T3: saturation >= 1.36 heals at 53 ticks/HP (+3/16 heart/s, base rate)
- T2: saturation >= 0.45 heals at 80 ticks/HP (+1/8 heart/s)
- T1: saturation < 0.45 heals at 160 ticks/HP (+1/16 heart/s)
- Hunger >= 18 requirement preserved from phase 28
- Updated javadoc to document tier system

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement saturation tier healing calculation** - `699c84d` (feat)
2. **Task 2: Verify tier boundaries** - verification only, no commit

## Files Created/Modified

- `src/main/java/thc/mixin/FoodDataMixin.java` - Added saturation tier calculation in healing block

## Decisions Made

- **Descending threshold checks:** Check highest tier first (6.36+) for clean if-else chain without complex range conditions.
- **Base rate as T3:** The vanilla-equivalent base rate (53 ticks) is now a middle tier, not the floor. Lower saturation means SLOWER healing, creating a penalty for poor food management.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Manual Verification Points

To verify the mechanics in-game:
1. Enable F3 debug overlay to see saturation level
2. Give player damage: `/damage @s 10 minecraft:generic`
3. Eat golden carrot (high saturation) - should heal fast at T5 rate
4. Watch saturation drain and observe healing rate slow through T4, T3, T2, T1
5. At low saturation (<0.45), healing should be noticeably slow (160 tick interval)

Test commands:
```
/effect give @s minecraft:saturation 1 5 true  # Boost saturation
/effect clear @s minecraft:saturation
```

## Next Phase Readiness

- Saturation tier healing complete
- Build passes with no mixin injection warnings
- Phase 29 complete

---
*Phase: 29-saturation-tiers*
*Completed: 2026-01-22*
