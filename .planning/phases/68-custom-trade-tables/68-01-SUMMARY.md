---
phase: 68-custom-trade-tables
plan: 01
subsystem: villager-trading
tags: [mixin, trades, villager, infrastructure]
dependency-graph:
  requires:
    - 67-job-restriction
  provides:
    - trade-interception-hook
    - trade-factory-methods
  affects:
    - 68-02 (librarian trades)
    - 68-03 (remaining trades)
    - 69 (manual leveling)
    - 70 (trade cycling)
tech-stack:
  added: []
  patterns:
    - mixin-trade-interception
    - deterministic-trade-tables
    - factory-method-pattern
file-tracking:
  key-files:
    created:
      - src/main/java/thc/villager/CustomTradeTables.java
      - src/main/java/thc/mixin/VillagerTradesMixin.java
    modified:
      - src/main/resources/thc.mixins.json
decisions:
  - id: "68-01-01"
    decision: "Use ResourceKey comparison via equals() for profession matching"
    rationale: "VillagerProfession constants are ResourceKeys, direct equals() is cleaner than location comparison"
metrics:
  duration: "4 min"
  completed: "2026-01-31"
---

# Phase 68 Plan 01: Trade Table Foundation Summary

Custom trade table infrastructure with mixin interception for 4 allowed professions.

## One-liner

Trade interception mixin + factory methods scaffold for deterministic villager trades.

## What Was Built

### CustomTradeTables.java
Trade table helper class providing:
- `hasCustomTrades()` - Returns true for LIBRARIAN, BUTCHER, MASON, CARTOGRAPHER
- `getTradesFor()` - Dispatcher to profession-specific trade getters (placeholder)
- `createSimpleTrade()` - Factory for single-input trades (cost -> result)
- `createTwoInputTrade()` - Factory for dual-input trades (cost1 + cost2 -> result)
- `getVariantTrade()` - 50/50 random selection between two trade options

Key parameters per CONTEXT.md decisions:
- `maxUses = Integer.MAX_VALUE` - unlimited trades
- `xp = 0` - no XP gain from trading (manual leveling in Phase 69)
- `priceMultiplier = 0.05f` - standard price multiplier

### VillagerTradesMixin.java
Mixin targeting `Villager.class` that:
- Injects at `updateTrades()` HEAD with cancellable
- Checks if profession has custom trades via `CustomTradeTables.hasCustomTrades()`
- Gets custom offers via `CustomTradeTables.getTradesFor()`
- Adds custom offers to villager's MerchantOffers
- Cancels vanilla trade generation for custom professions

## Key Links

| From | To | Via |
|------|----|-----|
| VillagerTradesMixin | CustomTradeTables | `hasCustomTrades()` call |
| VillagerTradesMixin | CustomTradeTables | `getTradesFor()` call |

## Commits

| Hash | Type | Description |
|------|------|-------------|
| deba795 | feat | Create CustomTradeTables scaffold with factory methods |
| 37231cf | feat | Create VillagerTradesMixin for trade interception |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] Build succeeds: `./gradlew build` - BUILD SUCCESSFUL
- [x] VillagerTradesMixin registered in thc.mixins.json
- [x] CustomTradeTables.hasCustomTrades() returns true for allowed professions
- [x] Foundation ready for trade content in subsequent plans

## Next Phase Readiness

Phase 68-02 (Librarian Trades) is unblocked:
- Trade interception infrastructure in place
- Factory methods available for trade creation
- getTradesFor() dispatcher ready to route to profession-specific methods

## Files Changed

| File | Change | Lines |
|------|--------|-------|
| src/main/java/thc/villager/CustomTradeTables.java | Created | 165 |
| src/main/java/thc/mixin/VillagerTradesMixin.java | Created | 90 |
| src/main/resources/thc.mixins.json | Modified | 1 |

---

*Plan: 68-01 | Duration: 4 min | Completed: 2026-01-31*
