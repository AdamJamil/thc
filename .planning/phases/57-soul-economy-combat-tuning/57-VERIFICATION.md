---
phase: 57-soul-economy-combat-tuning
verified: 2026-01-29T04:46:56Z
status: passed
score: 9/9 must-haves verified
---

# Phase 57: Soul Economy & Combat Tuning Verification Report

**Phase Goal:** Players can farm soul dust from illagers and craft soul soil; arrow and melee balance adjusted
**Verified:** 2026-01-29T04:46:56Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Pillagers drop soul dust at 20% rate on death | ✓ VERIFIED | pillager.json contains thc:soul_dust pool with unenchanted_chance: 0.2 |
| 2 | Vindicators drop soul dust at 20% rate on death | ✓ VERIFIED | vindicator.json contains thc:soul_dust pool with unenchanted_chance: 0.2 |
| 3 | Evokers drop soul dust at 20% rate on death | ✓ VERIFIED | evoker.json contains thc:soul_dust pool with unenchanted_chance: 0.2 |
| 4 | Illusioners drop soul dust at 20% rate on death | ✓ VERIFIED | illusioner.json contains thc:soul_dust pool with unenchanted_chance: 0.2 |
| 5 | Ravagers drop soul dust at 20% rate on death | ✓ VERIFIED | ravager.json contains thc:soul_dust pool with unenchanted_chance: 0.2 |
| 6 | Witches drop soul dust at 20% rate on death | ✓ VERIFIED | witch.json contains thc:soul_dust pool with unenchanted_chance: 0.2 |
| 7 | 4 soul dust in 2x2 pattern crafts 1 soul soil | ✓ VERIFIED | soul_soil.json recipe exists with 2x2 pattern (DD/DD) producing minecraft:soul_soil |
| 8 | Arrow hits apply Speed III (not Speed IV) to targets | ✓ VERIFIED | AbstractArrowMixin.java line 67: MobEffects.SPEED with amplifier 2 (Speed III) |
| 9 | Melee pillagers deal 6.5 damage (not 4.5) | ✓ VERIFIED | DamageRebalancing.kt applies +0.444 modifier to iron sword pillagers |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `data/minecraft/loot_table/entities/pillager.json` | Soul dust pool added | ✓ VERIFIED | EXISTS (65 lines), SUBSTANTIVE (valid loot table), WIRED (contains thc:soul_dust entry) |
| `data/minecraft/loot_table/entities/vindicator.json` | Soul dust pool added | ✓ VERIFIED | EXISTS (63 lines), SUBSTANTIVE (valid loot table), WIRED (contains thc:soul_dust entry) |
| `data/minecraft/loot_table/entities/evoker.json` | Soul dust pool added | ✓ VERIFIED | EXISTS (73 lines), SUBSTANTIVE (valid loot table), WIRED (contains thc:soul_dust entry) |
| `data/minecraft/loot_table/entities/illusioner.json` | Soul dust pool added | ✓ VERIFIED | EXISTS (28 lines), SUBSTANTIVE (valid loot table), WIRED (contains thc:soul_dust entry) |
| `data/minecraft/loot_table/entities/ravager.json` | Soul dust pool replaces saddle | ✓ VERIFIED | EXISTS (28 lines), SUBSTANTIVE (valid loot table), WIRED (contains thc:soul_dust entry) |
| `data/minecraft/loot_table/entities/witch.json` | Soul dust pool added | ✓ VERIFIED | EXISTS (213 lines), SUBSTANTIVE (valid loot table), WIRED (contains thc:soul_dust entry) |
| `src/main/resources/data/thc/recipe/soul_soil.json` | 2x2 crafting recipe | ✓ VERIFIED | EXISTS (16 lines), SUBSTANTIVE (valid shaped recipe), WIRED (referenced in recipe system) |
| `src/main/java/thc/mixin/AbstractArrowMixin.java` | Arrow hit speed effect | ✓ VERIFIED | EXISTS (107 lines), SUBSTANTIVE (real implementation), WIRED (registered in thc.mixins.json) |
| `src/main/kotlin/thc/monster/DamageRebalancing.kt` | Melee pillager damage modifier | ✓ VERIFIED | EXISTS (101 lines), SUBSTANTIVE (real implementation), WIRED (imported and called in THC.kt) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| 6 loot tables | thc:soul_dust | loot table entry | ✓ WIRED | All 6 illager loot tables contain "name": "thc:soul_dust" entries |
| soul_soil.json | thc:soul_dust | recipe ingredient | ✓ WIRED | Recipe uses "D": "thc:soul_dust" in key pattern |
| AbstractArrowMixin.java | Speed effect | MobEffect application | ✓ WIRED | Line 67: target.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 2)) |
| DamageRebalancing.kt | PILLAGER entity | Equipment check | ✓ WIRED | Lines 89-91: type check + iron sword check, modifier applied to ATTACK_DAMAGE |
| AbstractArrowMixin | Mixin system | Mixin registration | ✓ WIRED | Registered in src/main/resources/thc.mixins.json line 6 |
| DamageRebalancing | Init system | Module registration | ✓ WIRED | Imported at THC.kt:31, called at THC.kt:74 |

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| SOUL-01: Illagers drop soul dust at 20% rate | ✓ SATISFIED | All 6 illager loot tables verified with unenchanted_chance: 0.2 |
| SOUL-02: 4 soul dust crafts 1 soul soil | ✓ SATISFIED | soul_soil.json verified with 2x2 pattern |
| CMBT-01: Arrow speed reduced to Speed III | ✓ SATISFIED | AbstractArrowMixin.java verified with amplifier 2 |
| CMBT-02: Melee pillager damage increased to 6.5 | ✓ SATISFIED | DamageRebalancing.kt verified with +0.444 modifier |

