# Phase 19 Plan 01: Undead Sun Immunity Summary

HEAD injection on Mob.isSunBurnTick returning false unconditionally, preventing zombies, skeletons, and phantoms from sun burning while preserving fire damage from lava and fire aspect.

## What Was Built

1. **MobSunBurnMixin** - Mixin targeting Mob.isSunBurnTick to prevent sun burning for all mob entities
2. **Mixin registration** - Added to thc.mixins.json alphabetically

## Changes Made

### Files Created

| File | Purpose |
|------|---------|
| `src/main/java/thc/mixin/MobSunBurnMixin.java` | Intercepts isSunBurnTick to always return false |

### Files Modified

| File | Change |
|------|--------|
| `src/main/resources/thc.mixins.json` | Added MobSunBurnMixin to mixins array |

### Key Implementation Details

- Targets `net.minecraft.world.entity.Mob` class
- Injects at HEAD of `isSunBurnTick()` method with cancellable = true
- Returns `false` unconditionally via `cir.setReturnValue(false)`
- All undead (Zombie, Skeleton, Phantom) inherit from Mob, so single mixin covers all
- Fire damage from other sources (lava, fire aspect, flaming arrows) use separate code paths and remain unaffected
- Uses `thc$preventSunBurn` naming convention for inject method

## Verification Results

- Build: PASSED
- MobSunBurnMixin.java exists with isSunBurnTick inject: VERIFIED
- thc.mixins.json includes MobSunBurnMixin: VERIFIED
- Pattern follows NaturalSpawnerMixin HEAD cancellation style: VERIFIED

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Target Mob class not individual undead classes | Single injection point covers all undead via inheritance |
| Unconditional false return | Part of twilight hardcore - undead should always be sun-immune |

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Message |
|------|---------|
| 8f8a7d2 | feat(19-01): create MobSunBurnMixin for sun immunity |
| 44a6b0f | feat(19-01): register MobSunBurnMixin |

## Requirements Coverage

| Requirement | Status | Verified By |
|-------------|--------|-------------|
| Zombies survive in daylight without burning | MET | isSunBurnTick always returns false |
| Skeletons survive in daylight without burning | MET | Skeleton extends Mob, inherits override |
| Phantoms survive in daylight without burning | MET | Phantom extends FlyingMob extends Mob |
| Fire Aspect enchantment still ignites undead | MET | Uses setRemainingFireTicks, not isSunBurnTick |
| Lava still damages undead normally | MET | Lava damage uses separate code path |

## Duration

~1 minute
