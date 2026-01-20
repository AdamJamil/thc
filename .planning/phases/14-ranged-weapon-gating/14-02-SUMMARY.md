# Phase 14 Plan 02: Bow/Crossbow Loot Removal Summary

Extended removedItems set to filter bows and crossbows from all loot drops.

## What Was Built

Added Items.BOW and Items.CROSSBOW to the existing removedItems set in THC.kt. This leverages the existing LootTableEvents.MODIFY_DROPS handler that already filters all items in this set from every loot drop.

## Key Changes

1. **THC.kt removedItems Extension**
   - Added Items.BOW to removedItems set
   - Added Items.CROSSBOW to removedItems set
   - No additional handler code needed - existing MODIFY_DROPS handler processes all items in set

## Files Modified

| File | Changes |
|------|---------|
| src/main/kotlin/thc/THC.kt | Extended removedItems set with BOW and CROSSBOW |

## Commits

| Hash | Message |
|------|---------|
| 2a5c54a | feat(14-02): add bow and crossbow to removedItems loot filter |

## Loot Sources Blocked

This single change blocks all acquisition paths:
- Skeleton drops (bow)
- Stray drops (bow)
- Pillager drops (crossbow)
- Piglin barter (crossbow)
- All chest loot tables (villages, dungeons, bastion remnants, etc.)

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] Items.BOW is in removedItems set
- [x] Items.CROSSBOW is in removedItems set
- [x] `./gradlew build` succeeds without errors

## Duration

~1 minute
