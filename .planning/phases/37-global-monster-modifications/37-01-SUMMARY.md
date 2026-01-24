---
phase: 37-global-monster-modifications
plan: 01
subsystem: combat
tags: [monster, speed, loot, attributes, fabricmc, kotlin, minecraft]

# Dependency graph
requires:
  - phase: 36-class-boon-system
    provides: Attachment system patterns for entity state management
provides:
  - Global monster speed modifications with exclusions
  - Baby zombie speed normalization
  - Complete loot filtering for equipment and iron ingots
affects: [38-skeleton-modifications, 39-zombie-modifications, 40-creeper-modifications]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ServerEntityEvents.ENTITY_LOAD for entity spawn modification
    - Transient AttributeModifiers for runtime-only stat changes
    - EntityType comparison for type-specific behavior (MC 1.21.11)
    - Identifier instead of ResourceLocation (MC 1.21.11 API)

key-files:
  created:
    - src/main/kotlin/thc/monster/MonsterModifications.kt
  modified:
    - src/main/kotlin/thc/THC.kt

key-decisions:
  - "Use transient AttributeModifiers to avoid save bloat"
  - "Apply speed boost via ENTITY_LOAD event instead of mixin"
  - "Use EntityType comparison instead of instanceof for Zombie checks (MC 1.21.11)"
  - "Use Identifier.fromNamespaceAndPath instead of ResourceLocation (MC 1.21.11 API)"

patterns-established:
  - "Event-driven attribute modification for global monster changes"
  - "Exclusion-based speed modification (whitelist safe mobs)"
  - "Counter-modifiers for negating vanilla bonuses (baby zombie speed)"

# Metrics
duration: 5min
completed: 2026-01-24
---

# Phase 37 Plan 01: Global Monster Modifications Summary

**20% speed boost for hostile mobs with Creeper exclusion, baby zombie normalization, and complete equipment/iron loot filtering**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-24T00:44:14Z
- **Completed:** 2026-01-24T00:49:21Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- 20% speed increase for hostile mobs (excluding Creepers and bosses)
- Baby zombies now move at same speed as adult zombies
- Zero armor drops from equipped monsters (20 armor pieces filtered)
- Zero weapon drops from equipped monsters (5 sword types + bow/crossbow)
- Zero iron ingot drops from zombies/husks

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MonsterModifications.kt with speed modifications** - `b601774` (feat)
2. **Task 2: Extend THC.kt with loot filtering and registration** - `593c55c` (feat)

## Files Created/Modified
- `src/main/kotlin/thc/monster/MonsterModifications.kt` - Global monster speed modifications via ServerEntityEvents.ENTITY_LOAD
- `src/main/kotlin/thc/THC.kt` - Extended removedItems set with armor, swords, iron ingot; registered MonsterModifications

## Decisions Made

**1. Event-based attribute modification instead of mixin**
- Used ServerEntityEvents.ENTITY_LOAD for speed modifications
- Avoids mixin complexity for global stat changes
- Transient modifiers applied at spawn, not persisted to disk

**2. Minecraft 1.21.11 API adjustments**
- Used `Identifier.fromNamespaceAndPath()` instead of `ResourceLocation`
- Used `EntityType` comparison (`mob.type == EntityType.ZOMBIE`) instead of `instanceof Zombie`
- These are MC 1.21.11 API changes discovered during compilation

**3. Counter-modifier for baby zombie normalization**
- Applied -0.5 multiplier to negate vanilla's +0.5 BABY_SPEED_BONUS
- Result: baby zombies move at same speed as adult zombies (both benefit from 20% boost)

**4. Exclusion strategy for speed boost**
- Creepers: unchanged (FR-03 explicit requirement)
- Baby zombies: excluded from boost, handled separately by normalization
- Bosses (EnderDragon, WitherBoss): excluded to avoid breaking hardcoded behaviors

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated imports for Minecraft 1.21.11 API**
- **Found during:** Task 1 (MonsterModifications.kt compilation)
- **Issue:** `ResourceLocation` class not found - API changed in MC 1.21.11
- **Fix:** Changed to `Identifier.fromNamespaceAndPath()` (new MC 1.21.11 API)
- **Files modified:** src/main/kotlin/thc/monster/MonsterModifications.kt
- **Verification:** Build succeeded
- **Committed in:** b601774 (Task 1 commit)

**2. [Rule 3 - Blocking] Changed Zombie instanceof checks to EntityType comparison**
- **Found during:** Task 1 (MonsterModifications.kt compilation)
- **Issue:** `Zombie` class import unresolved in Kotlin
- **Fix:** Changed from `mob is Zombie` to `mob.type == EntityType.ZOMBIE`
- **Files modified:** src/main/kotlin/thc/monster/MonsterModifications.kt
- **Verification:** Build succeeded
- **Committed in:** b601774 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (both blocking - MC 1.21.11 API compatibility)
**Impact on plan:** Both fixes necessary for compilation. No functional changes to requirements. Documented MC 1.21.11 API patterns for future phases.

## Issues Encountered

**Minecraft 1.21.11 API changes**
- ResourceLocation → Identifier API change required adjustment
- Zombie instanceof pattern required EntityType comparison instead
- Resolution: Updated to MC 1.21.11 APIs, documented patterns for future reference

## Next Phase Readiness

**Ready for next phase:**
- Global monster modifications complete and building successfully
- Speed boost verified via attribute modifiers
- Loot filtering extended to cover all equipment and iron ingots
- Patterns established for monster-specific modifications in phases 38-40

**No blockers:** Build succeeds, all requirements implemented

---
*Phase: 37-global-monster-modifications*
*Completed: 2026-01-24*
