---
phase: 25-furnace-gating
plan: 01
subsystem: crafting
tags: [recipe-gating, progression, nether-requirement]

dependency-graph:
  requires: [24-blast-totem]
  provides: [furnace-gating, blast-furnace-gating]
  affects: []

tech-stack:
  added: []
  patterns:
    - "REMOVED_RECIPE_PATHS: Set-based recipe filtering in RecipeManagerMixin"

key-files:
  created:
    - src/main/resources/data/minecraft/recipe/furnace.json
    - src/main/resources/data/thc/recipe/blast_furnace.json
  modified:
    - src/main/java/thc/mixin/RecipeManagerMixin.java

decisions: []

metrics:
  duration: 8 min
  completed: 2026-01-22
---

# Phase 25 Plan 01: Furnace Gating Summary

**One-liner:** Gate furnace behind Nether access (blaze powder) and blast furnace behind Evoker fight (blast totem) via recipe removal and replacement.

## What Was Done

### Task 1: Remove vanilla furnace and blast furnace recipes
- Added "furnace" and "blast_furnace" to `REMOVED_RECIPE_PATHS` set in `RecipeManagerMixin.java`
- Existing mixin infrastructure filters these recipes during recipe loading
- **Commit:** `99a04c2`

### Task 2: Create custom furnace recipe with blaze powder
- Created `data/minecraft/recipe/furnace.json` shaped recipe
- Pattern: 3x3 grid with cobblestone surrounding blaze powder center
- Uses `minecraft:cobblestone` (C) and `minecraft:blaze_powder` (B)
- Result: `minecraft:furnace` (count 1)
- **Commit:** `c7653ca`

### Task 3: Create custom blast furnace recipe with blast totem
- Created `data/thc/recipe/blast_furnace.json` shapeless recipe
- Ingredients: `minecraft:furnace` + `thc:blast_totem`
- Result: `minecraft:blast_furnace` (count 1)
- **Commit:** `1435e0e`

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

None - implementation followed established patterns exactly.

## Technical Notes

The furnace recipe uses minecraft namespace (`data/minecraft/recipe/`) to override the vanilla recipe at the same path, while blast furnace uses thc namespace (`data/thc/recipe/`) since it introduces a mod-specific ingredient.

Progression flow:
1. Player must reach Nether to obtain blaze powder
2. Blaze powder + cobblestone = furnace
3. Player must defeat Evoker to obtain blast totem (via Phase 24)
4. Furnace + blast totem = blast furnace

This creates meaningful risk gates:
- Smelting requires Nether travel risk
- Efficient smelting requires mansion/raid Evoker fight risk

## Next Phase Readiness

Furnace gating complete. No blockers for future phases.
