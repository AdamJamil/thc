---
phase: 15-threat-system
verified: 2026-01-19T20:30:00Z
status: passed
score: 6/6 must-haves verified
---

# Phase 15: Threat System Verification Report

**Phase Goal:** Tactical aggro management where damage creates threat across nearby mobs
**Verified:** 2026-01-19T20:30:00Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Each mob maintains a threat map (player -> double) | VERIFIED | `THCAttachments.MOB_THREAT` is `AttachmentType<Map<UUID, Double>>` (line 52-55); `ThreatManager` uses this attachment in all methods |
| 2 | Dealing damage to a mob adds that damage value as threat to all hostile/neutral mobs within 15 blocks | VERIFIED | `MobDamageThreatMixin` line 24: `THC_THREAT_RADIUS = 15.0`; line 53: `ThreatManager.addThreat(nearby, player.getUUID(), amount)`; line 58-61: filters for `MONSTER || CREATURE` |
| 3 | Threat decays by 1 per second per player per mob | VERIFIED | `ThreatManager.decayThreat()` line 72: 20-tick check (1 second); line 82: `threat - 1.0`; called in `ThreatTargetGoal.canUse/canContinueToUse` |
| 4 | Arrow hits add +10 bonus threat to the struck mob | VERIFIED | `ProjectileEntityMixin` line 48-49: `ThreatManager.addThreat(mob, player.getUUID(), 10.0)` in `thc$applyHitEffects` |
| 5 | Mobs target highest-threat player when any player's threat >= 5 (unless revenge takes priority) | VERIFIED | `ThreatTargetGoal` line 22: `MIN_THREAT = 5.0`; line 37: `getHighestThreatTarget(mob, MIN_THREAT)`; lines 73-82: revenge check with `getLastHurtByMob()` |
| 6 | Mobs only switch targets on: revenge strike OR another player gains strictly higher threat | VERIFIED | `ThreatTargetGoal` lines 73-82: revenge allows immediate switch; lines 84-91: `highestThreat > currentThreat` (strictly greater) check |

