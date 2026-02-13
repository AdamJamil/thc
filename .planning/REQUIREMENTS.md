# Requirements: THC v3.4 Bow Overhaul

**Defined:** 2026-02-12
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v3.4 Requirements

Requirements for bow overhaul milestone. Each maps to roadmap phases.

### Bow Items & Recipes

- [ ] **ITEM-01**: Vanilla bow renamed to "Wooden Bow"
- [ ] **ITEM-02**: Wooden bow recipe changed to 3 sticks + 3 string (vanilla recipe)
- [ ] **ITEM-03**: Blaze bow item registered with custom textures (blaze_bow.png, pulling states)
- [ ] **ITEM-04**: Blaze bow craftable with 3 blaze rods + 3 string
- [ ] **ITEM-05**: Breeze bow item registered with custom textures (breeze_bow.png, pulling states)
- [ ] **ITEM-06**: Breeze bow craftable with 3 breeze rods + 3 string

### Arrow Physics

- [ ] **PHYS-01**: Player arrows use horizontal drag per tick: drag_coefficient = max(0.8, 1 - bow_drag_factor * ticks_in_flight); horizontal velocity *= drag_coefficient, vertical velocity untouched
- [ ] **PHYS-02**: Bow drag factors: wooden 0.015, blaze 0.015, breeze 0.01
- [ ] **PHYS-03**: Previous gravity-over-time arrow physics removed

### Damage & Effects

- [ ] **DMG-01**: Wooden bow arrows tagged "wooden_bow", deal 50% final damage
- [ ] **DMG-02**: Breeze bow arrows tagged "breeze_bow", deal 75% final damage
- [ ] **DMG-03**: Blaze bow arrows set target on fire for 3 seconds (0.5 damage/second)
- [ ] **DMG-04**: Breeze bow arrows apply regular arrow knockback to targets
- [ ] **DMG-05**: Glowing no longer applied to mobs hit by player projectiles

### Bow Mechanics

- [ ] **MECH-01**: Blaze bow draw time is 1.5x wooden bow draw time
- [ ] **MECH-02**: Breeze bow draw time is 0.75x wooden bow draw time
- [ ] **MECH-03**: Wooden bow cannot fire tipped arrows
- [ ] **MECH-04**: Tipped arrows from breeze bow create splash AoE on impact with per-arrow direct/splash effects

### Class Gating

- [ ] **GATE-01**: Blaze bow requires Ranged class and Stage 2+ ("The bow burns your fragile hands.")
- [ ] **GATE-02**: Breeze bow requires Support class and Stage 2+ ("The bow gusts are beyond your control.")

## Future Requirements

None — milestone is self-contained.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Crossbow overhaul | Separate milestone if needed |
| Tipped arrow crafting changes | Existing anvil system sufficient |
| Bow enchantment changes | Enchantment system already overhauled in v2.5 |
| Blaze/breeze bow loot drops | Follow existing pattern — bows removed from all loot tables |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ITEM-01 | Phase 86 | Pending |
| ITEM-02 | Phase 86 | Pending |
| ITEM-03 | Phase 87 | Pending |
| ITEM-04 | Phase 87 | Pending |
| ITEM-05 | Phase 88 | Pending |
| ITEM-06 | Phase 88 | Pending |
| PHYS-01 | Phase 86 | Pending |
| PHYS-02 | Phase 86 | Pending |
| PHYS-03 | Phase 86 | Pending |
| DMG-01 | Phase 86 | Pending |
| DMG-02 | Phase 88 | Pending |
| DMG-03 | Phase 87 | Pending |
| DMG-04 | Phase 88 | Pending |
| DMG-05 | Phase 86 | Pending |
| MECH-01 | Phase 87 | Pending |
| MECH-02 | Phase 88 | Pending |
| MECH-03 | Phase 86 | Pending |
| MECH-04 | Phase 88 | Pending |
| GATE-01 | Phase 87 | Pending |
| GATE-02 | Phase 88 | Pending |

**Coverage:**
- v3.4 requirements: 20 total
- Mapped to phases: 20
- Unmapped: 0

---
*Requirements defined: 2026-02-12*
*Last updated: 2026-02-12 after roadmap creation*
