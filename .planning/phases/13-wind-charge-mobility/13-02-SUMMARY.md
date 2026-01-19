---
phase: 13
plan: 02
subsystem: mobility
tags: [wind-charge, mixin, fall-damage, attachment, velocity]

dependency_graph:
  requires:
    - "Phase 4: Attachment system for player state tracking"
  provides:
    - "Enhanced wind charge self-boost (50% Y velocity increase)"
    - "One-time fall damage negation after wind charge boost"
  affects:
    - "Future wind charge modifications"

tech_stack:
  added: []
  patterns:
    - "TAIL injection for post-explosion velocity modification"
    - "HEAD injection with cancellation for fall damage negation"
    - "Boolean attachment for one-time state tracking"

key_files:
  created:
    - src/main/java/thc/mixin/WindChargePlayerBoostMixin.java
    - src/main/java/thc/mixin/PlayerFallDamageMixin.java
  modified:
    - src/main/java/thc/THCAttachments.java
    - src/main/resources/thc.mixins.json

decisions:
  - decision: "Target WindCharge.explode TAIL for boost enhancement"
    rationale: "Allows modification after vanilla explosion applies knockback"
    alternatives: ["@Redirect on knockback calculation", "Event-based approach"]
  - decision: "Target LivingEntity.causeFallDamage for negation"
    rationale: "Method exists in LivingEntity, not ServerPlayer"
    alternatives: ["ServerPlayer.checkFallDamage"]

metrics:
  duration: ~4 min
  completed: 2026-01-19
---

# Phase 13 Plan 02: Wind Charge Player Boost Summary

**One-liner:** Wind charge self-boost enhanced 50% with one-time fall damage negation via TAIL injection on explode and HEAD cancellation on causeFallDamage

## What Was Built

Three interconnected components implementing enhanced wind charge mobility:

### 1. WIND_CHARGE_BOOSTED Attachment (THCAttachments.java)
- Boolean attachment to track wind charge boost state
- Non-persistent (resets on relog - acceptable for temporary state)
- Links boost mixin to fall damage mixin

### 2. WindChargePlayerBoostMixin
- Targets `WindCharge.explode` method with TAIL injection
- Checks if owner (ServerPlayer) is within 4 blocks of explosion
- Only enhances if player has upward velocity (actually affected)
- Multiplies Y velocity by 1.5 (50% boost)
- Sets `hurtMarked = true` for server-client sync
- Sets WIND_CHARGE_BOOSTED attachment to true

### 3. PlayerFallDamageMixin
- Targets `LivingEntity.causeFallDamage` with HEAD injection
- Checks if entity is ServerPlayer with WIND_CHARGE_BOOSTED flag
- If flagged: clears flag and cancels damage (returns true)
- One-time use per boost

## Commits

| Hash | Message |
|------|---------|
| 14ed01a | feat(13-02): add WIND_CHARGE_BOOSTED attachment |
| a6213c4 | feat(13-02): add wind charge self-boost enhancement mixin |
| a6b25cb | feat(13-02): add fall damage negation mixin |

## Decisions Made

### 1. TAIL Injection on WindCharge.explode
**Decision:** Inject at TAIL of explode method to enhance boost after vanilla processing
**Rationale:** The explosion system applies knockback to all nearby entities. By injecting at TAIL, we can read the player's velocity after vanilla has applied the boost, then multiply the Y component by 1.5.
**Alternatives considered:**
- @Redirect on knockback: Would require complex bytecode manipulation
- Event-based: Fabric API doesn't provide wind charge specific events

### 2. Target LivingEntity for Fall Damage
**Decision:** Mixin to LivingEntity.causeFallDamage, not ServerPlayer
**Rationale:** The causeFallDamage method is defined in LivingEntity with signature `(double, float, DamageSource)`. ServerPlayer doesn't override this method.
**Fix applied:** Initially targeted ServerPlayer which caused mixin warning; corrected to LivingEntity.

### 3. 4-block Boost Range
**Decision:** Only enhance boost if player within 4 blocks of explosion center
**Rationale:** Standard wind charge explosion affects entities within this range. Players further away wouldn't be boosted anyway.

## Technical Details

### Velocity Modification Pattern
```java
// After explosion applies vanilla knockback
Vec3 velocity = player.getDeltaMovement();
if (velocity.y > 0.0) {
    double boostedY = velocity.y * 1.5;
    player.setDeltaMovement(velocity.x, boostedY, velocity.z);
    player.hurtMarked = true; // Required for sync
}
```

### Fall Damage Negation Pattern
```java
Boolean boosted = target.getAttached(THCAttachments.WIND_CHARGE_BOOSTED);
if (boosted != null && boosted) {
    target.setAttached(THCAttachments.WIND_CHARGE_BOOSTED, false);
    cir.setReturnValue(true); // "Handled" damage
}
```

## Deviations from Plan

### 1. [Rule 3 - Blocking] Fixed method signature mismatch
**Found during:** Task 3 implementation
**Issue:** Plan specified `causeFallDamage(float, float, DamageSource)` but actual signature is `causeFallDamage(double, float, DamageSource)`. Also method is in LivingEntity, not ServerPlayer.
**Fix:** Updated mixin to target LivingEntity with correct signature
**Files modified:** PlayerFallDamageMixin.java
**Commit:** a6b25cb

## Verification Checklist

- [x] `./gradlew build` succeeds without errors
- [x] THCAttachments.java contains WIND_CHARGE_BOOSTED
- [x] WindChargePlayerBoostMixin.java exists and is registered
- [x] PlayerFallDamageMixin.java exists and is registered
- [x] Both mixins properly reference WIND_CHARGE_BOOSTED attachment
- [x] Key links verified: WindChargePlayerBoostMixin sets attachment, PlayerFallDamageMixin reads and clears it

## Success Criteria Verification

- [x] WIND-02 satisfied: Wind charge self-boost launches player 50% higher (1.5x Y velocity)
- [x] WIND-03 satisfied: Fall damage negated on next landing after self-boost
- [x] Build passes
- [x] All three components (attachment + 2 mixins) properly wired

## Next Phase Readiness

Phase 13 Wind Charge Mobility is now complete:
- 13-01: Wind charge recipe override (12 yield) - Done
- 13-02: Enhanced player boost with fall damage negation - Done

Ready to proceed to Phase 14 or next milestone.
