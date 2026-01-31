# Requirements: THC v2.8 Villager Overhaul

**Defined:** 2026-01-30
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v2.8 Requirements

Requirements for villager system overhaul. Each maps to roadmap phases.

### Villager Job Restriction

- [ ] **VJOB-01**: Only 4 professions allowed (mason, librarian, butcher, cartographer)
- [ ] **VJOB-02**: Disallowed professions reset to NONE (jobless villager)
- [ ] **VJOB-03**: Job blocks for disallowed professions don't grant jobs (POI blocking)
- [ ] **VJOB-04**: Naturally spawned villagers with disallowed jobs become jobless

### Manual Leveling

- [ ] **VLEV-01**: Villagers cannot level up automatically (vanilla XP → level progression disabled)
- [ ] **VLEV-02**: Stage gates: stage 2=apprentice, 3=journeyman, 4=expert, 5=master
- [ ] **VLEV-03**: Right-click with emerald at max XP + stage requirement to level up
- [ ] **VLEV-04**: Emerald consumed on level up
- [ ] **VLEV-05**: Reduced XP: 2/3/4/5 trades per level to reach max XP (5 XP per trade)

### Trade Cycling

- [ ] **VCYC-01**: Right-click with emerald at 0 XP to reroll current rank trades
- [ ] **VCYC-02**: Only current rank trades affected (earlier ranks preserved)
- [ ] **VCYC-03**: Emerald consumed on cycle

### Structure Locators

- [x] **SLOC-01**: Structure locator item class (compass-style behavior pointing to nearest structure)
- [x] **SLOC-02**: Search radius capped at 100 chunks (performance protection)
- [x] **SLOC-03**: Dimension-aware (Nether locators work in Nether, Overworld in Overworld)
- [x] **SLOC-04**: Trial chamber locator item (Overworld)
- [x] **SLOC-05**: Pillager outpost locator item (Overworld)
- [x] **SLOC-06**: Nether fortress locator item (Nether)
- [x] **SLOC-07**: Bastion locator item (Nether)
- [x] **SLOC-08**: Ancient city locator item (Overworld)
- [x] **SLOC-09**: Stronghold locator item (Overworld)
- [x] **SLOC-10**: 6 locator textures (one per structure type)

### Custom Trade Tables — Librarian

- [ ] **TLIB-01**: Novice slot 1: 24 paper → 1e OR 1e → 8 lanterns (50/50)
- [ ] **TLIB-02**: Novice slot 2: 5e + book → mending OR 5e + book → unbreaking (50/50)
- [ ] **TLIB-03**: Apprentice slot 3: 10e + book → efficiency OR 10e + book → fortune (50/50)
- [ ] **TLIB-04**: Apprentice slot 4: 10e + book → silk touch OR 4 books → 1e (50/50)
- [ ] **TLIB-05**: Journeyman slot 5: 15e + book → protection OR 15e + book → proj prot (50/50)
- [ ] **TLIB-06**: Journeyman slot 6: 15e + book → looting OR 9e → 3 bookshelves (50/50)
- [ ] **TLIB-07**: Expert slot 7: 20e + book → sharpness OR 20e + book → power (50/50)
- [ ] **TLIB-08**: Expert slot 8: 20e + book → blast prot OR 20e + book → feather falling (50/50)
- [ ] **TLIB-09**: Master slot 9: 30e + book → breach OR 30e + book → piercing (50/50)

### Custom Trade Tables — Butcher

- [ ] **TBUT-01**: Novice slot 1: 4 raw chicken → 1e (deterministic)
- [ ] **TBUT-02**: Novice slot 2: 5 raw porkchop → 1e (deterministic)
- [ ] **TBUT-03**: Apprentice slot 3: 5 raw beef → 1e (deterministic)
- [ ] **TBUT-04**: Apprentice slot 4: 3 raw mutton → 1e (deterministic)
- [ ] **TBUT-05**: Journeyman slot 5: 1e → 6 cooked porkchop (deterministic)
- [ ] **TBUT-06**: Journeyman slot 6: 1e → 5 steak (deterministic)
- [ ] **TBUT-07**: Expert slot 7: 10 dried kelp blocks → 1e (deterministic)
- [ ] **TBUT-08**: Master slot 8: 10 sweet berries → 1e (deterministic)

### Custom Trade Tables — Mason

- [ ] **TMAS-01**: Novice slot 1: 1e → 64 cobblestone (deterministic)
- [ ] **TMAS-02**: Novice slot 2: 1e → 64 stone bricks (deterministic)
- [ ] **TMAS-03**: Novice slot 3: 1e → 64 bricks (deterministic)
- [ ] **TMAS-04**: Novice slot 4: 1e → 64 polished andesite (deterministic)
- [ ] **TMAS-05**: Apprentice slot 5: 1e → 64 polished granite OR 1e → 64 polished diorite (50/50)
- [ ] **TMAS-06**: Apprentice slot 6: 1e → 64 smooth stone OR 1e → 64 calcite (50/50)
- [ ] **TMAS-07**: Journeyman slot 7: 1e → 64 tuff OR 1e → 64 mud bricks (50/50)
- [ ] **TMAS-08**: Journeyman slot 8: 1e → 32 deepslate bricks OR 1e → 32 deepslate tiles (50/50)
- [ ] **TMAS-09**: Expert slot 9: 1e → 32 polished blackstone OR 1e → 32 polished blackstone bricks (50/50)
- [ ] **TMAS-10**: Master slot 10: 1e → 16 copper block OR 1e → 16 quartz block (50/50)

