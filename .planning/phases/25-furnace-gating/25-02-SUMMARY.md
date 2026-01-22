---
phase: 25-furnace-gating
plan: 02
subsystem: world-gen
tags: [structure-generation, mixin, furnace-gating, village]

dependency-graph:
  requires: [25-01]
  provides: [furnace-structure-filtering]
  affects: []

tech-stack:
  added: []
  patterns:
    - "@Redirect setBlock for structure block filtering"

key-files:
  created:
    - src/main/java/thc/mixin/StructureTemplateMixin.java
  modified:
    - src/main/resources/thc.mixins.json

decisions:
  - id: redirect-setblock
    choice: Use @Redirect on ServerLevelAccessor.setBlock in placeInWorld
    reason: Most direct interception point - catches every block placement during structure generation

metrics:
  duration: 12 min
  completed: 2026-01-22
---

# Phase 25 Plan 02: Village Structure Filtering Summary

**One-liner:** Mixin intercepts structure generation to skip furnace/blast furnace placement in villages and other structures.

## What Was Done

### Task 1: Create StructureTemplateMixin to filter furnaces from structure placement
- Created `StructureTemplateMixin.java` targeting `StructureTemplate.class`
- Defined `FILTERED_STRUCTURE_BLOCKS` set containing `Blocks.FURNACE` and `Blocks.BLAST_FURNACE`
- Used `@Redirect` on `ServerLevelAccessor.setBlock` call within `placeInWorld` method
- Redirect returns `true` (pretending success) when block is in filter set, effectively skipping placement
- **Commit:** `bd6c886`

### Task 2: Register StructureTemplateMixin
- Added `"StructureTemplateMixin"` to the mixins array in `thc.mixins.json`
- Mixin is server-side (not client) since structure generation happens on server
- **Commit:** `9a746cd`

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

1. **@Redirect on setBlock** - The most direct approach. By intercepting the actual block placement call, we ensure no furnace blocks are placed regardless of structure type. Alternative approaches like @Inject with locals or @ModifyVariable on the block list would be more complex.

## Technical Notes

The mixin intercepts every `setBlock` call during structure template placement:

```java
@Redirect(
    method = "placeInWorld",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
    )
)
private boolean thc$filterFurnaceBlocks(ServerLevelAccessor level, BlockPos pos, BlockState state, int flags) {
    if (FILTERED_STRUCTURE_BLOCKS.contains(state.getBlock())) {
        return true; // Skip placement but report success
    }
    return level.setBlock(pos, state, flags);
}
```

This works for any structure using StructureTemplate, including:
- Villages (armorer houses, blacksmiths)
- Any future structures with furnaces
- Custom structures using templates

## Next Phase Readiness

The furnace gating system is now complete:
- Plan 01: Recipe modifications gate furnace crafting behind Nether (blaze powder)
- Plan 02: Structure filtering prevents obtaining furnaces from villages
- Players must visit the Nether to smelt items
