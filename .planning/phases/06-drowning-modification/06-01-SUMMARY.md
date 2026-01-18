---
phase: 06-drowning-modification
plan: 01
subsystem: mechanics
tags: [drowning, damage, mixin, underwater, exploration]

# Dependency graph
requires:
  - phase: 04-world-restrictions
    provides: mixin patterns and project structure
provides:
  - Drowning damage rate reduced to 4 seconds (from 1 second)
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Counter-based damage interception for rate limiting
    - Server-side damage interception via hurtServer mixin

key-files:
  created:
    - src/main/java/thc/mixin/LivingEntityDrowningMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "hurtServer interception over baseTick modification"
  - "Counter-based skip (3 of 4) rather than timer-based"

patterns-established:
  - "Damage rate modification: Use counter + cancellable inject on hurtServer to skip damage events"
  - "Entity-specific state: Use @Unique instance fields on mixin for per-entity counters"

# Metrics
duration: 4min
completed: 2026-01-18
---

# Phase 6 Plan 1: Drowning Modification Summary

**Counter-based mixin reduces drowning damage rate from 1 to 4 seconds by blocking 3 of 4 damage ticks via hurtServer interception**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-18
- **Completed:** 2026-01-18
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Drowning damage now occurs every 4 seconds instead of every 1 second
- Counter resets when entity surfaces (air supply becomes positive)
- Uses cancellable inject at hurtServer HEAD to block damage
- Maintains separation from existing LivingEntityMixin (buckler combat)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create LivingEntityDrowningMixin** - `00d0202` (feat)
2. **Task 2: Register mixin and verify** - `2eb2bd0` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/LivingEntityDrowningMixin.java` - Mixin with counter-based drowning damage rate reduction
- `src/main/resources/thc.mixins.json` - Registered LivingEntityDrowningMixin

## Decisions Made
- **hurtServer interception:** Chose to intercept damage at hurtServer() rather than modifying baseTick() drowning logic. This is cleaner as it doesn't modify air supply mechanics, just filters damage events.
- **Counter-based approach:** Used a simple counter (block 3, allow 4th) rather than timer-based. Simpler, no need to track game time, resets naturally when entity surfaces.
- **Separate mixin from buckler:** Kept drowning logic in its own mixin class for clean separation of concerns, following existing pattern of one mixin per feature.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Wrong method name in mixin target**
- **Found during:** Task 2 verification (build warning)
- **Issue:** Initial implementation targeted `hurt(DamageSource, float)` which doesn't exist in LivingEntity in Minecraft 1.21.4
- **Fix:** Changed to target `hurtServer(ServerLevel, DamageSource, float)` which is the correct server-side damage method
- **Files modified:** LivingEntityDrowningMixin.java
- **Verification:** Build passes without warnings
- **Committed in:** 2eb2bd0 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Minor fix during verification. No scope creep.

## Issues Encountered
None - mixin pattern well-established in project.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 6 complete (single plan phase)
- Ready for Phase 7 (Spawn Point Expansion) or Phase 8 (Beds)
- Recommend in-game testing to verify 4-second drowning interval

---
*Phase: 06-drowning-modification*
*Completed: 2026-01-18*
