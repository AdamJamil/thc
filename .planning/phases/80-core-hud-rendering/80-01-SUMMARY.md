---
phase: 80-core-hud-rendering
plan: 01
subsystem: ui
tags: [hud, fabric, mob-effects, gui-rendering, kotlin]

# Dependency graph
requires: []
provides:
  - "EffectsHudRenderer - renders active status effects in bottom-left HUD with priority sorting"
  - "HUD element registration pattern for effects overlay via Fabric HudElementRegistry"
affects: [81-health-hud-rendering]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Priority-sorted effect rendering with ResourceKey-based comparator"
    - "Vanilla mob effect icon texture path derivation from registry key"

key-files:
  created:
    - src/client/kotlin/thc/client/EffectsHudRenderer.kt
  modified:
    - src/client/kotlin/thc/THCClient.kt

key-decisions:
  - "Used ResourceKey<MobEffect> for priority map keys via unwrapKey() for stable identity comparison"
  - "Attached HUD element after VanillaHudElements.CHAT for render order (position is absolute, not relative)"
  - "Vanilla mob effect icons sourced at textures/mob_effect/{path}.png and rendered at 2x scale (18x18 -> 36x36)"

patterns-established:
  - "Effects HUD rendering: collect activeEffects, sort by priority comparator, render frame + icon per effect"

# Metrics
duration: 5min
completed: 2026-02-10
---

# Phase 80 Plan 01: Core HUD Rendering Summary

**Effects HUD renderer with priority-sorted status effect icons in 44x44 frames stacked vertically from bottom-left**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-10T02:34:23Z
- **Completed:** 2026-02-10T02:39:10Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Created EffectsHudRenderer with hardcoded priority sorting (Wither > Poison > Resistance > Absorption > Strength > Slowness > Weakness > Speed > others)
- Renders each active effect inside a 44x44 frame with vanilla mob effect icon at 2x scale (36x36) centered with 4px border
- Effects stack vertically upward from bottom-left with zero gaps between frames
- Registered as Fabric HUD element via HudElementRegistry in THCClient

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EffectsHudRenderer with priority sorting and frame+icon rendering** - `fd7c0fb` (feat)
2. **Task 2: Register EffectsHudRenderer in THCClient** - `82b9342` (feat)

## Files Created/Modified
- `src/client/kotlin/thc/client/EffectsHudRenderer.kt` - Effects HUD renderer object with priority map, comparator, and frame+icon rendering
- `src/client/kotlin/thc/THCClient.kt` - Added EffectsHudRenderer import and HudElementRegistry registration

## Decisions Made
- Used `ResourceKey<MobEffect>` as priority map keys (extracted via `holder.unwrapKey()`) for stable identity comparison rather than comparing Holder references directly
- Attached after `VanillaHudElements.CHAT` for render order -- since the effects overlay uses absolute positioning (bottom-left), the attachment point only affects layering order, not position
- Derived vanilla icon textures via `textures/mob_effect/{path}.png` pattern from the effect's registry key location

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- EffectsHudRenderer is registered and rendering -- ready for in-game visual verification
- Health HUD rendering (phase 81) can proceed independently
- Future enhancements (duration display, blinking, etc.) can extend EffectsHudRenderer

## Self-Check: PASSED

- [x] EffectsHudRenderer.kt exists
- [x] THCClient.kt exists and modified
- [x] Commit fd7c0fb found
- [x] Commit 82b9342 found
- [x] Build succeeds

---
*Phase: 80-core-hud-rendering*
*Completed: 2026-02-10*
