---
phase: 35-class-system
plan: 02
subsystem: combat-mechanics
tags: [class-system, damage-scaling, mixins, melee, ranged]
dependency-graph:
  requires: [35-01-class-foundation]
  provides: [class-damage-scaling, melee-multipliers, ranged-multipliers]
  affects: [combat-balance, class-viability]
tech-stack:
  added: []
  patterns: [post-reduction-multiplier-application, server-only-class-lookup]
decisions:
  - id: DEC-35-02-01
    title: "Class multipliers apply after base reduction"
    choice: "Class multipliers are applied AFTER the 18.75% melee reduction (0.1875f) and 13% ranged reduction (0.13), not instead of them"
    rationale: "Preserves critical balance values from CLAUDE.md while allowing class differentiation. Base reductions remain unchanged, class multipliers scale from reduced baseline"
  - id: DEC-35-02-02
    title: "Players without class get 1.0x multiplier"
    choice: "ClassManager.getClass() returns null for unclassed players, skipping multiplier application"
    rationale: "Default behavior is unchanged for players who haven't selected a class, maintaining backward compatibility"
key-files:
  created: []
  modified:
    - src/main/java/thc/mixin/PlayerAttackMixin.java
    - src/main/java/thc/mixin/AbstractArrowMixin.java
metrics:
  duration: 3min
  completed: 2026-01-23
---

# Phase 35 Plan 02: Class-Based Damage Multipliers Summary

**One-liner:** Class-specific melee and ranged damage scaling with Tank/Melee/Ranged/Support differentiation applied after base reduction

## What Was Built

### Melee Damage Scaling

**PlayerAttackMixin Enhancement** (`PlayerAttackMixin.java`)
- Enhanced `thc$reduceMeleeDamage` method to apply class-based scaling
- Damage calculation flow:
  1. Apply base reduction: `originalDamage * 0.1875f` (CLAUDE.md critical value preserved)
  2. Lookup player class via `ClassManager.getClass(serverPlayer)`
  3. Apply class melee multiplier if class exists
  4. Return final damage

**Class-specific melee damage:**
- **No class:** `base * 0.1875` (unchanged)
- **TANK:** `base * 0.1875 * 2.5` = `base * 0.46875`
- **MELEE:** `base * 0.1875 * 4.0` = `base * 0.75`
- **RANGED:** `base * 0.1875 * 1.0` = `base * 0.1875`
- **SUPPORT:** `base * 0.1875 * 1.0` = `base * 0.1875`

### Ranged Damage Scaling

**AbstractArrowMixin Enhancement** (`AbstractArrowMixin.java`)
- Enhanced `thc$applyArrowHitEffects` method to apply class-based scaling
- Damage calculation flow:
  1. Apply base reduction: `baseDamage * 0.13` (preserves 87% reduction)
  2. Lookup player class via `ClassManager.getClass(player)`
  3. Apply class ranged multiplier if class exists
  4. Set final damage

**Class-specific ranged damage:**
- **No class:** `base * 0.13` (unchanged)
- **TANK:** `base * 0.13 * 1.0` = `base * 0.13`
- **MELEE:** `base * 0.13 * 1.0` = `base * 0.13`
- **RANGED:** `base * 0.13 * 5.0` = `base * 0.65`
- **SUPPORT:** `base * 0.13 * 3.0` = `base * 0.39`

## Implementation Patterns

### Post-Reduction Multiplier Pattern
```java
// Apply base reduction (CRITICAL VALUE from CLAUDE.md)
float baseDamage = originalDamage * 0.1875f;

// Apply class-based multiplier if player has class
Player self = (Player) (Object) this;
if (self instanceof ServerPlayer serverPlayer) {
    PlayerClass playerClass = ClassManager.getClass(serverPlayer);
    if (playerClass != null) {
        baseDamage *= (float) playerClass.getMeleeMultiplier();
    }
}

return baseDamage;
```

### Server-Only Class Lookup Pattern
```java
// Only apply class multipliers on server (where attachments exist)
if (self instanceof ServerPlayer serverPlayer) {
    PlayerClass playerClass = ClassManager.getClass(serverPlayer);
    if (playerClass != null) {
        // Apply multiplier
    }
}
// Client players skip multiplier (no attachment data)
```

### Null-Safe Multiplier Application
```java
// Players without class selection skip multiplier entirely
PlayerClass playerClass = ClassManager.getClass(player);
if (playerClass != null) {
    damage *= playerClass.getRangedMultiplier();
}
// If null, damage remains at base reduction level (1.0x implicit)
```

## Deviations from Plan

None - plan executed exactly as written.

## Class Viability Analysis

