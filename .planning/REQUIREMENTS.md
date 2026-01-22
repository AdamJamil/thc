# Requirements: THC v2.1 Extra Features Batch 5

**Defined:** 2026-01-22
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v2.1 Requirements

Requirements for v2.1 release. Each maps to roadmap phases.

### Survival Progression

- [x] **PROG-01**: Blast Totem item exists with custom texture (blast_totem.png)
- [x] **PROG-02**: Blast Totem replaces Totem of Undying in all loot tables and chests
- [x] **PROG-03**: Furnace recipe requires blaze powder (center) + 8 cobblestone
- [x] **PROG-04**: Natural furnace spawns removed from world generation
- [x] **PROG-05**: Blast furnace recipe is furnace + blast totem (shapeless)
- [x] **PROG-06**: Natural blast furnace spawns removed from world generation

### World Protection

- [x] **PROT-01**: Village block breaking restricted to structure bounding boxes only
- [x] **PROT-02**: Underground traversal below villages is unrestricted
- [x] **PROT-03**: Existing village chunk protection replaced with structure-based protection

### Healing System

- [ ] **HEAL-01**: Eating sets saturation to max(food_saturation, current_saturation)
- [ ] **HEAL-02**: Eating duration is 64 ticks (instead of vanilla 32)
- [ ] **HEAL-03**: Exhaustion rate: 4.0 exhaustion removes 1.21 saturation
- [ ] **HEAL-04**: Healing requires hunger ≥ 18 (9 bars)
- [ ] **HEAL-05**: Base healing rate: 3/16 hearts per second when hunger ≥ 18
- [ ] **HEAL-06**: Saturation T5 (6.36+) adds +1 heart/s healing
- [ ] **HEAL-07**: Saturation T4 (2.73+) adds +0.5 heart/s healing
- [ ] **HEAL-08**: Saturation T3 (1.36+) adds +3/16 heart/s healing
- [ ] **HEAL-09**: Saturation T2 (0.45+) adds +1/8 heart/s healing
- [ ] **HEAL-10**: Saturation T1 (0-0.45) adds +1/16 heart/s healing
- [ ] **HEAL-11**: Vanilla natural regeneration disabled

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

*No features deferred from v2.1*

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Tipped tiered arrows | Complexity deferred (from PROJECT.md) |
| Totem of Undying functionality on Blast Totem | Blast Totem is item replacement only, not functional duplicate |

## Traceability

Which phases cover which requirements. Updated by create-roadmap.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PROG-01 | Phase 24 | Complete |
| PROG-02 | Phase 24 | Complete |
| PROG-03 | Phase 25 | Complete |
| PROG-04 | Phase 25 | Complete |
| PROG-05 | Phase 25 | Complete |
| PROG-06 | Phase 25 | Complete |
| PROT-01 | Phase 26 | Complete |
| PROT-02 | Phase 26 | Complete |
| PROT-03 | Phase 26 | Complete |
| HEAL-01 | Phase 27 | Pending |
| HEAL-02 | Phase 27 | Pending |
| HEAL-03 | Phase 28 | Pending |
| HEAL-04 | Phase 28 | Pending |
| HEAL-05 | Phase 28 | Pending |
| HEAL-06 | Phase 29 | Pending |
| HEAL-07 | Phase 29 | Pending |
| HEAL-08 | Phase 29 | Pending |
| HEAL-09 | Phase 29 | Pending |
| HEAL-10 | Phase 29 | Pending |
| HEAL-11 | Phase 28 | Pending |

**Coverage:**
- v2.1 requirements: 20 total
- Mapped to phases: 20 ✓
- Unmapped: 0 ✓

---
*Requirements defined: 2026-01-22*
*Last updated: 2026-01-22 after roadmap creation*
