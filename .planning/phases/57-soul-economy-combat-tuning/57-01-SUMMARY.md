---
phase: 57
plan: 01
subsystem: game-economy
tags: [loot-tables, crafting, combat-balance, soul-dust]
requires: [55-enchanting-system, 53-illager-spawns]
provides:
  - soul-dust-farming
  - soul-soil-crafting
  - arrow-kiting-nerf
  - melee-pillager-buff
affects: [58-spawn-table-replacements]
tech-stack:
  added: []
  patterns:
    - random_chance_with_enchanted_bonus for percentage-based drops
    - Equipment check for mob variant detection
key-files:
  created:
    - data/minecraft/loot_table/entities/pillager.json
    - data/minecraft/loot_table/entities/vindicator.json
    - data/minecraft/loot_table/entities/evoker.json
    - data/minecraft/loot_table/entities/illusioner.json
    - data/minecraft/loot_table/entities/ravager.json
    - data/minecraft/loot_table/entities/witch.json
    - src/main/resources/data/thc/recipe/soul_soil.json
  modified:
    - src/main/java/thc/mixin/AbstractArrowMixin.java
    - src/main/kotlin/thc/monster/DamageRebalancing.kt
decisions:
  - id: SOUL-LOOT-01
    choice: 20% base drop with +1% per Looting level
    rationale: Matches blaze pattern for flat Looting bonus
  - id: SOUL-CRAFT-01
    choice: 2x2 recipe (4 dust -> 1 soil)
    rationale: Makes soul soil accessible but not trivial
  - id: CMBT-ARROW-01
    choice: Speed III instead of Speed IV
    rationale: Reduces kiting effectiveness without removing mechanic
  - id: CMBT-PILLAGER-01
    choice: Equipment-based damage modifier for melee pillagers
    rationale: Distinguishes melee from ranged using iron sword check
metrics:
  duration: 5 minutes
  completed: 2026-01-29
---

# Phase 57 Plan 01: Soul Economy & Combat Tuning Summary

Soul dust drops from illagers + soul soil crafting + combat balance tweaks

## What Was Built

Implemented soul economy (illager drops + crafting) and combat tuning (arrow speed + pillager damage):

**Soul Economy (SOUL-01, SOUL-02):**
- All 6 illager types drop soul dust at 20% rate (+1% per Looting level)
- 4 soul dust crafts 1 soul soil in 2x2 pattern
- Enables enchanting table progression via soul dust farming

**Combat Tuning (CMBT-01, CMBT-02):**
- Arrow hits apply Speed III (was Speed V) to reduce kiting effectiveness
- Melee pillagers deal 6.5 damage (was ~4.5) via +44.4% modifier
- Equipment check distinguishes melee (iron sword) from ranged pillagers

## Key Technical Decisions

### Loot Table Structure

Used `random_chance_with_enchanted_bonus` pattern from blaze.json:
- `unenchanted_chance: 0.2` for 20% base drop
- `base: 0.21` for 21% with any Looting level
- `per_level_above_first: 0.0` for flat +1% bonus regardless of level

### Per-Entity Loot Table Modifications

| Entity | Modification | Rationale |
|--------|-------------|-----------|
| Pillager | Add soul dust pool | Preserve ominous_bottle captain drop |
| Vindicator | Add soul dust pool | Preserve emerald drop |
| Evoker | Add soul dust pool | Preserve totem and emerald drops |
| Illusioner | Add pools array | Was empty, now has soul dust |
| Ravager | Replace saddle | Saddles removed in v2.4 |
| Witch | Add soul dust pool | Preserve all 7 ingredient pools |

### Soul Soil Recipe

2x2 shaped crafting:
- 4 soul dust in square pattern
- Produces 1 soul soil
- Category: building (matches vanilla block crafting)

### Arrow Speed Reduction

Changed effect amplifier from 4 to 2:
- Minecraft effects are 0-indexed (amplifier 2 = Speed III)
- Reduces kiting effectiveness while maintaining mechanic
- Still provides movement advantage for tactical positioning

