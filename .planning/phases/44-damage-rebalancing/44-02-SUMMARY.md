---
phase: 44-damage-rebalancing
plan: 02
subsystem: damage
tags: [mixin, damage-modification, projectile, blaze, evoker, piglin]

dependency-graph:
  requires:
    - phase-44-01-melee-damage
  provides:
    - blaze-fireball-reduction
    - evoker-fang-reduction
    - piglin-arrow-boost
  affects: []

tech-stack:
  added: []
  patterns:
    - modifyarg-damage-interception
    - crossbow-projectile-modification

key-files:
  created:
    - src/main/java/thc/mixin/SmallFireballMixin.java
    - src/main/java/thc/mixin/EvokerFangsMixin.java
    - src/main/java/thc/mixin/PiglinCrossbowMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Universal reduction: Apply to ALL SmallFireball/EvokerFangs sources, not just mob-shot"
  - "CrossbowItem injection: Target shootProjectile for reliable Piglin arrow access"
  - "AbstractArrowAccessor: Use existing accessor for baseDamage read"

patterns-established:
  - "@ModifyArg index=1 for hurt() float damage parameter"
  - "CrossbowItem TAIL injection for shooter-specific projectile modification"
  - "EntityType comparison for mob-specific behavior in projectile context"

metrics:
  duration: 6min
  completed: 2026-01-24
---

# Phase 44 Plan 02: Projectile Damage Mixins Summary

@ModifyArg mixins for Blaze fireball (0.76x) and Evoker fang (0.417x) damage reduction, plus CrossbowItem injection for Piglin arrow damage boost (2.0x).

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-24
- **Completed:** 2026-01-24
- **Tasks:** 4
- **Files created:** 3
- **Files modified:** 1

## Accomplishments
- SmallFireballMixin reduces Blaze fireball from ~5 to ~3.8 damage
- EvokerFangsMixin reduces Evoker fang from ~6 to ~2.5 damage
- PiglinCrossbowMixin boosts Piglin arrow from ~4 to ~8 damage
- All mixins registered in thc.mixins.json

## Task Commits

Each task was committed atomically:

1. **Task 1: Create SmallFireballMixin** - `ce77238` (feat)
2. **Task 2: Create EvokerFangsMixin** - `383a0c6` (feat)
3. **Task 3: Create PiglinCrossbowMixin** - `0f24225` (feat)
4. **Task 4: Register mixins** - `3f2d2a2` (chore)

## Files Created/Modified
- `src/main/java/thc/mixin/SmallFireballMixin.java` - @ModifyArg on onHitEntity.hurt() with 0.76 multiplier
- `src/main/java/thc/mixin/EvokerFangsMixin.java` - @ModifyArg on dealDamageTo.hurt() with 0.417 multiplier
- `src/main/java/thc/mixin/PiglinCrossbowMixin.java` - @Inject TAIL on shootProjectile with EntityType.PIGLIN check
- `src/main/resources/thc.mixins.json` - Added three new mixin registrations

## Decisions Made
- **Universal SmallFireball/EvokerFangs reduction:** Applies to ALL sources (mob-shot, dispenser, commands) for consistency
- **CrossbowItem.shootProjectile injection:** Chosen over Piglin.performRangedAttack because projectile is directly accessible as parameter
- **EntityType.PIGLIN check:** Only boost regular Piglins, not Brutes (which use melee anyway)
- **AbstractArrowAccessor usage:** Reuse existing accessor for baseDamage read (setBaseDamage is public in 1.21.11)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing getBaseDamage() method**
- **Found during:** Task 3 (PiglinCrossbowMixin compilation)
- **Issue:** Plan assumed getBaseDamage() was public on AbstractArrow but it's private in MC 1.21.11
- **Fix:** Cast arrow to AbstractArrowAccessor to access baseDamage via existing accessor
- **Files modified:** src/main/java/thc/mixin/PiglinCrossbowMixin.java
- **Verification:** Compiles successfully
- **Committed in:** 0f24225 (Task 3 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Accessor cast required for private field access. No scope creep.

## Issues Encountered
None - plan executed successfully after accessor fix.

## Key Implementation Details

**SmallFireballMixin:**
- Targets: onHitEntity method's hurt() call
- Index 1 = float damage parameter (index 0 = DamageSource)
- Multiplier: 0.76 (reduces 5 to 3.8, preserves Hard scaling to ~5.7)

**EvokerFangsMixin:**
- Targets: dealDamageTo method's hurt() call
- Same index pattern as SmallFireballMixin
- Multiplier: 0.417 (reduces 6 to 2.5, preserves Hard scaling to ~3.75)

**PiglinCrossbowMixin:**
- Targets: CrossbowItem.shootProjectile static method
- Checks shooter.getType() == EntityType.PIGLIN
- Filters to AbstractArrow instanceof (excludes fireworks)
- Multiplier: 2.0 (boosts ~4 base to ~8)

## Next Phase Readiness

**Phase 44 complete:**
- Plan 01: Melee damage modifiers (Vex, Vindicator, Magma Cube) via ATTACK_DAMAGE attributes
- Plan 02: Projectile/special damage mixins (Blaze, Evoker, Piglin) via @ModifyArg/@Inject

All FR-08 through FR-13 (v2.3 damage rebalancing) requirements implemented.

---
*Phase: 44-damage-rebalancing*
*Completed: 2026-01-24*
