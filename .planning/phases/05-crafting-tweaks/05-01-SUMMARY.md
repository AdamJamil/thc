---
phase: 05-crafting-tweaks
plan: 01
subsystem: crafting
tags: [recipes, mixin, data-pack, snowball, ladder]

# Dependency graph
requires:
  - phase: 04-world-restrictions
    provides: core mod structure and mixin patterns
provides:
  - Ladder recipe override (16 from 7 sticks)
  - Snow block/snowball conversion recipes
  - Snowball stack size increased to 64
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Recipe override via minecraft namespace data pack
    - Item component modification via accessor mixin

key-files:
  created:
    - src/main/resources/data/minecraft/recipe/ladder.json
    - src/main/resources/data/thc/recipe/snow_block_to_snowballs.json
    - src/main/resources/data/thc/recipe/snowballs_to_snow_block.json
    - src/main/java/thc/mixin/SnowballItemMixin.java
    - src/main/java/thc/mixin/access/ItemAccessor.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Accessor mixin for Item component modification"
  - "Recipe override via minecraft namespace"

patterns-established:
  - "Item component modification: Use accessor mixin to replace immutable DataComponentMap"
  - "Vanilla recipe override: Place recipe JSON in data/minecraft/recipe/ with same filename"

# Metrics
duration: 3min
completed: 2026-01-17
---

# Phase 5 Plan 1: Crafting Tweaks Summary

**Ladder recipe yields 16, snowballs stack to 64 with snow block conversion recipes via data pack and mixin**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-17T22:41:00Z
- **Completed:** 2026-01-17T22:44:03Z
- **Tasks:** 4
- **Files modified:** 6

## Accomplishments
- Ladder recipe overridden to yield 16 (up from vanilla 3) for same 7-stick pattern
- Snow block decomposition recipe: 1 block -> 9 snowballs
- Snowball compression recipe: 9 snowballs -> 1 block
- Snowball max stack size increased from 16 to 64 via mixin

## Task Commits

Each task was committed atomically:

1. **Task 1: Override vanilla ladder recipe** - `d0696e2` (feat)
2. **Task 2: Add snow block to snowballs recipe** - `b859406` (feat)
3. **Task 3: Add snowballs to snow block recipe** - `b204084` (feat)
4. **Task 4: Create snowball stack size mixin** - `7f979db` (feat)

## Files Created/Modified
- `src/main/resources/data/minecraft/recipe/ladder.json` - Vanilla recipe override yielding 16 ladders
- `src/main/resources/data/thc/recipe/snow_block_to_snowballs.json` - Shapeless decomposition recipe
- `src/main/resources/data/thc/recipe/snowballs_to_snow_block.json` - Shaped compression recipe
- `src/main/java/thc/mixin/SnowballItemMixin.java` - Modifies SNOWBALL item's MAX_STACK_SIZE component
- `src/main/java/thc/mixin/access/ItemAccessor.java` - Accessor for Item component modification
- `src/main/resources/thc.mixins.json` - Registered new mixins

## Decisions Made
- **Accessor mixin for component modification:** In MC 1.21+, item components are immutable via getter. Created ItemAccessor mixin with @Mutable @Accessor to replace entire component map with modified version.
- **Recipe override via minecraft namespace:** Placing recipe JSON in `data/minecraft/recipe/` with same filename as vanilla recipe automatically overrides it via data pack mechanism.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] DataComponentMap immutability**
- **Found during:** Task 4 (Snowball stack size mixin)
- **Issue:** Initial approach tried calling `.set()` on components(), which returns immutable DataComponentMap
- **Fix:** Created ItemAccessor mixin to access the mutable component field, built new DataComponentMap with modified stack size
- **Files modified:** Created access/ItemAccessor.java, updated SnowballItemMixin.java
- **Verification:** Build passes, mixin applies correctly
- **Committed in:** 7f979db (part of Task 4 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Auto-fix necessary for MC 1.21+ compatibility. No scope creep.

## Issues Encountered
None - recipes use standard JSON format, mixin pattern established in project.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All crafting tweaks complete
- Phase 5 is the final phase per ROADMAP.md
- Mod ready for testing all features together

---
*Phase: 05-crafting-tweaks*
*Completed: 2026-01-17*
