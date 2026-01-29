---
phase: 61-smithing-table-tier-upgrades
plan: 03
subsystem: crafting
tags: [recipe, smithing-table, copper, progression]

# Dependency graph
requires:
  - phase: 56-copper-overhaul
    provides: Copper items and copper-based crafting ecosystem
provides:
  - Alternative smithing table recipe using copper ingots
  - Earlier access to smithing table in progression
affects: [tier-upgrades, early-game-progression]

# Tech tracking
tech-stack:
  added: []
  patterns: [alternative-crafting-recipes]

key-files:
  created:
    - src/main/resources/data/thc/recipe/smithing_table_copper.json
  modified: []

key-decisions:
  - "Alternative recipe pattern: thc:smithing_table_copper as separate recipe ID (does not override vanilla)"
  - "Same shaped pattern as vanilla (2 ingots top, 4 planks bottom) for consistency"

patterns-established:
  - "Alternative crafting recipes with mod namespace maintain vanilla compatibility"

# Metrics
duration: 2min
completed: 2026-01-29
---

# Phase 61 Plan 03: Copper Smithing Table Recipe Summary

**Alternative smithing table crafting recipe using copper ingots enables earlier progression access**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-29T19:37:44Z
- **Completed:** 2026-01-29T19:39:45Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Created alternative copper-based smithing table recipe
- Verified recipe format and build integration
- Maintained vanilla netherite upgrade compatibility by design

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Copper Smithing Table Recipe and Verify Netherite Compatibility** - `30b2761` (feat)

## Files Created/Modified
- `src/main/resources/data/thc/recipe/smithing_table_copper.json` - Alternative smithing table recipe using copper ingots instead of iron (2 copper ingots top, 4 planks bottom)

## Decisions Made

**Recipe naming pattern:**
- Used `thc:smithing_table_copper` as recipe ID (mod namespace)
- Does NOT override vanilla `minecraft:smithing_table` recipe
- Both recipes remain valid - players can use either copper or iron

**Progression alignment:**
- Copper ingots are more accessible than iron in early game
- Maintains same crafting pattern as vanilla for consistency
- Enables earlier access to smithing functionality

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Recipe created successfully, validated by build system, and included in JAR.

**Netherite upgrade compatibility note:** Per plan design, SmithingMenuMixin (created in plan 61-01 by parallel agent) will not interfere with vanilla netherite upgrades because:
- Diamond items (DIAMOND_HELMET, etc.) are NOT included in TierUpgradeConfig base item maps
- NETHERITE_INGOT is NOT in the addition materials map
- Mixin's isValidTierUpgrade() returns false for diamond+netherite combinations, allowing vanilla logic to handle netherite upgrades

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for continued tier upgrade implementation in phase 61. The copper smithing table recipe provides:
- Alternative crafting path for smithing tables
- Foundation for copper-tier progression
- No conflicts with existing vanilla or modded smithing mechanics

**Dependencies satisfied:**
- Recipe file created and verified in build
- Pattern matches vanilla for player familiarity
- Namespace isolation prevents vanilla conflicts

---
*Phase: 61-smithing-table-tier-upgrades*
*Completed: 2026-01-29*
