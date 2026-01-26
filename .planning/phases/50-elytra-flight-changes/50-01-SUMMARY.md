---
phase: 50-elytra-flight-changes
plan: 01
subsystem: elytra
tags: [mixin, velocity, elytra, firework, flight]
dependency-graph:
  requires: []
  provides: [firework-boost-cancellation, pitch-velocity-multipliers]
  affects: []
tech-stack:
  added: []
  patterns: [@Redirect on setDeltaMovement, HEAD+TAIL velocity capture on travel]
key-files:
  created:
    - src/main/java/thc/mixin/FireworkRocketEntityMixin.java
    - src/main/java/thc/mixin/PlayerElytraMixin.java
  modified:
    - src/main/resources/thc.mixins.json
decisions:
  - id: pitch-sign
    choice: "pitch >= 0 uses diving multiplier"
    reason: "Neutral pitch (horizon) is treated as diving since player is not ascending"
metrics:
  duration: 3min
  completed: 2026-01-26
---

# Phase 50 Plan 01: Elytra Flight Changes Summary

**One-liner:** Skill-based elytra flight via firework boost cancellation and pitch-based 2x/1.8x velocity delta multipliers.

## What Was Built

### FireworkRocketEntityMixin
Intercepts the `setDeltaMovement(Vec3)` call in `FireworkRocketEntity.tick()` that would normally boost an attached gliding player. For players, the velocity addition is silently cancelled; non-player entities still receive the boost if applicable. The firework still fires visually and explodes - only the velocity change to the player is prevented.

### PlayerElytraMixin
Uses HEAD+TAIL injection pattern on `LivingEntity.travel()` to:
1. **HEAD**: Capture velocity before vanilla elytra physics
2. **TAIL**: Calculate delta, apply pitch-based multiplier, set new velocity

Pitch determines multiplier:
- `pitch >= 0` (diving or neutral): 2x multiplier on velocity delta
- `pitch < 0` (ascending): 1.8x multiplier on velocity delta

The `hurtMarked = true` flag ensures client receives velocity sync.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 3f7c50e | feat | Block firework rocket boost during elytra flight |
| 855d030 | feat | Apply pitch-based velocity multipliers during elytra flight |

## Technical Details

### Injection Strategy
- **Firework mixin**: `@Redirect` is ideal here because we need to conditionally cancel or allow the exact `setDeltaMovement` call. There's only one such call in the gliding code path.
- **Elytra mixin**: HEAD+TAIL pattern on `travel()` allows capturing before/after state without modifying vanilla physics logic. The multiplier amplifies natural glide behavior rather than replacing it.

### Why LivingEntity Target
The elytra physics happen in `LivingEntity.travelFallFlying()` which is called from `travel()`. Targeting `travel()` gives us a clean entry/exit point that captures all elytra velocity changes, including the complex physics in `updateFallFlyingMovement()`.

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

| Check | Result |
|-------|--------|
| Build succeeds | PASS |
| Both mixins registered | PASS |
| FireworkRocketEntityMixin has @Redirect | PASS |
| PlayerElytraMixin has HEAD+TAIL with hurtMarked | PASS |

## Files Changed

```
src/main/java/thc/mixin/FireworkRocketEntityMixin.java (new)
src/main/java/thc/mixin/PlayerElytraMixin.java (new)
src/main/resources/thc.mixins.json (modified)
```

## Next Phase Readiness

Phase 50 complete. Elytra flight now rewards skill-based diving over firework spam. Ready for Phase 51.
