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

Healing occurs every 5 ticks (4x per second) with tier-based heal amounts:
- T5: saturation >= 6.36 → 0.5 HP/tick = +1 heart/s
- T4: saturation >= 2.73 → 0.25 HP/tick = +0.5 heart/s
- T3: saturation >= 1.36 → 0.09375 HP/tick = +3/16 heart/s
- T2: saturation >= 0.45 → 0.0625 HP/tick = +1/8 heart/s
- T1: saturation < 0.45 → 0.03125 HP/tick = +1/16 heart/s

Formula: hearts/s × 2 HP/heart ÷ 4 ticks/s = HP/tick
- Hunger >= 18 requirement preserved from phase 28
- Updated javadoc to document tier system

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement saturation tier healing calculation** - `699c84d` (feat)
2. **Task 2: Verify tier boundaries** - verification only, no commit

## Files Created/Modified

- `src/main/java/thc/mixin/FoodDataMixin.java` - Added saturation tier calculation in healing block

## Decisions Made

- **Fixed interval, variable amount:** Heals every 5 ticks with variable HP amount per tier, rather than variable intervals with fixed HP. Simpler math and smoother healing in-game.
- **Descending threshold checks:** Check highest tier first (6.36+) for clean if-else chain without complex range conditions.
- **Base rate as T3:** The base rate is a middle tier, not the floor. Lower saturation means SLOWER healing, creating a penalty for poor food management.

## Deviations from Plan

Implementation approach changed from variable tick intervals to fixed 5-tick interval with variable heal amounts. Same effective rates, cleaner implementation.

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
5. At low saturation (<0.45), healing should be noticeably slow (only 0.03125 HP per 5 ticks)

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
