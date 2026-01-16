---
phase: 03-base-area-permissions
plan: 01
subsystem: permissions
tags: [minecraft, fabric, events, combat, permissions, base-area]

# Dependency graph
requires:
  - phase: 02-01
    provides: "ClaimManager.isInBase() API for base area detection"
provides:
  - "BasePermissions object with combat restriction handlers"
  - "Attack blocking via AttackEntityCallback"
  - "Bow/crossbow blocking via UseItemCallback"
affects: [block-permissions, pvp-rules, future-permission-checks]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Fabric event callbacks for permission enforcement"
    - "ServerLevel cast pattern for server access"

key-files:
  created:
    - src/main/kotlin/thc/base/BasePermissions.kt
  modified:
    - src/main/kotlin/thc/THC.kt

key-decisions:
  - "Get server via level cast (world as ServerLevel).server instead of player.server (private)"
  - "Use hand parameter in UseItemCallback to get correct item being used"
  - "Return InteractionResult.PASS for non-applicable cases"
  - "Return InteractionResult.FAIL to block action"

patterns-established:
  - "BasePermissions singleton for permission enforcement"
  - "Event handler pattern with early returns for client/non-applicable cases"

# Metrics
duration: 6min
completed: 2026-01-16
---

# Phase 03 Plan 01: Combat Restrictions in Base Summary

**Combat restriction event handlers using Fabric events to enforce "No violence indoors!" in base areas**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-16
- **Completed:** 2026-01-16
- **Tasks:** 2
- **Files created:** 1
- **Files modified:** 1

## Accomplishments
- BasePermissions object created with register() function
- AttackEntityCallback blocks all entity attacks in base areas
- UseItemCallback blocks bow and crossbow use in base areas
- Red "No violence indoors!" message displayed on action bar when combat blocked
- BasePermissions registered in THC.onInitialize() after BellHandler.register()

## Task Commits

Each task was committed atomically:

1. **Task 1: Create BasePermissions with combat blocking handlers** - `bd0908a` (feat)
2. **Task 2: Register BasePermissions in mod initializer** - `4a05ef9` (feat)

## Files Created

- `src/main/kotlin/thc/base/BasePermissions.kt` - Singleton object with:
  - `register()` - Registers both event handlers
  - `registerAttackBlocking()` - AttackEntityCallback handler
  - `registerRangedWeaponBlocking()` - UseItemCallback handler
  - Both handlers call `ClaimManager.isInBase(server, player.blockPosition())`

## Files Modified

- `src/main/kotlin/thc/THC.kt`:
  - Added import for `thc.base.BasePermissions`
  - Added `BasePermissions.register()` call after `BellHandler.register()`

## Decisions Made

- **Server access pattern:** Used `(world as? ServerLevel)?.server` instead of `player.server` which is private in Minecraft 1.21.11
- **UseItemCallback return type:** Returns `InteractionResult` not `InteractionResultHolder` (differs from item.use())
- **Hand parameter usage:** UseItemCallback provides the hand being used, so we get the item via `player.getItemInHand(hand)` for correct behavior

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed server access pattern**
- **Found during:** Task 1
- **Issue:** Plan specified `player.server` but this field is private in ServerPlayer
- **Fix:** Cast world to ServerLevel and access `.server` from the level instead
- **Files modified:** BasePermissions.kt
- **Commit:** bd0908a

**2. [Rule 1 - Bug] Fixed UseItemCallback return type**
- **Found during:** Task 1
- **Issue:** Initial implementation used `InteractionResultHolder` which doesn't exist for UseItemCallback
- **Fix:** Changed to return `InteractionResult` directly (PASS or FAIL)
- **Files modified:** BasePermissions.kt
- **Commit:** bd0908a

## Issues Encountered

Initial compilation failed due to two API mismatches:
1. `serverPlayer.server` is private - resolved by using ServerLevel.server
2. `UseItemCallback` returns `InteractionResult` not `InteractionResultHolder` - resolved by checking Fabric API source

Both issues were resolved by referencing the actual Fabric API source from the gradle cache.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Combat restriction foundation complete:
- Players cannot attack while in base area
- Players cannot use ranged weapons while in base area
- Clear feedback message displayed to player
- Ready for additional permission checks (block breaking, placement, etc.)

**Blockers:** None

**Concerns:** None - in-game testing will verify behavior works correctly

---
*Phase: 03-base-area-permissions*
*Completed: 2026-01-16*
