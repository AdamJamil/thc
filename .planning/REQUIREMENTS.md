# Requirements: THC (True Hardcore)

**Defined:** 2026-01-15
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.

## v1 Requirements

### Land Plots

- [ ] **PLOT-01**: First bell ring drops a "land plot" book item
- [ ] **PLOT-02**: Player can collect multiple land plot books from different bells
- [ ] **PLOT-03**: Bells are removed from villager trading offers
- [ ] **PLOT-04**: Bell state (whether first ring occurred) persists per bell instance

### Chunk Claiming

- [ ] **CLAIM-01**: Player can use land plot book on a block to claim the chunk containing that block
- [ ] **CLAIM-02**: Chunk claiming validates terrain flatness (max 10 block Y difference across all surface blocks)
- [ ] **CLAIM-03**: Chunk claiming fails with message "The chunk's surface is not flat enough!" if terrain is too uneven
- [ ] **CLAIM-04**: Chunk claiming fails if target chunk is a village chunk
- [ ] **CLAIM-05**: Chunk claiming fails if chunk is already claimed as a base chunk
- [ ] **CLAIM-06**: Land plot book is consumed upon successful chunk claim
- [ ] **CLAIM-07**: Base area is defined as all coordinates (x, y, z) where y >= (lowest surface block Y in chunk) - 10
- [ ] **CLAIM-08**: Village chunks are detected using Minecraft's structure/feature detection API

### Base Area Mechanics

- [ ] **BASE-01**: Player can place any block type within base area without restrictions
- [ ] **BASE-02**: Player can break any block within base area without restrictions
- [ ] **BASE-03**: Player does not receive mining fatigue when breaking blocks in base area
- [ ] **BASE-04**: Player cannot attack mobs or other players while inside base area
- [ ] **BASE-05**: Player cannot draw bow or crossbow while inside base area
- [ ] **BASE-06**: Player sees "No violence indoors!" message (red text) when attempting combat in base area

### Block Placement Restrictions

- [ ] **PLACE-01**: Outside base chunks, only allowlist blocks can be placed (anvil, blast furnace, brewing stand, cartography table, cauldron, chest, composter, crafting table, enchantment table, furnace, grindstone, lectern, lodestone, loom, smithing table, smoker, stonecutter, TNT, ladder, torch)
- [ ] **PLACE-02**: Placement of non-allowlist blocks outside base chunks fails silently (block is not placed)
- [ ] **PLACE-03**: Allowlist blocks (except torches and ladders) cannot be placed within 26 coordinates of another allowlist block
- [ ] **PLACE-04**: Adjacent block placement restriction applies everywhere outside base chunks (including village chunks)
- [ ] **PLACE-05**: Torches and ladders are exempt from the adjacent block placement restriction

### Block Breaking Restrictions

- [ ] **BREAK-01**: Player receives mining fatigue when breaking blocks outside base chunks (non-village)
- [ ] **BREAK-02**: Mining fatigue stacks accumulate - with x stacks, blocks take 1.4^x times longer to break
- [ ] **BREAK-03**: Mining fatigue stacks decay one level every 12 seconds (Fatigue III → Fatigue II after 12s, Fatigue II → Fatigue I after 12s, etc.)
- [ ] **BREAK-04**: Each mining fatigue level displays with 12 seconds remaining duration
- [ ] **BREAK-05**: Blocks in village chunks cannot be broken at all
- [ ] **BREAK-06**: Ores in village chunks can be broken (exception to village protection)
- [ ] **BREAK-07**: Allowlist blocks in village chunks can be broken (exception to village protection)

### Testing & Validation

- [ ] **TEST-01**: Game tests verify chunk claiming validation logic (flatness, village detection, already claimed)
- [ ] **TEST-02**: Game tests verify block placement restrictions and adjacency rules
- [ ] **TEST-03**: Game tests verify block breaking restrictions and mining fatigue application
- [ ] **TEST-04**: Game tests verify base area permission boundaries
- [ ] **TEST-05**: Game tests verify combat restriction in base areas
- [ ] **TEST-06**: Smoke tests pass after implementation

## v2 Requirements

(None defined - future systems to be designed)

## Out of Scope

| Feature | Reason |
|---------|--------|
| TNT placement with boon system | Future system not yet designed - TNT on allowlist but boon mechanic deferred |
| Multiplayer territory conflicts | Focus is single-player or cooperative - no PvP land claiming for v1 |
| Base chunk ownership transfer | Not needed for initial single-player focused implementation |
| Chunk claiming UI/map | Command-line/in-game book interaction sufficient for v1 |
| Mob spawning restrictions in bases | Not explicitly part of current design - may revisit based on testing |
| Bell crafting recipe changes | Only removing from trades, not modifying crafting for v1 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PLOT-01 | Phase 1 | Pending |
| PLOT-02 | Phase 1 | Pending |
| PLOT-03 | Phase 1 | Pending |
| PLOT-04 | Phase 1 | Pending |
| CLAIM-01 | Phase 2 | Pending |
| CLAIM-02 | Phase 2 | Pending |
| CLAIM-03 | Phase 2 | Pending |
| CLAIM-04 | Phase 2 | Pending |
| CLAIM-05 | Phase 2 | Pending |
| CLAIM-06 | Phase 2 | Pending |
| CLAIM-07 | Phase 2 | Pending |
| CLAIM-08 | Phase 2 | Pending |
| BASE-01 | Phase 3 | Pending |
| BASE-02 | Phase 3 | Pending |
| BASE-03 | Phase 3 | Pending |
| BASE-04 | Phase 3 | Pending |
| BASE-05 | Phase 3 | Pending |
| BASE-06 | Phase 3 | Pending |
| PLACE-01 | Phase 4 | Pending |
| PLACE-02 | Phase 4 | Pending |
| PLACE-03 | Phase 4 | Pending |
| PLACE-04 | Phase 4 | Pending |
| PLACE-05 | Phase 4 | Pending |
| BREAK-01 | Phase 4 | Pending |
| BREAK-02 | Phase 4 | Pending |
| BREAK-03 | Phase 4 | Pending |
| BREAK-04 | Phase 4 | Pending |
| BREAK-05 | Phase 4 | Pending |
| BREAK-06 | Phase 4 | Pending |
| BREAK-07 | Phase 4 | Pending |
| TEST-01 | Phase 5 | Pending |
| TEST-02 | Phase 5 | Pending |
| TEST-03 | Phase 5 | Pending |
| TEST-04 | Phase 5 | Pending |
| TEST-05 | Phase 5 | Pending |
| TEST-06 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 30 total
- Mapped to phases: 30
- Unmapped: 0 ✓

---
*Requirements defined: 2026-01-15*
*Last updated: 2026-01-15 after initial definition*
