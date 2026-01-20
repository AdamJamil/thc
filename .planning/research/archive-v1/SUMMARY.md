# Research Summary: v1.1 Extra Features Batch 1

**Researched:** 2026-01-18
**Milestone:** v1.1 Extra Features Batch 1
**Overall Confidence:** MEDIUM-HIGH

## Executive Summary

Research into four v1.1 features revealed clear implementation paths using existing codebase patterns. All features can be implemented with mixins and Fabric API events, no new infrastructure required.

**Key findings:**

| Feature | Approach | Difficulty | Confidence |
|---------|----------|------------|------------|
| Drowning 4x slower | Mixin `decreaseAirSupply` | LOW | HIGH |
| Spear removal | Recipe mixin + loot JSON + mob mixin | MEDIUM | HIGH |
| Projectile aggro + effects | Mixin `onHitEntity` | LOW | HIGH |
| Projectile physics | Mixin projectile `tick` | MEDIUM | MEDIUM |

**Critical discovery:** Spears were added in Mounts of Mayhem (Dec 2025). Seven tiers exist (wooden through netherite). Spear removal requires three prongs: crafting recipes (7), loot tables (6), and mob spawn equipment (6+ mob types).

**Risk area:** Projectile physics modification requires state tracking for distance traveled. The "quadratic gravity after 8 blocks" formula needs careful implementation to avoid desync between client and server.

## Stack Findings

**Mixin targets identified:**
- `LivingEntity.decreaseAirSupply()` - drowning tick rate
- `Mob.finalizeSpawn()` - strip spear equipment from zombies/piglins
- `Projectile.onHit()` / `AbstractArrow.onHitEntity()` - hit detection
- `AbstractArrow.tick()` or `shootFromRotation()` - physics modification

**Spear item IDs confirmed:**
- `minecraft:wooden_spear`, `minecraft:stone_spear`, `minecraft:copper_spear`
- `minecraft:iron_spear`, `minecraft:golden_spear`, `minecraft:diamond_spear`
- `minecraft:netherite_spear`

**Existing patterns to reuse:**
- `RecipeManagerMixin` - already removes shield recipes, extend for spears
- `LootTableEvents.MODIFY` - already used in THC.kt
- `MobEffectInstance` application - already used in LivingEntityMixin

## Features Findings

**Drowning mechanics:**
- Air supply: 300 ticks (15 seconds), decreases 1/tick underwater
- Damage at air = -20, then resets to 0 (1 damage/second cycle)
- 4x slower = only decrement every 4th tick (randomized)

**Spear loot sources (6 tables):**
- Ocean ruins (small + big) - stone spears
- Village weaponsmith - copper/iron spears
- Buried treasure - iron spears
- Bastion remnant - diamond spears
- End city - enchanted diamond spears

**Mob spawn equipment:**
- Zombies, husks, zombie horsemen - iron spears
- Piglins, zombified piglins - golden spears

**Projectile physics (vanilla):**
- Arrow: gravity -0.05 blocks/tick², drag 0.99/tick
- Snowball: gravity -0.03 blocks/tick², drag 0.99/tick
- Order (1.21.2+): Acceleration → Drag → Position

## Architecture Findings

**Recommended package structure:**
```
thc/mixin/
  LivingEntityDrowningMixin.java  # NEW
  MobEquipmentMixin.java          # NEW - strip spear equipment
  ProjectileMixin.java            # NEW - physics + aggro
```

