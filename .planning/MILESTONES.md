# Project Milestones: THC (True Hardcore)

## v2.5 Enchantment Overhaul (Shipped: 2026-01-28)

**Delivered:** Complete enchantment system overhaul with 12 enchantments removed, single-level display, custom fire damage, lectern enchanting for early-stage books, deterministic book-slot enchanting table, and mob-specific acquisition gating for powerful enchantments.

**Phases completed:** 53-56 (9 plans total)

**Key accomplishments:**

- Removed 12 enchantments from game (loyalty, impaling, riptide, infinity, knockback, punch, quick charge, lunge, thorns, wind burst, multishot, density)
- Single-level enchantment display (no I/II/III suffix) and custom fire damage (Flame 6HP, Fire Aspect 9HP over 6s)
- Lectern enchanting system for stage 1-2 books (mending, unbreaking, efficiency, fortune, silk_touch, lure, luck_of_the_sea) with unlimited use
- Deterministic enchanting table with book-slot mechanic, 15 bookshelf requirement, stage-based level requirements (10/20/30)
- Stage 3+ enchantments removed from Overworld chest loot, now only from specific mob drops (drowned, spider, husk, stray, blaze, magma cube)
- New Soul Dust crafting ingredient and enchanting table recipe (iron blocks + soul dust + book)

**Stats:**

- 49 files created/modified
- ~9,304 lines of Kotlin/Java (cumulative)
- 4 phases, 9 plans, ~24 tasks
- 2 days from start to ship (Jan 27-28, 2026)

**Git range:** `feat(53-01)` → `feat(56-02)`

**What's next:** To be determined

---

## v2.4 Extra Features Batch 8 (Shipped: 2026-01-26)

**Delivered:** Quality-of-life features including iron boats for lava navigation, saddle removal, copper buckets, elytra flight rebalancing, brewing removal, and comprehensive armor stat rebalancing.

**Phases completed:** 46-52 (7 plans total)

**Key accomplishments:**

- Iron boat entity for lava navigation with custom textures and fire immunity
- Saddle removal from all loot sources (no more passive mob riding)
- Copper bucket system (water/milk only, lava placement blocked)
- Skill-based elytra flight (no firework propulsion, pitch-based speed multipliers)
- Brewing economy removal (stands removed from spawns and crafting)
- Armor progression rebalancing (5 tiers with half-point values)

**Stats:**

- ~14,506 lines of Kotlin/Java (cumulative)
- 7 phases, 7 plans
- 1 day (Jan 26, 2026)

**Git range:** `feat(46-01)` → `feat(52-01)`

**What's next:** v2.5 Enchantment Overhaul

---

## v2.3 Extra Features Batch 7 (Shipped: 2026-01-25)

**Delivered:** Comprehensive monster overhaul with regional spawn distributions, behavior modifications for 10+ mob types, and partitioned monster caps creating distinct threat profiles across Overworld regions.

**Phases completed:** 37-45 (13 plans total)

**Key accomplishments:**

- Global monster threat increase: 20% speed boost, baby zombie normalization, complete equipment loot filtering
- Spawn table overhaul: Surface zombies → husks, skeletons → strays with jockey preservation
- Entity behavior modifications: Ghast (faster fireballs, expanded fire), Enderman (teleport-behind, proximity aggro), Vex (health/sword), Phantom removal, Patrol stage-gating, Iron golem prevention
- Regional spawn system: Three-way Overworld split with custom mob distributions (witches, pillagers, blazes, breezes, vindicators, evokers)
- Pillager MELEE/RANGED variants with equipment loadouts
- Partitioned monster caps (30%/40%/50%) preventing surface spawns from consuming cave capacity

**Stats:**

- 78 files created/modified
- ~6,807 lines of Kotlin/Java (cumulative)
- 9 phases, 13 plans, 23 requirements
- 3 days from start to ship (Jan 23-25, 2026)

**Git range:** `feat(37-01)` → `feat(45-01)`

**What's next:** To be determined

---

## v2.2 Extra Features Batch 6 (Shipped: 2026-01-23)

**Delivered:** Food economy overhaul with cooking progression gates and meaningful food choices, plus class/stage system foundation for multiplayer role differentiation.

**Phases completed:** 30-36 (9 plans total)

**Key accomplishments:**

- Smoker gated behind iron acquisition (recipe + village structure removal)
- Universal apple drops from all 9 leaf types at 5x rate
- Instant crop maturation + doubled bonemeal yield (6 per bone)
- Complete food stat rebalancing (29 items across 4 tiers)
- New foods: Hearty Stew (renamed rabbit stew), Honey Apple item
- Class system with permanent role selection (tank/melee/ranged/support)
- Stage system with server-wide progression and per-player boon tracking

**Stats:**

- 59 files created/modified
- ~5,145 lines of Kotlin/Java (cumulative)
- 7 phases, 9 plans, 11 requirements
- 2 days from start to ship (Jan 22-23, 2026)

**Git range:** `feat(30-01)` → `feat(36-02)`

**What's next:** To be determined

---

## v2.1 Extra Features Batch 5 (Shipped: 2026-01-22)

**Delivered:** Survival progression gating (furnaces behind Nether/Evoker), healing skill expression through saturation-tiered regeneration, and improved village protection granularity via structure bounding boxes.

**Phases completed:** 24-29 (7 plans total)

**Key accomplishments:**

- Blast Totem item replaces Totem of Undying in all loot sources
- Furnace recipe requires blaze powder (Nether progression gate)
- Blast furnace recipe requires blast totem (Evoker progression gate)
- Village protection based on structure bounding boxes (underground traversal unrestricted)
- Extended eating duration (3.2s) with saturation cap preserving maximum
- Custom exhaustion (21% faster drain) with hunger ≥18 healing gate
- Saturation-tiered healing: T5 (+1 heart/s) down to T1 (+1/16 heart/s)

