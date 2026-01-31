# Phase 66: Structure Locators - Context

**Gathered:** 2026-01-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Custom compass-style items that point to specific structures. Foundation for cartographer trades (Phase 68). Players obtain these through villager trades, not crafting.

</domain>

<decisions>
## Implementation Decisions

### Target structures
- 6 locator types: trial chamber, pillager outpost, nether fortress, bastion, ancient city, stronghold
- Each is a distinct item with its own texture

### Dimension behavior
- Overworld locators: trial chamber, pillager outpost, ancient city, stronghold
- Nether locators: nether fortress, bastion
- Locators only work in their intended dimension

### Search behavior
- Compass-style needle pointing toward nearest matching structure
- 100 chunk search radius cap (performance protection)
- When no structure found: needle spins randomly (like vanilla compass in Nether)

### Visual identity
- 6 distinct textures (one per structure type)
- Naming: "[Structure] Locator" (e.g., "Fortress Locator", "Stronghold Locator")

### Claude's Discretion
- Texture design (colors, iconography)
- Exact spin animation speed when no target
- Whether to use lodestone compass mechanics or custom implementation

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. Follow vanilla compass patterns where sensible.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 66-structure-locators*
*Context gathered: 2026-01-30*
