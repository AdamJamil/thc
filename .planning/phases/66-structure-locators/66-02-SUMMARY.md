---
phase: 66-structure-locators
plan: 02
completed: 2026-01-31
duration: 5 min

subsystem: assets
tags: [compass, textures, models, item-definitions, range-dispatch]

dependency-graph:
  requires:
    - "66-01 (locator items registered)"
  provides:
    - "Compass range_dispatch item model definitions"
    - "96 directional models for compass angles"
    - "96 themed textures for 6 locator types"
    - "Lang translations for all locators"
  affects:
    - "Phase 68 (cartographer trades using locators)"

tech-stack:
  added: []
  patterns:
    - "range_dispatch with compass property for lodestone target"
    - "16-angle compass models (22.5 degree increments)"
    - "Node.js canvas for programmatic texture generation"

key-files:
  created:
    - src/main/resources/assets/thc/items/fortress_locator.json
    - src/main/resources/assets/thc/items/bastion_locator.json
    - src/main/resources/assets/thc/items/trial_chamber_locator.json
    - src/main/resources/assets/thc/items/pillager_outpost_locator.json
    - src/main/resources/assets/thc/items/ancient_city_locator.json
    - src/main/resources/assets/thc/items/stronghold_locator.json
    - src/main/resources/assets/thc/models/item/*_locator_*.json (96 files)
    - src/main/resources/assets/thc/textures/item/*_locator_*.png (96 files)
  modified:
    - src/main/resources/assets/thc/lang/en_us.json

decisions: []

metrics:
  tasks: 3
  duration: 5 min
---

# Phase 66 Plan 02: Structure Locator Assets Summary

**One-liner:** Complete compass-style asset pipeline with range_dispatch item definitions, 96 directional models, 96 themed textures, and lang translations for 6 structure locators.

## What Was Built

Created the visual rendering assets for structure locator compasses:

1. **Item Model Definitions** - 6 JSON files in `items/` using `range_dispatch` property to select models based on compass angle to lodestone target
2. **Directional Models** - 96 JSON files (16 per locator) pointing to angle-specific textures
3. **Compass Textures** - 96 PNG files (16 per locator) with color-coded compass bodies and rotated needles
4. **Lang Translations** - Display names for all 6 locator items

## Key Implementation Details

### Item Model Definition Structure

```json
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:compass",
    "target": "lodestone",
    "wobble": true,
    "scale": 1.0,
    "fallback": { "type": "minecraft:model", "model": "thc:item/fortress_locator_00" },
    "entries": [
      { "threshold": 0.0, "model": { ... "thc:item/fortress_locator_00" } },
      { "threshold": 0.0625, "model": { ... "thc:item/fortress_locator_01" } },
      ...
      { "threshold": 0.9375, "model": { ... "thc:item/fortress_locator_15" } }
    ]
  }
}
```

### Texture Color Themes

| Locator | Base Color | Theme |
|---------|------------|-------|
| fortress_locator | Dark red/maroon | Nether |
| bastion_locator | Gold/yellow | Piglin |
| trial_chamber_locator | Copper/orange | Trial chamber copper |
| pillager_outpost_locator | Gray/dark | Pillager |
| ancient_city_locator | Cyan/dark blue | Deep Dark |
| stronghold_locator | Purple | End |

### Compass Needle Rotation

16 discrete angles at 22.5 degree increments (360/16):
- Model 00: 0 degrees (north)
- Model 01: 22.5 degrees
- ...
- Model 15: 337.5 degrees

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| e07d4ef | feat | Add item model definitions with range_dispatch compass |
| efd296c | feat | Add directional models and textures for 6 locators |
| 18c599a | feat | Add locator item translations |

## Files Changed

**Created:**
- 6 item model definitions in `items/`
- 96 model files in `models/item/`
- 96 texture files in `textures/item/`

**Modified:**
- `lang/en_us.json` (+6 translations)

**Total:** 198 new files, 1 modified

## Next Phase Readiness

Phase 66 (Structure Locators) is complete. The locators now have:
- Server-side structure search logic (66-01)
- Client-side compass rendering (66-02)

Ready to proceed to Phase 67 (Villager Jobs).
