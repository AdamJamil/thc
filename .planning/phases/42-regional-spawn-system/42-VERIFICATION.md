---
phase: 42-regional-spawn-system
verified: 2026-01-24T14:30:00Z
status: passed
score: 7/7 must-haves verified
human_verification:
  - test: "Spawn a witch on the surface (5% chance) by standing in open area at night"
    expected: "Witches occasionally spawn among normal mobs"
    why_human: "Requires in-game testing to verify spawn probability"
  - test: "Explore upper caves (Y >= 0, no sky) and observe mob composition"
    expected: "Pillagers (melee with iron sword, ranged with crossbow), vexes, witches mixed with zombies/skeletons/spiders"
    why_human: "Requires in-game observation of spawn distribution variety"
  - test: "Explore lower caves (Y < 0) and observe mob composition"
    expected: "Blazes, breezes, vindicators, pillagers (melee only), evokers mixed with vanilla mobs"
    why_human: "Requires in-game observation of spawn distribution variety"
  - test: "Observe melee pillager combat behavior"
    expected: "Melee pillagers with iron swords actively pursue and attack with melee, not standing idle"
    why_human: "Requires in-game observation of AI behavior"
  - test: "Observe custom mob pack sizes"
    expected: "Custom distribution mobs spawn in groups of 1-4 nearby"
    why_human: "Requires in-game observation of spawn pack behavior"
  - test: "Visit a Nether fortress or pillager outpost"
    expected: "Structure spawns use vanilla mob types, not affected by regional distribution"
    why_human: "Requires in-game testing in structures"
---

# Phase 42: Regional Spawn System Verification Report

**Phase Goal:** Overworld spawns follow region-based distributions with custom mob types
**Verified:** 2026-01-24T14:30:00Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Surface has 5% witch, 95% vanilla distribution | VERIFIED | SpawnDistributions.java lines 64-67: 5 witch, 95 vanilla fallback in OW_SURFACE table |
| 2 | Upper cave (Y >= 0, no sky) has pillagers, vexes, witches mixed with vanilla | VERIFIED | SpawnDistributions.java lines 70-76: 5% witch, 2% vex, 10% ranged pillager, 25% melee pillager, 58% vanilla in OW_UPPER_CAVE |
| 3 | Lower cave (Y < 0, no sky) has blazes, breezes, vindicators, pillagers, evokers | VERIFIED | SpawnDistributions.java lines 79-86: 8% blaze, 8% breeze, 12% vindicator, 25% melee pillager, 2% evoker, 45% vanilla in OW_LOWER_CAVE |
| 4 | Cave pillagers have visible melee weapons (iron swords) OR crossbows | VERIFIED | PillagerVariant.java lines 21-34: MELEE sets iron sword, RANGED preserves vanilla crossbow |
| 5 | Custom distribution mobs spawn in packs of 1-4 | VERIFIED | SpawnReplacementMixin.java line 148: `1 + level.random.nextInt(4)` gives [1,4] uniform |
| 6 | Vanilla fallback spawns use vanilla pack sizes | VERIFIED | SpawnReplacementMixin.java lines 96-98: vanilla fallback proceeds to `thc$getReplacementEntity` then `addFreshEntityWithPassengers` with original entity |
| 7 | Structure spawns bypass regional system entirely | VERIFIED | Mixin targets `NaturalSpawner.spawnCategoryForPosition` (line 68) which only handles natural spawning; structures use different spawners |

