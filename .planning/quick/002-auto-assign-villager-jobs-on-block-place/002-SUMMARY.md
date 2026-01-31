---
type: quick-summary
id: "002"
description: Auto-assign villager jobs on block place
completed: 2026-01-31
duration: ~4 min
tasks: 3/3
---

# Quick Task 002: Auto-assign Villager Jobs on Block Place

**One-liner:** UseBlockCallback handler auto-assigns nearest unemployed villager when placing stonecutter/smoker/cartography table/lectern.

## What Was Built

When a player places one of the 4 allowed job blocks, the nearest unemployed villager (profession = NONE) within 5 blocks is automatically assigned the corresponding profession:
- Stonecutter -> Mason
- Smoker -> Butcher
- Cartography Table -> Cartographer
- Lectern -> Librarian

Visual and audio feedback (HAPPY_VILLAGER particles + VILLAGER_YES sound) confirms the assignment.

## Files Modified

| File | Change |
|------|--------|
| `src/main/java/thc/villager/AllowedProfessions.java` | Added JOB_BLOCK_TO_PROFESSION map and getProfessionForJobBlock() method |
| `src/main/kotlin/thc/villager/JobBlockAssignment.kt` | New file - UseBlockCallback handler for auto-assignment |
| `src/main/kotlin/thc/THC.kt` | Register JobBlockAssignment.register() in onInitialize() |

## Commits

| Hash | Message |
|------|---------|
| 4165f11 | feat(002): add job block to profession mapping |
| d4ea614 | feat(002): create job block placement handler |
| e170d42 | feat(002): register JobBlockAssignment in mod init |

## Deviations from Plan

None - plan executed exactly as written.

## Verification

- `./gradlew build` passes
- In-game: Place stonecutter near unemployed villager, verify villager becomes mason with particles/sound
