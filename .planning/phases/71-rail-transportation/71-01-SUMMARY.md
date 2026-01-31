---
phase: 71
plan: 01
subsystem: recipes
tags: [rail, crafting, yield, copper, transportation]
dependency-graph:
  requires: []
  provides: [rail-recipes, powered-rail-recipes]
  affects: []
tech-stack:
  added: []
  patterns: [vanilla-recipe-override, alternative-recipe-namespace]
key-files:
  created:
    - src/main/resources/data/minecraft/recipe/rail.json
    - src/main/resources/data/minecraft/recipe/powered_rail.json
    - src/main/resources/data/thc/recipe/rail_copper.json
  modified: []
decisions:
  - id: rail-yield-64
    choice: "64 rails per craft (4x vanilla)"
    reason: "Rail networks need high volume, vanilla 16 too expensive"
  - id: powered-rail-yield-64
    choice: "64 powered rails per craft (~10x vanilla)"
    reason: "Powered rails even more critical, vanilla 6 prohibitively expensive"
  - id: copper-alternative
    choice: "Copper as alternative to iron for rails"
    reason: "Copper abundant and underutilized, early-game accessibility"
metrics:
  duration: 2 min
  tasks: 1/1
  completed: 2026-01-31
---

# Phase 71 Plan 01: Rail Recipe Overrides Summary

**One-liner:** Rail recipes with 64 yield and copper alternative for accessible transportation infrastructure.

## What Was Built

Three recipe JSON files that modify rail crafting:

1. **rail.json** - Overrides vanilla iron rail recipe to yield 64 instead of 16
2. **powered_rail.json** - Overrides vanilla powered rail recipe to yield 64 instead of 6
3. **rail_copper.json** - New alternative recipe using copper instead of iron

## Why This Approach

Rail transportation is fundamental to base infrastructure but vanilla yields are prohibitively low:
- Vanilla rails: 6 iron + 1 stick = 16 rails
- Vanilla powered rails: 6 gold + 1 stick + 1 redstone = 6 powered rails

A simple rail line needs hundreds of rails. This change makes rail networks practical while keeping the same material ratios.

Copper alternative uses the proven pattern from dough recipes - alternative ingredient in thc namespace producing vanilla items.

## Key Files

| File | Purpose |
|------|---------|
| `data/minecraft/recipe/rail.json` | Iron rail recipe, 64 yield |
| `data/minecraft/recipe/powered_rail.json` | Powered rail recipe, 64 yield |
| `data/thc/recipe/rail_copper.json` | Copper rail alternative, 64 yield |

## Implementation Details

All recipes use `category: misc` matching vanilla rail categorization. The pattern structure matches vanilla exactly - only the count changes.

## Verification

- Build successful
- All three recipe files validated
- Success criteria met:
  - RAIL-01: Copper rails via rail_copper.json
  - RAIL-02: 64 rail yield via rail.json
  - RAIL-03: 64 powered rail yield via powered_rail.json

## Commits

| Hash | Description |
|------|-------------|
| 697258d | feat(71-01): add rail and powered rail recipe overrides |

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

Phase 71 complete. All rail transportation requirements implemented.