**Score:** 6/6 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/THCAttachments.java` | MOB_THREAT and THREAT_LAST_DECAY attachments | VERIFIED (67 lines) | MOB_THREAT at line 52, THREAT_LAST_DECAY at line 56 |
| `src/main/java/thc/threat/ThreatManager.java` | Threat data operations (add, get, set, decay, getHighest) | VERIFIED (126 lines) | All methods present: addThreat, getThreat, setThreat, getThreatMap, hasThreat, decayThreat, getHighestThreatTarget |
| `src/main/java/thc/threat/ThreatTargetGoal.java` | AI goal for threat-based targeting | VERIFIED (103 lines) | Extends TargetGoal, implements canUse/canContinueToUse with threat logic |
| `src/main/java/thc/mixin/MobDamageThreatMixin.java` | Damage event hook for threat propagation | VERIFIED (64 lines) | Targets LivingEntity.hurtServer, propagates to 15-block radius |
| `src/main/java/thc/mixin/MonsterThreatGoalMixin.java` | Goal injection into Monster mobs | VERIFIED (36 lines) | Injects ThreatTargetGoal at priority 0 for Monster instances |
| `src/main/java/thc/mixin/ProjectileEntityMixin.java` | Arrow bonus threat (+10) | VERIFIED (132 lines) | Line 48-49 adds +10 threat on projectile hit |
| `src/main/resources/thc.mixins.json` | Mixin registrations | VERIFIED | MobDamageThreatMixin (line 14), MonsterThreatGoalMixin (line 15) registered |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| ThreatManager.java | THCAttachments.MOB_THREAT | getAttachedOrCreate calls | WIRED | 7 usages across all methods |
| ThreatManager.java | THCAttachments.THREAT_LAST_DECAY | decayThreat timing | WIRED | Lines 69 (read) and 87 (write) |
| MobDamageThreatMixin.java | ThreatManager.addThreat | hurtServer TAIL injection | WIRED | Line 53 calls addThreat with damage amount |
| ProjectileEntityMixin.java | ThreatManager.addThreat | onHitEntity HEAD injection | WIRED | Line 49 calls addThreat with 10.0 |
| ThreatTargetGoal.java | ThreatManager (multiple methods) | canUse/canContinueToUse | WIRED | decayThreat (lines 34, 58), getThreat (line 61), getHighestThreatTarget (lines 37, 64, 85) |
| MonsterThreatGoalMixin.java | ThreatTargetGoal | registerGoals TAIL injection | WIRED | Line 33: `targetSelector.addGoal(0, new ThreatTargetGoal(...))` |

### Requirements Coverage

All THREAT requirements from ROADMAP.md are satisfied:

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| THREAT-01: Per-mob threat map storage | SATISFIED | MOB_THREAT attachment (Map<UUID, Double>) |
| THREAT-02: Damage propagates to 15-block radius | SATISFIED | MobDamageThreatMixin with THC_THREAT_RADIUS = 15.0 |
| THREAT-03: 1/sec decay rate | SATISFIED | decayThreat() with 20-tick check and -1.0 per player |
| THREAT-04: Arrow +10 bonus | SATISFIED | ProjectileEntityMixin line 49 |
| THREAT-05: MIN_THREAT = 5 threshold | SATISFIED | ThreatTargetGoal.MIN_THREAT = 5.0 |
| THREAT-06: Revenge/strictly-higher switching | SATISFIED | ThreatTargetGoal canContinueToUse() logic |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None | - | - | - | - |

No TODO, FIXME, placeholder, or stub patterns found in threat system files.

### Human Verification Required

### 1. Threat Propagation Test
**Test:** Have two players attack a zombie while other mobs are within 15 blocks. Check if all nearby mobs accumulate threat correctly.
**Expected:** All hostile/neutral mobs within 15 blocks receive threat equal to damage dealt.
**Why human:** Requires multiplayer testing and visual observation of mob behavior.

### 2. Arrow Bonus Threat Test
**Test:** Shoot a mob with an arrow and compare threat to melee damage of the same amount.
**Expected:** Arrow hit adds damage threat PLUS +10 bonus (total threat = damage + 10).
**Why human:** Requires in-game testing with threat values not directly visible.

### 3. Threat Decay Test
**Test:** Build up threat on a mob, then wait and observe target switching behavior.
**Expected:** After enough seconds (threat/1 per second), mob loses target when threat drops below 5.
**Why human:** Requires timing observation and mob AI behavior verification.

### 4. Target Switching Rules Test
**Test:** Player A has threat 10, Player B has threat 10. Have Player B deal damage (strictly higher) OR have Player B hit the mob (revenge).
**Expected:** Mob switches to B only on revenge strike OR when B's threat becomes strictly higher than A's.
**Why human:** Requires precise multiplayer coordination and observation.

### 5. Monster Coverage Test
**Test:** Verify threat targeting applies to various Monster subclasses (zombie, skeleton, creeper, spider, etc.).
**Expected:** All Monster-extending mobs use ThreatTargetGoal at priority 0.
**Why human:** Requires testing multiple mob types in-game.

### Gaps Summary

No gaps found. All 6 success criteria from ROADMAP.md are implemented and verified:

1. **Threat map storage**: Implemented via MOB_THREAT attachment
2. **Damage propagation (15 blocks)**: Implemented via MobDamageThreatMixin with THC_THREAT_RADIUS = 15.0
3. **Decay (1/sec)**: Implemented via ThreatManager.decayThreat() with 20-tick intervals
4. **Arrow bonus (+10)**: Implemented via ProjectileEntityMixin line 49
5. **MIN_THREAT threshold (5)**: Implemented in ThreatTargetGoal.MIN_THREAT = 5.0
6. **Switching rules**: Implemented in ThreatTargetGoal.canContinueToUse() with revenge and strictly-higher checks

All artifacts exist, are substantive (no stubs), and are properly wired together. Phase 15 goals are achieved.

---

*Verified: 2026-01-19T20:30:00Z*
*Verifier: Claude (gsd-verifier)*
