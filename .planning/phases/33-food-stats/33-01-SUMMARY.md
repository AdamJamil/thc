---
phase: 33-food-stats
plan: 01
subsystem: food
tags: [food-balance, component-modification, fabric-api]

dependency-graph:
  requires:
    - 31-healing (saturation-tiered healing system)
  provides:
    - rebalanced-food-nutrition
    - rebalanced-food-saturation
    - food-tier-separation
  affects:
    - healing rates (via saturation tiers)
    - food progression strategy

tech-stack:
  added: []
  patterns:
    - DefaultItemComponentEvents.MODIFY for vanilla item component modification
    - FoodProperties.Builder for food stat specification
    - saturationModifier calculation: targetSat / (nutrition * 2)

file-tracking:
  key-files:
    created:
      - src/main/kotlin/thc/food/FoodStatsModifier.kt
    modified:
      - src/main/kotlin/thc/THC.kt

decisions:
  - id: effects-preservation
    choice: Effects handled by CONSUMABLE component, not FOOD
    rationale: MC 1.21+ separates food stats from effects - modifying FOOD preserves existing CONSUMABLE effects automatically
  - id: saturation-modifier-formula
    choice: Calculate modifier as targetSat/(nutrition*2)
    rationale: FoodProperties.Builder uses saturationModifier, not direct saturation; formula is saturation = nutrition * modifier * 2

metrics:
  duration: 7 min
  completed: 2026-01-23
---

# Phase 33 Plan 01: Food Stats Rebalancing Summary

Rebalance 29 vanilla food items across 4 tiers using DefaultItemComponentEvents.MODIFY to integrate with saturation-tiered healing system.

## What Was Built

### Food Rebalancing Handler

Created `FoodStatsModifier.kt` that modifies vanilla item FOOD components at startup using Fabric's DefaultItemComponentEvents API.

**Food Categories and Target Values:**

| Category | Saturation | Healing Tier | Items |
|----------|------------|--------------|-------|
| Raw Meats | 0 | T0 (base only) | beef, porkchop, chicken, rabbit, mutton, cod, salmon |
| Cooked Meats | 1.6-1.8 | T2 | cooked versions of above |
| Crops/Vegetables | 0-0.7 | T0-T1 | bread, carrot, potato, baked_potato, melon_slice, apple, cookie, pumpkin_pie, dried_kelp, sweet_berries, glow_berries, beetroot |
| Golden Foods | 10 | T4 | golden_apple, enchanted_golden_apple, golden_carrot |

## Key Implementation Details

**FoodStatsModifier.kt Pattern:**
```kotlin
DefaultItemComponentEvents.MODIFY.register { context ->
    context.modify(Items.BEEF) { builder ->
        builder.set(DataComponents.FOOD, FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0f)  // 0 saturation
            .build())
    }
    // ... 28 more items
}
```

**Saturation Calculation:**
- Formula: `saturation = nutrition * saturationModifier * 2.0`
- To get target saturation: `modifier = targetSat / (nutrition * 2.0)`
- Example: COOKED_BEEF (8 hunger, 1.8 sat) -> modifier = 1.8/16 = 0.1125

**Effects Preservation:**
- Status effects (Hunger on raw chicken, Regen/Absorption on golden apples) are stored in CONSUMABLE component
- Modifying only FOOD component preserves existing effects automatically
- No need to extract/reapply effects

## Commits

| Hash | Message |
|------|---------|
| ae28f93 | feat(33-01): create FoodStatsModifier with DefaultItemComponentEvents |
| 9e7b0d8 | feat(33-01): register FoodStatsModifier in mod initialization |
| 0584562 | fix(33-01): correct item names and API usage for food rebalancing |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Incorrect item names**
- **Found during:** Task 3 (build verification)
- **Issue:** Plan used `RAW_BEEF`, `RAW_PORKCHOP`, etc. but MC 1.21+ uses `BEEF`, `PORKCHOP`
- **Fix:** Updated all item references to correct names
- **Commit:** 0584562

**2. [Rule 1 - Bug] Incorrect FoodProperties API usage**
- **Found during:** Task 3 (build verification)
- **Issue:** Plan assumed FoodProperties has `effects()` method - removed in MC 1.21+
- **Fix:** Effects are now in CONSUMABLE component; only modify FOOD for nutrition/saturation
- **Commit:** 0584562

**3. [Rule 1 - Bug] Plan's saturation values were final values, not modifiers**
- **Found during:** Task 3 (build verification)
- **Issue:** Plan specified "saturation 1.8f" but FoodProperties.Builder uses saturationModifier
- **Fix:** Calculated correct modifiers using formula: targetSat / (nutrition * 2)
- **Commit:** 0584562

## Verification Results

- [x] `./gradlew build` succeeds without errors
- [x] FoodStatsModifier.kt exists with 29 food modifications
- [x] FoodStatsModifier.register() called in THC.kt
- [x] DefaultItemComponentEvents.MODIFY.register pattern present

## Next Phase Readiness

Phase 33-01 complete. Food stats now integrate with saturation-tiered healing from phase 31:
- Raw meats (0 sat): T0 healing (0.125 hearts/s)
- Crops (0-0.7 sat): T0-T1 healing (0.125-0.3125 hearts/s)
- Cooked meats (1.6-1.8 sat): T2 healing (0.4375 hearts/s)
- Golden foods (10 sat): T4+ healing (0.9375+ hearts/s)

No blockers for subsequent phases.
