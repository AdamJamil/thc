# Roadmap: THC

## Overview

Combat and survival systems that reinforce risk/reward philosophy. Melee damage weakened, ranged weapons gated behind Trial Chambers, wind charges become core mobility, threat system adds tactical aggro management, world difficulty maximized while bases remain safe, and twilight system creates perpetual hostility.

## Milestones

- SHIPPED **v1.0 MVP** — Phases 1-5 (shipped 2026-01-17)
- SHIPPED **v1.1 Extra Features Batch 1** — Phases 6-8 (shipped 2026-01-18)
- SHIPPED **v1.2 Extra Features Batch 2** — Phases 9-11 (shipped 2026-01-19)
- SHIPPED **v1.3 Extra Features Batch 3** — Phases 12-16 (shipped 2026-01-20)
- SHIPPED **v2.0 Twilight Hardcore** — Phases 17-23 (shipped 2026-01-22)
- 🚧 **v2.1 Extra Features Batch 5** — Phases 24-29 (in progress)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

<details>
<summary>v1.0 MVP (Phases 1-5) — SHIPPED 2026-01-17</summary>

### Phase 1: Foundation
**Goal**: Project scaffolding and base claiming infrastructure
**Status**: Complete

### Phase 2: Core Claiming
**Goal**: Bell rings drop land plots, chunks claimable
**Status**: Complete

### Phase 3: Base Area Permissions
**Goal**: Combat blocked in bases, unrestricted building
**Status**: Complete

### Phase 4: World Restrictions
**Goal**: Mining fatigue, village protection, allowlist placement
**Status**: Complete

### Phase 5: Crafting Tweaks
**Goal**: Ladder yield, snowball stacking, snow conversion
**Status**: Complete

</details>

<details>
<summary>v1.1 Extra Features Batch 1 (Phases 6-8) — SHIPPED 2026-01-18</summary>

**Milestone Goal:** Combat and survival tweaks reinforcing risk/reward — drowning is more forgiving, spears removed from player acquisition, projectiles create danger through hit effects and enhanced physics.

- [x] **Phase 6: Drowning Modification** — Drowning damage 4x slower
- [x] **Phase 7: Spear Removal** — Spears removed from all player sources
- [x] **Phase 8: Projectile Combat** — Hit effects, aggro redirect, enhanced physics

</details>

<details>
<summary>v1.2 Extra Features Batch 2 (Phases 9-11) — SHIPPED 2026-01-19</summary>

**Milestone Goal:** Ranged combat depth through tiered arrows and parry improvements, plus XP economy tightening.

- [x] **Phase 9: Parry Stun Improvements** — Increased stun range (3 blocks) and knockback
- [x] **Phase 10: XP Economy Restriction** — XP orbs only from mob deaths
- [x] **Phase 11: Tiered Arrows** — Flint/Iron/Diamond/Netherite arrows with scaling damage

</details>

<details>
<summary>v1.3 Extra Features Batch 3 (Phases 12-16) — SHIPPED 2026-01-20</summary>

**Milestone Goal:** Combat rebalancing (melee weakened, ranged gated), wind charge mobility system, threat-based aggro management, and world difficulty tuning.

- [x] **Phase 12: Combat Rebalancing** — Melee damage reduced, arrow effects enhanced
- [x] **Phase 13: Wind Charge Mobility** — Enhanced wind charges as core mobility tool
- [x] **Phase 14: Ranged Weapon Gating** — Bows/crossbows gated behind Trial Chambers
- [x] **Phase 15: Threat System** — Tactical aggro management across mob groups
- [x] **Phase 16: World Difficulty** — Max difficulty everywhere, safer bases

</details>

<details>
<summary>v2.0 Twilight Hardcore (Phases 17-23) — SHIPPED 2026-01-22</summary>

**Milestone Goal:** Replace night-lock with a twilight system where time flows normally but the world remains perpetually hostile — mobs spawn in daylight, undead don't burn, and clients see eternal dusk.

- [x] **Phase 17: Remove Night Lock** — Delete existing time-lock code
- [x] **Phase 18: Twilight Visuals** — Client sees perpetual dusk sky
- [x] **Phase 19: Undead Sun Immunity** — Zombies/skeletons/phantoms don't burn
- [x] **Phase 20: Hostile Spawn Bypass** — Monsters spawn regardless of sky light
- [x] **Phase 21: Bee Always-Work** — Bees work 24/7 regardless of time/weather
- [x] **Phase 22: Villager Twilight** — Villagers behave as if always night
- [x] **Phase 23: Bed Mechanics** — Beds always usable, don't skip time

</details>

### 🚧 v2.1 Extra Features Batch 5 (In Progress)

**Milestone Goal:** Survival progression gating through furnace requirements, healing skill expression through saturation management, and improved village protection granularity.

- [x] **Phase 24: Blast Totem** — Custom item replaces Totem of Undying
- [ ] **Phase 25: Furnace Gating** — Furnaces require blaze powder, blast furnace requires blast totem
- [ ] **Phase 26: Structure Protection** — Village protection based on structure bounding boxes
- [ ] **Phase 27: Eating Mechanics** — Saturation cap behavior, longer eating duration
- [ ] **Phase 28: Exhaustion & Healing** — Faster exhaustion, hunger-gated healing
- [ ] **Phase 29: Saturation Tiers** — Healing rate scales with saturation level

