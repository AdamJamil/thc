# Phase 70: Trade Cycling - Context

**Gathered:** 2026-01-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Players can reroll current-rank trades by paying an emerald. The cycling mechanism allows players to search for specific trades within the deterministic trade tables established in Phase 68.

</domain>

<decisions>
## Implementation Decisions

### Trigger Mechanism
- Right-click (not shift+right-click) villager while holding emerald
- Only works when villager is at 0 XP (hasn't been traded with since last level/cycle)
- GUI must be closed — cannot cycle from within trade screen

### Reroll Behavior
- Random selection from trade pool, excluding current trade
- Guarantees a different trade if pool has 2+ options
- If pool has only 1 option: block cycling, don't consume emerald, show failure feedback (villager shakes head)

### Cost and Limits
- 1 emerald consumed per successful cycle
- Unlimited cycling allowed — pay emerald, get new trade, repeat as desired
- No cooldowns or caps

### Scope of Change
- Only current rank trades are rerolled
- Earlier rank trades are preserved unchanged
- Level progress (XP) stays at 0 after cycling

### Player Feedback
- Success: Villager sound + emerald/happy particles
- Failure (single-trade pool): Villager shake head, no emerald consumed

### Claude's Discretion
- Specific sound effect choices (within villager sound palette)
- Particle effect details (color, count, pattern)
- Internal trade pool iteration/selection algorithm

</decisions>

<specifics>
## Specific Ideas

No specific requirements — standard implementation approaches apply.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 70-trade-cycling*
*Context gathered: 2026-01-31*
