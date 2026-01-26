---
phase: 48-copper-bucket
plan: 01
subsystem: items
tags: [bucket, copper, item-registration, fabric-events, cow-milking]

# Dependency graph
requires:
  - phase: none
    provides: none (standalone feature)
provides:
  - Copper bucket item with water-only pickup restriction
  - Copper bucket of water with placement mechanics
  - Copper bucket of milk with drinking/effect clearing
  - Cow milking via UseEntityCallback
  - Crafting recipe for copper bucket
affects: [future-bucket-features, cauldron-interactions]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Custom bucket item extending Item (not BucketItem) for fluid restriction"
    - "UseEntityCallback for entity-specific item interactions"
    - "FluidTags.WATER check for selective fluid pickup"

key-files:
  created:
    - src/main/kotlin/thc/item/CopperBucketItem.kt
    - src/main/kotlin/thc/item/CopperWaterBucketItem.kt
    - src/main/kotlin/thc/item/CopperMilkBucketItem.kt
    - src/main/resources/data/thc/recipe/copper_bucket.json
    - src/main/resources/assets/thc/models/item/copper_bucket.json
    - src/main/resources/assets/thc/models/item/copper_bucket_of_water.json
    - src/main/resources/assets/thc/models/item/copper_bucket_of_milk.json
  modified:
    - src/main/kotlin/thc/item/THCItems.kt
    - src/main/kotlin/thc/THC.kt
    - src/main/resources/assets/thc/lang/en_us.json

key-decisions:
  - "Custom Item classes instead of extending BucketItem (BucketItem hardcoded for specific fluids)"
  - "InteractionResult.SUCCESS instead of sidedSuccess (MC 1.21.11 API change)"
  - "Cow import path net.minecraft.world.entity.animal.cow.Cow (MC 1.21.11 animal subpackage)"

patterns-established:
  - "Custom bucket pattern: Extend Item, override use(), check FluidState with FluidTags"
  - "Entity interaction pattern: UseEntityCallback.EVENT for custom item + entity combinations"
  - "Cow class import path in MC 1.21.11: net.minecraft.world.entity.animal.cow.Cow"

# Metrics
duration: 5min
completed: 2026-01-26
---

# Phase 48 Plan 01: Copper Bucket Implementation Summary

**Copper bucket as early-game water transport with explicit lava restriction via custom Item classes and UseEntityCallback cow milking**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-26T00:59:08Z
- **Completed:** 2026-01-26T01:04:24Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Three copper bucket item variants (empty, water, milk) with correct stack sizes
- Water-only fluid pickup with silent fail on lava/other fluids
- Cow milking via UseEntityCallback producing copper milk bucket
- Crafting recipe using 3 copper ingots in bucket pattern

## Task Commits

Each task was committed atomically:

1. **Task 1: Create copper bucket items and registration** - `07d0738` (feat)
2. **Task 2: Wire cow milking callback and create assets** - `9290fd3` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/item/CopperBucketItem.kt` - Empty bucket with water-only pickup
- `src/main/kotlin/thc/item/CopperWaterBucketItem.kt` - Water placement returning empty bucket
- `src/main/kotlin/thc/item/CopperMilkBucketItem.kt` - Drinkable milk clearing all effects
- `src/main/kotlin/thc/item/THCItems.kt` - Item registrations with stack sizes 16/1/1
- `src/main/kotlin/thc/THC.kt` - UseEntityCallback for cow milking
- `src/main/resources/data/thc/recipe/copper_bucket.json` - Crafting recipe
- `src/main/resources/assets/thc/models/item/copper_bucket*.json` - Item models (3 files)
- `src/main/resources/assets/thc/lang/en_us.json` - Translations

## Decisions Made
- Used `InteractionResult.SUCCESS` instead of `sidedSuccess()` - MC 1.21.11 API difference from research patterns
- Cow class path `net.minecraft.world.entity.animal.cow.Cow` - MC 1.21.11 moved Cow to cow subpackage

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed InteractionResult API for MC 1.21.11**
- **Found during:** Task 1 (Item classes)
- **Issue:** Research suggested `InteractionResult.sidedSuccess()` which doesn't exist in MC 1.21.11
- **Fix:** Changed to `InteractionResult.SUCCESS` matching IronBoatItem pattern
- **Files modified:** CopperBucketItem.kt, CopperWaterBucketItem.kt
- **Verification:** Build succeeds
- **Committed in:** 07d0738 (Task 1 commit)

**2. [Rule 3 - Blocking] Fixed Cow import path for MC 1.21.11**
- **Found during:** Task 2 (THC.kt cow milking)
- **Issue:** `net.minecraft.world.entity.animal.Cow` doesn't exist - Cow moved to cow subpackage
- **Fix:** Changed import to `net.minecraft.world.entity.animal.cow.Cow`
- **Files modified:** THC.kt
- **Verification:** Build succeeds
- **Committed in:** 9290fd3 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes necessary for compilation. No scope creep.

## Issues Encountered
None beyond the blocking issues auto-fixed above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Copper bucket fully functional for water pickup, placement, cow milking, and effect clearing
- Ready for in-game testing via `./gradlew runClient`
- Textures already exist (confirmed at start of execution)

---
*Phase: 48-copper-bucket*
*Completed: 2026-01-26*
