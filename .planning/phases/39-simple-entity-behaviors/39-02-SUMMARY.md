---
phase: 39-simple-entity-behaviors
plan: 02
subsystem: spawning
tags: [phantom, patrol, iron-golem, spawner, mixin, stage-gating, minecraft, fabricmc]

# Dependency graph
requires:
  - phase: 38-spawn-table-replacements
    provides: Spawn modification patterns and mixin approach
provides:
  - Phantom spawning completely disabled
  - Illager patrol spawning gated to stage 2+
  - Player-summoned iron golem creation prevented
  - HEAD cancellation pattern for spawner control
affects: [40-creeper-modifications, future behavior gating]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@Inject HEAD cancellation pattern for spawners"
    - "Stage-conditional spawning via StageManager integration"
    - "Pattern detection and cancellation (iron golem summon)"

key-files:
  created:
    - src/main/java/thc/mixin/PhantomSpawnerMixin.java
    - src/main/java/thc/mixin/PatrolSpawnerMixin.java
    - src/main/java/thc/mixin/CarvedPumpkinBlockMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "HEAD cancellation returning 0 for complete phantom removal (no partial spawning)"
  - "Stage < 2 check for patrol gating (not stage == 1 to future-proof for more stages)"
  - "Iron golem pattern detection + cancel instead of preventing pumpkin placement"
  - "Snow golem summons preserved (different pattern, vanilla continues on non-cancel)"

patterns-established:
  - "Spawner HEAD cancellation for complete spawn removal"
  - "Stage-based spawn gating pattern"
  - "Block pattern detection + cancellation for summon prevention"

# Metrics
duration: 3min
completed: 2026-01-23
---

# Phase 39 Plan 02: Spawner HEAD Cancellations Summary

**Phantoms removed entirely, illager patrols gated to stage 2+, player-summoned iron golems prevented**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-24T03:27:36Z
- **Completed:** 2026-01-24T03:30:48Z
- **Tasks:** 4
- **Files modified:** 4

## Accomplishments
- Phantom natural spawning completely disabled (FR-14)
- Illager patrol spawning gated to stage 2+ (FR-15)
- Player-summoned iron golem creation blocked (FR-17)
- Snow golem summons preserved (different pattern)
- Villager-summoned iron golems preserved (different code path)
- Stage-conditional spawning pattern established for future use

## Task Commits

All tasks completed in single atomic commit:

1. **Tasks 1-4: Implement spawner HEAD cancellations** - `4a97db5` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/PhantomSpawnerMixin.java` - HEAD injection on PhantomSpawner.tick returning 0
- `src/main/java/thc/mixin/PatrolSpawnerMixin.java` - Stage-conditional HEAD injection on PatrolSpawner.tick
- `src/main/java/thc/mixin/CarvedPumpkinBlockMixin.java` - HEAD injection on trySpawnGolem with iron golem pattern detection
- `src/main/resources/thc.mixins.json` - Added all three mixins alphabetically

## Decisions Made

**1. HEAD cancellation returning 0 for phantoms**
- PhantomSpawner.tick returns int count of spawned phantoms
- Returning 0 from HEAD completely prevents spawn checks
- Alternative (TAIL cancellation) would still compute spawn attempts
- Insomnia stat unchanged (players can still see insomnia in debug screen)

**2. Stage < 2 check instead of stage == 1**
- Future-proofs for potential stage 0 or stage 3+ additions
- Patrols resume at exactly stage 2 (evoker kill milestone)
- Consistent with THC stage progression philosophy (incremental unlocking)

**3. Iron golem pattern detection at trySpawnGolem HEAD**
- Checks for iron golem pattern match at HEAD of trySpawnGolem
- If pattern detected, cancel to prevent spawn
- If no pattern, vanilla continues to check snow golem
- Snow golems still summonable (different T-shape using snow blocks)
- Pumpkin block placement succeeds regardless (only spawn is prevented)

**4. Villager-summoned iron golems preserved**
- Villagers spawn golems via different code path (Villager.spawnGolem)
- Village defense mechanics remain intact
- Only player summons blocked (via pumpkin + iron blocks)
- Enforces THC philosophy: risk required for defense (find/protect village)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed isClientSide field to method call**
- **Found during:** Task 4 (build verification)
- **Issue:** CarvedPumpkinBlockMixin used `level.isClientSide` (field access) which is private
- **Fix:** Changed to `level.isClientSide()` (method call) matching Minecraft 1.21.11 API
- **Files modified:** src/main/java/thc/mixin/CarvedPumpkinBlockMixin.java
- **Verification:** Build succeeded
- **Committed in:** 4a97db5 (Task 4 commit)

---

**Total deviations:** 1 auto-fixed (bug - incorrect API usage)
**Impact on plan:** No functional changes to requirements. Documented correct MC 1.21.11 Level API usage.

## Issues Encountered

**Minecraft 1.21.11 Level API**
- Level.isClientSide is a private field in MC 1.21.11
- Correct usage: isClientSide() method call
- Resolution: Fixed to use method, build succeeded

## Next Phase Readiness

**Ready for next phase:**
- All three spawner mixins compiling and registered successfully
- Phantom spawning completely disabled (FR-14 complete)
- Patrol gating to stage 2+ implemented (FR-15 complete)
- Iron golem player-summon prevention implemented (FR-17 complete)
- Pattern established for future stage-gated behaviors

**No blockers:** Build succeeds, all requirements met per must_haves verification criteria

**For future phases:**
- Stage-conditional pattern reusable for other entity behaviors
- HEAD cancellation pattern applicable to other spawners
- Pattern detection + cancellation approach extends to other summons

---
*Phase: 39-simple-entity-behaviors*
*Completed: 2026-01-23*
