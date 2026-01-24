---
phase: 42-regional-spawn-system
plan: 01
subsystem: spawn
tags: [spawn-distribution, weighted-random, pillager-variants, regional-spawning]

dependency-graph:
  requires:
    - phase-41-nbt-spawn-tagging
  provides:
    - regional-spawn-distribution
    - pillager-variants
    - pack-spawning
  affects:
    - phase-42-02-regional-caps
    - phase-44b-end-distribution

tech-stack:
  added: []
  patterns:
    - weighted-random-selection
    - pack-spawning-with-group-data
    - variant-equipment-application

key-files:
  created:
    - src/main/java/thc/spawn/SpawnDistributions.java
    - src/main/java/thc/spawn/PillagerVariant.java
  modified:
    - src/main/java/thc/mixin/SpawnReplacementMixin.java

decisions:
  - id: "42-01-region-detection"
    choice: "canSeeSky(pos) for surface detection"
    reason: "FR-18 spec requires isSkyVisible semantics - any sky visibility means surface"
  - id: "42-01-pack-spawning"
    choice: "1-4 pack size with SpawnGroupData threading"
    reason: "Matches vanilla pack spawning behavior, ensures consistent equipment/behavior"
  - id: "42-01-equipment-timing"
    choice: "Apply equipment AFTER finalizeSpawn"
    reason: "populateDefaultEquipmentSlots in finalizeSpawn would overwrite early equipment"

metrics:
  duration: 12min
  completed: 2026-01-24
---

# Phase 42 Plan 01: Regional Spawn Distribution Infrastructure Summary

Regional spawn distribution with weighted random selection for custom mobs (witch, vex, pillager, blaze, breeze, vindicator, evoker) across Overworld zones. Custom spawns bypass vanilla conditions, spawn in packs of 1-4.

## What Was Built

### SpawnDistributions.java
Weighted random selection system with three distribution tables:
- **OW_SURFACE**: 5% witch, 95% vanilla
- **OW_UPPER_CAVE**: 5% witch, 2% vex, 10% ranged pillager, 25% melee pillager, 58% vanilla
- **OW_LOWER_CAVE**: 8% blaze, 8% breeze, 12% vindicator, 25% melee pillager, 2% evoker, 45% vanilla

Tables validated to sum to 100% in static initializer. `selectMob(region, random)` returns `MobSelection` record with type, variant, and vanilla flag.

### PillagerVariant.java
Enum for pillager equipment variants:
- **MELEE**: Iron sword in main hand, 0% drop chance, clear offhand
- **RANGED**: No-op (vanilla crossbow preserved)

### SpawnReplacementMixin.java Updates
Integrated regional distribution into existing spawn replacement:
1. Region detection using `canSeeSky(pos)` per FR-18
2. Roll `SpawnDistributions.selectMob()` before surface variant replacement
3. Pack spawning with `SpawnGroupData` threading through members
4. `SpawnPlacements.isSpawnPositionOk()` for collision checks
5. Pillager equipment applied AFTER `finalizeSpawn`

## Key Implementation Details

**Region Detection Priority:**
```
canSeeSky(pos) -> OW_SURFACE
else Y < 0    -> OW_LOWER_CAVE
else          -> OW_UPPER_CAVE
```

**Spawn Priority Order:**
1. Base chunk blocking (NaturalSpawnerMixin HEAD) - cancels claimed chunk spawns
2. Regional distribution roll (this redirect) - custom mob selection
3. Surface variant replacement - only on vanilla fallback

**Pack Spawning:**
- Size: 1 + random.nextInt(4) for [1,4] uniform
- Offset: random (-5 to +5) on X/Z from previous position
- Collision: `isSpawnPositionOk()` check per member, skip on fail
- GroupData: Threaded through all pack members for consistent equipment

**NBT Integration:**
Custom mobs go through `finalizeSpawn()` which triggers `MobFinalizeSpawnMixin`, automatically setting SPAWN_REGION and SPAWN_COUNTED attachments.

## Commits

| Commit | Type | Description |
|--------|------|-------------|
| 1c9c7ea | feat | Create spawn distribution infrastructure |
| f431e55 | feat | Integrate regional distribution into spawn replacement |
| 1803754 | docs | Document NBT integration and dimension handling |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed Pillager import path**
- **Found during:** Task 1
- **Issue:** Pillager class in MC 1.21.11 is at `monster.illager.Pillager`, not `monster.Pillager`
- **Fix:** Updated import to `net.minecraft.world.entity.monster.illager.Pillager`
- **Files modified:** PillagerVariant.java
- **Commit:** 1c9c7ea

## Next Phase Readiness

**Phase 42-02 (Regional Caps) requirements met:**
- SPAWN_REGION attachment set for all custom mobs
- SPAWN_COUNTED attachment set for monsters
- Region detection consistent between distribution and tagging

**Deferred to Phase 44b:**
- End dimension distribution (25% endermite, 75% vanilla)
- Requires separate distribution table and NBT tagging update
