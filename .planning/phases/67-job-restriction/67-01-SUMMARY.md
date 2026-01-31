---
phase: 67-job-restriction
plan: 01
subsystem: villager
tags: [mixin, villager, profession, restriction]

dependency-graph:
  requires: []
  provides: [profession-restriction-layer, allowed-professions-helper]
  affects: [67-02, 68-custom-trades]

tech-stack:
  added: []
  patterns: [setVillagerData-interception, registry-access-holder-lookup]

files:
  created:
    - src/main/java/thc/villager/AllowedProfessions.java
    - src/main/java/thc/mixin/VillagerProfessionMixin.java
  modified:
    - src/main/resources/thc.mixins.json

decisions:
  - id: registry-holder-access
    choice: "Use registryAccess().lookupOrThrow().getOrThrow() for Holder lookup"
    reason: "BuiltInRegistries doesn't have direct getHolderOrThrow in MC 1.21"
    alternatives: ["BuiltInRegistries.wrapAsHolder (doesn't work with Optional return)"]

metrics:
  duration: 5m
  completed: 2026-01-31
---

# Phase 67 Plan 01: Profession Restriction Layer Summary

Profession restriction at data layer via setVillagerData interception with AllowedProfessions validation

## What Was Built

### AllowedProfessions Helper Class
Created `src/main/java/thc/villager/AllowedProfessions.java`:
- ALLOWED set with 6 professions: MASON, LIBRARIAN, BUTCHER, CARTOGRAPHER, NONE, NITWIT
- DISALLOWED_JOB_BLOCKS set with 12 blocks for all blocked job sites
- `isAllowed(ResourceKey)` - validates profession against allowed set
- `isDisallowedJobBlock(Block)` - checks if block grants disallowed profession
- `getNoneHolder(RegistryAccess)` - retrieves NONE profession Holder from registry

### VillagerProfessionMixin
Created `src/main/java/thc/mixin/VillagerProfessionMixin.java`:
- Mixin targets `Villager.class`
- Injects at HEAD of `setVillagerData()` with cancellable=true
- Extracts profession key via `holder.unwrapKey().orElse(null)`
- Forces disallowed professions to NONE and cancels original call
- Covers all profession change vectors: AI job acquisition, NBT loading, commands, zombie cures

## Task Commits

| Task | Name | Commit | Key Changes |
|------|------|--------|-------------|
| 1 | AllowedProfessions helper | fff8679 | New villager package, helper class with constants and validation |
| 2 | VillagerProfessionMixin | 22680f4 | Mixin + registration in thc.mixins.json |

## Verification Results

1. `./gradlew build` - PASSED
2. VillagerProfessionMixin registered in thc.mixins.json - VERIFIED
3. AllowedProfessions.ALLOWED has 6 professions - VERIFIED
4. AllowedProfessions.DISALLOWED_JOB_BLOCKS has 12 blocks - VERIFIED

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Registry API compatibility**
- **Found during:** Task 1
- **Issue:** `BuiltInRegistries.VILLAGER_PROFESSION.getHolderOrThrow()` doesn't exist in MC 1.21
- **Fix:** Changed to `registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow()` which requires passing RegistryAccess from the mixin caller
- **Files modified:** AllowedProfessions.java
- **Commit:** Part of fff8679

## Requirements Addressed

- VJOB-01: Profession restriction (villagers blocked from disallowed professions)
- VJOB-02: Jobless conversion (disallowed professions become NONE)
- VJOB-04: Zombie cure handling (cured villagers with disallowed professions become jobless)

## Next Phase Readiness

Phase 67-02 (POI blocking for job blocks) can now proceed. The `AllowedProfessions.isDisallowedJobBlock()` method is ready for use in extending `ServerLevelPoiMixin`.
