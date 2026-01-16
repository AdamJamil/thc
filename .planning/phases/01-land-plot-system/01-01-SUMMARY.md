---
phase: 01-land-plot-system
plan: 01
subsystem: items
tags: [minecraft, fabric, items, economy, villagers]

# Dependency graph
requires:
  - phase: foundation
    provides: "Mod initialization structure (THC.kt, mixins)"
provides:
  - "Land plot book item (non-stackable currency)"
  - "Bell trade removal from villagers"
  - "Item registration pattern (THCItems object)"
affects: [01-02, 01-03, economy, land-claiming]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Item registration via THCItems object following THCBucklers pattern"
    - "Mixin injection for trade filtering"

key-files:
  created:
    - src/main/kotlin/thc/item/LandPlotItem.kt
    - src/main/kotlin/thc/item/THCItems.kt
    - src/main/resources/assets/thc/textures/item/land_plot.png
    - src/main/resources/assets/thc/models/item/land_plot.json
    - src/main/resources/assets/thc/lang/en_us.json
  modified:
    - src/main/kotlin/thc/THC.kt
    - src/main/java/thc/mixin/AbstractVillagerMixin.java

key-decisions:
  - "Land plot book is non-stackable (stacksTo(1)) to maintain scarcity"
  - "Bells removed from all villager trades to force world exploration"
  - "Item added to tools creative tab for accessibility"

patterns-established:
  - "THCItems object follows THCBucklers registration pattern"
  - "Trade filtering extends existing AbstractVillagerMixin"

# Metrics
duration: 4min
completed: 2026-01-16
---

# Phase 01 Plan 01: Land Plot Item Foundation Summary

**Land plot book item with brown book texture, registered via THCItems, bells removed from villager trades**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-16T04:28:03Z
- **Completed:** 2026-01-16T04:32:19Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments
- Created land plot book item as non-stackable currency
- Removed bells from all villager trading offers
- Established item registration pattern for future land plot economy items
- Full asset pipeline (texture, model, localization) functional

## Task Commits

Each task was committed atomically:

1. **Task 1: Create land plot book item with registration** - `0be8a12` (feat)
2. **Task 2: Remove bells from villager trades** - `6641cf6` (feat)
3. **Task 3: Add item assets (texture, model, localization)** - `a99a7a8` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/item/LandPlotItem.kt` - Land plot book item class
- `src/main/kotlin/thc/item/THCItems.kt` - Item registration object
- `src/main/kotlin/thc/THC.kt` - Added THCItems.init() call
- `src/main/java/thc/mixin/AbstractVillagerMixin.java` - Added bell filtering
- `src/main/resources/assets/thc/textures/item/land_plot.png` - 16x16 brown book texture
- `src/main/resources/assets/thc/models/item/land_plot.json` - Item model definition
- `src/main/resources/assets/thc/lang/en_us.json` - "Land Plot" localization

## Decisions Made
- Land plot book made non-stackable (stacksTo(1)) to enforce scarcity and value
- Added to tools creative tab (following game convention for utility items)
- Used simple book texture (brown) as placeholder - can be customized later
- Bells filtered from villager trades via existing mixin pattern

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all tasks completed smoothly, build succeeded on first attempt after fixing import.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for Plan 02 (Bell-to-Land-Plot Conversion):
- Land plot item exists and can be spawned with `/give @p thc:land_plot`
- Bells are unavailable from villagers (must be found naturally)
- Item infrastructure ready for bell interaction logic

**Blockers:** None

**Concerns:** None

---
*Phase: 01-land-plot-system*
*Completed: 2026-01-16*
