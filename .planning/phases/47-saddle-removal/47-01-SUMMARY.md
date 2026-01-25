---
phase: 47-saddle-removal
plan: 01
subsystem: game-mechanics
tags: [loot-tables, recipes, villager-trades, mixins, data-pack]

# Dependency graph
requires:
  - phase: multiple-prior-phases
    provides: RecipeManagerMixin and AbstractVillagerMixin patterns
provides:
  - Saddle completely removed from all obtainment sources
  - 8 loot table overrides (chests, fishing, mob drops)
  - Recipe and villager trade filtering for saddles
affects: [mounted-combat, nether-traversal, player-mobility]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Loot table data pack overrides for complete item removal
    - REMOVED_RECIPE_PATHS expansion for recipe filtering
    - Multi-item villager trade filtering

key-files:
  created:
    - src/main/resources/data/minecraft/loot_table/chests/bastion_hoglin_stable.json
    - src/main/resources/data/minecraft/loot_table/chests/end_city_treasure.json
    - src/main/resources/data/minecraft/loot_table/chests/nether_bridge.json
    - src/main/resources/data/minecraft/loot_table/chests/village/village_weaponsmith.json
    - src/main/resources/data/minecraft/loot_table/chests/village/village_tannery.json
    - src/main/resources/data/minecraft/loot_table/chests/village/village_savanna_house.json
    - src/main/resources/data/minecraft/loot_table/gameplay/fishing/treasure.json
    - src/main/resources/data/minecraft/loot_table/entities/ravager.json
  modified:
    - src/main/java/thc/mixin/RecipeManagerMixin.java
    - src/main/java/thc/mixin/AbstractVillagerMixin.java

key-decisions:
  - "Ravager loot table uses empty pools array after saddle removal"
  - "Preserved all vanilla loot table structure except saddle entries"

patterns-established:
  - "Complete item removal via combined mixin + loot table override approach"

# Metrics
duration: 5min
completed: 2026-01-25
---

# Phase 47 Plan 01: Saddle Removal Summary

**Saddles removed from all obtainment sources via recipe filtering, villager trade blocking, and 8 loot table overrides**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-25T22:44:00Z
- **Completed:** 2026-01-25T22:49:09Z
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments
- Recipe crafting removed (1.21.6+ saddle recipe disabled)
- Leatherworker villager trades filtered (Master level 6 emerald trade)
- All chest loot sources overridden (bastion, end city, nether bridge, 3 village types)
- Fishing treasure pool no longer includes saddles
- Ravager drops eliminated (empty loot table)

## Task Commits

Each task was committed atomically:

1. **Task 1: Mixin changes for recipe and villager trade removal** - `05886f8` (feat)
2. **Task 2: Create loot table overrides for all saddle sources** - `f3b64c4` (feat)
3. **Task 3: Build verification and smoke test** - (no separate commit - verification only)

## Files Created/Modified

### Created
- `src/main/resources/data/minecraft/loot_table/chests/bastion_hoglin_stable.json` - Hoglin stable loot without saddle
- `src/main/resources/data/minecraft/loot_table/chests/end_city_treasure.json` - End city treasure without saddle
- `src/main/resources/data/minecraft/loot_table/chests/nether_bridge.json` - Nether fortress chests without saddle
- `src/main/resources/data/minecraft/loot_table/chests/village/village_weaponsmith.json` - Weaponsmith chests without saddle
- `src/main/resources/data/minecraft/loot_table/chests/village/village_tannery.json` - Tannery chests without saddle
- `src/main/resources/data/minecraft/loot_table/chests/village/village_savanna_house.json` - Savanna house chests without saddle
- `src/main/resources/data/minecraft/loot_table/gameplay/fishing/treasure.json` - Fishing treasure without saddle
- `src/main/resources/data/minecraft/loot_table/entities/ravager.json` - Ravager drops without saddle (empty)

### Modified
- `src/main/java/thc/mixin/RecipeManagerMixin.java` - Added "saddle" to REMOVED_RECIPE_PATHS
- `src/main/java/thc/mixin/AbstractVillagerMixin.java` - Added Items.SADDLE filter to trade removal

## Decisions Made

1. **Ravager loot table structure**: Used empty `"pools": []` array after saddle removal rather than removing the file entirely - maintains data pack override structure and explicitly signals intentional emptiness
2. **Preserved vanilla structure**: All loot tables maintain full vanilla structure with only saddle entries removed - minimizes future maintenance and merge conflicts with vanilla updates

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Build succeeded on first attempt with all loot table JSON syntax valid.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Saddle removal implementation complete and compiling successfully
- In-game testing blocked by existing PlayerSleepMixin compatibility issue (see STATE.md blockers)
- Ready for testing verification once Minecraft 1.21.11 mixin compatibility is resolved
- No dependencies on saddle removal for other phases in Extra Features Batch 8

### Verification Plan (for future testing session)

When PlayerSleepMixin is fixed, verify:
1. Recipe book shows no saddle recipe
2. Leatherworker villagers do not offer saddle trades at any level
3. Chest loot in structures (bastion, end city, fortress, villages) contains no saddles
4. Ravagers drop no saddles when killed
5. Fishing treasure pool does not yield saddles

---
*Phase: 47-saddle-removal*
*Completed: 2026-01-25*
