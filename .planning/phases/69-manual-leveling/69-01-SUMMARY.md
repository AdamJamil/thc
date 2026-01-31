---
phase: 69-manual-leveling
plan: 01
subsystem: villager-leveling
tags: [villager, xp, mixin, leveling]
dependency-graph:
  requires: []
  provides: [VillagerXpConfig, VillagerLevelingMixin]
  affects: [69-02, 69-03, 70]
tech-stack:
  added: []
  patterns: [xp-capping, auto-level-blocking]
key-files:
  created:
    - src/main/java/thc/villager/VillagerXpConfig.java
    - src/main/java/thc/mixin/VillagerLevelingMixin.java
  modified:
    - src/main/resources/thc.mixins.json
decisions:
  - id: xp-per-trade
    choice: 5 XP per trade uniformly
    reason: Matches 2/3/4/5 trade requirements exactly
metrics:
  duration: 2 min
  completed: 2026-01-31
---

# Phase 69 Plan 01: Block Auto-Leveling Summary

XP thresholds with shouldIncreaseLevel block and rewardTradeXp capping

## What Was Built

### VillagerXpConfig.java
XP threshold constants for villager leveling:
- `XP_PER_TRADE = 5` - uniform XP gain per trade
- `MAX_XP_PER_LEVEL = [0, 10, 15, 20, 25, 0]` - XP caps per level
- `getMaxXpForLevel(level)` - returns max XP for given level
- `isAtMaxXp(level, currentXp)` - checks if at/above cap

Trade requirements:
- Level 1 (Novice): 10 XP = 2 trades
- Level 2 (Apprentice): 15 XP = 3 trades
- Level 3 (Journeyman): 20 XP = 4 trades
- Level 4 (Expert): 25 XP = 5 trades

### VillagerLevelingMixin.java
Two injections to control villager leveling:

1. **thc$blockAutoLeveling** - `@Inject(method = "shouldIncreaseLevel", at = @At("HEAD"), cancellable = true)`
   - Returns false always to prevent vanilla increaseMerchantCareer() call

2. **thc$capXpAtLevelMax** - `@Inject(method = "rewardTradeXp", at = @At("TAIL"))`
   - Caps tradingXp at VillagerXpConfig.getMaxXpForLevel(currentLevel)
   - Prevents XP overflow beyond level threshold

## Key Implementation Details

```java
// Block auto-leveling
@Inject(method = "shouldIncreaseLevel", at = @At("HEAD"), cancellable = true)
private void thc$blockAutoLeveling(CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(false);
}

// Cap XP at level max
@Inject(method = "rewardTradeXp", at = @At("TAIL"))
private void thc$capXpAtLevelMax(MerchantOffer offer, CallbackInfo ci) {
    int currentLevel = this.getVillagerData().level();
    int maxXp = VillagerXpConfig.getMaxXpForLevel(currentLevel);
    if (maxXp > 0 && this.tradingXp > maxXp) {
        this.tradingXp = maxXp;
    }
}
```

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Message |
|------|---------|
| aa1ba08 | feat(69-01): create VillagerXpConfig with XP thresholds |
| 9f8d60d | feat(69-01): create VillagerLevelingMixin to block auto-level and cap XP |

## Next Phase Readiness

Ready for 69-02: Manual Level-up with Emerald
- VillagerXpConfig.isAtMaxXp() available for level-up eligibility check
- VillagerXpConfig.getMaxXpForLevel() available for XP reset after level-up
- Auto-leveling blocked, so manual emerald payment will be sole advancement path

---

*Phase: 69-manual-leveling*
*Plan: 01*
*Completed: 2026-01-31*