**Score:** 7/7 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/spawn/SpawnDistributions.java` | Weighted random selection for regional spawn distributions | VERIFIED (131 lines) | Has selectMob(), MobSelection, WeightedEntry; tables sum to 100 validated |
| `src/main/java/thc/spawn/PillagerVariant.java` | Equipment application for MELEE/RANGED pillager variants | VERIFIED (53 lines) | MELEE sets iron sword with 0% drop chance, RANGED is no-op |
| `src/main/java/thc/mixin/SpawnReplacementMixin.java` | Regional distribution integration with spawn replacement | VERIFIED (296 lines) | @Redirect on addFreshEntityWithPassengers, region detection, pack spawning |
| `src/main/java/thc/mixin/PillagerMixin.java` | AI goal modification for melee pillager variant | VERIFIED (91 lines) | Removes RangedAttackGoal, adds MeleeAttackGoal for iron sword pillagers |
| `src/main/resources/thc.mixins.json` | Mixin registration | VERIFIED | PillagerMixin on line 10, SpawnReplacementMixin on line 40 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| SpawnReplacementMixin | SpawnDistributions.selectMob | method call in redirect | WIRED | Line 86: `SpawnDistributions.selectMob(region, level.random)` |
| SpawnReplacementMixin | PillagerVariant.applyEquipment | method call for pillager equipment | WIRED | Line 197: `PillagerVariant.valueOf(selection.variant()).applyEquipment(pillager)` |
| PillagerMixin | goalSelector | @Shadow field access | WIRED | Line 56: `protected GoalSelector goalSelector` with @Shadow @Final |
| PillagerMixin | Items.IRON_SWORD | equipment check for variant detection | WIRED | Line 73: `self.getMainHandItem().is(Items.IRON_SWORD)` |
| SpawnReplacementMixin | canSeeSky | region detection | WIRED | Lines 119, 218: `level.canSeeSky(pos)` for surface detection per FR-18 |
| SpawnReplacementMixin | SpawnPlacements.isSpawnPositionOk | collision checks | WIRED | Line 164: validates each pack member position |
| SpawnReplacementMixin | SpawnGroupData | pack threading | WIRED | Lines 151, 193: `groupData` threaded through `mob.finalizeSpawn()` calls |

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| FR-18: Region detection | SATISFIED | canSeeSky -> SURFACE, else Y<0 -> LOWER_CAVE, else UPPER_CAVE |
| FR-19: Custom distributions per region | SATISFIED | Three tables with exact percentages per spec |
| FR-20: Pillager variants | SATISFIED | MELEE gets iron sword, RANGED keeps crossbow; melee AI modified |
| FR-21: Pack sizes | SATISFIED | Custom: 1-4 uniform; Vanilla: unchanged (entity passed through) |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | No anti-patterns found |

### Human Verification Required

These items require in-game testing to fully verify:

### 1. Surface Witch Spawns
**Test:** Stand in an open area at night on the surface and observe spawns
**Expected:** Witches occasionally appear (~5% of monster spawns)
**Why human:** Statistical probability requires observation over multiple spawns

### 2. Upper Cave Mob Variety
**Test:** Explore caves above Y=0 with no sky access
**Expected:** Pillagers (sword and crossbow variants), vexes, witches among vanilla mobs
**Why human:** Need to observe actual spawn distribution variety

### 3. Lower Cave Mob Variety  
**Test:** Explore deep caves below Y=0
**Expected:** Blazes, breezes, vindicators, melee pillagers, evokers among vanilla mobs
**Why human:** Need to observe actual spawn distribution variety

### 4. Melee Pillager Combat
**Test:** Let a melee pillager (iron sword) target you
**Expected:** Pillager actively chases and attacks with melee, does not stand idle
**Why human:** Requires observation of AI behavior in combat

### 5. Pack Spawn Sizes
**Test:** Observe custom mob groups when they spawn
**Expected:** Custom mobs appear in groups of 1-4, spread across a few blocks
**Why human:** Pack spawning behavior requires real-time observation

### 6. Structure Spawn Bypass
**Test:** Visit Nether fortress, pillager outpost, or woodland mansion
**Expected:** Only vanilla structure-specific mobs spawn (no blazes in outpost, etc.)
**Why human:** Requires testing in actual game structures

## Summary

All automated verification checks pass:
- All 4 artifacts exist and are substantive (131, 53, 296, 91 lines respectively)
- All 7 key links verified as wired
- All 4 requirements covered (FR-18, FR-19, FR-20, FR-21)
- Build passes with no errors
- No anti-patterns found
- Distribution tables validated (each sums to 100)

The regional spawn system is structurally complete. Human verification items remain for confirming in-game behavior.

---

*Verified: 2026-01-24T14:30:00Z*
*Verifier: Claude (gsd-verifier)*
