---
phase: 81-duration-overlay-and-numerals
plan: 01
subsystem: ui
tags: [hud, effects, duration-overlay, roman-numerals, gui-rendering]

# Dependency graph
requires:
  - phase: 80-core-hud-rendering
    provides: "EffectsHudRenderer with frame+icon per-effect render loop"
provides:
  - "Duration overlay rendering (green fill draining bottom-up proportional to remaining time)"
  - "Roman numeral rendering (II-X from spritesheet for amplifier >= 1)"
  - "Original duration tracking map for drain ratio calculation"
affects: [82-health-bar-hud]

# Tech tracking
tech-stack:
  added: []
  patterns: ["originalDurations mutableMap for tracking initial effect duration", "sub-tick interpolation via deltaTracker.getGameTimeDeltaPartialTick"]

key-files:
  created: []
  modified:
    - src/client/kotlin/thc/client/EffectsHudRenderer.kt

key-decisions:
  - "Used GuiGraphics.fill() with ARGB color for overlay (RenderPipelines.GUI supports alpha blending)"
  - "Track original durations in mutableMap keyed by effect registry name, reset when duration increases (effect refresh)"
  - "Sub-tick interpolation subtracts (1 - partialTick) from remaining ticks for smooth drain"

patterns-established:
  - "originalDurations map pattern: store on first-see or duration-increase, clean up via retainAll each frame"

# Metrics
duration: 5min
completed: 2026-02-10
---

# Phase 81 Plan 01: Duration Overlay and Numerals Summary

**Green 50% alpha duration overlay with bottom-up drain and roman numeral amplifier labels (II-X) from numerals.png spritesheet**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-10T02:53:40Z
- **Completed:** 2026-02-10T02:59:11Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Green semi-transparent overlay fills icon area from bottom, height proportional to remaining/original duration ratio
- Sub-tick interpolation using partialTick for visually smooth drain (no stepping)
- Roman numerals II-X rendered at (frameX+5, frameY+5) from 13x90 spritesheet for effects with amplifier >= 1
- originalDurations map tracks initial duration per effect, resets on refresh, cleaned up each frame
- Infinite effects show full 100% overlay height
- Amplifier 0 (level I) shows no numeral -- only II through X rendered

## Task Commits

Each task was committed atomically:

1. **Task 1: Add duration overlay and numeral rendering to EffectsHudRenderer** - `0fd11e4` (feat)

**Plan metadata:** `dbef6e3` (docs: complete plan)

## Files Created/Modified
- `src/client/kotlin/thc/client/EffectsHudRenderer.kt` - Added renderDurationOverlay() and renderAmplifierNumeral() methods, originalDurations tracking map, NUMERALS_TEXTURE constant, sub-tick interpolation

## Decisions Made
- Used simple `GuiGraphics.fill(x1, y1, x2, y2, color)` which delegates to RenderPipelines.GUI with alpha support, rather than a textured approach
- Tracked original durations in a mutableMap keyed by effect registry name string; resets when current duration exceeds stored (handles effect refresh)
- Sub-tick interpolation: `effectiveRemaining = remaining - (1 - partialTick)` clamped to >= 0 for smooth visual drain between ticks

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Effects HUD now has full duration overlay and numeral rendering
- Ready for Phase 82 (health bar HUD) or further effects GUI work
- In-game visual verification recommended: apply timed effects, check drain smoothness, verify numeral positioning

## Self-Check: PASSED

- FOUND: src/client/kotlin/thc/client/EffectsHudRenderer.kt
- FOUND: commit 0fd11e4
- FOUND: 81-01-SUMMARY.md

---
*Phase: 81-duration-overlay-and-numerals*
*Completed: 2026-02-10*
