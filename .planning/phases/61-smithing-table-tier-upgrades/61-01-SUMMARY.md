---
phase: 61-smithing-table-tier-upgrades
plan: 01
subsystem: smithing
tags: [smithing-table, armor-upgrades, mixin, tier-progression, enchantment-preservation]

# Dependency graph
requires:
  - phase: 28-copper-armor
    provides: Copper armor items for tier progression
provides:
  - SmithingMenuMixin for intercepting tier upgrades
  - TierUpgradeConfig for material count validation
  - 12 armor tier upgrade recipes (leather->copper->iron->diamond)
  - Enchantment preservation during upgrades
  - Durability restoration on tier upgrade
affects: [62-tool-tier-upgrades, smithing-upgrades]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - SmithingMenu HEAD injection for recipe result override
    - onTake RETURN injection for extra material consumption
    - DataComponents.DAMAGE removal for durability restoration
    - Component copying for enchantment preservation

key-files:
  created:
    - src/main/kotlin/thc/smithing/TierUpgradeConfig.kt
    - src/main/java/thc/mixin/SmithingMenuMixin.java
    - src/main/resources/data/thc/recipe/leather_to_copper_*.json (4 files)
    - src/main/resources/data/thc/recipe/copper_to_iron_*.json (4 files)
    - src/main/resources/data/thc/recipe/iron_to_diamond_*.json (4 files)
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Barrier block as dummy template in recipes - mixin bypasses template requirement for tier upgrades"
  - "Material counts match vanilla crafting costs (5/8/7/4 for helmet/chest/legs/boots)"
  - "Component copying preserves all enchantments automatically"
  - "DAMAGE component removal restores durability to maximum"

patterns-established:
  - "SmithingMenu interception pattern: HEAD for result validation, RETURN for extra consumption"
  - "TierUpgradeConfig object pattern for upgrade mappings reusable for tools"

# Metrics
duration: 4min
completed: 2026-01-29
---

# Phase 61 Plan 01: Smithing Table Tier Upgrades Summary

**SmithingMenu mixin infrastructure with armor tier upgrades (leather->copper->iron->diamond) preserving enchantments and restoring durability**

## Performance

- **Duration:** 4 min 12 sec
- **Started:** 2026-01-29T19:37:44Z
- **Completed:** 2026-01-29T19:41:56Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- SmithingMenu interception validates material counts and consumes correct amounts
- 12 armor tier upgrade recipes enable leather->copper->iron->diamond progression
- Enchantments automatically preserved via component copying
- Durability restored to maximum on tier upgrade
- Infrastructure reusable for tool tier upgrades in phase 62

## Task Commits

Each task was committed atomically:

1. **Task 1: Create TierUpgradeConfig and SmithingMenuMixin** - `7577e5e` (feat)
2. **Task 2: Create Armor Tier Upgrade Recipes** - `8460889` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/smithing/TierUpgradeConfig.kt` - Material count mappings and upgrade path validation
- `src/main/java/thc/mixin/SmithingMenuMixin.java` - SmithingMenu interception for count validation and extra consumption
- `src/main/resources/thc.mixins.json` - Registered SmithingMenuMixin
- `src/main/resources/data/thc/recipe/leather_to_copper_*.json` - 4 recipes for leather to copper upgrades
- `src/main/resources/data/thc/recipe/copper_to_iron_*.json` - 4 recipes for copper to iron upgrades
- `src/main/resources/data/thc/recipe/iron_to_diamond_*.json` - 4 recipes for iron to diamond upgrades

## Decisions Made

**SMTH-BARRIER-01:** Barrier block as dummy template in recipes
- **Rationale:** Smithing table UI requires template slot, but tier upgrades only need base + addition validation. Mixin checks upgrade validity independently.

**SMTH-COUNT-01:** Material counts match vanilla crafting costs
- **Values:** Helmet: 5, Chestplate: 8, Leggings: 7, Boots: 4
- **Rationale:** Players should provide equivalent materials to crafting new armor at higher tier

**SMTH-COMPONENT-01:** Component copying for enchantment preservation
- **Pattern:** `result.applyComponents(base.getComponents())` then `result.remove(DataComponents.DAMAGE)`
- **Rationale:** Preserves all data components (enchantments, custom name, etc.) while resetting durability

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

**ItemCombinerMenu constructor signature:** Initial mixin had incorrect constructor parameters (4 vs 5 required). Fixed by adding fifth `null` parameter for ItemCombinerMenuSlotDefinition.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- SmithingMenu mixin infrastructure ready for tool tier upgrades
- TierUpgradeConfig pattern established and reusable
- Material count validation working for armor pieces
- Component preservation pattern verified
- Ready for phase 62: tool tier upgrades

---
*Phase: 61-smithing-table-tier-upgrades*
*Completed: 2026-01-29*
