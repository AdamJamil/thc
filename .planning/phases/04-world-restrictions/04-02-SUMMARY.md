---
phase: 04-world-restrictions
plan: 02
subsystem: gameplay
tags: [minecraft, fabric, mob-effects, mining-fatigue, player-events]

# Dependency graph
requires:
  - phase: 02-chunk-claiming-core
    provides: ClaimManager.isInBase() for base area detection
  - phase: 02-chunk-claiming-core
    provides: ChunkValidator.isVillageChunk() for village detection
provides:
  - MiningFatigue handler applying stacking effects on block break outside base
  - 12-second decay timer per fatigue level
  - Base/village exemption from mining fatigue
affects: [04-world-restrictions, testing]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PlayerBlockBreakEvents.BEFORE for block break interception"
    - "MobEffectInstance with amplifier stacking for progressive debuff"

key-files:
  created:
    - src/main/kotlin/thc/world/MiningFatigue.kt
  modified:
    - src/main/kotlin/thc/THC.kt

key-decisions:
  - "MobEffects.MINING_FATIGUE (1.21 naming, not DIG_SLOWDOWN)"
  - "Remove+reapply effect to reset duration with new amplifier"
  - "12 second = 240 ticks for natural level decay"

patterns-established:
  - "Effect stacking: remove old, apply new with incremented amplifier"
  - "Block break events: check base/village before applying penalties"

# Metrics
duration: 6min
completed: 2026-01-16
---

# Phase 4 Plan 2: Mining Fatigue Summary

**Mining fatigue system with stacking amplifiers and 12-second decay, exempting base areas and villages**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-16T18:25:03Z
- **Completed:** 2026-01-16T18:31:32Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Mining fatigue applied on block break outside base/village
- Fatigue stacks with each block broken (amplifier +1 per break)
- 12-second duration enables natural decay per level
- Base areas and village chunks exempt from fatigue

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MiningFatigue handler with effect application** - `2ce0974` (feat)
2. **Task 2: Register MiningFatigue in mod initializer** - `5033020` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/world/MiningFatigue.kt` - Mining fatigue handler with PlayerBlockBreakEvents listener
- `src/main/kotlin/thc/THC.kt` - Added MiningFatigue.register() call

## Decisions Made
- Used MobEffects.MINING_FATIGUE (1.21 naming convention, DIG_SLOWDOWN was renamed)
- Effect stacking via remove old effect + add new with incremented amplifier
- 12 seconds (240 ticks) provides time for levels to decay naturally when player stops mining

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated MobEffects constant name for 1.21**
- **Found during:** Task 1 (MiningFatigue handler creation)
- **Issue:** Plan referenced MobEffects.DIG_SLOWDOWN which was renamed to MINING_FATIGUE in Minecraft 1.21
- **Fix:** Changed all references to MobEffects.MINING_FATIGUE
- **Files modified:** src/main/kotlin/thc/world/MiningFatigue.kt
- **Verification:** Build compiles successfully
- **Committed in:** 2ce0974 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Naming change necessary for compilation. No scope creep.

## Issues Encountered
- MobEffects.DIG_SLOWDOWN was renamed to MINING_FATIGUE in Minecraft 1.21 - discovered via decompiled class inspection

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Mining fatigue system ready for in-game testing (BREAK-01 through BREAK-04)
- Integration with block placement restrictions complete (WorldRestrictions from 04-01)
- Ready for 04-03: Village protections

---
*Phase: 04-world-restrictions*
*Completed: 2026-01-16*
