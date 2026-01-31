# Requirements: THC v3.0 Revival System

**Defined:** 2026-01-31
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v3.0 Requirements

Requirements for revival system. Each maps to roadmap phases.

### Downed State

- [ ] **DOWN-01**: Player enters downed state when HP reaches 0 (instead of dying)
- [ ] **DOWN-02**: Downed player displays in laying/horizontal pose
- [ ] **DOWN-03**: Downed player cannot perform any actions (movement, items, camera)
- [ ] **DOWN-04**: Downed player is invulnerable to all damage sources
- [ ] **DOWN-05**: Downed player is ignored by mob AI (not targeted)

### Revival Mechanics

- [ ] **REVV-01**: Alive player can revive downed player by sneaking within 2 blocks
- [ ] **REVV-02**: Reviver must stay still while reviving (movement pauses progress)
- [ ] **REVV-03**: Revival progress increases at 0.5 per tick (10 seconds total)
- [ ] **REVV-04**: Support class revival progress is 1.0 per tick (5 seconds total)
- [ ] **REVV-05**: Revival progress is preserved when interrupted (does not reset)

### Revival UI

- [ ] **RVUI-01**: Radial progress ring displays around cursor when looking at downed player within 2 blocks
- [ ] **RVUI-02**: Empty ring texture (revival_progress_empty.png) shown at 0% progress
- [ ] **RVUI-03**: Filled ring texture (revival_progress_full.png) fills radially with progress percentage

### Revival Outcome

- [ ] **RVOU-01**: Revived player returns to normal play state
- [ ] **RVOU-02**: Revived player has 50% of max HP
- [ ] **RVOU-03**: Revived player has 0 hunger
- [ ] **RVOU-04**: Green particles play on successful revival

## Future Requirements

Deferred to later milestones.

- **DOWN-06**: Bleedout timer (die after N seconds if not revived) — deferred, may add if solo play becomes issue
- **REVV-06**: Self-revive mechanic — explicitly excluded per design
- **RVUI-04**: Downed player indicator visible through walls — deferred

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Bleedout timer | User specified indefinite downed duration |
| Crawling while downed | User specified no actions while downed |
| Self-revive | Against cooperative design intent |
| Revive items | Keep revival purely cooperative |
| Downed player damage | User specified invulnerable while downed |
| Knockdown shield | Complexity, not part of spec |
| Multiple revivers stacking | Single reviver model simpler |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DOWN-01 | TBD | Pending |
| DOWN-02 | TBD | Pending |
| DOWN-03 | TBD | Pending |
| DOWN-04 | TBD | Pending |
| DOWN-05 | TBD | Pending |
| REVV-01 | TBD | Pending |
| REVV-02 | TBD | Pending |
| REVV-03 | TBD | Pending |
| REVV-04 | TBD | Pending |
| REVV-05 | TBD | Pending |
| RVUI-01 | TBD | Pending |
| RVUI-02 | TBD | Pending |
| RVUI-03 | TBD | Pending |
| RVOU-01 | TBD | Pending |
| RVOU-02 | TBD | Pending |
| RVOU-03 | TBD | Pending |
| RVOU-04 | TBD | Pending |

**Coverage:**
- v3.0 requirements: 17 total
- Mapped to phases: 0 (pending roadmap)
- Unmapped: 17

---
*Requirements defined: 2026-01-31*
*Last updated: 2026-01-31 after initial definition*
