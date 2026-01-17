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

### Crafting Tweaks

- [ ] **CRAFT-01**: Ladder recipe yields 16 ladders from 7 sticks (instead of vanilla 3)
- [ ] **CRAFT-02**: Snowballs stack to 64 (instead of vanilla 16)
- [ ] **CRAFT-03**: Snow block can be converted into 9 snowballs via crafting
- [ ] **CRAFT-04**: 9 snowballs can be crafted into 1 snow block

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
| PLOT-01 | Phase 1 | Complete |
| PLOT-02 | Phase 1 | Complete |
| PLOT-03 | Phase 1 | Complete |
| PLOT-04 | Phase 1 | Complete |
| CLAIM-01 | Phase 2 | Complete |
| CLAIM-02 | Phase 2 | Complete |
| CLAIM-03 | Phase 2 | Complete |
| CLAIM-04 | Phase 2 | Complete |
| CLAIM-05 | Phase 2 | Complete |
| CLAIM-06 | Phase 2 | Complete |
| CLAIM-07 | Phase 2 | Complete |
| CLAIM-08 | Phase 2 | Complete |
| BASE-01 | Phase 3 | Complete |
| BASE-02 | Phase 3 | Complete |
| BASE-03 | Phase 3 | Complete |
| BASE-04 | Phase 3 | Complete |
| BASE-05 | Phase 3 | Complete |
| BASE-06 | Phase 3 | Complete |
| PLACE-01 | Phase 4 | Complete |
| PLACE-02 | Phase 4 | Complete |
| PLACE-03 | Phase 4 | Complete |
| PLACE-04 | Phase 4 | Complete |
| PLACE-05 | Phase 4 | Complete |
| BREAK-01 | Phase 4 | Complete |
| BREAK-02 | Phase 4 | Complete |
| BREAK-03 | Phase 4 | Complete |
| BREAK-04 | Phase 4 | Complete |
| BREAK-05 | Phase 4 | Complete |
| BREAK-06 | Phase 4 | Complete |
| BREAK-07 | Phase 4 | Complete |
| CRAFT-01 | Phase 5 | Pending |
| CRAFT-02 | Phase 5 | Pending |
| CRAFT-03 | Phase 5 | Pending |
| CRAFT-04 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 28 total
- Mapped to phases: 28
- Unmapped: 0 ✓

---
*Requirements defined: 2026-01-15*
*Last updated: 2026-01-17 — Added Phase 5 crafting tweaks*
