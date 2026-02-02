---
phase: 73-revival-mechanics
plan: 02
subsystem: downed
tags: [revival, completion, particles, gamemode, health, food]

dependency_graph:
  requires: [73-01]
  provides: [revival-completion, player-restoration]
  affects: [74-revival-ui]

tech_stack:
  added: []
  patterns: [tick-processor-completion-check, state-clearing-cascade]

key_files:
  created: []
  modified:
    - src/main/java/thc/downed/DownedState.java
    - src/main/kotlin/thc/THC.kt

decisions:
  - id: state-clear-cascade
    choice: "clearDowned calls RevivalState.clearProgress"
    rationale: "Single method clears all related state, prevents orphaned data"
  - id: separate-completion-pass
    choice: "Completion check in separate loop after progress accumulation"
    rationale: "Avoids concurrent modification of downedPlayers list"
  - id: particle-type
    choice: "HAPPY_VILLAGER particles (30 count, 0.5 spread)"
    rationale: "Green particles as specified in CONTEXT.md, visible feedback"

metrics:
  duration: 4 min
  completed: 2026-02-02
---

# Phase 73 Plan 02: Revival Completion Summary

**One-liner:** Revival completion restores player to survival mode with 50% HP, 6 hunger, and green particles when progress reaches 100%.

## What Was Built

1. **DownedState.clearDowned cascade** - Now clears both downed location and revival progress in single call
2. **completeRevival function** - Handles all state restoration when revival completes
3. **Completion check in tick processor** - Separate pass checks for progress >= 1.0 and triggers completion

## Technical Details

### DownedState.clearDowned Update
```java
public static void clearDowned(ServerPlayer player) {
    // Clear downed location
    target(player).setAttached(THCAttachments.DOWNED_LOCATION, null);
    // Also clear revival progress
    RevivalState.clearProgress(player);
}
```

### completeRevival Function
- Clears downed state (cascades to clear revival progress)
- Sets game mode to SURVIVAL
- Teleports player to exact downed location
- Sets health to 50% of max (`maxHealth * 0.5f`)
- Sets food level to 6 (per CONTEXT.md override, not 0)
- Spawns 30 HAPPY_VILLAGER particles at revival location

### Tick Processor Completion Check
```kotlin
// Check for revival completion (separate pass to avoid concurrent modification)
for (downed in downedPlayers) {
    if (RevivalState.getProgress(downed) >= 1.0) {
        val downedLoc = DownedState.getDownedLocation(downed)
        if (downedLoc != null) {
            completeRevival(downed, downedLoc)
        }
    }
}
```

## Commits

| Hash | Description |
|------|-------------|
| 2a0ff6b | feat(73-02): update clearDowned to also clear revival progress |
| abe96b2 | feat(73-02): add revival completion logic |

## Deviations from Plan

None - plan executed exactly as written.

## Tests

Build verification only (no automated tests). Manual testing required:
1. Down a player (take lethal damage)
2. Revive with another player (sneak within 2 blocks)
3. Verify: survival mode, at downed location, 50% HP, 6 food, green particles

## Next Phase Readiness

Phase 73 complete. Revival mechanics foundation ready for:
- **Phase 74:** Revival UI - Client-side progress bar and visual indicators

---

*Phase: 73-revival-mechanics*
*Plan: 02 of 02*
*Completed: 2026-02-02*
