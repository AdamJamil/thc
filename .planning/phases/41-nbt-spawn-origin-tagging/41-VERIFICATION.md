---
phase: 41-nbt-spawn-origin-tagging
verified: 2026-01-24T16:39:54Z
status: passed
score: 5/5 must-haves verified
---

# Phase 41: NBT Spawn Origin Tagging Verification Report

**Phase Goal:** Every spawned mob has region origin tracked for cap counting
**Verified:** 2026-01-24T16:39:54Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Surface-spawned mob has spawnSystem.region = OW_SURFACE in NBT | VERIFIED | `MobFinalizeSpawnMixin.java:60` returns "OW_SURFACE" when y >= surfaceY, attachment set at line 41 |
| 2 | Upper cave mob (Y >= 0, below heightmap) has spawnSystem.region = OW_UPPER_CAVE | VERIFIED | `MobFinalizeSpawnMixin.java:64` returns "OW_UPPER_CAVE" for y >= 0 but below heightmap |
| 3 | Lower cave mob (Y < 0) has spawnSystem.region = OW_LOWER_CAVE | VERIFIED | `MobFinalizeSpawnMixin.java:54` returns "OW_LOWER_CAVE" when y < 0 |
| 4 | NATURAL + MONSTER spawns have spawnSystem.counted = true | VERIFIED | `MobFinalizeSpawnMixin.java:44-45` sets SPAWN_COUNTED based on MobCategory.MONSTER, only for NATURAL/CHUNK_GENERATION spawn reasons (lines 29-31) |
| 5 | Structure spawner mobs have no spawn tags | VERIFIED | `MobFinalizeSpawnMixin.java:29-31` returns early if reason is not NATURAL or CHUNK_GENERATION, leaving attachments at defaults (null/false) |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/THCAttachments.java` | SPAWN_REGION and SPAWN_COUNTED attachment definitions | VERIFIED | Lines 76-89: Both attachments defined with persistent codecs (STRING, BOOL) |
| `src/main/java/thc/mixin/MobFinalizeSpawnMixin.java` | Spawn-time tagging logic with region detection (min 40 lines) | VERIFIED | 66 lines, substantive implementation with region detection logic |
| `src/main/resources/thc.mixins.json` | MobFinalizeSpawnMixin registered | VERIFIED | Line 23: "MobFinalizeSpawnMixin" in mixins array |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| MobFinalizeSpawnMixin.java | THCAttachments.SPAWN_REGION | setAttached call | WIRED | Line 41: `self.setAttached(THCAttachments.SPAWN_REGION, region)` |
| MobFinalizeSpawnMixin.java | THCAttachments.SPAWN_COUNTED | setAttached call | WIRED | Line 45: `self.setAttached(THCAttachments.SPAWN_COUNTED, isMonster)` |
| MobFinalizeSpawnMixin.java | Heightmap.Types.MOTION_BLOCKING | getHeight call | WIRED | Line 58: `level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())` |

### Requirements Coverage

| Requirement | Status | Notes |
|-------------|--------|-------|
| FR-23: NBT spawn origin tagging | SATISFIED | SPAWN_REGION and SPAWN_COUNTED attachments implemented with proper region detection |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | No anti-patterns found |

### Human Verification Required

| # | Test | Expected | Why Human |
|---|------|----------|-----------|
| 1 | Spawn mob on surface (plains) via natural spawning, check NBT with /data command | `spawnSystem.region = "OW_SURFACE"` and `spawnSystem.counted = true` (if monster) | Requires in-game spawn and NBT viewer |
| 2 | Spawn mob in cave above Y=0, check NBT | `spawnSystem.region = "OW_UPPER_CAVE"` | Requires in-game spawn location verification |
| 3 | Spawn mob in deep cave (Y < 0), check NBT | `spawnSystem.region = "OW_LOWER_CAVE"` | Requires specific spawn location |
| 4 | Activate dungeon spawner, check spawned mob NBT | No spawn tags (region = null, counted = false) | Requires spawner structure |

### Verification Details

**Artifact Level 1 (Existence):**
- `THCAttachments.java`: EXISTS (97 lines)
- `MobFinalizeSpawnMixin.java`: EXISTS (66 lines)
- `thc.mixins.json`: EXISTS, contains MobFinalizeSpawnMixin

**Artifact Level 2 (Substantive):**
- `THCAttachments.java`: SPAWN_REGION (lines 76-82) with persistent Codec.STRING, SPAWN_COUNTED (lines 83-89) with persistent Codec.BOOL
- `MobFinalizeSpawnMixin.java`: 66 lines, no TODO/FIXME/placeholder patterns, real region detection logic with heightmap comparison

**Artifact Level 3 (Wired):**
- THCAttachments imported in mixin (line 18)
- Both attachments used via setAttached calls (lines 41, 45)
- Heightmap API correctly used for surface detection (line 58)

**Region Detection Logic Verified:**
```java
// Y < 0 -> OW_LOWER_CAVE
// Y >= surfaceY -> OW_SURFACE  
// Y >= 0 but < surfaceY -> OW_UPPER_CAVE
```

**Spawn Reason Filtering Verified:**
- Only NATURAL and CHUNK_GENERATION reasons tagged (line 29)
- Only Overworld dimension tagged (line 35)
- Only MONSTER category gets counted=true (line 44)

---

_Verified: 2026-01-24T16:39:54Z_
_Verifier: Claude (gsd-verifier)_
