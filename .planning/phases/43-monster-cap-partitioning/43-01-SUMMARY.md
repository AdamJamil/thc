---
phase: 43-monster-cap-partitioning
plan: 01
subsystem: spawn
tags: [monster-cap, regional-caps, threadlocal, spawn-counting]

dependency-graph:
  requires:
    - phase-41-nbt-spawn-tagging
    - phase-42-regional-spawn-system
  provides:
    - regional-cap-enforcement
    - independent-zone-caps
  affects:
    - phase-44-end-spawning

tech-stack:
  added: []
  patterns:
    - threadlocal-per-cycle-state
    - spawn-cycle-lifecycle-hooks

key-files:
  created:
    - src/main/java/thc/spawn/RegionalCapManager.java
  modified:
    - src/main/java/thc/mixin/NaturalSpawnerMixin.java
    - src/main/java/thc/mixin/SpawnReplacementMixin.java

key-decisions:
  - "ThreadLocal for per-cycle counts to ensure thread safety"
  - "Independent caps (21/28/35) with no global cap"
  - "Cap check before distribution roll to avoid wasted computation"

patterns-established:
  - "ThreadLocal for paired injection state (HEAD init, RETURN cleanup)"
  - "Independent regional caps that operate separately"

metrics:
  duration: 5min
  completed: 2026-01-24
---

# Phase 43 Plan 01: Regional Monster Cap Implementation Summary

ThreadLocal-based regional monster caps (Surface:21, Upper Cave:28, Lower Cave:35) with spawn cycle lifecycle hooks that count SPAWN_COUNTED monsters and block spawns when regional cap reached.

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-24T20:05:59Z
- **Completed:** 2026-01-24T20:10:51Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- RegionalCapManager utility with ThreadLocal counting and hard-coded caps
- Spawn cycle lifecycle hooks (HEAD init, RETURN cleanup) in NaturalSpawnerMixin
- Cap check integration in SpawnReplacementMixin before distribution roll

## Task Commits

Each task was committed atomically:

1. **Task 1: Create RegionalCapManager utility** - `97adfe9` (feat)
2. **Task 2: Add spawn cycle hooks to NaturalSpawnerMixin** - `713cdb7` (feat)
3. **Task 3: Integrate cap check into SpawnReplacementMixin** - `fc80b48` (feat)

## Files Created/Modified
- `src/main/java/thc/spawn/RegionalCapManager.java` - ThreadLocal counting and cap enforcement
- `src/main/java/thc/mixin/NaturalSpawnerMixin.java` - Spawn cycle HEAD/RETURN hooks
- `src/main/java/thc/mixin/SpawnReplacementMixin.java` - Cap check before distribution roll

## Decisions Made
- **ThreadLocal storage:** Ensures thread-safe counting per spawn cycle, automatically cleaned up
- **Independent caps:** Each region operates separately - surface at 100% doesn't affect caves
- **Cap check ordering:** Check cap BEFORE distribution roll to avoid rolling for spawns that would be blocked
- **Method signature fix:** Used actual 1.21.11 spawnForChunk signature with List<MobCategory> instead of boolean parameters

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed spawnForChunk method signature**
- **Found during:** Task 2 (NaturalSpawnerMixin hooks)
- **Issue:** Plan suggested boolean parameters (spawnFriendlies, spawnMonsters, spawnMisc) but actual MC 1.21.11 method uses List<MobCategory>
- **Fix:** Updated injection parameters to match actual method signature: `(ServerLevel, LevelChunk, SpawnState, List<MobCategory>)`
- **Files modified:** src/main/java/thc/mixin/NaturalSpawnerMixin.java
- **Verification:** Compiles successfully, injection targets correct method
- **Committed in:** 713cdb7 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Method signature fix necessary for compilation. No scope creep.

## Issues Encountered
None - plan executed successfully after signature fix.

## Key Implementation Details

**Cap Values (hard-coded per user decision):**
- Surface: 21 (30% of 70)
- Upper Cave: 28 (40% of 70)
- Lower Cave: 35 (50% of 70)

**Counting Logic:**
1. HEAD inject on spawnForChunk calls countMobsByRegion()
2. Iterates all entities, filters to MONSTER category mobs
3. Only counts mobs with SPAWN_COUNTED=true attachment
4. Groups by SPAWN_REGION attachment, stores in ThreadLocal
5. RETURN inject calls clearCounts() to prevent memory leaks

**Spawn Blocking:**
- Cap check uses same region detection as distribution roll
- When cap reached, spawn attempt returns early (no fallback)
- Nether/End bypass regional caps (null region returns true)

## Next Phase Readiness

**Phase 44 (End Spawning) requirements met:**
- Regional cap infrastructure complete
- Cap bypass for non-Overworld dimensions already implemented (null region check)
- Pattern established for regional spawn control

---
*Phase: 43-monster-cap-partitioning*
*Completed: 2026-01-24*
