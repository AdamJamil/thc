# Phase 55: Enchanting Table Overhaul - Context

**Gathered:** 2026-01-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Transform enchanting tables from RNG-based enchanting to deterministic book-slot mechanics. The lapis slot becomes a book slot where enchanted books determine exact enchantment applied. Supports all stages (1-5) with tiered level requirements. Includes new recipe requiring soul dust.

</domain>

<decisions>
## Implementation Decisions

### GUI Behavior
- GUI opens normally even with <16 bookshelves, but enchant buttons are disabled/grayed
- Lapis slot replaced with book slot — accepts enchanted books (easiest implementation)
- Replace lapis icon with `book_slot.png` (greyed out book icon)
- Enchant button on right side displays level requirement in vanilla format
- When hovering enchant button with valid book, tooltip shows enchantment name only (no level suffix since all single-level)
- Button unclickable when requirements not met (no action bar message needed)
- Level requirements by stage:
  - Stage 1-2: Level 10 minimum, costs 3 levels
  - Stage 3: Level 20 minimum, costs 3 levels
  - Stage 4-5: Level 30 minimum, costs 3 levels

### Bookshelf Validation
- Minimum 15 bookshelves required (all valid positions filled — vanilla max detectable is 15)
- No visual indicator of bookshelf count in GUI
- Silent disabled state when <15 bookshelves (no tooltip explanation)
- Keep vanilla bookshelf placement rules (2-block radius, same level or 1 above, no obstructions)

### Enchanting Flow
- Gear goes in existing left slot (same as vanilla)
- Book goes in former lapis slot (right side)
- Book stays in slot after successful enchant (unlimited uses, like lectern)
- Can add enchantments to already-enchanted items (stacking allowed)
- Button disabled if:
  - Enchantment incompatible with item type
  - Item already has the enchantment from the book
  - Player level too low
  - Fewer than 15 bookshelves

### Recipe Change
- New recipe: ISI/SBS/ISI where I = Block of Iron, S = Soul Dust, B = Book
- Vanilla enchanting table recipe removed entirely
- Existing enchanting tables in world use new mechanics (no re-crafting needed)
- Soul dust item must be created — use `soul_dust.png` texture (already exists)
- Soul dust acquisition: To be specified later (not part of this phase)

### Claude's Discretion
- Exact disabled button visual styling
- How to handle edge cases in enchantment compatibility checking
- Whether to add any sound effects for success/failure
- Implementation details for slot type validation

</decisions>

<specifics>
## Specific Ideas

- Level requirement display should match vanilla enchanting table format exactly
- Book slot icon (`book_slot.png`) is a greyed-out book to indicate where enchanted books go
- Stage 1-2 enchantments work at enchanting table too (not just lectern) — lectern is unlimited, table also unlimited but requires more infrastructure

</specifics>

<deferred>
## Deferred Ideas

- Soul dust acquisition method — specified later, not part of this phase

</deferred>

---

*Phase: 55-enchanting-table-overhaul*
*Context gathered: 2026-01-28*
