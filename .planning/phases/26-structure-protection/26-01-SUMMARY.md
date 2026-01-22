---
phase: 26
plan: 01
subsystem: world-protection
tags: [village, structure, protection, block-break]

dependency-graph:
  requires: [10-base-building]
  provides: [position-based-village-protection]
  affects: []

tech-stack:
  added: []
  patterns:
    - "Position-based structure detection via getStructureWithPieceAt"

key-files:
  created: []
  modified:
    - src/main/kotlin/thc/world/VillageProtection.kt

decisions:
  - id: position-over-chunk
    choice: "Use getStructureWithPieceAt(pos) instead of isVillageChunk(chunkPos)"
    rationale: "Enables underground mining below villages while protecting actual structures"

metrics:
  duration: 3m
  completed: 2026-01-22
---

# Phase 26 Plan 01: Structure Protection Summary

Position-based village protection using getStructureWithPieceAt for structure bounding box checks instead of chunk-level detection.

## Completed Tasks

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Replace chunk check with position-based structure check | c9bbf1d | VillageProtection.kt |
| 2 | Clean up logging for position-based checks | d6a232e | VillageProtection.kt |

## Changes Made

### VillageProtection.kt Refactoring

**Before:** Checked if block break was in a "village chunk" using ChunkValidator.isVillageChunk()
- Protected ALL blocks in ANY chunk that contained ANY village structure piece
- Blocked underground mining even if 50 blocks below village surface

**After:** Checks if block break position is inside a village structure bounding box
- Uses `structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)`
- Only protects blocks that are actually INSIDE village structure piece bounding boxes
- Allows underground mining below villages

### Code Changes

```kotlin
// OLD: Chunk-based (too aggressive)
val isVillage = ChunkValidator.isVillageChunk(serverLevel, chunkPos)

// NEW: Position-based (precise)
private fun isInsideVillageStructure(level: ServerLevel, pos: BlockPos): Boolean {
    val structureManager = level.structureManager()
    val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
    return structureAt.isValid
}
```

### Preserved Behaviors

- Ore blocks still breakable inside village structures (BREAK-06)
- Allowlist blocks still breakable inside village structures (BREAK-07)
- Village chunks still cannot be claimed (ChunkValidator.isVillageChunk unchanged)

## Decisions Made

### Position-Based Over Chunk-Based Detection

**Decision:** Use `getStructureWithPieceAt(pos)` instead of `isVillageChunk(chunkPos)`

**Rationale:** The chunk-based approach was overly restrictive. A village chunk might extend 64 blocks underground but the village buildings are only at surface level. Players should be able to mine underground while village structures remain protected.

**Impact:** Players can now traverse underground beneath villages without protection restrictions, while village buildings, paths, and foundations remain protected.

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] ./gradlew build succeeds without errors
- [x] VillageProtection.kt uses getStructureWithPieceAt for position checking
- [x] No reference to ChunkValidator.isVillageChunk in VillageProtection.kt
- [x] Ore and allowlist exceptions still present
- [x] StructureTags.VILLAGE import present

## Next Phase Readiness

**Blockers:** None

**Available for:** Phase 27 (Saturation Healing) can proceed independently.
