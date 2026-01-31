---
phase: 68-custom-trade-tables
plan: 02
subsystem: villager-trading
tags: [trades, enchanted-books, librarian, villager]
dependency-graph:
  requires:
    - 68-01 (trade infrastructure)
    - EnchantmentEnforcement
  provides:
    - librarian-trade-table
    - enchanted-book-factory
  affects:
    - 68-03 (remaining trades)
    - 69 (manual leveling)
    - 70 (trade cycling)
tech-stack:
  added: []
  patterns:
    - enchanted-book-trade-factory
    - registry-based-enchantment-lookup
file-tracking:
  key-files:
    created: []
    modified:
      - src/main/java/thc/villager/CustomTradeTables.java
decisions:
  - id: "68-02-01"
    decision: "Use Identifier.withDefaultNamespace() for enchantment ID lookup"
    rationale: "Fabric/Yarn mapping uses Identifier instead of ResourceLocation"
metrics:
  duration: "5 min"
  completed: "2026-01-31"
---

# Phase 68 Plan 02: Librarian Trades Summary

Complete librarian trade table with enchanted book factory using THC internal levels.

## One-liner

9 librarian trades (TLIB-01 to TLIB-09) with 50/50 variants and EnchantmentEnforcement-based book levels.

## What Was Built

### createEnchantedBookTrade() Factory Method
Factory for creating enchanted book trades that:
- Takes emerald cost, enchantment ID (without namespace), and ServerLevel
- Looks up enchantment from registry via `serverLevel.registryAccess()`
- Gets internal level from `EnchantmentEnforcement.INSTANCE.getINTERNAL_LEVELS()`
- Defaults to level 1 if enchantment not in INTERNAL_LEVELS map
- Sets enchantment on book via `DataComponents.STORED_ENCHANTMENTS`

Enchantments with internal levels > 1:
- efficiency: 3
- fortune: 3
- looting: 3
- unbreaking: 3
- feather_falling: 4

All others (mending, silk_touch, protection, projectile_protection, sharpness, power, blast_protection, breach, piercing) default to level 1.

### getLibrarianTrades() Method
Complete librarian trade table with 50/50 variants for all 9 slots:

| Level | Trades | Slot Details |
|-------|--------|--------------|
| 1 (Novice) | 2 | TLIB-01: paper/lanterns, TLIB-02: mending/unbreaking |
| 2 (Apprentice) | 2 | TLIB-03: efficiency/fortune, TLIB-04: silk touch/books |
| 3 (Journeyman) | 2 | TLIB-05: protection/proj prot, TLIB-06: looting/bookshelves |
| 4 (Expert) | 2 | TLIB-07: sharpness/power, TLIB-08: blast prot/feather falling |
| 5 (Master) | 1 | TLIB-09: breach/piercing |

### Key Trade Parameters
All trades use:
- `maxUses = Integer.MAX_VALUE` (unlimited)
- `xp = 0` (no XP gain, manual leveling in Phase 69)
- `priceMultiplier = 0.05f` (standard)

## Key Links

| From | To | Via |
|------|----|-----|
| getLibrarianTrades | createEnchantedBookTrade | Factory calls for all enchanted book trades |
| createEnchantedBookTrade | EnchantmentEnforcement.INTERNAL_LEVELS | Enchantment level lookup |
| getTradesFor | getLibrarianTrades | Dispatcher routing for librarian profession |

## Commits

| Hash | Type | Description |
|------|------|-------------|
| ce2a659 | feat | Add enchanted book creation method for librarian trades |
| 473d040 | feat | Implement all 9 librarian trades (TLIB-01 to TLIB-09) |

## Deviations from Plan

### Linter Auto-Added Code

**1. Butcher trades added by linter**
- **Found during:** Task 1 compilation
- **Issue:** Linter/IDE auto-added butcher trades and dispatch to CustomTradeTables.java
- **Impact:** None - butcher trades were for 68-03 anyway, just added early
- **Files modified:** CustomTradeTables.java
- **Commit:** Included in ce2a659

**2. Mason trades dispatch added by linter**
- **Found during:** Task 2 build
- **Issue:** Linter/IDE auto-added mason dispatch to getTradesFor()
- **Impact:** None - mason trades are for 68-03, implementation still needed
- **Files modified:** CustomTradeTables.java
- **Commit:** Included in 473d040

### Import Name Fix

**3. [Rule 3 - Blocking] Changed ResourceLocation to Identifier**
- **Found during:** Task 1 compilation
- **Issue:** Plan used `ResourceLocation.withDefaultNamespace()` but Fabric mapping uses `Identifier`
- **Fix:** Changed import and usage to `net.minecraft.resources.Identifier`
- **Files modified:** CustomTradeTables.java
- **Commit:** ce2a659

## Next Phase Readiness

**68-03 Ready:** Yes
- Librarian trades complete
- Butcher trades already present (linter added)
- Mason and cartographer trades remain to implement
- createEnchantedBookTrade() available for any future enchanted book needs
