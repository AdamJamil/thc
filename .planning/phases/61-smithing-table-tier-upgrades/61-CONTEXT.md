# Phase 61: Smithing Table Tier Upgrades - Context

**Gathered:** 2026-01-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Equipment tier upgrades at smithing tables — leather→copper→iron→diamond for armor, wooden→stone→copper→iron→diamond for tools. Material costs match crafting recipes. Enchantments preserved, durability restored. Existing netherite upgrade unchanged.

</domain>

<decisions>
## Implementation Decisions

### Material Costs
- "Count matches crafting recipe" — use the number of ingots/materials in the corresponding crafting recipe
- Example: Iron chestplate = 8 ingots, so copper→iron chestplate upgrade costs 8 iron ingots

### Upgrade Behavior
- Enchantments fully preserved (transfer all from source to result)
- Durability restored to maximum for target tier (not transferred)
- Source item consumed

### Compatibility
- Existing diamond→netherite upgrade via smithing template must continue working unchanged
- Do not interfere with vanilla smithing behavior

### Claude's Discretion
- SmithingMenu mixin approach vs recipe-based approach
- Template slot usage (empty vs custom template vs no requirement)
- Specific validation and error handling

</decisions>

<specifics>
## Specific Ideas

No specific requirements — requirements document fully specifies behavior.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 61-smithing-table-tier-upgrades*
*Context gathered: 2026-01-29*
