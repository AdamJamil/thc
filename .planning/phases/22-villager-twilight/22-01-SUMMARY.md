---
phase: 22-villager-twilight
plan: 01
subsystem: gameplay
tags: [mixin, villager, brain-ai, schedule, minecraft-1.21]

# Dependency graph
requires:
  - phase: 21-bee-always-work
    provides: Environment attribute and brain schedule modification patterns
provides:
  - VillagerMixin for perpetual night schedule behavior
  - Brain.updateActivityFromSchedule redirect pattern with EnvironmentAttributeSystem
affects: [none - self-contained villager behavior modification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@Redirect on Brain.updateActivityFromSchedule for schedule time override"
    - "EnvironmentAttributeSystem parameter handling in MC 1.21+"

key-files:
  created:
    - src/main/java/thc/mixin/VillagerMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Target customServerAiStep method where Villager calls Brain.updateActivityFromSchedule"
  - "Use 13000L (mid-night) for perpetual REST activity selection"

patterns-established:
  - "Brain schedule time redirect: @Redirect on updateActivityFromSchedule with constant time for forced activity"

# Metrics
duration: 5min
completed: 2026-01-20
---

# Phase 22 Plan 01: Villager Twilight Summary

**VillagerMixin with @Redirect on Brain.updateActivityFromSchedule for perpetual night schedule behavior**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-20T19:45:00Z
- **Completed:** 2026-01-20T19:50:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Villagers now use night schedule (REST activity) regardless of server time
- Brain.updateActivityFromSchedule redirected to always pass 13000L (mid-night)
- Trading still works when players interact (player interactions override schedule)

## Task Commits

1. **Task 1: Create VillagerMixin for perpetual night schedule** - `e5f29e7` (feat)
2. **Task 2: Register mixin and test behavior** - `dc0d0b3` (chore)

**Plan metadata:** Pending

## Files Created/Modified

- `src/main/java/thc/mixin/VillagerMixin.java` - Redirects Brain.updateActivityFromSchedule to always use 13000L (night)
- `src/main/resources/thc.mixins.json` - Registered VillagerMixin in alphabetical order

## Decisions Made

- **Targeted customServerAiStep method:** This is where Villager calls brain.updateActivityFromSchedule with the day time parameter. Redirecting here allows us to modify just the time input.

- **Used 13000L as constant night time:** 13000 ticks is solidly in the REST/SLEEP activity range (12000-23999), ensuring villagers always select their night behavior.

- **Used @Redirect instead of @ModifyVariable:** @Redirect gives precise control over the Brain method call and allows us to pass through the EnvironmentAttributeSystem and Vec3 parameters unchanged while modifying only the time.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed incorrect package path for Villager class**
- **Found during:** Task 1 (VillagerMixin creation)
- **Issue:** Plan assumed `net.minecraft.world.entity.npc.Villager` but MC 1.21 moved it to `net.minecraft.world.entity.npc.villager.Villager`
- **Fix:** Updated import to use correct package path with extra `.villager.` segment
- **Files modified:** src/main/java/thc/mixin/VillagerMixin.java
- **Verification:** Build passes with no import errors
- **Committed in:** e5f29e7

**2. [Rule 1 - Bug] Fixed incorrect Brain.updateActivityFromSchedule signature**
- **Found during:** Task 1 (VillagerMixin creation)
- **Issue:** Plan assumed signature `(long, long)` but MC 1.21 uses `(EnvironmentAttributeSystem, long, Vec3)`
- **Fix:** Updated @Redirect target and method signature to match MC 1.21 EnvironmentAttributeSystem API
- **Files modified:** src/main/java/thc/mixin/VillagerMixin.java
- **Verification:** Build passes with no remap warnings for VillagerMixin
- **Committed in:** e5f29e7

---

**Total deviations:** 2 auto-fixed (2 bugs - incorrect API assumptions for MC 1.21)
**Impact on plan:** Essential fixes for Minecraft 1.21 compatibility. Same behavioral outcome achieved.

## Issues Encountered

- Initial build failed with "cannot find symbol" for Villager class - discovered MC 1.21 package reorganization similar to Bee class
- Method signature changed from (long, long) to (EnvironmentAttributeSystem, long, Vec3) in MC 1.21+ Brain API

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Villager twilight behavior complete
- Phase 22 complete (single plan phase)
- Ready for next milestone

---
*Phase: 22-villager-twilight*
*Completed: 2026-01-20*