### Custom Trade Tables — Cartographer

- [ ] **TCRT-01**: Novice slot 1: 24 paper → 1e (deterministic)
- [ ] **TCRT-02**: Novice slot 2: 5e → empty map (deterministic)
- [ ] **TCRT-03**: Novice slot 3: 10e → trial chamber locator (deterministic)
- [ ] **TCRT-04**: Apprentice slot 4: 15e → pillager outpost locator (deterministic)
- [ ] **TCRT-05**: Apprentice slot 5: 1e → 8 glass panes (deterministic)
- [ ] **TCRT-06**: Apprentice slot 6: 3e → spyglass (deterministic)
- [ ] **TCRT-07**: Journeyman slot 7: 20e → nether fortress locator (deterministic)
- [ ] **TCRT-08**: Journeyman slot 8: 20e → bastion locator (deterministic)
- [ ] **TCRT-09**: Expert slot 9: 25e → ancient city locator (deterministic)
- [ ] **TCRT-10**: Master slot 10: 30e → stronghold locator (deterministic)

### Rail Transportation

- [ ] **RAIL-01**: Rails craftable with copper OR iron (alternative recipe)
- [ ] **RAIL-02**: Rails yield 64 per recipe (up from 16, 4x increase)
- [ ] **RAIL-03**: Powered rails yield 64 per recipe (up from 6, ~10x increase)

## Future Requirements

Deferred to later milestones.

(None identified for v2.8)

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Wandering trader changes | Focus on stationary villagers first |
| Villager breeding changes | Not part of trade/profession overhaul |
| Nitwit/child mechanics | Natural progression, not a focus |
| Villager-zombie cure changes | Could bypass profession restrictions, defer |
| Gossip/reputation system changes | Keep vanilla behavior for price modifiers |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| SLOC-01 | 66 | Complete |
| SLOC-02 | 66 | Complete |
| SLOC-03 | 66 | Complete |
| SLOC-04 | 66 | Complete |
| SLOC-05 | 66 | Complete |
| SLOC-06 | 66 | Complete |
| SLOC-07 | 66 | Complete |
| SLOC-08 | 66 | Complete |
| SLOC-09 | 66 | Complete |
| SLOC-10 | 66 | Complete |
| VJOB-01 | 67 | Pending |
| VJOB-02 | 67 | Pending |
| VJOB-03 | 67 | Pending |
| VJOB-04 | 67 | Pending |
| TLIB-01 | 68 | Pending |
| TLIB-02 | 68 | Pending |
| TLIB-03 | 68 | Pending |
| TLIB-04 | 68 | Pending |
| TLIB-05 | 68 | Pending |
| TLIB-06 | 68 | Pending |
| TLIB-07 | 68 | Pending |
| TLIB-08 | 68 | Pending |
| TLIB-09 | 68 | Pending |
| TBUT-01 | 68 | Pending |
| TBUT-02 | 68 | Pending |
| TBUT-03 | 68 | Pending |
| TBUT-04 | 68 | Pending |
| TBUT-05 | 68 | Pending |
| TBUT-06 | 68 | Pending |
| TBUT-07 | 68 | Pending |
| TBUT-08 | 68 | Pending |
| TMAS-01 | 68 | Pending |
| TMAS-02 | 68 | Pending |
| TMAS-03 | 68 | Pending |
| TMAS-04 | 68 | Pending |
| TMAS-05 | 68 | Pending |
| TMAS-06 | 68 | Pending |
| TMAS-07 | 68 | Pending |
| TMAS-08 | 68 | Pending |
| TMAS-09 | 68 | Pending |
| TMAS-10 | 68 | Pending |
| TCRT-01 | 68 | Pending |
| TCRT-02 | 68 | Pending |
| TCRT-03 | 68 | Pending |
| TCRT-04 | 68 | Pending |
| TCRT-05 | 68 | Pending |
| TCRT-06 | 68 | Pending |
| TCRT-07 | 68 | Pending |
| TCRT-08 | 68 | Pending |
| TCRT-09 | 68 | Pending |
| TCRT-10 | 68 | Pending |
| VLEV-01 | 69 | Pending |
| VLEV-02 | 69 | Pending |
| VLEV-03 | 69 | Pending |
| VLEV-04 | 69 | Pending |
| VLEV-05 | 69 | Pending |
| VCYC-01 | 70 | Pending |
| VCYC-02 | 70 | Pending |
| VCYC-03 | 70 | Pending |
| RAIL-01 | 71 | Pending |
| RAIL-02 | 71 | Pending |
| RAIL-03 | 71 | Pending |

**Coverage:**
- v2.8 requirements: 59 total
- Mapped to phases: 59
- Unmapped: 0

---
*Requirements defined: 2026-01-30*
*Last updated: 2026-01-30 after roadmap creation*
