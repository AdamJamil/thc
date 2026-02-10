---
phase: 82-scaling-settings
plan: 01
subsystem: ui
tags: [option-instance, mixin, video-settings, hud-scaling, config-persistence]

# Dependency graph
requires:
  - phase: 80-effects-hud
    provides: "EffectsHudRenderer with frame/icon/numeral rendering"
  - phase: 81-duration-overlay-and-numerals
    provides: "Duration overlay and roman numeral rendering in effects HUD"
provides:
  - "EffectsGuiConfig with OptionInstance slider (2-20% range) and file persistence"
  - "VideoSettingsScreen mixin injecting Effects GUI Scaling slider"
  - "Dynamic frame sizing in EffectsHudRenderer driven by config scale percent"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "OptionInstance<Int> with IntRange for discrete slider in Video Settings"
    - "Simple text file config persistence via FabricLoader.configDir"
    - "Ratio-based proportional scaling for HUD rendering (all sizes derived from frame size)"

key-files:
  created:
    - src/client/kotlin/thc/client/EffectsGuiConfig.kt
    - src/client/java/thc/mixin/client/VideoSettingsScreenMixin.java
  modified:
    - src/client/kotlin/thc/client/EffectsHudRenderer.kt
    - src/client/kotlin/thc/THCClient.kt
    - src/client/resources/thc.client.mixins.json
    - src/main/resources/assets/thc/lang/en_us.json

key-decisions:
  - "Ratio-based proportional scaling instead of hardcoded pixel values for dynamic HUD sizing"
  - "Simple single-line text file config (thc-effects-gui.txt) over JSON or TOML for minimal dependency"

patterns-established:
  - "OptionInstance IntRange slider with save-on-change callback pattern"
  - "VideoSettingsScreen mixin extending OptionsSubScreen to access protected list field"

# Metrics
duration: 5min
completed: 2026-02-10
---

# Phase 82 Plan 01: Scaling Settings Summary

**OptionInstance slider in Video Settings (2-20% range) with file persistence, driving proportional HUD frame sizing**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-10T03:12:00Z
- **Completed:** 2026-02-10T03:17:06Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- EffectsGuiConfig object with OptionInstance<Int> slider (2-20 range), load/save to config/thc-effects-gui.txt
- VideoSettingsScreenMixin injects "Effects GUI Scaling" slider at bottom of Video Settings
- EffectsHudRenderer refactored from hardcoded 44px constants to ratio-based proportional sizing driven by config

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EffectsGuiConfig with OptionInstance slider and file persistence** - `a5e024e` (feat)
2. **Task 2: Mixin VideoSettingsScreen to inject scaling slider and wire to renderer** - `2acf686` (feat)

## Files Created/Modified
- `src/client/kotlin/thc/client/EffectsGuiConfig.kt` - Config object with OptionInstance slider, getScalePercent() accessor, file persistence
- `src/client/java/thc/mixin/client/VideoSettingsScreenMixin.java` - Mixin injecting slider into Video Settings addOptions()
- `src/client/kotlin/thc/client/EffectsHudRenderer.kt` - Refactored to dynamic sizing via EffectsGuiConfig.getScalePercent()
- `src/client/kotlin/thc/THCClient.kt` - Added EffectsGuiConfig.load() call on client init
- `src/client/resources/thc.client.mixins.json` - Registered VideoSettingsScreenMixin
- `src/main/resources/assets/thc/lang/en_us.json` - Added "Effects GUI Scaling" translation key

## Decisions Made
- Used ratio-based proportional scaling: all render sizes (icon, numeral, offset, margin) derived from frame size using ratios from the original 44px design, so the HUD scales smoothly at any percentage
- Simple text file config (`effectsGuiScale:N`) instead of JSON/TOML for zero extra dependencies
- Mixin extends OptionsSubScreen to access protected `list` field directly rather than using @Shadow

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Scaling config system complete, slider visible in Video Settings
- Effects HUD renders at configurable size from 2% to 20% of screen width
- Config persists across game restarts

## Self-Check: PASSED

All 6 files verified present. Both task commits (a5e024e, 2acf686) verified in git log.

---
*Phase: 82-scaling-settings*
*Completed: 2026-02-10*
