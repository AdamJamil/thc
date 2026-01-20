---
phase: 14-ranged-weapon-gating
plan: 01
subsystem: recipes
tags: [crafting, gating, trial-chambers, ranged-weapons]

dependency-graph:
  requires:
    - phase-12 (trial chamber XP/loot)
  provides:
    - bow recipe override
    - crossbow recipe override
  affects:
    - phase-14-02 (if exists - arrow recipe)

tech-stack:
  added: []
  patterns:
    - shaped recipe override via datapack

key-files:
  created:
    - src/main/resources/data/minecraft/recipe/bow.json
    - src/main/resources/data/minecraft/recipe/crossbow.json
  modified: []

decisions:
  - id: bow-pattern
    choice: "3 breeze rods + 3 string in bow shape"
    rationale: "Mirrors vanilla pattern, replaces sticks with breeze rods"
  - id: crossbow-pattern
    choice: "3 breeze rods + 1 diamond + tripwire hook + 2 string"
    rationale: "Higher cost reflects crossbow power; diamond adds Trial Chamber progression requirement"

metrics:
  duration: "3 min"
  completed: "2026-01-19"
---

# Phase 14 Plan 01: Ranged Weapon Recipe Overrides Summary

**One-liner:** Bow and crossbow recipes gated behind Trial Chambers via breeze rod requirements.

## What Was Built

Created recipe override JSONs that replace vanilla bow and crossbow crafting recipes:

1. **Bow Recipe** (`bow.json`)
   - Pattern: 3 breeze rods + 3 string
   - Layout mirrors vanilla bow shape
   - Gates basic ranged weapon behind Trial Chamber progression

2. **Crossbow Recipe** (`crossbow.json`)
   - Pattern: 3 breeze rods + 1 diamond + tripwire hook + 2 string
   - Replaces sticks with breeze rods, iron with diamond
   - Higher material cost reflects crossbow's superior capabilities

## Commits

| Hash | Message |
|------|---------|
| 065e63f | feat(14-01): add bow recipe requiring breeze rods |
| cf29f0c | feat(14-01): add crossbow recipe requiring breeze rod and diamond |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed spurious pdnSave file**
- **Found during:** Task 1 verification
- **Issue:** Paint.NET autosave file `honey_apple.png.0.pdnSave` causing build failure
- **Fix:** Deleted the spurious file
- **Files modified:** Deleted `src/main/resources/assets/thc/textures/item/honey_apple.png.0.pdnSave`
- **Commit:** Not committed (file deletion only, unrelated to plan)

## Verification Results

- [x] bow.json exists with breeze_rod ingredient
- [x] crossbow.json exists with breeze_rod and diamond ingredients
- [x] Build passes successfully

## Next Phase Readiness

**Ready for:** 14-02-PLAN.md (arrow recipe gating, if exists)

**No blockers or concerns.**
