# Milestone v2.4: Extra Features Batch 8

**Status:** In Progress
**Phases:** 46-52
**Total Requirements:** 24

## Overview

Mobility and survival mechanics expansion with resource economy restrictions. Iron boats enable lava navigation (Nether traversal), copper buckets provide early-game water transport at reduced capability. Elytra flight rebalanced for skill-based diving mechanics. Economy restrictions remove brewing completely (structure spawns, crafting, bartering) and saddles from all sources. Armor progression rebalanced with copper tier insertion and half armor point support.

## Phases

### Phase 46: Iron Boat

**Goal**: Players can craft and use iron boats for safe lava navigation
**Depends on**: Nothing (first phase)
**Requirements**: BOAT-01, BOAT-02, BOAT-03, BOAT-04, BOAT-05
**Plans:** 3 plans

Plans:
- [ ] 46-01-PLAN.md — Entity + item registration with lava physics and damage immunity
- [ ] 46-02-PLAN.md — Client renderer and asset files (recipe, models, lang)
- [ ] 46-03-PLAN.md — Passenger fire protection mixin and verification

**Success Criteria:**
1. Player can craft iron boat using 5 iron ingots + magma cream in minecart shape
2. Iron boat placed on lava floats and is controllable like water boats
3. Iron boat displays custom iron_boat.png texture when riding
4. Broken iron boat drops lava-proof item that floats on lava surface
5. Iron boat item flies towards player when boat entity is destroyed

**Details:**
- Custom item registration with recipe data generation
- Custom entity or vanilla boat extension for lava compatibility
- Item entity behavior override for lava immunity and floating
- Fly-to-player behavior on boat break (similar to trident loyalty)

---

### Phase 47: Saddle Removal

**Goal**: Saddles are completely unobtainable, removing mounted combat
**Depends on**: Nothing (independent)
**Requirements**: SADL-01, SADL-02, SADL-03

**Success Criteria:**
1. Player cannot find saddles in any chest loot (dungeons, temples, etc.)
2. Mobs that would drop saddles (Ravagers, saddled pigs/horses) no longer drop them
3. No saddle recipe exists in crafting menu

**Details:**
- Loot table modification pattern (established in prior milestones)
- Mob drop filtering via loot event interception
- Recipe removal via REMOVED_RECIPE_PATHS pattern

---

### Phase 48: Copper Bucket

**Goal**: Players have early-game bucket option with water/milk restriction
**Depends on**: Nothing (independent)
**Requirements**: BUCK-01, BUCK-02, BUCK-03

**Success Criteria:**
1. Player can craft copper bucket using 3 copper ingots in bucket pattern
2. Copper bucket can scoop and place water (fills, empties normally)
3. Copper bucket can milk cows (fills with milk, drinkable)
4. Copper bucket cannot pick up lava, powder snow, or other non-water fluids
5. All copper bucket states display correct custom textures

**Details:**
- Custom item registration with three texture variants
- Fluid interaction restriction via item use event
- Recipe data generation with copper ingot inputs

---

### Phase 49: Fluid Placement Mechanics

**Goal**: Bucket-based fluid economy restricts infinite water and lava placement
**Depends on**: Phase 48 (copper bucket must exist first)
**Requirements**: BUCK-04, WATR-01, WATR-02

**Success Criteria:**
1. Right-clicking with lava bucket does nothing (lava placement blocked)
2. Water placed from any bucket creates flowing water at max height (not source)
3. Placed water flows normally and eventually drains away (no infinite sources)

**Details:**
- BucketItem use interception for lava placement blocking
- Water placement override to spawn flowing water instead of source
- Vanilla water physics preserved (natural flow and drain)

---

### Phase 50: Elytra Flight Changes

**Goal**: Elytra flight requires skill-based diving instead of firework spam
**Depends on**: Nothing (independent)
**Requirements**: ELYT-01, ELYT-02, ELYT-03

**Success Criteria:**
1. Using firework rocket during elytra flight does not boost player speed
2. Player gains 2x speed multiplier when diving (pitch below horizon)
3. Player loses 1.8x speed when ascending (pitch above horizon)

**Details:**
- Firework boost cancellation via player flight event or mixin
- Pitch-based velocity modification during elytra tick
- Speed multipliers applied to existing elytra physics

---

### Phase 51: Brewing Removal

**Goal**: Potions are completely unobtainable, removing buff/heal economy
**Depends on**: Nothing (independent)
**Requirements**: BREW-01, BREW-02, BREW-03

**Success Criteria:**
1. Brewing stands do not spawn in villages, igloos, or any structure
2. Brewing stand recipe does not appear in crafting menu
3. Piglins do not offer potions when bartering gold

**Details:**
- Structure block filtering via existing setBlock redirect pattern
- Recipe removal via REMOVED_RECIPE_PATHS
- Piglin bartering loot table modification

---

### Phase 52: Armor Rebalancing

**Goal**: Armor progression provides clear upgrade tiers with copper insertion
**Depends on**: Nothing (independent)
**Requirements**: ARMR-01, ARMR-02, ARMR-03, ARMR-04

**Success Criteria:**
1. Full leather armor provides 7 total armor points
2. Full copper armor provides 10 total armor points
3. Full iron armor provides 15 total armor points
4. Full diamond armor provides 18 armor + 4 toughness
5. Full netherite armor provides 10 armor + 6 toughness (glass cannon tier)
6. Each armor tier upgrade gives strictly more protection than previous
7. Individual pieces can have half armor points (bar display rounds down)

**Details:**
- ArmorMaterial modification via DefaultItemComponentEvents.MODIFY
- Per-piece armor distribution calculated to ensure monotonic progression
- Half armor point rendering handled by vanilla (supports fractions internally)
- Netherite intentionally weaker armor but highest toughness

---

## Progress

| Phase | Name | Status | Plans |
|-------|------|--------|-------|
| 46 | Iron Boat | Planned | 0/3 |
| 47 | Saddle Removal | Pending | 0/? |
| 48 | Copper Bucket | Pending | 0/? |
| 49 | Fluid Placement Mechanics | Pending | 0/? |
| 50 | Elytra Flight Changes | Pending | 0/? |
| 51 | Brewing Removal | Pending | 0/? |
| 52 | Armor Rebalancing | Pending | 0/? |

## Coverage

| Requirement | Phase | Status |
|-------------|-------|--------|
| BOAT-01 | 46 | Pending |
| BOAT-02 | 46 | Pending |
| BOAT-03 | 46 | Pending |
| BOAT-04 | 46 | Pending |
| BOAT-05 | 46 | Pending |
| SADL-01 | 47 | Pending |
| SADL-02 | 47 | Pending |
| SADL-03 | 47 | Pending |
| BUCK-01 | 48 | Pending |
| BUCK-02 | 48 | Pending |
| BUCK-03 | 48 | Pending |
| BUCK-04 | 49 | Pending |
| WATR-01 | 49 | Pending |
| WATR-02 | 49 | Pending |
| ELYT-01 | 50 | Pending |
| ELYT-02 | 50 | Pending |
| ELYT-03 | 50 | Pending |
| BREW-01 | 51 | Pending |
| BREW-02 | 51 | Pending |
| BREW-03 | 51 | Pending |
| ARMR-01 | 52 | Pending |
| ARMR-02 | 52 | Pending |
| ARMR-03 | 52 | Pending |
| ARMR-04 | 52 | Pending |

**Coverage:** 24/24 requirements mapped (100%)

---

*Roadmap created: 2026-01-25*
