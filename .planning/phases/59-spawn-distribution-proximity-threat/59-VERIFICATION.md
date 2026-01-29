---
phase: 59-spawn-distribution-proximity-threat
verified: 2026-01-29T16:01:11Z
status: passed
score: 5/5 must-haves verified
---

# Phase 59: Spawn Distribution & Proximity Threat Verification Report

**Phase Goal:** Deepslate region has wither skeletons in spawn pool; damage dealing propagates threat to nearby mobs

**Verified:** 2026-01-29T16:01:11Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Wither skeletons spawn in deepslate caves at 15% rate | ✓ VERIFIED | SpawnDistributions.java line 80: `EntityType.WITHER_SKELETON, 15` |
| 2 | Pillagers spawn in deepslate caves at 20% rate (reduced from 25%) | ✓ VERIFIED | SpawnDistributions.java line 84: `EntityType.PILLAGER, "MELEE", 20` with comment "// was 25" |
| 3 | Vanilla mobs spawn in deepslate caves at 35% rate (reduced from 45%) | ✓ VERIFIED | SpawnDistributions.java line 86: `null, null, 35` with comment "// was 45" |
| 4 | Dealing damage adds ceil(damage/4) threat to mobs within 5 blocks of player | ✓ VERIFIED | MobDamageThreatMixin.java line 50: `Math.ceil(amount / 4.0)` and line 53: `inflate(5.0)` |
| 5 | Direct damage target does not receive proximity threat bonus | ✓ VERIFIED | MobDamageThreatMixin.java lines 56-58: `if (nearby == damagedMob) continue;` |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/spawn/SpawnDistributions.java` | OW_LOWER_CAVE distribution with wither skeleton at 15% | ✓ VERIFIED | 132 lines, substantive implementation, wired to SpawnReplacementMixin |
| `src/main/java/thc/mixin/MobDamageThreatMixin.java` | Proximity threat propagation with ceil(dmg/4) | ✓ VERIFIED | 70 lines, substantive implementation, wired to ThreatManager |

#### Artifact Analysis

**SpawnDistributions.java:**
- **Level 1 (Exists):** ✓ File exists at expected path
- **Level 2 (Substantive):** ✓ 132 lines, complete weighted distribution system with validation
  - Contains required entry: `EntityType.WITHER_SKELETON, 15` (line 80)
  - Contains required entry: `EntityType.PILLAGER, "MELEE", 20` (line 84)
  - Contains required entry: `null, null, 35` for vanilla fallback (line 86)
  - Distribution sum validation: throws IllegalStateException if not 100
  - No stub patterns (TODO, FIXME, placeholder)
- **Level 3 (Wired):** ✓ Imported and used by SpawnReplacementMixin.java (line 27, line 97)
  - Called via `SpawnDistributions.selectMob(region, level.random)`
  - Integration point verified in spawn replacement logic

**MobDamageThreatMixin.java:**
- **Level 1 (Exists):** ✓ File exists at expected path
- **Level 2 (Substantive):** ✓ 70 lines, complete proximity threat implementation
  - Contains required calculation: `Math.ceil(amount / 4.0)` (line 50)
  - Contains required radius: `player.getBoundingBox().inflate(5.0)` (line 53)
  - Contains required exclusion: `if (nearby == damagedMob) continue;` (lines 56-58)
  - No stub patterns (TODO, FIXME, placeholder)
  - Proper exports and mixin registration
- **Level 3 (Wired):** ✓ Imports ThreatManager (line 16), calls `ThreatManager.addThreat()` (line 59)
  - Registered in thc.mixins.json (line 33)
  - Injects into LivingEntity.hurtServer at TAIL

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| SpawnDistributions.java | SpawnReplacementMixin | selectMob() weighted random | ✓ WIRED | SpawnReplacementMixin imports SpawnDistributions (line 27), calls selectMob() at line 97 with region and random source |
| MobDamageThreatMixin.java | ThreatManager | addThreat() for proximity mobs | ✓ WIRED | MobDamageThreatMixin imports ThreatManager (line 16), calls addThreat() with proximityThreat variable (line 59) |

**Link Analysis:**

**Link 1: SpawnDistributions → SpawnReplacementMixin**
- Pattern match: `SpawnDistributions.selectMob(region, level.random)` found at SpawnReplacementMixin.java line 97
- Call includes correct region parameter ("OW_LOWER_CAVE" from RegionDetector)
- Result properly handled: custom pack spawn on !isVanilla(), vanilla fallback on isVanilla()
- Distribution correctly affects spawn behavior in deepslate caves (Y < 0)

**Link 2: MobDamageThreatMixin → ThreatManager**
- Pattern match: `ThreatManager.addThreat(nearby, player.getUUID(), proximityThreat)` found at line 59
- Correct parameters: mob entity, player UUID, calculated threat value
- Proximity threat calculation verified: `Math.ceil(amount / 4.0)`
- Player-centered AABB verified: `player.getBoundingBox().inflate(5.0)`
- Direct target exclusion verified: `if (nearby == damagedMob) continue;`

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| SPAWN-01: Deepslate region includes wither skeletons at 15% spawn weight | ✓ SATISFIED | None - verified at line 80 |
| SPAWN-02: Deepslate pillager weight reduced from 25% to 20% | ✓ SATISFIED | None - verified at line 84 |
| SPAWN-03: Deepslate vanilla fallback reduced from 45% to 35% | ✓ SATISFIED | None - verified at line 86 |
| THRT-01: Dealing X damage adds ceil(X/4) threat to mobs within 5 blocks of player | ✓ SATISFIED | None - verified at lines 50, 53 |
| THRT-02: Direct damage target does not receive double threat bonus | ✓ SATISFIED | None - verified at lines 56-58 |

**All 5 requirements satisfied.** Phase 59 requirements complete.

### Anti-Patterns Found

**Scan Results:** No anti-patterns detected

- No TODO/FIXME/XXX/HACK comments found
- No placeholder or "coming soon" text found
- No empty return statements found
- No console.log-only implementations found
- All implementations are substantive and complete

### Human Verification Required

#### 1. Wither Skeleton Spawn Verification

**Test:** Enter deepslate region (Y < 0 in caves), observe mob spawns over time
**Expected:** Wither skeletons spawn at noticeable rate (~15% of hostile spawns), no fortress required
**Why human:** Spawn rates require time observation and can't be verified from code analysis alone

#### 2. Proximity Threat Radius Verification

**Test:** Attack a zombie while standing near (within 5 blocks) a skeleton; verify skeleton becomes aggressive
**Expected:** Skeleton receives threat and targets player even though not directly attacked
**Why human:** Requires in-game testing to verify AABB inflation and entity filtering work correctly

#### 3. Direct Target Exclusion Verification

**Test:** Attack a zombie dealing 8 damage; check threat values on the zombie vs nearby mobs
**Expected:** Direct zombie receives full threat from attack, nearby mobs receive only ceil(8/4)=2 threat
**Why human:** Threat values are internal state requiring debug output or behavior observation

#### 4. Threat Calculation Verification

**Test:** Deal various damage amounts (4, 8, 16, 20) and observe threat on nearby mobs
**Expected:** 
- 4 damage → 1 threat (ceil(4/4))
- 8 damage → 2 threat (ceil(8/4))
- 16 damage → 4 threat (ceil(16/4))
- 20 damage → 5 threat (ceil(20/4))
**Why human:** Requires controlled damage dealing and threat value inspection

#### 5. Spawn Distribution Sum Verification

**Test:** Start server and check logs for distribution validation
**Expected:** No "Distribution for OW_LOWER_CAVE sums to X, expected 100" error
**Why human:** Already verified by successful builds mentioned in SUMMARY, but worth confirming in actual server startup

---

## Verification Summary

**All must-haves verified.** Phase 59 goal achieved.

### Artifacts Verified
- ✓ SpawnDistributions.java: OW_LOWER_CAVE distribution correctly modified (15% wither skeleton, 20% pillager, 35% vanilla)
- ✓ MobDamageThreatMixin.java: Proximity threat correctly implemented (player-centered, ceil(dmg/4), excludes direct target)

### Wiring Verified
- ✓ SpawnDistributions integrated with SpawnReplacementMixin for weighted spawn selection
- ✓ MobDamageThreatMixin integrated with ThreatManager for threat propagation

### Requirements Verified
- ✓ All 5 requirements (SPAWN-01, SPAWN-02, SPAWN-03, THRT-01, THRT-02) satisfied

### Anti-Patterns
- ✓ No stubs, TODOs, or placeholder code detected

### Code Quality
- ✓ Substantive implementations (132 and 70 lines respectively)
- ✓ Proper error handling (distribution sum validation)
- ✓ Clear variable naming (damagedMob, proximityThreat)
- ✓ Documented behavior (comments on line 84, 86)

**Human verification items:** 5 items flagged for in-game testing, but automated structural verification passes all checks.

**Ready to proceed to Phase 60.**

---

_Verified: 2026-01-29T16:01:11Z_
_Verifier: Claude (gsd-verifier)_
