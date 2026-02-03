# Phase 79: Boat Mastery - Context

**Gathered:** 2026-02-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Enable land boat placement and mob trapping as a Bastion Stage 5+ boon. Boats become crowd-control tools: placeable on solid ground, hostile mobs become trapped passengers on collision, then break free after 4 seconds. Also includes recipe modifications (2 copper ingots for all 9 wood variants) and stack size increase (16).

</domain>

<decisions>
## Implementation Decisions

### Trap trigger
- Vanilla behavior: mobs naturally enter boats when colliding with them
- No special player action required — place boat, push/lure mob into it

### Breakout behavior
- Trapped mobs break out after 4 seconds (80 ticks)
- Boat drops intact as item (reusable, not destroyed)
- Mob is released at boat's position

### Combat access
- Trapped mobs can be attacked while in boat
- Boats function as crowd control that enables damage (not just repositioning)

### Claude's Discretion
- "Solid ground" definition (standard Minecraft solid block check)
- Action bar message for non-Bastion/low-stage land placement attempts
- Breakout visual/audio feedback (if any beyond boat drop sound)
- Edge cases: boat destroyed by other means during trap, multiple mobs, etc.

</decisions>

<specifics>
## Specific Ideas

No specific requirements — spec is clear, open to standard approaches for implementation details.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 79-boat-mastery*
*Context gathered: 2026-02-03*
