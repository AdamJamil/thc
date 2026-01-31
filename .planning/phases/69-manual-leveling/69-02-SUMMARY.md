---
phase: 69-manual-leveling
plan: 02
started: 2026-01-31T18:54:34Z
completed: 2026-01-31T18:59:00Z
status: complete
commits: 2
---

## Summary

Implemented manual villager level-up via emerald right-click with stage gates using UseEntityCallback.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Create VillagerInteraction.kt with UseEntityCallback handler | `d339a0b` | VillagerInteraction.kt, VillagerAccessor.java, thc.mixins.json |
| 2 | Register VillagerInteraction in THC.kt | `17b9b4e` | THC.kt |
| 3 | Verify complete integration | - | (verification only) |

## Deliverables

- `src/main/kotlin/thc/villager/VillagerInteraction.kt` - UseEntityCallback handler for emerald level-up:
  - Stage gate: targetLevel == requiredStage (Stage 2 for Apprentice, Stage 3 for Journeyman, etc.)
  - 0 XP returns PASS (reserved for Phase 70 cycling)
  - Emerald consumed only on successful level-up
  - HAPPY_VILLAGER particles and VILLAGER_YES sound on success
- `src/main/java/thc/mixin/access/VillagerAccessor.java` - Accessor mixin for tradingXp field read/write
- `src/main/resources/thc.mixins.json` - Registered VillagerAccessor
- `src/main/kotlin/thc/THC.kt` - VillagerInteraction.register() call

## Key Decisions

- VillagerAccessor mixin created to access private tradingXp field (accessor pattern used elsewhere in codebase)
- Registration placed before cow milking callback to ensure villager interactions processed first
- Messages displayed via actionbar (displayClientMessage with true parameter)

## Verification

- [x] Build passes: `./gradlew build` completed successfully
- [x] Registration exists: `VillagerInteraction.register()` in THC.kt
- [x] Stage check exists: `StageManager.getCurrentStage` in VillagerInteraction.kt
- [x] XP config used: `VillagerXpConfig.getMaxXpForLevel` in VillagerInteraction.kt

## Issues

None.
