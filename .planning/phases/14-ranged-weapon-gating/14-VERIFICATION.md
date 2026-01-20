---
phase: 14-ranged-weapon-gating
verified: 2026-01-20T00:13:18Z
status: passed
score: 6/6 must-haves verified
---

# Phase 14: Ranged Weapon Gating Verification Report

**Phase Goal:** Bows and crossbows require Trial Chamber materials
**Verified:** 2026-01-20T00:13:18Z
**Status:** passed
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Bow recipe requires 3 breeze rods + 3 string (no sticks) | VERIFIED | bow.json pattern " RS", "R S", " RS" with R=breeze_rod, S=string. Counted: 3 breeze rods, 3 string, no sticks. |
| 2 | Crossbow recipe requires breeze rod + diamond (no sticks/iron) | VERIFIED | crossbow.json pattern "RDR", "STS", " R " with R=breeze_rod, D=diamond, S=string, T=tripwire_hook. Contains 3 breeze rods + 1 diamond, no sticks or iron. |
| 3 | Bows do not appear in any overworld chest loot tables | VERIFIED | THC.kt line 74: Items.BOW in removedItems; line 77-79: LootTableEvents.MODIFY_DROPS filters all removedItems from all drops. |
| 4 | Crossbows do not appear in any overworld chest loot tables | VERIFIED | THC.kt line 75: Items.CROSSBOW in removedItems; same MODIFY_DROPS filter applies to all loot tables. |
| 5 | Bows do not drop from any mob (skeleton, stray, etc.) | VERIFIED | Items.BOW in removedItems set; MODIFY_DROPS applies to all mob drops (verified by existing mod pattern for spear/shield removal). |
| 6 | Crossbows do not drop from any mob (pillager, piglin) | VERIFIED | Items.CROSSBOW in removedItems set; same universal MODIFY_DROPS filter. |

**Score:** 6/6 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/data/minecraft/recipe/bow.json` | Shaped recipe with breeze_rod | VERIFIED | 16 lines, valid JSON, contains breeze_rod (2 uses), results in minecraft:bow |
| `src/main/resources/data/minecraft/recipe/crossbow.json` | Shaped recipe with breeze_rod + diamond | VERIFIED | 18 lines, valid JSON, contains breeze_rod and diamond, results in minecraft:crossbow |
| `src/main/kotlin/thc/THC.kt` | Extended removedItems with BOW and CROSSBOW | VERIFIED | Line 74: Items.BOW, Line 75: Items.CROSSBOW in removedItems set |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| bow.json | minecraft:bow result | crafting_shaped recipe | WIRED | Valid recipe JSON in data/minecraft/recipe/, will override vanilla |
| crossbow.json | minecraft:crossbow result | crafting_shaped recipe | WIRED | Valid recipe JSON in data/minecraft/recipe/, will override vanilla |
| THC.kt removedItems | LootTableEvents.MODIFY_DROPS | drops.removeIf filter | WIRED | Line 78: `drops.removeIf { stack -> removedItems.any { stack.is(it) } }` |

### Artifact Detail Verification

**Level 1 (Existence):**
- bow.json: EXISTS (16 lines)
- crossbow.json: EXISTS (18 lines)
- THC.kt: EXISTS (160 lines)

**Level 2 (Substantive):**
- bow.json: SUBSTANTIVE - Complete shaped recipe with all required fields (type, category, key, pattern, result)
- crossbow.json: SUBSTANTIVE - Complete shaped recipe with all required fields
- THC.kt: SUBSTANTIVE - Items.BOW and Items.CROSSBOW present in removedItems set (lines 74-75)

**Level 3 (Wired):**
- Recipe files: Located in `data/minecraft/recipe/` which is automatically loaded by Fabric datapack system
- removedItems: Connected to LootTableEvents.MODIFY_DROPS.register() callback (line 77)
- Filter logic: Uses `drops.removeIf` with `removedItems.any` pattern (line 78)

### Requirements Coverage

| Requirement | Status | Blocking Issue |
|-------------|--------|----------------|
| RANGED-01: Bow recipe with breeze rods | SATISFIED | - |
| RANGED-02: Crossbow recipe with breeze rod + diamond | SATISFIED | - |
| RANGED-03: No bows in loot tables | SATISFIED | - |
| RANGED-04: No crossbows in loot tables | SATISFIED | - |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | - | - | - | - |

No anti-patterns detected. No TODOs, FIXMEs, placeholder text, or stub implementations found in the modified files.

### Human Verification Required

None required. All success criteria can be verified programmatically:
1. Recipe files contain correct ingredients (verified via JSON parsing)
2. Loot table filter uses correct items (verified via code inspection)
3. Build passes (verified via ./gradlew build)

### Summary

Phase 14 goal fully achieved. All six success criteria verified:

1. **Bow recipe** correctly requires 3 breeze rods + 3 string with no sticks
2. **Crossbow recipe** correctly requires 3 breeze rods + 1 diamond + tripwire hook + 2 string with no sticks or iron
3. **Loot filtering** removes both BOW and CROSSBOW from all loot drops via the existing removedItems/MODIFY_DROPS pattern

The implementation uses two mechanisms:
- **Recipe overrides** via datapack JSONs that replace vanilla recipes
- **Loot removal** via Fabric's LootTableEvents.MODIFY_DROPS API with a universal filter

Both mechanisms are well-established patterns used elsewhere in the mod (ladder recipe, spear/shield removal).

---

*Verified: 2026-01-20T00:13:18Z*
*Verifier: Claude (gsd-verifier)*
