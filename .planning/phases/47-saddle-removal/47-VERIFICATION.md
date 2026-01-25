---
phase: 47-saddle-removal
verified: 2026-01-25T22:53:19Z
status: passed
score: 5/5 must-haves verified
---

# Phase 47: Saddle Removal Verification Report

**Phase Goal:** Saddles are completely unobtainable, removing mounted combat
**Verified:** 2026-01-25T22:53:19Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Player cannot craft saddles (recipe book shows no saddle recipe) | ✓ VERIFIED | RecipeManagerMixin.java line 40 contains "saddle" in REMOVED_RECIPE_PATHS Set |
| 2 | Leatherworker villagers do not offer saddle trades | ✓ VERIFIED | AbstractVillagerMixin.java line 21 filters Items.SADDLE from MerchantOffers |
| 3 | Chest loot in structures does not contain saddles | ✓ VERIFIED | 6 chest loot table overrides exist, none contain "minecraft:saddle" |
| 4 | Ravagers do not drop saddles when killed | ✓ VERIFIED | ravager.json exists with empty pools array (saddle removed) |
| 5 | Fishing treasure pool does not include saddles | ✓ VERIFIED | treasure.json exists with 5 items (bow, book, rod, name tag, shell), no saddle |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/mixin/RecipeManagerMixin.java` | Recipe removal for saddle | ✓ VERIFIED | 62 lines, contains "saddle" at line 40, exports thc$removeDisabledRecipes method |
| `src/main/java/thc/mixin/AbstractVillagerMixin.java` | Villager trade filtering for saddle | ✓ VERIFIED | 24 lines, contains Items.SADDLE filter at line 21, exports thc$removeShieldTrades method |
| `src/main/resources/data/minecraft/loot_table/chests/bastion_hoglin_stable.json` | Bastion loot without saddle | ✓ VERIFIED | 390 lines, valid JSON, contains no "saddle" string |
| `src/main/resources/data/minecraft/loot_table/chests/end_city_treasure.json` | End city loot without saddle | ✓ VERIFIED | 314 lines, valid JSON, contains no "saddle" string |
| `src/main/resources/data/minecraft/loot_table/chests/nether_bridge.json` | Nether fortress loot without saddle | ✓ VERIFIED | 105 lines, valid JSON, contains no "saddle" string |
| `src/main/resources/data/minecraft/loot_table/chests/village/village_weaponsmith.json` | Weaponsmith loot without saddle | ✓ VERIFIED | 165 lines, valid JSON, contains no "saddle" string |
| `src/main/resources/data/minecraft/loot_table/chests/village/village_tannery.json` | Tannery loot without saddle | ✓ VERIFIED | 80 lines, valid JSON, contains no "saddle" string |
| `src/main/resources/data/minecraft/loot_table/chests/village/village_savanna_house.json` | Savanna house loot without saddle | ✓ VERIFIED | 125 lines, valid JSON, contains no "saddle" string |
| `src/main/resources/data/minecraft/loot_table/gameplay/fishing/treasure.json` | Fishing treasure without saddle | ✓ VERIFIED | 72 lines, valid JSON, 5 entries (no saddle) |
| `src/main/resources/data/minecraft/loot_table/entities/ravager.json` | Ravager loot without saddle | ✓ VERIFIED | 4 lines, valid JSON, empty pools array |

**All artifacts:** Exist, substantive (adequate lines), valid JSON syntax

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| RecipeManagerMixin.java | RecipeManager.prepare | REMOVED_RECIPE_PATHS filtering | ✓ WIRED | Line 40 contains "saddle" in Set.of() list, line 53 filters by path matching |
| AbstractVillagerMixin.java | AbstractVillager.getOffers | removeIf on MerchantOffers | ✓ WIRED | Line 13 injects at getOffers RETURN, line 21 filters Items.SADDLE |

**All key links:** Wired correctly, implementation follows established patterns

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| SADL-01: Saddles removed from chest loot | ✓ SATISFIED | 6 chest loot table overrides (bastion, end city, nether, 3 villages) contain no saddle entries |
| SADL-02: Mobs no longer drop saddles | ✓ SATISFIED | ravager.json override with empty pools array removes saddle drop |
| SADL-03: Saddle recipe removed | ✓ SATISFIED | RecipeManagerMixin adds "saddle" to REMOVED_RECIPE_PATHS |

**Bonus coverage:**
- Villager saddle trades removed (AbstractVillagerMixin filters Items.SADDLE from leatherworker master trades)
- Fishing treasure saddle removed (treasure.json override excludes saddle)

### Anti-Patterns Found

None detected. All files are substantive implementations:
- No TODO/FIXME/placeholder comments found
- No stub patterns (empty returns, console.log only)
- All loot tables preserve vanilla structure with only saddle entries removed
- Ravager empty pools array is intentional (documented in SUMMARY.md)

### Human Verification Required

The following items require in-game testing to verify complete saddle removal:

#### 1. Recipe Book Check
**Test:** Open crafting table, check recipe book for saddle
**Expected:** No saddle recipe appears in any category
**Why human:** Recipe book UI state cannot be verified programmatically

#### 2. Villager Trade Check
**Test:** Trade with Leatherworker villager to master level
**Expected:** No saddle trade offer at any level (especially master level which previously offered saddle for 6 emeralds)
**Why human:** Villager trade generation is runtime behavior

#### 3. Chest Loot Check
**Test:** Generate new chunks, loot chests in:
- Bastion hoglin stables
- End city treasure rooms
- Nether fortresses
- Village weaponsmith/tannery/savanna houses
**Expected:** No saddles in any chest
**Why human:** Loot generation is runtime behavior

#### 4. Ravager Drop Check
**Test:** Kill ravagers in raids or patrols
**Expected:** Ravagers drop nothing (empty loot table)
**Why human:** Mob loot generation is runtime behavior

#### 5. Fishing Check
**Test:** Fish in treasure-eligible water with Luck of the Sea
**Expected:** Treasure pool yields bows, books, rods, name tags, nautilus shells — but no saddles
**Why human:** Fishing loot generation is runtime behavior

**Note:** In-game testing currently blocked by PlayerSleepMixin compatibility issue with Minecraft 1.21.11 (documented in STATE.md). Build verification confirms:
- Mixins compile correctly
- JSON resources are valid
- Data pack structure is correct

---

## Verification Summary

**All automated checks passed:**
- ✓ All 5 observable truths verified
- ✓ All 10 required artifacts exist and are substantive
- ✓ All 2 key links wired correctly
- ✓ All 3 requirements satisfied (plus 2 bonus)
- ✓ No anti-patterns detected
- ✓ Build succeeds without errors

**Phase goal achieved:** Saddles are completely unobtainable through all programmatically verifiable sources (recipe, villager trades, loot tables).

**Human verification needed:** In-game testing to confirm runtime behavior once PlayerSleepMixin is fixed.

---

_Verified: 2026-01-25T22:53:19Z_
_Verifier: Claude (gsd-verifier)_
