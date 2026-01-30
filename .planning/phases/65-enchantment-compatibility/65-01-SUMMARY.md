---
phase: 65
plan: 01
subsystem: enchantment-system
tags: [enchantment, compatibility, data-pack, tag-override]

dependency-graph:
  requires: [55-enchantment-removal]
  provides: [enchantment-stacking]
  affects: []

tech-stack:
  added: []
  patterns:
    - "Tag override with replace:true for emptying vanilla tags"
    - "Data-driven enchantment exclusivity via exclusive_set tags"

key-files:
  created:
    - src/main/resources/data/minecraft/tags/enchantment/exclusive_set/armor.json
    - src/main/resources/data/minecraft/tags/enchantment/exclusive_set/damage.json
  modified: []

decisions:
  - id: ENCH-STACK-01
    choice: "Data-driven tag override vs mixin approach"
    rationale: "Tag override is simpler, no code required, fully data-driven"
  - id: ENCH-DAMAGE-01
    choice: "Empty entire damage exclusive set including impaling/density/breach"
    rationale: "Impaling and density already in REMOVED_ENCHANTMENTS; breach stacking consistent with phase goal"

metrics:
  duration: 1 min 22 sec
  completed: 2026-01-30
---

# Phase 65 Plan 01: Enchantment Exclusive Set Overrides Summary

**Data-driven tag overrides to remove enchantment mutual exclusivity for protection and damage families**

## What Was Built

Created two JSON tag override files that empty vanilla exclusive sets, enabling:
- All 4 protection enchantments (protection, blast_protection, fire_protection, projectile_protection) to coexist on armor
- All damage enchantments (sharpness, smite, bane_of_arthropods) to coexist on weapons

The `"replace": true` key overwrites vanilla entries entirely, breaking the exclusivity check in `Enchantment.areCompatible()`.

## Key Changes

### Tag Override Files

**armor.json** - Empties armor protection exclusive set:
```json
{
  "replace": true,
  "values": []
}
```

**damage.json** - Empties damage exclusive set (also affects impaling, density, breach):
```json
{
  "replace": true,
  "values": []
}
```

## How It Works

1. Minecraft's data-driven enchantment system uses `exclusive_set` field in enchantment definitions
2. Each protection enchantment references `#minecraft:exclusive_set/armor`
3. Each damage enchantment references `#minecraft:exclusive_set/damage`
4. `Enchantment.areCompatible()` checks if either enchantment's exclusiveSet HolderSet contains the other
5. With empty exclusive sets, different enchantments always pass compatibility check
6. Stacked enchantments apply their effects additively (standard vanilla behavior)

## Deviations from Plan

None - plan executed exactly as written.

## Verification Performed

- Files created at correct paths: `src/main/resources/data/minecraft/tags/enchantment/exclusive_set/`
- Both files contain `"replace": true` and `"values": []`
- Build succeeds without errors

## Next Phase Readiness

Phase 65 complete. No blockers for future phases.

## Commits

| Hash | Message |
|------|---------|
| 8524ef1 | feat(65-01): add enchantment exclusive set tag overrides |
