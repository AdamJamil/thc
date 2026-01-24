# Milestone v2.3: Extra Features Batch 7 - Monster Overhaul

**Status:** PLANNING
**Phases:** 37-43
**Requirements:** 23 (FR-01 through FR-23)

## Overview

Comprehensive monster overhaul creating distinct threat profiles across Overworld regions. Global modifications increase monster speed and remove equipment drops. Spawn table replacements substitute husks for zombies and strays for skeletons. Entity-specific behaviors modify Ghast, Enderman, Vex, Phantom, Iron Golem, and Illager patrols. Regional spawn system introduces three-way Overworld split (Surface/Upper Cave/Lower Cave) with custom mob distributions and Pillager variants. Partitioned monster caps prevent surface spawns from consuming cave capacity. NBT tagging enables region tracking and cap counting.

## Phase Dependencies

```
Phase 37 (Global Mods)
    |
    v
Phase 38 (Spawn Replacements)
    |
    v
Phase 39 (Simple Entity Behaviors) --- depends on Stage System (v2.2)
    |
    v
Phase 40 (Complex Entity Behaviors)
    |
    v
Phase 41 (Regional Spawns) --- depends on Phase 42 (NBT Tagging)
    |
    v
Phase 42 (NBT Tagging)
    |
    v
Phase 43 (Cap Partitioning) --- depends on Phase 42
    |
    v
Phase 44 (Damage Rebalancing)
```

Note: Phase 42 (NBT Tagging) is prerequisite for both Phase 41 and Phase 43. Execute 42 before 41.

## Phases

### Phase 37: Global Monster Modifications

**Goal**: All hostile mobs behave more aggressively through speed increase and loot economy changes
**Depends on**: Nothing (first phase)
**Requirements**: FR-01, FR-02, FR-04, FR-05
**Plans:** 1 plan

Plans:
- [x] 37-01-PLAN.md — Speed modifications + loot filtering (FR-01, FR-02, FR-04, FR-05)

**Success Criteria:**
1. Zombies visibly outpace creepers when pursuing player
2. Baby zombies move at same speed as adult zombies (not 1.5x faster)
3. Killing 50 equipped zombies/skeletons yields zero armor or weapon drops
4. Killing 100 husks/zombies yields zero iron ingot drops

**Details:**
- FR-01: 20% speed increase via attribute modifier on ENTITY_LOAD
- Exclusions: Creepers (unchanged), Baby zombies (normalized), Bosses (Wither, Dragon)
- FR-02: Equipment drops removed via loot event interception
- FR-04: Baby zombie speed normalized by removing vanilla BABY_SPEED_BONUS or applying counter-modifier
- FR-05: Iron ingot drops removed from zombie/husk loot table

**Research Flags:**
- HIGH confidence: Existing ServerPlayerMixin attribute pattern applies
- Pitfall MOB-01: Ensure baby zombies excluded from speed boost
- Pitfall MOB-02: Ensure bosses excluded from speed boost

---

### Phase 38: Spawn Table Replacements

**Goal**: Overworld surface threats shift from basic zombies/skeletons to more dangerous variants
**Depends on**: Phase 37
**Requirements**: FR-03, FR-06
**Plans:** 1 plan

Plans:
- [x] 38-01-PLAN.md — Entity replacement mixin for surface spawns (FR-03, FR-06)

**Success Criteria:**
1. AFK 10 minutes in plains biome - zero zombies spawn, only husks
2. AFK 10 minutes in plains biome - zero skeletons spawn, only strays
3. Spider jockeys still spawn with skeleton riders (not strays)
4. Structure spawners (dungeons) still spawn vanilla zombies/skeletons

**Details:**
- FR-03: Zombie -> Husk replacement in NaturalSpawner.getRandomSpawnMobAt
- FR-06: Skeleton -> Stray replacement in same mixin
- Exception: Structure spawns use vanilla pools (dungeon spawners, fortress, monument)
- Exception: Spider jockeys keep skeleton riders (check spawn reason)
- Document: Husk->Drowned conversion takes longer (90s vs 45s) - intentional

