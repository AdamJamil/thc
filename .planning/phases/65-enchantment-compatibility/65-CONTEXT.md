# Phase 65: Enchantment Compatibility - Context

**Gathered:** 2026-01-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Remove mutual exclusivity between protection enchantments (protection, blast_protection, fire_protection, projectile_protection) and damage enchantments (sharpness, smite, bane_of_arthropods) so they can all coexist on the same gear. All stacked enchantments apply their effects simultaneously.

</domain>

<decisions>
## Implementation Decisions

### Acquisition Methods
- Works through any valid enchantment method — anvil, lectern, enchanting table
- Treated as normal compatible enchantments, not special-cased
- Follows existing v2.5 enchantment system patterns

### Effect Behavior
- All stacked enchantments apply simultaneously at full strength
- No level caps or diminishing returns
- Protection types are additive (standard vanilla damage reduction formula)
- Damage types are additive (standard vanilla damage bonus formula)

### Claude's Discretion
- Specific mixin injection point (EnchantmentDefinition.areCompatible or equivalent)
- How to intercept the compatibility check cleanly
- Tag-based vs hardcoded enchantment ID matching

</decisions>

<specifics>
## Specific Ideas

No specific requirements — follow existing enchantment compatibility patterns.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 65-enchantment-compatibility*
*Context gathered: 2026-01-29*
