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

- [ ] **SLOC-01**: Structure locator item class (compass-style behavior pointing to nearest structure)
- [ ] **SLOC-02**: Search radius capped at 100 chunks (performance protection)
- [ ] **SLOC-03**: Dimension-aware (Nether locators work in Nether, Overworld in Overworld)
- [ ] **SLOC-04**: Trial chamber locator item (Overworld)
- [ ] **SLOC-05**: Pillager outpost locator item (Overworld)
- [ ] **SLOC-06**: Nether fortress locator item (Nether)
- [ ] **SLOC-07**: Bastion locator item (Nether)
- [ ] **SLOC-08**: Ancient city locator item (Overworld)
- [ ] **SLOC-09**: Stronghold locator item (Overworld)
- [ ] **SLOC-10**: 6 locator textures (one per structure type)

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
| VJOB-01 to VJOB-04 | TBD | Pending |
| VLEV-01 to VLEV-05 | TBD | Pending |
| VCYC-01 to VCYC-03 | TBD | Pending |
| SLOC-01 to SLOC-10 | TBD | Pending |
| TLIB-01 to TLIB-09 | TBD | Pending |
| TBUT-01 to TBUT-08 | TBD | Pending |
| TMAS-01 to TMAS-10 | TBD | Pending |
| TCRT-01 to TCRT-10 | TBD | Pending |
| RAIL-01 to RAIL-03 | TBD | Pending |

**Coverage:**
- v2.8 requirements: 59 total
- Mapped to phases: 0
- Unmapped: 59

---
*Requirements defined: 2026-01-30*
*Last updated: 2026-01-30 after initial definition*
