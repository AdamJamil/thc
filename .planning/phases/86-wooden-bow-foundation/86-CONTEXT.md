# Phase 86: Wooden Bow Foundation - Context

**Gathered:** 2026-02-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Rework the vanilla bow into a "Wooden Bow" with new horizontal drag arrow physics, 50% damage, tipped arrow restriction, and glowing removal on projectile hits. This is the foundation phase — class-specific bows (Blaze, Breeze) are phases 87–88.

</domain>

<decisions>
## Implementation Decisions

### Bow identity
- Vanilla bow renamed to "Wooden Bow" in inventory and tooltips
- Recipe stays 3 sticks + 3 string (unchanged from vanilla)
- Arrows fired from Wooden Bow tagged "wooden_bow" for downstream damage logic

### Arrow physics (horizontal drag)
- Per-tick drag: `drag_coefficient = max(0.8, 1 - 0.015 * ticks_in_flight)`
- Horizontal velocity multiplied by drag_coefficient each tick; vertical velocity untouched
- This replaces the old gravity-over-time curve which must be fully removed
- Drag factors set per bow: wooden 0.015, blaze 0.015, breeze 0.01 (infrastructure for phases 87–88)

### Damage
- Wooden Bow arrows deal 50% of normal final damage
- Damage reduction applied after all other calculations (final multiplier)

### Tipped arrow restriction
- Wooden Bow cannot fire tipped arrows
- If player attempts to fire a tipped arrow, a regular arrow is consumed instead
- Success criteria specifies "consumes a regular arrow instead" — tipped arrow stays in inventory

### Glowing removal
- Mobs hit by player projectiles no longer receive the Glowing effect
- Applies to all player projectiles (arrows, snowballs, etc.) per requirement DMG-05

### Claude's Discretion
- Edge case: player has ONLY tipped arrows and no regular arrows — bow simply doesn't fire (no regular arrow to consume)
- Whether to show actionbar feedback when tipped arrow is blocked ("Your bow can't fire tipped arrows")
- How to implement bow tagging infrastructure (NBT, custom component, or arrow entity field) — researcher/planner decides

</decisions>

<specifics>
## Specific Ideas

No specific requirements — the requirements document (REQUIREMENTS.md) provides exact formulas, values, and behavior specs. Implementation should follow those precisely.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 86-wooden-bow-foundation*
*Context gathered: 2026-02-12*
