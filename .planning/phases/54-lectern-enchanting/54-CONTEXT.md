# Phase 54: Lectern Enchanting - Context

**Gathered:** 2026-01-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Players can use lecterns to apply stage 1-2 enchantments repeatedly without consuming books. Right-click interaction model for both placing books and applying enchantments. Unlimited use per book — the lectern becomes an enchanting station for early-stage progression.

</domain>

<decisions>
## Implementation Decisions

### Book Placement
- Right-click lectern while holding enchanted book to attach it
- Only empty lecterns accept enchantment books (no vanilla book/writable book present)
- Shift+right-click removes book from lectern
- Enchanted book renders visually on the lectern (players can see which lectern has which book)

### Enchanting Interaction
- Right-click lectern with compatible gear in hand to apply enchantment
- Success feedback: enchanting sound + particles around item
- Incompatible item: action bar message "Incompatible enchantment!"
- Already enchanted with same enchantment: action bar message "Already applied!"

### Stage Gating
- Hardcoded list of stage 1-2 enchantments (list defined in existing spec/requirements)
- Stage 3+ books rejected at placement: "This enchantment requires an enchanting table!"
- Lectern enchanting available to all players regardless of class selection

### Level Requirements
- Level 10 minimum required to enchant
- Below level 10: "You must be level 10!" (exact message from spec)
- Enchanting costs 3 levels (player level drops by 3 after each enchant)
- No cost preview needed — players learn through experience

### Claude's Discretion
- Exact particle effect used for success feedback
- Sound effect choice (use existing enchanting sounds)
- Internal implementation of book attachment (block entity vs attachment)

</decisions>

<specifics>
## Specific Ideas

- Interaction model mirrors natural Minecraft feel — right-click to place, right-click to use
- Visual feedback prioritized over text messages for success (sound + particles)
- Error states use action bar messages to keep UI clean

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 54-lectern-enchanting*
*Context gathered: 2026-01-27*
