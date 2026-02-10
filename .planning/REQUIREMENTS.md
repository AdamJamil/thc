# Requirements: THC (True Hardcore)

**Defined:** 2026-02-09
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v3.2 Requirements

Requirements for Effects GUI milestone. Each maps to roadmap phases.

### HUD Layout

- [ ] **HUD-01**: Player sees active status effects displayed in the bottom-left corner of the screen
- [ ] **HUD-02**: Effects are stacked vertically upward from the bottom-left with no gaps between frames
- [ ] **HUD-03**: Effects are sorted by priority: Wither > Poison > Resistance > Absorption > Strength > Slowness > Weakness > Speed > all others

### Frame Rendering

- [ ] **FRAM-01**: Each effect is rendered inside a frame using effect_frame.png (44x44 base)
- [ ] **FRAM-02**: Vanilla mob effect icon is scaled 2x (18x18 -> 36x36) and centered inside the frame with 4px border
- [ ] **FRAM-03**: Green 50% transparent overlay covers the effect icon from the bottom, proportional to remaining duration (smooth per-tick updates)
- [ ] **FRAM-04**: Roman numeral (I through X) from numeral.png (13x9 subsections) is drawn at 5px right and 5px down from top-left of frame

### Scaling

- [ ] **SCAL-01**: Video Settings menu includes an "Effects GUI Scaling" option
- [ ] **SCAL-02**: Scale range maps frame width from 2% to 20% of screen width

### Lifecycle

- [ ] **LIFE-01**: Effects appear instantly when applied and disappear instantly when expired or removed
- [ ] **LIFE-02**: Duration overlay updates smoothly every tick based on remaining vs original duration

## Future Requirements

None identified.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Replace vanilla effect display | Exists alongside vanilla inventory effect icons |
| Animated transitions | Effects appear/disappear instantly per user spec |
| Effect tooltips on hover | Not requested, keep HUD non-interactive |
| Custom effect icons | Uses vanilla mob effect textures |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| HUD-01 | Phase 80 | Pending |
| HUD-02 | Phase 80 | Pending |
| HUD-03 | Phase 80 | Pending |
| FRAM-01 | Phase 80 | Pending |
| FRAM-02 | Phase 80 | Pending |
| FRAM-03 | Phase 81 | Pending |
| FRAM-04 | Phase 81 | Pending |
| SCAL-01 | Phase 82 | Pending |
| SCAL-02 | Phase 82 | Pending |
| LIFE-01 | Phase 81 | Pending |
| LIFE-02 | Phase 81 | Pending |

**Coverage:**
- v3.2 requirements: 11 total
- Mapped to phases: 11
- Unmapped: 0

---
*Requirements defined: 2026-02-09*
*Last updated: 2026-02-09 after roadmap creation*
