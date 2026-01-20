# Roadmap: THC

## Overview

Combat and survival systems that reinforce risk/reward philosophy. Melee damage weakened, ranged weapons gated behind Trial Chambers, wind charges become core mobility, threat system adds tactical aggro management, and world difficulty maximized while bases remain safe.

## Milestones

- ✅ **v1.0 MVP** — Phases 1-5 (shipped 2026-01-17)
- ✅ **v1.1 Extra Features Batch 1** — Phases 6-8 (shipped 2026-01-18)
- ✅ **v1.2 Extra Features Batch 2** — Phases 9-11 (shipped 2026-01-19)
- 🚧 **v1.3 Extra Features Batch 3** — Phases 12-16 (in progress)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

<details>
<summary>✅ v1.0 MVP (Phases 1-5) — SHIPPED 2026-01-17</summary>

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
<summary>✅ v1.1 Extra Features Batch 1 (Phases 6-8) — SHIPPED 2026-01-18</summary>

**Milestone Goal:** Combat and survival tweaks reinforcing risk/reward — drowning is more forgiving, spears removed from player acquisition, projectiles create danger through hit effects and enhanced physics.

- [x] **Phase 6: Drowning Modification** — Drowning damage 4x slower
- [x] **Phase 7: Spear Removal** — Spears removed from all player sources
- [x] **Phase 8: Projectile Combat** — Hit effects, aggro redirect, enhanced physics

</details>

<details>
<summary>✅ v1.2 Extra Features Batch 2 (Phases 9-11) — SHIPPED 2026-01-19</summary>

**Milestone Goal:** Ranged combat depth through tiered arrows and parry improvements, plus XP economy tightening.

- [x] **Phase 9: Parry Stun Improvements** — Increased stun range (3 blocks) and knockback
- [x] **Phase 10: XP Economy Restriction** — XP orbs only from mob deaths
- [x] **Phase 11: Tiered Arrows** — Flint/Iron/Diamond/Netherite arrows with scaling damage

</details>

### 🚧 v1.3 Extra Features Batch 3 (In Progress)

**Milestone Goal:** Combat rebalancing (melee weakened, ranged gated), wind charge mobility system, threat-based aggro management, and world difficulty tuning.

- [x] **Phase 12: Combat Rebalancing** — Melee damage reduced, arrow effects enhanced
- [x] **Phase 13: Wind Charge Mobility** — Enhanced wind charges as core mobility tool
- [x] **Phase 14: Ranged Weapon Gating** — Bows/crossbows gated behind Trial Chambers
- [x] **Phase 15: Threat System** — Tactical aggro management across mob groups
- [x] **Phase 16: World Difficulty** — Max difficulty everywhere, safer bases

## Phase Details

### Phase 12: Combat Rebalancing
**Goal**: Melee weakened, arrow effects enhanced to push players toward ranged combat
**Depends on**: Nothing (first phase of v1.3)
**Requirements**: COMBAT-01, COMBAT-02, COMBAT-03, COMBAT-04
**Success Criteria** (what must be TRUE):
  1. Arrow hits cause Speed 4 on target mob (not Speed 2)
  2. Arrow hits do not knock back enemy mobs
  3. Sweeping edge enchantment has no effect on weapon hits
  4. All melee damage is reduced by 75%
**Research**: Unlikely (existing ProjectileEntityMixin, LivingEntityMixin patterns)
**Plans**: TBD

Plans:
- [x] 12-01: Arrow combat modifications
- [x] 12-02: Melee damage reduction

### Phase 13: Wind Charge Mobility
**Goal**: Wind charges become core mobility tool with fall damage negation
**Depends on**: Phase 12
**Requirements**: WIND-01, WIND-02, WIND-03
**Success Criteria** (what must be TRUE):
  1. Crafting breeze rod yields 12 wind charges (not 4)
  2. Wind charge self-boost launches player 50% higher than vanilla
  3. After wind charge self-boost, player negates fall damage on next landing
