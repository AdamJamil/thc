---
phase: 61
plan: 02
subsystem: smithing
tags: [smithing-table, tier-upgrades, tools, recipes]
requires: [61-01]
provides:
  - Tool tier upgrade system (wooden->stone->copper->iron->diamond)
  - 20 smithing recipes for tool upgrades
  - TOOL_MATERIAL_COUNTS configuration
  - Tool upgrade validation in existing mixin
affects: []
dependencies:
  required_by_plans: []
  blocks_plans: []
tech_stack:
  added: []
  patterns:
    - Smithing recipe data packs for tool tier upgrades
    - Material count maps for tool upgrade validation
decisions:
  - id: SMTH-TOOL-01
    choice: "Wooden->Stone uses smooth_stone (not cobblestone)"
    rationale: "Matches crafting-equivalent feel per research recommendation"
    alternatives: ["cobblestone (cheaper but less intuitive)"]
key_files:
  created:
    - src/main/resources/data/thc/recipe/wooden_to_stone_pickaxe.json
    - src/main/resources/data/thc/recipe/wooden_to_stone_axe.json
    - src/main/resources/data/thc/recipe/wooden_to_stone_shovel.json
    - src/main/resources/data/thc/recipe/wooden_to_stone_hoe.json
    - src/main/resources/data/thc/recipe/wooden_to_stone_sword.json
    - src/main/resources/data/thc/recipe/stone_to_copper_pickaxe.json
    - src/main/resources/data/thc/recipe/stone_to_copper_axe.json
    - src/main/resources/data/thc/recipe/stone_to_copper_shovel.json
    - src/main/resources/data/thc/recipe/stone_to_copper_hoe.json
    - src/main/resources/data/thc/recipe/stone_to_copper_sword.json
    - src/main/resources/data/thc/recipe/copper_to_iron_pickaxe.json
    - src/main/resources/data/thc/recipe/copper_to_iron_axe.json
    - src/main/resources/data/thc/recipe/copper_to_iron_shovel.json
    - src/main/resources/data/thc/recipe/copper_to_iron_hoe.json
    - src/main/resources/data/thc/recipe/copper_to_iron_sword.json
    - src/main/resources/data/thc/recipe/iron_to_diamond_pickaxe.json
    - src/main/resources/data/thc/recipe/iron_to_diamond_axe.json
    - src/main/resources/data/thc/recipe/iron_to_diamond_shovel.json
    - src/main/resources/data/thc/recipe/iron_to_diamond_hoe.json
    - src/main/resources/data/thc/recipe/iron_to_diamond_sword.json
  modified:
    - src/main/kotlin/thc/smithing/TierUpgradeConfig.kt
metrics:
  duration: "3 min"
  completed: 2026-01-29
---

# Phase 61 Plan 02: Tool Tier Upgrades Summary

**One-liner:** Tool tier upgrades via smithing table with crafting-equivalent material costs

## What Was Built

Extended the tier upgrade system from Plan 01 to support tool upgrades across the full wooden->stone->copper->iron->diamond progression.

### Configuration

Extended `TierUpgradeConfig.kt`:
- Added `TOOL_MATERIAL_COUNTS` map with crafting-equivalent material counts:
  - Pickaxe/Axe: 3 material
  - Shovel: 1 material
  - Hoe/Sword: 2 material
- Extended `VALID_TIER_UPGRADES` with 20 tool upgrade paths
- Updated `getRequiredMaterialCount()` to check both armor and tool maps

### Recipes

Created 20 smithing_transform recipes covering all tool tier progressions:
- **Wooden -> Stone** (5 tools): Uses smooth_stone as upgrade material
- **Stone -> Copper** (5 tools): Uses copper_ingot as upgrade material
- **Copper -> Iron** (5 tools): Uses iron_ingot as upgrade material
- **Iron -> Diamond** (5 tools): Uses diamond as upgrade material

All tool types supported: pickaxe, axe, shovel, hoe, sword

## How It Works

### Upgrade Flow

