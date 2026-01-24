---
phase: 40-complex-entity-behaviors
plan: 01
subsystem: entity
tags: [ghast, fireball, velocity, fire-rate, fire-spread, mixin, entity-load]

# Dependency graph
requires:
  - phase: 37-monster-speed
    provides: MonsterModifications.kt ENTITY_LOAD pattern
provides:
  - GhastModifications.kt for fireball velocity boost
  - GhastShootFireballGoalMixin for fire rate modification
  - LargeFireballMixin for expanded fire spread
affects: [40-02-enderman-behaviors, in-game-testing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@ModifyConstant for inner class field modification"
    - "ENTITY_LOAD for projectile velocity boost"
    - "TAIL inject on onHit for post-explosion fire placement"

key-files:
  created:
    - src/main/kotlin/thc/monster/GhastModifications.kt
    - src/main/java/thc/mixin/GhastShootFireballGoalMixin.java
    - src/main/java/thc/mixin/LargeFireballMixin.java
  modified:
    - src/main/kotlin/thc/THC.kt
    - src/main/resources/thc.mixins.json
    - gradle.properties

key-decisions:
  - "Target GhastShootFireballGoal inner class instead of RangedAttackGoal (plan was incorrect)"
  - "Use @ModifyConstant to change chargeTime reset from -40 to -60"
  - "Boost ALL LargeFireball velocity (not just ghast-owned) since vanilla only spawns from ghasts"
  - "Disabled kotlin incremental compilation to fix daemon corruption issues"

patterns-established:
  - "Inner class mixin: targets = \"pkg.Outer$InnerClass\" syntax"
  - "@ModifyConstant for single constant value replacement"

# Metrics
duration: 19min
completed: 2026-01-24
---

# Phase 40 Plan 01: Ghast Behavior Modifications Summary

**Ghast fireballs 50% faster, 80-tick fire interval (4 seconds), and doubled fire spread radius (6 blocks)**

## Performance

- **Duration:** 19 min
- **Started:** 2026-01-24T14:56:08Z
- **Completed:** 2026-01-24T15:14:56Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments
- 50% velocity boost on all LargeFireball entities at spawn time
- Fire rate changed from 60 ticks (3s) to 80 ticks (4s) via chargeTime reset modification
- Fire spread expanded from vanilla ~3 blocks to 6 block radius ring with 33% placement chance

## Task Commits

Each task was committed atomically:

1. **Task 1: Ghast fireball velocity boost (FR-07)** - `84b358b` (feat)
2. **Task 2: Ghast fire rate reduction (FR-08)** - `97d4d0f` (feat)
3. **Task 3: Ghast fireball fire spread doubling (FR-09)** - `a3a9a51` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/monster/GhastModifications.kt` - ENTITY_LOAD handler for LargeFireball velocity boost
- `src/main/java/thc/mixin/GhastShootFireballGoalMixin.java` - @ModifyConstant to change chargeTime reset
- `src/main/java/thc/mixin/LargeFireballMixin.java` - TAIL inject on onHit for expanded fire placement
- `src/main/kotlin/thc/THC.kt` - Register GhastModifications
- `src/main/resources/thc.mixins.json` - Register GhastShootFireballGoalMixin and LargeFireballMixin
- `gradle.properties` - Disabled kotlin incremental compilation

## Decisions Made
- **Corrected mixin target:** Plan specified RangedAttackGoal but Ghast uses GhastShootFireballGoal inner class. Used `targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal"` syntax.
- **@ModifyConstant pattern:** Simpler than accessor + goal removal - directly modify the -40 chargeTime reset to -60.
- **Fire spread ring:** Placed fire in 3-6 block ring around impact (skipping inner 3 blocks where vanilla already places fire).
- **All LargeFireballs boosted:** Vanilla only spawns LargeFireball from Ghasts, so boosting all is equivalent and simpler than owner checking.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Corrected LargeFireball import path**
- **Found during:** Task 1 (GhastModifications.kt)
- **Issue:** MC 1.21.11 moved LargeFireball to `projectile.hurtingprojectile` subpackage
- **Fix:** Changed import from `net.minecraft.world.entity.projectile.LargeFireball` to `net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball`
- **Files modified:** src/main/kotlin/thc/monster/GhastModifications.kt
- **Committed in:** 84b358b (Task 1 commit)

**2. [Rule 3 - Blocking] Fixed Kotlin daemon corruption**
- **Found during:** Task 1/2 builds
- **Issue:** Kotlin daemon cache corruption causing build failures
- **Fix:** Disabled kotlin incremental compilation via gradle.properties
- **Files modified:** gradle.properties
- **Committed in:** 97d4d0f (Task 2 commit)

**3. [Rule 4 - Architectural] Changed mixin target from RangedAttackGoal to GhastShootFireballGoal**
- **Found during:** Task 2 planning
- **Issue:** Plan specified RangedAttackGoal but decompilation showed Ghast uses internal GhastShootFireballGoal
- **Decision:** Proceeded with inner class mixin target using @ModifyConstant instead of goal replacement
- **Files modified:** Created GhastShootFireballGoalMixin.java instead of planned GhastRegisterGoalsMixin.java
- **Impact:** Simpler implementation, same outcome

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 architectural)
**Impact on plan:** All deviations necessary for correct implementation. No scope creep.

## Issues Encountered
- Persistent Kotlin daemon cache corruption required multiple build attempts and eventual disabling of incremental compilation
- Plan incorrectly specified RangedAttackGoal - required decompilation analysis to find actual GhastShootFireballGoal inner class

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All three Ghast modifications implemented (FR-07, FR-08, FR-09)
- Ready for plan 02: Enderman behavior modifications
- Ready for in-game testing of Ghast changes

---
*Phase: 40-complex-entity-behaviors*
*Completed: 2026-01-24*
