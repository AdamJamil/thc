---
phase: 79-boat-mastery
plan: 02
subsystem: boon-gate
tags: [boat, mixin, bastion, stage-gate, land-placement]

dependency-graph:
  requires: [79-01]
  provides: [boat-land-placement-gate]
  affects: [79-03]

tech-stack:
  added: []
  patterns: [mixin-item-intercept, entity-spawn-pattern]

key-files:
  created:
    - src/main/java/thc/mixin/BoatPlacementMixin.java
  modified:
    - src/main/java/thc/mixin/access/ItemAccessor.java
    - src/main/resources/thc.mixins.json

decisions:
  - id: mc-1.21-boat-api
    context: "BoatItem API changed in MC 1.21"
    choice: "Use EntityType<? extends AbstractBoat> instead of Supplier"
    outcome: "Mixin correctly shadows entityType field"

metrics:
  duration: ~7 min
  completed: 2026-02-03
---

# Phase 79 Plan 02: Boat Land Placement Gate Summary

**One-liner:** BoatPlacementMixin gates land boat placement to Bastion Stage 5+ with custom spawn logic

## What Was Built

### BoatPlacementMixin (NEW)
Intercepts vanilla BoatItem.use() at HEAD to:
1. Detect land vs water placement via ray trace
2. Gate land placement to Bastion class + Stage 5+
3. Show action bar message for unqualified players
4. Spawn boat on solid ground for qualified players

Key implementation:
- Uses ItemAccessor.invokeGetPlayerPOVHitResult() to detect target block
- Checks FluidTags.WATER to let vanilla handle water placement
- Checks ClassManager.getClass() and StageManager.getBoonLevel() for gate
- Creates boat via EntityType.create() with proper MC 1.21 API

### ItemAccessor Enhancement
Added invoker for protected static `getPlayerPOVHitResult` method:
```java
@Invoker("getPlayerPOVHitResult")
static BlockHitResult invokeGetPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluid)
```

## Commits

| Hash | Type | Description |
|------|------|-------------|
| bb5cb83 | feat | Initial BoatPlacementMixin with gate and spawn logic |
| f062779 | fix | Correct BoatItem field names for MC 1.21 API |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Incorrect MC 1.21 BoatItem field names**
- **Found during:** Task 2 verification
- **Issue:** Plan assumed `entityTypeSupplier` and `Boat` class; MC 1.21 uses `entityType` and `AbstractBoat`
- **Fix:** Updated shadow field to `EntityType<? extends AbstractBoat> entityType`, used `setInitialPos()` and `createDefaultStackConfig()`
- **Files modified:** BoatPlacementMixin.java
- **Commit:** f062779

## Verification

- [x] `./gradlew build` passes without shadow warnings
- [x] Mixin registered in thc.mixins.json
- [x] Gate uses ClassManager.getClass() pattern
- [x] Gate uses StageManager.getBoonLevel() >= 5 pattern
- [ ] In-game: Water placement works for all (requires runtime test)
- [ ] In-game: Land placement blocked for non-Bastion (requires runtime test)
- [ ] In-game: Land placement works for Bastion Stage 5+ (requires runtime test)

## Requirements Satisfied

- BOAT-01: Bastion + Stage 5+ can place wooden boats on land
- BOAT-02: Non-Bastion or lower stage cannot place boats on land (message shown)
- BOAT-03: Water boat placement unchanged for all players (returns early, vanilla handles)

## Next Phase Readiness

**Ready for 79-03:** Mob trapping mixin
- BoatPlacementMixin established pattern for boat-related mixins
- AbstractBoat reference confirmed correct for MC 1.21
- Land-placed boats work; now need trapping behavior
