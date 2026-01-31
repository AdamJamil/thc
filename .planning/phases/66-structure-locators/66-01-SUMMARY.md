---
phase: 66-structure-locators
plan: 01
completed: 2026-01-31
duration: 6 min

subsystem: items
tags: [compass, structure-search, lodestone-tracker, locator]

dependency-graph:
  requires: []
  provides:
    - "StructureLocatorItem base class with inventoryTick search"
    - "6 registered locator items for nether/overworld structures"
  affects:
    - "66-02 (item models and textures)"
    - "Phase 68 (cartographer trades using locators)"

tech-stack:
  added: []
  patterns:
    - "lodestone_tracker component with tracked=false"
    - "inventoryTick for periodic server-side structure search"
    - "TagKey creation for structures without StructureTags constants"

key-files:
  created:
    - src/main/kotlin/thc/item/StructureLocatorItem.kt
  modified:
    - src/main/kotlin/thc/item/THCItems.kt

decisions:
  - id: SLOC-01
    decision: "Use custom TagKey for structures without StructureTags constants"
    rationale: "Fortress, bastion, pillager_outpost, ancient_city lack built-in tags"

metrics:
  tasks: 2
  duration: 6 min
---

# Phase 66 Plan 01: Structure Locator Items Summary

**One-liner:** StructureLocatorItem base class with lodestone_tracker compass behavior and 6 registered locator items for Nether/Overworld structures.

## What Was Built

Created the core structure locator system with two components:

1. **StructureLocatorItem base class** - Compass-style item that uses `inventoryTick` to periodically search for structures using `ServerLevel.findNearestMapStructure()`. Updates `lodestone_tracker` component with `tracked=false` to leverage vanilla compass rendering.

2. **6 registered locator items** covering both dimensions:
   - **Nether:** FORTRESS_LOCATOR, BASTION_LOCATOR
   - **Overworld:** TRIAL_CHAMBER_LOCATOR, PILLAGER_OUTPOST_LOCATOR, ANCIENT_CITY_LOCATOR, STRONGHOLD_LOCATOR

## Key Implementation Details

### StructureLocatorItem.kt

```kotlin
class StructureLocatorItem(
    properties: Properties,
    private val structureTag: TagKey<Structure>,
    private val expectedDimension: ResourceKey<Level>
) : Item(properties) {

    override fun inventoryTick(
        stack: ItemStack,
        serverLevel: ServerLevel,
        entity: Entity,
        slot: EquipmentSlot?
    ) {
        if (entity !is Player) return
        if (serverLevel.gameTime % 20L != 0L) return  // Throttle

        // Wrong dimension = spinning needle
        if (serverLevel.dimension() != expectedDimension) {
            clearTarget(stack)
            return
        }

        val found = serverLevel.findNearestMapStructure(
            structureTag, entity.blockPosition(), 100, false
        )
        // Update lodestone_tracker with tracked=false
    }
}
```

### Structure Tags

- **Built-in tags used:** `StructureTags.ON_TRIAL_CHAMBERS_MAPS`, `StructureTags.EYE_OF_ENDER_LOCATED`
- **Custom TagKeys created:** fortress, bastion_remnant, pillager_outpost, ancient_city

## API Discovery

MC 1.21.11 changed `inventoryTick` signature from `(ItemStack, Level, Entity, Int, Boolean)` to `(ItemStack, ServerLevel, Entity, EquipmentSlot?)`. No client-side check needed since ServerLevel is passed directly.

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 344ddd3 | feat | Create StructureLocatorItem base class |
| 0ae12a3 | feat | Register 6 structure locator items |

## Files Changed

**Created:**
- `src/main/kotlin/thc/item/StructureLocatorItem.kt` (90 lines)

**Modified:**
- `src/main/kotlin/thc/item/THCItems.kt` (+84 lines)

## Next Phase Readiness

Phase 66-02 can proceed with item models and textures. The 6 locator items are registered and functional (structure search works), but need:
- `items/*.json` model definitions with compass range_dispatch
- `models/item/*.json` for 16 angles per locator
- `textures/item/*.png` for each locator type
