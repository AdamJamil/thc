---
phase: 31-apple-bonemeal
verified: 2026-01-22T19:00:00Z
status: passed
score: 4/4 must-haves verified
re_verification: false
---

# Phase 31: Apple & Bonemeal Verification Report

**Phase Goal:** Improve early-game food availability and farming
**Verified:** 2026-01-22T19:00:00Z
**Status:** passed
**Re-verification:** No -- initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Apples drop from any leaf type when broken | VERIFIED | All 9 leaf loot tables contain `minecraft:apple` entry |
| 2 | Apple drop rate is approximately 5x vanilla (2.5% base vs 0.5%) | VERIFIED | All 9 loot tables use `"chances": [0.025, 0.027777778, 0.03125, 0.041666668, 0.125]` |
| 3 | Bonemeal fully matures any crop in one use | VERIFIED | CropBlockMixin HEAD-injects performBonemeal, calls `getStateForAge(getMaxAge())`, cancels original |
| 4 | Bone crafts into 6 bonemeal | VERIFIED | `bone_meal.json` has `"count": 6`; RecipeManagerMixin filters vanilla `bone_meal` recipe |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/data/minecraft/loot_table/blocks/oak_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/dark_oak_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/birch_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/spruce_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/jungle_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/acacia_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/cherry_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance |
| `src/main/resources/data/minecraft/loot_table/blocks/azalea_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance; correct sapling (`minecraft:azalea`) |
| `src/main/resources/data/minecraft/loot_table/blocks/mangrove_leaves.json` | Apple drops at 5x rate | VERIFIED | Contains `minecraft:apple` with 0.025 base chance; correct sapling (`minecraft:mangrove_propagule`) |
| `src/main/java/thc/mixin/CropBlockMixin.java` | Instant crop maturation | VERIFIED | 35 lines, HEAD injection on `performBonemeal`, sets max age, cancels original |
| `src/main/resources/data/minecraft/recipe/bone_meal.json` | 6 bonemeal from 1 bone | VERIFIED | Shapeless recipe with `"count": 6` |
| `src/main/java/thc/mixin/RecipeManagerMixin.java` | Filters vanilla bone_meal recipe | VERIFIED | `REMOVED_RECIPE_PATHS` includes `"bone_meal"` |
| `src/main/resources/thc.mixins.json` | CropBlockMixin registered | VERIFIED | Contains `"CropBlockMixin"` in mixins array |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CropBlockMixin.java` | `CropBlock.performBonemeal` | HEAD injection with ci.cancel() | VERIFIED | Lines 26-34: `@Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)` followed by `ci.cancel()` |
| `bone_meal.json` | `RecipeManagerMixin` | vanilla recipe filtered | VERIFIED | Line 35 of RecipeManagerMixin: `"bone_meal"` in REMOVED_RECIPE_PATHS set |
| `CropBlockMixin` | `thc.mixins.json` | mixin registration | VERIFIED | Line 7: `"CropBlockMixin"` in mixins array |

### Requirements Coverage

All success criteria from ROADMAP.md verified:

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Apples drop from any leaf type (oak, birch, spruce, jungle, acacia, dark oak, cherry, azalea, mangrove) | SATISFIED | 9 loot table files, each with apple entry |
| Apple drop rate is 5x vanilla | SATISFIED | All use 0.025 base (5x 0.005 vanilla) |
| Bonemeal fully matures any crop in one use | SATISFIED | CropBlockMixin sets to getMaxAge() |
| Bone crafts into 6 bonemeal (not 3) | SATISFIED | Custom recipe with count: 6, vanilla filtered |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | - |

No anti-patterns detected. CropBlockMixin is substantive (35 lines), has proper exports, no TODOs/FIXMEs/placeholders.

### Human Verification Required

None. All success criteria can be verified programmatically through code inspection.

### Build Verification

`./gradlew build` succeeds without errors.

### Commits

| Hash | Message |
|------|---------|
| 8425451 | feat(31-01): add 5x apple drops to all leaf types |
| 2ef97a0 | feat(31-01): instant crop maturation on bonemeal |
| 870a919 | feat(31-01): double bonemeal yield from bones (6 instead of 3) |
| 4f18c8a | docs(31-01): complete apple and bonemeal plan |

---

*Verified: 2026-01-22T19:00:00Z*
*Verifier: Claude (gsd-verifier)*
