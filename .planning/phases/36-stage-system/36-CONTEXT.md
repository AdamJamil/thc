# Phase 36: Stage System - Context

**Gathered:** 2026-01-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Server-wide stage progression (1→2→3→4→5) with per-player boon level tracking. This is scaffolding — boon level is tracked but has no gameplay effect yet. Effects will be added in future phases.

</domain>

<decisions>
## Implementation Decisions

### Command behavior
- `/advanceStage` requires operator permissions (op only)
- At stage 5, command returns error message (already at max stage)
- Stage advances increment all connected players' boon levels

### Notification
- Broadcast to all players as red actionbar message
- Format: "Trial complete. The world has advanced to Stage X."

### Late joiners
- Players who join after stage has advanced get boon level matching current stage
- New player at stage 3 server → boon level 3

### Claude's Discretion
- SavedData implementation details
- Command feedback messages for operator
- Edge case handling for disconnected players during advance

</decisions>

<specifics>
## Specific Ideas

- Boon level is a counter only for now — no effects until future phases implement class-specific boons
- Stage is the server's progress through trials; boon level is the player's accumulated power

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 36-stage-system*
*Context gathered: 2026-01-23*
