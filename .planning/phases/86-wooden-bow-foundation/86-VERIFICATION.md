---
phase: 86-wooden-bow-foundation
verified: 2026-02-13T02:44:00Z
status: passed
score: 5/5 must-haves verified
re_verification: false
---

# Phase 86: Wooden Bow Foundation Verification Report

**Phase Goal:** Rename bow to "Wooden Bow", change recipe to sticks+string, replace gravity-over-time physics with horizontal drag, apply 50% damage for wooden bow, remove Glowing from projectile hits, block tipped arrows from wooden bow.

**Verified:** 2026-02-13T02:44:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                                                   | Status     | Evidence                                                                                                                   |
| --- | ------------------------------------------------------------------------------------------------------- | ---------- | -------------------------------------------------------------------------------------------------------------------------- |
| 1   | Wooden Bow arrows deal 50% of their normal final damage                                                | ✓ VERIFIED | AbstractArrowMixin.java lines 73-79: `bowDamageMultiplier = 0.5` for `"wooden_bow"`, applied to `baseDamage`             |
| 2   | Mobs hit by player projectiles no longer receive Glowing                                               | ✓ VERIFIED | AbstractArrowMixin.java line 86-87: Only Speed III applied, comment confirms "Glowing removed per DMG-05", no GLOWING ref |
| 3   | Tipped arrows cannot be fired from the Wooden Bow                                                      | ✓ VERIFIED | BowItemMixin.java lines 39-52: Detects `TippedArrowItem`, blocks if `BowType.WOODEN`                                      |
| 4   | When tipped arrow blocked, a regular arrow is consumed instead and tipped arrow stays in inventory     | ✓ VERIFIED | BowItemMixin.java lines 55-58: `thc$findRegularArrow()` searches inventory, returns non-tipped arrow stack                |
| 5   | If player has ONLY tipped arrows and no regular arrows, bow does not fire                              | ✓ VERIFIED | BowItemMixin.java lines 60-65: Returns `ItemStack.EMPTY` if no regular arrow found, shows actionbar message               |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact                                        | Expected                                      | Status     | Details                                                                                                    |
| ----------------------------------------------- | --------------------------------------------- | ---------- | ---------------------------------------------------------------------------------------------------------- |
| `src/main/java/thc/mixin/AbstractArrowMixin.java` | 50% damage for wooden_bow, glowing removal    | ✓ VERIFIED | Contains `thc$bowTypeTag` (line 75), 0.5 multiplier (line 77), no Glowing (line 87), 125 lines total      |
| `src/main/java/thc/mixin/BowItemMixin.java`       | Tipped arrow restriction                      | ✓ VERIFIED | Contains `TippedArrowItem` checks (lines 9, 50, 83), `BowType.WOODEN` gating (line 40), 89 lines total    |
| `src/main/java/thc/bow/BowTypeTagAccess.java`     | Duck interface for cross-mixin bow type       | ✓ VERIFIED | Defines `thc$getBowTypeTag()` method (line 9), 10 lines total                                              |
| `src/main/kotlin/thc/bow/BowType.kt`              | BowType enum with WOODEN, drag factor         | ✓ VERIFIED | WOODEN enum (line 7), `fromBowItem()` companion (lines 11-19), 28 lines total                              |

**Wiring:**
- AbstractArrowMixin: Imported by 2 files, used as cast target for `thc$getBowTypeTag()` call
- BowItemMixin: Registered in thc.mixins.json (line 7), uses BowType enum for bow detection
- BowTypeTagAccess: Implemented by ProjectileEntityMixin (line 21), used by AbstractArrowMixin (line 75)
- BowType: Used by BowItemMixin for bow type check, by ProjectileEntityMixin for tagging

### Key Link Verification

| From                      | To                        | Via                                    | Status     | Details                                                                                      |
| ------------------------- | ------------------------- | -------------------------------------- | ---------- | -------------------------------------------------------------------------------------------- |
| AbstractArrowMixin.java   | ProjectileEntityMixin.java | bow type tag from shoot                | ✓ WIRED    | Line 75: `((BowTypeTagAccess) self).thc$getBowTypeTag()` cast to interface                  |
| BowItemMixin.java         | BowType.kt                | bow type check for tipped restriction  | ✓ WIRED    | Line 39: `BowType.fromBowItem(weaponStack)`, line 40: `BowType.WOODEN` comparison           |

**Additional wiring verified:**
- ProjectileEntityMixin implements BowTypeTagAccess interface (line 21)
- ProjectileEntityMixin.thc$getBowTypeTag() returns thc$bowTypeTag field (lines 122-125)
- ProjectileEntityMixin.thc$tagBowTypeOnShoot() sets tag from BowType (line 89)
- BowItemMixin registered in thc.mixins.json (line 7)

### Requirements Coverage

No requirements explicitly mapped to phase 86 in REQUIREMENTS.md. Phase implements mechanics described in v3.4-ROADMAP.md.

### Anti-Patterns Found

None. No TODOs, placeholders, empty implementations, or console.log-only handlers found in modified files.

### Human Verification Required

#### 1. Test Wooden Bow 50% Damage Reduction

**Test:** Shoot a mob with the Wooden Bow, observe damage dealt vs. expected
**Expected:** Arrow damage should be reduced to 6.5% of original (0.13 base * 0.5 bow multiplier * class multiplier)
**Why human:** Requires in-game damage calculation verification, multiple factors interact

#### 2. Test Glowing Effect Removed

**Test:** Shoot a mob with the Wooden Bow, observe mob effects
**Expected:** Mob receives Speed III for 6 seconds, but NOT Glowing effect
**Why human:** Visual effect verification requires running the game and observing mob status

#### 3. Test Tipped Arrow Blocking

**Test:** Place tipped arrows in inventory (or hotbar), attempt to fire Wooden Bow
**Expected:** 
- If regular arrows exist: Regular arrow is consumed, tipped arrow stays
- If ONLY tipped arrows exist: Bow does not fire, actionbar message "Your bow can't fire tipped arrows" appears
**Why human:** Requires interactive inventory manipulation and bow firing, actionbar message visibility check

#### 4. Test Regular Arrow Fallback Search

**Test:** Place tipped arrow in hotbar slot 0, regular arrow in slot 8, attempt to fire
**Expected:** Bow searches entire inventory, finds regular arrow, consumes it instead of tipped arrow
**Why human:** Requires inventory manipulation across multiple slots to test search logic

---

_Verified: 2026-02-13T02:44:00Z_
_Verifier: Claude (gsd-verifier)_
