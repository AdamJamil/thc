# Phase 46: Iron Boat - Context

**Gathered:** 2026-01-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Players can craft and use iron boats for safe lava navigation. Custom item with recipe, custom entity behavior for lava compatibility, and lava-proof item drops. This phase covers the complete iron boat feature — crafting, riding, and destruction behavior.

</domain>

<decisions>
## Implementation Decisions

### Fluid compatibility
- Works on BOTH water and lava (not lava-only)
- Behaves like normal boat on water, adds lava capability as bonus
- Single item serves both purposes

### Speed and handling
- Same speed as vanilla boats on both fluids
- Identical handling characteristics — no "heavier" feel tradeoff

### Destruction behavior
- Only player attacks can break the iron boat
- Immune to: lava damage, fire damage, mob attacks, cacti, collisions
- Very durable — designed for safe Nether traversal

### Item drop behavior
- Initial trajectory toward the player who broke it (not fly-to-player over time)
- Standard item physics after initial direction is set
- Item floats on lava surface (doesn't sink)
- Lava-proof — won't burn in lava

### Crafting
- 5 iron ingots + magma cream in minecart shape (from spec)
- Outputs 1 iron boat

### Claude's Discretion
- Exact entity implementation (extend Boat vs custom entity)
- Texture design details beyond "iron_boat.png"
- Any particle effects during lava travel (probably none needed)

</decisions>

<specifics>
## Specific Ideas

- "There is a direction chosen when any item is spawned and has an initial trajectory. Just make that towards the player that broke it" — not loyalty-trident behavior, just initial direction

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 46-iron-boat*
*Context gathered: 2026-01-25*
