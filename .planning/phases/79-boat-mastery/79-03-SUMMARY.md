---
phase: 79
plan: 03
subsystem: boons/bastion
tags: [boat, mob-trapping, mixin, crowd-control]
dependency-graph:
  requires: [79-02]
  provides: [boat-trapping-mechanic, hostile-mob-breakout]
  affects: []
tech-stack:
  added: []
  patterns: [mixin-tick-injection, uuid-tick-tracking, accessor-invoker]
key-files:
  created:
    - src/main/java/thc/mixin/BoatTrappingMixin.java
  modified:
    - src/main/java/thc/mixin/access/AbstractBoatAccessor.java
    - src/main/resources/thc.mixins.json
decisions:
  - key: mob-tracking
    choice: "UUID-to-tick Map in mixin"
    reason: "Simple tracking without NBT persistence (4 second trap is ephemeral)"
  - key: breakout-behavior
    choice: "Eject all passengers, drop boat as item, discard entity"
    reason: "Boat is reusable (drops as item), not destroyed"
  - key: scope-filter
    choice: "instanceof Boat check excludes IronBoat"
    reason: "Iron boat is lava transport, not mob trap"
metrics:
  duration: "3.5 min"
  completed: "2026-02-03"
---

# Phase 79 Plan 03: Hostile Mob Boat Trapping Summary

Hostile mob trapping in vanilla boats with 4-second timed breakout.

## What Was Done

### Task 1: Boat Trapping Mixin
- Created `BoatTrappingMixin.java` injecting into `AbstractBoat.tick()`
- Tracks hostile mob (MobCategory.MONSTER) passengers via UUID-to-tick Map
- Auto-ejects mob after 80 ticks (4 seconds) with boat drop
- Added `invokeGetDropItem()` to AbstractBoatAccessor for protected method access
- Registered mixin in thc.mixins.json

### Task 2: Build Verification
- Full build passes with all Phase 79 mixins active
- Verified all 5 boat-related mixins registered:
  - IronBoatLavaMixin (existing)
  - IronBoatPassengerMixin (existing)
  - BoatPlacementMixin (79-02)
  - BoatStackSizeMixin (79-01)
  - BoatTrappingMixin (this plan)

## Implementation Details

**Trap Mechanics:**
```java
// Track hostile mob entry
if (passenger instanceof Mob mob &&
    mob.getType().getCategory() == MobCategory.MONSTER) {
    thc$trappedSince.putIfAbsent(mob.getUUID(), currentTick);
}

// Check for breakout (80 ticks = 4 seconds)
if (boardTick != null && currentTick - boardTick >= 80) {
    // Eject all, drop boat, discard entity
}
```

**Scope Filtering:**
- Only vanilla `Boat` class (not `IronBoat`)
- Server-side only (`!level.isClientSide()`)
- Only hostile mobs (`MobCategory.MONSTER`)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] getDropItem() protected access**
- **Found during:** Task 1 compilation
- **Issue:** AbstractBoat.getDropItem() is protected, cannot call from mixin
- **Fix:** Added `@Invoker("getDropItem")` to AbstractBoatAccessor
- **Files modified:** AbstractBoatAccessor.java
- **Commit:** 1c5793a (included in main commit)

## Requirements Satisfied

- BOAT-04: Hostile mobs can be trapped inside boats
- BOAT-05: Hostile mobs break out after 4 seconds
- BOAT-06: Boat drops as item when mob breaks out (reusable)

## Phase 79 Complete Summary

All Boat Mastery boon features implemented:

| Plan | Feature | Status |
|------|---------|--------|
| 79-01 | Boats stack to 16 | Done |
| 79-01 | Boats require copper to craft | Done |
| 79-02 | Bastion Stage 5+ land placement | Done |
| 79-03 | Hostile mob trapping | Done |
| 79-03 | 4-second breakout timer | Done |
| 79-03 | Boat drops as item on breakout | Done |

## Commits

| Hash | Description |
|------|-------------|
| 1c5793a | feat(79-03): implement hostile mob boat trapping with 4-second breakout |

## Testing Notes

To verify in-game:
1. Push zombie into boat (vanilla boarding behavior)
2. Count to 4 seconds
3. Zombie breaks out, boat drops as item
4. Can attack trapped mob while in boat (vanilla behavior)
5. Iron boat does NOT trap mobs (filtered out)
