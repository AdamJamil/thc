---
phase: 02-chunk-claiming-core
plan: 01
subsystem: data-persistence
tags: [minecraft, fabric, saveddata, codec, claims, persistence]

# Dependency graph
requires:
  - phase: 01-02
    provides: "Bell interaction drops land plot books"
provides:
  - "ClaimData PersistentState for world-level claim storage"
  - "ClaimManager API for claim query and mutation"
  - "Base floor Y tracking per claimed chunk"
affects: [base-permissions, world-restrictions, adjacent-claiming]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "SavedDataType with Codec for automatic serialization"
    - "Singleton object manager with lazy state access"

key-files:
  created:
    - src/main/kotlin/thc/claim/ClaimData.kt
    - src/main/kotlin/thc/claim/ClaimManager.kt
  modified: []

key-decisions:
  - "Use SavedDataType with Codec (1.21.11 pattern) not legacy Factory"
  - "Store chunks as Long keys via ChunkPos.toLong()"
  - "Base floor Y stored in separate map for efficient lookup"
  - "DataFixTypes.LEVEL used for data fix compatibility"

patterns-established:
  - "SavedData with Codec-based serialization for world state"
  - "ClaimManager singleton mirrors BellState/BucklerState patterns"
  - "Overworld-scoped persistent data via Level.OVERWORLD"

# Metrics
duration: 5min
completed: 2026-01-16
---

# Phase 02 Plan 01: Claim Storage and Query API Summary

**Persistent storage layer for claimed chunks with SavedData/Codec pattern, plus ClaimManager singleton API**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-16T05:01:28Z
- **Completed:** 2026-01-16T05:06:19Z
- **Tasks:** 2
- **Files created:** 2

## Accomplishments
- ClaimData extends SavedData with modern Codec-based serialization
- Claimed chunks stored as Set<Long> for efficient membership testing
- Base floor Y stored per chunk for future permission boundary checks
- ClaimManager provides complete CRUD API (isClaimed, addClaim, getBaseFloorY, isInBase)
- Data persists across server restarts via overworld dataStorage

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ClaimData PersistentState with codec** - `80c6ca7` (feat)
2. **Task 2: Create ClaimManager singleton with claim API** - `f4b0c7a` (feat)

## Files Created

- `src/main/kotlin/thc/claim/ClaimData.kt` - SavedData subclass with:
  - `claimedChunks: MutableSet<Long>` - claimed chunk keys
  - `baseFloors: MutableMap<Long, Int>` - base floor Y per chunk
  - `CODEC` for automatic serialization via RecordCodecBuilder
  - `TYPE: SavedDataType<ClaimData>` for dataStorage registration
  - `getServerState(server)` static accessor for overworld data

- `src/main/kotlin/thc/claim/ClaimManager.kt` - Singleton object with:
  - `isClaimed(server, chunkPos)` - check if chunk is claimed
  - `addClaim(server, chunkPos, baseFloorY)` - claim a chunk
  - `getBaseFloorY(server, chunkPos)` - get base floor Y or null
  - `isInBase(server, pos)` - check if block position is in base area

## Decisions Made

- Used modern SavedDataType(id, supplier, codec, dataFixType) constructor instead of legacy Factory pattern
- Chose DataFixTypes.LEVEL for data fix type (appropriate for world-scoped custom data)
- Stored chunks as Long via ChunkPos.toLong() for compact storage and fast lookup
- Base floor Y stored separately from claimed set to support efficient Y-level queries
- ClaimManager is stateless - all state access through ClaimData.getServerState()

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated to 1.21.11 SavedData API**
- **Found during:** Task 1
- **Issue:** Plan specified older PersistentState/Factory pattern that doesn't exist in 1.21.11
- **Fix:** Used SavedDataType with Codec parameter instead of Factory with BiFunction
- **Files modified:** ClaimData.kt
- **Commit:** 80c6ca7

## Issues Encountered

Initial implementation used outdated API patterns. After analyzing Mojang mappings, discovered:
- `SavedData` no longer has `save()` method override in 1.21.11
- `SavedDataType` takes `(String, Supplier, Codec, DataFixTypes)` not a Factory
- Codec handles both serialization and deserialization automatically

Resolved by referencing mappings.tiny and adapting to modern API.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Claim storage foundation complete:
- Can persist which chunks are claimed
- Can query if chunk/position is claimed/in-base
- Can store base floor Y for permission boundaries
- Ready for land plot consumption and claim creation logic

**Blockers:** None

**Concerns:** None - manual testing in-game will verify persistence works correctly

---
*Phase: 02-chunk-claiming-core*
*Completed: 2026-01-16*
