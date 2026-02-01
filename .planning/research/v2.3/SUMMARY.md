# Research Summary: v2.3 Monster Overhaul

**Domain:** Minecraft Fabric mod - monster modifications and regional spawn system
**Researched:** 2026-01-23
**Overall confidence:** MEDIUM-HIGH

## Executive Summary

The v2.3 monster overhaul is technically feasible using a combination of Fabric API events and targeted mixins. The existing THC codebase provides strong foundations: NaturalSpawner mixin infrastructure, entity attribute patterns (ServerPlayerMixin), and threat system integration points.

Three categories of modifications are required:
1. **Global changes** (speed, loot) - Handled via `ServerEntityEvents.ENTITY_LOAD`
2. **Spawn modifications** (replacements, regional distribution, caps) - NaturalSpawner mixins
3. **Entity-specific behaviors** (Ghast, Enderman, Vex, Phantom, Iron Golem) - Per-entity mixins

The regional spawn system is the most complex feature, requiring custom spawn tables and cap tracking. The existing NaturalSpawner mixin can be extended rather than replaced.

## Key Findings

**Stack:** Fabric API events for load-time modifications + targeted mixins for behavior changes
**Architecture:** Extend existing NaturalSpawnerMixin, add per-entity behavior mixins
**Critical pitfall:** BiomeModifications API is NOT suitable for conditional spawn replacement (stage-gating, regional logic)

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Global Monster Modifications
**Features:**
- 20% speed increase (all monsters except creepers/baby zombies)
- Monster loot removal (armor/weapon drops)
- Baby zombie speed normalization

**Rationale:** Single `ServerEntityEvents.ENTITY_LOAD` handler covers all. Low risk, validates event-based modification pattern.

**Research flags:** None - HIGH confidence

---

### Phase 2: Spawn Table Replacements
**Features:**
- Zombie -> Husk replacement
- Skeleton -> Stray replacement

**Rationale:** Extends existing NaturalSpawnerMixin with `getRandomSpawnMobAt` injection. Foundation for regional system.

**Research flags:** None - HIGH confidence (mixin pattern verified)

---

### Phase 3: Entity-Specific Behaviors (Simple)
**Features:**
- Vex iron sword removal + health reduction
- Phantom natural spawn removal
- Illager patrol stage-gating (stage 2+)
- Iron golem summon prevention

**Rationale:** Straightforward HEAD cancellation mixins on spawners and entity methods. Group these as low-risk entity modifications.

**Research flags:**
- Iron golem: MEDIUM confidence - Verify `Villager.spawnGolem()` method name in decompiled source

---

### Phase 4: Entity-Specific Behaviors (Complex)
**Features:**
- Ghast projectile modifications (speed +50%, fire rate -25%, fire spread +100%)
- Enderman teleport-behind mechanic (50% after damage)
- Enderman proximity aggro (3 block radius)

**Rationale:** Inner class mixins (Ghast) and behavior modifications require more careful implementation. Higher verification needs.

**Research flags:**
- Ghast: MEDIUM confidence - Inner class `GhastShootFireballGoal` mixin needs structure verification
- Enderman: MEDIUM confidence - `teleportTowards` signature and behavior needs verification

---

### Phase 5: Regional Spawn System
**Features:**
- Overworld 3-way split (Surface/Upper Cave/Lower Cave)
- Custom mob distributions per region
- NBT spawn origin tagging
- Pillager variants (MELEE/RANGED equipment)

**Rationale:** Build on Phase 2 spawn interception. Requires new `SpawnRegion` system and spawn tables. Most complex phase.

**Research flags:**
- Needs careful integration with existing base chunk blocking
- Spawn table data structure design decision

---

### Phase 6: Monster Cap Partitioning
**Features:**
- Per-region monster caps (30%/40%/50% of vanilla)

**Rationale:** Separated from Phase 5 because cap tracking is architecturally complex. May need custom tracking system rather than modifying vanilla `SpawnState`.

**Research flags:**
- LOW confidence - Vanilla cap system is chunk-based, not region-based
- May require iterating approach to find workable solution

---

### Phase 7: Damage Rebalancing
**Features:**
- Monster damage rebalancing (7 mobs)

**Rationale:** Deferred to after spawn system is working. Simple attribute modifications via ENTITY_LOAD.

**Research flags:** None - HIGH confidence (same pattern as speed)

---

## Phase Ordering Rationale

1. **Global first:** Validates ServerEntityEvents.ENTITY_LOAD pattern used throughout
2. **Spawn replacement before regional:** Establishes NaturalSpawner injection points
3. **Simple entity behaviors before complex:** Builds confidence before inner class mixins
4. **Regional system after entity behaviors:** Needs all entity modifications working first
5. **Caps after regional:** Most uncertain feature, may need iteration
6. **Damage last:** Simple feature, low dependency on others

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Global modifications (speed, loot) | HIGH | Verified pattern in THC codebase |
| Spawn replacements | HIGH | NaturalSpawner mixin pattern verified |
| Simple entity behaviors | HIGH | Standard HEAD cancellation |
| Ghast fireball | MEDIUM | Inner class mixin needs verification |
| Enderman teleport | MEDIUM | Method signatures need verification |
| Regional spawn system | HIGH | Curtain mod provides reference pattern |
| Cap partitioning | LOW | May need custom approach |
| Damage rebalancing | HIGH | Same as speed modification |

## Gaps to Address

1. **Ghast inner class structure:** Verify `GhastShootFireballGoal.tick()` method in decompiled 1.21.11
2. **Enderman methods:** Verify `teleportTowards`, `teleport`, `customServerAiStep` signatures
3. **Iron golem method:** Verify `Villager.spawnGolem()` or equivalent in 1.21.11
4. **Cap tracking approach:** May need phase-specific research if vanilla modification fails

## Files Created

| File | Purpose |
|------|---------|
| `.planning/research/v2.3/STACK.md` | Detailed mixin targets, method signatures, implementation patterns |
| `.planning/research/v2.3/SUMMARY.md` | This file - executive summary and roadmap implications |

---

*Research complete. Ready for roadmap creation.*
