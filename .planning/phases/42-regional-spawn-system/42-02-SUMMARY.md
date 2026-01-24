---
phase: 42-regional-spawn-system
plan: 02
subsystem: spawn
tags: [pillager-ai, melee-attack, goal-selector, mixin]

dependency-graph:
  requires:
    - phase-42-01-spawn-distribution
  provides:
    - pillager-melee-ai
  affects:
    - phase-44b-end-distribution

tech-stack:
  added: []
  patterns:
    - goal-selector-manipulation
    - equipment-based-variant-detection
    - finalizeSpawn-tail-injection

key-files:
  created:
    - src/main/java/thc/mixin/PillagerMixin.java
  modified:
    - src/main/resources/thc.mixins.json

decisions:
  - id: "42-02-melee-detection"
    choice: "Equipment-based detection via Items.IRON_SWORD check"
    reason: "PillagerVariant.MELEE sets iron sword; detecting equipment is simpler than tracking variant state"
  - id: "42-02-priority"
    choice: "MeleeAttackGoal priority 4"
    reason: "Matches vanilla RangedAttackGoal priority for consistent behavior"
  - id: "42-02-pursuit"
    choice: "pauseWhenMobIdle=false for aggressive pursuit"
    reason: "Melee pillagers should actively chase targets, not pause"

metrics:
  duration: 4min
  completed: 2026-01-24
---

# Phase 42 Plan 02: Pillager Melee AI Modification Summary

PillagerMixin that removes RangedAttackGoal and adds MeleeAttackGoal for iron sword pillager variants, enabling melee combat behavior for sword-wielding pillagers spawned via regional distribution.

## What Was Built

### PillagerMixin.java
Mixin targeting `Pillager.finalizeSpawn` at TAIL to configure melee AI:

**Detection:** Checks mainhand for `Items.IRON_SWORD` to identify melee variants
**Goal Removal:** `goalSelector.getAvailableGoals().removeIf()` removes RangedAttackGoal
**Goal Addition:** `goalSelector.addGoal(4, new MeleeAttackGoal(self, 1.0, false))`

**Key parameters:**
- Priority 4: Same as vanilla RangedAttackGoal
- Speed multiplier 1.0: Normal movement speed
- pauseWhenMobIdle false: Aggressive pursuit behavior

### Timing Integration

The mixin injects at finalizeSpawn TAIL, which runs after:
1. `Mob.populateDefaultEquipmentSlots` (vanilla equipment)
2. `PillagerVariant.applyEquipment` (THC equipment override from SpawnReplacementMixin)
3. `MobFinalizeSpawnMixin` (NBT tagging)

This ensures the iron sword is present when we check, and AI modification happens at the correct lifecycle point.

## Key Implementation Details

**Why this is needed:**
Vanilla pillagers have RangedAttackGoal hardcoded, which only fires if the mob holds a bow or crossbow. When a melee pillager has an iron sword, it becomes passive - the RangedAttackGoal never activates (no ranged weapon), and there's no MeleeAttackGoal to fall back to.

**Integration with Plan 01:**
- SpawnReplacementMixin spawns pillagers via regional distribution
- PillagerVariant.MELEE applies iron sword AFTER finalizeSpawn
- PillagerMixin detects iron sword and reconfigures AI

**Ranged variant preservation:**
Pillagers without iron sword (including vanilla crossbow pillagers) are not modified - vanilla RangedAttackGoal remains intact.

## Commits

| Commit | Type | Description |
|--------|------|-------------|
| 91c5f15 | feat | Create PillagerMixin for melee AI modification |
| dffc92e | docs | Add debug logging and verify integration timing |

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

**Phase 42 complete.** Regional spawn system fully implemented:
- Plan 01: Spawn distribution infrastructure (witch, vex, pillager, blaze, breeze, vindicator, evoker)
- Plan 02: Pillager melee AI for sword variants

**Deferred to Phase 44b:**
- End dimension distribution (25% endermite, 75% vanilla)
