# Phase 51: Brewing Removal - Context

**Gathered:** 2026-01-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Complete removal of potion economy. Brewing stands removed from all structure spawns and crafting. Piglin bartering no longer offers potions. Brewing ingredients remain obtainable — only the brewing process itself is removed.

</domain>

<decisions>
## Implementation Decisions

### Structure Spawns
- Brewing stands removed from villages, igloos, and any other structure that generates them
- Use existing setBlock redirect pattern established in prior milestones

### Recipe Removal
- Brewing stand recipe removed via REMOVED_RECIPE_PATHS pattern
- Consistent with existing recipe removal approach

### Bartering Loot
- Potion entries (Fire Resistance, Splash Fire Resistance) removed entirely from piglin bartering loot table
- No replacement items — piglins roll other existing rewards from the table
- Loot table override via data pack pattern

### Ingredient Availability
- Blaze powder, nether wart, glass bottles remain obtainable
- Only the brewing mechanism is removed, not ingredients
- Players can still collect these items for other uses

### Claude's Discretion
- Specific structure block filtering implementation details
- Loot table modification approach (data pack override vs mixin)

</decisions>

<specifics>
## Specific Ideas

No specific requirements — straightforward removal using established patterns.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 51-brewing-removal*
*Context gathered: 2026-01-26*
