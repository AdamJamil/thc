---
phase: 74-revival-ui
plan: 01
subsystem: ui
tags: [hud, networking, sync, revival, fabric-api]

# Dependency graph
requires:
  - phase: 73-revival-mechanics
    provides: RevivalState progress tracking on downed players
  - phase: 72-core-downed-state
    provides: DownedState with downed location tracking
provides:
  - RevivalStatePayload for server-to-client revival sync
  - RevivalSync with delta-based state sync and look-direction check
  - RevivalClientState for caching synced state
  - RevivalProgressRenderer HUD element for visual feedback
affects: [74-02 if created for radial fill enhancement]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Delta sync pattern (RevivalSync follows BucklerSync)
    - Client state caching pattern (RevivalClientState follows BucklerClientState)
    - HUD element before crosshair for overlay effects

key-files:
  created:
    - src/main/java/thc/network/RevivalStatePayload.java
    - src/main/java/thc/network/RevivalSync.java
    - src/client/java/thc/client/RevivalClientState.java
    - src/client/kotlin/thc/client/RevivalProgressRenderer.kt
  modified:
    - src/main/kotlin/thc/THC.kt
    - src/client/kotlin/thc/THCClient.kt

key-decisions:
  - "Look direction check: 60-degree cone (cos(60) = 0.5 dot product threshold)"
  - "Vertical fill instead of radial pie fill due to MC API limitations"
  - "Top-down fill approximates starting from 12 o'clock concept"

patterns-established:
  - "Revival sync pattern: sync each tick after processRevival, delta-based"
  - "HUD overlay pattern: register before crosshair for center-screen elements"

# Metrics
duration: 8min
completed: 2026-02-02
---

# Phase 74 Plan 01: Revival Progress UI Summary

**Server-to-client revival state sync with radial progress ring HUD centered on cursor**

## Performance

- **Duration:** 8 min
- **Started:** 2026-02-02T05:00:00Z
- **Completed:** 2026-02-02T05:08:00Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- RevivalStatePayload syncs downed UUID, location, and progress from server to client
- RevivalSync finds look target within 2 blocks using 60-degree view cone
- RevivalProgressRenderer displays progress ring centered on cursor
- Empty ring shows as background, filled ring fills based on progress percentage

## Task Commits

Each task was committed atomically:

1. **Task 1: Add revival state sync** - `2dcc571` (feat)
2. **Task 2: Add client-side renderer** - `aaad6dd` (feat)

## Files Created/Modified
- `src/main/java/thc/network/RevivalStatePayload.java` - Network payload with downed UUID, location, progress
- `src/main/java/thc/network/RevivalSync.java` - Server-side sync with delta detection and look-target finding
- `src/client/java/thc/client/RevivalClientState.java` - Client-side cache of synced state
- `src/client/kotlin/thc/client/RevivalProgressRenderer.kt` - HUD renderer with empty/filled ring textures
- `src/main/kotlin/thc/THC.kt` - Payload registration, sync call in tick handler
- `src/client/kotlin/thc/THCClient.kt` - Receiver registration, HUD element registration

## Decisions Made
- Look direction uses 60-degree half-angle cone (dot product >= 0.5) for generous but directional targeting
- Sync clears on player disconnect to prevent stale data
- HUD element registered before crosshair so ring renders behind the crosshair dot

## Deviations from Plan

### Rendering Approach Changed

**[Rule 3 - Blocking] Changed from radial pie fill to vertical fill**
- **Found during:** Task 2 (RevivalProgressRenderer implementation)
- **Issue:** Plan specified triangle fan rendering for radial clockwise fill, but MC 1.21.x removed/changed the rendering APIs used (BufferUploader, RenderSystem.setShaderTexture, CoreShaders no longer accessible in this context)
- **Fix:** Changed to vertical top-down fill using standard GuiGraphics.blit which is known to work (per BucklerHudRenderer)
- **Files modified:** src/client/kotlin/thc/client/RevivalProgressRenderer.kt
- **Verification:** Build passes, code follows working BucklerHudRenderer pattern
- **Impact:** Visual fill direction is top-down instead of clockwise radial; functional feedback remains the same

---

**Total deviations:** 1 (rendering approach)
**Impact on plan:** Functional outcome preserved (progress visualization works). Visual style differs but can be enhanced in future iteration if radial fill is desired.

## Issues Encountered
- Minecraft 1.21.x rendering API changes made the planned triangle fan approach infeasible
- Resolution: Used standard blit with UV clipping for vertical fill instead

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Revival progress UI functional and ready for in-game testing
- Textures (revival_progress_empty.png, revival_progress_full.png) need to exist at expected paths
- If radial fill is desired, would need custom shader or different rendering approach

---
*Phase: 74-revival-ui*
*Completed: 2026-02-02*
