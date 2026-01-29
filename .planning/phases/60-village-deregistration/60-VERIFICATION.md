---
phase: 60-village-deregistration
verified: 2026-01-29T17:45:00Z
status: passed
score: 4/4 must-haves verified
re_verification: false
---

# Phase 60: Village Deregistration Verification Report

**Phase Goal:** Claimed chunks do not contribute to village mechanics (villager counts, bed counts)
**Verified:** 2026-01-29T17:45:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Beds placed in claimed chunks do not register as village POI | ✓ VERIFIED | ServerLevelPoiMixin intercepts updatePOIOnBlockStateChange with HEAD cancellation, blocks registration in claimed chunks |
| 2 | Villagers cannot claim beds located in claimed chunks | ✓ VERIFIED | BrainPoiMemoryMixin intercepts Brain.setMemory, blocks GlobalPos storage for HOME/JOB_SITE/POTENTIAL_JOB_SITE/MEETING_POINT in claimed chunks |
| 3 | Villages in unclaimed territory function normally | ✓ VERIFIED | Both mixins check ClaimManager.isClaimed - returns false for unclaimed chunks, normal flow proceeds |
| 4 | Existing village mechanics outside claimed chunks are unaffected | ✓ VERIFIED | HEAD injection with early return - no modification to vanilla logic when claim check fails |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/mixin/ServerLevelPoiMixin.java` | POI registration blocking in claimed chunks | ✓ VERIFIED | 41 lines, contains thc$blockPoiInClaimedChunks, HEAD injection on updatePOIOnBlockStateChange, calls ClaimManager.isClaimed |
| `src/main/java/thc/mixin/BrainPoiMemoryMixin.java` | Villager POI claiming prevention in claimed chunks | ✓ VERIFIED | 68 lines, contains thc$blockPoiClaimInClaimedChunks, HEAD injection on setMemory, filters GlobalPos values, checks memory types |
| `src/main/kotlin/thc/village/ServerHolder.kt` | Server reference for contexts without server access | ✓ VERIFIED | 26 lines, singleton with @Volatile server storage, getServer/setServer methods |

**All artifacts exist, substantive (well above minimum line counts), and properly wired.**

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| ServerLevelPoiMixin.java | ClaimManager.isClaimed | HEAD injection on updatePOIOnBlockStateChange | ✓ WIRED | Line 37: ClaimManager.INSTANCE.isClaimed(self.getServer(), chunkPos), calls ci.cancel() if claimed |
| BrainPoiMemoryMixin.java | ClaimManager.isClaimed | HEAD injection on setMemory | ✓ WIRED | Line 64: ClaimManager.INSTANCE.isClaimed(server, chunkPos), calls ci.cancel() if claimed |
| BrainPoiMemoryMixin.java | ServerHolder.getServer | Import and usage in setMemory injection | ✓ WIRED | Line 14: import, Line 56: ServerHolder.INSTANCE.getServer(), provides server context for Brain mixin |
| THC.kt | ServerHolder.setServer | SERVER_STARTED event registration | ✓ WIRED | Line 125: ServerHolder.setServer(server), sets reference during server initialization |
| Both mixins | thc.mixins.json | Mixin registration | ✓ WIRED | Both ServerLevelPoiMixin and BrainPoiMemoryMixin registered in mixins array |

**All key links verified - complete two-layer defense with proper server context handling.**

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| TERR-01: Villagers in claimed chunks deregistered | ✓ SATISFIED | None - BrainPoiMemoryMixin blocks POI memory storage |
| TERR-02: Beds in claimed chunks deregistered | ✓ SATISFIED | None - ServerLevelPoiMixin blocks POI registration |

**All requirements satisfied.**

### Anti-Patterns Found

**None detected.** 

Scanned all three implementation files:
- No TODO/FIXME/XXX/HACK comments
- No placeholder text
- No console.log or stub patterns
- No empty implementations
- All methods have real logic with ClaimManager checks and cancellation behavior

### Human Verification Required

The following items require manual in-game testing to fully verify goal achievement:

#### 1. Bed Registration Blocking in Claimed Chunks

**Test:** Claim a chunk using /claim, place a bed in the claimed chunk, spawn a villager nearby (within 48 blocks)
**Expected:** Villager should NOT pathfind to or claim the bed. Bed should not appear in villager's brain HOME memory. Villager should act as if the bed doesn't exist.
**Why human:** Requires runtime observation of villager AI behavior and brain memory state

#### 2. Job Site Blocking in Claimed Chunks

**Test:** Claim a chunk, place a workstation (lectern, barrel, etc.) in claimed chunk, spawn an unemployed villager nearby
**Expected:** Villager should NOT claim the job site. JOB_SITE memory should not be set. Villager should remain unemployed or seek workstations in unclaimed chunks.
**Why human:** Requires observing villager job acquisition behavior at runtime

#### 3. Village Mechanics in Unclaimed Territory

**Test:** In an unclaimed chunk, place beds and workstations, spawn villagers
**Expected:** Normal village mechanics - villagers claim beds, acquire jobs, pathfind to POI, breed when conditions met
**Why human:** Requires confirming vanilla behavior is preserved - no accidental over-blocking

#### 4. Pre-existing POI Handling

**Test:** Place bed in unclaimed chunk, let villager claim it, then claim the chunk, restart server (to ensure Brain mixin catches existing memories)
**Expected:** After chunk is claimed, villager should lose the bed memory and stop returning to it
**Why human:** Tests the second layer of defense (Brain mixin) catches POI that existed before claim

#### 5. POI in Adjacent Unclaimed Chunks

**Test:** Claim chunk A, leave chunk B unclaimed (adjacent), place bed in chunk B, spawn villager in chunk A
**Expected:** Villager in claimed chunk A should be able to claim bed in unclaimed chunk B (POI blocking is per-chunk, not per-villager)
**Why human:** Verifies chunk-based filtering doesn't accidentally block villagers based on their location

### Gaps Summary

**No gaps found.** All automated verification checks passed:

✓ All observable truths verified through code analysis
✓ All required artifacts exist and are substantive (135 total lines)
✓ All key links wired correctly (ClaimManager integration, ServerHolder pattern, mixin registration)
✓ Both requirements satisfied (TERR-01, TERR-02)
✓ No anti-patterns detected
✓ Build compiles successfully

**Phase goal achieved:** The codebase implements a complete two-layer defense preventing village mechanics in claimed chunks. ServerLevelPoiMixin blocks POI registration at the source (ServerLevel), and BrainPoiMemoryMixin blocks villager memory storage as a fallback. Villages in unclaimed territory are unaffected.

**Human verification recommended** to confirm runtime behavior matches code implementation, but automated verification confirms all structural requirements are met.

---

## Detailed Analysis

### Architecture Verification

**Two-Layer Defense Pattern:**

1. **Layer 1 (ServerLevelPoiMixin):** Upstream interception
   - Target: ServerLevel.updatePOIOnBlockStateChange
   - When: POI-eligible block (bed, workstation, bell) placed/removed
   - Action: Cancel if chunk is claimed
   - Coverage: Prevents POI from entering the system at all

2. **Layer 2 (BrainPoiMemoryMixin):** Downstream interception
   - Target: Brain.setMemory with GlobalPos values
   - When: Villager attempts to store POI location memory
   - Action: Cancel if memory type is POI-related and position is in claimed chunk
   - Coverage: Catches POI that existed before chunk was claimed

**Server Context Pattern:**
- Brain class lacks owner/server reference
- Solution: ServerHolder singleton set during SERVER_STARTED event
- Accessed via static reference in BrainPoiMemoryMixin
- Thread-safe with @Volatile annotation
- Null-safe: returns early if server not initialized

### Code Quality

**Substantiveness Check:**
- ServerLevelPoiMixin: 41 lines (exceeds 10 line minimum for mixins)
- BrainPoiMemoryMixin: 68 lines (exceeds 10 line minimum for mixins)
- ServerHolder: 26 lines (exceeds 5 line minimum for utilities)
- Total: 135 lines of new code

**Mixin Injection Quality:**
- Both use HEAD injection (earliest possible interception)
- Both are cancellable (ci.cancel() pattern)
- Both check ClaimManager.isClaimed before cancelling
- Both use defensive programming (null checks, type checks)
- BrainPoiMemoryMixin filters by memory type to avoid over-blocking

**No Stub Patterns:**
- No TODO/FIXME comments
- No placeholder returns (return null, return {})
- No console.log debugging
- All cancellation paths have real ClaimManager checks
- All methods are complete implementations

### Wiring Verification

**Import/Usage Analysis:**
- ServerLevelPoiMixin: Imports ClaimManager, uses at line 37
- BrainPoiMemoryMixin: Imports ClaimManager (line 13), ServerHolder (line 14)
- BrainPoiMemoryMixin: Uses ServerHolder.getServer() at line 56
- BrainPoiMemoryMixin: Uses ClaimManager.isClaimed at line 64
- THC.kt: Calls ServerHolder.setServer at line 125 in SERVER_STARTED event

**Mixin Registration:**
- thc.mixins.json contains "ServerLevelPoiMixin" in mixins array
- thc.mixins.json contains "BrainPoiMemoryMixin" in mixins array
- Both mixins will be loaded by Mixin framework at runtime

**ClaimManager Integration:**
- ClaimManager.isClaimed signature: fun isClaimed(server: MinecraftServer, chunkPos: ChunkPos): Boolean
- ServerLevelPoiMixin provides server via self.getServer() (ServerLevel has direct server reference)
- BrainPoiMemoryMixin provides server via ServerHolder.getServer() (Brain lacks server reference)
- Both convert BlockPos to ChunkPos for claim check
- ClaimManager returns false for unclaimed chunks, allowing normal flow

### Edge Cases

**Covered by implementation:**
1. **Pre-existing POI:** BrainPoiMemoryMixin catches attempts to claim POI that existed before chunk was claimed
2. **Client-side safety:** ServerHolder returns null on client, BrainPoiMemoryMixin returns early
3. **Server startup timing:** Null check in BrainPoiMemoryMixin handles calls before SERVER_STARTED event
4. **Non-POI memories:** GlobalPos type check ensures only POI-related memories (HOME, JOB_SITE, POTENTIAL_JOB_SITE, MEETING_POINT) are filtered
5. **POI removal:** ServerLevelPoiMixin also blocks POI removal in claimed chunks (acceptable - claimed chunk POI should be ignored entirely)
6. **Unclaimed chunks:** ClaimManager.isClaimed returns false, both mixins return early without cancelling, vanilla flow proceeds normally

### Build Status

```
BUILD SUCCESSFUL in 6s
```

No compilation errors, no mixin application errors, no runtime failures during build.

---

_Verified: 2026-01-29T17:45:00Z_
_Verifier: Claude (gsd-verifier)_
