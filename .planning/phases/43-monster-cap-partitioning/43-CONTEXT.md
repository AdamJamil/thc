# Phase 43: Monster Cap Partitioning - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement regional monster caps that prevent any single Overworld region from monopolizing spawn capacity. Surface, Upper Cave, and Lower Cave each have independent caps. Uses SPAWN_REGION attachment from Phase 41 to determine which cap a mob counts against.

</domain>

<decisions>
## Implementation Decisions

### Cap structure
- Three independent regional caps, no global cap applies on top
- All three regions can be at 100% simultaneously (84 total mobs possible)
- Hard-coded absolute values: Surface 21, Upper Cave 28, Lower Cave 35
- Nether and End use vanilla caps unchanged

### Region tracking
- Mob counts against its spawn region tag, not current position
- If mob wanders between regions after spawning, still counts against original region
- Only mobs with `spawnSystem.counted == true` contribute to caps

### Count method
- Fresh count each spawn cycle (iterate loaded mobs, count by region tag)
- No persistent counter maintenance or event-based tracking
- Simple and always accurate, acceptable overhead

### Claude's Discretion
- Exact iteration/counting implementation
- Performance optimization within the fresh-count approach
- How to integrate with existing NaturalSpawnerMixin

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 43-monster-cap-partitioning*
*Context gathered: 2026-01-24*
