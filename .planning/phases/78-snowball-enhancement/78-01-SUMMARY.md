---
phase: 78-snowball-enhancement
plan: 01
subsystem: combat
tags: [snowball, slowness, knockback, projectile, mixin, boon]

# Dependency graph
requires:
  - phase: 75-class-rename-buckler-gate
    provides: "Bastion class enum (PlayerClass.BASTION)"
  - phase: 22-class-system
    provides: "ClassManager.getClass() for class check"
  - phase: 23-stage-system
    provides: "StageManager.getBoonLevel() for stage check"
provides:
  - "SnowballHitMixin for enhanced snowball effects"
  - "Bastion Stage 4+ snowball crowd-control utility"
  - "AoE slowness pattern on projectile hit"
affects:
  - "79: Boat mastery (final boon phase)"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Projectile-specific mixin (Snowball extends ThrowableItemProjectile)"
    - "Effect + knockback combo on entity hit"

key-files:
  created:
    - "src/main/java/thc/mixin/SnowballHitMixin.java"
  modified:
    - "src/main/resources/thc.mixins.json"

key-decisions:
  - "Target Snowball.class directly since it overrides onHitEntity without super call"
  - "Use hostile mob filter: MobCategory.MONSTER + getTarget() instanceof Player"
  - "Apply effects independently (slowness and knockback not bundled)"

patterns-established:
  - "Throwable projectile mixin: target specific class not Projectile.class"
  - "Hostile mob filter: category check + targeting check for combat-engaged mobs"

# Metrics
duration: 8min
completed: 2026-02-03
---

# Phase 78 Plan 01: Snowball Enhancement Summary

**Snowball hit mixin with Slowness III (2s) AoE and knockback for Bastion class at Stage 4+**

## Performance

- **Duration:** 8 min
- **Started:** 2026-02-03T09:15:00Z
- **Completed:** 2026-02-03T09:23:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Created SnowballHitMixin targeting Snowball.onHitEntity()
- Implemented Bastion + Stage 4+ boon gate for enhanced effects
- Applied Slowness III (40 ticks) to target and 1.5 block AoE
- Applied directional knockback (0.4 strength) away from thrower
- Only affects hostile mobs targeting players (SNOW-05 compliance)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create SnowballHitMixin with enhanced effects** - `c6aaff6` (feat)
2. **Task 2: Register mixin and verify build** - `8485f5a` (chore)

## Files Created/Modified

- `src/main/java/thc/mixin/SnowballHitMixin.java` - Snowball hit enhancement mixin with boon-gated slowness and knockback
- `src/main/resources/thc.mixins.json` - Added SnowballHitMixin registration

## Decisions Made

- **Target Snowball.class directly:** Snowball overrides Projectile.onHitEntity() without calling super, so ProjectileEntityMixin doesn't intercept snowball hits
- **Compound hostile mob filter:** Uses `MobCategory.MONSTER` AND `getTarget() instanceof Player` to only affect hostile mobs actively engaged with players (per CONTEXT.md)
- **Independent effects:** Slowness and knockback applied separately - if a mob is immune to one, the other can still apply

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed incorrect Snowball import path**
- **Found during:** Task 1 (initial compilation)
- **Issue:** Plan specified `net.minecraft.world.entity.projectile.Snowball` but MC 1.21.11 has it in `throwableitemprojectile` subpackage
- **Fix:** Changed import to `net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball`
- **Files modified:** src/main/java/thc/mixin/SnowballHitMixin.java
- **Verification:** Build passes
- **Committed in:** c6aaff6 (Task 1 commit)

**2. [Rule 3 - Blocking] Fixed incorrect MobEffects constant name**
- **Found during:** Task 1 (initial compilation)
- **Issue:** Plan specified `MobEffects.MOVEMENT_SLOWDOWN` but MC uses `MobEffects.SLOWNESS`
- **Fix:** Changed to `MobEffects.SLOWNESS` (matching existing LivingEntityMixin pattern)
- **Files modified:** src/main/java/thc/mixin/SnowballHitMixin.java
- **Verification:** Build passes
- **Committed in:** c6aaff6 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both auto-fixes were necessary for compilation. No scope creep - just correcting outdated/incorrect research.

## Issues Encountered

None beyond the blocking issues documented above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 78 complete - snowball enhancement boon implemented
- Ready for Phase 79 (Boat Mastery - final Bastion boon)
- Requirements satisfied: SNOW-01, SNOW-02, SNOW-03, SNOW-05

---
*Phase: 78-snowball-enhancement*
*Completed: 2026-02-03*
