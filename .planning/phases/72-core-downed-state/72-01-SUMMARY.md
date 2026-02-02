---
phase: 72
plan: 01
subsystem: downed-state
tags: [death-interception, spectator-mode, tether, fabric-api, attachment]
dependency-graph:
  requires: []
  provides: [downed-location-attachment, downed-state-accessor, death-interception, tether-enforcement]
  affects: [73-revival-mechanics, 74-revival-ui]
tech-stack:
  added: []
  patterns: [fabric-api-events, non-persistent-attachment, tick-hook-enforcement]
key-files:
  created:
    - src/main/java/thc/downed/DownedState.java
    - src/main/java/thc/downed/DownedManager.java
  modified:
    - src/main/java/thc/THCAttachments.java
    - src/main/java/thc/mixin/ServerPlayerMixin.java
    - src/main/kotlin/thc/THC.kt
decisions: []
metrics:
  duration: 7min
  completed: 2026-02-02
---

# Phase 72 Plan 01: Core Downed State Summary

**One-liner:** Death interception via Fabric API ALLOW_DEATH event, switching to spectator mode with 50-block tether enforcement via tick hook.

## What Was Built

### DOWNED_LOCATION Attachment (THCAttachments.java)
- Non-persistent Vec3 attachment for tracking downed position
- Null when player is not downed
- Session-scoped: resets on server restart (intentional - downed players shouldn't stay stuck)

### DownedState Accessor (DownedState.java)
- Static utility class following BucklerState pattern
- Methods: `getDownedLocation`, `setDownedLocation`, `isDowned`, `clearDowned`
- `isDowned` checks both location presence AND spectator mode (prevents false positives)

### Death Interception (DownedManager.java)
- Registers `ServerLivingEntityEvents.ALLOW_DEATH` callback
- On player death:
  1. Stores exact death position (Vec3)
  2. Switches to spectator mode (immediate invulnerability)
  3. Records downed location in attachment
  4. Returns false to cancel death

### Tether Enforcement (ServerPlayerMixin.java)
- New tick injection: `thc$enforceTether`
- Checks if player has downed location set
- If distance > 50 blocks, teleports back to downed location
- Uses squared distance comparison (2500.0) for performance

## Technical Decisions

| Decision | Rationale |
|----------|-----------|
| Fabric API event over mixin | ALLOW_DEATH is purpose-built for this; more stable than LivingEntity.die() mixin |
| Vec3 over BlockPos | Preserves sub-block precision; prevents teleport-into-block issues |
| Non-persistent attachment | Downed state should not survive restart; players would be permanently stuck |
| Spectator mode first, then store | Prevents death during teleport in void/lava edge cases |
| Squared distance check | Avoids sqrt until actually needed; 2500 = 50^2 |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Performed

1. `./gradlew compileJava` - passed (Task 1)
2. `./gradlew build` - passed (Task 2)
3. Code inspection confirmed all key links:
   - DOWNED_LOCATION in THCAttachments
   - ALLOW_DEATH.register in DownedManager
   - setDownedLocation call in DownedManager
   - getDownedLocation call in ServerPlayerMixin
   - DownedManager.register() call in THC.kt

## Commits

| Hash | Message |
|------|---------|
| 95fab07 | feat(72-01): add DOWNED_LOCATION attachment and DownedState accessor |
| fb8eeed | feat(72-01): implement death interception and tether enforcement |

## Next Phase Readiness

**Phase 73 (Revival Mechanics) dependencies satisfied:**
- DownedState.isDowned() available for detecting downed players
- DownedState.clearDowned() ready for revival completion
- DownedState.getDownedLocation() available for revival position

**No blockers identified.**

---
*Completed: 2026-02-02 in ~7 minutes*
