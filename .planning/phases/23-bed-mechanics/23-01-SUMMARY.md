---
phase: 23-bed-mechanics
plan: 01
subsystem: twilight-gameplay
tags: [mixin, player, sleep, spawn, serverLevel]

dependency_graph:
  requires:
    - "Phase 17: Time mechanics (doDaylightCycle enabled)"
    - "Phase 18: Client visual override (perpetual dusk)"
    - "Phase 22: Villager twilight behavior"
  provides:
    - "BED-01: 24/7 bed usage"
    - "BED-02: Time skip prevention"
    - "BED-03: Spawn point still works"
  affects:
    - "Future phases: None (final phase of v2.0)"

tech_stack:
  added: []
  patterns:
    - "BedRule redirect for sleep restriction bypass"
    - "GameRules redirect for time skip prevention"
    - "EnvironmentAttribute interception pattern"

key_files:
  created:
    - src/main/java/thc/mixin/PlayerSleepMixin.java
    - src/main/java/thc/mixin/ServerLevelSleepMixin.java
  modified:
    - src/main/resources/thc.mixins.json

decisions:
  - id: bed-redirect-approach
    choice: "Redirect BedRule lookup in startSleepInBed"
    reason: "Cleanest approach - replaces time check rule with ALWAYS rule"
  - id: time-skip-approach
    choice: "Redirect ADVANCE_TIME gamerule check in sleep block"
    reason: "Prevents time skip while preserving wake-up and other mechanics"

metrics:
  duration: 3.5 min
  completed: 2026-01-21
---

# Phase 23 Plan 01: Bed Mechanics Summary

**One-liner:** BedRule redirect for 24/7 sleep with GameRules redirect preventing time skip.

## What Was Built

Two mixins enabling beds to work as spawn point setters in the perpetually hostile twilight hardcore world:

### 1. PlayerSleepMixin (82 lines)

**Target:** `ServerPlayer.startSleepInBed`
**Method:** `@Redirect` on `EnvironmentAttributeSystem.getValue()`

Intercepts the BedRule lookup and replaces the default `CAN_SLEEP_WHEN_DARK` rule with a custom rule:
- `canSleep = ALWAYS` - No time-of-day restriction
- `canSetSpawn = ALWAYS` - Spawn point always set
- `explodes = false` - Normal bed behavior
- `errorMessage = empty` - No custom error

Preserves all other bed checks:
- Monsters nearby (NOT_SAFE)
- Bed obstruction (OBSTRUCTED)
- Distance check (TOO_FAR_AWAY)
- Dimension behavior (still explodes in Nether/End via dimension's BedRule)

### 2. ServerLevelSleepMixin (77 lines)

**Target:** `ServerLevel.tick`
**Method:** `@Redirect` on `GameRules.get()` (ordinal=0)

Intercepts the ADVANCE_TIME gamerule check in the sleep handling block. When all players are sleeping, vanilla would:
1. Check if ADVANCE_TIME gamerule is true
2. If true, call `setDayTime()` to skip to morning
3. Wake up all players

By returning false for ADVANCE_TIME in this context:
- Time does NOT skip to morning
- Players still wake up naturally after sleep duration
- Weather reset still works (separate check)
- Spawn points still set (handled in bed interaction)

## Technical Decisions

### BedRule Redirect Approach

The 1.21+ bed mechanics use an `EnvironmentAttributeSystem` that provides `BedRule` objects. The default overworld rule is `CAN_SLEEP_WHEN_DARK` which checks `level.isDarkOutside()`.

Rather than:
- Modifying the level's time reporting (breaks other features)
- Cancelling the method and reimplementing (fragile)
- Redirecting `isDarkOutside()` (too broad)

We redirect the BedRule lookup itself, providing a custom rule that always allows sleep while preserving the entire vanilla bed validation chain.

### Time Skip Prevention

The sleep time skip in `ServerLevel.tick()` is gated by `GameRules.ADVANCE_TIME`. By redirecting this specific check to return false, we prevent only the sleep-triggered time skip while:
- Preserving normal day/night cycle progression (separate `tickTime()` method)
- Allowing players to complete sleep animation
- Maintaining weather reset functionality

## Commits

| Commit | Type | Description |
|--------|------|-------------|
| d0b26fd | feat | Allow bed usage at any time of day |
| a0d98df | feat | Prevent time skip when players sleep |
| b324c9d | chore | Register bed mechanics mixins |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] `./gradlew build` succeeds
- [x] PlayerSleepMixin.java exists with startSleepInBed injection (82 lines, min 25)
- [x] ServerLevelSleepMixin.java exists with time skip prevention (77 lines, min 20)
- [x] Both mixins registered in thc.mixins.json
- [x] No compilation errors or warnings

## Next Phase Readiness

This is the **final phase** of v2.0 Twilight Hardcore milestone.

The complete twilight hardcore system is now functional:
- Time flows normally (Phase 17)
- Client sees perpetual dusk (Phase 18)
- Monsters never burn in sunlight (Phase 18)
- Monsters spawn in daylight (Phase 18)
- Villagers behave as always night (Phase 22)
- Beds work 24/7 for spawn points (Phase 23)
- Sleeping doesn't skip past the danger (Phase 23)

**v2.0 Twilight Hardcore is COMPLETE.**
