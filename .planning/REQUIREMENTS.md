# Requirements: THC (True Hardcore)

**Defined:** 2026-02-10
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v3.3 Requirements

Requirements for Enemy Health Bars milestone. Each maps to roadmap phases.

### Health Bar Rendering

- [ ] **HBAR-01**: Hostile mobs within 32 blocks display a floating health bar above their head
- [ ] **HBAR-02**: Health bar renders ~0.5 blocks above mob head, always facing the player (billboard)
- [ ] **HBAR-03**: `health_bar_empty.png` renders as base layer (328x62px texture)
- [ ] **HBAR-04**: `health_bar_full.png` renders over empty bar, clipped to mob's current HP percentage (8px offset + 312px * hp/maxHp)
- [ ] **HBAR-05**: `health_bar_absorption.png` renders over full bar, clipped to mob's absorption percentage
- [ ] **HBAR-06**: Health bar hidden when mob is at full HP and has no effects

### Mob Effects Display

- [ ] **MEFF-01**: Active status effects on mob render as icons left-to-right directly above the health bar (no gap)
- [ ] **MEFF-02**: Effect icons use same frame/icon/overlay style as player effects GUI
- [ ] **MEFF-03**: Effect duration overlay drains per tick (matching effects GUI behavior)

### Scaling

- [ ] **SCAL-01**: Single "Mob Health Bar" scaling slider in Video Settings controls all health bar and effect icon sizes
- [ ] **SCAL-02**: Scaling persists to file (same pattern as effects GUI scaling)

## Future Requirements

None deferred.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Health bars on passive/neutral mobs | Not needed for combat awareness |
| Boss mob health bars | Already have vanilla boss bars |
| Nameplate integration | Health bar is separate from vanilla nametags |
| Damage numbers | Different feature, adds visual clutter |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| HBAR-01 | — | Pending |
| HBAR-02 | — | Pending |
| HBAR-03 | — | Pending |
| HBAR-04 | — | Pending |
| HBAR-05 | — | Pending |
| HBAR-06 | — | Pending |
| MEFF-01 | — | Pending |
| MEFF-02 | — | Pending |
| MEFF-03 | — | Pending |
| SCAL-01 | — | Pending |
| SCAL-02 | — | Pending |

**Coverage:**
- v3.3 requirements: 11 total
- Mapped to phases: 0
- Unmapped: 11

---
*Requirements defined: 2026-02-10*
*Last updated: 2026-02-10 after initial definition*
