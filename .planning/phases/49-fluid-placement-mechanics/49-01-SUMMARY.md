---
phase: 49-fluid-placement-mechanics
plan: 01
subsystem: fluid-mechanics
tags: [mixin, bucket, water, lava, fluid-physics]

dependency-graph:
  requires:
    - 48 (copper bucket implementation)
  provides:
    - BUCK-04 lava bucket placement blocking
    - WATR-01 flowing water placement from copper bucket
    - WATR-02 vanilla flow physics via scheduleTick
  affects:
    - Future water economy mechanics

tech-stack:
  added: []
  patterns:
    - HEAD cancellation for item use interception
    - FlowingFluid.LEVEL for water flow level control
    - FluidState.createLegacyBlock() for proper block placement
    - scheduleTick for fluid physics activation

file-tracking:
  key-files:
    created:
      - src/main/java/thc/mixin/BucketItemLavaMixin.java
    modified:
      - src/main/kotlin/thc/item/CopperWaterBucketItem.kt
      - src/main/resources/thc.mixins.json

decisions: []

metrics:
  duration: 3 min
  completed: 2026-01-26
---

# Phase 49 Plan 01: Fluid Placement Mechanics Summary

**One-liner:** Lava bucket placement blocked via BucketItem mixin, copper water bucket places flowing water at level 8 that drains naturally.

## What Was Built

### Task 1: Block Vanilla Lava Bucket Placement
Created `BucketItemLavaMixin.java` that injects at HEAD of `BucketItem.use()`:
- Checks for `Items.LAVA_BUCKET` in player's hand
- Returns `InteractionResult.FAIL` to silently cancel placement
- Vanilla lava buckets remain in inventory but cannot place lava

### Task 2: Copper Water Bucket Places Flowing Water
Modified `CopperWaterBucketItem.kt`:
- Replaced `Blocks.WATER.defaultBlockState()` with flowing water creation
- Uses `Fluids.FLOWING_WATER.defaultFluidState().setValue(FlowingFluid.LEVEL, 8).createLegacyBlock()`
- Schedules tick for water flow physics activation
- Water spreads 7 blocks horizontally, flows down, and drains naturally

## Technical Approach

### Lava Blocking Strategy
The mixin approach was chosen over custom items because:
- Works with any lava bucket in the game (dungeon chests, trades, etc.)
- No need to replace existing lava buckets in player inventories
- Simple HEAD cancellation pattern with immediate return

### Flowing Water Physics
Level 8 flowing water was selected because:
- Maximum spread distance (7 blocks) before draining
- Falls downward like natural springs
- Uses vanilla physics - no custom tick handling needed
- Creates realistic water behavior without infinite source generation

## Deviations from Plan

None - plan executed exactly as written.

## Commit Log

| Commit | Type | Description |
|--------|------|-------------|
| 50887df | feat | Block vanilla lava bucket placement |
| d5dd40d | feat | Copper water bucket places flowing water |

## Verification Results

- [x] Build passes
- [x] BucketItemLavaMixin.java contains Items.LAVA_BUCKET check
- [x] BucketItemLavaMixin.java returns InteractionResult.FAIL
- [x] BucketItemLavaMixin registered in thc.mixins.json
- [x] CopperWaterBucketItem.kt contains FLOWING_WATER
- [x] CopperWaterBucketItem.kt uses FlowingFluid.LEVEL setValue
- [x] CopperWaterBucketItem.kt calls scheduleTick

## Success Criteria Met

- [x] BUCK-04: Lava bucket placement blocked via HEAD injection returning FAIL
- [x] WATR-01: Copper water bucket places flowing water at level 8
- [x] WATR-02: Placed water uses vanilla physics via scheduleTick

## Key Files

### BucketItemLavaMixin.java
```java
@Mixin(BucketItem.class)
public class BucketItemLavaMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void thc$blockLavaPlacement(...) {
        if (stack.getItem() == Items.LAVA_BUCKET) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
```

### CopperWaterBucketItem.kt (water placement)
```kotlin
val flowingWater = Fluids.FLOWING_WATER
    .defaultFluidState()
    .setValue(FlowingFluid.LEVEL, 8)
    .createLegacyBlock()

level.setBlock(targetPos, flowingWater, 11)
level.scheduleTick(targetPos, Fluids.FLOWING_WATER, Fluids.FLOWING_WATER.getTickDelay(level))
```

## Next Phase Readiness

Phase 49 complete. Fluid placement mechanics fully implemented:
- Lava buckets can no longer place lava
- Copper water buckets place finite water that drains
- Natural water sources (oceans, rivers) remain unaffected
