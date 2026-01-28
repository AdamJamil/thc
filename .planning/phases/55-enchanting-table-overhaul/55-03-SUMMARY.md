---
phase: 55-enchanting-table-overhaul
plan: 03
subsystem: enchanting
tags: [mixin, enchantment-menu, deterministic, book-slot]

# Dependency graph
requires:
  - phase: 55-01
    provides: EnchantmentEnforcement stage classification (getStageForEnchantment, getLevelRequirementForStage)
  - phase: 55-02
    provides: EnchantmentEnforcement STAGE_4_5_ENCHANTMENTS set
provides:
  - EnchantmentMenuMixin with deterministic book-slot enchanting
  - HEAD cancellation on cost calculation (method_17411)
  - HEAD cancellation on click handler (clickMenuButton)
  - 15 bookshelf requirement enforcement
  - Single-enchantment book validation
  - Stage-based level requirements (10/20/30)
affects: [56-fire-overhaul]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - EnchantmentMenu HEAD cancellation for complete behavior replacement
    - Bookshelf counting via EnchantingTableBlock.BOOKSHELF_OFFSETS + isValidBookShelf()
    - EnchantmentHelper.updateEnchantments() for applying enchantments to gear

key-files:
  created:
    - src/main/java/thc/mixin/EnchantmentMenuMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use ResourceKey.identifier() not location() in MC 1.21.11"
  - "15 bookshelves required (all valid positions filled)"
  - "Single-enchantment books only - reject multi-enchantment"
  - "3 level cost regardless of enchantment stage"
  - "Book remains in slot after enchanting (unlimited uses)"

patterns-established:
  - "EnchantmentMenu mixin: HEAD cancel on method_17411 and clickMenuButton"
  - "Bookshelf counting: EnchantingTableBlock.BOOKSHELF_OFFSETS iteration"
  - "Enchantment ID extraction: holder.unwrapKey().get().identifier().toString()"

# Metrics
duration: 5min
completed: 2026-01-28
---

# Phase 55 Plan 03: EnchantmentMenuMixin Summary

**Deterministic book-slot enchanting via EnchantmentMenuMixin - book determines exact enchantment, 15 bookshelves required, costs 3 levels**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-28T16:51:52Z
- **Completed:** 2026-01-28T16:57:18Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Created EnchantmentMenuMixin replacing RNG-based enchanting with deterministic book-slot mechanic
- HEAD cancellation on method_17411 (cost calculation) sets level requirement based on enchantment stage
- HEAD cancellation on clickMenuButton applies enchantment from book to item, costs 3 levels
- Book remains in slot after enchanting (unlimited uses)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EnchantmentMenuMixin with cost calculation** - `5ae5580` (feat)
2. **Task 2: Add click handler for enchantment application** - `90dffcb` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/EnchantmentMenuMixin.java` - Mixin replacing RNG with deterministic book-slot enchanting
- `src/main/resources/thc.mixins.json` - Added EnchantmentMenuMixin registration

## Decisions Made
- Used `identifier()` not `location()` for ResourceKey ID extraction in MC 1.21.11 (API difference discovered during build)
- Book stays in slot after enchanting - provides unlimited uses as specified in CONTEXT
- All 15 bookshelf positions must be filled (not 16, which is impossible with vanilla placement)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed ResourceKey method name**
- **Found during:** Task 1 (Initial mixin creation)
- **Issue:** Plan used `location()` but MC 1.21.11 API uses `identifier()`
- **Fix:** Changed to `enchantHolder.unwrapKey().get().identifier().toString()`
- **Files modified:** src/main/java/thc/mixin/EnchantmentMenuMixin.java
- **Verification:** Build passed after fix
- **Committed in:** 5ae5580 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** API method name difference between plan template and actual MC 1.21.11 API. No scope creep.

## Issues Encountered
None - straightforward mixin implementation following established patterns.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- EnchantmentMenuMixin complete with deterministic enchanting
- Ready for Phase 56 (Fire Overhaul) if needed
- Enchanting table now fully converted from RNG to book-slot mechanic

---
*Phase: 55-enchanting-table-overhaul*
*Completed: 2026-01-28*