**Research Flags:**
- HIGH confidence: Existing NaturalSpawnerMixin infrastructure
- Pitfall MOB-03: Document drowned conversion time increase as intentional
- Pitfall MOB-11: Preserve skeleton jockey context

---

### Phase 39: Entity-Specific Behaviors (Simple)

**Goal**: Individual mob modifications that are straightforward HEAD cancellation or attribute changes
**Depends on**: Phase 38, Stage System (v2.2)
**Requirements**: FR-12, FR-13, FR-14, FR-15, FR-17
**Plans:** 2 plans

Plans:
- [x] 39-01-PLAN.md — Vex modifications (health reduction, sword removal)
- [x] 39-02-PLAN.md — Spawner cancellations (phantom, patrol) + iron golem prevention

**Success Criteria:**
1. Spawned vex shows 8 HP (4 hearts) in F3 debug
2. Evoker-summoned vexes appear without visible iron swords
3. Player can go 3+ in-game days without sleeping - zero phantom spawns
4. At stage 1, extended AFK yields zero illager patrols
5. Advance to stage 2, patrols resume spawning normally
6. Building iron golem pattern (pumpkin + iron blocks) creates no golem

**Details:**
- FR-12: Vex health set to 8 HP on spawn via ENTITY_LOAD
- FR-13: Vex mainhand cleared on spawn
- FR-14: PhantomSpawner.tick HEAD cancellation returning 0
- FR-15: PatrolSpawner.tick HEAD cancellation when stage < 2
- FR-17: Iron golem summon prevention via Villager.spawnGolem or pumpkin place handler

**Research Flags:**
- HIGH confidence: PhantomSpawner/PatrolSpawner patterns verified
- MEDIUM confidence: Iron golem prevention method name needs verification
- Pitfall MOB-12: Monitor vex summon frequency (faster death = more summons)
- Pitfall MOB-13: Document insomnia mechanic removal as intentional

---

### Phase 40: Entity-Specific Behaviors (Complex)

**Goal**: Ghast and Enderman behavior modifications requiring careful implementation
**Depends on**: Phase 39
**Requirements**: FR-07, FR-08, FR-09, FR-10, FR-11
**Plans:** 2 plans

Plans:
- [x] 40-01-PLAN.md — Ghast modifications (velocity, fire rate, fire spread)
- [x] 40-02-PLAN.md — Enderman modifications (proximity aggro, teleport-behind)

**Success Criteria:**
1. Ghast fireballs travel visibly faster (50% increase)
2. Timing 10 ghast shots averages ~4 second intervals (not 3)
3. Ghast fireball ground impacts spread fire noticeably further than vanilla
4. Fighting enderman 20 times - ~50% result in enderman appearing behind player
5. Walking within 3 blocks of neutral enderman triggers aggro (no eye contact needed)

**Details:**
- FR-07: Fireball velocity scaled 1.5x at spawn time in Ghast$GhastShootFireballGoal
- FR-08: Shoot cooldown increased from 60 to 80 ticks
- FR-09: LargeFireball.onHit TAIL injection adds extra fire blocks
- FR-10: EnderMan.hurtServer RETURN injection with 50% chance teleport behind attacker
- FR-11: EnderMan.customServerAiStep HEAD injection for proximity aggro check

**Research Flags:**
- MEDIUM confidence: Inner class mixin for Ghast goal needs structure verification
- MEDIUM confidence: Enderman method signatures need verification
- Pitfall MOB-04: Implement teleport-behind with retry limit and cooldown
- Pitfall MOB-05: Modify fireball velocity at spawn time to avoid client desync

---

### Phase 41: NBT Spawn Origin Tagging

