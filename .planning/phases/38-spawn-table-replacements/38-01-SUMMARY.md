---
phase: 38-spawn-table-replacements
plan: 01
subsystem: spawning
tags: [monster, spawn, husk, stray, mixin, minecraft, fabricmc]

# Dependency graph
requires:
  - phase: 37-global-monster-modifications
    provides: Monster modification patterns and EntityType comparison approach
provides:
  - Surface zombie -> husk spawn replacement
  - Surface skeleton -> stray spawn replacement
  - Spawn interception via NaturalSpawner mixin
affects: [39-zombie-modifications, 40-creeper-modifications]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@Redirect on NaturalSpawner.addFreshEntityWithPassengers for spawn interception"
    - "canSeeSky(BlockPos) for surface determination"
    - "EntitySpawnReason.NATURAL for replacement entity creation"
    - "snapTo() for entity positioning in MC 1.21.11"

key-files:
  created:
    - src/main/java/thc/mixin/SpawnReplacementMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use @Redirect pattern instead of biome pool modification"
  - "canSeeSky for surface definition (not y-level threshold)"
  - "Passenger check for spider jockey preservation"
  - "Copy baby status and equipment to replacement entities"

patterns-established:
  - "Spawn-time entity replacement via NaturalSpawner mixin"
  - "Sky visibility check for surface vs underground distinction"

# Metrics
duration: 6min
completed: 2026-01-24
---

# Phase 38 Plan 01: Spawn Table Replacements Summary

**Surface zombies spawn as husks, surface skeletons spawn as strays via NaturalSpawner spawn interception**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-24T01:23:12Z
- **Completed:** 2026-01-24T01:29:45Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments
- Surface zombie spawns (sky-visible) replaced with husks
- Surface skeleton spawns (sky-visible) replaced with strays
- Underground spawns preserved (no sky visibility)
- Spider jockey skeletons preserved (passenger check)
- Spawner/structure spawns unaffected (different code path)
- Baby zombie status preserved during replacement
- Equipment copied from original to replacement entity

## Task Commits

Each task was committed atomically:

1. **Task 1: Create SpawnReplacementMixin with entity replacement logic** - `9c5c95d` (feat)

## Files Created/Modified
- `src/main/java/thc/mixin/SpawnReplacementMixin.java` - @Redirect mixin on NaturalSpawner.spawnCategoryForPosition intercepting addFreshEntityWithPassengers
- `src/main/resources/thc.mixins.json` - Added SpawnReplacementMixin registration

## Decisions Made

**1. @Redirect pattern for spawn interception**
- Intercepts addFreshEntityWithPassengers call in spawnCategoryForPosition
- Allows conditional entity replacement before spawn
- Avoids biome pool modification which would break structure spawners

**2. Sky visibility for surface definition**
- Uses level.canSeeSky(pos) not y-level threshold
- Per phase context: "Y-level is not a factor - only sky visibility matters"
- Caves at high y-levels correctly get vanilla spawns

**3. Passenger check for jockey preservation**
- Checks entity.getPassengers().isEmpty() and entity.getVehicle() != null
- Spider jockeys have skeleton passengers - preserved as vanilla skeletons
- Covers edge case where spider with rider passes through spawn

**4. Entity data preservation**
- Baby status: original.isBaby() -> husk.setBaby(true)
- Equipment: loops EquipmentSlot.values() copying all slots
- Position: uses snapTo() per MC 1.21.11 API

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed package paths for MC 1.21.11**
- **Found during:** Task 1 (SpawnReplacementMixin compilation)
- **Issue:** Zombie/Skeleton classes moved to subpackages in MC 1.21.11
- **Fix:** Changed imports from `monster.Zombie` to `monster.zombie.Zombie` and `monster.Skeleton` to `monster.skeleton.AbstractSkeleton`
- **Files modified:** src/main/java/thc/mixin/SpawnReplacementMixin.java
- **Verification:** Build succeeded
- **Committed in:** 9c5c95d (Task 1 commit)

**2. [Rule 3 - Blocking] Fixed EntityType.create() signature for MC 1.21.11**
- **Found during:** Task 1 (SpawnReplacementMixin compilation)
- **Issue:** EntityType.create(Level) no longer exists - requires EntitySpawnReason parameter
- **Fix:** Changed to EntityType.create(level, EntitySpawnReason.NATURAL)
- **Files modified:** src/main/java/thc/mixin/SpawnReplacementMixin.java
- **Verification:** Build succeeded
- **Committed in:** 9c5c95d (Task 1 commit)

**3. [Rule 3 - Blocking] Fixed moveTo() to snapTo() for MC 1.21.11**
- **Found during:** Task 1 (SpawnReplacementMixin compilation)
- **Issue:** Entity.moveTo(x,y,z,yRot,xRot) method renamed to snapTo() in MC 1.21.11
- **Fix:** Changed moveTo() calls to snapTo()
- **Files modified:** src/main/java/thc/mixin/SpawnReplacementMixin.java
- **Verification:** Build succeeded
- **Committed in:** 9c5c95d (Task 1 commit)

---

**Total deviations:** 3 auto-fixed (all blocking - MC 1.21.11 API compatibility)
**Impact on plan:** All fixes necessary for compilation. No functional changes to requirements. Documented MC 1.21.11 API patterns for future phases.

## Issues Encountered

**Minecraft 1.21.11 API changes**
- Entity classes moved to subpackages (monster.zombie, monster.skeleton)
- EntityType.create() now requires EntitySpawnReason parameter
- Entity.moveTo() renamed to snapTo()
- Resolution: Updated to MC 1.21.11 APIs, documented patterns for future reference

## Next Phase Readiness

**Ready for next phase:**
- Spawn replacement mixin complete and building successfully
- Surface definition (canSeeSky) matches phase context requirements
- Spider jockey preservation via passenger check
- Underground spawns correctly preserved

**No blockers:** Build succeeds, all requirements implemented

---
*Phase: 38-spawn-table-replacements*
*Completed: 2026-01-24*
