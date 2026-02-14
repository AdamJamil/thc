---
phase: quick-9
plan: 01
subsystem: gameplay
tags: [class-system, commands, refactor, testing-tools]

requires:
  - phase: player-class-system
    provides: ClassManager, PlayerClass enum, class gate checks
provides:
  - "is<Class>(player) API on ClassManager with allClasses override"
  - "/allClasses operator command to toggle override mode"
  - "All boolean class gates refactored to use is<Class>() API"
affects: [class-gates, boon-gates, ability-unlocks]

tech-stack:
  added: []
  patterns: ["is<Class>() boolean gate API pattern for class checks"]

key-files:
  created:
    - src/main/java/thc/playerclass/AllClassesCommand.java
  modified:
    - src/main/java/thc/playerclass/ClassManager.java
    - src/main/kotlin/thc/THC.kt
    - src/main/kotlin/thc/item/BucklerItem.kt
    - src/main/kotlin/thc/bow/BreezeBowItem.kt
    - src/main/kotlin/thc/item/BlazeBowItem.kt
    - src/main/java/thc/mixin/SnowballHitMixin.java
    - src/main/java/thc/mixin/BoatPlacementMixin.java
    - src/main/java/thc/boon/BoonGate.java

key-decisions:
  - "allClasses is a static server-wide toggle, not per-player"
  - "Revival speed uses isSupport() so support class fast-revive works for all when allClasses enabled"
  - "Numeric multiplier paths (melee/ranged damage, health) intentionally NOT routed through is<Class>()"

duration: 5min
completed: 2026-02-14
---

# Quick Task 9: Add /allClasses Override Command with is<Class> API

**is<Class>() boolean gate API on ClassManager with /allClasses operator toggle, all 7 gate call sites refactored**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-14T04:26:45Z
- **Completed:** 2026-02-14T04:32:34Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Added isBastion/isMelee/isRanged/isSupport methods to ClassManager that respect allClasses override
- Created /allClasses operator-only toggle command with actionbar feedback (green ENABLED / red DISABLED)
- Refactored all 7 boolean class gate sites to use the new API
- Numeric multiplier paths (PlayerAttackMixin melee, AbstractArrowMixin ranged) intentionally left unchanged

## Task Commits

Each task was committed atomically:

1. **Task 1: Add is<Class> API and /allClasses command** - `99934fe` (feat)
2. **Task 2: Refactor all boolean class gates to use is<Class> API** - `d3e0e39` (refactor)

## Files Created/Modified
- `src/main/java/thc/playerclass/ClassManager.java` - Added isBastion/isMelee/isRanged/isSupport + allClasses toggle state
- `src/main/java/thc/playerclass/AllClassesCommand.java` - New /allClasses operator command
- `src/main/kotlin/thc/THC.kt` - Registered AllClassesCommand, refactored revival speed gate
- `src/main/kotlin/thc/item/BucklerItem.kt` - ClassManager.isBastion() gate
- `src/main/kotlin/thc/bow/BreezeBowItem.kt` - ClassManager.isSupport() gate
- `src/main/kotlin/thc/item/BlazeBowItem.kt` - ClassManager.isRanged() gate
- `src/main/java/thc/mixin/SnowballHitMixin.java` - ClassManager.isBastion() gate
- `src/main/java/thc/mixin/BoatPlacementMixin.java` - ClassManager.isBastion() gate
- `src/main/java/thc/boon/BoonGate.java` - ClassManager.isBastion() gate

## Decisions Made
- allClasses is a server-wide static toggle, not per-player (simpler, sufficient for testing)
- Revival speed boolean gate refactored to isSupport() -- when allClasses is on, all players get fast revival
- Numeric paths (melee multiplier, ranged multiplier, health bonus) remain on getClass() -- these should always use actual class

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed permission API call for MC 1.21+**
- **Found during:** Task 1 (AllClassesCommand compilation)
- **Issue:** Plan specified `source.hasPermission(2)` but MC 1.21+ uses `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`
- **Fix:** Updated to match AdvanceStageCommand pattern with Permissions import
- **Files modified:** src/main/java/thc/playerclass/AllClassesCommand.java
- **Verification:** Build passes
- **Committed in:** 99934fe (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** API difference between MC versions. No scope creep.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- /allClasses command ready for in-game testing
- All class-gated abilities (buckler, breeze bow, blaze bow, snowball effects, land boat, sweeping edge, fast revival) will unlock for any class when toggle is active

---
*Quick Task: 9*
*Completed: 2026-02-14*