### Damage Output Comparison (per base damage unit)

**Melee damage output:**
1. **MELEE class:** 0.75 (4x multiplier) - **highest melee**
2. **TANK class:** 0.46875 (2.5x multiplier) - **balanced melee**
3. **RANGED/SUPPORT:** 0.1875 (1x multiplier) - **base melee**

**Ranged damage output:**
1. **RANGED class:** 0.65 (5x multiplier) - **highest ranged**
2. **SUPPORT class:** 0.39 (3x multiplier) - **moderate ranged**
3. **TANK/MELEE:** 0.13 (1x multiplier) - **base ranged**

### Role Differentiation

**TANK** (+1 heart, 2.5x melee, 1x ranged)
- Focus: Survivability + moderate melee
- Role: Frontline fighter who can take hits
- Trade-off: Poor ranged capability

**MELEE** (+0.5 hearts, 4x melee, 1x ranged)
- Focus: Maximum melee damage
- Role: High-risk high-reward close combat
- Trade-off: Vulnerable to ranged enemies

**RANGED** (no health bonus, 1x melee, 5x ranged)
- Focus: Maximum ranged damage
- Role: Backline damage dealer
- Trade-off: Weak in melee, no survivability boost

**SUPPORT** (no health bonus, 1x melee, 3x ranged)
- Focus: Moderate ranged + future support abilities
- Role: Balanced ranged with utility potential
- Trade-off: Not specialized in any single area

## Testing & Verification

**Compilation:** ✅ Successful (`./gradlew build`)

**Code verification:**
- ✅ Both mixins use `ClassManager.getClass()` lookup
- ✅ `getMeleeMultiplier()` called in `PlayerAttackMixin`
- ✅ `getRangedMultiplier()` called in `AbstractArrowMixin`
- ✅ Critical balance values preserved: `0.1875f` (melee), `0.13` (ranged)
- ✅ Server-only class lookup (ServerPlayer instanceof check)
- ✅ Null-safe multiplier application (players without class skip multiplier)

**Runtime verification:**
- ⚠️ Smoke test fails due to pre-existing PlayerSleepMixin breakage (documented in 35-01-SUMMARY.md)
- Build successful, code compiles correctly
- Manual in-game testing required after mixin fixes

**What to test in-game (when runtime is fixed):**
1. **TANK class:**
   - Melee attack on zombie → verify ~2.5x damage vs unclassed player
   - Arrow shot on zombie → verify same damage as unclassed player
2. **MELEE class:**
   - Melee attack on zombie → verify ~4x damage vs unclassed player (highest melee)
   - Arrow shot on zombie → verify same damage as unclassed player
3. **RANGED class:**
   - Arrow shot on zombie → verify ~5x damage vs unclassed player (highest ranged)
   - Melee attack on zombie → verify same damage as unclassed player
4. **SUPPORT class:**
   - Arrow shot on zombie → verify ~3x damage vs unclassed player
   - Melee attack on zombie → verify same damage as unclassed player
5. **Unclassed player:**
   - Verify melee/ranged damage unchanged from pre-class-system baseline

## Task Commits

Each task was committed atomically:

1. **Task 1: Add class-based melee damage multiplier** - `f82ed8b` (feat)
2. **Task 2: Add class-based ranged damage multiplier** - `29541e9` (feat)

## Critical Balance Values Preserved

Per CLAUDE.md instructions, these critical values remain unchanged:

| File | Value | Setting | Status |
|------|-------|---------|--------|
| `PlayerAttackMixin.java` | `0.1875f` | Melee damage base reduction | ✅ Preserved (line 33) |
| `AbstractArrowMixin.java` | `0.13` | Ranged damage base reduction | ✅ Preserved (line 51) |

Class multipliers are applied **AFTER** these base reductions, not instead of them.

## Next Phase Readiness

### Class System Complete

**Delivered:**
- ✅ Player class selection with persistence (35-01)
- ✅ Health bonuses per class (35-01)
- ✅ Melee damage scaling per class (35-02)
- ✅ Ranged damage scaling per class (35-02)

**Ready for:**
- Phase 36 or next feature development
- All class system functionality integrated into damage mixins
- No additional class-related work needed

**Blockers:**
- Pre-existing mixin breakages (PlayerSleepMixin) prevent in-game testing
- Resolution needed before v2.2 release

**Concerns:**
- None for class system functionality
- Class balance may need tuning based on in-game testing

## Metrics

- **Tasks completed:** 2/2
- **Commits:** 2 feature commits
- **Files created:** 0
- **Files modified:** 2
- **Duration:** 3 minutes
- **Lines of code added:** ~25 (excluding comments)
