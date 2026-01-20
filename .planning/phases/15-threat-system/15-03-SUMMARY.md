---
phase: 15-threat-system
plan: 03
subsystem: combat
tags: [threat, decay, tick-based, mob-ai]

# Dependency graph
requires:
  - phase: 15-01
    provides: MOB_THREAT attachment and ThreatManager class
provides:
  - decayThreat method for passive threat reduction
  - THREAT_LAST_DECAY timestamp attachment
affects: [15-04-threat-targeting]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Tick-based decay with timestamp attachment
    - Lazy decay (called when needed vs tick mixin)

key-files:
  created: []
  modified:
    - src/main/java/thc/threat/ThreatManager.java
    - src/main/java/thc/THCAttachments.java

key-decisions:
  - "1/sec decay rate - balances aggro management without trivializing threat"
  - "Lazy decay via method call, not tick mixin - more efficient"
  - "Zero/negative cleanup prevents map bloat"

patterns-established:
  - "Timestamp attachment for rate-limiting operations (THREAT_LAST_DECAY pattern)"
  - "getGameTime for tick-accurate timing comparisons"

# Metrics
duration: 3min
completed: 2026-01-19
---

# Phase 15 Plan 03: Threat Decay Summary

**Decay method reducing threat by 1 per second with tick-based timing and cleanup of zero entries**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-19
- **Completed:** 2026-01-19
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Added THREAT_LAST_DECAY attachment for tracking decay timing
- Implemented decayThreat method with 1/sec decay rate
- Tick-based check (20 ticks) ensures decay happens exactly once per second
- Automatic cleanup of zero/negative threat entries

## Task Commits

Each task was committed atomically:

1. **Task 1: Add THREAT_LAST_DECAY attachment** - `1a2eab4` (feat)
2. **Task 2: Add decayThreat method to ThreatManager** - `77aa985` (feat)

## Files Created/Modified
- `src/main/java/thc/THCAttachments.java` - Added THREAT_LAST_DECAY Long attachment with initializer 0L
- `src/main/java/thc/threat/ThreatManager.java` - Added decayThreat method with tick-based timing

## Decisions Made
- Lazy decay approach - method called when needed (e.g., in AI goals) rather than via tick mixin
- 1/sec decay rate per requirement THREAT-03 (prevents permanent aggro accumulation)
- Non-persistent timestamp attachment - resets on mob unload (consistent with MOB_THREAT)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- decayThreat method ready for threat targeting goal (Plan 04)
- Complete threat data layer now available: storage, propagation, decay
- No blockers

---
*Phase: 15-threat-system*
*Completed: 2026-01-19*
