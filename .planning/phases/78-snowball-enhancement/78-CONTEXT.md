# Phase 78: Snowball Enhancement - Context

**Gathered:** 2026-02-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Enhance snowball hits with AoE slowness and knockback for Bastion class at Stage 4+. This is a combat utility boon — snowballs become crowd-control tools. Applies Slowness III (2s) to target and nearby hostile mobs, plus knockback. Non-Bastion or lower stage gets vanilla snowball behavior. PvP unchanged.

</domain>

<decisions>
## Implementation Decisions

### AoE Targeting
- Only affects hostile mobs (entities currently targeting a player)
- Neutral mobs (wolves, iron golems, pigmen) are NOT affected
- Players are never affected (PvP unchanged per spec)

### Knockback Direction
- Knockback direction is away from the thrower, not the impact point
- Consistent direction regardless of snowball trajectory
- ~1 block knockback distance per spec

### Effect Stacking
- Use vanilla Minecraft effect application behavior
- If mob already has Slowness, Minecraft handles duration/level comparison
- No custom stacking logic needed

### Immune Mobs
- Try to apply both slowness and knockback independently
- If a mob resists slowness (e.g., boss immunity), knockback can still apply
- If a mob resists knockback (e.g., Warden), slowness can still apply
- Effects are not bundled — each applied separately, mobs selective about what they receive

### Claude's Discretion
- Particle/sound effects on enhanced snowball hit (if any)
- Exact knockback strength tuning to achieve ~1 block distance
- Performance optimization for AoE entity query

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches for projectile hit handling and effect application.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 78-snowball-enhancement*
*Context gathered: 2026-02-03*