1. Player places tool in base slot of smithing table
2. Player places upgrade material (smooth_stone, copper_ingot, iron_ingot, or diamond) in addition slot
3. Smithing recipe shows upgraded tool as result
4. SmithingMenuMixin (from 61-01) validates material count:
   - Checks `TierUpgradeConfig.getRequiredMaterialCount()` for base tool
   - Verifies player has sufficient materials in inventory
5. On item take, mixin consumes required material count
6. Upgraded tool has full durability and preserved enchantments

### Material Count Validation

`TierUpgradeConfig.getRequiredMaterialCount()` now checks both maps:
```kotlin
fun getRequiredMaterialCount(baseItem: Item): Int {
    return ARMOR_MATERIAL_COUNTS[baseItem] ?: TOOL_MATERIAL_COUNTS[baseItem] ?: 0
}
```

This allows the existing SmithingMenuMixin to work for both armor and tools without modification.

### Enchantment Preservation

Handled automatically by smithing_transform recipe type and SmithingMenuMixin component copying logic from 61-01. All enchantments transfer from old tool to new tool.

## Technical Implementation

### Smooth Stone Choice

Wooden->Stone upgrades use `smooth_stone` instead of `cobblestone`:
- Matches the "crafting-equivalent" feel (smooth stone requires extra processing)
- Players must smelt cobblestone, creating intentional progression friction
- Aligns with research recommendation from 61-RESEARCH

### Recipe Structure

All recipes follow consistent pattern:
```json
{
  "type": "minecraft:smithing_transform",
  "template": "minecraft:barrier",
  "base": "minecraft:{tier}_{tool}",
  "addition": "minecraft:{material}",
  "result": {
    "id": "minecraft:{next_tier}_{tool}"
  }
}
```

Barrier block serves as dummy template (mixin bypasses template requirement).

## Verification Results

- ✅ `./gradlew build` succeeds
- ✅ TierUpgradeConfig.kt compiles with TOOL_MATERIAL_COUNTS
- ✅ 20 recipe JSON files created
- ✅ All tool types covered (pickaxe, axe, shovel, hoe, sword)
- ✅ All tiers covered (wooden->stone, stone->copper, copper->iron, iron->diamond)

## Success Criteria Met

- ✅ SMTH-04: Wooden tools upgrade to stone using smooth stone
- ✅ SMTH-05: Stone tools upgrade to copper using copper ingots
- ✅ SMTH-06: Copper tools upgrade to iron using iron ingots
- ✅ SMTH-07: Iron tools upgrade to diamond using diamonds
- ✅ SMTH-08: Enchantments preserved (automatic via smithing_transform)
- ✅ SMTH-09: Durability restored (mixin from Plan 01)

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

| ID | Decision | Impact |
|----|----------|--------|
| SMTH-TOOL-01 | Use smooth_stone for wooden->stone upgrades | Adds processing step, matches crafting-equivalent intent |

## Files Changed

**Modified (1):**
- `src/main/kotlin/thc/smithing/TierUpgradeConfig.kt` - Added TOOL_MATERIAL_COUNTS and tool upgrade mappings

**Created (20):**
- Wooden->Stone recipes: 5 files (pickaxe, axe, shovel, hoe, sword)
- Stone->Copper recipes: 5 files
- Copper->Iron recipes: 5 files
- Iron->Diamond recipes: 5 files

## Testing Notes

Relies on SmithingMenuMixin from 61-01 for:
- Material count validation
- Extra material consumption
- Enchantment preservation
- Durability restoration

No additional testing infrastructure needed beyond existing mixin.

## Next Phase Readiness

**Ready for Plan 03:** Netherite upgrade integration (requires templates, not material counts)

**Blocks:** None

**Dependencies resolved:** Plan 01 complete (mixin infrastructure available)

## Maintenance Notes

### Adding New Tool Types

To add new tool tier upgrades:
1. Add material counts to `TOOL_MATERIAL_COUNTS`
2. Add upgrade paths to `VALID_TIER_UPGRADES`
3. Create corresponding recipe files

### Changing Material Costs

Modify values in `TOOL_MATERIAL_COUNTS` - mixin automatically uses updated counts.

### Recipe Removal

To disable specific upgrades, delete recipe files. Config validation will still work for remaining upgrades.
