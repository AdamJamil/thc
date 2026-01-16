---
phase: 02-chunk-claiming-core
plan: 02
subsystem: validation
tags: [minecraft, fabric, heightmap, structures, validation, claims]

# Dependency graph
requires:
  - phase: 02-01
    provides: "ClaimData and ClaimManager for claim storage"
provides:
  - "ChunkValidator.validateTerrain for terrain flatness check"
  - "ChunkValidator.isVillageChunk for village detection"
  - "ValidationResult sealed class for type-safe results"
affects: [land-plot-use, claim-creation]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Heightmap.Types.WORLD_SURFACE for accurate surface detection"
    - "StructureTags.VILLAGE for multi-type village detection"
    - "Sealed class for exhaustive result handling"

key-files:
  created:
    - src/main/kotlin/thc/claim/ChunkValidator.kt
  modified: []

key-decisions:
  - "Use StructureTags.VILLAGE instead of checking each village type individually"
  - "Sample 9 positions (3x3) at 7 Y levels for reliable structure detection"
  - "Return lowestSurfaceY in Success for base floor calculation"
  - "MAX_HEIGHT_DIFFERENCE = 10 as constant for terrain flatness"

patterns-established:
  - "ValidationResult sealed class pattern for typed success/failure"
  - "getStructureWithPieceAt with TagKey for structure detection"
  - "Heightmap API for surface Y detection"

# Metrics
duration: 8min
completed: 2026-01-16
---

# Phase 02 Plan 02: Chunk Validation Summary

**Terrain flatness validation and village detection for chunk claiming eligibility using Heightmap and StructureManager APIs**

## Performance

- **Duration:** 8 min
- **Started:** 2026-01-16T05:08:00Z
- **Completed:** 2026-01-16T05:16:00Z
- **Tasks:** 2
- **Files created:** 1

## Accomplishments
- ChunkValidator.validateTerrain checks all 256 surface positions in chunk
- Enforces max 10 block Y difference for terrain flatness (CLAIM-02)
- Returns lowestSurfaceY for base floor calculation (CLAIM-07)
- ChunkValidator.isVillageChunk detects all village types via StructureTags.VILLAGE (CLAIM-08)
- Samples 63 positions (9 x 7 Y levels) for reliable village detection
- ValidationResult sealed class enables exhaustive pattern matching

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ChunkValidator with terrain flatness check** - `51b8f33` (feat)
2. **Task 2: Add village detection to ChunkValidator** - `0018c14` (feat)

## Files Created

- `src/main/kotlin/thc/claim/ChunkValidator.kt` - Validation object with:
  - `validateTerrain(level, chunkPos): ValidationResult` - terrain flatness check
  - `isVillageChunk(level, chunkPos): Boolean` - village structure detection
  - Uses Heightmap.Types.WORLD_SURFACE for accurate surface Y
  - Uses StructureTags.VILLAGE with getStructureWithPieceAt API
  - MAX_HEIGHT_DIFFERENCE = 10 constant

- `ValidationResult` sealed class:
  - `Success(lowestSurfaceY: Int)` - validation passed with data
  - `Failure(reason: String)` - validation failed with message

## Decisions Made

- **StructureTags.VILLAGE over individual structure checks**: Single tag covers all village types (plains, desert, savanna, snowy, taiga) without maintenance burden
- **Multi-position sampling for villages**: Villages can span multiple chunks, so sampling 9 positions at various Y levels ensures reliable detection
- **Heightmap.Types.WORLD_SURFACE**: Returns first air block Y, subtract 1 for actual surface - standard Minecraft pattern
- **Sealed class for results**: Enables exhaustive when() expressions and type-safe error handling

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - APIs worked as expected after researching Mojang mappings.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Validation layer complete:
- Can validate terrain flatness with correct algorithm
- Can detect village chunks for claiming restrictions
- ValidationResult provides lowestSurfaceY for base floor calculation
- Ready for land plot USE handler to consume validation

**Blockers:** None

**Concerns:** Village detection sampling may miss edge cases where village piece is entirely outside sampled positions. Consider more comprehensive sampling if issues arise in testing.

---
*Phase: 02-chunk-claiming-core*
*Completed: 2026-01-16*
