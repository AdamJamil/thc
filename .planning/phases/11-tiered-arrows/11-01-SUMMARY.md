---
phase: 11-tiered-arrows
plan: 01
subsystem: resources
tags: [arrow, translation, model, texture]

dependency-graph:
  requires: []
  provides: [flint-arrow-rename, vanilla-arrow-texture-override]
  affects: [11-02, 11-03, 11-04]

tech-stack:
  added: []
  patterns:
    - minecraft-namespace-override

key-files:
  created:
    - src/main/resources/assets/minecraft/models/item/arrow.json
  modified:
    - src/main/resources/assets/thc/lang/en_us.json

decisions: []

metrics:
  duration: 2 min
  completed: 2026-01-19
---

# Phase 11 Plan 01: Flint Arrow Rename Summary

Vanilla arrow renamed to "Flint Arrow" with custom texture via resource pack override.

## What Was Done

### Task 1: Translation Override
Added `item.minecraft.arrow` translation key to lang file overriding vanilla's "Arrow" name.

**Files modified:**
- `src/main/resources/assets/thc/lang/en_us.json`

**Commit:** 7fa5539

### Task 2: Model Override
Created minecraft namespace model override pointing vanilla arrow to flint_arrow texture.

**Files created:**
- `src/main/resources/assets/minecraft/models/item/arrow.json`

**Commit:** ad01afe

## Verification Results

- [x] ./gradlew build succeeds
- [x] Lang file contains `item.minecraft.arrow` key
- [x] Arrow model exists in minecraft namespace with `thc:item/flint_arrow` texture reference

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

Phase 11-01 complete. Ready for:
- 11-02: Iron Arrow item and recipe
- 11-03: Diamond Arrow item and recipe
- 11-04: Netherite Arrow item and recipe

All three can proceed in parallel (wave 2).
