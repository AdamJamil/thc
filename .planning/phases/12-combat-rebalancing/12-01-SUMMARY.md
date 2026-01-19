---
phase: 12-combat-rebalancing
plan: 01
subsystem: combat
tags: [projectile, effects, knockback, mixin, arrow]

# Dependency graph
requires:
  - phase: 08-projectile-combat
    provides: ProjectileEntityMixin with onHitEntity injection pattern
provides:
  - Arrow hits apply Speed IV (amplifier 3) to target mobs
  - Arrow hits on monster-category mobs have knockback removed
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - TAIL injection for post-processing vanilla hit behavior
    - MobCategory filtering for monster-only effects

key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/ProjectileEntityMixin.java

key-decisions:
  - "TAIL injection for knockback removal to run after vanilla applies knockback"
  - "MobCategory.MONSTER filter to only affect hostile mobs, not animals/villagers"
  - "Preserve Y velocity during knockback reset for natural gravity behavior"

patterns-established:
  - "Post-hit modification: Use TAIL injection on onHitEntity to modify after vanilla processing"
  - "Velocity reset with hurtMarked: Set velocity then hurtMarked=true for client sync"

# Metrics
duration: 6min
completed: 2026-01-19
---

# Phase 12 Plan 1: Arrow Combat Rebalancing Summary

**Arrow hits now apply Speed IV and remove knockback from monsters, making ranged combat create faster non-staggerable threats**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-19
- **Completed:** 2026-01-19
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- Increased Speed effect from level II (amplifier 1) to level IV (amplifier 3)
- Added TAIL injection to remove horizontal knockback from arrow-hit monster mobs
- Preserved vertical velocity so mobs still fall naturally
- Used hurtMarked flag to sync velocity changes to clients

## Task Commits

Each task was committed atomically:

1. **Task 1: Increase Speed effect amplifier from 1 to 3** - `311d07d` (feat)
2. **Task 2: Remove knockback from arrow hits on enemy mobs** - `5ade865` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/ProjectileEntityMixin.java` - Added MobCategory import, increased Speed amplifier to 3, added thc$removeArrowKnockback TAIL injection

## Decisions Made
- **TAIL injection for knockback removal:** Using TAIL instead of modifying during HEAD ensures we run after vanilla's knockback application, allowing a clean reset.
- **MobCategory.MONSTER filter:** Only affects hostile mobs - animals, villagers, and other neutral mobs still receive normal knockback.
- **Preserve Y velocity:** Resetting only X/Z while keeping Y allows natural falling/jumping behavior.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- **Gradle build cache corruption:** WSL + Kotlin daemon caused transient build failures due to file locking issues. Resolved by stopping daemon and retrying.
- **Git stash conflict:** Uncommitted changes from a separate session (12-02 work) caused stash/pop conflicts during troubleshooting. Resolved by dropping stash and reapplying changes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Arrow combat rebalancing complete
- Speed IV + no knockback makes arrow-hit mobs significantly more threatening
- Recommend in-game testing: shoot hostile mobs and verify they gain Speed IV without knockback

---
*Phase: 12-combat-rebalancing*
*Completed: 2026-01-19*
