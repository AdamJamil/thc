# Requirements: v2.3 Extra Features Batch 7

**Milestone:** Monster Overhaul
**Created:** 2026-01-23
**Source:** MILESTONE_EXTRA_FEATURES_BATCH_7.md + Research synthesis

---

## Functional Requirements

### FR-01: Monster Speed Increase
All hostile mobs move 20% faster.

**Exceptions:**
- Creepers (unchanged speed)
- Baby zombies (normalized to adult speed, not boosted)

**Acceptance:** Spawn zombie and creeper side-by-side. Zombie visibly faster. Baby zombie same speed as adult.

---

### FR-02: Monster Loot Removal
Monsters never drop armor or weapons.

**Acceptance:** Kill 50 zombies/skeletons in full armor. Zero armor/weapon drops.

---

### FR-03: Zombie -> Husk Replacement
All natural zombie spawns produce husks instead.

**Exceptions:**
- Structure spawns (dungeons, etc.) use vanilla pools
- Zombie reinforcements may still spawn zombies (document behavior)
- Drowned remain drowned (no conversion chain effect)

**Acceptance:** AFK in plains biome 10 minutes. Zero zombies, only husks.

---

### FR-04: Baby Zombie Speed Normalization
Baby zombies move at the same speed as adult zombies (not 1.5x faster).

**Acceptance:** Spawn baby zombie and adult zombie. Identical movement speed.

---

### FR-05: Zombie Iron Drop Removal
Zombies (and husks) no longer drop iron ingots.

**Acceptance:** Kill 100 husks. Zero iron drops.

---

### FR-06: Skeleton -> Stray Replacement
All natural skeleton spawns produce strays instead.

**Exceptions:**
- Structure spawns (dungeons, etc.) use vanilla pools
- Spider jockeys may still spawn skeletons (document behavior)
- Skeleton trap horses remain skeletons

**Acceptance:** AFK in plains biome 10 minutes. Zero skeletons, only strays.

---

### FR-07: Ghast Projectile Speed
Ghast fireballs travel 50% faster.

**Acceptance:** Fight ghast, fireballs visibly faster. Still deflectable.

---

### FR-08: Ghast Fire Rate
Ghasts fire 25% less frequently (every 4 seconds instead of 3).

**Acceptance:** Time 10 ghast shots. Average interval ~4 seconds.

---

### FR-09: Ghast Explosion Radius
Ghast fireball explosions have 100% larger fire spread radius.

**Acceptance:** Let ghast fireball hit ground. Fire spreads noticeably further than vanilla.

---

### FR-10: Enderman Teleport-Behind
Endermen have 50% chance to teleport 3 blocks behind the player after:
- Player damages enderman
- Enderman damages player

**Acceptance:** Fight enderman 20 times. ~50% of exchanges result in enderman appearing behind player.

---

### FR-11: Enderman Proximity Aggro
Endermen aggro if player is within 3 blocks (regardless of eye contact).

**Acceptance:** Walk within 3 blocks of neutral enderman. Enderman aggros.

---

### FR-12: Vex Health Reduction
Vex health reduced to 4 hearts (from 14).

**Acceptance:** Spawn vex, check health with F3. Shows 8 HP (4 hearts).

---

### FR-13: Vex Sword Removal
Vexes no longer spawn with iron swords.

**Acceptance:** Trigger evoker summon. Vexes appear without visible weapon.

---

### FR-14: Phantom Natural Spawn Removal
Phantoms no longer spawn naturally from insomnia.

**Note:** Phantoms from spawn eggs, commands, or structures (if any) still work.

**Acceptance:** Don't sleep for 3+ in-game days. Zero phantom spawns.

---

### FR-15: Illager Patrol Stage-Gating
Illager patrols only spawn when server is at stage 2 or above.

**Acceptance:** At stage 1, no patrols spawn after extended play. Advance to stage 2, patrols resume.

---

### FR-16: Damage Rebalancing
Seven mobs have damage values adjusted:

| Mob | Old Damage | New Damage |
|-----|------------|------------|
| Vex | 13.5 | 4 |
| Vindicator | 19.5 | 11.7 |
| Evoker (fangs) | 6 | 2.5 |
| Blaze (fireball) | 7.5 | 3.8 |
| Piglin (arrow) | 4 | 8 |
| Magma cube (large) | 9 | 4.7 |
| Ghast | 9 | 9.3 |

**Acceptance:** Take damage from each mob type. Damage matches table values (+/-0.5 for rounding).

---

### FR-17: Iron Golem Summon Prevention
Players cannot summon iron golems using the pumpkin + iron block pattern.

**Note:** Naturally spawning village golems may still work (verify with user).

**Acceptance:** Build iron golem pattern. No golem spawns.

---

### FR-18: Regional Spawn System (Overworld)
Natural monster spawning uses region-based selection:

**Regions:**
- **OW_SURFACE:** `isSkyVisible(pos)` (regardless of Y)
- **OW_UPPER_CAVE:** `pos.y >= 0 && !isSkyVisible(pos)`
- **OW_LOWER_CAVE:** `pos.y < 0 && !isSkyVisible(pos)`

**Structure bypass:** Structure-provided spawn pools (fortress, outpost, mansion) bypass this system entirely.

**Acceptance:** Different mob composition visible at surface vs upper cave vs lower cave.

