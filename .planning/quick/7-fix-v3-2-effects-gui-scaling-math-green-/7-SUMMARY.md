---
phase: quick-7
plan: 01
subsystem: ui
tags: [hud, blit, scaling, effects, fabric-api]

# Dependency graph
requires:
  - phase: 80-effects-gui
    provides: "EffectsHudRenderer with frame/icon/numeral rendering"
  - phase: 82-scaling-settings
    provides: "EffectsGuiConfig with scale percentage slider"
provides:
  - "Correct blit scaling at any config percentage"
  - "20% alpha green duration overlay"
  - "Infinite effect filtering"
  - "Vanilla HUD effects removal"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "12-param blit overload for decoupling render size from source UV region"
    - "Pre-render filter chain for effect visibility rules"

key-files:
  created: []
  modified:
    - src/client/kotlin/thc/client/EffectsHudRenderer.kt
    - src/client/kotlin/thc/THCClient.kt

key-decisions:
  - "Use 12-param blit overload to decouple render dimensions from source UV sampling"
  - "Filter infinite effects before render loop rather than special-casing in overlay"

patterns-established:
  - "12-param blit pattern: always pass explicit sourceWidth/sourceHeight when render size differs from texture size"

# Metrics
duration: 2min
completed: 2026-02-10
---

# Quick Task 7: Fix v3.2 Effects GUI Scaling/Overlay/Infinite/Vanilla HUD

**Fixed four effects HUD bugs: 12-param blit for correct UV scaling, 20% alpha overlay, infinite effect filter, vanilla HUD removal**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-10T03:55:24Z
- **Completed:** 2026-02-10T03:57:28Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- All three blit calls (frame, icon, numeral) switched to 12-param overload that decouples render size from source UV region -- fixes tiling at large scales and partial display at small scales
- Green duration overlay alpha reduced from 50% (0x80) to 20% (0x33) for subtle visual
- Infinite-duration effects (beacons, conduits) filtered out before render loop
- Vanilla status effects removed from top-right HUD via Fabric API; inventory GUI effects remain unaffected

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix all four effects GUI issues** - `209be5c` (fix)

## Files Created/Modified
- `src/client/kotlin/thc/client/EffectsHudRenderer.kt` - Fixed blit calls to 12-param, 20% overlay alpha, infinite filter, removed dead code
- `src/client/kotlin/thc/THCClient.kt` - Added HudElementRegistry.removeElement(VanillaHudElements.STATUS_EFFECTS)

## Decisions Made
- Used 12-param blit overload rather than pre-scaling source textures -- keeps textures at original resolution and lets GPU handle scaling
- Filtered infinite effects at the top of render() rather than special-casing in renderDurationOverlay() -- cleaner and removes dead code path

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Effects GUI is now functionally correct at all scale settings
- Ready for in-game visual verification

## Self-Check: PASSED
- [x] EffectsHudRenderer.kt exists
- [x] THCClient.kt exists
- [x] 7-SUMMARY.md exists
- [x] Commit 209be5c found in git log
