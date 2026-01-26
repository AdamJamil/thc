# Phase 52: Armor Rebalancing - Context

**Gathered:** 2026-01-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Modify vanilla armor materials to create clear upgrade progression. Adjust armor points and toughness values for leather, copper, iron, diamond, and netherite. No new items created — copper armor already exists in vanilla.

</domain>

<decisions>
## Implementation Decisions

### Armor Point Totals
- Leather: 7 armor (no toughness)
- Copper: 10 armor (no toughness)
- Iron: 15 armor (no toughness)
- Diamond: 18 armor + 4 toughness
- Netherite: 20 armor + 6 toughness

### Per-piece Distribution
- Preserve vanilla ratios (~15% helmet, ~40% chest, ~30% legs, ~15% boots)
- Scale each piece proportionally to new totals
- Half armor points allowed where needed for odd totals

### Half Armor Points
- Individual pieces can have fractional armor values (e.g., 1.5)
- Vanilla supports fractional armor internally
- Tooltips and HUD display half-point values

### Claude's Discretion
- Exact per-piece values within the ratio constraint
- Knockback resistance values (preserve or adjust)
- Durability (preserve vanilla values unless issues arise)

</decisions>

<specifics>
## Specific Ideas

- Clear monotonic progression: each tier strictly better than previous
- Netherite is the ultimate tier (highest armor AND highest toughness)
- Copper tier provides meaningful mid-game upgrade between leather and iron

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 52-armor-rebalancing*
*Context gathered: 2026-01-26*
