---
phase: 15-threat-system
plan: 02
subsystem: combat
tags: [threat, mixin, damage-events, mob-ai]

# Dependency graph
requires:
  - phase: 15-01
    provides: MOB_THREAT attachment, ThreatManager utility class
provides:
  - Threat propagation on player-dealt damage
  - Bonus threat on projectile hits
affects: [15-03-threat-decay, 15-04-threat-targeting]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - TAIL injection for post-damage threat propagation
    - HEAD injection for pre-damage projectile threat bonus
    - MobCategory filtering for hostile/neutral mob selection

key-files:
  created:
    - src/main/java/thc/mixin/MobDamageThreatMixin.java
  modified:
    - src/main/java/thc/mixin/ProjectileEntityMixin.java
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Target LivingEntity.hurtServer with Mob instanceof filter (hurtServer not directly on Mob)"
  - "Include CREATURE category for neutral mobs (wolves, iron golems)"
  - "Arrow bonus (+10) is additive with damage-based threat"

patterns-established:
  - "LivingEntity mixin with Mob filter pattern for mob-only damage hooks"
  - "Static method reference for Predicate in getEntitiesOfClass"

# Metrics
duration: 4min
completed: 2026-01-19
---

# Phase 15 Plan 02: Threat Propagation Summary

**Mixin-based threat propagation system: damage spreads threat to nearby hostile/neutral mobs within 15 blocks, arrows add +10 bonus threat**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-19T20:05:00Z
- **Completed:** 2026-01-19T20:09:00Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Created MobDamageThreatMixin hooking LivingEntity.hurtServer with Mob filter
- Implemented 15-block radius threat propagation for hostile/neutral mobs
- Added +10 bonus threat for projectile hits in ProjectileEntityMixin
- Registered new mixin in thc.mixins.json

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MobDamageThreatMixin** - `78af15b` (feat)
2. **Task 2: Add +10 bonus threat for arrow hits** - `b1e53f2` (feat)
3. **Task 3: Register MobDamageThreatMixin** - `3a79a57` (chore)

## Files Created/Modified
- `src/main/java/thc/mixin/MobDamageThreatMixin.java` - New mixin for damage-based threat propagation
- `src/main/java/thc/mixin/ProjectileEntityMixin.java` - Added ThreatManager.addThreat call for +10 bonus
- `src/main/resources/thc.mixins.json` - Registered MobDamageThreatMixin

## Decisions Made
- Targeted LivingEntity instead of Mob because hurtServer method is inherited (Mob.class has no direct hurtServer)
- Used static method reference for Predicate to avoid lambda issues with @Unique instance methods
- Included MobCategory.CREATURE in addition to MONSTER for neutral mobs like wolves and iron golems

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed mixin target class for hurtServer**
- **Found during:** Task 1
- **Issue:** Plan specified `@Mixin(Mob.class)` but hurtServer method is on LivingEntity, not Mob
- **Fix:** Changed target to `@Mixin(LivingEntity.class)` with `instanceof Mob` filter
- **Files modified:** MobDamageThreatMixin.java
- **Commit:** 78af15b

## Issues Encountered

None beyond the blocking issue documented above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Threat propagation active for all player-dealt damage
- Projectile bonus threat functional
- Ready for decay system (Plan 03) and targeting logic (Plan 04)
- No blockers

---
*Phase: 15-threat-system*
*Completed: 2026-01-19*
