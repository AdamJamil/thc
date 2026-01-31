---
phase: 67-job-restriction
plan: 02
subsystem: villager
tags: [mixin, poi, job-block, profession-restriction]

dependency-graph:
  requires:
    - phase: 67-01
      provides: AllowedProfessions.isDisallowedJobBlock()
  provides: [poi-blocking-disallowed-jobs]
  affects: []

tech-stack:
  added: []
  patterns: [poi-blocking-defense-in-depth]

files:
  created: []
  modified:
    - src/main/java/thc/mixin/ServerLevelPoiMixin.java

decisions: []

metrics:
  duration: 3m
  completed: 2026-01-31
---

# Phase 67 Plan 02: POI Blocking for Job Blocks Summary

Defense-in-depth POI blocking - disallowed job blocks (brewing stand, smithing table, etc.) do not register as Points of Interest

## What Was Built

### Extended ServerLevelPoiMixin
Modified `src/main/java/thc/mixin/ServerLevelPoiMixin.java`:
- Added imports for `Block` and `AllowedProfessions`
- Extended `thc$blockPoiInClaimedChunks` method to check job blocks after claimed chunk check
- Added early return after claimed chunk cancel for efficiency
- Calls `AllowedProfessions.isDisallowedJobBlock(newBlock)` to filter POI registration
- Updated javadoc to document both blocking behaviors

The mixin now blocks POI in two scenarios:
1. All POI in claimed chunks (existing behavior)
2. Disallowed job site POI everywhere (new behavior)

## Task Commits

| Task | Name | Commit | Key Changes |
|------|------|--------|-------------|
| 1 | Extend ServerLevelPoiMixin | 5a900d9 | Added job block filtering via AllowedProfessions |

## Verification Results

1. `./gradlew build` - PASSED
2. ServerLevelPoiMixin imports AllowedProfessions - VERIFIED
3. ServerLevelPoiMixin calls isDisallowedJobBlock() - VERIFIED
4. Both claimed chunk and job block blocking work sequentially - VERIFIED

## Deviations from Plan

None - plan executed exactly as written.

## Requirements Addressed

- VJOB-03: Job blocks for disallowed professions don't grant jobs (POI never registered)

## Defense-in-Depth Strategy

This plan complements 67-01's VillagerProfessionMixin:
- **VillagerProfessionMixin**: Catches profession changes at data layer (direct assignment)
- **ServerLevelPoiMixin**: Prevents POI registration for disallowed job blocks

Even if a villager somehow bypassed the profession mixin, they couldn't acquire a disallowed profession because:
1. The job block POI doesn't exist
2. Villagers can only acquire professions by detecting nearby job site POI

## Next Phase Readiness

Phase 67 (Job Restriction) is complete. Both layers of profession restriction are in place:
- Profession restriction at data layer (67-01)
- POI blocking for job blocks (67-02)

Ready for Phase 68 (Custom Trade Tables).
