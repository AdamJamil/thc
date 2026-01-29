# Phase 64: Food Economy - Dough System - Context

**Gathered:** 2026-01-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace instant bread crafting with a cooking-based dough system. Add leather drops to pigs and sheep. Bucket preservation pattern from phase 52 applies.

</domain>

<decisions>
## Implementation Decisions

### Dough recipe
- Shapeless recipe: 3 wheat + water bucket (copper or iron)
- Bucket preserved and returned to player after crafting
- Follow existing copper bucket milk recipe pattern for bucket preservation

### Dough item
- Not edible — purely a crafting/smelting ingredient
- Standard item stack size (64)
- Custom texture required

### Smelting
- Dough smelts into bread in furnace and smoker
- Standard cooking time and XP (vanilla defaults)

### Leather drops
- Pigs: Add leather drops (0-2 base, +1 per looting level)
- Sheep: Add leather drops alongside existing wool and mutton (all three drop)
- Drop rates match cow leather (0-2 base, +1 per looting)

### Claude's Discretion
- Exact smelting XP value
- Dough texture design approach
- Loot table JSON structure details

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

*Phase: 64-food-economy-dough-system*
*Context gathered: 2026-01-29*
