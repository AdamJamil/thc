---
phase: 30-smoker-gating
verified: 2026-01-22T23:45:00Z
status: passed
score: 4/4 must-haves verified
---

# Phase 30: Smoker Gating Verification Report

**Phase Goal:** Gate smoker behind iron acquisition
**Verified:** 2026-01-22T23:45:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Vanilla smoker recipe is uncraftable | VERIFIED | `RecipeManagerMixin.java:34` contains `"smoker"` in REMOVED_RECIPE_PATHS set |
| 2 | Custom smoker recipe requires 2 iron ingots in top corners | VERIFIED | `smoker.json` pattern `"ILI"` with `"I": "minecraft:iron_ingot"` |
| 3 | Smokers do not spawn in newly generated villages | VERIFIED | `StructureTemplateMixin.java:34` contains `Blocks.SMOKER` in FILTERED_STRUCTURE_BLOCKS set |
| 4 | Existing smoker block functionality is unchanged | VERIFIED | No modifications to smoker block behavior; only recipe and structure generation affected |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/data/minecraft/recipe/smoker.json` | Custom smoker recipe with iron requirement | EXISTS, SUBSTANTIVE, WIRED | 16 lines, valid JSON, shaped recipe with iron_ingot key |
| `src/main/java/thc/mixin/RecipeManagerMixin.java` | Recipe filtering including smoker | EXISTS, SUBSTANTIVE, WIRED | 55 lines, "smoker" in REMOVED_RECIPE_PATHS, registered in thc.mixins.json |
| `src/main/java/thc/mixin/StructureTemplateMixin.java` | Structure block filtering including smoker | EXISTS, SUBSTANTIVE, WIRED | 62 lines, Blocks.SMOKER in FILTERED_STRUCTURE_BLOCKS, registered in thc.mixins.json |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| RecipeManagerMixin.java | REMOVED_RECIPE_PATHS | "smoker" string in set | WIRED | Line 34: `"smoker"` present in Set.of() |
| StructureTemplateMixin.java | FILTERED_STRUCTURE_BLOCKS | Blocks.SMOKER in set | WIRED | Line 34: `Blocks.SMOKER` present in Set.of() |
| thc.mixins.json | RecipeManagerMixin | mixin registration | WIRED | "RecipeManagerMixin" in mixins array |
| thc.mixins.json | StructureTemplateMixin | mixin registration | WIRED | "StructureTemplateMixin" in mixins array |

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| Smoker recipe requires 2 iron ingots in top corners | SATISFIED | - |
| Smokers do not spawn naturally in villages | SATISFIED | - |
| Existing smoker functionality unchanged | SATISFIED | - |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| - | - | - | - | No anti-patterns found |

### Human Verification Required

None required. All success criteria can be verified programmatically:
- Build succeeds (verified: `./gradlew build` passes)
- Recipe contains iron (verified: JSON inspection)
- Mixins registered (verified: thc.mixins.json inspection)
- Code contains expected strings (verified: grep)

### Gaps Summary

No gaps found. All phase success criteria are met:

1. **Smoker recipe requires 2 iron ingots in top corners** - Custom recipe at `data/minecraft/recipe/smoker.json` uses pattern `ILI` with `I` mapped to `minecraft:iron_ingot`. The pattern places iron in positions 0 and 2 of the top row (both top corners).

2. **Smokers do not spawn naturally in villages** - `StructureTemplateMixin.java` adds `Blocks.SMOKER` to `FILTERED_STRUCTURE_BLOCKS`, which prevents smoker block placement during structure generation via the `thc$filterFurnaceBlocks` redirect.

3. **Existing smoker functionality unchanged** - No modifications to smoker block behavior, only recipe availability and structure generation. Players who obtain a smoker (via the new recipe) can use it normally.

---

*Verified: 2026-01-22T23:45:00Z*
*Verifier: Claude (gsd-verifier)*
