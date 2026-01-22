# Project Milestones: THC (True Hardcore)

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
