# Roadmap: THC v2.2 Extra Features Batch 6

## Overview

Food economy overhaul establishing cooking progression gates and meaningful food choices, plus foundational class/stage system for multiplayer role differentiation. Smokers gated behind iron, apples available from all leaves at 5x rate, complete food stat rebalancing, and new foods (Hearty Stew, Honey Apple). Class system introduces permanent role selection with health/damage modifiers, and stage system provides scaffolding for future boon progression.

## Phases

**Phase Numbering:**
- Continues from v2.1 (ended at Phase 29)
- Integer phases (30, 31, 32...): Planned milestone work

- [x] **Phase 30: Smoker Gating** - Iron requirement for smoker crafting, removed from villages
- [x] **Phase 31: Apple & Bonemeal** - 5x apple drops from all leaves, improved bonemeal
- [x] **Phase 32: Food Removals** - Remove suspicious stew, mushroom stew, beetroot soup, sugar recipe
- [x] **Phase 33: Food Stats** - Complete hunger/saturation rebalancing
- [ ] **Phase 34: New Foods** - Hearty Stew rename, Honey Apple item
- [ ] **Phase 35: Class System** - /selectClass command with permanent role modifiers
- [ ] **Phase 36: Stage System** - /advanceStage command with boon level scaffolding

## Phase Details

### Phase 30: Smoker Gating
**Goal**: Gate smoker behind iron acquisition
**Depends on**: Nothing (first phase)
**Success Criteria** (what must be TRUE):
  1. Smoker recipe requires 2 iron ingots in top corners
  2. Smokers do not spawn naturally in villages
  3. Existing smoker functionality unchanged
**Research**: Unlikely (follows furnace gating pattern from v2.1)
**Plans**: 1 (30-01)
**Completed**: 2026-01-22

### Phase 31: Apple & Bonemeal
**Goal**: Improve early-game food availability and farming
**Depends on**: Phase 30
**Success Criteria** (what must be TRUE):
  1. Apples drop from any leaf type (oak, birch, spruce, jungle, acacia, dark oak, cherry, azalea, mangrove)
  2. Apple drop rate is 5x vanilla
  3. Bonemeal fully matures any crop in one use
  4. Bone crafts into 6 bonemeal (not 3)
**Research**: Likely (leaf loot table structure, bonemeal mechanics)
**Research topics**: Leaf block loot tables, bonemeal growth mechanics intercept point
**Plans**: 1 (31-01)
**Completed**: 2026-01-22

### Phase 32: Food Removals
**Goal**: Remove low-value food items to simplify food choices
**Depends on**: Phase 31
**Success Criteria** (what must be TRUE):
  1. Suspicious stew recipe removed
  2. Mushroom stew recipe removed
  3. Beetroot soup recipe removed
  4. Sugarcane → sugar recipe removed
**Research**: None required
**Plans**: 1 (32-01)
**Completed**: 2026-01-22

### Phase 33: Food Stats
**Goal**: Complete hunger/saturation rebalancing per design table
**Depends on**: Phase 32
**Success Criteria** (what must be TRUE):
  1. All raw meats: low hunger (2-3), zero saturation
  2. All cooked meats: high hunger (5-8), moderate saturation (1.6-1.8)
  3. All crops/vegetables: low-moderate hunger (1-5), low saturation (0-0.7)
  4. Golden foods: moderate hunger (4-6), very high saturation (8-10)
  5. All food values match design specification
**Research**: Likely (food component modification in 1.21)
**Research topics**: FoodProperties modification, DataComponent system for food stats
**Plans**: 1 (33-01)
**Completed**: 2026-01-23

### Phase 34: New Foods
**Goal**: Add Hearty Stew and Honey Apple
**Depends on**: Phase 33
**Success Criteria** (what must be TRUE):
  1. Rabbit stew renamed to "Hearty Stew"
  2. Hearty Stew provides 10 hunger, 6.36 saturation
  3. Honey Apple craftable (apple + honey bottle shapeless)
  4. Honey Apple provides 8 hunger, 2.73 saturation
  5. Honey Apple has custom texture (honey_apple.png)
**Research**: Unlikely (follows existing item patterns)
**Plans**: TBD

### Phase 35: Class System
**Goal**: Implement permanent class selection with role modifiers
**Depends on**: Phase 34
**Success Criteria** (what must be TRUE):
  1. /selectClass <tank|melee|ranged|support> command exists
  2. Command only works when player is in a base chunk
  3. Tank: +1 heart max health, x2.5 melee damage, x1 ranged damage
  4. Melee: +0.5 hearts max health, x4 melee damage, x1 ranged damage
  5. Ranged: no health change, x1 melee damage, x5 ranged damage
  6. Support: no health change, x1 melee damage, x3 ranged damage
  7. Class selection is permanent (cannot change once selected)
  8. Class persists across sessions
**Research**: Likely (max health attribute modification, command registration)
**Research topics**: AttributeModifier for max health, Fabric command registration, persistent player attachments
**Plans**: TBD

### Phase 36: Stage System
**Goal**: Server-wide stage progression with per-player boon tracking
**Depends on**: Phase 35
**Success Criteria** (what must be TRUE):
  1. /advanceStage command advances server to next stage (1→2→3→4→5)
  2. Stage is server-wide (all players on same stage)
  3. Each player's boon level increments when stage advances
  4. Boon level tracked per-player with their class
  5. Class + boon level persist across server restarts
**Research**: Likely (server-wide persistent state)
**Research topics**: SavedData for server-wide state, attachment persistence patterns
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 30 → 31 → 32 → 33 → 34 → 35 → 36

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 30. Smoker Gating | 1/1 | Complete | 2026-01-22 |
| 31. Apple & Bonemeal | 1/1 | Complete | 2026-01-22 |
| 32. Food Removals | 1/1 | Complete | 2026-01-22 |
| 33. Food Stats | 1/1 | Complete | 2026-01-23 |
| 34. New Foods | 0/TBD | Not started | - |
| 35. Class System | 0/TBD | Not started | - |
| 36. Stage System | 0/TBD | Not started | - |
