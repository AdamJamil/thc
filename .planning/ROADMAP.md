# Roadmap: THC

## Overview

Combat and survival systems that reinforce risk/reward philosophy. Melee damage weakened, ranged weapons gated behind Trial Chambers, wind charges become core mobility, threat system adds tactical aggro management, and world difficulty maximized while bases remain safe.

## Milestones

- ✅ **v1.0 MVP** — Phases 1-5 (shipped 2026-01-17)
- ✅ **v1.1 Extra Features Batch 1** — Phases 6-8 (shipped 2026-01-18)
- ✅ **v1.2 Extra Features Batch 2** — Phases 9-11 (shipped 2026-01-19)
- ✅ **v1.3 Extra Features Batch 3** — Phases 12-16 (shipped 2026-01-20)
- 🚧 **v2.0 Twilight Hardcore** — Phases 17-23 (in progress)

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

## Progress

**All milestones shipped.**

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1-5 | v1.0 | 13/13 | Complete | 2026-01-17 |
| 6-8 | v1.1 | 4/4 | Complete | 2026-01-18 |
| 9-11 | v1.2 | 5/5 | Complete | 2026-01-19 |
| 12-16 | v1.3 | 13/13 | Complete | 2026-01-20 |

**Cumulative:** 35 plans across 16 phases in 4 milestones

---

### 🚧 v2.0 Twilight Hardcore (In Progress)

**Milestone Goal:** Replace night-lock with a twilight system where time flows normally but the world remains perpetually hostile — mobs spawn in daylight, undead don't burn, and clients see eternal dusk.

- [x] **Phase 17: Remove Night Lock** — Delete existing time-lock code
- [x] **Phase 18: Twilight Visuals** — Client sees perpetual dusk sky
- [x] **Phase 19: Undead Sun Immunity** — Zombies/skeletons/phantoms don't burn
- [x] **Phase 20: Hostile Spawn Bypass** — Monsters spawn regardless of sky light
- [ ] **Phase 21: Bee Always-Work** — Bees work 24/7 regardless of time/weather
- [ ] **Phase 22: Villager Twilight** — Villagers behave as if always night
- [ ] **Phase 23: Bed Mechanics** — Beds always usable, don't skip time

## Phase Details (v2.0)

### Phase 17: Remove Night Lock
**Goal**: Server time flows normally again
**Depends on**: Nothing (first v2.0 phase)
**Requirements**: TIME-01, TIME-02
**Success Criteria** (what must be TRUE):
  1. Server time advances continuously (doDaylightCycle active)
  2. Existing night-lock mixin is removed/disabled
  3. Game starts without time being frozen
**Research**: Unlikely (removal only)
**Plans**: TBD

### Phase 18: Twilight Visuals
**Goal**: Client sees perpetual dusk regardless of actual server time
**Depends on**: Phase 17
**Requirements**: SKY-01, SKY-02
**Success Criteria** (what must be TRUE):
  1. Sky appears at dusk (~13000 ticks visual) at all server times
  2. Ambient lighting matches dusk atmosphere
  3. Nether and End dimensions remain unaffected
  4. Other players and mobs render normally
**Research**: Likely (new client mixin pattern)
**Research topics**: ClientLevel.getDayTime() injection, shader compatibility
**Plans**: TBD

### Phase 19: Undead Sun Immunity
**Goal**: Undead mobs never burn from sunlight
**Depends on**: Phase 17
**Requirements**: MOB-01, MOB-02, MOB-03, MOB-04
**Success Criteria** (what must be TRUE):
  1. Zombies survive in daylight without burning
  2. Skeletons survive in daylight without burning
  3. Phantoms survive in daylight without burning
  4. Fire Aspect, lava, and other fire sources still damage undead
**Research**: Unlikely (established mixin pattern via isSunBurnTick)
**Plans**: TBD

### Phase 20: Hostile Spawn Bypass
**Goal**: Monsters spawn regardless of sky light level
**Depends on**: Phase 17
**Requirements**: SPAWN-01, SPAWN-02
**Success Criteria** (what must be TRUE):
  1. Hostile mobs spawn during server daytime
  2. Block light still affects spawn density (vanilla behavior preserved)
  3. Base chunk spawn blocking still works
**Research**: Unlikely (existing spawn pattern from v1.3)
**Plans**: TBD

### Phase 21: Bee Always-Work
**Goal**: Bees work continuously regardless of time/weather
**Depends on**: Phase 17
**Requirements**: BEE-01, BEE-02, BEE-03
**Success Criteria** (what must be TRUE):
  1. Bees collect nectar at any server time
  2. Bees collect nectar during rain
  3. Bees still return to hive when nectar-full (behavior preserved)
**Research**: Unlikely (verified wantsToEnterHive target)
**Plans**: TBD

### Phase 22: Villager Twilight
**Goal**: Villagers behave as if it's always night
**Depends on**: Phase 17
**Requirements**: VILLAGER-01, VILLAGER-02
**Success Criteria** (what must be TRUE):
  1. Villagers seek shelter/beds continuously
  2. Villagers follow night schedule for sleep purposes
**Research**: Likely (villager Brain system, schedule activities)
**Research topics**: Villager schedule system, Brain vs Goal AI
**Plans**: TBD

### Phase 23: Bed Mechanics
**Goal**: Beds always usable, no time skip
**Depends on**: Phase 17
**Requirements**: BED-01, BED-02, BED-03
**Success Criteria** (what must be TRUE):
  1. Player can use beds at any server time
  2. Sleeping does not advance the day/night cycle
  3. Beds still set spawn point when used
**Research**: Likely (sleep and respawn system)
**Research topics**: BedBlock interaction, ServerLevel sleep handling
**Plans**: TBD

## Progress (v2.0)

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 17. Remove Night Lock | 1/1 | Complete | 2026-01-20 |
| 18. Twilight Visuals | 1/1 | Complete | 2026-01-20 |
| 19. Undead Sun Immunity | 1/1 | Complete | 2026-01-20 |
| 20. Hostile Spawn Bypass | 1/1 | Complete | 2026-01-20 |
| 21. Bee Always-Work | 0/TBD | Not started | - |
| 22. Villager Twilight | 0/TBD | Not started | - |
| 23. Bed Mechanics | 0/TBD | Not started | - |

---
*Created: 2026-01-19*
*Last updated: 2026-01-20 after phase 20 completion*
