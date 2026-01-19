# Requirements: THC v1.3 Extra Features Batch 3

**Defined:** 2026-01-19
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v1.3 Requirements

Requirements for v1.3 release. Each maps to roadmap phases.

### Combat Rebalancing

- [ ] **COMBAT-01**: Arrow aggro causes Speed 4 (up from Speed 2) for 6 seconds
- [ ] **COMBAT-02**: Remove knockback from arrow hits on enemy mobs
- [ ] **COMBAT-03**: Sweeping edge no longer applies to weapon hits
- [ ] **COMBAT-04**: All melee damage reduced by 75%

### Wind Charge / Mobility

- [ ] **WIND-01**: Breeze rods yield 12 wind charges (up from 4)
- [ ] **WIND-02**: Wind charges knock player 50% higher
- [ ] **WIND-03**: Wind charge self-use negates fall damage on next landing (wind charge boost only)

### Ranged Weapon Gating

- [ ] **RANGED-01**: Bows require 3 breeze rods + 3 string (replaces all sticks in recipe)
- [ ] **RANGED-02**: Crossbows require breeze rod + diamond instead of sticks + iron
- [ ] **RANGED-03**: Bows and crossbows don't spawn in overworld chests
- [ ] **RANGED-04**: Bows and crossbows don't drop from mobs

### Threat System

- [ ] **THREAT-01**: Per-mob threat map (player → threat value as double)
- [ ] **THREAT-02**: Dealing X damage adds X threat to all hostile/neutral mobs within 15 blocks
- [ ] **THREAT-03**: Threat decays by 1 per second per mob per player
- [ ] **THREAT-04**: Arrow hits add +10 bonus threat to struck mob
- [ ] **THREAT-05**: Mobs target highest-threat player when threat ≥ 5 (unless revenge priority)
- [ ] **THREAT-06**: Target switch only on: (1) revenge strike, or (2) another player gains strictly higher threat

### World Changes

- [ ] **WORLD-01**: Mob griefing disabled
- [ ] **WORLD-02**: Smooth stone drops cobblestone without silk touch
- [ ] **WORLD-03**: Always max regional difficulty & clamped regional difficulty in every chunk
- [ ] **WORLD-04**: Moon phase always "true" for mob/difficulty checks
- [ ] **WORLD-05**: Mobs cannot spawn in base chunks

## v2 Requirements

None deferred.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Tipped tiered arrows | Complexity - defer if needed later |
| Crossbow-specific arrow behavior | Uses same arrows, no special handling needed |
| Threat persistence across chunk unload | Threat is ephemeral, mobs forget when unloaded |
| Threat on passive mobs | Only hostile/neutral mobs track threat |

## Traceability

Which phases cover which requirements. Updated by create-roadmap.

| Requirement | Phase | Status |
|-------------|-------|--------|
| COMBAT-01 | Phase 12 | Pending |
| COMBAT-02 | Phase 12 | Pending |
| COMBAT-03 | Phase 12 | Pending |
| COMBAT-04 | Phase 12 | Pending |
| WIND-01 | Phase 13 | Pending |
| WIND-02 | Phase 13 | Pending |
| WIND-03 | Phase 13 | Pending |
| RANGED-01 | Phase 14 | Pending |
| RANGED-02 | Phase 14 | Pending |
| RANGED-03 | Phase 14 | Pending |
| RANGED-04 | Phase 14 | Pending |
| THREAT-01 | Phase 15 | Pending |
| THREAT-02 | Phase 15 | Pending |
| THREAT-03 | Phase 15 | Pending |
| THREAT-04 | Phase 15 | Pending |
| THREAT-05 | Phase 15 | Pending |
| THREAT-06 | Phase 15 | Pending |
| WORLD-01 | Phase 16 | Pending |
| WORLD-02 | Phase 16 | Pending |
| WORLD-03 | Phase 16 | Pending |
| WORLD-04 | Phase 16 | Pending |
| WORLD-05 | Phase 16 | Pending |

**Coverage:**
- v1.3 requirements: 22 total
- Mapped to phases: 22
- Unmapped: 0 ✓

---
*Requirements defined: 2026-01-19*
*Last updated: 2026-01-19 after roadmap phase mapping*