**Goal**: Every spawned mob has region origin tracked for cap counting
**Depends on**: Phase 40
**Requirements**: FR-23

**Success Criteria:**
1. NBT viewer on surface-spawned mob shows `spawnSystem.region = "OW_SURFACE"`
2. NBT viewer on upper cave mob shows `spawnSystem.region = "OW_UPPER_CAVE"`
3. NBT viewer on lower cave mob shows `spawnSystem.region = "OW_LOWER_CAVE"`
4. NBT viewer on THC-processed natural spawn shows `spawnSystem.counted = true`
5. Structure spawner mobs show `spawnSystem.counted = false`

**Details:**
- FR-23: Two entity attachments - SPAWN_REGION (String) and SPAWN_COUNTED (Boolean)
- Region calculated at spawn time from position: isSkyVisible + Y level
- Region values: OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE, NETHER, END, OTHER
- Counted = true only for: NATURAL spawn reason + MONSTER category + THC-processed
- Use byte-encoded region attachment to minimize save bloat

**Research Flags:**
- HIGH confidence: Existing THCAttachments pattern
- Pitfall MOB-07: Use byte encoding to minimize NBT save size

---

### Phase 42: Regional Spawn System

**Goal**: Overworld spawns follow region-based distributions with custom mob types
**Depends on**: Phase 41 (requires NBT tagging for region assignment)
**Requirements**: FR-18, FR-19, FR-20, FR-21

**Success Criteria:**
1. Surface has visibly different mob composition (5% witch, 95% vanilla)
2. Upper cave (Y >= 0, no sky) has pillagers, vexes, witches mixed with vanilla
3. Lower cave (Y < 0, no sky) has blazes, breezes, vindicators, pillagers, evokers
4. Cave pillagers have visible melee weapons (iron swords) OR crossbows
5. Custom distribution mobs spawn in packs of 1-4
6. Vanilla fallback spawns use vanilla pack sizes
7. Structure spawns (fortress, mansion, outpost) bypass regional system entirely

**Details:**
- FR-18: Region detection - OW_SURFACE: isSkyVisible(pos), OW_UPPER_CAVE: Y >= 0 && !sky, OW_LOWER_CAVE: Y < 0 && !sky
- FR-19: Custom distributions per region (see REQUIREMENTS.md for exact percentages)
- FR-20: Pillager MELEE variant gets iron sword, RANGED keeps crossbow
- FR-21: Custom spawns use pack size [1, 4], vanilla fallback uses biome pack sizes

**Research Flags:**
- HIGH confidence: Curtain mod provides reference pattern
- Pitfall MOB-06: Set pillager equipment AFTER populateDefaultEquipmentSlots
- Pitfall MOB-08: Don't hard-partition caps, use spawn weights
- Pitfall INT-03: Integrate with existing NaturalSpawnerMixin (claimed chunk check)

---

### Phase 43: Monster Cap Partitioning

**Goal**: Regional caps prevent surface spawns from consuming all cave capacity
**Depends on**: Phase 42 (requires regional spawns and NBT tagging)
**Requirements**: FR-22

**Success Criteria:**
1. Surface cap full (30% of 70 = 21 mobs) - cave spawns continue
2. Upper cave cap full (40% of 70 = 28 mobs) - lower cave and surface spawns continue
3. Lower cave cap full (50% of 70 = 35 mobs) - other regions unaffected
4. Only mobs with `spawnSystem.counted == true` contribute to regional caps
5. Nether and End use vanilla caps unchanged

**Details:**
- FR-22: Per-region caps - OW_SURFACE 30%, OW_UPPER_CAVE 40%, OW_LOWER_CAVE 50%
- Total 120% intentional overlap
- Cap counting uses SPAWN_REGION attachment (spawn origin, not current position)
- Track regional counts during spawn cycle, not via vanilla SpawnState

