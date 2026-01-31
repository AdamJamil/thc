---
phase: 70-trade-cycling
plan: 01
subsystem: villager
tags: [trade-cycling, emerald-interaction, villager-trades, fabric]

# Dependency graph
requires:
  - phase: 68-01
    provides: CustomTradeTables.getTradesFor() for trade regeneration
  - phase: 69-02
    provides: VillagerInteraction framework with 0 XP pass-through
provides:
  - getTradeCount(profession, level) - trade count per profession/level
  - getTradePoolSize(profession, level) - pool size for cycling validation
  - Trade cycling at 0 XP with emerald right-click
  - Earlier level trade preservation during cycling
affects: [71-rail-locator]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pool size checking before emerald consumption"
    - "Trade removal via index calculation (sum of earlier levels)"

key-files:
  created: []
  modified:
    - src/main/java/thc/villager/CustomTradeTables.java
    - src/main/kotlin/thc/villager/VillagerInteraction.kt

key-decisions:
  - "Emerald consumed ONLY after pool size validation passes"
  - "Failure feedback: VILLAGER_NO sound only (no particles)"
  - "Success feedback: HAPPY_VILLAGER particles + VILLAGER_YES sound (consistent with level-up)"

patterns-established:
  - "Trade index calculation: sum getTradeCount(1..currentLevel-1) for start index"
  - "Pool size gates cycling: pool=1 blocks action without consuming resources"

# Metrics
duration: 3min
completed: 2026-01-31
---

# Phase 70 Plan 01: Trade Cycling Summary

**Trade cycling via emerald right-click at 0 XP with pool validation and deterministic earlier-level preservation**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-31T19:09:02Z
- **Completed:** 2026-01-31T19:12:00Z
- **Tasks:** 3 (2 code tasks + 1 verification)
- **Files modified:** 2

## Accomplishments
- Added getTradeCount() returning exact trade counts per profession/level
- Added getTradePoolSize() returning 2 for 50/50 variants, 1 for deterministic
- Implemented trade cycling in VillagerInteraction at 0 XP path
- Pool size validation prevents cycling on deterministic trades (no emerald consumed)
- Earlier level trades preserved via index calculation

## Task Commits

Each task was committed atomically:

1. **Task 1: Add trade count and pool size methods** - `9906926` (feat)
2. **Task 2: Implement trade cycling in VillagerInteraction** - `2a95537` (feat)
3. **Task 3: Verify integration** - (verification only, no commit)

## Files Created/Modified
- `src/main/java/thc/villager/CustomTradeTables.java` - Added getTradeCount() and getTradePoolSize() methods
- `src/main/kotlin/thc/villager/VillagerInteraction.kt` - Added trade cycling logic with handleTradeCycling(), cycleCurrentLevelTrades(), and feedback effect functions

## Decisions Made
- Emerald consumed only after successful pool size validation (prevents loss on deterministic professions)
- Failure feedback uses VILLAGER_NO sound without particles (simple rejection indication)
- Success feedback reuses same pattern as level-up (HAPPY_VILLAGER + VILLAGER_YES)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Trade cycling complete for all 4 allowed professions
- Librarian: cycling always works (all levels have 50/50 variants)
- Mason: cycling works at levels 2-5 (level 1 deterministic, blocked)
- Butcher/Cartographer: cycling blocked at all levels (fully deterministic)
- Ready for Phase 71 (Rail Locator) to complete v2.8

---
*Phase: 70-trade-cycling*
*Completed: 2026-01-31*
