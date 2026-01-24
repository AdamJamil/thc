# Phase 40: Entity-Specific Behaviors (Complex) - Context

**Gathered:** 2026-01-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Ghast and Enderman behavior modifications requiring careful implementation. Ghast changes: projectile speed +50%, fire rate -25% (60→80 ticks), explosion fire radius +100%. Enderman changes: 50% chance teleport-behind on hit, proximity aggro within 3 blocks.

</domain>

<decisions>
## Implementation Decisions

### Ghast fire behavior
- Fire spread pattern: Claude's discretion (ring, scatter, or whatever looks natural)
- Deflected fireballs: Yes, player-deflected fireballs also create expanded fire area
- Fire duration: Vanilla fire behavior (no extended burn times)
- Weather: N/A — ghasts are Nether-only, no rain interaction needed

### Claude's Discretion
- Exact fire spread pattern (ring vs scatter vs hybrid)
- Ghast projectile velocity implementation details
- Enderman teleport-behind positioning logic
- Enderman proximity aggro implementation approach
- All technical decisions not discussed above

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches for the undiscussed areas (enderman behaviors, ghast projectile tuning).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 40-complex-entity-behaviors*
*Context gathered: 2026-01-23*
