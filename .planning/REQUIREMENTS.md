# Requirements: THC v2.0 Twilight Hardcore

**Defined:** 2026-01-20
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.

## v2.0 Requirements

Requirements for the Twilight Hardcore release. Each maps to roadmap phases.

### Time & Sky

- [x] **TIME-01**: Server time flows normally (doDaylightCycle continues)
- [x] **TIME-02**: Existing night-lock code is removed
- [x] **SKY-01**: Client sees perpetual dusk sky (locked at ~13000 ticks)
- [x] **SKY-02**: Sky rendering works correctly in Overworld only (Nether/End unaffected)

### Mob Behavior

- [x] **MOB-01**: Zombies do not burn in sunlight
- [x] **MOB-02**: Skeletons do not burn in sunlight
- [x] **MOB-03**: Phantoms do not burn in sunlight
- [x] **MOB-04**: Undead can still catch fire from fire aspect, lava, and other non-sun sources
- [x] **SPAWN-01**: Hostile mobs can spawn regardless of sky light level
- [x] **SPAWN-02**: Block light still affects spawn density (vanilla behavior preserved)

### Passive Mobs

- [x] **BEE-01**: Bees work continuously regardless of time of day
- [x] **BEE-02**: Bees work continuously regardless of weather (rain)
- [x] **BEE-03**: Bees still return to hive when nectar-full (that behavior preserved)

### Villagers

- [x] **VILLAGER-01**: Villagers always attempt to stay inside/go to bed (night behavior active continuously)
- [x] **VILLAGER-02**: Villagers behave as if it's always night for schedule purposes

### Beds

- [ ] **BED-01**: Player can always use beds (no time-of-day restriction)
- [ ] **BED-02**: Sleeping does not skip time or advance day/night cycle
- [ ] **BED-03**: Beds still set spawn point when used

## v2 Requirements

No features deferred — all researched features included in v2.0.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Weather visual override | Rain during dusk is atmospheric, acceptable |
| Nether/End twilight | These dimensions have their own aesthetics |
| Shader-specific fixes | Basic compatibility expected, deep Iris integration deferred |
| Phantom spawning changes | Phantoms should still spawn based on insomnia mechanic |

## Traceability

Which phases cover which requirements. Updated by create-roadmap.

| Requirement | Phase | Status |
|-------------|-------|--------|
| TIME-01 | Phase 17 | Complete |
| TIME-02 | Phase 17 | Complete |
| SKY-01 | Phase 18 | Complete |
| SKY-02 | Phase 18 | Complete |
| MOB-01 | Phase 19 | Complete |
| MOB-02 | Phase 19 | Complete |
| MOB-03 | Phase 19 | Complete |
| MOB-04 | Phase 19 | Complete |
| SPAWN-01 | Phase 20 | Complete |
| SPAWN-02 | Phase 20 | Complete |
| BEE-01 | Phase 21 | Complete |
| BEE-02 | Phase 21 | Complete |
| BEE-03 | Phase 21 | Complete |
| VILLAGER-01 | Phase 22 | Complete |
| VILLAGER-02 | Phase 22 | Complete |
| BED-01 | Phase 23 | Pending |
| BED-02 | Phase 23 | Pending |
| BED-03 | Phase 23 | Pending |

**Coverage:**
- v2.0 requirements: 18 total
- Mapped to phases: 18 ✓
- Unmapped: 0

---
*Requirements defined: 2026-01-20*
*Last updated: 2026-01-20 after phase 22 completion*
