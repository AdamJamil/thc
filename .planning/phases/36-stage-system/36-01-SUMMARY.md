# Phase 36 Plan 01: Stage System Data Foundation Summary

**Status:** Complete
**Duration:** 3m 42s
**Completed:** 2026-01-23

## One-Liner

Server-wide stage progression SavedData with per-player boon level attachment and StageManager CRUD utility.

## What Was Built

Created the persistent state infrastructure for the stage system:

1. **StageData.kt** - SavedData for server-wide stage tracking
   - Codec-based serialization with RecordCodecBuilder
   - Stage range enforcement (1-5) with advanceStage() validation
   - getServerState() accessor following ClaimData pattern
   - setDirty() called after modification for persistence

2. **BOON_LEVEL Attachment** - Per-player boon level tracking
   - Integer attachment with persistent(Codec.INT)
   - copyOnDeath() ensures survival through respawn
   - Initializer returns 0 for new players

3. **StageManager.java** - Static utility API
   - getCurrentStage(server) - Get server-wide stage
   - advanceStage(server) - Advance stage with player boon increment and broadcast
   - getBoonLevel(player) - Get player's boon level with null safety
   - incrementBoonLevel(player) - Increment player's boon by 1
   - setBoonLevel(player, level) - Set player's boon to specific value

## Key Implementation Details

### Stage Persistence
- Stage stored in overworld's DataStorage via computeIfAbsent(TYPE)
- Default stage is 1 (constructor parameter default)
- advanceStage() returns false at stage 5 (no further advancement)
- Codec handles automatic serialization/deserialization

### Boon Level Behavior
- Incremented for all online players when stage advances
- New players start with boon level 0 (attachment initializer)
- Late-joiner synchronization deferred to command implementation (36-02)
- Persists across death via copyOnDeath() flag

### Broadcast Mechanism
- Red actionbar message to all online players
- Format: "Trial complete. The world has advanced to Stage X."
- Uses server.getPlayerList().getPlayers() iteration
- displayClientMessage(message, true) for actionbar display

## Files Created

- `src/main/kotlin/thc/stage/StageData.kt` (47 lines)
- `src/main/java/thc/stage/StageManager.java` (73 lines)

## Files Modified

- `src/main/java/thc/THCAttachments.java` - Added BOON_LEVEL attachment

## Commits

1. `8221c44` - feat(36-01): add StageData SavedData and BOON_LEVEL attachment
2. `1bc5d83` - feat(36-01): add StageManager static utility

## Testing

**Compilation:** ✅ Pass
- `./gradlew compileJava compileKotlin` - Success
- `./gradlew build` - Success

**Code Verification:** ✅ Pass
- thc_stage found in StageData.kt
- BOON_LEVEL found in THCAttachments.java and StageManager references
- StageData.getServerState called by StageManager

**Smoke Test:** ⚠️ Blocked by PlayerSleepMixin breakage
- Pre-existing issue from MC 1.21.11 upgrade (documented in STATE.md)
- Not related to stage system implementation
- Stage system code compiles and follows all established patterns

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

None - all implementation details specified in plan.

## Dependencies

**Requires:**
- Phase 35 (Class System) - PLAYER_CLASS attachment pattern established
- ClaimData SavedData pattern
- ThreatManager/ClassManager static utility patterns

**Provides:**
- Stage persistence infrastructure for /advanceStage command (36-02)
- Boon level scaffolding for future class-specific boon effects

**Affects:**
- Phase 36-02 (Advance Stage Command) - Will use StageManager API
- Future phases implementing boon effects

## Next Phase Readiness

**Ready for 36-02:** ✅
- StageManager.advanceStage() ready for command integration
- StageManager.getCurrentStage() available for validation
- Late-joiner boon sync logic can use StageManager.setBoonLevel()

**Blockers:** None for stage system development
- PlayerSleepMixin issue blocks in-game testing but not compilation/development

**Concerns:** None

## Metadata

**Phase:** 36-stage-system
**Plan:** 01
**Type:** foundation
**Wave:** 1
**Subsystem:** stage-persistence
**Tags:** SavedData, Codec, Attachment, CRUD, server-state, player-state

**Tech Stack:**
- Added: SavedDataType with Codec pattern (stage system)
- Added: Integer attachment with copyOnDeath (boon level)

**Patterns Established:**
- Static utility manager for dual state management (server-wide + per-player)
- Actionbar broadcast pattern for server-wide announcements
- Null-safe attachment getter with default value return
