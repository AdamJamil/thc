# Requirements: THC v3.0 Revival System

**Defined:** 2026-01-31
**Core Value:** Risk must be required for progress. No tedious grinding to avoid challenge.

## v3.0 Requirements

Requirements for revival system. Each maps to roadmap phases.

### Downed State

- [ ] **DOWN-01**: Player enters downed state when HP reaches 0 (instead of dying)
- [ ] **DOWN-02**: Downed player is set to spectator mode
- [ ] **DOWN-03**: Downed player's downed location is tracked (for tether and revival)
- [ ] **DOWN-04**: Downed player is teleported back if more than 50 blocks from downed location
- [ ] **DOWN-05**: Spectator mode provides invulnerability and mob AI exclusion automatically

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

- [ ] **RVOU-01**: Revived player is set back to survival mode
- [ ] **RVOU-02**: Revived player is teleported to their downed location
- [ ] **RVOU-03**: Revived player has 50% of max HP
- [ ] **RVOU-04**: Revived player has 0 hunger
- [ ] **RVOU-05**: Green particles play on successful revival

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
| Self-revive | Against cooperative design intent |
| Revive items | Keep revival purely cooperative |
| Knockdown shield | Complexity, not part of spec |
| Multiple revivers stacking | Single reviver model simpler |
| Visible downed body | Spectator mode approach - body is invisible |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DOWN-01 | Phase 72 | Pending |
| DOWN-02 | Phase 72 | Pending |
| DOWN-03 | Phase 72 | Pending |
| DOWN-04 | Phase 72 | Pending |
| DOWN-05 | Phase 72 | Pending |
| REVV-01 | Phase 73 | Pending |
| REVV-02 | Phase 73 | Pending |
| REVV-03 | Phase 73 | Pending |
| REVV-04 | Phase 73 | Pending |
| REVV-05 | Phase 73 | Pending |
| RVUI-01 | Phase 74 | Pending |
| RVUI-02 | Phase 74 | Pending |
| RVUI-03 | Phase 74 | Pending |
| RVOU-01 | Phase 73 | Pending |
| RVOU-02 | Phase 73 | Pending |
| RVOU-03 | Phase 73 | Pending |
| RVOU-04 | Phase 73 | Pending |
| RVOU-05 | Phase 73 | Pending |

**Coverage:**
- v3.0 requirements: 18 total
- Mapped to phases: 18
- Unmapped: 0

---
*Requirements defined: 2026-01-31*
*Last updated: 2026-01-31 after roadmap creation*
