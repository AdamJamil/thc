---
phase: 64-food-economy-dough-system
verified: 2026-01-30T04:24:00Z
status: passed
score: 12/12 must-haves verified
---

# Phase 64: Food Economy - Dough System Verification Report

**Phase Goal:** Replace instant bread crafting with a cooking-based dough system and normalize leather drops across animals
**Verified:** 2026-01-30T04:24:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Player cannot craft bread directly from wheat | ✓ VERIFIED | "bread" in REMOVED_RECIPE_PATHS (RecipeManagerMixin.java:43) |
| 2 | Dough item appears in creative menu food tab | ✓ VERIFIED | DOUGH added to foodTabKey (THCItems.kt:130) |
| 3 | Dough is craftable from 3 wheat + iron water bucket | ✓ VERIFIED | dough.json recipe exists with correct ingredients |
| 4 | Dough is craftable from 3 wheat + copper water bucket | ✓ VERIFIED | dough_copper.json recipe exists with thc:copper_bucket_of_water |
| 5 | Bucket is preserved after crafting dough | ✓ VERIFIED | Iron bucket uses vanilla craftRemainder; copper bucket has .craftRemainder(COPPER_BUCKET) in THCItems.kt:83 |
| 6 | Dough smelts into bread in furnace | ✓ VERIFIED | dough_smelting.json: thc:dough -> minecraft:bread (200 ticks) |
| 7 | Dough cooks into bread in smoker (faster) | ✓ VERIFIED | dough_smoking.json: thc:dough -> minecraft:bread (100 ticks, 2x faster) |
| 8 | Pigs drop 0-2 leather when killed | ✓ VERIFIED | pig.json pool[0]: uniform count 0.0-2.0 |
| 9 | Pigs drop +1 leather per looting level | ✓ VERIFIED | pig.json pool[0]: looting enchantment with 0-1 count increase |
| 10 | Sheep drop 0-2 leather when killed | ✓ VERIFIED | sheep.json pool[0]: uniform count 0.0-2.0 |
| 11 | Sheep drop +1 leather per looting level | ✓ VERIFIED | sheep.json pool[0]: looting enchantment with 0-1 count increase |
| 12 | Sheep continue to drop wool (all colors) and mutton | ✓ VERIFIED | sheep.json pool[1] (mutton) and pool[2] (16 wool color alternatives) preserved |

**Score:** 12/12 truths verified

### Required Artifacts

| Artifact | Expected | Exists | Substantive | Wired | Status |
|----------|----------|--------|-------------|-------|--------|
| `src/main/java/thc/mixin/RecipeManagerMixin.java` | Bread recipe removal | ✓ | ✓ (65 lines) | ✓ (used in mixin injection) | ✓ VERIFIED |
| `src/main/kotlin/thc/item/THCItems.kt` | DOUGH item registration | ✓ | ✓ (140 lines) | ✓ (registered in BuiltInRegistries) | ✓ VERIFIED |
| `src/main/resources/assets/thc/models/item/dough.json` | Dough item model | ✓ | ✓ (6 lines, valid JSON) | ✓ (references texture thc:item/dough) | ✓ VERIFIED |
| `src/main/resources/assets/thc/textures/item/dough.png` | Dough texture | ✓ | ✓ (480 bytes) | ✓ (referenced by model) | ✓ VERIFIED |
| `src/main/resources/assets/thc/lang/en_us.json` | Dough translation | ✓ | ✓ (contains "item.thc.dough": "Dough") | ✓ (translation key matches item ID) | ✓ VERIFIED |
| `src/main/resources/data/thc/recipe/dough.json` | Iron bucket dough recipe | ✓ | ✓ (14 lines, valid JSON) | ✓ (result.id: thc:dough) | ✓ VERIFIED |
| `src/main/resources/data/thc/recipe/dough_copper.json` | Copper bucket dough recipe | ✓ | ✓ (14 lines, valid JSON) | ✓ (result.id: thc:dough) | ✓ VERIFIED |
| `src/main/resources/data/thc/recipe/dough_smelting.json` | Furnace cooking recipe | ✓ | ✓ (8 lines, valid JSON) | ✓ (ingredient: thc:dough, result: minecraft:bread) | ✓ VERIFIED |
| `src/main/resources/data/thc/recipe/dough_smoking.json` | Smoker cooking recipe | ✓ | ✓ (8 lines, valid JSON) | ✓ (ingredient: thc:dough, result: minecraft:bread) | ✓ VERIFIED |
| `data/minecraft/loot_table/entities/pig.json` | Pig loot with leather | ✓ | ✓ (102 lines, valid JSON) | ✓ (2 pools: leather + porkchop) | ✓ VERIFIED |
| `data/minecraft/loot_table/entities/sheep.json` | Sheep loot with leather | ✓ | ✓ (417 lines, valid JSON) | ✓ (3 pools: leather + mutton + 16 wool colors) | ✓ VERIFIED |

