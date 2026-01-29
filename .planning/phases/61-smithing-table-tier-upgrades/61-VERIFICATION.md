---
phase: 61-smithing-table-tier-upgrades
verified: 2026-01-29T19:53:43Z
status: passed
score: 11/11 must-haves verified
---

# Phase 61: Smithing Table Tier Upgrades Verification Report

**Phase Goal:** Players can upgrade equipment tiers at smithing tables, preserving enchantments and using crafting-equivalent material costs
**Verified:** 2026-01-29T19:53:43Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Leather armor upgrades to copper at smithing table | ✓ VERIFIED | 4 recipes exist + TierUpgradeConfig maps LEATHER_* + COPPER_INGOT -> COPPER_* for all armor pieces |
| 2 | Copper armor upgrades to iron at smithing table | ✓ VERIFIED | 4 recipes exist + TierUpgradeConfig maps COPPER_* + IRON_INGOT -> IRON_* for all armor pieces |
| 3 | Iron armor upgrades to diamond at smithing table | ✓ VERIFIED | 4 recipes exist + TierUpgradeConfig maps IRON_* + DIAMOND -> DIAMOND_* for all armor pieces |
| 4 | Wooden tools upgrade to stone at smithing table | ✓ VERIFIED | 5 recipes exist + TierUpgradeConfig maps WOODEN_* + SMOOTH_STONE -> STONE_* for all 5 tools |
| 5 | Stone tools upgrade to copper at smithing table | ✓ VERIFIED | 5 recipes exist + TierUpgradeConfig maps STONE_* + COPPER_INGOT -> COPPER_* for all 5 tools |
| 6 | Copper tools upgrade to iron at smithing table | ✓ VERIFIED | 5 recipes exist + TierUpgradeConfig maps COPPER_* + IRON_INGOT -> IRON_* for all 5 tools |
| 7 | Iron tools upgrade to diamond at smithing table | ✓ VERIFIED | 5 recipes exist + TierUpgradeConfig maps IRON_* + DIAMOND -> DIAMOND_* for all 5 tools |
| 8 | Upgrades require crafting-equivalent material counts | ✓ VERIFIED | ARMOR_MATERIAL_COUNTS (5/8/7/4) and TOOL_MATERIAL_COUNTS (3/3/1/2/2) match vanilla recipes; SmithingMenuMixin validates count before upgrade |
| 9 | Enchantments preserved during tier upgrade | ✓ VERIFIED | SmithingMenuMixin line 74: `result.applyComponents(base.getComponents())` copies all components including enchantments |
| 10 | Durability restored to maximum after upgrade | ✓ VERIFIED | SmithingMenuMixin line 75: `result.remove(DataComponents.DAMAGE)` removes damage component |
| 11 | Existing diamond to netherite upgrade unchanged | ✓ VERIFIED | Diamond items NEVER appear as base in VALID_TIER_UPGRADES; NETHERITE_INGOT not in config; mixin returns early if not valid tier upgrade |
| 12 | Smithing table craftable with copper ingots | ✓ VERIFIED | smithing_table_copper.json exists with shaped recipe (2 copper ingots + 4 planks) |

