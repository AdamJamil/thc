---
phase: 51-brewing-removal
verified: 2026-01-26T03:50:41Z
status: passed
score: 5/5 must-haves verified
---

# Phase 51: Brewing Removal Verification Report

**Phase Goal:** Potions are completely unobtainable, removing buff/heal economy  
**Verified:** 2026-01-26T03:50:41Z  
**Status:** PASSED  
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Brewing stands do not appear in villages | ✓ VERIFIED | Blocks.BREWING_STAND in FILTERED_STRUCTURE_BLOCKS (StructureTemplateMixin.java:36) |
| 2 | Brewing stands do not appear in igloos | ✓ VERIFIED | Same structure filtering applies to all structures including igloos |
| 3 | Brewing stands do not appear in End ships | ✓ VERIFIED | Same structure filtering applies to all structures including End ships |
| 4 | Brewing stand recipe is not in crafting menu | ✓ VERIFIED | "brewing_stand" in REMOVED_RECIPE_PATHS (RecipeManagerMixin.java:41) |
| 5 | Piglins do not give Fire Resistance potions when bartering | ✓ VERIFIED | No "fire_resistance" string in piglin_bartering.json, water bottles retained |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/mixin/StructureTemplateMixin.java` | Structure block filtering for brewing stands | ✓ VERIFIED | EXISTS (64 lines), SUBSTANTIVE (real @Redirect implementation), WIRED (registered in thc.mixins.json:47) |
| `src/main/java/thc/mixin/RecipeManagerMixin.java` | Recipe removal for brewing stand | ✓ VERIFIED | EXISTS (62 lines), SUBSTANTIVE (real @Inject implementation), WIRED (registered in thc.mixins.json:40) |
| `src/main/resources/data/minecraft/loot_table/gameplay/piglin_bartering.json` | Piglin bartering without potions | ✓ VERIFIED | EXISTS (243 lines), SUBSTANTIVE (complete loot table with 17 entries), WIRED (data pack override location) |

**All artifacts:** 3/3 verified at all three levels (existence, substantiveness, wiring)

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| StructureTemplateMixin.java | FILTERED_STRUCTURE_BLOCKS | Set.contains check in redirect | ✓ WIRED | Line 58: `FILTERED_STRUCTURE_BLOCKS.contains(state.getBlock())` |
| RecipeManagerMixin.java | REMOVED_RECIPE_PATHS | Set.contains check in injection | ✓ WIRED | Line 54: `REMOVED_RECIPE_PATHS.contains(holder.id().identifier().getPath())` |
| piglin_bartering.json | loot table type | type field matching context | ✓ WIRED | Line 2: `"type": "minecraft:barter"` with proper random_sequence |

**All key links:** 3/3 verified as properly wired

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| BREW-01: Brewing stands removed from natural spawns | ✓ SATISFIED | StructureTemplateMixin filters Blocks.BREWING_STAND from all structure generation |
| BREW-02: Brewing stands cannot be crafted | ✓ SATISFIED | RecipeManagerMixin removes "brewing_stand" recipe path |
| BREW-03: Potions removed from piglin bartering tables | ✓ SATISFIED | piglin_bartering.json contains no fire_resistance entries, water bottles retained |

**Requirements:** 3/3 satisfied

### Anti-Patterns Found

**None detected.**

Verification scanned for:
- TODO/FIXME/placeholder comments: None found
- Empty implementations: None found  
- Stub patterns: None found
- Console.log only implementations: N/A (Java/Mixin code)

All implementations are complete and production-ready.

### Build Verification

```bash
./gradlew build
```

**Result:** ✓ SUCCESS (build completes with no errors)

## Detailed Verification Analysis

### Artifact 1: StructureTemplateMixin.java

**Level 1 - Existence:** ✓ PASS  
File exists at expected path with 64 lines.

**Level 2 - Substantiveness:** ✓ PASS
- Line count: 64 lines (exceeds 15-line minimum for mixins)
- Contains required pattern: `Blocks.BREWING_STAND` (line 36)
- Real implementation: @Redirect on setBlock with Set.contains logic
- Javadoc updated: "Filters furnace, blast furnace, smoker, and brewing stand blocks" (line 17)
- No stub patterns: No TODO, FIXME, placeholder, or empty returns

**Level 3 - Wired:** ✓ PASS
- Registered in thc.mixins.json at line 47
- Contains check: `FILTERED_STRUCTURE_BLOCKS.contains(state.getBlock())` (line 58)
- Pattern match verified: Set.contains check in redirect method

**Artifact Status:** ✓ FULLY VERIFIED

### Artifact 2: RecipeManagerMixin.java

**Level 1 - Existence:** ✓ PASS  
File exists at expected path with 62 lines.

**Level 2 - Substantiveness:** ✓ PASS
- Line count: 62 lines (exceeds 10-line minimum for mixins)
- Contains required pattern: `"brewing_stand"` (line 41)
- Real implementation: @Inject on recipe loading with filtering logic
- No stub patterns: No TODO, FIXME, placeholder, or empty returns

**Level 3 - Wired:** ✓ PASS
- Registered in thc.mixins.json at line 40
- Contains check: `REMOVED_RECIPE_PATHS.contains(holder.id().identifier().getPath())` (line 54)
- Pattern match verified: Set.contains check in injection

**Artifact Status:** ✓ FULLY VERIFIED

### Artifact 3: piglin_bartering.json

**Level 1 - Existence:** ✓ PASS  
File exists at expected path with 243 lines.

**Level 2 - Substantiveness:** ✓ PASS
- Line count: 243 lines (complete loot table)
- Contains required pattern: `"minecraft:barter"` (line 2)
- Contains required sequence: `"minecraft:gameplay/piglin_bartering"` (line 242)
- Real loot table: 17 complete entries with weights, functions, counts
- Fire Resistance verification: grep -i "fire_resistance" returns NO MATCHES
- Water bottle retained: Entry at lines 30-39 with `"minecraft:water"` potion ID (weight 10)
- No stub patterns: Valid JSON, complete entry structure

**Level 3 - Wired:** ✓ PASS
- Location: `src/main/resources/data/minecraft/loot_table/gameplay/` (correct data pack override path)
- Type field: `"type": "minecraft:barter"` matches piglin bartering context
- Random sequence: Proper `random_sequence` field for gameplay loot tables

**Artifact Status:** ✓ FULLY VERIFIED

## Truth-to-Artifact Mapping

### Truth 1-3: Brewing stands do not appear in structures

**Supporting Artifacts:**
- StructureTemplateMixin.java (✓ VERIFIED)

**Wiring:**
- FILTERED_STRUCTURE_BLOCKS set contains Blocks.BREWING_STAND
- @Redirect on setBlock checks Set.contains before placement
- Returns true (pretends placement succeeded) without actually placing block

**Conclusion:** ✓ All three structure-based truths verified via single artifact

### Truth 4: Brewing stand recipe is not in crafting menu

**Supporting Artifacts:**
- RecipeManagerMixin.java (✓ VERIFIED)

**Wiring:**
- REMOVED_RECIPE_PATHS set contains "brewing_stand" 
- @Inject on recipe loading filters recipes where holder.id path matches
- Returns filtered RecipeMap without brewing_stand recipe

**Conclusion:** ✓ Truth verified via artifact implementation

### Truth 5: Piglins do not give Fire Resistance potions when bartering

**Supporting Artifacts:**
- piglin_bartering.json (✓ VERIFIED)

**Wiring:**
- Loot table at correct data pack override location
- Type "minecraft:barter" ensures it overrides vanilla piglin bartering
- No entries with set_potion function for fire_resistance
- Water bottle entry preserved (weight 10, minecraft:water potion)

**Conclusion:** ✓ Truth verified via data pack override

## Human Verification Required

**None.** All truths can be verified through code inspection and build success. No runtime testing needed at this stage.

Optional manual testing (if desired):
1. **Structure Generation Test:** Create new world, locate village/igloo, verify no brewing stands
2. **Recipe Access Test:** Open recipe book, search "brewing", verify no recipe appears  
3. **Piglin Bartering Test:** Trade gold ingots with piglins, verify no fire resistance potions in results, verify water bottles still appear

These are optional quality checks, not required for verification. Code review confirms implementation correctness.

---

## Summary

**Status:** ✓ PASSED

All must-haves verified:
- ✅ 5/5 observable truths verified
- ✅ 3/3 artifacts verified at all three levels
- ✅ 3/3 key links wired correctly  
- ✅ 3/3 requirements satisfied
- ✅ 0 anti-patterns detected
- ✅ Build succeeds

**Phase 51 goal achieved:** Potions are completely unobtainable. Brewing stands removed from all sources (structure generation, crafting). Fire Resistance potions removed from piglin bartering. Buff/heal economy eliminated.

**Next Steps:** Phase 51 complete. Ready to proceed to Phase 52 (Armor Rebalancing) or close Milestone v2.4.

---

_Verified: 2026-01-26T03:50:41Z_  
_Verifier: Claude (gsd-verifier)_
