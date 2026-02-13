---
phase: 86-wooden-bow-foundation
plan: 01
subsystem: combat
tags: [bow, arrow-physics, drag, projectile, mixin]

# Dependency graph
requires: []
provides:
  - "Wooden Bow identity (display name + sticks+string recipe)"
  - "BowType enum with per-bow drag factors (WOODEN/BLAZE/BREEZE)"
  - "Horizontal drag physics on player arrows replacing gravity-over-time"
  - "Bow type tagging infrastructure on ProjectileEntityMixin"
  - "Public accessor thc$getBowTypeTag() for downstream damage logic"
affects: [87-blaze-bow, 88-breeze-bow]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Per-bow drag factor system via BowType enum"
    - "Bow type tagging on projectile shoot for downstream logic"
    - "Horizontal-only drag leaving vertical velocity untouched"

key-files:
  created:
    - src/main/kotlin/thc/bow/BowType.kt
  modified:
    - src/main/resources/assets/thc/lang/en_us.json
    - src/main/resources/data/minecraft/recipe/bow.json
    - src/main/java/thc/mixin/ProjectileEntityMixin.java

key-decisions:
  - "BowType stored as @Unique fields on ProjectileEntityMixin rather than NBT/components"
  - "Drag applied multiplicatively per tick to horizontal velocity only"

patterns-established:
  - "BowType.fromBowItem() pattern for mapping items to bow types"
  - "thc$getBowTypeTag() accessor pattern for cross-mixin bow type lookup"

# Metrics
duration: 5min
completed: 2026-02-13
---

# Phase 86 Plan 01: Wooden Bow Foundation Summary

**Vanilla bow renamed to "Wooden Bow" with sticks+string recipe, gravity-over-time replaced with per-bow horizontal drag physics, bow type tagging infrastructure on projectile shoot**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-13T02:23:15Z
- **Completed:** 2026-02-13T02:28:39Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Vanilla bow displays as "Wooden Bow" in inventory and tooltips via lang override
- Bow recipe changed from breeze rods + string to sticks + string (making bows craftable early)
- BowType enum created with WOODEN (0.015), BLAZE (0.015), BREEZE (0.01) drag factors
- Old gravity-over-time system fully removed (spawn position tracking, distance-based gravity, 20% velocity boost)
- New horizontal drag system applies per-tick drag coefficient to x/z velocity, vertical untouched
- Bow type tag and drag factor stored on projectile at shoot time for downstream use

## Task Commits

Each task was committed atomically:

1. **Task 1: Rename bow, change recipe, create BowType** - `659d6e6` (feat)
2. **Task 2: Replace gravity-over-time with horizontal drag** - `95c1b73` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/bow/BowType.kt` - Enum with WOODEN/BLAZE/BREEZE values, drag factors, and lookup methods
- `src/main/resources/assets/thc/lang/en_us.json` - Added "Wooden Bow" display name override
- `src/main/resources/data/minecraft/recipe/bow.json` - Changed recipe from breeze rods to sticks
- `src/main/java/thc/mixin/ProjectileEntityMixin.java` - Removed old gravity physics, added horizontal drag and bow type tagging

## Decisions Made
- BowType stored as @Unique fields on ProjectileEntityMixin (tag string + drag factor double) rather than using NBT or custom components -- simpler, no serialization needed since it's runtime-only
- Drag applied multiplicatively each tick: `max(0.8, 1.0 - dragFactor * ticksInFlight)` -- floor of 0.8 prevents arrows from stopping mid-air
- Bow type determined from player.getUseItem() at shoot time (the active use item is the bow while drawing)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- BowType infrastructure ready for phases 87 (Blaze Bow) and 88 (Breeze Bow) to add their item mappings
- thc$getBowTypeTag() accessor ready for Plan 02's AbstractArrowMixin damage multiplier
- Drag factor system automatically applies correct drag per bow type

## Self-Check: PASSED

All 5 files verified present. Both task commits (659d6e6, 95c1b73) verified in git log.

---
*Phase: 86-wooden-bow-foundation*
*Completed: 2026-02-13*