**Research Flags:**
- LOW confidence: May need custom tracking system rather than modifying vanilla SpawnState
- Pitfall MOB-08: Track by spawn region tag, not current Y position

---

### Phase 44: Damage Rebalancing

**Goal**: Seven mobs have damage values tuned for THC balance
**Depends on**: Phase 43
**Requirements**: FR-16

**Success Criteria:**
1. Vex deals ~4 damage per hit (down from 13.5)
2. Vindicator deals ~11.7 damage (down from 19.5)
3. Evoker fangs deal ~2.5 damage (down from 6)
4. Blaze fireball deals ~3.8 damage (down from 7.5)
5. Piglin arrow deals ~8 damage (up from 4)
6. Large magma cube deals ~4.7 damage (down from 9)
7. Ghast fireball deals ~9.3 damage (up from 9)

**Details:**
- FR-16: Damage modifications via ENTITY_LOAD attribute modifiers
- Same pattern as speed modification in Phase 37
- Projectile damage may require specific mixin on hit methods

**Research Flags:**
- HIGH confidence: Same attribute pattern as speed modification
- Melee damage: ATTACK_DAMAGE attribute
- Projectile damage: May need hit event interception for Blaze, Ghast, Piglin

---

## Requirement Coverage

| Requirement | Phase | Description |
|-------------|-------|-------------|
| FR-01 | 37 | Monster speed increase (20% faster, exclusions) |
| FR-02 | 37 | Monster loot removal (no armor/weapon drops) |
| FR-03 | 38 | Zombie -> Husk replacement |
| FR-04 | 37 | Baby zombie speed normalization |
| FR-05 | 37 | Zombie iron drop removal |
| FR-06 | 38 | Skeleton -> Stray replacement |
| FR-07 | 40 | Ghast projectile speed (+50%) |
| FR-08 | 40 | Ghast fire rate (-25%) |
| FR-09 | 40 | Ghast explosion radius (+100% fire) |
| FR-10 | 40 | Enderman teleport-behind (50% chance) |
| FR-11 | 40 | Enderman proximity aggro (3 blocks) |
| FR-12 | 39 | Vex health reduction (4 hearts) |
| FR-13 | 39 | Vex sword removal |
| FR-14 | 39 | Phantom natural spawn removal |
| FR-15 | 39 | Illager patrol stage-gating (stage 2+) |
| FR-16 | 44 | Damage rebalancing (7 mobs) |
| FR-17 | 39 | Iron golem summon prevention |
| FR-18 | 42 | Regional spawn system (3-way Overworld) |
| FR-19 | 42 | Custom mob distributions per region |
| FR-20 | 42 | Pillager variants (MELEE/RANGED) |
| FR-21 | 42 | Custom pack sizes [1,4] |
| FR-22 | 43 | Partitioned monster caps |
| FR-23 | 41 | NBT spawn origin tagging |

**Coverage:** 23/23 requirements mapped

---

## Progress

| Phase | Name | Plans | Status |
|-------|------|-------|--------|
| 37 | Global Monster Modifications | 1 | Complete |
| 38 | Spawn Table Replacements | 1 | Complete |
| 39 | Entity-Specific Behaviors (Simple) | 2 | Complete |
| 40 | Entity-Specific Behaviors (Complex) | 2 | Complete |
| 41 | NBT Spawn Origin Tagging | - | Pending |
| 42 | Regional Spawn System | - | Pending |
| 43 | Monster Cap Partitioning | - | Pending |
| 44 | Damage Rebalancing | - | Pending |

---

## Non-Functional Requirements

| NFR | Addressed In |
|-----|--------------|
| NFR-01: Performance (<0.1ms per spawn) | Phase 42 (regional detection efficiency) |
| NFR-02: Compatibility (existing THC systems) | All phases (test threat, parry, base blocking) |
| NFR-03: Testability | All phases (F3 debug, observation, game tests) |

---

_For current project status, see .planning/STATE.md_
