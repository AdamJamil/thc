# Phase 67: Job Restriction - Context

**Gathered:** 2026-01-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Prevent villagers from acquiring or keeping professions other than mason, librarian, butcher, or cartographer. This is a restriction layer — no new capabilities, just blocking disallowed states.

</domain>

<decisions>
## Implementation Decisions

### Enforcement timing
- Intercept at setVillagerData — any attempt to set an illegal profession is rejected
- No tick-based scanning or one-time migration needed
- Covers all scenarios: natural spawns, job block acquisition, zombie villager cures, NBT edits

### Zombie villager cure behavior
- Cured villagers with disallowed professions become jobless
- The cure completes normally, but profession assignment is rejected
- Villager can then pick up an allowed job block

### POI blocking
- Disallowed job blocks (brewing stand, smithing table, composter, etc.) should not register as valid POIs for villager job seeking
- Combined with setVillagerData interception, this provides defense in depth

### Claude's Discretion
- Exact mixin injection points (setVillagerData, POI registration)
- Logging/debugging output for blocked profession attempts
- Whether to handle nitwits specially (they have no profession, so likely no-op)

</decisions>

<specifics>
## Specific Ideas

No specific requirements — standard restriction pattern via mixins.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 67-job-restriction*
*Context gathered: 2026-01-30*
