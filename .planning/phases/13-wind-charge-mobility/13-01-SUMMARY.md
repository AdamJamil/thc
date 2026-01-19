---
phase: 13-wind-charge-mobility
plan: 01
subsystem: recipes
tags: [wind-charge, mobility, crafting, datapack]

dependency_graph:
  requires: []
  provides:
    - wind_charge_12x_yield
  affects: []

tech_stack:
  added: []
  patterns:
    - "Data pack recipe override for yield modification"

key_files:
  created:
    - src/main/resources/data/minecraft/recipe/wind_charge.json
  modified: []

decisions: []

metrics:
  duration: "2 min"
  completed: "2026-01-19"
---

# Phase 13 Plan 01: Wind Charge Recipe Override Summary

**One-liner:** Breeze rod yields 12 wind charges via data pack recipe override.

## What Was Done

### Task 1: Override breeze rod to wind charge recipe
**Commit:** cd731a3

Created recipe override that triples wind charge yield from breeze rods:
- Vanilla: 1 breeze rod -> 4 wind charges
- Modified: 1 breeze rod -> 12 wind charges

Implementation uses same pattern as ladder.json - place a recipe JSON with the vanilla filename in `data/minecraft/recipe/` to override the vanilla recipe through Minecraft's data pack system.

## Files Created

| File | Purpose |
|------|---------|
| src/main/resources/data/minecraft/recipe/wind_charge.json | Recipe override for 12 wind charge yield |

## Verification Results

- [x] Recipe JSON exists with count: 12
- [x] Recipe type is crafting_shapeless
- [x] `./gradlew build` succeeds without errors

## Deviations from Plan

None - plan executed exactly as written.

## Test Instructions

1. Launch Minecraft with mod
2. Enter creative mode or obtain breeze rod
3. Open crafting table
4. Place 1 breeze rod
5. Verify output shows 12 wind charges (not vanilla 4)

## Patterns Applied

**Data Pack Recipe Override:**
- Place JSON file matching vanilla recipe filename in `data/minecraft/recipe/`
- Minecraft's resource loading system replaces vanilla recipe with mod version
- Simple, non-invasive approach for recipe modifications

## Next Phase Readiness

Phase 13 complete. Ready for Phase 14 (Ender Pearl Cooldown).
