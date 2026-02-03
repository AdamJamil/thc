# Quick Task 006 Summary

## Task
Fix snowballs proccing arrow-only speed/glowing effects

## Problem
`ProjectileEntityMixin.thc$applyHitEffects` was applying Speed III and Glowing effects to ALL projectiles (snowballs, eggs, etc.), but these effects should only apply to player arrows.

## Solution
1. Added `instanceof AbstractArrow` check at start of `thc$applyHitEffects` to skip arrows (they have their own handler in `AbstractArrowMixin`)
2. Removed Speed III and Glowing effect application from base `Projectile` mixin
3. Kept threat/aggro behavior for all projectiles (snowballs can still aggro mobs)

## Files Changed
- `src/main/java/thc/mixin/ProjectileEntityMixin.java`

## Commit
`8e3c3e2` - fix(projectile): prevent snowballs from proccing arrow-only effects
