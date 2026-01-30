---
phase: 63-combat-balancing
plan: 01
subsystem: combat
tags: [arrows, damage-reduction, recipe, pillager, stray]

dependency-graph:
  requires: []
  provides: [arrow-recipe-16x, pillager-damage-reduction, stray-damage-reduction]
  affects: []

tech-stack:
  added: []
  patterns:
    - EntityType owner check for mob-specific arrow damage modification

key-files:
  created:
    - src/main/resources/data/minecraft/recipe/arrow.json
  modified:
    - src/main/java/thc/mixin/AbstractArrowMixin.java

decisions:
  - id: CMBT-ARROW-01
    choice: Arrow recipe yields 16 (4x vanilla)
    rationale: Improves arrow economy for ranged combat engagement
  - id: CMBT-PILLAGER-02
    choice: Pillager arrow damage multiplied by 0.667
    rationale: Reduces 5-7 damage to 3-5, making encounters challenging not frustrating
  - id: CMBT-STRAY-01
    choice: Stray arrow damage multiplied by 0.5
    rationale: Reduces 4-8 damage to 2-4, significant reduction for skeleton variant

metrics:
  duration: 2 min
  completed: 2026-01-30
---

# Phase 63 Plan 01: Combat Balancing - Arrow Economy Summary

Arrow recipe yields 16 (4x vanilla), Pillager arrows reduced to ~67% damage, Stray arrows reduced to 50% damage.

## What Was Built

### Arrow Recipe Override
- Created `arrow.json` recipe override in data/minecraft/recipe
- Same ingredients: 1 flint + 1 stick + 1 feather
- Output increased from 4 to 16 arrows
- Improves player ability to engage in ranged combat

### Enemy Arrow Damage Reduction
- Extended AbstractArrowMixin with EntityType checks before player-only logic
- Pillager arrows: 0.667x multiplier (5-7 -> 3-5 damage)
- Stray arrows: 0.5x multiplier (4-8 -> 2-4 damage)
- Returns early after applying reduction (no other effects for enemy arrows)

## Implementation Details

### Recipe Pattern
```json
{
  "pattern": ["X", "#", "Y"],
  "result": { "count": 16, "id": "minecraft:arrow" }
}
```

### Damage Reduction Pattern
```java
// Check owner type before player-specific logic
if (owner != null && owner.getType() == EntityType.PILLAGER) {
    baseDamage = baseDamage * 0.667;
    return;
}
```

## Decisions Made

| ID | Decision | Rationale |
|----|----------|-----------|
| CMBT-ARROW-01 | 16 arrow yield | 4x vanilla enables ranged combat engagement |
| CMBT-PILLAGER-02 | 0.667x Pillager damage | Reduces frustration while maintaining challenge |
| CMBT-STRAY-01 | 0.5x Stray damage | Larger reduction for arrow + slowness combo |

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Message |
|------|---------|
| 004dd44 | feat(63-01): arrow recipe yields 16 instead of 4 |
| b0182cb | feat(63-01): reduce Pillager and Stray arrow damage |

## Verification Results

- [x] CMBT-01: Arrow recipe yields 16 (JSON verified)
- [x] CMBT-02: Pillager arrow damage * 0.667 (code inspection)
- [x] CMBT-03: Stray arrow damage * 0.5 (code inspection)
- [x] Build succeeds without errors

## Next Phase Readiness

Phase 63 combat balancing complete. Ready for human verification of in-game behavior.