**Research**: Likely (wind charge mechanics, breeze rod crafting, player attachment for fall negation state)
**Research topics**: WindChargeEntity physics, BreezeRod crafting recipe structure, player state tracking for one-time fall negation
**Plans**: TBD

Plans:
- [x] 13-01: Breeze rod yield modification
- [x] 13-02: Wind charge physics and fall negation

### Phase 14: Ranged Weapon Gating
**Goal**: Bows and crossbows require Trial Chamber materials
**Depends on**: Phase 13
**Requirements**: RANGED-01, RANGED-02, RANGED-03, RANGED-04
**Success Criteria** (what must be TRUE):
  1. Bow recipe requires 3 breeze rods + 3 string (no sticks)
  2. Crossbow recipe requires breeze rod + diamond (no sticks/iron)
  3. Bows do not appear in any overworld chest loot tables
  4. Crossbows do not appear in any overworld chest loot tables
  5. Bows do not drop from any mob (skeleton, stray, etc.)
  6. Crossbows do not drop from any mob (pillager, piglin)
**Research**: Unlikely (existing RecipeManagerMixin, LootTableEvents patterns)
**Plans**: TBD

Plans:
- [x] 14-01: Bow and crossbow recipe modifications
- [x] 14-02: Bow and crossbow loot removal

### Phase 15: Threat System
**Goal**: Tactical aggro management where damage creates threat across nearby mobs
**Depends on**: Phase 14
**Requirements**: THREAT-01, THREAT-02, THREAT-03, THREAT-04, THREAT-05, THREAT-06
**Success Criteria** (what must be TRUE):
  1. Each mob maintains a threat map (player → double)
  2. Dealing damage to a mob adds that damage value as threat to all hostile/neutral mobs within 15 blocks
  3. Threat decays by 1 per second per player per mob
  4. Arrow hits add +10 bonus threat to the struck mob
  5. Mobs target highest-threat player when any player's threat ≥ 5 (unless revenge takes priority)
  6. Mobs only switch targets on: revenge strike OR another player gains strictly higher threat
**Research**: Likely (Minecraft AI GoalSelector, Brain system, MobEntity targeting)
**Research topics**: Goal vs Brain AI systems for different mob types, setTarget() behavior, custom goal priority, entity data attachments for threat storage
**Plans**: TBD

Plans:
- [x] 15-01: Threat data structure and storage
- [x] 15-02: Threat propagation on damage
- [x] 15-03: Threat decay system
- [x] 15-04: Threat-based targeting behavior

### Phase 16: World Difficulty
**Goal**: Harder world outside bases, safer inside bases
**Depends on**: Phase 15
**Requirements**: WORLD-01, WORLD-02, WORLD-03, WORLD-04, WORLD-05
**Success Criteria** (what must be TRUE):
  1. Mob griefing is disabled (creepers don't destroy blocks, endermen don't pick up blocks)
  2. Smooth stone drops cobblestone when mined without silk touch
  3. Regional difficulty is always at maximum in every chunk
  4. Moon phase is always treated as "true" for all mob/difficulty checks
  5. Mobs cannot spawn naturally in base chunks
**Research**: Unlikely (gamerule modification, loot tables, spawn events, existing base chunk detection)
**Plans**: TBD

Plans:
- [x] 16-01: Mob griefing and smooth stone
- [x] 16-02: Regional difficulty and moon phase
- [x] 16-03: Base chunk spawn blocking

## Progress

**Execution Order:**
Phases execute in numeric order: 12 → 13 → 14 → 15 → 16

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 12. Combat Rebalancing | v1.3 | 2/2 | Complete | 2026-01-19 |
| 13. Wind Charge Mobility | v1.3 | 2/2 | Complete | 2026-01-19 |
| 14. Ranged Weapon Gating | v1.3 | 2/2 | Complete | 2026-01-19 |
| 15. Threat System | v1.3 | 4/4 | Complete | 2026-01-19 |
| 16. World Difficulty | v1.3 | 3/3 | Complete | 2026-01-20 |

---
*Created: 2026-01-19*
*Current Milestone: v1.3 Extra Features Batch 3*
