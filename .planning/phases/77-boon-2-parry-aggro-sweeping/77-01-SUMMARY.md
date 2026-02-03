---
phase: "77"
plan: "01"
subsystem: "combat"
tags: [parry, threat, sweeping, bastion, boon, stage-gate]
dependency-graph:
  requires:
    - "75: Class rename TANK to BASTION"
    - "76: Buckler stage gate"
  provides:
    - "BoonGate utility for Stage 3+ checks"
    - "Parry threat propagation for Bastion"
    - "Conditional sweeping edge for Bastion"
  affects:
    - "78: Snowball boons"
    - "79: Boat mastery"
tech-stack:
  added: []
  patterns:
    - "Shared gate utility for class+stage checks"
    - "Replicated vanilla logic for private method access"
key-files:
  created:
    - "src/main/java/thc/boon/BoonGate.java"
  modified:
    - "src/main/java/thc/mixin/LivingEntityMixin.java"
    - "src/main/java/thc/mixin/PlayerAttackMixin.java"
decisions:
  - id: "77-01-D01"
    choice: "BoonGate utility"
    why: "Avoid duplicating class+stage gate logic across mixins"
  - id: "77-01-D02"
    choice: "10.0 threat for parry"
    why: "Matches arrow hit threat, above 5.0 minimum threshold"
  - id: "77-01-D03"
    choice: "Replicate isSweepAttack logic"
    why: "Vanilla method is private, cannot call through redirect"
metrics:
  duration: "~4 min"
  completed: "2026-02-03"
---

# Phase 77 Plan 01: Parry Aggro and Sweeping Edge Summary

**One-liner:** Bastion Stage 3+ gains parry threat propagation (10.0 to nearby mobs) and vanilla sweeping edge via BoonGate utility

## What Was Built

### BoonGate Utility
New utility class `thc.boon.BoonGate` with shared `hasStage3Boon(ServerPlayer)` method that checks:
1. Player class is BASTION (via ClassManager)
2. Boon level >= 3 (via StageManager)

This utility is used by both features and can be reused for future Stage 3+ boons.

### Parry Threat Propagation
In `LivingEntityMixin`, after successful parry and stun:
- Checks `BoonGate.hasStage3Boon(serverPlayer)`
- If true, calls `thc$propagateParryThreat` which adds 10.0 threat to all monster mobs within 3 blocks
- Uses same radius as existing `thc$stunNearby` method
- Uses same threat amount as arrow hit (above 5.0 MIN_THREAT threshold)

### Conditional Sweeping Edge
In `PlayerAttackMixin`, the `thc$disableSweepAttack` redirect now:
- Checks `BoonGate.hasStage3Boon(serverPlayer)` first
- If not Bastion Stage 3+, returns false (sweeping disabled)
- If Bastion Stage 3+, replicates vanilla `isSweepAttack` logic:
  - Charged attack, on ground, not sprinting
  - Movement below speed threshold
  - Holding a sword

The vanilla logic was replicated because `isSweepAttack` is a private method and cannot be called through the redirect.

## Commits

| Commit | Description |
|--------|-------------|
| debb09a | Create BoonGate utility class |
| 576875c | Add parry threat propagation for Bastion Stage 3+ |
| b41d853 | Enable conditional sweeping edge for Bastion Stage 3+ |

## Key Links Verified

- LivingEntityMixin calls `BoonGate.hasStage3Boon` after parry
- PlayerAttackMixin calls `BoonGate.hasStage3Boon` in sweep redirect
- LivingEntityMixin calls `ThreatManager.addThreat` for threat propagation

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Cannot call private isSweepAttack from redirect**

- **Found during:** Task 3
- **Issue:** The plan suggested calling `instance.isSweepAttack(bl, bl2, bl3)` from the redirect, but `isSweepAttack` is a private method and the compiler rejected it
- **Fix:** Replicated the vanilla isSweepAttack logic directly in the redirect method
- **Files modified:** PlayerAttackMixin.java
- **Commit:** b41d853

## Success Criteria Verification

- [x] PRRY-01: Parry threat propagation requires Bastion class + Stage 3+ (gate check in LivingEntityMixin)
- [x] PRRY-02: Sweeping edge enabled for Bastion class + Stage 3+ (gate check in PlayerAttackMixin)
- [x] PRRY-03: Non-Bastion players cannot use sweeping edge (default return false)

## Next Phase Readiness

Phase 77 complete. Ready for Phase 78 (Snowball boons).

The BoonGate utility pattern established here can be reused for future stage-gated boons. For different stage thresholds, add new methods like `hasStage2Boon` or parameterize the check.
