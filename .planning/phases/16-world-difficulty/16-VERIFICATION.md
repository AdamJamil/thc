---
phase: 16-world-difficulty
verified: 2026-01-20T17:45:00Z
status: passed
score: 5/5 must-haves verified
---

# Phase 16: World Difficulty Verification Report

**Phase Goal:** Harder world outside bases, safer inside bases
**Verified:** 2026-01-20T17:45:00Z
**Status:** passed
**Re-verification:** No -- initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Mob griefing is disabled (creepers don't destroy blocks, endermen don't pick up blocks) | VERIFIED | THC.kt:158 sets `GameRules.MOB_GRIEFING` to `false` on server start |
| 2 | Smooth stone drops cobblestone when mined without silk touch | VERIFIED | smooth_stone.json uses `minecraft:alternatives` with silk_touch condition -- fallback is cobblestone |
| 3 | Regional difficulty is always at maximum in every chunk | VERIFIED | ServerLevelDifficultyMixin.java:21-27 creates DifficultyInstance with HARD, 3600000L inhabited time |
| 4 | Moon phase is always treated as "true" for all mob/difficulty checks | VERIFIED | ServerLevelDifficultyMixin.java:25 sets moonPhaseFactor to 1.0f (full moon) |
| 5 | Mobs cannot spawn naturally in base chunks | VERIFIED | NaturalSpawnerMixin.java:43-44 returns false for claimed chunks via ClaimManager.isClaimed() |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/thc/THC.kt` | MOB_GRIEFING gamerule disabled | EXISTS + SUBSTANTIVE + WIRED | Line 158: `world.gameRules.set(GameRules.MOB_GRIEFING, false, server)` in lockWorldToNight() |
| `src/main/resources/data/minecraft/loot_table/blocks/smooth_stone.json` | Silk touch conditional loot | EXISTS + SUBSTANTIVE + WIRED | 41 lines, uses alternatives entry with silk_touch condition |
| `src/main/java/thc/mixin/ServerLevelDifficultyMixin.java` | Max difficulty override | EXISTS + SUBSTANTIVE + WIRED | 29 lines, HEAD inject on getCurrentDifficultyAt returning max DifficultyInstance |
| `src/main/java/thc/mixin/NaturalSpawnerMixin.java` | Spawn blocking in claimed chunks | EXISTS + SUBSTANTIVE + WIRED | 47 lines, HEAD inject on isValidSpawnPostitionForType checking ClaimManager |
| `src/main/resources/thc.mixins.json` | Mixin registrations | EXISTS + SUBSTANTIVE + WIRED | Contains both ServerLevelDifficultyMixin and NaturalSpawnerMixin |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| THC.kt lockWorldToNight | GameRules.MOB_GRIEFING | world.gameRules.set() | WIRED | Called in SERVER_STARTED event for all worlds |
| ServerLevelDifficultyMixin | ServerLevel.getCurrentDifficultyAt | @Inject HEAD | WIRED | Registered in thc.mixins.json, cancellable return |
| NaturalSpawnerMixin | NaturalSpawner.isValidSpawnPostitionForType | @Inject HEAD | WIRED | Registered in thc.mixins.json |
| NaturalSpawnerMixin | ClaimManager.isClaimed | ChunkPos lookup | WIRED | ClaimManager.INSTANCE.isClaimed(server, chunkPos) matches signature |

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| WORLD-01: Mob griefing disabled | SATISFIED | - |
| WORLD-02: Smooth stone silk touch | SATISFIED | - |
| WORLD-03: Max regional difficulty | SATISFIED | - |
| WORLD-04: Moon phase always true | SATISFIED | - |
| WORLD-05: No mob spawns in base chunks | SATISFIED | - |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | - |

No TODO, FIXME, placeholder, or stub patterns found in any phase 16 artifacts.

### Human Verification Required

### 1. Mob Griefing Disabled
**Test:** Place a creeper near blocks and let it explode (use flint/steel or wait for player proximity)
**Expected:** Creeper explosion damages entities but does NOT destroy any blocks
**Why human:** Requires in-game testing of explosion behavior

### 2. Enderman Block Picking
**Test:** Observe endermen in the world near grass/dirt blocks
**Expected:** Endermen do NOT pick up blocks (they teleport but don't carry blocks)
**Why human:** Requires observation of mob behavior over time

### 3. Smooth Stone Loot
**Test:** Mine smooth stone with a normal pickaxe, then with silk touch pickaxe
**Expected:** Normal pickaxe drops cobblestone, silk touch drops smooth stone
**Why human:** Requires actual block breaking in-game

### 4. Regional Difficulty
**Test:** Use F3 debug screen to check "Local Difficulty" in a brand new chunk
**Expected:** Local difficulty shows maximum value (6.75 on Hard) immediately
**Why human:** Requires F3 screen inspection in-game

### 5. Mob Equipment
**Test:** Observe zombie/skeleton spawns at night
**Expected:** Higher rate of mobs with armor and weapons than vanilla (max difficulty equipment rates)
**Why human:** Requires observation over multiple spawns

### 6. Base Chunk Spawning
**Test:** Claim a chunk, set it to night, wait in the claimed area
**Expected:** No hostile mobs spawn naturally within the claimed chunk boundaries
**Why human:** Requires time-based observation in claimed territory

### Gaps Summary

No gaps found. All five success criteria are implemented:

1. **Mob griefing** - GameRules.MOB_GRIEFING set to false on server start
2. **Smooth stone** - Loot table with silk_touch conditional using alternatives entry type
3. **Regional difficulty** - ServerLevelDifficultyMixin overrides getCurrentDifficultyAt with HARD + max inhabited time
4. **Moon phase** - DifficultyInstance constructed with moonPhaseFactor 1.0f (full moon)
5. **Base spawns** - NaturalSpawnerMixin blocks isValidSpawnPostitionForType in claimed chunks

Build passes successfully (11 tasks UP-TO-DATE). All mixins registered. All wiring verified.

---

*Verified: 2026-01-20T17:45:00Z*
*Verifier: Claude (gsd-verifier)*
