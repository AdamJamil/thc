---
phase: 15-threat-system
plan: 04
subsystem: combat
tags: [threat, mob-ai, targeting, goal-system]

# Dependency graph
requires:
  - phase: 15-01
    provides: MOB_THREAT attachment and ThreatManager class
  - phase: 15-02
    provides: threat propagation on damage
  - phase: 15-03
    provides: threat decay method
provides:
  - ThreatTargetGoal AI goal for threat-based mob targeting
  - getHighestThreatTarget method in ThreatManager
  - MonsterThreatGoalMixin for goal injection
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - TargetGoal extension for custom targeting
    - @Shadow for protected field access in mixin
    - Type check pattern for filtered mixin injection

key-files:
  created:
    - src/main/java/thc/threat/ThreatTargetGoal.java
    - src/main/java/thc/mixin/MonsterThreatGoalMixin.java
  modified:
    - src/main/java/thc/threat/ThreatManager.java
    - src/main/resources/thc.mixins.json

key-decisions:
  - "MIN_THREAT = 5.0 - threshold for threat targeting activation"
  - "Priority 0 for ThreatTargetGoal - highest priority to override vanilla targeting"
  - "Mixin targets Mob.registerGoals with Monster type check - efficient filtering"
  - "Lazy decay in canUse/canContinueToUse - ensures fresh threat values"

patterns-established:
  - "TargetGoal extension with Flag.TARGET for targeting AI"
  - "@Shadow @Final for protected GoalSelector access"
  - "Type check in mixin callback for class-specific logic"

# Metrics
duration: 6min
completed: 2026-01-19
---

# Phase 15 Plan 04: Threat Targeting Summary

**ThreatTargetGoal AI goal implementing threat-based mob targeting with revenge switch and strictly-higher switching rules**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-19
- **Completed:** 2026-01-19
- **Tasks:** 4
- **Files created:** 2
- **Files modified:** 2

## Accomplishments
- Added getHighestThreatTarget method to ThreatManager for finding highest-threat valid player
- Created ThreatTargetGoal extending TargetGoal with threat-based targeting logic
- Implemented target switching rules: revenge allows immediate switch, otherwise only strictly higher threat
- Created MonsterThreatGoalMixin to inject ThreatTargetGoal into all Monster mobs
- Registered mixin in thc.mixins.json

## Task Commits

Each task was committed atomically:

1. **Task 1: Add getHighestThreatTarget method** - `2d9db90` (feat)
2. **Task 2: Create ThreatTargetGoal** - `3c567b9` (feat)
3. **Task 3: Create MonsterThreatGoalMixin** - `4418e70` (feat)
4. **Task 4: Register mixin** - `209cbfe` (chore)

## Files Created/Modified
- `src/main/java/thc/threat/ThreatManager.java` - Added getHighestThreatTarget method (38 lines)
- `src/main/java/thc/threat/ThreatTargetGoal.java` - New AI goal (103 lines)
- `src/main/java/thc/mixin/MonsterThreatGoalMixin.java` - New mixin for goal injection (36 lines)
- `src/main/resources/thc.mixins.json` - Added MonsterThreatGoalMixin

## Decisions Made
- Targeting threshold set to 5.0 threat (per THREAT-05 spec)
- checkVisibility = false in TargetGoal - threat overrides line of sight
- Revenge switch checks getLastHurtByMob for immediate target switch (THREAT-06)
- Strictly higher threat required for non-revenge switching (THREAT-06)
- Goal injected at priority 0 to override vanilla targeting (priority 1-2)
- Mixin targets Mob.class with Monster instanceof check to cover all hostile mobs

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed @Nullable annotation**
- **Found during:** Task 1
- **Issue:** javax.annotation.Nullable not available in dependencies
- **Fix:** Removed annotation, method documentation indicates null return possible
- **Files modified:** ThreatManager.java
- **Commit:** 2d9db90

**2. [Rule 3 - Blocking] Changed mixin target from Monster to Mob**
- **Found during:** Task 3
- **Issue:** Monster class doesn't override registerGoals(), so mixin injection would fail
- **Fix:** Target Mob.registerGoals() with Monster instanceof type check
- **Files modified:** MonsterThreatGoalMixin.java
- **Commit:** 4418e70

## Issues Encountered

None beyond the auto-fixed blocking issues.

## User Setup Required

None - no external service configuration required.

## Threat System Completion

Phase 15 is now complete. All THREAT requirements addressed:
- THREAT-01: Per-mob threat map storage (Plan 01)
- THREAT-02: Half-damage threat propagation to 15-block radius (Plan 02)
- THREAT-03: 1/sec decay rate (Plan 03)
- THREAT-04: Arrow bonus (+10 threat) (Plan 02)
- THREAT-05: Targeting threshold (MIN_THREAT = 5.0) (Plan 04)
- THREAT-06: Target switching rules (revenge, strictly higher) (Plan 04)

---
*Phase: 15-threat-system*
*Completed: 2026-01-19*
