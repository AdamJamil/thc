# Roadmap: THC v1.1 Extra Features Batch 1

## Overview

Combat and survival tweaks that reinforce risk/reward philosophy. Drowning becomes more forgiving, spears are removed from player access, and projectiles create danger while traveling further with enhanced physics.

## Milestones

- ✅ **v1.0 MVP** - Phases 1-5 (shipped 2026-01-17)
- 🚧 **v1.1 Extra Features Batch 1** - Phases 6-8 (in progress)

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

### 🚧 v1.1 Extra Features Batch 1 (In Progress)

**Milestone Goal:** Combat and survival tweaks reinforcing risk/reward - projectiles create danger, drowning is forgiving, spears removed.

- [x] **Phase 6: Drowning Modification** - Drowning damage 4x slower
- [x] **Phase 7: Spear Removal** - Spears removed from all player sources
- [x] **Phase 8: Projectile Combat** - Hit effects, aggro redirect, enhanced physics

## Phase Details

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

## Progress

**Execution Order:**
Phases execute in numeric order: 6 → 7 → 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 6. Drowning Modification | 1/1 | Complete | 2026-01-18 |
| 7. Spear Removal | 1/1 | Complete | 2026-01-18 |
| 8. Projectile Combat | 2/2 | Complete | 2026-01-18 |

---
*Created: 2026-01-18*
*Milestone: v1.1 Extra Features Batch 1*
