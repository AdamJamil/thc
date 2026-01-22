---
phase: 25-furnace-gating
verified: 2026-01-22T16:57:32Z
status: passed
score: 4/4 must-haves verified
---

# Phase 25: Furnace Gating Verification Report

**Phase Goal:** Furnace progression gated behind Nether access
**Verified:** 2026-01-22T16:57:32Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Furnace cannot be crafted with vanilla recipe (requires blaze powder) | VERIFIED | `furnace` in REMOVED_RECIPE_PATHS (line 32) + custom recipe with `minecraft:blaze_powder` (furnace.json line 6) |
| 2 | Furnaces do not spawn naturally in villages | VERIFIED | StructureTemplateMixin filters `Blocks.FURNACE` from structure placement (line 32) |
| 3 | Blast furnace requires furnace + blast totem to craft | VERIFIED | `blast_furnace` in REMOVED_RECIPE_PATHS (line 33) + custom recipe with `thc:blast_totem` (blast_furnace.json line 6) |
| 4 | Blast furnaces do not spawn naturally in villages | VERIFIED | StructureTemplateMixin filters `Blocks.BLAST_FURNACE` from structure placement (line 33) |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/data/minecraft/recipe/furnace.json` | Custom furnace recipe with blaze powder | EXISTS, SUBSTANTIVE, WIRED | 16 lines, valid JSON, uses `minecraft:blaze_powder` center + 8 cobblestone, results in `minecraft:furnace` |
| `src/main/resources/data/thc/recipe/blast_furnace.json` | Custom blast furnace recipe with blast totem | EXISTS, SUBSTANTIVE, WIRED | 11 lines, valid JSON, shapeless recipe with `minecraft:furnace` + `thc:blast_totem`, results in `minecraft:blast_furnace` |
| `src/main/java/thc/mixin/RecipeManagerMixin.java` | Vanilla recipe removal for furnace and blast furnace | EXISTS, SUBSTANTIVE, WIRED | 54 lines, contains "furnace" and "blast_furnace" in REMOVED_RECIPE_PATHS, registered in thc.mixins.json |
| `src/main/java/thc/mixin/StructureTemplateMixin.java` | Structure block filtering for furnaces | EXISTS, SUBSTANTIVE, WIRED | 61 lines, FILTERED_STRUCTURE_BLOCKS contains Blocks.FURNACE and Blocks.BLAST_FURNACE, @Redirect on setBlock, registered in thc.mixins.json |
| `src/main/resources/thc.mixins.json` | Mixin registration | EXISTS, SUBSTANTIVE | Contains both "RecipeManagerMixin" (line 25) and "StructureTemplateMixin" (line 30) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| RecipeManagerMixin.java | vanilla recipes | REMOVED_RECIPE_PATHS set filtering | WIRED | Set contains "furnace" and "blast_furnace", filtering logic in prepare() method removes matching recipes |
| StructureTemplateMixin.java | StructureTemplate.placeInWorld | @Redirect on setBlock | WIRED | @Redirect intercepts ServerLevelAccessor.setBlock calls, skips furnace blocks |
| furnace.json | Minecraft recipe system | minecraft namespace override | WIRED | Placed in data/minecraft/recipe/ to override vanilla recipe |
| blast_furnace.json | Minecraft recipe system | thc namespace recipe | WIRED | Placed in data/thc/recipe/ and adds thc:blast_totem dependency |
| blast_totem item | THCItems registry | Kotlin registration | WIRED | BLAST_TOTEM registered in THCItems.kt (Phase 24), referenced by recipe |

### Requirements Coverage

| Requirement | Status | Notes |
|-------------|--------|-------|
| PROG-03: Furnace recipe gating | SATISFIED | Vanilla recipe removed, custom requires blaze powder |
| PROG-04: Furnace village removal | SATISFIED | StructureTemplateMixin filters Blocks.FURNACE |
| PROG-05: Blast furnace recipe gating | SATISFIED | Vanilla recipe removed, custom requires blast totem |
| PROG-06: Blast furnace village removal | SATISFIED | StructureTemplateMixin filters Blocks.BLAST_FURNACE |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None found | - | - | - | - |

No TODO, FIXME, placeholder, or stub patterns detected in phase artifacts.

### Human Verification Required

None required. All success criteria are verifiable through code inspection:
- Recipe contents verified via file inspection
- Recipe removal verified via REMOVED_RECIPE_PATHS set membership
- Structure filtering verified via FILTERED_STRUCTURE_BLOCKS set membership
- Mixin registration verified via thc.mixins.json entries
- Build succeeds (BUILD SUCCESSFUL)

### Build Verification

```
./gradlew build
BUILD SUCCESSFUL in 7s
11 actionable tasks: 11 up-to-date
```

All mixins compile without errors.

---

*Verified: 2026-01-22T16:57:32Z*
*Verifier: Claude (gsd-verifier)*
