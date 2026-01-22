---
phase: 26-structure-protection
verified: 2026-01-22T12:30:00Z
status: passed
score: 4/4 must-haves verified
---

# Phase 26: Structure Protection Verification Report

**Phase Goal:** Village protection based on structures, not chunks
**Verified:** 2026-01-22T12:30:00Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Player cannot break non-ore blocks inside village structure bounding boxes | VERIFIED | VillageProtection.kt line 66 returns false to block breaks inside structures |
| 2 | Player can break blocks underground below villages | VERIFIED | Uses getStructureWithPieceAt(pos) which only protects actual structure piece bounding boxes, not underground |
| 3 | Player can still break ores inside village structures | VERIFIED | Lines 52-56 check isOre() and return true to allow |
| 4 | Player can still break allowlist blocks inside village structures | VERIFIED | Lines 58-62 check ALLOWED_BLOCKS and return true to allow |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/thc/world/VillageProtection.kt` | Position-based village structure protection | VERIFIED | 107 lines, substantive implementation, uses getStructureWithPieceAt |

### Key Link Verification

| From | To | Via | Status | Details |
|------|------|-----|--------|---------|
| VillageProtection.kt | StructureManager.getStructureWithPieceAt | Direct API call | VERIFIED | Line 83: `structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)` |
| VillageProtection.kt | THC.kt | Registration | VERIFIED | Imported line 25, registered line 46 |
| VillageProtection.kt | WorldRestrictions.ALLOWED_BLOCKS | Import | VERIFIED | Line 59 uses allowlist for break exceptions |

### ROADMAP Success Criteria

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Player cannot break non-ore blocks within village structure bounding boxes | VERIFIED | Position-based check via isInsideVillageStructure() |
| Player can mine freely underground below villages | VERIFIED | getStructureWithPieceAt checks specific block position, not entire chunk |
| Current chunk-based protection replaced with structure-based protection | VERIFIED | No ChunkValidator.isVillageChunk call in VillageProtection.kt |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None | - | - | - | - |

No anti-patterns detected. No TODO, FIXME, placeholder, or stub patterns found.

### Build Verification

```
./gradlew build
BUILD SUCCESSFUL in 6s
```

### Implementation Details

**Key Change:**
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

**Why This Works:**
- `getStructureWithPieceAt()` checks if the specific block position falls within any village structure piece's bounding box
- Village structure pieces are only the actual buildings, paths, and foundations - not the entire underground
- A block 50 blocks below a village surface is NOT inside any structure piece's bounding box
- Therefore, underground mining is allowed while village structures remain protected

### Human Verification Recommended

While all automated checks pass, the following would benefit from human testing:

1. **Underground Mining Test**
   - Test: Find a village, dig straight down through a structure, continue mining at Y=10
   - Expected: Can mine freely underground, blocked only when inside actual structure pieces
   - Why human: Requires in-game testing to verify bounding box behavior

2. **Surface Structure Test**
   - Test: Try to break a village building block (wood, cobblestone)
   - Expected: Break is blocked
   - Why human: Requires in-game testing

3. **Ore Exception Test**
   - Test: If a village generates over an ore vein, try mining the ore
   - Expected: Ore can be mined even inside village structure
   - Why human: Requires specific world seed/scenario

---

*Verified: 2026-01-22T12:30:00Z*
*Verifier: Claude (gsd-verifier)*
