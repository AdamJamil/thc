---
phase: 11-tiered-arrows
plan: 02
subsystem: combat
tags: [arrows, items, damage, ranged]

dependency_graph:
  requires: [11-01]
  provides: [tiered-arrow-items, damage-bonus-arrows]
  affects: [11-03, 11-04]

tech-stack:
  added: []
  patterns:
    - Accessor mixin for private field access (AbstractArrowAccessor)
    - Item registration with factory pattern (THCArrows)

file-tracking:
  key-files:
    created:
      - src/main/kotlin/thc/item/TieredArrowItem.kt
      - src/main/kotlin/thc/item/THCArrows.kt
      - src/main/java/thc/mixin/access/AbstractArrowAccessor.java
      - src/main/resources/assets/thc/items/iron_arrow.json
      - src/main/resources/assets/thc/items/diamond_arrow.json
      - src/main/resources/assets/thc/items/netherite_arrow.json
      - src/main/resources/assets/thc/models/item/iron_arrow.json
      - src/main/resources/assets/thc/models/item/diamond_arrow.json
      - src/main/resources/assets/thc/models/item/netherite_arrow.json
    modified:
      - src/main/kotlin/thc/THC.kt
      - src/main/resources/thc.mixins.json
      - src/main/resources/assets/thc/lang/en_us.json

decisions: []

metrics:
  duration: 7min
  completed: 2026-01-19
---

# Phase 11 Plan 02: Tiered Arrow Items Summary

TieredArrowItem class + registration with damage bonuses for ranged progression

## What Was Built

### TieredArrowItem Class
Created `TieredArrowItem` extending `ArrowItem`:
- Takes `damageBonus` constructor parameter
- Overrides `createArrow()` to apply bonus via accessor mixin
- Iron: +1, Diamond: +2, Netherite: +3 damage (on top of base 2.0)

### AbstractArrowAccessor Mixin
Required because `baseDamage` is private in 1.21.11:
- Accessor mixin pattern to get/set baseDamage field
- Registered in thc.mixins.json

### THCArrows Registration
Following THCBucklers pattern:
- IRON_ARROW, DIAMOND_ARROW, NETHERITE_ARROW items
- Registered in combat creative tab
- init() called from THC.onInitialize()

### Assets
- Item definitions (items/*.json) with model references
- Model files (models/item/*.json) pointing to existing textures
- Lang translations for all three arrows

## Commits

| Hash | Description |
|------|-------------|
| b3bd7ff | TieredArrowItem class with damage bonus |
| 6cbf36b | THCArrows registration object |
| 3a6b3ff | Arrow item assets and translations |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] AbstractArrow package path changed**
- **Found during:** Task 1
- **Issue:** Plan referenced `net.minecraft.world.entity.projectile.AbstractArrow`, but 1.21.11 moved it to `net.minecraft.world.entity.projectile.arrow.AbstractArrow`
- **Fix:** Updated import path in both accessor mixin and TieredArrowItem
- **Commit:** b3bd7ff

**2. [Rule 3 - Blocking] baseDamage field is private**
- **Found during:** Task 1
- **Issue:** Cannot directly access baseDamage field as shown in plan
- **Fix:** Created AbstractArrowAccessor mixin to provide getter/setter access
- **Files added:** src/main/java/thc/mixin/access/AbstractArrowAccessor.java
- **Commit:** b3bd7ff

## Verification Results

- [x] ./gradlew build succeeds
- [x] TieredArrowItem class exists with createArrow override
- [x] THCArrows registers all three arrow types
- [x] THC.onInitialize calls THCArrows.init()
- [x] Item definitions exist for all three arrows
- [x] Model files point to correct textures
- [x] Lang file has translations for all arrows

## Next Phase Readiness

Plan 11-02 complete. Tiered arrow items are registered and ready for:
- Plan 11-03: Arrow recipes
- Plan 11-04: Arrow textures (if additional textures needed)
