---
phase: 08-projectile-combat
plan: 02
subsystem: combat
tags: [projectile, physics, velocity, gravity, mixin]

# Dependency graph
requires:
  - phase: 08-projectile-combat
    plan: 01
    provides: ProjectileEntityMixin base with onHitEntity injection
provides:
  - 20% velocity boost on player projectile launch
  - Quadratic gravity increase after 8 blocks distance
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Projectile shoot interception via shoot TAIL injection
    - Projectile tick interception for per-frame physics modification
    - @Unique fields for spawn position tracking across ticks

key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/ProjectileEntityMixin.java

key-decisions:
  - "TAIL injection on shoot: Apply velocity boost after vanilla sets initial velocity"
  - "@Unique spawn tracking: Record position on first shoot for distance calculation"
  - "Quadratic gravity formula: 0.01 * (extraBlocks/8)^2, capped at 0.1"

patterns-established:
  - "Projectile physics modification: Use shoot TAIL + tick HEAD injections"
  - "Distance tracking: @Unique fields with spawn position recording"

# Metrics
duration: 4min
completed: 2026-01-18
---

# Phase 8 Plan 2: Projectile Physics Summary

**Player projectiles travel 20% faster initially with increasing gravity drop after 8 blocks, making close-range shots powerful but requiring skill at range**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-18
- **Completed:** 2026-01-18
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- Player projectiles launch with 20% velocity boost (1.2x scale on initial delta movement)
- Spawn position tracked via @Unique fields for distance calculation
- After 8 blocks travel distance, quadratic extra gravity applied each tick
- Gravity formula: 0.01 * ((distance - 8) / 8)^2, capped at 0.1 maximum
- Non-player projectiles (skeleton arrows, dispenser projectiles) unaffected
- Close-range effectiveness preserved while requiring aim compensation at range

## Task Commits

Each task was committed atomically:

1. **Task 1: Add velocity boost on projectile shoot** - `af3e7aa` (feat)
2. **Task 2: Add enhanced gravity after 8 blocks** - `5f3d08a` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/ProjectileEntityMixin.java` - Extended with shoot and tick injections

## Decisions Made
- **TAIL injection for shoot:** Inject after vanilla sets velocity so we can scale the final result by 1.2x.
- **HEAD injection for tick:** Apply gravity modification before vanilla tick processes movement.
- **Quadratic gravity curve:** Provides smooth transition - at 8-16 blocks distance only +0.01 extra/tick, at 16-24 blocks +0.04 extra/tick, capping at +0.1 to prevent extreme drop.
- **3D distance calculation:** Uses full sqrt(dx^2 + dy^2 + dz^2) to account for vertical shooting angles.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None - straightforward extension of existing ProjectileEntityMixin.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 8 Plan 2 complete
- Full projectile combat system now implemented:
  - Plan 1: Hit effects (Speed II, Glowing, aggro redirect)
  - Plan 2: Physics (velocity boost, quadratic gravity)
- Recommend in-game testing: shoot arrows at varying distances and observe:
  - Close shots (< 8 blocks): Fast and accurate
  - Medium shots (8-16 blocks): Noticeable drop
  - Long shots (> 16 blocks): Significant arc requiring aim compensation

---
*Phase: 08-projectile-combat*
*Completed: 2026-01-18*