**Build order (dependencies):**
1. Drowning (standalone, simplest)
2. Spear removal (data + mixin, no dependencies)
3. Projectile aggro/effects (hit detection mixin)
4. Projectile physics (builds on #3 mixin infrastructure)

**Note:** Phases 3 and 4 share the same mixin file - could be combined into single phase.

## Pitfalls Identified

**Critical path (highest risk):**

1. **PROJ-04: Quadratic gravity state tracking** - Must decide: distance from spawn vs ticks traveled
2. **SPEAR-01: Incomplete removal** - Must enumerate ALL sources before coding
3. **AGGRO-01: Goal vs Brain AI** - Zombies use Goal-based, Piglins use Brain-based
4. **PROJ-06: Multiple projectile classes** - AbstractArrow vs ThrowableProjectile differ

**Mitigations:**
- Clarify gravity formula interpretation before implementation
- Create checklist of spear sources, verify each independently
- Test aggro on both zombie (Goal) and piglin (Brain)
- May need separate mixins for arrow types vs thrown items

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Drowning | HIGH | Well-documented LivingEntity methods |
| Spear item removal | HIGH | Existing patterns for recipe + loot |
| Spear mob equipment | MEDIUM | Need to verify exact spawn method name |
| Projectile hit detection | HIGH | Standard mixin pattern |
| Projectile physics | MEDIUM | State tracking + gravity formula needs tuning |
| Effect application | HIGH | Already used in codebase |
| Aggro redirection | MEDIUM | Different AI systems need testing |

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Drowning Modification
**Rationale:** Standalone feature, simplest mixin, quick win to establish pattern.
- Implements: Drowning 4x slower requirement
- Avoids: DROWN-01 (state persistence) by using built-in airSupply
- Uses: Mixin to `decreaseAirSupply()`
- Estimated: 1 plan

### Phase 2: Spear Removal
**Rationale:** Multi-source removal, uses existing recipe mixin pattern.
- Implements: Spear removal from crafting, loot, mob equipment
- Avoids: SPEAR-01 (incomplete removal) by explicit source checklist
- Uses: RecipeManagerMixin extension, LootTableEvents, Mob.finalizeSpawn mixin
- Estimated: 2-3 plans (crafting/loot vs mob equipment)

### Phase 3: Projectile Combat
**Rationale:** Combined aggro + effects + physics as they share mixin infrastructure.
- Implements: Speed II + Glowing on hit, aggro redirect, velocity boost, gravity curve
- Avoids: PROJ-06 by identifying projectile scope upfront
- Uses: Projectile.onHit mixin, AbstractArrow.tick mixin
- Estimated: 2-3 plans (hit effects vs physics)

**Phase ordering rationale:**
- Drowning first: isolated, no dependencies, validates mixin patterns
- Spear removal second: uses existing patterns, prepares for projectile work
- Projectile last: most complex, benefits from patterns established in earlier phases

**Research flags for phases:**
- Phase 2: May need runtime verification of `#minecraft:spears` tag existence
- Phase 3: Gravity formula needs design clarification (distance interpretation)

## Open Questions

1. **Quadratic gravity definition:** Does "after 8 blocks" mean distance from shooter at hit time, or cumulative distance traveled? Affects implementation complexity.

2. **Projectile scope:** Requirement says "all player projectiles" - does this include fishing bobbers, wind charges, and lingering potions? Recommend: arrows, bolts, snowballs, eggs, ender pearls.

3. **Spear tag accessibility:** Does `ItemTags.SPEARS` exist in 1.21.11 Fabric, or must we check items individually?

## Sources

### Primary
- [Minecraft Wiki - Spear](https://minecraft.wiki/w/Spear)
- [Minecraft Wiki - Projectile](https://minecraft.wiki/w/Projectile)
- [Fabric API LootTableEvents](https://maven.fabricmc.net/docs/fabric-api-0.129.0+1.21.7/net/fabricmc/fabric/api/loot/v3/LootTableEvents.html)
- [Yarn API - LivingEntity](https://maven.fabricmc.net/docs/yarn-1.21+build.9/net/minecraft/entity/LivingEntity.html)

### Secondary
- [Fabric Wiki - Mixin Examples](https://fabricmc.net/wiki/tutorial:mixin_examples)
- [Minecraft Wiki - Damage](https://minecraft.wiki/w/Damage)

---
*Synthesized: 2026-01-18*
*Research valid: ~30 days (stable domain)*
