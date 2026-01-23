---
phase: 34-new-foods
plan: 01
subsystem: food
tags: [food-items, item-registration, recipes, translations]

dependency-graph:
  requires:
    - 33-food-stats (FoodStatsModifier pattern)
  provides:
    - honey-apple-item
    - hearty-stew-rename
    - new-food-recipe
  affects:
    - food progression (new high-tier options)
    - healing rates (via saturation tiers)

tech-stack:
  added: []
  patterns:
    - THCItems registration with food tab integration
    - Context.modify for custom THC items in FoodStatsModifier
    - Translation override for vanilla item rename

file-tracking:
  key-files:
    created:
      - src/main/resources/assets/thc/models/item/honey_apple.json
      - src/main/resources/data/thc/recipe/honey_apple.json
    modified:
      - src/main/kotlin/thc/item/THCItems.kt
      - src/main/kotlin/thc/food/FoodStatsModifier.kt
      - src/main/resources/assets/thc/lang/en_us.json

decisions:
  - id: food-tab-placement
    choice: Add HONEY_APPLE to food_and_drinks creative tab
    rationale: Food items belong in food tab, not tools tab
  - id: saturation-modifier-calculation
    choice: Use formula targetSat/(nutrition*2) for modifier
    rationale: FoodProperties.Builder uses saturationModifier, not direct saturation

metrics:
  duration: 3 min
  completed: 2026-01-23
---

# Phase 34 Plan 01: New Foods Summary

Add Hearty Stew (rabbit stew rename with 10 hunger/6.36 sat) and Honey Apple (new item with 8 hunger/2.73 sat) crafted from apple + honey bottle.

## What Was Built

### Honey Apple Item

New custom food item registered in `THCItems.kt`:
- Stacks to 64
- Appears in Food & Drinks creative tab
- Food stats: 8 hunger, 2.73 saturation (T2+ healing tier)
- Custom texture at `textures/item/honey_apple.png`

### Hearty Stew (Rabbit Stew Rename)

Modified vanilla rabbit stew via FoodStatsModifier:
- Display name: "Hearty Stew" (translation override)
- Food stats: 10 hunger, 6.36 saturation (T5 healing tier)
- Premium food option for high-end healing

### Crafting Recipe

Shapeless recipe: Apple + Honey Bottle = Honey Apple

## Key Implementation Details

**Item Registration Pattern:**
```kotlin
@JvmField
val HONEY_APPLE: Item = register("honey_apple") { key ->
    Item(
        Item.Properties()
            .setId(key)
            .stacksTo(64)
    )
}
```

**Food Stats via FoodStatsModifier:**
```kotlin
// RABBIT_STEW: 10 hunger, 6.36 sat -> modifier = 6.36/20 = 0.318
context.modify(Items.RABBIT_STEW) { builder ->
    builder.set(DataComponents.FOOD, FoodProperties.Builder()
        .nutrition(10)
        .saturationModifier(0.318f)
        .build())
}

// HONEY_APPLE: 8 hunger, 2.73 sat -> modifier = 2.73/16 = 0.170625
context.modify(THCItems.HONEY_APPLE) { builder ->
    builder.set(DataComponents.FOOD, FoodProperties.Builder()
        .nutrition(8)
        .saturationModifier(0.170625f)
        .build())
}
```

## Commits

| Hash | Message |
|------|---------|
| ca5ac30 | feat(34-01): add Honey Apple item registration |
| a6924aa | feat(34-01): add Hearty Stew and Honey Apple food stats |
| 15e4e6c | feat(34-01): add translations and Honey Apple model |
| e6e6ee9 | feat(34-01): add Honey Apple crafting recipe |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] `./gradlew build` succeeds without errors
- [x] THCItems.kt contains HONEY_APPLE registration
- [x] FoodStatsModifier.kt contains RABBIT_STEW and HONEY_APPLE modifications
- [x] en_us.json contains "Hearty Stew" translation
- [x] honey_apple.json model exists and references correct texture
- [x] honey_apple.json recipe exists with correct ingredients

## Next Phase Readiness

Phase 34-01 complete. New foods integrate with existing food system:
- Honey Apple: T2+ healing (2.73 sat) - mid-tier recovery
- Hearty Stew: T5 healing (6.36 sat) - premium healing

No blockers for subsequent phases.
