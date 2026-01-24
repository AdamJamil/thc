# Phase 39: Entity-Specific Behaviors (Simple) - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Individual mob modifications using straightforward HEAD cancellation or attribute changes. Covers Vex (health/weapon), Phantom (spawn removal), Illager Patrol (stage-gating), and Iron Golem (summon prevention). Complex entity behaviors (Ghast, Enderman) are Phase 40.

</domain>

<decisions>
## Implementation Decisions

### Iron Golem Prevention
- Block player-built golems only (pumpkin + iron block pattern)
- Villager-summoned golems remain functional for village defense
- Existing world-generated golems unaffected

### Claude's Discretion
- Vex health/sword implementation approach (ENTITY_LOAD vs spawn event)
- PhantomSpawner.tick injection method (HEAD cancellation pattern)
- PatrolSpawner.tick stage check implementation
- Iron golem prevention mechanism (pumpkin place handler vs golem spawn check)

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. Requirements are numerically precise (8 HP, stage < 2, etc.).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 39-simple-entity-behaviors*
*Context gathered: 2026-01-24*
