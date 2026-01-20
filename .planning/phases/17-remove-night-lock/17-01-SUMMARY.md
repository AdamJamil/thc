# Phase 17 Plan 01: Remove Night Lock Summary

Deleted night-lock code from THC.kt so server time advances naturally (doDaylightCycle not forced to false).

## What Was Built

1. **Removed night lock system** - Deleted NIGHT_TIME constant, lockWorldToNight function, and simplified SERVER_STARTED handler
2. **Preserved MOB_GRIEFING** - Phase 16 feature (creeper explosion blocking) maintained in server started handler

## Changes Made

### Files Modified

| File | Change |
|------|--------|
| `src/main/kotlin/thc/THC.kt` | Removed NIGHT_TIME constant, lockWorldToNight function, simplified SERVER_STARTED handler |

### Key Implementation Details

- Removed `private const val NIGHT_TIME = 18000L` constant
- Removed `lockWorldToNight` function that was setting `ADVANCE_TIME` to false, `MOB_GRIEFING` to false, and `dayTime` to NIGHT_TIME
- Simplified `SERVER_STARTED` handler to only set `MOB_GRIEFING` to false (preserving Phase 16 feature)
- Server time now flows naturally since doDaylightCycle is no longer disabled

## Verification Results

- Build: PASSED
- No `NIGHT_TIME`, `lockWorldToNight`, `ADVANCE_TIME`, or `dayTime` references remain: VERIFIED
- `MOB_GRIEFING` gamerule still set to false: VERIFIED

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Keep MOB_GRIEFING in server started handler | Phase 16 feature must be preserved |

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Message |
|------|---------|
| 9cdd772 | feat(17-01): remove night lock system |

## Requirements Coverage

| Requirement | Status | Verified By |
|-------------|--------|-------------|
| TIME-01: Server time flows normally | MET | doDaylightCycle not forced to false |
| TIME-02: Night-lock code removed | MET | grep confirms no NIGHT_TIME/lockWorldToNight/ADVANCE_TIME/dayTime references |

## Duration

~2 minutes
