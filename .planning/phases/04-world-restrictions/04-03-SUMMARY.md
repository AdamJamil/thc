---
phase: 04-world-restrictions
plan: 03
subsystem: gameplay
tags: [minecraft, fabric, village-protection, block-break, player-events]

# Dependency graph
requires:
  - phase: 02-chunk-claiming-core
    provides: ChunkValidator.isVillageChunk() for village detection
  - phase: 04-world-restrictions
    provides: WorldRestrictions.ALLOWED_BLOCKS for exception checking
provides:
  - VillageProtection handler blocking non-ore/allowlist breaks in village chunks
  - Ore exception using BlockTags for all ore types
  - Allowlist exception for utility blocks
affects: [testing, gameplay-balance]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PlayerBlockBreakEvents.BEFORE for break prevention"
    - "BlockTags for ore type detection (COAL_ORES, IRON_ORES, etc.)"

key-files:
  created:
    - src/main/kotlin/thc/world/VillageProtection.kt
  modified:
    - src/main/kotlin/thc/THC.kt

key-decisions:
  - "Ore detection via vanilla BlockTags covers all variants (regular + deepslate)"
  - "Allowlist reused from WorldRestrictions for consistency"
  - "Registration order: VillageProtection before MiningFatigue"

patterns-established:
  - "Handler ordering: protection handlers before penalty handlers"
  - "Block type detection via BlockTags for future ore additions"

# Metrics
duration: 5min
completed: 2026-01-16
---

# Phase 4 Plan 3: Village Protection Summary

**Village block break protection preventing non-ore/allowlist breaking with PlayerBlockBreakEvents.BEFORE handler**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-16T18:43:57Z
- **Completed:** 2026-01-16T18:49:04Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Village chunks protected from arbitrary block breaking (BREAK-05)
- Ores can be broken in villages for resource extraction (BREAK-06)
- Allowlist blocks can be broken for utility management (BREAK-07)
- Handler registered before MiningFatigue to prevent penalty on blocked breaks

## Task Commits

Each task was committed atomically:

1. **Task 1: Create VillageProtection handler with ore/allowlist exceptions** - `4900afe` (feat)
2. **Task 2: Register VillageProtection in mod initializer** - `400fdc4` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/world/VillageProtection.kt` - Village protection handler with ore/allowlist exceptions
- `src/main/kotlin/thc/THC.kt` - Added VillageProtection.register() before MiningFatigue

## Decisions Made
- Used vanilla BlockTags for ore detection (COAL_ORES, IRON_ORES, COPPER_ORES, GOLD_ORES, REDSTONE_ORES, LAPIS_ORES, DIAMOND_ORES, EMERALD_ORES) to cover all variants
- Reused WorldRestrictions.ALLOWED_BLOCKS for consistency between placement and break exceptions
- Registration order matters: VillageProtection returns false before MiningFatigue can apply penalty

## Deviations from Plan

None - plan executed exactly as written. VillageProtection.kt already existed from previous work with correct implementation.

## Issues Encountered
- None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Village protection system ready for in-game testing (BREAK-05 through BREAK-07)
- All Phase 04 plans complete
- Ready for Phase 05: Trading & Economy

---
*Phase: 04-world-restrictions*
*Completed: 2026-01-16*
