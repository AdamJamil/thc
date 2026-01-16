# Roadmap: THC (True Hardcore)

## Overview

Implementation of the base claiming system that enforces risk-based territorial progression. Players must explore to find bells that grant land plots, which can be used to claim flat chunks as safe building areas. Outside these bases, strict placement restrictions and mining fatigue penalties force players into risk/reward decisions. The system includes village protection, combat restrictions in bases, and comprehensive testing.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Land Plot System** - Bell mechanics and land plot item distribution
- [x] **Phase 2: Chunk Claiming Core** - Chunk validation, claiming, and base area tracking
- [x] **Phase 3: Base Area Permissions** - Unrestricted building and combat restrictions in bases
- [x] **Phase 4: World Restrictions** - Block placement/breaking restrictions and mining fatigue
- [ ] **Phase 4.1: Bug Fixes** - INSERTED: Fix bugs discovered during gameplay testing
- [ ] **Phase 5: Testing & Integration** - Comprehensive game tests and validation

## Phase Details

### Phase 1: Land Plot System
**Goal**: Players can obtain land plot books from bells to enable chunk claiming
**Depends on**: Nothing (first phase)
**Requirements**: PLOT-01, PLOT-02, PLOT-03, PLOT-04
**Success Criteria** (what must be TRUE):
  1. Bell rings drop land plot books on first activation
  2. Multiple land plot books can be collected from different bells across the world
  3. Bells no longer appear in villager trades
  4. Bell activation state persists across server restarts and world reloads
**Research**: Likely (bell interaction events, custom items, trade modification)
**Research topics**: Fabric event hooks for bell USE events in 1.21.11, custom item registration patterns, TradeOfferHelper API usage for removing bell trades, block entity data persistence
**Plans**: 2 plans in 2 waves
**Status**: Complete (2026-01-15)

Plans:
- 01-01: Land plot item foundation (Wave 1) - Complete
- 01-02: Bell interaction + state persistence (Wave 2) - Complete

### Phase 2: Chunk Claiming Core
**Goal**: Players can use land plot books to claim valid chunks as base areas
**Depends on**: Phase 1
**Requirements**: CLAIM-01, CLAIM-02, CLAIM-03, CLAIM-04, CLAIM-05, CLAIM-06, CLAIM-07, CLAIM-08
**Success Criteria** (what must be TRUE):
  1. Player can use land plot book on a block to claim the containing chunk
  2. Claiming fails appropriately with feedback (uneven terrain, village chunks, already claimed)
  3. Base area boundaries are correctly calculated (y >= lowest surface - 10) and tracked
  4. Village chunks are properly detected using structure/feature API
  5. Land plot book is consumed on successful claim
**Research**: Likely (chunk management APIs, structure detection, terrain analysis)
**Research topics**: Chunk coordinate conversion and access patterns, ServerLevel chunk queries, Structure/FeatureDetection API in 1.21.11, heightmap queries for terrain flatness validation, attachment or world data storage for claimed chunks
**Plans**: 3 plans in 2 waves
**Status**: Complete (2026-01-16)

Plans:
- 02-01: Claim data storage (Wave 1) - Complete
- 02-02: Chunk validator (Wave 1) - Complete
- 02-03: Land plot use action (Wave 2) - Complete

### Phase 3: Base Area Permissions
**Goal**: Base areas provide unrestricted building with combat restrictions
**Depends on**: Phase 2
**Requirements**: BASE-01, BASE-02, BASE-03, BASE-04, BASE-05, BASE-06
**Success Criteria** (what must be TRUE):
  1. Player can place any block type within base area without restrictions
  2. Player can break any block within base area without restrictions or mining fatigue
  3. Player cannot attack or draw weapons while inside their base area
  4. "No violence indoors!" message appears (red text) when attempting combat in base
**Research**: Likely (block event interception, combat hooks, area boundary checks)
**Research topics**: Block placement/destruction event APIs in Fabric, PlayerInteractEntityEvent for attack blocking, UseItemEvent for bow/crossbow drawing, player position vs chunk/area queries, action bar message display
**Plans**: TBD

Plans:
- (To be created during plan-phase)

### Phase 4: World Restrictions
**Goal**: Outside bases, strict placement rules and mining fatigue enforce risk
**Depends on**: Phase 3
**Requirements**: PLACE-01, PLACE-02, PLACE-03, PLACE-04, PLACE-05, BREAK-01, BREAK-02, BREAK-03, BREAK-04, BREAK-05, BREAK-06, BREAK-07
**Success Criteria** (what must be TRUE):
  1. Only allowlist blocks can be placed outside base chunks (silently fails otherwise)
  2. Allowlist blocks (except torches/ladders) respect 26-coordinate adjacency restriction
  3. Mining fatigue applies when breaking blocks outside bases with 1.4^x stacking
  4. Mining fatigue decays one level every 12 seconds with correct duration display
  5. Village chunks are protected (no block breaking except ores and allowlist blocks)
**Research**: Likely (effect management, spatial queries for adjacency)
**Research topics**: MobEffect application with custom amplifier/duration, effect stacking and decay timing, spatial queries for 26-coordinate adjacency checks, block type checking for ore detection, village chunk boundary queries
**Plans**: TBD

Plans:
- (To be created during plan-phase)

### Phase 4.1: Bug Fixes (INSERTED)
**Goal**: Fix bugs discovered during gameplay testing
**Depends on**: Phase 4
**Requirements**: None (fixes for existing requirements)
**Success Criteria** (what must be TRUE):
  1. Land plot item displays correct custom texture (land_plot.png) in inventory and when held
  2. Bells cannot be broken by players (infinite hardness protection)
  3. Block placement rejection does not cause inventory desync (count doesn't flicker)
  4. Blocks in village chunks cannot be broken (except ores and allowlist blocks)
**Research**: Unlikely (fixes to existing implementations)
**Plans**: 1 plan in 1 wave
**Status**: Not started

Plans:
- 04.1-01: Fix land plot icon, bell protection, inventory sync, village detection (Wave 1)

### Phase 5: Testing & Integration
**Goal**: All mechanics validated through comprehensive automated tests
**Depends on**: Phase 4.1
**Requirements**: TEST-01, TEST-02, TEST-03, TEST-04, TEST-05, TEST-06
**Success Criteria** (what must be TRUE):
  1. Game tests verify chunk claiming validation logic (all edge cases covered)
  2. Game tests verify block placement restrictions and adjacency rules
  3. Game tests verify block breaking restrictions and mining fatigue mechanics
  4. Game tests verify base area permission boundaries and combat restrictions
  5. Smoke tests complete successfully (100 tick validation)
**Research**: Unlikely (Fabric GameTest patterns established in existing buckler tests)
**Plans**: TBD

Plans:
- (To be created during plan-phase)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 4.1 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Land Plot System | 2/2 | Complete | 2026-01-15 |
| 2. Chunk Claiming Core | 3/3 | Complete | 2026-01-16 |
| 3. Base Area Permissions | 1/1 | Complete | 2026-01-16 |
| 4. World Restrictions | 4/4 | Complete | 2026-01-16 |
| 4.1 Bug Fixes | 0/1 | Not started | - |
| 5. Testing & Integration | 0/TBD | Not started | - |
