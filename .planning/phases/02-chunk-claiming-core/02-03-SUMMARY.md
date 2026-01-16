---
phase: 02-chunk-claiming-core
plan: 03
subsystem: item-use
tags: [minecraft, fabric, item, claiming, validation, player-feedback]

# Dependency graph
requires:
  - phase: 02-01
    provides: "ClaimManager API for claim registration and query"
  - phase: 02-02
    provides: "ChunkValidator for terrain and village validation"
provides:
  - "LandPlotItem.useOn with complete claiming flow"
  - "Player feedback via action bar messages"
  - "Base floor Y calculation on successful claim"
affects: [claim-removal, claim-display, permission-checks]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "UseOnContext for block-targeted item use"
    - "Action bar messages via displayClientMessage with ChatFormatting"
    - "Exhaustive when() for sealed class handling"

key-files:
  created: []
  modified:
    - src/main/kotlin/thc/item/LandPlotItem.kt

key-decisions:
  - "Return SUCCESS on client side to show arm swing animation"
  - "Use action bar (true) for messages - less intrusive than chat"
  - "Validate in order: claimed -> village -> terrain (fail fast)"

patterns-established:
  - "Item use validation chain pattern"
  - "Action bar feedback for item operations"

# Metrics
duration: 3min
completed: 2026-01-16
---

# Phase 02 Plan 03: Land Plot Use Behavior Summary

**LandPlotItem.useOn implementation with validation chain, claim registration, and player feedback via action bar messages**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-16T05:10:06Z
- **Completed:** 2026-01-16T05:13:13Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- LandPlotItem overrides useOn() for block-targeted claiming
- Validates in order: already claimed, village chunk, terrain flatness
- Calculates base floor Y as lowestSurfaceY - 10 per CLAIM-07
- Registers claim via ClaimManager.addClaim
- Consumes item only on successful claim (CLAIM-06)
- Displays colored action bar messages for all outcomes

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement LandPlotItem.useOn with validation and claiming** - `cf29507` (feat)
2. **Task 2: Verify claiming flow integration** - (verification only, no changes needed)

## Files Modified

- `src/main/kotlin/thc/item/LandPlotItem.kt` - Enhanced from stub to full implementation:
  - `useOn(context: UseOnContext)` - complete claiming flow
  - `sendFailureMessage(player, message)` - red action bar message
  - `sendSuccessMessage(player, chunkPos)` - green action bar message
  - Imports for ClaimManager, ChunkValidator, ValidationResult

## Requirements Implemented

| Requirement | Description | Implementation |
|-------------|-------------|----------------|
| CLAIM-01 | Chunk registered as claimed | ClaimManager.addClaim(server, chunkPos, baseFloorY) |
| CLAIM-02/03 | Terrain flatness validation | ChunkValidator.validateTerrain check |
| CLAIM-04 | Village chunk rejection | ChunkValidator.isVillageChunk check |
| CLAIM-05 | Already claimed rejection | ClaimManager.isClaimed check |
| CLAIM-06 | Item consumed on success | context.itemInHand.shrink(1) after addClaim |
| CLAIM-07 | Base floor Y calculation | baseFloorY = lowestSurfaceY - 10 |

## Messages

- Already claimed: "This chunk is already claimed!" (red)
- Village chunk: "Cannot claim village chunks!" (red)
- Terrain uneven: "The chunk's surface is not flat enough!" (red)
- Success: "Claimed chunk at (X, Z)!" (green)

## Decisions Made

- **Return SUCCESS on client side**: Shows arm swing animation when using item, even though claim logic runs server-side only
- **Action bar for messages**: Less intrusive than chat, appropriate for quick feedback
- **Validation order (claimed->village->terrain)**: Fail fast on simple checks before expensive terrain scan
- **Exhaustive when() on ValidationResult**: Kotlin compiler enforces handling all sealed class variants

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - APIs worked as expected with prior plans providing tested components.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Chunk claiming core complete:
- Land plot books drop from bell activation (Phase 01)
- Chunks validate and store correctly (Phase 02-01, 02-02)
- Using land plot book claims chunk with full validation (Phase 02-03)
- Ready for Phase 03: claim visualization and management

**Blockers:** None

**Concerns:** None - in-game testing will verify complete flow works end-to-end

---
*Phase: 02-chunk-claiming-core*
*Completed: 2026-01-16*
