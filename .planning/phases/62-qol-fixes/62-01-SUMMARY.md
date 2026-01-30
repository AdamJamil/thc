---
phase: 62-qol-fixes
plan: 01
subsystem: gameplay-polish
tags: [qol, mining-fatigue, bell, poise-meter, hud]
dependency_graph:
  requires: []
  provides: [mining-fatigue-cap, bell-ringing-fix, poise-meter-scaling]
  affects: []
tech_stack:
  added: []
  patterns:
    - "InteractionResult.PASS for vanilla passthrough in UseBlockCallback"
    - "Matrix3x2fStack transformations for HUD icon scaling"
    - "minOf() for capping incremental values"
key_files:
  created: []
  modified:
    - src/main/kotlin/thc/world/MiningFatigue.kt
    - src/main/kotlin/thc/bell/BellHandler.kt
    - src/client/kotlin/thc/client/BucklerHudRenderer.kt
decisions:
  - id: QOL-FATIGUE-01
    choice: "Cap mining fatigue at amplifier 9 (level 10)"
    rationale: "Prevents extreme slowdown while maintaining penalty"
  - id: QOL-BELL-01
    choice: "Use InteractionResult.PASS instead of SUCCESS"
    rationale: "Allows vanilla bell ringing to continue after our handler"
  - id: QOL-POISE-01
    choice: "0.92f scale with 9px spacing"
    rationale: "~8% smaller icons with visible gaps between them"
metrics:
  duration: 3 min
  completed: 2026-01-29
---

# Phase 62 Plan 01: QoL Fixes Summary

**One-liner:** Mining fatigue capped at level 10, bells ring after land plot, poise icons scaled smaller with gaps.

## What Was Built

### Mining Fatigue Cap (MiningFatigue.kt)
Added `MAX_AMPLIFIER = 9` constant and modified `applyFatigue()` to use `minOf(currentAmplifier + 1, MAX_AMPLIFIER)`. This caps the displayed mining fatigue at level 10 regardless of how many blocks the player breaks outside their base.

### Bell Ringing Fix (BellHandler.kt)
Changed all `InteractionResult.SUCCESS` returns to `InteractionResult.PASS`. This allows vanilla to continue processing the bell interaction (ringing sound and animation) after our custom handler runs. Previously, SUCCESS indicated we fully handled the interaction, blocking the vanilla bell ring.

### Poise Meter Scaling (BucklerHudRenderer.kt)
Added `ICON_SCALE = 0.92f` constant and updated the render loop to use matrix transformations:
- `pushMatrix()` / `popMatrix()` for state isolation
- `translate(baseX, top)` for positioning
- `scale(ICON_SCALE, ICON_SCALE)` for 8% size reduction
- `ICON_SPACING = 9` for visible gaps between icons

### XP Bottle Verification (ExperienceOrbXpMixin.java)
Verified the existing mixin already has proper ThrownExperienceBottle detection at line 51:
```java
if (className.contains("ThrownExperienceBottle") || className.contains("ExperienceBottle")) {
    return; // Allow
}
```
No changes needed - the whitelist is correctly in place.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 9182f0a | fix | Cap mining fatigue at level 10 and restore bell ringing |
| 4c5881c | feat | Scale poise meter icons with spacing |

## Deviations from Plan

None - plan executed exactly as written.

## Files Modified

| File | Changes |
|------|---------|
| MiningFatigue.kt | +MAX_AMPLIFIER constant, minOf() cap in applyFatigue() |
| BellHandler.kt | SUCCESS -> PASS for all 3 return statements |
| BucklerHudRenderer.kt | +ICON_SCALE constant, matrix transforms in render loop |

## Verification

- [x] `./gradlew build` passes
- [ ] Visual check: Poise icons smaller with gaps (manual client test)
- [ ] Functional check: Bells ring after first land plot obtained (manual test)
- [ ] Functional check: Mining fatigue stays at level 10 max (manual test)
- [ ] Functional check: XP bottles grant XP when thrown (manual test)

Note: Visual and functional checks require in-game testing during verify-phase.

## Next Phase Readiness

Phase 62 plan 01 complete. No blockers for subsequent phases.
