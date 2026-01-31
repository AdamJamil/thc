---
phase: 67-job-restriction
verified: 2026-01-31T16:52:06Z
status: passed
score: 7/7 must-haves verified
re_verification: false
---

# Phase 67: Job Restriction Verification Report

**Phase Goal:** Villagers can only have mason, librarian, butcher, or cartographer professions
**Verified:** 2026-01-31T16:52:06Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Villagers cannot acquire disallowed professions | ✓ VERIFIED | VillagerProfessionMixin intercepts setVillagerData at HEAD, checks AllowedProfessions.isAllowed(), forces NONE if disallowed |
| 2 | Attempts to set disallowed profession result in NONE profession instead | ✓ VERIFIED | Line 58-69 of VillagerProfessionMixin: creates fixed VillagerData with getNoneHolder(), calls setVillagerData(fixed), cancels original |
| 3 | Zombie villager cures with disallowed professions become jobless | ✓ VERIFIED | setVillagerData intercepts all profession changes including cure completion |
| 4 | Naturally spawned villagers with disallowed professions become jobless | ✓ VERIFIED | setVillagerData intercepts NBT loading, covers structure spawns |
| 5 | Disallowed job blocks do not register as POI | ✓ VERIFIED | ServerLevelPoiMixin line 51: checks isDisallowedJobBlock(newBlock) and cancels POI registration |
| 6 | Villagers cannot see brewing stands, smithing tables, etc. as workstations | ✓ VERIFIED | POI blocking prevents villager AI from detecting these as job sites |
| 7 | Only lecterns, smokers, stonecutters, and cartography tables grant professions | ✓ VERIFIED | DISALLOWED_JOB_BLOCKS excludes these 4 blocks, POI registers normally for them |