**Stats:**

- 44 files created/modified
- ~8,709 lines of Kotlin/Java (cumulative)
- 6 phases, 7 plans, 20 requirements
- 1 day from start to ship (Jan 22, 2026)

**Git range:** feat(24-01) → docs(29)

**What's next:** To be determined

---

## v2.0 Twilight Hardcore (Shipped: 2026-01-22)

**Delivered:** Replaced permanent night with perpetual twilight system - time flows normally but world remains hostile with daylight mob spawns, sun-immune undead, and eternal dusk visuals.

**Phases completed:** 17-23 (7 plans total)

**Key accomplishments:**

- Removed night lock, server time now flows naturally
- Client-side perpetual dusk sky (13000 ticks visual, Overworld only)
- Undead sun immunity (zombies, skeletons, phantoms don't burn)
- Daylight hostile spawns with preserved block light protection
- 24/7 bee productivity regardless of time or weather
- Villager night schedule behavior (always shelter/sleep seeking)
- Anytime bed usage without time skip (spawn points still work)

**Stats:**

- ~4,045 lines of Kotlin/Java (cumulative)
- 7 phases, 7 plans
- 1 day from start to ship (Jan 20, 2026)

**Git range:** feat(17-01) → docs(23)

**What's next:** To be determined

---

## v1.3 Extra Features Batch 3 (Shipped: 2026-01-20)

**Delivered:** Combat rebalancing (melee weakened, ranged gated), wind charge mobility system, threat-based aggro management, and world difficulty tuning for harder exploration with safer bases.

**Phases completed:** 12-16 (13 plans total)

**Key accomplishments:**

- Combat rebalancing: Arrow Speed IV effect, knockback removal, sweeping edge disabled, 75% melee damage reduction
- Wind charge mobility: 12 charges per breeze rod, 50% higher boost, one-time fall damage negation
- Ranged weapon gating: Bows/crossbows require breeze rods from Trial Chambers, removed from all loot
- Threat system: Per-mob threat maps, 15-block propagation, 1/sec decay, arrow +10 bonus, AI targeting
- World difficulty: Max regional difficulty everywhere, mob griefing disabled, base spawn blocking

**Stats:**

- ~3,582 lines of Kotlin/Java (cumulative)
- 5 phases, 13 plans
- 2 days from start to ship (Jan 19-20, 2026)

**Git range:** feat(12-01) → feat(16-03)

**What's next:** To be determined

---

## v1.2 Extra Features Batch 2 (Shipped: 2026-01-19)

**Delivered:** Ranged combat depth through tiered arrows with damage progression, improved buckler crowd control, and XP economy restricted to combat-only.

**Phases completed:** 9-11 (5 plans total)

**Key accomplishments:**

- Enhanced buckler parry with 3-block stun range and ~1 block knockback
- XP economy restricted to combat only (blocked from ores, breeding, fishing, trading, smelting)
- Vanilla arrows renamed to "Flint Arrow" with custom texture
- Tiered arrow system: Iron (+1), Diamond (+2), Netherite (+3) damage
- Anvil crafting for arrow upgrades (64 arrows + material = 64 tiered arrows)

**Stats:**

- 124 files created/modified
- ~2,976 lines of Kotlin/Java (cumulative)
- 3 phases, 5 plans
- 1 day from start to ship (Jan 19, 2026)

**Git range:** 30 commits (589c514 → 1265288)

**What's next:** To be determined (v1.3 planning)

---

## v1.1 Extra Features Batch 1 (Shipped: 2026-01-18)

**Delivered:** Combat and survival tweaks reinforcing risk/reward - drowning is more forgiving, spears removed from player acquisition, projectiles create danger through hit effects and enhanced physics.

**Phases completed:** 6-8 (4 plans total)

**Key accomplishments:**

- Drowning damage 4x slower (every 4 seconds instead of 1)
- Spears removed from all player sources (crafting, loot, mob drops)
- Projectile hit effects: Speed II and Glowing for 6 seconds on target
- Projectile aggro redirection to shooter
- Projectile physics: 20% faster initial velocity, increased gravity after 8 blocks

**Stats:**

- 4 plans across 3 phases
- 1 day from start to ship (Jan 18, 2026)

**Git range:** feat(06-01) → feat(08-02)

**What's next:** v1.2 Extra Features Batch 2

---

## v1.0 Base Claiming System (Shipped: 2026-01-17)

**Delivered:** Complete territorial progression system where players explore for bell-granted land plots, claim flat chunks as protected bases, and face mining fatigue and placement restrictions in the wild.

**Phases completed:** 1-5 + 4.1, 4.2 (13 plans total)

**Key accomplishments:**

- Land plot economy via bell interactions with villager trade removal
- Chunk claiming with terrain flatness validation and village protection
- Base area safety with unrestricted building and combat blocking
- World restrictions with allowlist-only placement and 26-coordinate adjacency rules
- Mining fatigue stacking (1.4^x) with 12-second per-level decay
- Village chunk protection (no destruction except ores and allowlist blocks)
- QoL crafting tweaks (ladder 16x, snowball 64 stack, snow conversion)

**Stats:**

- 58 files created/modified
- ~2,500 lines of Kotlin/Java
- 7 phases (5 integer + 2 inserted bugfixes), 13 plans
- 3 days from start to ship (Jan 15-17, 2026)

**Git range:** `feat(01-01)` → `feat(05-01)` (59 commits)

**What's next:** To be determined (v1.1 planning)

---
