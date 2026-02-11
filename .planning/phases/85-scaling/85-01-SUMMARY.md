---
phase: 85-scaling
plan: 01
subsystem: ui
tags: [minecraft, fabric, mixin, optioninstance, config, video-settings]

# Dependency graph
requires:
  - phase: 82-effects-scaling
    provides: "EffectsGuiConfig pattern (OptionInstance slider + file persistence)"
provides:
  - "MobHealthBarConfig with OptionInstance slider for health bar scaling (2-20%)"
  - "Video Settings integration with both Effects GUI and Mob Health Bar sliders"
  - "MobHealthBarConfig.getScalePercent() accessor for renderer integration"
affects: [83-health-bar-rendering, 84-effects-mob-scaling]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Config object per feature with OptionInstance slider and file persistence"]

key-files:
  created:
    - src/client/kotlin/thc/client/MobHealthBarConfig.kt
  modified:
    - src/client/java/thc/mixin/client/VideoSettingsScreenMixin.java
    - src/client/kotlin/thc/THCClient.kt
    - src/main/resources/assets/thc/lang/en_us.json

key-decisions:
  - "Default scale 6% (slightly smaller than Effects GUI 8% since health bars are simpler)"
  - "Renamed mixin method from thc$addEffectsGuiScale to thc$addTHCOptions for multi-slider clarity"

patterns-established:
  - "Per-feature config objects: MobHealthBarConfig follows same OptionInstance+file pattern as EffectsGuiConfig"

# Metrics
duration: 3min
completed: 2026-02-11
---

# Phase 85 Plan 01: Scaling Summary

**MobHealthBarConfig with OptionInstance slider (2-20%) in Video Settings, file-persisted to thc-mob-health-bar.txt**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-11T04:36:50Z
- **Completed:** 2026-02-11T04:40:18Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- MobHealthBarConfig object with OptionInstance slider (range 2-20%, default 6%)
- File persistence to config/thc-mob-health-bar.txt with load/save on change
- Video Settings screen shows both "Effects GUI Scaling" and "Mob Health Bar" sliders
- MobHealthBarConfig.getScalePercent() accessor ready for Phases 83-84 renderer integration

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MobHealthBarConfig with OptionInstance slider and file persistence** - `ad3b074` (feat)
2. **Task 2: Extend VideoSettingsScreenMixin to add Mob Health Bar slider** - `6c54455` (feat)

## Files Created/Modified
- `src/client/kotlin/thc/client/MobHealthBarConfig.kt` - Config object with OptionInstance slider, getScalePercent() accessor, file persistence
- `src/client/java/thc/mixin/client/VideoSettingsScreenMixin.java` - Extended to inject both THC sliders into Video Settings
- `src/client/kotlin/thc/THCClient.kt` - Added MobHealthBarConfig.load() call on client init
- `src/main/resources/assets/thc/lang/en_us.json` - Added "thc.options.mobHealthBarScale" translation key

## Decisions Made
- Default scale set to 6% (smaller than Effects GUI's 8% since health bars are simpler visual elements)
- Renamed mixin method from `thc$addEffectsGuiScale` to `thc$addTHCOptions` since it now handles multiple THC sliders

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- MobHealthBarConfig.getScalePercent() is callable from health bar and mob effects renderers
- Phases 83 (health bar rendering) and 84 (effects mob scaling) can integrate this scale value into their renderers

## Self-Check: PASSED

All files verified present. All commits verified in git log.

---
*Phase: 85-scaling*
*Completed: 2026-02-11*
