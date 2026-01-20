---
phase: 16-world-difficulty
plan: 02
subsystem: world
tags: [difficulty, mixin, regional-difficulty, moon-phase]

# Dependency graph
requires:
  - phase: 12-foundation
    provides: Mixin infrastructure and mod initialization
provides:
  - Maximum regional difficulty everywhere
  - Permanent full moon effects
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - HEAD inject to override difficulty calculations

key-files:
  created:
    - src/main/java/thc/mixin/ServerLevelDifficultyMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use 3,600,000 ticks for max inhabited time (50 hour cap)"
  - "Force moon phase factor to 1.0f for permanent full moon"

patterns-established:
  - "Difficulty override: HEAD inject on getCurrentDifficultyAt returning custom DifficultyInstance"

# Metrics
duration: 4min
completed: 2026-01-20
---

# Phase 16 Plan 02: Maximum Regional Difficulty Summary

**Maximum regional difficulty with full moon effects forced everywhere via mixin**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-20
- **Completed:** 2026-01-20
- **Tasks:** 2
- **Files created:** 1
- **Files modified:** 1

## Accomplishments
- Regional difficulty always at maximum in every chunk (WORLD-03)
- Moon phase effects always at maximum/full moon (WORLD-04)
- Mobs spawn with maximum possible equipment and enchantments

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ServerLevelDifficultyMixin for max difficulty** - `7aa458e` (feat)
2. **Task 2: Register ServerLevelDifficultyMixin in mixin config** - `319f82c` (chore)

## Files Created/Modified
- `src/main/java/thc/mixin/ServerLevelDifficultyMixin.java` - Injects at HEAD of getCurrentDifficultyAt to return maximum difficulty
- `src/main/resources/thc.mixins.json` - Added ServerLevelDifficultyMixin registration

## Technical Details

The mixin overrides `ServerLevel.getCurrentDifficultyAt(BlockPos)` to always return a `DifficultyInstance` with:
- **Difficulty:** HARD (base difficulty)
- **Level time:** Current game time (for consistency)
- **Chunk inhabited time:** 3,600,000 ticks (50 hours - the internal cap)
- **Moon phase factor:** 1.0f (full moon, maximum bonus)

This ensures:
- Mobs have highest chance to spawn with armor and weapons
- Mobs have highest chance to have enchanted equipment
- Regional difficulty bonuses are maximized regardless of actual time spent in area

## Decisions Made
- Used maximum inhabited time of 3,600,000 ticks (the internal cap mentioned in vanilla code)
- Set moon phase to 1.0f to simulate permanent full moon for spawning bonuses

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Build system had temporary WSL file caching issues (unrelated to code changes)
- Resolved with clean rebuild

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- WORLD-03 (max regional difficulty) implemented
- WORLD-04 (moon phase always true/full) implemented
- Ready for next plan in phase

---
*Phase: 16-world-difficulty*
*Completed: 2026-01-20*
