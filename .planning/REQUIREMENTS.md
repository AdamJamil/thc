# Requirements: THC v2.0 Twilight Hardcore

**Defined:** 2026-01-20
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.

## v2.0 Requirements

Requirements for the Twilight Hardcore release. Each maps to roadmap phases.

### Time & Sky

- [ ] **TIME-01**: Server time flows normally (doDaylightCycle continues)
- [ ] **TIME-02**: Existing night-lock code is removed
- [ ] **SKY-01**: Client sees perpetual dusk sky (locked at ~13000 ticks)
- [ ] **SKY-02**: Sky rendering works correctly in Overworld only (Nether/End unaffected)

### Mob Behavior

- [ ] **MOB-01**: Zombies do not burn in sunlight
- [ ] **MOB-02**: Skeletons do not burn in sunlight
- [ ] **MOB-03**: Phantoms do not burn in sunlight
- [ ] **MOB-04**: Undead can still catch fire from fire aspect, lava, and other non-sun sources
- [ ] **SPAWN-01**: Hostile mobs can spawn regardless of sky light level
- [ ] **SPAWN-02**: Block light still affects spawn density (vanilla behavior preserved)

### Passive Mobs

- [ ] **BEE-01**: Bees work continuously regardless of time of day
- [ ] **BEE-02**: Bees work continuously regardless of weather (rain)
- [ ] **BEE-03**: Bees still return to hive when nectar-full (that behavior preserved)

### Villagers

- [ ] **VILLAGER-01**: Villagers always attempt to stay inside/go to bed (night behavior active continuously)
- [ ] **VILLAGER-02**: Villagers behave as if it's always night for schedule purposes

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
| TIME-01 | TBD | Pending |
| TIME-02 | TBD | Pending |
| SKY-01 | TBD | Pending |
| SKY-02 | TBD | Pending |
| MOB-01 | TBD | Pending |
| MOB-02 | TBD | Pending |
| MOB-03 | TBD | Pending |
| MOB-04 | TBD | Pending |
| SPAWN-01 | TBD | Pending |
| SPAWN-02 | TBD | Pending |
| BEE-01 | TBD | Pending |
| BEE-02 | TBD | Pending |
| BEE-03 | TBD | Pending |
| VILLAGER-01 | TBD | Pending |
| VILLAGER-02 | TBD | Pending |
| BED-01 | TBD | Pending |
| BED-02 | TBD | Pending |
| BED-03 | TBD | Pending |

**Coverage:**
- v2.0 requirements: 18 total
- Mapped to phases: 0 (pending roadmap creation)
- Unmapped: 18 ⚠️

---
*Requirements defined: 2026-01-20*
*Last updated: 2026-01-20 after initial definition*
