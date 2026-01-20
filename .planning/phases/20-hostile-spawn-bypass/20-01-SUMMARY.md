---
phase: 20
plan: 01
subsystem: spawning
tags: [mixin, monster, spawn, light, sky-light, block-light]

dependency_graph:
  requires:
    - "Phase 15: NaturalSpawnerMixin base chunk spawn blocking"
    - "Phase 19: MobSunBurnMixin pattern"
  provides:
    - "Sky light bypass for hostile mob spawning"
    - "Block light spawn prevention preserved"
  affects:
    - "All hostile mob spawn behavior"

tech_stack:
  added: []
  patterns:
    - "Static method mixin with HEAD inject cancellable for spawn check override"
    - "DimensionType query for dimension-specific spawn limits"

files:
  created:
    - src/main/java/thc/mixin/MonsterSpawnLightMixin.java
  modified:
    - src/main/resources/thc.mixins.json

decisions:
  - pattern: "Sky light bypass with block light preservation"
    rationale: "Daytime spawning without removing torch protection"
    confidence: high

metrics:
  duration: "2 min"
  completed: "2026-01-20"
---

# Phase 20 Plan 01: Hostile Spawn Light Bypass Summary

**One-liner:** HEAD inject on Monster.isDarkEnoughToSpawn bypassing sky light while preserving block light protection via DimensionType queries.

## What Changed

### MonsterSpawnLightMixin

Created mixin targeting `net.minecraft.world.entity.monster.Monster` that intercepts the `isDarkEnoughToSpawn` static method. The vanilla method checks sky light first, then block light, then dimension-specific brightness thresholds.

Our implementation:
1. Skips sky light check entirely (mobs spawn in daylight)
2. Preserves block light check using `dimensionType.monsterSpawnBlockLightLimit()`
3. Preserves brightness threshold test using `dimensionType.monsterSpawnLightTest()`

This means torches and lamps still protect areas from spawns, but sunlight no longer matters.

### Mixin Registration

Added MonsterSpawnLightMixin to thc.mixins.json in alphabetical order.

## Implementation Details

**Target method signature:**
```java
public static boolean isDarkEnoughToSpawn(
    ServerLevelAccessor serverLevelAccessor,
    BlockPos blockPos,
    RandomSource randomSource
)
```

**Injection point:** HEAD with cancellable=true (intercepts before vanilla logic)

**Key API usage:**
- `level.dimensionType().monsterSpawnBlockLightLimit()` - gets dimension's block light threshold
- `level.getBrightness(LightLayer.BLOCK, pos)` - reads block light at position
- `level.getMaxLocalRawBrightness(pos)` - combined brightness for threshold test
- `dimensionType.monsterSpawnLightTest().sample(random)` - dimension's randomized spawn threshold

## Decisions Made

| Decision | Rationale | Confidence |
|----------|-----------|------------|
| HEAD inject with full replacement | Need to skip vanilla sky light check entirely while preserving block light logic | High |
| Preserve dimension brightness test | Maintains compatibility with custom dimensions and thunderstorm mechanics | High |

## Deviations from Plan

None - plan executed exactly as written.

## Files Changed

| File | Change | Lines |
|------|--------|-------|
| src/main/java/thc/mixin/MonsterSpawnLightMixin.java | Created | +72 |
| src/main/resources/thc.mixins.json | Added registration | +1 |

## Commits

| Hash | Type | Description |
|------|------|-------------|
| dedf843 | feat | create MonsterSpawnLightMixin for sky light bypass |
| 4d4d3ca | feat | register MonsterSpawnLightMixin |

## Success Criteria Verification

- [x] All tasks completed (2/2)
- [x] All verification checks pass
- [x] SPAWN-01 implemented (hostile mobs can spawn regardless of sky light)
- [x] SPAWN-02 preserved (block light still affects spawn density)
- [x] No regressions to base chunk spawn blocking (NaturalSpawnerMixin unchanged)

## Next Phase Readiness

Phase 20 is complete with this single plan. The hostile spawn bypass system is now fully operational:
- Mobs spawn during daytime (sky light ignored)
- Torches and lamps still create safe zones (block light preserved)
- Claimed base chunks still block all spawns (NaturalSpawnerMixin preserved)

Ready to proceed to Phase 21 (Saturation Removal).
