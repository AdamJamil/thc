# Phase 42: Regional Spawn System - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Modify NATURAL monster spawning to use region-based distributions with custom mob types across three Overworld zones (Surface/Upper Cave/Lower Cave) plus End. Custom distributions replace vanilla spawns at selection time. Structure spawns bypass the system entirely.

</domain>

<decisions>
## Implementation Decisions

### Distribution Percentages
- Custom spawns REPLACE vanilla spawns (not additive) — total spawn count unchanged
- Bypass vanilla spawn conditions for custom mobs (witches spawn anywhere, blazes/breezes don't need fortress)
- If custom spawn fails (space/collision), skip that spawn attempt entirely — no fallback to vanilla
- CRITICAL: Preserve existing base chunk spawn blocking — do not break or modify that logic

### Pillager Variants
- MELEE pillagers: iron sword in main hand, no crossbow
- RANGED pillagers: standard crossbow (vanilla behavior)
- Melee pillagers need forced melee AI — don't allow ranged behaviors with no ranged weapon
- Split percentages from spec: Upper cave 10% ranged / 25% melee; Lower cave 25% melee only

### Region Boundary Handling
- Cap counting uses spawn origin (NBT tag), not current mob position
- Y=0 with no sky visibility = Upper Cave (Y >= 0 condition)
- Any sky visibility = Surface, even through small holes (use vanilla isSkyVisible semantics)
- Mob transformations: don't write special code, let default behavior apply

### Pack Spawning Behavior
- Pack size [1,4] with uniform distribution for custom spawns
- Pack members spread nearby (few blocks), not stacked at same position
- Same mob type per pack — if melee pillager is rolled, whole pack is melee
- Partial spawns allowed — if 3 of 4 fit, spawn those 3

### Claude's Discretion
- Exact spread radius/pattern for pack spawns
- How to implement melee AI for pillagers (goal removal vs goal priority)
- Integration approach with existing NaturalSpawnerMixin

</decisions>

<specifics>
## Specific Ideas

- Percentages and region definitions are fully specified in MILESTONE_EXTRA_FEATURES_BATCH_7.md
- Must integrate with existing claimed chunk spawn blocking
- Spec requires NBT tags from Phase 41 for cap counting

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 42-regional-spawn-system*
*Context gathered: 2026-01-24*
