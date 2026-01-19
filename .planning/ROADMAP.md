# Roadmap: THC

## Overview

Combat and survival systems that reinforce risk/reward philosophy. Base claiming creates safe zones, combat requires active defense via bucklers, projectiles create danger, and progression tightens around meaningful combat.

## Milestones

- ✅ **v1.0 MVP** - Phases 1-5 (shipped 2026-01-17)
- ✅ **v1.1 Extra Features Batch 1** - Phases 6-8 (shipped 2026-01-18)
- 🚧 **v1.2 Extra Features Batch 2** - Phases 9-11 (in progress)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

<details>
<summary>✅ v1.0 MVP (Phases 1-5) - SHIPPED 2026-01-17</summary>

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
<summary>✅ v1.1 Extra Features Batch 1 (Phases 6-8) - SHIPPED 2026-01-18</summary>

**Milestone Goal:** Combat and survival tweaks reinforcing risk/reward - projectiles create danger, drowning is forgiving, spears removed.

- [x] **Phase 6: Drowning Modification** - Drowning damage 4x slower
- [x] **Phase 7: Spear Removal** - Spears removed from all player sources
- [x] **Phase 8: Projectile Combat** - Hit effects, aggro redirect, enhanced physics

</details>

### 🚧 v1.2 Extra Features Batch 2 (In Progress)

**Milestone Goal:** Ranged combat depth through tiered arrows and parry improvements, plus XP economy tightening.

- [ ] **Phase 9: Parry Stun Improvements** - Increased stun range (3 blocks) and knockback
- [ ] **Phase 10: XP Economy Restriction** - XP orbs only from mob deaths
- [ ] **Phase 11: Tiered Arrows** - Flint/Iron/Diamond/Netherite arrows with scaling damage

## Phase Details

<details>
<summary>v1.1 Phase Details (Phases 6-8)</summary>

### Phase 6: Drowning Modification
**Goal**: Drowning damage is more forgiving underwater
**Depends on**: Nothing (first phase of v1.1)
**Requirements**: DROWN-01
**Success Criteria** (what must be TRUE):
  1. Once drowning begins, damage ticks occur every 4 seconds instead of every 1 second
**Research**: Unlikely (single mixin, well-documented LivingEntity methods)
**Plans**: TBD

Plans:
- [x] 06-01: Drowning tick rate modification

### Phase 7: Spear Removal
**Goal**: Spears cannot be obtained by players
**Depends on**: Phase 6
**Requirements**: SPEAR-01, SPEAR-02, SPEAR-03
**Success Criteria** (what must be TRUE):
  1. Player cannot craft any spear (all 7 tiers disabled)
  2. Spears do not appear in structure loot chests (ocean ruins, village weaponsmith, buried treasure, bastion, end city)
  3. Mobs that spawn with spears do not drop them on death
**Research**: Unlikely (extends existing RecipeManagerMixin pattern, uses LootTableEvents)
**Plans**: TBD

Plans:
- [x] 07-01: Spear crafting and loot removal

### Phase 8: Projectile Combat
**Goal**: Player projectiles create danger and have enhanced physics
**Depends on**: Phase 7
**Requirements**: PROJ-01, PROJ-02, PROJ-03, PROJ-04, PROJ-05
**Success Criteria** (what must be TRUE):
  1. Player projectile hit applies Speed II to target mob for 6 seconds
  2. Player projectile hit applies Glowing to target mob for 6 seconds
  3. Player projectile hit redirects mob aggro to the shooter
  4. Player projectiles travel 20% faster initially (velocity boost on launch)
  5. Player projectiles experience increased gravity after traveling 8 blocks
**Research**: Likely (quadratic gravity formula needs design clarification)
**Research topics**: Gravity formula interpretation (distance from spawn vs cumulative), projectile class scope (arrows, snowballs, eggs, ender pearls)
**Plans**: TBD

Plans:
- [x] 08-01: Projectile hit effects and aggro
- [x] 08-02: Projectile physics overhaul

</details>

### Phase 9: Parry Stun Improvements
**Goal**: Buckler parry creates stronger crowd control
**Depends on**: Nothing (first phase of v1.2)
**Requirements**: PARRY-01, PARRY-02
**Success Criteria** (what must be TRUE):
  1. Successful buckler parry stuns all enemies within 3 blocks of the parrying player
  2. Stunned enemies receive approximately 1 block of knockback away from the parrying player
**Research**: Unlikely (existing buckler mixin infrastructure)
**Plans**: TBD

Plans:
- [ ] 09-01: Parry stun range and knockback

### Phase 10: XP Economy Restriction
**Goal**: XP orbs only come from combat (mob kills)
**Depends on**: Phase 9
**Requirements**: XP-01, XP-02, XP-03, XP-04, XP-05, XP-06, XP-07
**Success Criteria** (what must be TRUE):
  1. Killing a mob spawns XP orbs as normal
  2. Mining ores (coal, lapis, redstone, emerald, diamond, nether quartz) does not spawn XP orbs
  3. Breeding animals does not spawn XP orbs
  4. Fishing does not spawn XP orbs
  5. Trading with villagers does not spawn XP orbs
  6. Smelting items in furnaces does not accumulate/spawn XP
  7. Throwing bottles o' enchanting does not spawn XP orbs
**Research**: Likely (need to identify all XP spawn mechanisms and intercept points)
**Research topics**: XP spawn events/methods for each source, mixin targets for blocking
**Plans**: TBD

Plans:
- [ ] 10-01: XP spawn blocking system

### Phase 11: Tiered Arrows
**Goal**: Ranged combat has progression through tiered arrow damage
**Depends on**: Phase 10
**Requirements**: ARROW-01, ARROW-02, ARROW-03, ARROW-04, ARROW-05
**Success Criteria** (what must be TRUE):
  1. Vanilla arrow shows as "Flint Arrow" with flint-based texture
  2. Iron Arrow exists, craftable via anvil (64 flint arrows + 1 iron ingot), deals +1 damage
  3. Diamond Arrow exists, craftable via anvil (64 flint arrows + 1 diamond), deals +2 damage
  4. Netherite Arrow exists, craftable via anvil (64 flint arrows + 1 netherite ingot), deals +3 damage
  5. Each tiered arrow has distinct visual texture
**Research**: Likely (custom item registration, anvil recipe API, projectile damage modification)
**Research topics**: Fabric item registration for arrows, anvil crafting mechanics, arrow damage hooks
**Plans**: TBD

Plans:
- [ ] 11-01: Flint arrow rename and texture
- [ ] 11-02: Tiered arrow items and recipes
- [ ] 11-03: Arrow damage modification

## Progress

**Execution Order:**
Phases execute in numeric order: 9 → 10 → 11

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 6. Drowning Modification | v1.1 | 1/1 | Complete | 2026-01-18 |
| 7. Spear Removal | v1.1 | 1/1 | Complete | 2026-01-18 |
| 8. Projectile Combat | v1.1 | 2/2 | Complete | 2026-01-18 |
| 9. Parry Stun Improvements | v1.2 | 0/1 | Not started | - |
| 10. XP Economy Restriction | v1.2 | 0/1 | Not started | - |
| 11. Tiered Arrows | v1.2 | 0/3 | Not started | - |

---
*Created: 2026-01-18*
*Updated: 2026-01-19 (added v1.2 phases)*
*Current Milestone: v1.2 Extra Features Batch 2*