### Melee Pillager Damage

Equipment-based variant detection:
- Check `mob.mainHandItem.is(Items.IRON_SWORD)` to distinguish melee
- Apply +44.4% damage modifier (4.5 -> 6.5)
- Follows established ATTACK_DAMAGE attribute modifier pattern
- Transient modifier (no save bloat)

## Requirements Satisfied

- **SOUL-01:** All 6 illagers drop soul dust at 20% (+1%/Looting)
- **SOUL-02:** 4 soul dust crafts 1 soul soil (2x2 pattern)
- **CMBT-01:** Arrow Speed III (reduced from Speed V)
- **CMBT-02:** Melee pillagers deal 6.5 damage (increased from ~4.5)

## Files Modified

**Loot tables (6 files):**
- `data/minecraft/loot_table/entities/pillager.json` - Added soul dust pool
- `data/minecraft/loot_table/entities/vindicator.json` - Added soul dust pool
- `data/minecraft/loot_table/entities/evoker.json` - Added soul dust pool
- `data/minecraft/loot_table/entities/illusioner.json` - Added pools array with soul dust
- `data/minecraft/loot_table/entities/ravager.json` - Replaced saddle with soul dust
- `data/minecraft/loot_table/entities/witch.json` - Added soul dust pool

**Recipe:**
- `src/main/resources/data/thc/recipe/soul_soil.json` - 2x2 crafting recipe

**Combat code:**
- `src/main/java/thc/mixin/AbstractArrowMixin.java` - Speed III amplifier
- `src/main/kotlin/thc/monster/DamageRebalancing.kt` - Melee pillager damage

## Commits

1. `7cc79e6` - feat(57-01): add soul dust drops and soul soil recipe
2. `911547d` - feat(57-01): reduce arrow speed effect to Speed III
3. `18eca38` - feat(57-01): increase melee pillager damage to 6.5

## Decisions Made

**SOUL-LOOT-01: Flat Looting Bonus**
- Decision: Use `per_level_above_first: 0.0` for +1% regardless of level
- Reason: Matches v2.5 blaze pattern for consistent Looting behavior
- Alternative considered: Progressive bonus (+1%, +2%, +3%) - rejected for complexity

**SOUL-CRAFT-01: 2x2 Recipe**
- Decision: 4 soul dust -> 1 soul soil in 2x2 pattern
- Reason: Accessible but not trivial; requires ~20 illager kills per soil
- Alternative considered: 3x3 recipe (9 dust) - rejected as too expensive

**CMBT-ARROW-01: Speed III**
- Decision: Reduce from Speed V (amplifier 4) to Speed III (amplifier 2)
- Reason: Reduces kiting effectiveness while maintaining tactical mobility
- Alternative considered: Speed II - rejected as too weak

**CMBT-PILLAGER-01: Equipment-Based Detection**
- Decision: Check iron sword in mainhand to distinguish melee pillagers
- Reason: Reliable detection without tracking spawn metadata
- Alternative considered: Attachment flag - rejected as overcomplicated

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

**Phase 58 (Spawn Table Replacements):**
- Soul dust established as illager drop for spawn table filtering
- No blockers

**Blockers:** None

**Concerns:** None

## Test Plan

**Soul Dust Drops:**
1. Spawn each illager type
2. Kill with/without Looting III
3. Verify ~20% drop rate base, ~21% with Looting

**Soul Soil Crafting:**
1. Obtain 4 soul dust
2. Place in 2x2 pattern
3. Verify 1 soul soil output

**Arrow Speed:**
1. Shoot arrow at mob
2. Verify Speed III effect (6 seconds)

**Melee Pillager Damage:**
1. Spawn pillager with iron sword
2. Take melee hit
3. Verify ~6.5 damage (Hard difficulty)

## Performance Impact

- Loot table additions: Negligible (6 additional pools)
- Recipe: Negligible (single shaped recipe)
- Arrow mixin: No change (amplifier value only)
- Pillager damage: Minimal (equipment check on spawn)

## Known Issues

None.
