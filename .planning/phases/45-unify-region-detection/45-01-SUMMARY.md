---
phase: 45
plan: 01
subsystem: spawn-system
tags: [region-detection, refactoring, integration-gap]

requires:
  - 44-01  # Regional spawn distributions
  - 44-02  # NBT tagging

provides:
  - unified-region-detection
  - heightmap-based-surface-detection

affects:
  - future spawn system features requiring region detection

tech-stack:
  patterns:
    - static-utility-class

decisions:
  - id: D45-01-001
    summary: "Heightmap-based region detection is authoritative"
    rationale: "canSeeSky() caused mismatches under tree canopy - mobs tagged as UPPER_CAVE but spawned as SURFACE. Heightmap MOTION_BLOCKING matches player intuition."
    affects: "spawn-system"

key-files:
  created:
    - src/main/java/thc/spawn/RegionDetector.java
  modified:
    - src/main/java/thc/mixin/SpawnReplacementMixin.java
    - src/main/java/thc/mixin/MobFinalizeSpawnMixin.java

metrics:
  duration: 5
  completed: 2026-01-25
---

# Phase 45 Plan 01: Unify Region Detection Summary

**One-liner:** Extracted heightmap-based region detection into shared RegionDetector utility to eliminate spawn/tagging mismatches under tree canopy.

## What Was Built

Created unified region detection system resolving integration gap where spawn distribution and NBT tagging used different detection methods:

### RegionDetector Utility (NEW)
- Static utility class with `getRegion(ServerLevel, BlockPos)` method
- Heightmap-based detection using `Heightmap.Types.MOTION_BLOCKING`
- Three-tier logic: Y < 0 → LOWER_CAVE, Y >= heightmap → SURFACE, else UPPER_CAVE
- Null return for non-Overworld dimensions (no regional system)

### SpawnReplacementMixin Updates
- Replaced `thc$detectRegion()` with `RegionDetector.getRegion()` call
- Removed 21-line duplicate region detection logic
- Updated `thc$getReplacementEntity()` to accept region parameter (eliminated redundant canSeeSky check)
- Updated Javadoc to document heightmap-based approach

### MobFinalizeSpawnMixin Updates
- Replaced `detectRegion()` with `RegionDetector.getRegion()` call
- Removed 18-line duplicate region detection logic
- Removed Heightmap import (no longer needed)

## Deviations from Plan

None - plan executed exactly as written.

## Problems Solved

### Integration Gap: Spawn vs Tagging Mismatch
**Problem:** SpawnReplacementMixin used `canSeeSky(pos)` while MobFinalizeSpawnMixin used heightmap. Under tree canopy:
- canSeeSky(pos) returns false (leaves block sky)
- Heightmap Y matches spawn position Y (leaves excluded from MOTION_BLOCKING)
- Result: mob spawned as SURFACE variant, tagged as UPPER_CAVE

**Impact:** Regional spawn caps counted mobs in wrong region. Surface cap could be exceeded while UPPER_CAVE cap had room.

**Solution:** Both mixins now use identical heightmap-based detection via RegionDetector.

### Code Quality: Duplicate Logic
**Problem:** Same region detection logic duplicated across two mixins (39 lines total).

**Solution:** Single source of truth - RegionDetector.java (30 lines). Both mixins reduced by 63 lines total.

## Technical Details

### Heightmap vs canSeeSky Semantics
- **MOTION_BLOCKING heightmap:** Top solid block (excludes leaves, glass)
- **canSeeSky(pos):** Ray trace to sky (blocked by any block including leaves)
- **Player intuition:** Ground level = surface, regardless of overhead canopy

Example: Oak tree at Y=64-72
- canSeeSky(pos) at Y=64: false (leaves overhead)
- Heightmap at X/Z: Y=64 (top of ground)
- Player expectation: "I'm on the surface" ✓ (heightmap matches)

### Why MOTION_BLOCKING (not MOTION_BLOCKING_NO_LEAVES)
MOTION_BLOCKING excludes transparent blocks (leaves, glass) from height calculation:
- Under tree: heightmap = ground level (correct)
- In cave with leaves on ceiling: heightmap = ground level (correct)
- MOTION_BLOCKING_NO_LEAVES would include leaves, defeating the purpose

## Verification

### Build Verification
```
./gradlew build
BUILD SUCCESSFUL in 51s
```

### Pattern Verification
```bash
# Both mixins use shared utility
$ grep "RegionDetector.getRegion" src/main/java/thc/mixin/*.java
SpawnReplacementMixin.java:86:  String region = RegionDetector.getRegion(level, pos);
MobFinalizeSpawnMixin.java:39:  String region = RegionDetector.getRegion(serverLevel, self.blockPosition());

# No duplicate logic remains
$ grep -c "canSeeSky" src/main/java/thc/mixin/SpawnReplacementMixin.java
0
$ grep -c "MOTION_BLOCKING" src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
0
```

## Impact

### Immediate Benefits
1. **Correct cap counting:** Mobs tagged in same region they're spawned for
2. **Single source of truth:** Region detection changes only need one update
3. **Reduced code:** 63 lines of duplicate logic eliminated

### Future Maintenance
Any spawn feature requiring region detection (new distributions, region-specific behaviors) can import RegionDetector without adding new detection variants.

## Commits

| Commit | Type | Description |
|--------|------|-------------|
| 3c8ce9e | feat | Create RegionDetector utility with heightmap-based getRegion() |
| b2b6113 | refactor | Update SpawnReplacementMixin to use RegionDetector |
| 1dc9ccb | refactor | Update MobFinalizeSpawnMixin to use RegionDetector |

## Next Phase Readiness

**Phase 45 Complete:** All region detection now unified.

**No blockers for future work.** Any phase requiring region detection can use RegionDetector.getRegion().