## Phase Details (v2.1)

### Phase 24: Blast Totem
**Goal**: Blast Totem item exists and replaces Totem of Undying everywhere
**Depends on**: Nothing (first phase of v2.1)
**Requirements**: PROG-01, PROG-02
**Success Criteria** (what must be TRUE):
  1. Player can obtain Blast Totem from any source that previously dropped Totem of Undying
  2. Blast Totem displays custom texture (blast_totem.png) in inventory
  3. Totem of Undying no longer appears in any loot table or chest
**Research**: Unlikely (established item/loot table patterns)
**Plans**: TBD

### Phase 25: Furnace Gating
**Goal**: Furnace progression gated behind Nether access
**Depends on**: Phase 24 (blast totem needed for blast furnace recipe)
**Requirements**: PROG-03, PROG-04, PROG-05, PROG-06
**Success Criteria** (what must be TRUE):
  1. Furnace cannot be crafted with vanilla recipe (requires blaze powder)
  2. Furnaces do not spawn naturally in villages
  3. Blast furnace requires furnace + blast totem to craft
  4. Blast furnaces do not spawn naturally in villages
**Research**: Likely (world generation removal)
**Research topics**: Disabling village structure features, furnace/blast furnace spawn removal patterns
**Plans**: TBD

### Phase 26: Structure Protection
**Goal**: Village protection based on structures, not chunks
**Depends on**: Nothing (independent system)
**Requirements**: PROT-01, PROT-02, PROT-03
**Success Criteria** (what must be TRUE):
  1. Player cannot break non-ore blocks within village structure bounding boxes
  2. Player can mine freely underground below villages
  3. Current chunk-based protection replaced with structure-based protection
**Research**: Likely (structure bounding box API)
**Research topics**: Structure bounding box API, StructureStart queries, replacing chunk-based checks
**Plans**: TBD

### Phase 27: Eating Mechanics
**Goal**: Eating provides saturation cap behavior and takes longer
**Depends on**: Nothing (independent system)
**Requirements**: HEAL-01, HEAL-02
**Success Criteria** (what must be TRUE):
  1. Eating food sets saturation to max of food's saturation and current saturation
  2. Eating takes 64 ticks (3.2 seconds) instead of vanilla 32 ticks
**Research**: Unlikely (established mixin patterns for food)
**Plans**: TBD

### Phase 28: Exhaustion & Base Healing
**Goal**: Exhaustion drains faster, healing requires high hunger
**Depends on**: Nothing (independent system)
**Requirements**: HEAL-03, HEAL-04, HEAL-05, HEAL-11
**Success Criteria** (what must be TRUE):
  1. 4.0 exhaustion removes 1.21 saturation (faster drain)
  2. Player does not heal below hunger level 18
  3. Player heals at 3/16 hearts/second when hunger ≥ 18
  4. Vanilla natural regeneration gamerule effect disabled
**Research**: Likely (exhaustion internals)
**Research topics**: FoodData exhaustion handling, natural regeneration disable approach
**Plans**: TBD

### Phase 29: Saturation Tiers
**Goal**: Healing rate scales with saturation tier
**Depends on**: Phase 28 (builds on healing infrastructure)
**Requirements**: HEAL-06, HEAL-07, HEAL-08, HEAL-09, HEAL-10
**Success Criteria** (what must be TRUE):
  1. Saturation 6.36+ adds +1 heart/s healing
  2. Saturation 2.73+ adds +0.5 heart/s healing
  3. Saturation 1.36+ adds +3/16 heart/s healing
  4. Saturation 0.45+ adds +1/8 heart/s healing
  5. Saturation 0-0.45 adds +1/16 heart/s healing
**Research**: Unlikely (builds on Phase 28 infrastructure)
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 24 → 25 → 26 → 27 → 28 → 29

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1-5 | v1.0 | 13/13 | Complete | 2026-01-17 |
| 6-8 | v1.1 | 4/4 | Complete | 2026-01-18 |
| 9-11 | v1.2 | 5/5 | Complete | 2026-01-19 |
| 12-16 | v1.3 | 13/13 | Complete | 2026-01-20 |
| 17-23 | v2.0 | 7/7 | Complete | 2026-01-22 |
| 24. Blast Totem | v2.1 | 1/1 | Complete | 2026-01-22 |
| 25. Furnace Gating | v2.1 | 0/TBD | Not started | - |
| 26. Structure Protection | v2.1 | 0/TBD | Not started | - |
| 27. Eating Mechanics | v2.1 | 0/TBD | Not started | - |
| 28. Exhaustion & Healing | v2.1 | 0/TBD | Not started | - |
| 29. Saturation Tiers | v2.1 | 0/TBD | Not started | - |

**Cumulative:** 42 plans across 23 phases in 5 milestones (v2.1 in progress)

---
*Created: 2026-01-19*
*Last updated: 2026-01-22 after v2.1 roadmap creation*
