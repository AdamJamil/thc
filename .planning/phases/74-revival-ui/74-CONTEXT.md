# Phase 74: Revival UI - Context

**Gathered:** 2026-02-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Visual feedback showing revival progress to revivers. Radial progress ring appears around cursor when looking at a downed location within range. Uses existing texture assets for empty/filled ring states.

</domain>

<decisions>
## Implementation Decisions

### Ring position
- Centered directly on the crosshair (overlapping it)
- Initial scale TBD — will iterate visually to find the right size

### Fill behavior
- Radial fill clockwise from top (12 o'clock position)
- Progress fills smoothly as revival progresses

### Transitions
- Instant show/hide — no fade transitions
- Appears immediately when looking at downed location within 2 blocks
- Disappears immediately when revival completes or reviver moves away

### Textures
- Use existing texture assets (already created)
- Do not create new textures — render using provided images

### Claude's Discretion
- Exact initial scale value (user will iterate on this)
- Shader/rendering approach for radial fill
- How to determine "looking at downed location" (raycast approach)

</decisions>

<specifics>
## Specific Ideas

- Textures already exist in the spec — use those, don't create new ones
- Scale will be tuned iteratively after initial implementation

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 74-revival-ui*
*Context gathered: 2026-02-02*