**Score:** 12/12 truths verified (all 11 success criteria + copper recipe)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/kotlin/thc/smithing/TierUpgradeConfig.kt` | Material count mappings and upgrade paths | ✓ VERIFIED | 163 lines; ARMOR_MATERIAL_COUNTS (16 entries), TOOL_MATERIAL_COUNTS (20 entries), VALID_TIER_UPGRADES (32 entries); no stubs |
| `src/main/java/thc/mixin/SmithingMenuMixin.java` | SmithingMenu interception for validation and consumption | ✓ VERIFIED | 107 lines; registered in thc.mixins.json; injects at createResult HEAD and onTake RETURN; no stubs |
| `src/main/resources/thc.mixins.json` | SmithingMenuMixin registration | ✓ VERIFIED | Contains "SmithingMenuMixin" entry |
| Armor recipes (12 files) | Tier upgrade recipes for all armor pieces | ✓ VERIFIED | 4 leather->copper + 4 copper->iron + 4 iron->diamond; all use smithing_transform type with barrier template |
| Tool recipes (20 files) | Tier upgrade recipes for all tools | ✓ VERIFIED | 5 wooden->stone + 5 stone->copper + 5 copper->iron + 5 iron->diamond; all use smithing_transform type with barrier template |
| `src/main/resources/data/thc/recipe/smithing_table_copper.json` | Copper smithing table crafting recipe | ✓ VERIFIED | Shaped recipe with 2 copper_ingot + 4 planks -> smithing_table |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| SmithingMenuMixin.java | TierUpgradeConfig.kt | getRequiredMaterialCount call | ✓ WIRED | Import exists; used 2x in mixin (lines 54, 97) |
| SmithingMenuMixin.java | TierUpgradeConfig.kt | isValidTierUpgrade call | ✓ WIRED | Import exists; used 1x in mixin (line 46) |
| SmithingMenuMixin.java | TierUpgradeConfig.kt | getUpgradeResult call | ✓ WIRED | Import exists; used 1x in mixin (line 65) |
| SmithingMenuMixin.java | DataComponents.DAMAGE | durability restoration | ✓ WIRED | Line 75: `result.remove(DataComponents.DAMAGE)` |
| Recipes | TierUpgradeConfig | upgrade path validation | ✓ WIRED | All 32 recipes have corresponding VALID_TIER_UPGRADES entries |

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| SMTH-01: Leather to copper armor upgrade | ✓ SATISFIED | None - 4 recipes + config entries verified |
| SMTH-02: Copper to iron armor upgrade | ✓ SATISFIED | None - 4 recipes + config entries verified |
| SMTH-03: Iron to diamond armor upgrade | ✓ SATISFIED | None - 4 recipes + config entries verified |
| SMTH-04: Wooden to stone tool upgrade | ✓ SATISFIED | None - 5 recipes + config entries verified |
| SMTH-05: Stone to copper tool upgrade | ✓ SATISFIED | None - 5 recipes + config entries verified |
| SMTH-06: Copper to iron tool upgrade | ✓ SATISFIED | None - 5 recipes + config entries verified |
| SMTH-07: Iron to diamond tool upgrade | ✓ SATISFIED | None - 5 recipes + config entries verified |
| SMTH-08: Tier upgrades preserve enchantments | ✓ SATISFIED | None - applyComponents() verified in mixin |
| SMTH-09: Tier upgrades restore durability | ✓ SATISFIED | None - DAMAGE removal verified in mixin |
| SMTH-10: Diamond to netherite unchanged | ✓ SATISFIED | None - diamond not in base upgrade map, netherite not mentioned |
| SMTH-11: Smithing table copper recipe | ✓ SATISFIED | None - recipe file exists and verified |

**Coverage:** 11/11 requirements satisfied

### Anti-Patterns Found

None detected.

**Scan Results:**
- No TODO/FIXME comments in implementation files
- No placeholder text or stub patterns
- No empty return statements
- All functions have substantive implementations
- File lengths appropriate (163 lines config, 107 lines mixin)

### Human Verification Required

#### 1. Armor Tier Upgrade with Enchantment Preservation

**Test:** 
1. Enchant leather armor with Protection I using enchanting table
2. Place enchanted leather helmet in smithing table base slot
3. Place 5 copper ingots in addition slot
4. Take copper helmet result

**Expected:** 
- Copper helmet has full durability
- Copper helmet has Protection I enchantment
- 5 copper ingots consumed from inventory

**Why human:** Visual verification of enchantment preservation and durability state requires in-game testing

#### 2. Tool Tier Upgrade Material Count Validation

**Test:**
1. Place iron pickaxe in smithing table base slot
2. Place only 2 diamonds in addition slot (need 3)
3. Observe result slot

**Expected:**
- Result slot shows nothing (upgrade blocked)
- Add 1 more diamond (total 3)
- Result slot now shows diamond pickaxe
- Take pickaxe, verify 3 diamonds consumed

**Why human:** Material count validation requires testing insufficient vs sufficient materials in-game

#### 3. Netherite Upgrade Compatibility

**Test:**
1. Place diamond chestplate in smithing table base slot
2. Place netherite upgrade smithing template in template slot
3. Place netherite ingot in addition slot
4. Observe result

**Expected:**
- Result shows netherite chestplate (vanilla upgrade works)
- Enchantments preserved
- Only 1 netherite ingot consumed (not affected by tier upgrade mixin)

**Why human:** Verifying tier upgrade mixin doesn't interfere with vanilla netherite upgrade requires in-game test

#### 4. Copper Smithing Table Crafting

**Test:**
1. Open crafting table
2. Place 2 copper ingots in top 2 slots
3. Place 4 planks (any type) in bottom 4 slots
4. Observe result

**Expected:**
- Result shows smithing table
- Can also craft with iron ingots (vanilla recipe still works)

**Why human:** Recipe book display and crafting validation requires in-game check

#### 5. Durability Restoration Verification

**Test:**
1. Use iron sword until durability is at 50%
2. Enchant damaged iron sword with Sharpness I
3. Place damaged, enchanted iron sword in smithing table
4. Add 2 diamonds, upgrade to diamond sword
5. Check diamond sword

**Expected:**
- Diamond sword has full durability (250/250, not transferred from iron)
- Diamond sword has Sharpness I enchantment

**Why human:** Durability restoration vs transfer requires visual inspection in-game

---

_Verified: 2026-01-29T19:53:43Z_
_Verifier: Claude (gsd-verifier)_