---

### FR-19: Custom Mob Distributions

**OW_SURFACE:**
- 5% witch
- 95% vanilla fallback

**OW_UPPER_CAVE:**
- 5% witch
- 2% vex
- 10% pillager (RANGED)
- 25% pillager (MELEE)
- 58% vanilla fallback

**OW_LOWER_CAVE:**
- 8% blaze
- 8% breeze
- 12% vindicator
- 25% pillager (MELEE)
- 2% evoker
- 45% vanilla fallback

**END:**
- 25% endermite
- 75% vanilla fallback

**NETHER:** No changes (vanilla behavior)

**Acceptance:** Spawn 100 mobs in each region. Distribution roughly matches percentages (+/-10%).

---

### FR-20: Pillager Variants

**MELEE pillager:** Spawns with melee weapon (iron sword), no crossbow.
**RANGED pillager:** Spawns with crossbow (vanilla behavior).

Variants selected per custom distribution probabilities.

**Acceptance:** Cave pillagers have visible melee weapons. Some have crossbows, some have swords.

---

### FR-21: Custom Pack Sizes
Mobs spawned via custom distribution use pack size [1, 4].
Vanilla fallback spawns use vanilla pack sizes.

**Acceptance:** Observe cave mob spawns. Custom mobs spawn in groups of 1-4.

---

### FR-22: Partitioned Monster Caps

**Overworld regional caps (multipliers of vanilla cap):**
- OW_SURFACE_CAP: 30% of vanilla
- OW_UPPER_CAVE_CAP: 40% of vanilla
- OW_LOWER_CAVE_CAP: 50% of vanilla

Total: 120% (intentional overlap)

**Counting rule:** Only mobs with `spawnSystem.counted == true` NBT marker count toward regional caps.

**Nether/End:** Vanilla caps unchanged.

**Acceptance:** With surface cap reached, cave spawns continue. Vice versa.

---

### FR-23: NBT Spawn Origin Tagging

All spawned entities receive NBT tags:
- `spawnSystem.region`: String enum (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE, NETHER, END, OTHER)
- `spawnSystem.counted`: Boolean (true only for NATURAL + MONSTER + THC-processed spawns)

**Acceptance:** Use NBT viewer on spawned mobs. Tags present with correct values.

---

## Non-Functional Requirements

### NFR-01: Performance
Regional detection (Y-level + sky check) must not cause measurable TPS impact during spawn cycles.

**Target:** <0.1ms per spawn attempt.

---

### NFR-02: Compatibility
Must not break existing THC systems:
- Base chunk spawn blocking
- Threat system propagation
- Stage system queries
- Parry stun effectiveness

---

### NFR-03: Testability
Each feature should be verifiable via:
- In-game observation (primary)
- F3 debug screen where applicable
- Game test where practical

---

## Out of Scope

- Biome-specific distributions (use dimension/region only)
- Mob cooldowns or per-mob spawn limits
- Finite vex lifetime
- Structure spawn pool modifications
- Nametagged mob cap changes
- Config file for distribution tweaking (hardcoded for now)

---

## Dependencies

| Requirement | Depends On |
|-------------|------------|
| FR-15 (Patrol gating) | Existing stage system |
| FR-19-22 (Regional system) | FR-23 (NBT tagging) |
| FR-04 (Baby zombie) | FR-01 (Speed increase) |

---

## Acceptance Test Summary

| # | Test | Pass Criteria |
|---|------|---------------|
| 1 | Speed comparison | Zombie faster than creeper |
| 2 | Loot check | No armor/weapon drops from 50 kills |
| 3 | Husk spawns | Only husks in overworld surface |
| 4 | Stray spawns | Only strays in overworld surface |
| 5 | Ghast fight | Faster fireballs, slower rate, bigger fires |
| 6 | Enderman proximity | Aggro within 3 blocks |
| 7 | Vex stats | 4 hearts, no sword |
| 8 | No phantoms | 3 days no sleep, no spawns |
| 9 | Regional mobs | Different mobs at different depths |
| 10 | Cap partitioning | Surface full doesn't block caves |

---

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| FR-01 | Phase 37 | Complete |
| FR-02 | Phase 37 | Complete |
| FR-03 | Phase 38 | Complete |
| FR-04 | Phase 37 | Complete |
| FR-05 | Phase 37 | Complete |
| FR-06 | Phase 38 | Complete |
| FR-07 | Phase 40 | Complete |
| FR-08 | Phase 40 | Complete |
| FR-09 | Phase 40 | Complete |
| FR-10 | Phase 40 | Complete |
| FR-11 | Phase 40 | Complete |
| FR-12 | Phase 39 | Complete |
| FR-13 | Phase 39 | Complete |
| FR-14 | Phase 39 | Complete |
| FR-15 | Phase 39 | Complete |
| FR-16 | Phase 44 | Pending |
| FR-17 | Phase 39 | Complete |
| FR-18 | Phase 42 | Complete |
| FR-19 | Phase 42 | Complete |
| FR-20 | Phase 42 | Complete |
| FR-21 | Phase 42 | Complete |
| FR-22 | Phase 43 | Complete |
| FR-23 | Phase 41 | Complete |

---

*Requirements derived from MILESTONE_EXTRA_FEATURES_BATCH_7.md specification.*
