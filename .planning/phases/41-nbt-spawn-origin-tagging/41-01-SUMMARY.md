---
phase: 41-nbt-spawn-origin-tagging
plan: 01
subsystem: entity
tags: [spawn, nbt, attachments, region-detection, mixin, mob]

# Dependency graph
requires:
  - phase: 37-monster-speed
    provides: Attachment patterns and ENTITY_LOAD pattern
provides:
  - SPAWN_REGION attachment for regional mob tracking
  - SPAWN_COUNTED attachment for cap counting eligibility
  - MobFinalizeSpawnMixin for spawn-time tagging
affects: [42-regional-spawns, 43-cap-partitioning]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TAIL inject on Mob.finalizeSpawn for spawn-time attachment setting"
    - "Heightmap.Types.MOTION_BLOCKING for surface/cave boundary detection"
    - "Persistent String attachment with null default (absence = not tagged)"

key-files:
  created:
    - src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
  modified:
    - src/main/java/thc/THCAttachments.java
    - src/main/resources/thc.mixins.json

key-decisions:
  - "String encoding for regions (OW_SURFACE/OW_UPPER_CAVE/OW_LOWER_CAVE) for debuggability over byte encoding"
  - "Y=0 as boundary between upper and lower caves (sea level)"
  - "Only NATURAL and CHUNK_GENERATION spawn reasons tagged (spawner mobs excluded)"

patterns-established:
  - "Mob.finalizeSpawn TAIL injection for spawn-time attachment setting"
  - "getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) for surface detection"

# Metrics
duration: 4min
completed: 2026-01-24
---

# Phase 41 Plan 01: NBT Spawn Origin Tagging Summary

**Persistent attachments to track spawn region (OW_SURFACE/OW_UPPER_CAVE/OW_LOWER_CAVE) and cap counting eligibility for naturally-spawned Overworld mobs**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-24T16:29:04Z
- **Completed:** 2026-01-24T16:33:XX
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- SPAWN_REGION (String, persistent) attachment stores spawn region
- SPAWN_COUNTED (Boolean, persistent) attachment marks monsters for cap counting
- MobFinalizeSpawnMixin tags mobs at spawn time based on position
- Region detection: surface (>= heightmap), upper cave (0 to heightmap), lower cave (< 0)
- Only NATURAL/CHUNK_GENERATION spawns tagged (excludes spawners, commands, etc.)
- Only Overworld mobs tagged (Nether/End skipped)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add spawn origin attachments** - `317b798` (feat)
2. **Task 2: Create MobFinalizeSpawnMixin for spawn tagging** - `844470a` (feat)

## Files Created/Modified
- `src/main/java/thc/THCAttachments.java` - Added SPAWN_REGION and SPAWN_COUNTED attachment definitions
- `src/main/java/thc/mixin/MobFinalizeSpawnMixin.java` - Created spawn-time tagging mixin
- `src/main/resources/thc.mixins.json` - Registered MobFinalizeSpawnMixin

## Decisions Made
- **String encoding for regions:** Human-readable values (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE) chosen over byte encoding for NBT debuggability. Storage overhead negligible (~12 bytes per entity).
- **Y=0 boundary:** Sea level used as threshold between upper and lower caves for conceptual simplicity.
- **Spawn reason filtering:** Only NATURAL and CHUNK_GENERATION reasons tagged to exclude spawner mobs, command-spawned mobs, etc.
- **MOTION_BLOCKING heightmap:** Correctly handles overhangs - mobs under tree canopies or other obstructions classified as caves.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected Heightmap API usage**
- **Found during:** Task 2 build
- **Issue:** Plan specified `level.getTopY(Heightmap.Type.MOTION_BLOCKING, ...)` but MC 1.21.11 API is `level.getHeight(Heightmap.Types.MOTION_BLOCKING, ...)`
- **Fix:** Changed to `getHeight` and `Types` enum
- **Files modified:** src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
- **Committed in:** 844470a (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (bug)
**Impact on plan:** Minor API correction. No scope creep.

## Issues Encountered
None - straightforward implementation after API correction.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Spawn tagging infrastructure complete
- Ready for Phase 42 (regional spawns) to consume SPAWN_REGION for spawn rule decisions
- Ready for Phase 43 (cap partitioning) to consume SPAWN_COUNTED for regional mob counting
- Manual verification possible via NBT viewer mod or /data command

---
*Phase: 41-nbt-spawn-origin-tagging*
*Completed: 2026-01-24*
