---
phase: 88-breeze-bow
plan: 01
subsystem: combat
tags: [bow, breeze, support-class, knockback, drag-factor, class-gating]

# Dependency graph
requires:
  - phase: 86-wooden-bow-foundation
    provides: "BowType enum, drag factor system, ProjectileEntityMixin, AbstractArrowMixin damage framework"
  - phase: 87-blaze-bow
    provides: "Blaze Bow item pattern (BlazeBowItem, THCItems registration, draw speed override via releaseUsing)"
provides:
  - "BreezeBowItem with Support Stage 2+ class gate and 0.75x draw speed"
  - "BowType.BREEZE with dragFactor=0.01 for extended horizontal range"
  - "75% damage multiplier for breeze bow arrows"
  - "Knockback preservation for breeze bow arrows (skips velocity zeroing)"
  - "THCBows registration object for custom bow registration"
affects: [future-splash-aoe, bow-balancing]

# Tech tracking
tech-stack:
  added: []
  patterns: ["THCBows registration object for bow items separate from THCItems", "releaseUsing charge scaling for draw speed modification"]

key-files:
  created:
    - src/main/kotlin/thc/bow/BreezeBowItem.kt
    - src/main/kotlin/thc/bow/THCBows.kt
    - src/main/resources/assets/thc/items/breeze_bow.json
    - src/main/resources/assets/thc/models/item/breeze_bow.json
    - src/main/resources/assets/thc/models/item/breeze_bow_pulling_0.json
    - src/main/resources/assets/thc/models/item/breeze_bow_pulling_1.json
    - src/main/resources/assets/thc/models/item/breeze_bow_pulling_2.json
    - src/main/resources/assets/thc/textures/item/breeze_bow.png
    - src/main/resources/assets/thc/textures/item/breeze_bow_iron_pulling_0.png
    - src/main/resources/assets/thc/textures/item/breeze_bow_iron_pulling_1.png
    - src/main/resources/assets/thc/textures/item/breeze_bow_iron_pulling_2.png
    - src/main/resources/data/thc/recipe/breeze_bow.json
  modified:
    - src/main/kotlin/thc/bow/BowType.kt
    - src/main/java/thc/mixin/AbstractArrowMixin.java
    - src/main/kotlin/thc/THC.kt
    - src/main/resources/assets/thc/lang/en_us.json

key-decisions:
  - "THCBows registration object created in thc.bow package (separate from THCItems) for Breeze Bow; Blaze Bow remains in THCItems"
  - "Draw speed 0.75x implemented via releaseUsing charge scaling (divide actual charge by 0.75 = faster power curve progression)"
  - "Knockback preserved by early return in thc$removeArrowKnockback when bow type tag is breeze_bow"

patterns-established:
  - "THCBows registration: custom bow items registered via THCBows object with combat tab placement"
  - "Draw speed override: releaseUsing actualCharge/speedFactor scaling pattern for faster or slower draws"

# Metrics
duration: 2min
completed: 2026-02-13
---

# Phase 88 Plan 01: Breeze Bow Summary

**Support-class Breeze Bow with 75% damage, vanilla knockback, 0.75x draw speed, drag factor 0.01 for extended range, and Support Stage 2+ class gating**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-13T03:52:31Z
- **Completed:** 2026-02-13T03:53:55Z
- **Tasks:** 2
- **Files modified:** 21

## Accomplishments
- Breeze Bow item with Support Stage 2+ class gate ("The bow gusts are beyond your control." for denied players)
- 0.75x draw speed via releaseUsing charge scaling (full draw in 15 ticks vs 20 vanilla)
- BowType.BREEZE with dragFactor=0.01 activates in ProjectileEntityMixin for longer horizontal arrow range
- 75% damage multiplier applied in AbstractArrowMixin bow-type damage system
- Knockback preservation: breeze bow arrows skip velocity zeroing, applying natural arrow knockback to monsters
- Crafting recipe: 3 breeze rods + 3 string in shaped pattern
- Cyan-themed textures with 4 pulling animation states

