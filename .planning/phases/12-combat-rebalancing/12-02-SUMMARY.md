---
phase: 12-combat-rebalancing
plan: 02
subsystem: combat
tags: [melee, damage, sweeping-edge, mixin]
depends_on:
  requires: []
  provides: [melee-damage-reduction, sweeping-edge-disabled]
  affects: [combat-balance, ranged-priority]
tech-stack:
  added: []
  patterns: [@ModifyVariable damage reduction, @Redirect enchantment nullification]
key-files:
  created: [src/main/java/thc/mixin/PlayerAttackMixin.java]
  modified: [src/main/resources/thc.mixins.json]
decisions:
  - id: DEC-12-02-01
    title: "@ModifyVariable for damage reduction"
    choice: "Use @ModifyVariable with ordinal=0 to intercept first float STORE in attack()"
    rationale: "Cleanly intercepts damage after getAttackDamage calculation, before any other processing"
  - id: DEC-12-02-02
    title: "@Redirect for sweeping edge"
    choice: "Redirect EnchantmentHelper.getSweepingDamageRatio to return 0"
    rationale: "Completely disables sweeping edge without touching enchantment registry or data"
metrics:
  duration: 5min
  tasks: 3/3
  completed: 2026-01-19
---

# Phase 12 Plan 02: Melee Damage Reduction Summary

**One-liner:** Melee damage reduced 75% via @ModifyVariable on attack(), sweeping edge disabled via @Redirect

## What Was Done

1. **Created PlayerAttackMixin** - New mixin targeting Player class to modify melee attack behavior
2. **Melee damage reduction** - All player melee damage multiplied by 0.25f (75% reduction)
3. **Sweeping edge nullification** - EnchantmentHelper.getSweepingDamageRatio redirected to return 0.0f
4. **Mixin registration** - Added PlayerAttackMixin to thc.mixins.json

## Key Implementation Details

### Damage Reduction Pattern
```java
@ModifyVariable(method = "attack", at = @At(value = "STORE"), ordinal = 0)
private float thc$reduceMeleeDamage(float originalDamage) {
    return originalDamage * 0.25f;
}
```

- Targets first float variable stored in attack() method (damage from getAttackDamage)
- Reduction happens early in calculation, affects all subsequent damage math
- Does NOT affect buckler damage (handled separately in LivingEntityMixin)

### Sweeping Edge Pattern
```java
@Redirect(method = "attack", at = @At(value = "INVOKE", target = "...getSweepingDamageRatio..."))
private float thc$disableSweepingEdge(ServerLevel level, ItemStack weapon, float baseDamage) {
    return 0.0f;
}
```

- Intercepts the enchantment helper call, not the enchantment itself
- Returns 0 making sweeping damage formula multiply by 0
- Enchantment still exists and can be applied, it just has no combat effect

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 6498381 | feat | Add 75% melee damage reduction for players |
| 9e9dc4b | feat | Disable sweeping edge enchantment effectiveness |
| e0ba805 | chore | Register PlayerAttackMixin in configuration |

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- [x] `./gradlew build` succeeds without errors
- [x] PlayerAttackMixin.java exists with both thc$reduceMeleeDamage and thc$disableSweepingEdge methods
- [x] thc.mixins.json contains "PlayerAttackMixin"
- [x] No mixin application errors in build output

## Success Criteria Met

- [x] COMBAT-03: Sweeping edge enchantment has no effect (returns 0 damage ratio)
- [x] COMBAT-04: All melee damage reduced by 75% (multiplied by 0.25)

## Next Phase Readiness

**Phase 12 Status:** Plan 12-01 (arrow combat) and 12-02 (melee damage) both complete. Phase 12 requirements satisfied.

**Ready for:** Phase 13 (Wind Charge Mobility)

---
*Generated: 2026-01-19*
