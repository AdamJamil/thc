# Phase 68: Custom Trade Tables - Context

**Gathered:** 2026-01-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace vanilla random trade pools with deterministic, curated trade tables for the 4 allowed professions (librarian, butcher, mason, cartographer). All 37 trade slots are fully specified in REQUIREMENTS.md with exact items, quantities, emerald costs, and 50/50 variant options.

</domain>

<decisions>
## Implementation Decisions

### 50/50 Variant Determination
- Coin flip happens per trade slot when cycling (Phase 70)
- Each cycle rerolls the 50/50 for that slot
- Players can cycle to get specific variants (mending vs unbreaking, etc.)
- Initial assignment also uses 50/50 random selection

### Existing Villagers
- Grandfathered — existing trades left intact
- Only new trade assignments use custom tables
- Simplest approach: modify the method that assigns new trades
- No retroactive replacement of existing villager trades

### Trade Stock Limits
- Unlimited trades — no stock limits
- Trades never run out, no restocking needed
- Simpler implementation, no maxUses tracking

### Trade Content
- All 37 slots fully specified in REQUIREMENTS.md
- No decisions needed — follow the spec exactly
- Librarian: 9 slots (enchanted books with 50/50 variants)
- Butcher: 8 slots (raw meat → emeralds, emeralds → cooked food)
- Mason: 10 slots (bulk building blocks, 64-stacks)
- Cartographer: 10 slots (paper, maps, structure locators)

### Claude's Discretion
- Data structure design for trade tables
- How to hook into vanilla trade assignment
- Whether to use mixins, events, or replacement

</decisions>

<specifics>
## Specific Ideas

- Trade cycling (Phase 70) should reroll 50/50 slots — this phase just needs to support that
- Structure locators from Phase 66 are used in cartographer trades
- Unlimited trades fits the mod's "no tedious grinding" philosophy

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 68-custom-trade-tables*
*Context gathered: 2026-01-31*
