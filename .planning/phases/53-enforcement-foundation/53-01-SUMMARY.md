---
phase: 53-enforcement-foundation
plan: 01
subsystem: enchant
tags: [enchantment, loot-table, mob-spawn, data-components]

# Dependency graph
requires:
  - phase: none
    provides: First v2.5 phase - no prior dependencies
provides:
  - EnchantmentEnforcement utility with REMOVED_ENCHANTMENTS set
  - INTERNAL_LEVELS map for enchantments with non-default levels
  - stripAndNormalize() function for enchantment cleaning
  - correctStack() function for ItemStack enchantment correction
  - Loot table filtering via LootTableEvents.MODIFY_DROPS integration
  - Mob equipment correction via MobFinalizeSpawnMixin
affects: [54-enchanting-controls, 55-display-fire-normalization, 56-villager-librarian-restrictions]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ItemEnchantments.Mutable builder pattern for modifying enchantments
    - DataComponents.ENCHANTMENTS and STORED_ENCHANTMENTS for item/book handling

key-files:
  created:
    - src/main/kotlin/thc/enchant/EnchantmentEnforcement.kt
    - src/main/java/thc/mixin/ItemEnchantmentsMixin.java
  modified:
    - src/main/kotlin/thc/THC.kt
    - src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use string IDs for enchantment comparison (e.g., 'minecraft:loyalty') instead of Holder references - more stable across registry reloads"
  - "Process ALL loot drops through enchantment correction, not just enchanted books"
  - "Equipment correction runs for ALL spawn reasons, not just NATURAL - ensures spawners/commands also get correct enchantments"

patterns-established:
  - "EnchantmentEnforcement.correctStack(stack) pattern for any future enchantment filtering needs"
  - "ItemEnchantments.Mutable builder pattern for modifying enchantments immutably"

# Metrics
duration: 17min
completed: 2026-01-27
---

# Phase 53 Plan 01: Enforcement Foundation Summary

**EnchantmentEnforcement utility strips 12 removed enchantments from loot/mob spawns and normalizes levels to internal values**

## Performance

- **Duration:** 17 min
- **Started:** 2026-01-27T21:45:06Z
- **Completed:** 2026-01-27T22:02:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Created EnchantmentEnforcement utility with REMOVED_ENCHANTMENTS set (12 enchantments)
- Implemented INTERNAL_LEVELS map for enchantments needing non-default levels
- Integrated enchantment correction into LootTableEvents.MODIFY_DROPS handler
- Added mob equipment correction at spawn time via MobFinalizeSpawnMixin

## Task Commits

Combined into single commit for cleaner history:

1. **All Tasks** - `e08a26f` (feat: enforce enchantment rules on loot and mob spawns)

## Files Created/Modified
- `src/main/kotlin/thc/enchant/EnchantmentEnforcement.kt` - Utility with REMOVED_ENCHANTMENTS, INTERNAL_LEVELS, stripAndNormalize(), correctStack()
- `src/main/kotlin/thc/THC.kt` - Added EnchantmentEnforcement.correctStack call in MODIFY_DROPS handler
- `src/main/java/thc/mixin/MobFinalizeSpawnMixin.java` - Added thc$correctEquipmentEnchantments method
- `src/main/java/thc/mixin/ItemEnchantmentsMixin.java` - Override addToTooltip to hide level suffixes (bonus LVL-01)
- `src/main/resources/thc.mixins.json` - Register ItemEnchantmentsMixin

## Decisions Made
- Used `holder.unwrapKey().orElse(null)?.identifier()?.toString()` for getting enchantment ID from Holder
- Handle both DataComponents.ENCHANTMENTS and STORED_ENCHANTMENTS to cover items and enchanted books
- Equipment correction runs as separate @Inject at TAIL, not inside the NATURAL-only spawn tagging method

## Deviations from Plan

### Bonus Work Included

**1. ItemEnchantmentsMixin (LVL-01 from Phase 55)**
- **What:** Added enchantment tooltip override to hide level suffixes
- **Reason:** Was included in working directory from prior incomplete session; cherry-pick brought it in
- **Impact:** Implements LVL-01 early (display enchantments without I/II/III suffix)
- **Files:** src/main/java/thc/mixin/ItemEnchantmentsMixin.java, thc.mixins.json
- **Result:** Beneficial - reduces work in future phase 55

## Issues Encountered
- Git HEAD became detached during execution due to external checkout; resolved by cherry-picking commits back to main branch
- Pre-existing untracked files (FlameIgniteMixin.java, etc.) from prior incomplete session caused build failures; removed them to proceed

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- EnchantmentEnforcement utility ready for use in phase 54 (enchanting controls)
- REMOVED_ENCHANTMENTS set can be referenced by enchanting table modifications
- Pattern established for any future enchantment filtering needs

---
*Phase: 53-enforcement-foundation*
*Completed: 2026-01-27*