### Anti-Patterns Found

No anti-patterns detected. All implementations are substantive and complete.

### Detailed Verification Evidence

**Loot Table Drop Rates (all 6 verified):**
```bash
$ for file in pillager vindicator evoker illusioner ravager witch; do 
    echo -n "$file: "; 
    grep -c "thc:soul_dust" data/minecraft/loot_table/entities/${file}.json; 
  done
pillager: 1
vindicator: 1
evoker: 1
illusioner: 1
ravager: 1
witch: 1

$ for file in pillager vindicator evoker illusioner ravager witch; do 
    echo -n "$file 0.2: "; 
    grep -c '"unenchanted_chance": 0.2' data/minecraft/loot_table/entities/${file}.json; 
  done
pillager 0.2: 1
vindicator 0.2: 1
evoker 0.2: 1
illusioner 0.2: 1
ravager 0.2: 1
witch 0.2: 1
```

**Soul Soil Recipe (verified):**
```bash
$ cat src/main/resources/data/thc/recipe/soul_soil.json
{
  "type": "minecraft:crafting_shaped",
  "category": "building",
  "key": {
    "D": "thc:soul_dust"
  },
  "pattern": [
    "DD",
    "DD"
  ],
  "result": {
    "count": 1,
    "id": "minecraft:soul_soil"
  }
}
```

**Arrow Speed Effect (verified):**
```bash
$ grep -E "MobEffects\.SPEED.*\b2\b" src/main/java/thc/mixin/AbstractArrowMixin.java
target.addEffect(new MobEffectInstance(MobEffects.SPEED, THC_EFFECT_DURATION_TICKS, 2), player);
```

Line 66 comment also updated: "Apply Speed 3" (not shown in grep but verified in file read).

**Pillager Damage Modifier (verified):**
```bash
$ grep "0.444" src/main/kotlin/thc/monster/DamageRebalancing.kt
 * Multiplier: 6.5 / 4.5 = 1.444, modifier = +0.444
    AttributeModifier(PILLAGER_MELEE_DAMAGE_ID, 0.444, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
```

Equipment check verified at lines 89-91:
```kotlin
if (mob.type != EntityType.PILLAGER) return
// Only melee variants have iron sword in mainhand
if (!mob.mainHandItem.`is`(Items.IRON_SWORD)) return
```

**Wiring Verification:**
- AbstractArrowMixin registered in thc.mixins.json (line 6)
- DamageRebalancing imported and initialized in THC.kt (lines 31, 74)
- Recipe file placed in correct data pack location (src/main/resources/data/thc/recipe/)
- All loot tables in correct location (data/minecraft/loot_table/entities/)

### Commits Verified

1. `7cc79e6` - feat(57-01): add soul dust drops and soul soil recipe
2. `911547d` - feat(57-01): reduce arrow speed effect to Speed III
3. `18eca38` - feat(57-01): increase melee pillager damage to 6.5

All three commits present in git history and correspond to implemented features.

### Level-by-Level Artifact Checks

**Level 1 (Existence):** ✓ All 9 artifacts exist
- 6 loot tables: pillager, vindicator, evoker, illusioner, ravager, witch
- 1 recipe: soul_soil.json
- 2 code files: AbstractArrowMixin.java, DamageRebalancing.kt

**Level 2 (Substantive):** ✓ All artifacts have real implementations
- Loot tables: Complete JSON structures with proper pool/entry/condition format
- Recipe: Valid shaped crafting recipe with correct pattern and ingredients
- AbstractArrowMixin: 107 lines with full arrow hit logic
- DamageRebalancing: 101 lines with complete damage modifier system

**Level 3 (Wired):** ✓ All artifacts properly connected
- Loot tables: Loaded by Minecraft data pack system (in data/minecraft/)
- Recipe: Loaded by Minecraft data pack system (in data/thc/recipe/)
- AbstractArrowMixin: Registered in mixin config
- DamageRebalancing: Imported and called in main mod class

### Success Criteria Verification

From ROADMAP Phase 57 success criteria:

1. ✓ **Pillagers, Vindicators, Evokers, Illusioners, Ravagers, and Witches drop soul dust at 20% rate on death**
   - Evidence: All 6 loot tables contain soul dust pool with unenchanted_chance: 0.2

2. ✓ **4 soul dust in 2x2 crafting grid produces 1 soul soil**
   - Evidence: soul_soil.json recipe with pattern ["DD", "DD"] and result "minecraft:soul_soil"

3. ✓ **Arrow hit speed effect is Speed III (not Speed IV)**
   - Evidence: AbstractArrowMixin.java line 67 uses amplifier 2 (Speed III)

4. ✓ **Melee pillager attacks deal 6.5 damage (up from 4.5)**
   - Evidence: DamageRebalancing.kt applies +44.4% modifier to iron sword pillagers

---

**Overall Status:** PASSED

All 9 truths verified. All 9 artifacts exist, are substantive, and are properly wired. All 4 requirements satisfied. All 4 success criteria met. Phase goal achieved.

---

_Verified: 2026-01-29T04:46:56Z_
_Verifier: Claude (gsd-verifier)_
