# Phase 69: Manual Leveling - Context

**Gathered:** 2026-01-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace automatic villager leveling with manual emerald-payment advancement. Players right-click villagers with emerald to level them up, gated by world stage requirements. Villagers no longer auto-level from trading XP.

</domain>

<decisions>
## Implementation Decisions

### Level-up Interaction
- Right-click with emerald in hand triggers level up (not shift+click)
- Emerald in hand = special action; anything else opens trade GUI
- Vanilla particles and sound on successful level up (green happy particles, ding)
- One level up per click, even with emerald stack

### Feedback Messages (action bar)
- Not enough XP: "Not enough experience to level up!"
- Already max level: "Already at master!"
- Stage requirement not met: "Complete the next trial!"
- Note: At 0 XP, no message — that's the cycling case for Phase 70

### Stage Gates
- Novice → Apprentice requires Stage 2
- Apprentice → Journeyman requires Stage 3
- Journeyman → Expert requires Stage 4
- Expert → Master requires Stage 5
- Novice trades always available (no stage requirement)
- Emerald NOT consumed when stage requirement not met

### XP Thresholds
- 2 trades to max XP at Novice
- 3 trades to max XP at Apprentice
- 4 trades to max XP at Journeyman
- 5 trades to max XP at Expert
- Uniform XP per trade (all trades give same amount)
- XP gained every time trade completed (no per-restock limits)
- XP caps at max for current level (no overflow)
- Vanilla XP bar appearance

### Edge Cases
- Only individual emeralds work (not emerald blocks)
- Naturally spawned villagers always start at Novice (no high-level spawn handling needed)

### Claude's Discretion
- GUI timing (whether trade GUI must be closed for level up)
- Exact XP values to achieve the trade-count targets
- Code structure for UseEntityCallback

</decisions>

<specifics>
## Specific Ideas

- Structure the callback so Phase 70 can easily add trade cycling logic at the same injection site (0 XP case)
- Follow vanilla leveling visual/audio patterns for familiarity

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 69-manual-leveling*
*Context gathered: 2026-01-31*
