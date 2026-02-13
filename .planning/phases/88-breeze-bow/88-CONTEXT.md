# Phase 88: Breeze Bow - Context

**Gathered:** 2026-02-12
**Status:** Ready for planning

<domain>
## Phase Boundary

Support-class bow with knockback and fast draw. Item, recipe, damage profile (75%), knockback, draw speed (0.75x), and class gating (Support Stage 2+). Follows the same bow infrastructure established in Phase 86.

</domain>

<decisions>
## Implementation Decisions

### Splash AoE — Removed
- Tipped arrow splash AoE mechanic is **deferred** — not part of this phase
- The Breeze Bow fires arrows normally (no special tipped arrow behavior)
- Tipped arrows are not blocked (unlike the Wooden Bow) — they just apply their effect on direct hit only

### Claude's Discretion
- Knockback strength and direction (spec says "regular arrow knockback")
- Class gating implementation (follow pattern from Phase 87 Blaze Bow)
- Texture style and pulling animation states
- Actionbar message wording for non-Support players

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. Follow Phase 87's patterns for item registration, textures, class gating, and draw speed modification.

</specifics>

<deferred>
## Deferred Ideas

- **Splash AoE tipped arrows** — Originally in this phase's spec. Tipped arrows fired from the Breeze Bow would create splash AoE on impact. Removed by user decision — to be implemented in a future phase.

</deferred>

---

*Phase: 88-breeze-bow*
*Context gathered: 2026-02-12*
