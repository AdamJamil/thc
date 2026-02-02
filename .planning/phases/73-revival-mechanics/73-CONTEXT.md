# Phase 73: Revival Mechanics - Context

**Gathered:** 2026-02-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Alive players can revive downed teammates through cooperative interaction. This phase implements the revival progress system, proximity detection, class bonuses, and revival completion effects. UI feedback is handled in Phase 74.

</domain>

<decisions>
## Implementation Decisions

### Revival trigger
- Player must be sneaking within 2 blocks of downed location
- Looking around or moving while sneaking does NOT pause progress
- Proximity + sneaking = progress accumulates

### Progress behavior
- 10 seconds base (0.5 progress/tick), 5 seconds for Support class (1.0/tick)
- Progress is preserved when interrupted (does not reset)
- No bleed-out timer — downed players stay downed indefinitely until revived
- Multiple revivers stack progress (atomically add to same progress value)

### Revival completion
- Player set to survival mode
- Teleported to downed location
- **50% HP, 6 hunger** (not 0 hunger)
- Green particles on completion
- No invulnerability frames
- No custom sound effects — particles only

### Claude's Discretion
- Exact particle type and quantity for green particles
- How to handle atomic progress addition for multi-reviver case
- Progress data structure (attachment on downed player vs separate tracker)

</decisions>

<specifics>
## Specific Ideas

- "Effectively I think there should be no code for [multi-revive], but need to carefully, atomically add progress to revival bar" — implementation should naturally support stacking without special-casing

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 73-revival-mechanics*
*Context gathered: 2026-02-02*
