# Phase 87: Blaze Bow - Context

**Gathered:** 2026-02-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Ranged-class fire-damage bow. Covers: Blaze Bow item with custom textures (idle + pulling states), recipe (3 blaze rods + 3 string), fire-on-hit mechanic, slow draw speed, and Ranged Stage 2+ class gate. Depends on Phase 86 bow tagging infrastructure.

</domain>

<decisions>
## Implementation Decisions

### Base arrow damage
- Blaze Bow arrows deal 100% (full) final damage — no damage reduction
- Slow draw speed (1.5x) and class gate justify full damage; fire is bonus DPS on top

### Tipped arrow handling
- Deferred — not decided for this phase. Implement without tipped arrow special handling for now; will be specified later

### Claude's Discretion
- Fire visual presentation (flaming arrow in flight, particles)
- Whether fire duration refreshes on re-hit
- Gate denial mechanic details (prevent drawing vs cancel on release — actionbar message "The bow burns your fragile hands." is locked)
- Pulling texture frame count and timing

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. Follow patterns established in Phase 86.

</specifics>

<deferred>
## Deferred Ideas

- Blaze Bow tipped arrow behavior — to be specified separately

</deferred>

---

*Phase: 87-blaze-bow*
*Context gathered: 2026-02-12*
