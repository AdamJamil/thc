---
phase: 87-blaze-bow
plan: 01
subsystem: combat
tags: [bow, fire, blaze-rod, class-gate, ranged, draw-speed]

# Dependency graph
requires:
  - phase: 86-wooden-bow-foundation
    provides: "BowType enum, BowTypeTagAccess interface, ProjectileEntityMixin bow tagging, AbstractArrowMixin damage framework"
provides:
  - "BlazeBowItem with Ranged Stage 2+ class gate and 1.5x draw speed"
  - "Blaze Bow crafting recipe (3 blaze rods + 3 string)"
  - "Fire-on-hit mechanic for blaze_bow tagged arrows (3 seconds)"
  - "Flaming arrow visual for blaze_bow arrows in flight"
  - "BowType.BLAZE enum entry with bow identification"
affects: [88-breeze-bow]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Custom BowItem subclass with class gate and draw speed scaling via releaseUsing override"]

key-files:
  created:
    - src/main/kotlin/thc/item/BlazeBowItem.kt
    - src/main/resources/assets/thc/items/blaze_bow.json
    - src/main/resources/assets/thc/models/item/blaze_bow.json
    - src/main/resources/assets/thc/models/item/blaze_bow_pulling_0.json
    - src/main/resources/assets/thc/models/item/blaze_bow_pulling_1.json
    - src/main/resources/assets/thc/models/item/blaze_bow_pulling_2.json
    - src/main/resources/data/thc/recipe/blaze_bow.json
  modified:
    - src/main/kotlin/thc/item/THCItems.kt
    - src/main/kotlin/thc/bow/BowType.kt
    - src/main/java/thc/mixin/AbstractArrowMixin.java
    - src/main/resources/assets/thc/lang/en_us.json

key-decisions:
  - "Draw speed 1.5x via releaseUsing charge scaling (dividing actualCharge by 1.5f) rather than overriding getUseDuration"
  - "Fire-on-hit via setRemainingFireTicks(60) -- refreshes on re-hit naturally"
  - "Flaming arrow visual via one-time tick inject with thc$blazeFireApplied boolean guard"
  - "100% damage for blaze_bow arrows -- no entry in bow damage multiplier chain defaults to 1.0"

patterns-established:
  - "Custom bow class gate pattern: override use() with ClassManager/StageManager check, return FAIL with actionbar message"
  - "Draw speed scaling pattern: override releaseUsing, scale actualCharge by dividing by speed factor, pass adjustedTimeLeft to super"

# Metrics
duration: 2min
completed: 2026-02-13
---

# Phase 87 Plan 01: Blaze Bow Summary

**Blaze Bow item with Ranged Stage 2+ class gate, 1.5x slow draw, fire-on-hit (3 sec), and 100% damage arrows**

## Performance

- **Duration:** 2 min (documentation-only -- code was pre-committed with Phase 88)
- **Started:** 2026-02-13T03:52:24Z
- **Completed:** 2026-02-13T03:54:00Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- BlazeBowItem with Ranged Stage 2+ class gate denying non-Ranged/low-stage players with "The bow burns your fragile hands." actionbar message
- 1.5x slower draw speed via releaseUsing charge scaling (30 real ticks = 20 vanilla ticks for full power)
- Fire-on-hit for blaze_bow tagged arrows: target set on fire for 3 seconds (1.5 HP total fire damage)
- Flaming arrow visual in flight via AbstractArrowMixin tick inject
- Full item pipeline: registration, recipe (3 blaze rods + 3 string), textures (idle + 3 pulling states), lang entry

## Task Commits

Code was pre-committed alongside Phase 88 (Breeze Bow) execution, which included Blaze Bow as a prerequisite:

1. **Task 1: Create BlazeBowItem with class gate and slow draw, register item with textures and recipe** - `d7f7d5f` (feat)
2. **Task 2: Add fire-on-hit for Blaze Bow arrows** - `8e01780` (feat)

**Plan metadata:** (this commit) (docs: complete blaze bow plan)

## Files Created/Modified
- `src/main/kotlin/thc/item/BlazeBowItem.kt` - Custom BowItem with Ranged Stage 2+ gate and 1.5x draw speed
- `src/main/kotlin/thc/item/THCItems.kt` - BLAZE_BOW registration (durability 384, combat tab)
- `src/main/kotlin/thc/bow/BowType.kt` - BLAZE entry with fromBowItem recognition
- `src/main/java/thc/mixin/AbstractArrowMixin.java` - Fire-on-hit (3 sec) and flaming arrow visual
- `src/main/resources/assets/thc/items/blaze_bow.json` - Item definition with pulling state overrides
- `src/main/resources/assets/thc/models/item/blaze_bow.json` - Idle model
- `src/main/resources/assets/thc/models/item/blaze_bow_pulling_0.json` - Pulling state 0
- `src/main/resources/assets/thc/models/item/blaze_bow_pulling_1.json` - Pulling state 1
- `src/main/resources/assets/thc/models/item/blaze_bow_pulling_2.json` - Pulling state 2
- `src/main/resources/data/thc/recipe/blaze_bow.json` - 3 blaze rods + 3 string shaped recipe
- `src/main/resources/assets/thc/lang/en_us.json` - "Blaze Bow" entry

## Decisions Made
- Draw speed scaling via `releaseUsing` override dividing charge by 1.5f -- `getUseDuration` change would not affect draw speed
- Fire-on-hit uses `setRemainingFireTicks(60)` which naturally refreshes on re-hit
- Flaming arrow visual via one-time tick inject with `thc$blazeFireApplied` boolean guard (set once, arrow burns for life)
- 100% damage: blaze_bow not added to damage multiplier if/else chain, defaults to `bowDamageMultiplier = 1.0`

## Deviations from Plan

None - plan executed exactly as written. Code was implemented during Phase 88 execution which included Blaze Bow prerequisites.

## Issues Encountered

Code was already committed as part of Phase 88 (Breeze Bow) execution, which included all Blaze Bow files as prerequisites. Both commits (`d7f7d5f`, `8e01780`) contain the complete Blaze Bow implementation alongside Breeze Bow code. This summary documents the already-committed work retroactively.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Blaze Bow complete, Breeze Bow (Phase 88) already committed
- v3.4 Bow Overhaul milestone ready for finalization

## Self-Check: PASSED

All 12 files verified present. Both commits (d7f7d5f, 8e01780) verified in git log.

---
*Phase: 87-blaze-bow*
*Completed: 2026-02-13*
