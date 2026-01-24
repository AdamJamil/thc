---
phase: 40-complex-entity-behaviors
plan: 02
subsystem: entity-ai
tags: [mixin, enderman, ai-goal, teleportation, aggro]

# Dependency graph
requires:
  - phase: 37-threat-system
    provides: ThreatTargetGoal pattern for custom AI goals
provides:
  - EndermanProximityAggroGoal custom AI goal
  - EnderMan teleport-behind behavior via hurtServer injection
  - EnderManAccessor for invoking private teleport method
affects: [monster-overhaul-testing, balance-tuning]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Invoker accessor for private method access in mixins"
    - "Vec3 directional calculation (getLookAngle, subtract, scale)"
    - "Random chance check via level.random.nextBoolean()"

key-files:
  created:
    - src/main/java/thc/entity/EndermanProximityAggroGoal.java
    - src/main/java/thc/mixin/access/EnderManAccessor.java
  modified:
    - src/main/java/thc/mixin/EndermanMixin.java
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Priority 1 for proximity goal (between ThreatTargetGoal 0 and vanilla 2+)"
  - "Use accessor invoker pattern for private EnderMan.teleport method"
  - "Position behind player using negative look vector scaled by 3 blocks"

patterns-established:
  - "Invoker accessor: @Invoker annotation for calling private methods in mixin targets"
  - "Vec3 behind-position: playerPos.subtract(playerLook.scale(distance))"

# Metrics
duration: 19min
completed: 2026-01-24
---

# Phase 40 Plan 02: Enderman Behavior Modifications Summary

**Proximity-triggered 3-block aggro goal and 50% teleport-behind flanking via EnderManAccessor invoker pattern**

## Performance

- **Duration:** 19 min
- **Started:** 2026-01-24T14:55:45Z
- **Completed:** 2026-01-24T15:14:47Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Enderman aggros on players within 3 blocks regardless of eye contact (FR-11)
- 50% chance to teleport 3 blocks behind player after taking damage (FR-10)
- Created accessor invoker pattern for private method access

## Task Commits

Each task was committed atomically:

1. **Task 1: Enderman proximity aggro goal (FR-11)** - `97444c7` (feat)
2. **Task 2: Enderman teleport-behind on damage (FR-10)** - `97d4d0f` (feat)

Note: Task 2 commit was merged with 40-01 commits by concurrent session.

## Files Created/Modified
- `src/main/java/thc/entity/EndermanProximityAggroGoal.java` - Custom AI goal for 3-block proximity aggro
- `src/main/java/thc/mixin/EndermanMixin.java` - Injects proximity goal and teleport-behind logic
- `src/main/java/thc/mixin/access/EnderManAccessor.java` - Invoker accessor for private teleport method
- `src/main/resources/thc.mixins.json` - Registered EndermanMixin and EnderManAccessor

## Decisions Made
- **Accessor pattern for private methods:** EnderMan.teleport is private, so used @Invoker accessor instead of @Shadow
- **Behind calculation:** Uses player look angle negation scaled by 3.0 to position enderman behind player
- **No fallback teleport:** If behind-position is invalid, vanilla random teleport still occurs naturally

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed LargeFireball import path**
- **Found during:** Task 1 (initial build)
- **Issue:** GhastModifications.kt had outdated import `net.minecraft.world.entity.projectile.LargeFireball`
- **Fix:** Changed to `net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball` for MC 1.21.11
- **Files modified:** src/main/kotlin/thc/monster/GhastModifications.kt
- **Verification:** Build passes
- **Note:** This file was from uncommitted 40-01 work, fix required to unblock compilation

**2. [Rule 3 - Blocking] Used accessor invoker instead of @Shadow for private method**
- **Found during:** Task 2 (teleport-behind implementation)
- **Issue:** EnderMan.teleport(double, double, double) is private, cannot use @Shadow
- **Fix:** Created EnderManAccessor with @Invoker annotation
- **Files modified:** src/main/java/thc/mixin/access/EnderManAccessor.java
- **Verification:** Build passes, accessor allows private method invocation

---

**Total deviations:** 2 auto-fixed (both blocking issues)
**Impact on plan:** Both fixes required for compilation. No scope creep.

## Issues Encountered
- WSL file locking caused intermittent build failures with gradle daemon - resolved by removing build directory manually
- Gradle daemon stopping unexpectedly - retry pattern worked

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Enderman behavior modifications complete
- Phase 40 ready for verification (combined with 40-01 Ghast modifications)
- In-game testing can verify FR-10 and FR-11 functionality

---
*Phase: 40-complex-entity-behaviors*
*Completed: 2026-01-24*
