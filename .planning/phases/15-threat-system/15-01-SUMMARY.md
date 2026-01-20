---
phase: 15-threat-system
plan: 01
subsystem: combat
tags: [threat, attachment, fabric-api, mob-ai]

# Dependency graph
requires:
  - phase: none
    provides: n/a (first plan in phase)
provides:
  - MOB_THREAT attachment type for per-mob threat storage
  - ThreatManager utility class with CRUD operations
affects: [15-02-threat-propagation, 15-03-threat-decay, 15-04-threat-targeting]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Static utility class for attachment operations
    - Non-persistent attachment for session-scoped data

key-files:
  created:
    - src/main/java/thc/threat/ThreatManager.java
  modified:
    - src/main/java/thc/THCAttachments.java

key-decisions:
  - "Non-persistent threat storage - resets on mob unload per design spec"
  - "Static utility pattern for ThreatManager (matches existing codebase)"

patterns-established:
  - "Threat package (thc.threat) for all threat system classes"
  - "Map<UUID, Double> for player-to-threat value mapping"

# Metrics
duration: 3min
completed: 2026-01-19
---

# Phase 15 Plan 01: Threat Data Foundation Summary

**MOB_THREAT attachment and ThreatManager utility class enabling per-mob player threat tracking with CRUD operations**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-19T20:00:00Z
- **Completed:** 2026-01-19T20:03:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Registered MOB_THREAT attachment type for Map<UUID, Double> threat storage
- Created ThreatManager utility class with addThreat, getThreat, setThreat, getThreatMap, hasThreat methods
- Established thc.threat package for threat system components

## Task Commits

Each task was committed atomically:

1. **Task 1: Add MOB_THREAT attachment to THCAttachments** - `ad07924` (feat)
2. **Task 2: Create ThreatManager utility class** - `abecb3a` (feat)

## Files Created/Modified
- `src/main/java/thc/THCAttachments.java` - Added MOB_THREAT attachment type with HashMap initializer
- `src/main/java/thc/threat/ThreatManager.java` - Static utility class with threat CRUD operations

## Decisions Made
- Non-persistent attachment (no Codec, no copyOnDeath) - threat resets when mobs unload per Out of Scope in requirements
- Static utility pattern consistent with existing codebase conventions

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- MOB_THREAT attachment ready for use by threat propagation (Plan 02)
- ThreatManager API ready for decay system (Plan 03) and targeting logic (Plan 04)
- No blockers

---
*Phase: 15-threat-system*
*Completed: 2026-01-19*
