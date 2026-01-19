---
phase: 11
plan: 03
subsystem: items
tags: [anvil, crafting, arrows, mixin]

dependency_graph:
  requires: [11-02]
  provides: [anvil-arrow-crafting]
  affects: [11-04]

tech_stack:
  added: []
  patterns:
    - "Anvil recipe interception via createResult HEAD injection"
    - "@Shadow for DataSlot/Container/ResultContainer access"

key_files:
  created:
    - src/main/java/thc/mixin/AnvilMenuMixin.java
  modified:
    - src/main/resources/thc.mixins.json

decisions:
  - id: anvil-head-injection
    choice: "HEAD injection with cancellation for custom anvil recipes"
    why: "Intercept before vanilla logic to handle custom recipe, cancel to prevent vanilla from overwriting result"
  - id: level-cost-scaling
    choice: "1/2/3 levels for iron/diamond/netherite"
    why: "Matches material tier progression, netherite most expensive"

metrics:
  duration: 4 min
  completed: 2026-01-19
---

# Phase 11 Plan 03: Anvil Crafting Summary

Anvil recipe interception for tiered arrow crafting using HEAD injection on createResult with @Shadow field access.

## What Was Built

### AnvilMenuMixin
Created `src/main/java/thc/mixin/AnvilMenuMixin.java`:
- HEAD injection on `createResult` method
- Detects 64 arrows in left slot + material in right slot
- Creates appropriate tiered arrow stack
- Sets XP level cost (1/2/3 for iron/diamond/netherite)
- Cancels vanilla logic when recipe matches

### Recipe Table

| Left Slot | Right Slot | Result | XP Cost |
|-----------|------------|--------|---------|
| 64 Arrow | 1 Iron Ingot | 64 Iron Arrow | 1 level |
| 64 Arrow | 1 Diamond | 64 Diamond Arrow | 2 levels |
| 64 Arrow | 1 Netherite Ingot | 64 Netherite Arrow | 3 levels |

## Task Completion

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | Create AnvilMenuMixin | b38127e | AnvilMenuMixin.java |
| 2 | Register mixin | 968be57 | thc.mixins.json |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed constructor signature incompatibility**
- **Found during:** Task 1
- **Issue:** Plan's approach extended ItemCombinerMenu with constructor, but constructor signature changed in 1.21.11
- **Fix:** Removed class extension, used @Shadow annotations for inputSlots, resultSlots, and cost fields
- **Files modified:** AnvilMenuMixin.java
- **Commit:** b38127e

## Implementation Details

The mixin uses a simple pattern:
1. Shadow the fields needed (cost DataSlot, inputSlots Container, resultSlots ResultContainer)
2. Inject at HEAD of createResult
3. Check if recipe matches (64 arrows + material)
4. If match: set result, set cost, cancel vanilla logic
5. If no match: return early, let vanilla handle it

## Next Phase Readiness

**Ready for 11-04:** Flint arrow recipe implementation can proceed independently.
- AnvilMenuMixin only handles arrow upgrades, doesn't conflict with flint arrow crafting
- THCArrows items are registered and available

## Verification

- [x] ./gradlew build succeeds
- [x] AnvilMenuMixin exists with createResult injection
- [x] Mixin registered in thc.mixins.json
- [x] Mixin imports THCArrows and uses registered arrow items