### Key Link Verification

| From | To | Via | Status | Details |
|------|---|----|--------|---------|
| RecipeManagerMixin | Vanilla bread recipe | REMOVED_RECIPE_PATHS.contains("bread") | ✓ WIRED | Recipe removal confirmed at line 43 |
| THCItems.DOUGH | Item registry | Registry.register(BuiltInRegistries.ITEM, key, item) | ✓ WIRED | DOUGH registered at lines 107-113 |
| THCItems.DOUGH | Food creative tab | ItemGroupEvents.modifyEntriesEvent(foodTabKey) | ✓ WIRED | Added to food tab at line 130 |
| dough.json recipe | THCItems.DOUGH | result.id: "thc:dough" | ✓ WIRED | Recipe creates dough item |
| dough_copper.json recipe | THCItems.DOUGH | result.id: "thc:dough" | ✓ WIRED | Recipe creates dough item |
| dough_smelting.json | minecraft:bread | result: "minecraft:bread" | ✓ WIRED | Smelting produces bread |
| dough_smoking.json | minecraft:bread | result: "minecraft:bread" | ✓ WIRED | Smoking produces bread |
| dough.json recipe | minecraft:water_bucket | ingredient: "minecraft:water_bucket" | ✓ WIRED | Uses vanilla water bucket (auto-returns via craftRemainder) |
| dough_copper.json recipe | COPPER_BUCKET_OF_WATER | ingredient: "thc:copper_bucket_of_water" | ✓ WIRED | Uses copper water bucket (returns via .craftRemainder(COPPER_BUCKET)) |
| pig.json leather pool | cow.json leather pool | Identical structure verified | ✓ WIRED | Pools match exactly (jq diff confirms) |
| sheep.json leather pool | cow.json leather pool | Identical structure verified | ✓ WIRED | Pools match exactly (jq diff confirms) |
| sheep.json | 16 wool color alternatives | pool[2].entries[0].children.length | ✓ WIRED | All 16 wool colors preserved |

### Requirements Coverage

| Requirement | Description | Status | Supporting Truths |
|-------------|-------------|--------|-------------------|
| FOOD-01 | Add bread recipe to REMOVED_RECIPE_PATHS | ✓ SATISFIED | Truth #1 |
| FOOD-02 | New dough item with custom texture, shapeless recipe preserving bucket | ✓ SATISFIED | Truths #2, #3, #4, #5 |
| FOOD-03 | Furnace/smoker recipe: dough -> bread | ✓ SATISFIED | Truths #6, #7 |
| FOOD-04 | Override pig loot table to include leather drops (0-2, +1 per looting) | ✓ SATISFIED | Truths #8, #9 |
| FOOD-05 | Override sheep loot table to include leather drops (0-2, +1 per looting) | ✓ SATISFIED | Truths #10, #11, #12 |

### Anti-Patterns Found

**None detected.**

Scanned files:
- RecipeManagerMixin.java: No TODOs, FIXMEs, or placeholders
- THCItems.kt: No TODOs, FIXMEs, or placeholders
- All recipe JSON files: Valid structure, no stub patterns
- All loot table JSON files: Valid structure, no stub patterns

### Build Verification

```bash
./gradlew build
```
**Result:** SUCCESS (build completed without errors)

### Structural Verification Details

**Bread Recipe Removal:**
- Line 43 of RecipeManagerMixin.java contains `"bread"` in REMOVED_RECIPE_PATHS
- Mixin injection at line 51-63 filters out recipes matching REMOVED_RECIPE_PATHS
- Recipe manager will exclude minecraft:bread from available recipes

