---
phase: 84-mob-effects-display
plan: 01
subsystem: ui
tags: [world-rendering, mob-effects, status-icons, billboard-quads, kotlin]

# Dependency graph
requires:
  - phase: 80-core-hud-rendering
    provides: "EffectsHudRenderer with priority map, comparator, and shared constants"
  - phase: 81-duration-overlay-and-numerals
    provides: "Duration overlay drain logic and numeral spritesheet rendering pattern"
  - phase: 83-health-bar-rendering
    provides: "MobHealthBarRenderer with world-space billboard quad infrastructure"
provides:
  - "Status effect icons rendered above mob health bars in world-space"
  - "Shared internal visibility for EffectsHudRenderer constants (FRAME_TEXTURE, NUMERALS_TEXTURE, comparator, etc.)"
  - "Per-mob duration tracking with sub-tick interpolation for smooth drain"
affects: [85-health-bar-config]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "World-space effect icon rendering via 4-layer textured quads (frame, icon, overlay, numeral)"
    - "Per-entity duration tracking keyed by entityId + effect name"
    - "Shared constants exposed as internal visibility for cross-renderer reuse"

key-files:
  created: []
  modified:
    - src/client/kotlin/thc/client/EffectsHudRenderer.kt
    - src/client/kotlin/thc/client/MobHealthBarRenderer.kt

key-decisions:
  - "Exposed EffectsHudRenderer constants as internal (not public) for module-scoped reuse"
  - "Used vertex-colored quad with center UV sampling for duration overlay instead of separate white texture"
  - "Frame world-size derived from BAR_HEIGHT * (44/64) ratio to match health bar proportions"
  - "Duration tracking uses entityId-keyed outer map with effect-name-keyed inner map, cleaned per frame"

patterns-established:
  - "World-space effect icon quad: frame + icon + overlay + numeral layers at consistent z-offsets"
  - "Per-mob originalDurations map with retainAll cleanup matching HUD renderer pattern"

# Metrics
duration: 4min
completed: 2026-02-11
---

# Phase 84 Plan 01: Mob Effects Display Summary

**World-space status effect icons above mob health bars with frame/icon/overlay/numeral layers, priority sorting, and sub-tick duration drain**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-11T04:37:37Z
- **Completed:** 2026-02-11T04:41:23Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Extracted shared effect rendering constants from EffectsHudRenderer as internal visibility for cross-renderer access
- Added renderEffects() to MobHealthBarRenderer with 4-layer world-space quads per effect icon (frame, vanilla icon, green duration overlay, roman numeral)
- Effects sorted by shared effectComparator (Wither > Poison > Resistance > etc.), centered left-to-right above health bar
- Duration overlay drains bottom-up with sub-tick interpolation matching player effects GUI behavior
- Per-mob duration tracking with automatic cleanup for despawned/out-of-range mobs

## Task Commits

Each task was committed atomically:

1. **Task 1: Extract shared effect rendering constants from EffectsHudRenderer** - `3dafa6a` (refactor)
2. **Task 2: Add effect icon rendering above mob health bars** - `1756a4c` (feat)

## Files Created/Modified
- `src/client/kotlin/thc/client/EffectsHudRenderer.kt` - Changed 14 constants/vals from private to internal for cross-renderer access
- `src/client/kotlin/thc/client/MobHealthBarRenderer.kt` - Added renderEffects(), computeDurationRatio(), renderColoredQuad(), per-mob duration tracking, effect filtering and sorting

## Decisions Made
- Exposed constants as `internal` (Kotlin module visibility) rather than `public` to limit API surface to the thc module
- Used vertex coloring on a center-sampled texture quad for the duration overlay color, avoiding need for a dedicated 1x1 white pixel texture
- Frame world-size calculated as `BAR_HEIGHT * (44/64)` to maintain proportional sizing relative to the health bar
- Duration tracking uses a two-level map (entityId -> effectName -> originalDuration) with retainAll cleanup each frame, matching the pattern established in EffectsHudRenderer

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Mob effect icons render above health bars, ready for in-game visual verification
- Phase 85 (health bar configuration) can add settings to toggle/scale effect display
- Effect rendering shares billboard pose stack with health bar, so any Phase 83 positioning changes propagate automatically

## Self-Check: PASSED

- [x] EffectsHudRenderer.kt exists
- [x] MobHealthBarRenderer.kt exists
- [x] 84-01-SUMMARY.md exists
- [x] Commit 3dafa6a found
- [x] Commit 1756a4c found
- [x] Build compiles (compileClientKotlin successful)

---
*Phase: 84-mob-effects-display*
*Completed: 2026-02-11*
