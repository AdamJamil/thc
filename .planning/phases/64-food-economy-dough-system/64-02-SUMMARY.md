---
phase: 64-food-economy-dough-system
plan: 02
subsystem: loot-tables
tags: [leather, loot-table, pig, sheep, farm-animals]

# Dependency graph
requires:
  - phase: none
    provides: vanilla loot table format reference
provides:
  - Pigs drop leather (0-2 base, +1 per looting level)
  - Sheep drop leather (0-2 base, +1 per looting level)
  - Normalized leather sources across farm animals
affects: [leather-economy, armor-crafting]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Loot table data pack override for entity drops"
    - "Leather pool structure matching cow rates exactly"

key-files:
  created:
    - data/minecraft/loot_table/entities/pig.json
    - data/minecraft/loot_table/entities/sheep.json
  modified: []

key-decisions:
  - "FOOD-LEATHER-01: Leather pool as first pool in loot tables (before meat/wool pools)"

patterns-established:
  - "Farm animal leather drops: Copy cow.json leather pool exactly for consistency"

# Metrics
duration: 5min
completed: 2026-01-30
---

# Phase 64 Plan 02: Leather Drops for Pigs and Sheep Summary

**Pigs and sheep now drop leather at cow rates (0-2 base, +1 per looting), normalizing leather availability across farm animals**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-30
- **Completed:** 2026-01-30
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Pigs drop leather (0-2 base, +1 per looting level) alongside porkchop
- Sheep drop leather (0-2 base, +1 per looting level) alongside mutton and wool
- All 16 wool colors preserved for sheep drops
- Leather drop rates match cow rates exactly for consistency

## Task Commits

Each task was committed atomically:

1. **Task 1: Add leather drops to pig loot table** - `c2b6eb0` (feat)
2. **Task 2: Add leather drops to sheep loot table** - `d635b9f` (feat)

## Files Created/Modified
- `data/minecraft/loot_table/entities/pig.json` - Added leather pool matching cow rates, preserves porkchop pool
- `data/minecraft/loot_table/entities/sheep.json` - Added leather pool matching cow rates, preserves mutton and 16 wool color pools

## Decisions Made
- FOOD-LEATHER-01: Placed leather pool as first pool in loot tables (before existing meat/wool pools) for consistency with cow.json structure

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Transient gradle build failures (daemon stopped, remapJar file issues) resolved with clean build - not related to JSON changes

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Leather now available from pigs, sheep, and cows (3 sources)
- Players have more leather farming options
- Ready for additional food economy changes

---
*Phase: 64-food-economy-dough-system*
*Completed: 2026-01-30*
