---
phase: 56
plan: 02
subsystem: loot-tables
tags: [enchanted-books, mob-drops, looting-bonus]
requires:
  - phase: 56-01 # Stage classification helpers
provides:
  - Enchanted book drops from 6 mob types
  - 10 total enchantment acquisition paths
affects:
  - Future enchantment balance tuning
tech-stack:
  added: []
  patterns:
    - "random_chance_with_enchanted_bonus with flat Looting bonus"
    - "is_baby: false condition for adult-only drops"
    - "slime size >= 2 condition for large magma cube drops"
key-files:
  created:
    - data/minecraft/loot_table/entities/drowned.json
    - data/minecraft/loot_table/entities/spider.json
    - data/minecraft/loot_table/entities/husk.json
    - data/minecraft/loot_table/entities/stray.json
    - data/minecraft/loot_table/entities/blaze.json
    - data/minecraft/loot_table/entities/magma_cube.json
  modified: []
decisions: []
metrics:
  duration: 3 min
  completed: 2026-01-28
---

# Phase 56 Plan 02: Mob Enchanted Book Drops Summary

**One-liner:** Added 10 enchanted book pools to 6 mob loot tables with 2.5%/5% drop rates and flat +1% Looting bonus.

## What Changed

### Drowned (Water Enchantments)
Added 4 independent enchanted book pools:
- Aqua Affinity (level 1) - 2.5% base, 3.5% with Looting
- Depth Strider (level 1) - 2.5% base, 3.5% with Looting
- Frost Walker (level 1) - 2.5% base, 3.5% with Looting
- Respiration (level 1) - 2.5% base, 3.5% with Looting

### Spider
- Bane of Arthropods (level 1) - 2.5% base, 3.5% with Looting

### Husk
- Smite (level 1) - 2.5% base, 3.5% with Looting

### Stray
- Smite (level 1) - 2.5% base, 3.5% with Looting

### Blaze
- Fire Protection (level 1) - 2.5% base, 3.5% with Looting

### Magma Cube
- Flame (level 1) - 5% base, 6% with Looting
- Fire Aspect (level 1) - 5% base, 6% with Looting

## Design Decisions

### Drop Rate Structure
- Base rates: 2.5% (most enchantments) or 5% (magma cube fire enchantments)
- Looting bonus: Flat +1% regardless of Looting level (not scaling)
- Implemented via `random_chance_with_enchanted_bonus` with `per_level_above_first: 0.0`

### Adult-Only Restrictions
- Drowned, Spider, Husk, Stray: `is_baby: false` condition
- Magma Cube: `size >= 2` condition (same as magma cream)
- Blaze: No restriction needed (blazes cannot be babies)

### Any Death Source
- No `killed_by_player` condition on any pool
- Books drop from environmental deaths, mob kills, etc.

## Verification Completed

1. All 6 JSON files valid syntax
2. Correct enchantments per mob type
3. Correct drop rates (2.5%/3.5% or 5%/6%)
4. Adult-only conditions where applicable
5. No player-kill restrictions
6. Build compiles successfully

## Commits

| Hash | Description |
|------|-------------|
| 08445a1 | feat(56-02): add water enchantment book drops to drowned |
| 4802a8b | feat(56-02): add enchantment book drops to mobs |

## Deviations from Plan

None - plan executed exactly as written.

## Files Changed

```
data/minecraft/loot_table/entities/drowned.json   (created)
data/minecraft/loot_table/entities/spider.json    (created)
data/minecraft/loot_table/entities/husk.json      (created)
data/minecraft/loot_table/entities/stray.json     (created)
data/minecraft/loot_table/entities/blaze.json     (created)
data/minecraft/loot_table/entities/magma_cube.json (created)
```

## Next Phase Readiness

Phase 56 is complete. All acquisition gating requirements implemented:
- Plan 01: Stage 3+ enchantment filtering from loot tables
- Plan 02: Mob-specific enchanted book drops

Enchantment progression is now properly gated:
- Stage 1-2: Available from vanilla sources
- Stage 3+: Only from specific mob farming