## Task Commits

Each task was committed atomically:

1. **Task 1: Breeze Bow item, textures, recipe, registration, and drag factor wiring** - `d7f7d5f` (feat)
2. **Task 2: Breeze Bow damage profile (75%) and knockback preservation** - `8e01780` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/bow/BreezeBowItem.kt` - Breeze Bow item with Support Stage 2+ class gate and 0.75x draw speed
- `src/main/kotlin/thc/bow/THCBows.kt` - Bow registration object with BREEZE_BOW (durability 384, combat tab)
- `src/main/kotlin/thc/bow/BowType.kt` - Added BREEZE enum with dragFactor=0.01, fromBowItem mapping
- `src/main/java/thc/mixin/AbstractArrowMixin.java` - 75% damage multiplier and knockback preservation for breeze bow arrows
- `src/main/kotlin/thc/THC.kt` - Added THCBows.init() call
- `src/main/resources/assets/thc/items/breeze_bow.json` - Item definition with pulling state model overrides
- `src/main/resources/assets/thc/models/item/breeze_bow.json` - Base model
- `src/main/resources/assets/thc/models/item/breeze_bow_pulling_0.json` - Pulling state 0 model
- `src/main/resources/assets/thc/models/item/breeze_bow_pulling_1.json` - Pulling state 1 model
- `src/main/resources/assets/thc/models/item/breeze_bow_pulling_2.json` - Pulling state 2 model
- `src/main/resources/assets/thc/textures/item/breeze_bow.png` - Idle texture (cyan bow)
- `src/main/resources/assets/thc/textures/item/breeze_bow_iron_pulling_0.png` - Pulling texture 0
- `src/main/resources/assets/thc/textures/item/breeze_bow_iron_pulling_1.png` - Pulling texture 1
- `src/main/resources/assets/thc/textures/item/breeze_bow_iron_pulling_2.png` - Pulling texture 2
- `src/main/resources/data/thc/recipe/breeze_bow.json` - Shaped recipe (3 breeze rods + 3 string)
- `src/main/resources/assets/thc/lang/en_us.json` - Added "Breeze Bow" display name
- `src/main/kotlin/thc/item/BlazeBowItem.kt` - Blaze Bow prereq (Phase 87)
- `src/main/kotlin/thc/item/THCItems.kt` - Blaze Bow registration (Phase 87 prereq)
- `src/main/resources/assets/thc/items/blaze_bow.json` - Blaze Bow item definition (Phase 87 prereq)
- `src/main/resources/data/thc/recipe/blaze_bow.json` - Blaze Bow recipe (Phase 87 prereq)

## Decisions Made
- THCBows registration object created in `thc.bow` package for Breeze Bow, separate from THCItems where Blaze Bow is registered
- Draw speed 0.75x implemented via `releaseUsing` charge scaling: `actualCharge / 0.75` makes the power curve progress faster, achieving full draw at 15 real ticks
- Knockback preserved by checking bow type tag in `thc$removeArrowKnockback` TAIL inject and returning early for breeze_bow arrows
- Included Phase 87 Blaze Bow prereq files in Task 1 commit (BlazeBowItem, assets, recipe, registration)

## Deviations from Plan

None - plan executed exactly as written. Phase 87 Blaze Bow prerequisites were bundled into the Task 1 commit as they were required dependencies.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Breeze Bow is fully functional and ready for in-game testing
- Splash AoE tipped arrow behavior deferred to a future phase per user decision
- All 7 requirements satisfied: ITEM-05, ITEM-06, DMG-02, DMG-04, MECH-02, GATE-02, PHYS-02

## Self-Check: PASSED

All 16 files verified present. Both task commits (d7f7d5f, 8e01780) verified in git history.

---
*Phase: 88-breeze-bow*
*Completed: 2026-02-13*
