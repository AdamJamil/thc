---
phase: 21-bee-always-work
plan: 01
subsystem: gameplay
tags: [mixin, bee, environment-attribute, minecraft-1.21]

# Dependency graph
requires:
  - phase: 19-sun-burn-prevention
    provides: Mixin pattern for behavior override via HEAD inject
provides:
  - BeeMixin for 24/7 bee productivity
  - Environment attribute redirect pattern
affects: [none - self-contained bee behavior modification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@Redirect on EnvironmentAttributeReader.getValue for attribute-specific behavior"
    - "BEES_STAY_IN_HIVE attribute check bypass"

key-files:
  created:
    - src/main/java/thc/mixin/BeeMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use @Redirect instead of @Inject for precise attribute interception"
  - "Target wantsToEnterHive method which uses EnvironmentAttributeSystem in MC 1.21+"

patterns-established:
  - "Environment attribute redirect: @Redirect on EnvironmentAttributeReader.getValue with attribute type check"

# Metrics
duration: 6min
completed: 2026-01-20
---

# Phase 21 Plan 01: Bee Always Work Summary

**BeeMixin with @Redirect on BEES_STAY_IN_HIVE environment attribute for 24/7 bee productivity**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-20T19:23:00Z
- **Completed:** 2026-01-20T19:29:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Bees now work 24/7 regardless of server time or weather
- Used Minecraft 1.21+ EnvironmentAttributeSystem instead of deprecated isNightOrRaining
- Bees still return to hive when nectar-full (hasNectar logic preserved)

## Task Commits

1. **Task 1 + 2: Create BeeMixin and register** - `603e515` (feat)
   - Combined into single commit due to related changes

**Plan metadata:** Pending

## Files Created/Modified

- `src/main/java/thc/mixin/BeeMixin.java` - Redirects BEES_STAY_IN_HIVE environment attribute check to always return false
- `src/main/resources/thc.mixins.json` - Registered BeeMixin in alphabetical order

## Decisions Made

- **Used @Redirect instead of @Inject:** The plan specified isNightOrRaining method which no longer exists in Minecraft 1.21. The bee behavior is now controlled by EnvironmentAttributeSystem.getValue for BEES_STAY_IN_HIVE attribute. @Redirect allows precise interception of this specific attribute check.

- **Target wantsToEnterHive method:** This method in Bee class checks the BEES_STAY_IN_HIVE environment attribute to determine if conditions favor hive return. By returning false for this attribute, bees never consider environmental conditions (night/rain) as a reason to stay in hive.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed incorrect method target for Minecraft 1.21**
- **Found during:** Task 1 (BeeMixin creation)
- **Issue:** Plan specified `isNightOrRaining(Level)` method which does not exist in Minecraft 1.21. The bee AI was refactored to use EnvironmentAttributeSystem.
- **Fix:** Changed from @Inject on non-existent method to @Redirect on EnvironmentAttributeReader.getValue, checking for BEES_STAY_IN_HIVE attribute specifically.
- **Files modified:** src/main/java/thc/mixin/BeeMixin.java
- **Verification:** Build passes with no remap warnings for BeeMixin
- **Committed in:** 603e515

---

**Total deviations:** 1 auto-fixed (1 bug - incorrect API assumption)
**Impact on plan:** Essential fix for Minecraft 1.21 compatibility. Same behavioral outcome achieved.

## Issues Encountered

- Initial build failed with "cannot find symbol" for net.minecraft.world.entity.animal.Bee - the class moved to net.minecraft.world.entity.animal.bee.Bee (extra .bee package) in recent versions
- Build warning "Cannot remap isNightOrRaining" indicated the method doesn't exist, prompting investigation into the new EnvironmentAttributeSystem

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Bee always-work feature complete
- Phase 21 complete (single plan phase)
- Ready for next milestone

---
*Phase: 21-bee-always-work*
*Completed: 2026-01-20*