**Dough Item Registration:**
- DOUGH item declared at lines 107-113 with 64 stack size
- Registered via `register("dough")` which calls Registry.register with key "thc:dough"
- Added to food creative tab at line 130 via ItemGroupEvents
- Translation exists: `"item.thc.dough": "Dough"`
- Model exists at assets/thc/models/item/dough.json
- Texture exists at assets/thc/textures/item/dough.png (480 bytes)

**Dough Recipes:**
- **dough.json**: 3 wheat + minecraft:water_bucket -> 1 thc:dough (iron bucket returns via vanilla craftRemainder)
- **dough_copper.json**: 3 wheat + thc:copper_bucket_of_water -> 1 thc:dough (copper bucket returns via .craftRemainder(COPPER_BUCKET) set in THCItems.kt)
- **dough_smelting.json**: thc:dough -> minecraft:bread (furnace, 200 ticks = 10 seconds, 0.35 XP)
- **dough_smoking.json**: thc:dough -> minecraft:bread (smoker, 100 ticks = 5 seconds, 0.35 XP)

**Bucket Preservation Mechanism:**
- Iron water bucket: Uses vanilla Item.craftRemainder property (automatically returns empty bucket)
- Copper water bucket: COPPER_BUCKET_OF_WATER has `.craftRemainder(COPPER_BUCKET)` set at THCItems.kt:83

**Leather Drop Rates:**
- **Cow baseline (cow.json):**
  - Pool[0]: 0-2 leather base, +0-1 per looting level
- **Pig (pig.json):**
  - Pool[0]: 0-2 leather base, +0-1 per looting level (IDENTICAL to cow via jq diff)
  - Pool[1]: 1-3 porkchop (unchanged from vanilla)
- **Sheep (sheep.json):**
  - Pool[0]: 0-2 leather base, +0-1 per looting level (IDENTICAL to cow via jq diff)
  - Pool[1]: 1-2 mutton (unchanged from vanilla)
  - Pool[2]: 16 wool color alternatives (all preserved: white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black)

**Leather Pool JSON Structure (verified identical across cow/pig/sheep):**
```json
{
  "bonus_rolls": 0.0,
  "entries": [{
    "type": "minecraft:item",
    "functions": [
      {
        "add": false,
        "count": {"type": "minecraft:uniform", "max": 2.0, "min": 0.0},
        "function": "minecraft:set_count"
      },
      {
        "count": {"type": "minecraft:uniform", "max": 1.0, "min": 0.0},
        "enchantment": "minecraft:looting",
        "function": "minecraft:enchanted_count_increase"
      }
    ],
    "name": "minecraft:leather"
  }],
  "rolls": 1.0
}
```

### Phase Success Criteria Verification

From v2.7-ROADMAP.md, Phase 64 success criteria:

1. **Bread cannot be crafted directly from wheat** → ✓ VERIFIED (REMOVED_RECIPE_PATHS contains "bread")
2. **Dough item exists and is craftable from 3 wheat + water bucket (copper or iron)** → ✓ VERIFIED (both dough.json and dough_copper.json exist and are valid)
3. **Dough smelts into bread in furnace/smoker** → ✓ VERIFIED (both smelting and smoking recipes exist with correct timing: 200/100 ticks)
4. **Pigs drop leather at same rate as cows** → ✓ VERIFIED (leather pools are identical via jq diff)
5. **Sheep drop leather at same rate as cows** → ✓ VERIFIED (leather pools are identical via jq diff)

**All 5 success criteria met.**

### Human Verification Required

None. All truths are structurally verifiable and have been confirmed via code inspection and build success.

**Optional in-game testing recommendations:**
1. Launch game and verify bread recipe is not available in crafting table
2. Craft dough with iron water bucket + 3 wheat, verify bucket returns
3. Craft dough with copper water bucket + 3 wheat, verify copper bucket returns
4. Smelt dough in furnace, verify it produces bread after 10 seconds
5. Cook dough in smoker, verify it produces bread after 5 seconds
6. Kill pigs with and without looting, verify leather drops (0-2 base, +1 per looting level)
7. Kill sheep with and without looting, verify leather drops (0-2 base, +1 per looting level)
8. Kill sheep of various colors, verify wool colors still drop correctly

---

_Verified: 2026-01-30T04:24:00Z_
_Verifier: Claude (gsd-verifier)_
