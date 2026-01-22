---
phase: 24-blast-totem
plan: 01
subsystem: items
tags: [item-registration, loot-tables, progression-gating]

dependency-graph:
  requires: []
  provides: [blast-totem-item, totem-replacement]
  affects: [25-blast-furnace]

tech-stack:
  added: []
  patterns:
    - "Loot table item replacement: detect before remove, add replacement after"

key-files:
  created:
    - src/main/resources/assets/thc/items/blast_totem.json
    - src/main/resources/assets/thc/models/item/blast_totem.json
  modified:
    - src/main/kotlin/thc/item/THCItems.kt
    - src/main/kotlin/thc/THC.kt
    - src/main/resources/assets/thc/lang/en_us.json

decisions:
  - id: blast-totem-plain-item
    choice: Use plain Item class instead of custom TotemItem
    reason: Blast Totem has no special behavior - it's just a crafting ingredient for blast furnace

metrics:
  duration: 4 min
  completed: 2026-01-22
---

# Phase 24 Plan 01: Blast Totem Item Registration Summary

**One-liner:** Registered Blast Totem item that replaces all Totem of Undying drops via loot table modification.

## What Was Done

### Task 1: Register Blast Totem item
- Added `BLAST_TOTEM` item registration in `THCItems.kt` following established pattern
- Used plain `Item` class with `stacksTo(1)` property
- Created `items/blast_totem.json` and `models/item/blast_totem.json`
- Added language key in `en_us.json` for "Blast Totem" display name
- Registered in tools creative tab alongside LAND_PLOT
- **Commit:** `1031d1f`

### Task 2: Replace Totem of Undying drops with Blast Totem
- Added `Items.TOTEM_OF_UNDYING` to `removedItems` set
- Extended `MODIFY_DROPS` callback to detect totem presence before removal
- Added replacement logic to add `BLAST_TOTEM.defaultInstance` when totem was present
- All loot sources (Evoker, chests, commands) now give Blast Totem
- **Commit:** `8d968e4`

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

1. **Plain Item class** - Blast Totem uses vanilla `Item` class without custom behavior. The totem is purely a crafting ingredient for blast furnace (Phase 25), not a functional item with special mechanics.

## Technical Notes

The loot replacement pattern is important:
```kotlin
val hadTotem = drops.any { it.`is`(Items.TOTEM_OF_UNDYING) }
drops.removeIf { stack -> removedItems.any { stack.`is`(it) } }
if (hadTotem) {
    drops.add(THCItems.BLAST_TOTEM.defaultInstance)
}
```

Checking for totem **before** removal is essential since the `removeIf` call deletes the item from drops. This pattern enables item replacement in a single pass through the loot modification event.

## Next Phase Readiness

Phase 25 (Blast Furnace) can now proceed:
- `THCItems.BLAST_TOTEM` available for crafting recipe ingredient
- Evoker drops provide Blast Totem acquisition path
- Progression gate established: Evoker fight required for blast furnace access
