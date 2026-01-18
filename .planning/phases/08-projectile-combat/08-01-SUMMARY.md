---
phase: 08-projectile-combat
plan: 01
subsystem: combat
tags: [projectile, effects, aggro, mixin, combat-risk]

# Dependency graph
requires:
  - phase: 04-world-restrictions
    provides: mixin patterns and project structure
provides:
  - Projectile hit applies Speed II + Glowing + aggro redirect to target mob
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Projectile hit interception via onHitEntity mixin
    - Effect application to target entity with player attribution

key-files:
  created:
    - src/main/java/thc/mixin/ProjectileEntityMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Inject at HEAD of onHitEntity for effect application before damage"
  - "Player attribution on effects for proper game mechanics"

patterns-established:
  - "Projectile hit modification: Use Projectile.onHitEntity inject with owner check"
  - "Effect application: Pass player as source to addEffect for proper attribution"

# Metrics
duration: 3min
completed: 2026-01-18
---

# Phase 8 Plan 1: Projectile Combat Summary

**Mixin applies Speed II, Glowing, and aggro redirect to mobs hit by player projectiles, making ranged attacks create visible fast-moving threats**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-18
- **Completed:** 2026-01-18
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Projectile hits from players now apply Speed II (6 seconds) to target mobs
- Projectile hits from players now apply Glowing (6 seconds) to target mobs
- Mob aggro automatically redirects to the shooter
- Effects only apply when projectile owner is a ServerPlayer
- Effects only apply when target is a LivingEntity

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ProjectileEntityMixin with hit effects** - `f0fc659` (feat)
2. **Task 2: Register mixin and verify** - `540a9cd` (chore)

## Files Created/Modified
- `src/main/java/thc/mixin/ProjectileEntityMixin.java` - Mixin with onHitEntity injection applying combat effects
- `src/main/resources/thc.mixins.json` - Registered ProjectileEntityMixin

## Decisions Made
- **HEAD injection:** Inject at HEAD of onHitEntity to apply effects before the vanilla damage handling occurs. This ensures effects are applied even if the mob dies from the hit.
- **Player attribution:** Pass player as the source parameter to addEffect() for proper game mechanic attribution.
- **MobEffects.SPEED:** Used SPEED instead of MOVEMENT_SPEED as the constant was renamed in Minecraft 1.21.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Wrong method name for mixin target**
- **Found during:** Task 1 build verification
- **Issue:** Plan specified `onEntityHit` but Minecraft 1.21 uses `onHitEntity`
- **Fix:** Changed method target to `onHitEntity`
- **Files modified:** ProjectileEntityMixin.java
- **Commit:** f0fc659

**2. [Rule 1 - Bug] Wrong constant name for Speed effect**
- **Found during:** Task 1 build
- **Issue:** `MobEffects.MOVEMENT_SPEED` does not exist in Minecraft 1.21, renamed to `SPEED`
- **Fix:** Changed to `MobEffects.SPEED`
- **Files modified:** ProjectileEntityMixin.java
- **Commit:** f0fc659

---

**Total deviations:** 2 auto-fixed (2 naming discrepancies due to Minecraft version)
**Impact on plan:** Minor naming corrections. No scope change.

## Issues Encountered
None - straightforward mixin implementation following established patterns.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 8 complete (single plan phase)
- Recommend in-game testing: shoot a mob with bow/arrow and verify:
  - Mob gains Speed II effect (visible as speed particles)
  - Mob gains Glowing effect (visible through walls)
  - Mob immediately targets and chases the player

---
*Phase: 08-projectile-combat*
*Completed: 2026-01-18*
