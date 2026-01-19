---
phase: 09-parry-stun
plan: 01
subsystem: combat
tags: [buckler, parry, stun, knockback, crowd-control]

dependency-graph:
  requires: []
  provides:
    - "Enhanced parry stun with 3-block range"
    - "Knockback on stunned enemies"
  affects: []

tech-stack:
  added: []
  patterns:
    - "Vec3 directional knockback calculation"
    - "hurtMarked flag for velocity sync"

key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/LivingEntityMixin.java

decisions:
  - id: PARRY-KB-01
    choice: "0.5 horizontal + 0.2 vertical knockback values"
    reason: "Provides ~1 block effective knockback similar to vanilla knockback mechanics"

metrics:
  duration: 3 min
  completed: 2026-01-19
---

# Phase 09 Plan 01: Parry Stun Range and Knockback Summary

Enhanced buckler parry crowd control with 3-block stun range and knockback application.

## What Was Built

### Task 1: Increased Parry Stun Range
Modified `thc$stunNearby` method to use 3-block bounding box inflation instead of 2 blocks.

**Change:** `player.getBoundingBox().inflate(2.0D)` -> `player.getBoundingBox().inflate(3.0D)`

### Task 2: Knockback for Stunned Enemies
Added directional knockback calculation and application to stunned mobs:

```java
Vec3 direction = mob.position().subtract(player.position()).normalize();
mob.setDeltaMovement(direction.x * 0.5, 0.2, direction.z * 0.5);
mob.hurtMarked = true;
```

**Knockback values:**
- 0.5 horizontal component: pushes mob ~1 block away from player
- 0.2 vertical component: slight upward lift to ensure knockback triggers
- `hurtMarked = true`: ensures velocity change syncs to clients

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 8ddb689 | increase parry stun range to 3 blocks |
| 2 | f4caa00 | add knockback to stunned enemies |

## Verification Results

- [x] `./gradlew build` succeeds without errors
- [x] LivingEntityMixin.java contains `inflate(3.0D)` in thc$stunNearby
- [x] LivingEntityMixin.java contains `setDeltaMovement` for knockback

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed hasImpulse field access**
- **Found during:** Task 2
- **Issue:** Plan specified `mob.hasImpulse = true` but `hasImpulse` is not accessible on Mob type
- **Fix:** Used `mob.hurtMarked = true` which is the Entity field for marking velocity changes that need client sync
- **Files modified:** src/main/java/thc/mixin/LivingEntityMixin.java
- **Commit:** f4caa00

## Success Criteria

- [x] All tasks completed
- [x] All verification checks pass
- [x] No errors or warnings introduced
- [x] PARRY-01: Stun range extended to 3 blocks
- [x] PARRY-02: Knockback applied to stunned enemies

## Next Phase Readiness

Phase 10 (XP Economy Restriction) can proceed. No blockers or concerns.