**Score:** 7/7 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/villager/AllowedProfessions.java` | Profession validation and job block constants | ✓ VERIFIED | 99 lines, exports ALLOWED (6 professions), DISALLOWED_JOB_BLOCKS (12 blocks), 3 static methods |
| `src/main/java/thc/mixin/VillagerProfessionMixin.java` | setVillagerData interception | ✓ VERIFIED | 72 lines, @Inject at HEAD of setVillagerData with cancellable=true, calls AllowedProfessions.isAllowed() |
| `src/main/java/thc/mixin/ServerLevelPoiMixin.java` | POI blocking for disallowed job blocks | ✓ VERIFIED | 55 lines, extends existing POI mixin with isDisallowedJobBlock() check |
| thc.mixins.json | VillagerProfessionMixin registration | ✓ VERIFIED | Line 58: "VillagerProfessionMixin" registered |

**Artifact Verification Details:**

**AllowedProfessions.java:**
- Level 1 (Exists): ✓ PASS
- Level 2 (Substantive): ✓ PASS (99 lines, no stub patterns, exports 3 methods)
- Level 3 (Wired): ✓ PASS (imported by VillagerProfessionMixin and ServerLevelPoiMixin)

**VillagerProfessionMixin.java:**
- Level 1 (Exists): ✓ PASS
- Level 2 (Substantive): ✓ PASS (72 lines, no stub patterns, full interception logic)
- Level 3 (Wired): ✓ PASS (registered in thc.mixins.json line 58)

**ServerLevelPoiMixin.java:**
- Level 1 (Exists): ✓ PASS
- Level 2 (Substantive): ✓ PASS (55 lines, no stub patterns, complete POI blocking)
- Level 3 (Wired): ✓ PASS (registered in thc.mixins.json line 49)

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| VillagerProfessionMixin.java | AllowedProfessions.java | isAllowed() check | ✓ WIRED | Line 58: `if (!AllowedProfessions.isAllowed(profKey))` |
| VillagerProfessionMixin.java | AllowedProfessions.java | getNoneHolder() call | ✓ WIRED | Line 62: `AllowedProfessions.getNoneHolder(registryAccess)` used to create fixed VillagerData |
| ServerLevelPoiMixin.java | AllowedProfessions.java | isDisallowedJobBlock() check | ✓ WIRED | Line 51: `if (AllowedProfessions.isDisallowedJobBlock(newBlock))` |
| VillagerProfessionMixin | Villager.setVillagerData | @Inject at HEAD | ✓ WIRED | Line 51: `@Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)` |
| VillagerProfessionMixin | CallbackInfo.cancel | ci.cancel() | ✓ WIRED | Line 69: cancels original call when disallowed profession detected |
| ServerLevelPoiMixin | POI registration | ci.cancel() | ✓ WIRED | Lines 45, 52: cancels POI registration for disallowed blocks |

### Requirements Coverage

| Requirement | Status | Supporting Truths |
|-------------|--------|-------------------|
| VJOB-01: Only 4 professions allowed | ✓ SATISFIED | Truth 1 (profession restriction at setVillagerData) |
| VJOB-02: Disallowed professions reset to NONE | ✓ SATISFIED | Truth 2 (forced NONE via getNoneHolder) |
| VJOB-03: Job blocks for disallowed professions don't grant jobs | ✓ SATISFIED | Truth 5, 6, 7 (POI blocking) |
| VJOB-04: Naturally spawned villagers with disallowed jobs become jobless | ✓ SATISFIED | Truth 3, 4 (NBT loading interception) |

### Anti-Patterns Found

**None.** All files are substantive implementations with:
- No TODO/FIXME/placeholder comments
- No stub patterns (empty returns, console.log only)
- Proper method implementations
- Complete javadoc documentation
- Appropriate use of Set.of() for immutable constants

### Build Verification

```
./gradlew compileJava
```

**Result:** BUILD SUCCESSFUL in 5s

**Compilation status:** ✓ PASS — All files compile without errors

### Code Quality Analysis

**AllowedProfessions.java:**
- Immutable constants using Set.of()
- Null-safe isAllowed() (treats null as allowed/NONE)
- Clear javadoc explaining each profession's purpose
- Follows existing THC utility class patterns

**VillagerProfessionMixin.java:**
- Strategic injection at HEAD of setVillagerData (single chokepoint)
- Proper use of @Shadow for getVillagerData()
- Cancellable injection with ci.cancel()
- Comprehensive javadoc listing all interception vectors (AI, NBT, commands, cures)

**ServerLevelPoiMixin.java:**
- Defense-in-depth approach (complements profession mixin)
- Early return after claimed chunk check (efficiency)
- Clear javadoc explaining both blocking scenarios

### Allowed vs Disallowed Professions

**Allowed (6 total):**
1. MASON (stonecutter) — building blocks
2. LIBRARIAN (lectern) — enchanted books
3. BUTCHER (smoker) — food trades
4. CARTOGRAPHER (cartography table) — maps/locators
5. NONE — jobless state
6. NITWIT — no trades

**Disallowed (9 total):**
1. ARMORER (blast furnace)
2. CLERIC (brewing stand)
3. FARMER (composter)
4. FISHERMAN (barrel)
5. FLETCHER (fletching table)
6. LEATHERWORKER (cauldron variants)
7. SHEPHERD (loom)
8. TOOLSMITH (smithing table)
9. WEAPONSMITH (grindstone)

**Job Block Coverage:**
- DISALLOWED_JOB_BLOCKS contains 12 blocks (9 professions + 3 cauldron variants)
- All disallowed profession job sites blocked
- All allowed profession job sites (lectern, smoker, stonecutter, cartography table) NOT in disallowed set

### Defense-in-Depth Verification

The phase implements a two-layer defense:

**Layer 1: Data Layer (VillagerProfessionMixin)**
- Intercepts setVillagerData() at HEAD
- Catches profession changes from: AI job acquisition, NBT loading, commands, zombie cures
- Forces disallowed professions to NONE

**Layer 2: World Layer (ServerLevelPoiMixin)**
- Prevents POI registration for disallowed job blocks
- Villagers cannot see brewing stands, smithing tables, etc. as workstations
- Even if Layer 1 failed, villagers couldn't acquire disallowed professions

**Coverage:** Both layers functional and wired correctly.

---

## Summary

**Status:** PASSED

All must-haves verified. Phase 67 goal fully achieved.

**Phase Goal Achieved:** ✓ Yes

Villagers are restricted to 4 allowed professions (mason, librarian, butcher, cartographer) with NONE and NITWIT permitted for jobless states. All 9 disallowed professions blocked via setVillagerData interception and POI blocking. Defense-in-depth approach ensures comprehensive coverage.

**Critical Verifications:**
- ✓ ALLOWED set contains exactly 6 professions (4 gameplay + NONE + NITWIT)
- ✓ DISALLOWED_JOB_BLOCKS contains 12 blocks covering all disallowed professions
- ✓ Allowed job blocks (lectern, smoker, stonecutter, cartography table) NOT in disallowed set
- ✓ VillagerProfessionMixin intercepts setVillagerData at HEAD with cancellable=true
- ✓ ServerLevelPoiMixin blocks POI for disallowed job blocks
- ✓ Both mixins properly registered in thc.mixins.json
- ✓ Code compiles successfully
- ✓ No stub patterns or incomplete implementations

**Ready for Phase 68:** Custom Trade Tables can now proceed. The profession restriction layer is complete and all 4 allowed professions (mason, librarian, butcher, cartographer) are ready for deterministic trade table implementation.

---

_Verified: 2026-01-31T16:52:06Z_
_Verifier: Claude (gsd-verifier)_
