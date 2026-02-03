# Phase 77: Boon 2 - Parry Aggro & Sweeping - Context

**Gathered:** 2026-02-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Enable two combat capabilities for Bastion class at Stage 3+:
1. Successful parry propagates threat to nearby mobs (they target the Bastion)
2. Sweeping edge attacks damage adjacent enemies

Non-Bastion classes are unaffected. Bastion at Stage 1-2 does not get these abilities.

</domain>

<decisions>
## Implementation Decisions

### Parry threat propagation
- Use existing 3-block radius from `thc$stunNearby` in LivingEntityMixin
- Add threat to mobs in that radius when parry succeeds
- Threat amount should match existing patterns in the codebase
- Only triggers for Bastion class at Stage 3+

### Sweeping edge
- Modify existing `thc$disableSweepAttack` mixin to conditionally enable
- Use vanilla sweeping edge behavior once enabled (no custom damage values)
- Only enabled for Bastion class at Stage 3+
- Non-Bastion and lower-stage Bastion keeps sweeping disabled

### Claude's Discretion
- Exact threat amount (should be meaningful but consistent with existing threat values)
- Implementation approach for the conditional class/stage checks
- Whether to extract common class+stage gate logic into a utility

</decisions>

<specifics>
## Specific Ideas

- Parry threat should use existing infrastructure in ThreatManager.addThreat()
- The 3-block radius for stun already exists and should be reused for threat propagation
- Both features gate on the same condition: Bastion class + Stage 3+

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 77-boon-2-parry-aggro-sweeping*
*Context gathered: 2026-02-03*
