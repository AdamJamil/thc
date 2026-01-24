---
phase: 38-spawn-table-replacements
verified: 2026-01-24T01:32:54Z
status: passed
score: 6/6 must-haves verified
---

# Phase 38: Spawn Table Replacements Verification Report

**Phase Goal:** Overworld surface threats shift from basic zombies/skeletons to more dangerous variants
**Verified:** 2026-01-24T01:32:54Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Surface zombies spawn as husks (sky-visible NATURAL spawns) | VERIFIED | Line 93: `if (type == EntityType.ZOMBIE)` + Line 85: `level.canSeeSky(pos)` check + Line 110: `EntityType.HUSK.create()` |
| 2 | Surface skeletons spawn as strays (sky-visible NATURAL spawns) | VERIFIED | Line 97: `if (type == EntityType.SKELETON)` + Line 85: `canSeeSky` check + Line 142: `EntityType.STRAY.create()` |
| 3 | Underground zombies remain zombies (no sky visibility) | VERIFIED | Line 85-87: `if (!level.canSeeSky(pos)) { return entity; }` - returns original entity unchanged |
| 4 | Underground skeletons remain skeletons (no sky visibility) | VERIFIED | Same canSeeSky check at Line 85-87 preserves original entity |
| 5 | Spider jockey skeletons are preserved (JOCKEY spawn reason bypass) | VERIFIED | Line 78: `if (!entity.getPassengers().isEmpty() \|\| entity.getVehicle() != null)` - passenger check bypasses replacement |
| 6 | Dungeon spawner mobs are preserved (SPAWNER spawn reason bypass) | VERIFIED | Mixin only targets `NaturalSpawner.spawnCategoryForPosition` - structure spawners use different code paths (BaseSpawner class) |

**Score:** 6/6 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/mixin/SpawnReplacementMixin.java` | Entity replacement logic at spawn time | VERIFIED | 163 lines, has @Redirect annotation, substantive implementation |
| `src/main/resources/thc.mixins.json` | Mixin registration | VERIFIED | Line 32: `"SpawnReplacementMixin"` present in mixins array |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| SpawnReplacementMixin | NaturalSpawner.spawnCategoryForPosition | @Redirect on addFreshEntityWithPassengers | WIRED | Line 53-59: @Redirect targeting `ServerLevel;addFreshEntityWithPassengers` within `spawnCategoryForPosition` method |
| SpawnReplacementMixin | thc.mixins.json | Mixin registration | WIRED | Line 32 of thc.mixins.json registers SpawnReplacementMixin |

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| FR-03: Zombie -> Husk replacement in NaturalSpawner | SATISFIED | None |
| FR-06: Skeleton -> Stray replacement in same mixin | SATISFIED | None |
| Exception: Structure spawns use vanilla pools | SATISFIED | Mixin only targets NaturalSpawner, not BaseSpawner |
| Exception: Spider jockeys keep skeleton riders | SATISFIED | Passenger check at line 78 |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None | - | - | - | No anti-patterns detected |

**Checks performed:**
- No TODO/FIXME/placeholder comments found
- No empty returns (return null/return {}/return []) found
- No console.log patterns (N/A for Java)
- File size adequate (163 lines > 60 line minimum)

### Human Verification Required

The following items need human testing to fully confirm functionality:

### 1. Surface Spawn Replacement Test
**Test:** AFK 10 minutes in plains biome at night on surface
**Expected:** Zero zombies spawn, only husks. Zero skeletons spawn, only strays.
**Why human:** Requires in-game observation of actual spawn behavior over time

### 2. Spider Jockey Preservation Test
**Test:** AFK in plains until spider jockey spawns (1% chance per spider)
**Expected:** Spider jockey has skeleton rider, not stray
**Why human:** Rare spawn requiring patience and visual confirmation

### 3. Structure Spawner Test
**Test:** Find dungeon spawner, observe spawned mobs
**Expected:** Spawners produce vanilla zombies/skeletons (not husks/strays)
**Why human:** Requires locating dungeon and observing spawner behavior

### 4. Underground Spawn Test
**Test:** AFK in cave with no sky access (sealed underground)
**Expected:** Zombies and skeletons spawn normally (not replaced)
**Why human:** Requires in-game observation in specific environment

### Verification Summary

All automated verification checks pass:

1. **Artifact existence:** SpawnReplacementMixin.java exists (163 lines)
2. **Artifact substantive:** No stub patterns, real implementation with @Redirect, entity creation, data copying
3. **Artifact wired:** Registered in thc.mixins.json, targets correct method
4. **Key logic present:**
   - Sky visibility check: `level.canSeeSky(pos)` at line 85
   - Zombie replacement: `EntityType.ZOMBIE` check + `EntityType.HUSK.create()` at lines 93, 110
   - Skeleton replacement: `EntityType.SKELETON` check + `EntityType.STRAY.create()` at lines 97, 142
   - Spider jockey bypass: Passenger check at line 78
   - Data preservation: Baby status (lines 125-127), equipment copying (lines 130-132, 157-158)
5. **Build succeeds:** `./gradlew build` completes without errors
6. **Commit exists:** `9c5c95d` contains the implementation

**Confidence:** HIGH - All structural verification passes. Human testing recommended to confirm runtime behavior matches expectations.

---

*Verified: 2026-01-24T01:32:54Z*
*Verifier: Claude (gsd-verifier)*
