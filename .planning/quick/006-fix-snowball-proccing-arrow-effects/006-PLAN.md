---
phase: quick
plan: 006
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/thc/mixin/ProjectileEntityMixin.java
autonomous: true

must_haves:
  truths:
    - "Arrows apply Speed III and Glowing to targets"
    - "Snowballs do NOT apply Speed or Glowing effects"
    - "Other non-arrow projectiles do NOT apply Speed or Glowing effects"
  artifacts:
    - path: "src/main/java/thc/mixin/ProjectileEntityMixin.java"
      provides: "Projectile hit effects with arrow-only check"
      contains: "instanceof AbstractArrow"
  key_links:
    - from: "ProjectileEntityMixin.thc$applyHitEffects"
      to: "AbstractArrowMixin.thc$applyArrowHitEffects"
      via: "instanceof check excludes arrows from base Projectile handler"
      pattern: "if.*self.*instanceof.*AbstractArrow"
---

<objective>
Fix bug where snowballs incorrectly apply Speed and Glowing effects that should only apply to player arrows.

Purpose: The ProjectileEntityMixin applies effects to ALL projectiles, but these effects (Speed III, Glowing) should only apply to arrows. Snowballs get the effects from the base Projectile mixin when they should only apply their own slowness/knockback effects (for Bastion class).

Output: ProjectileEntityMixin excludes AbstractArrow instances, letting AbstractArrowMixin handle arrow-specific effects.
</objective>

<execution_context>
@/home/tack/.claude/get-shit-done/workflows/execute-plan.md
@/home/tack/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@src/main/java/thc/mixin/ProjectileEntityMixin.java
@src/main/java/thc/mixin/AbstractArrowMixin.java
@src/main/java/thc/mixin/SnowballHitMixin.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add AbstractArrow exclusion to ProjectileEntityMixin</name>
  <files>src/main/java/thc/mixin/ProjectileEntityMixin.java</files>
  <action>
In `thc$applyHitEffects` method, add a check at the start (after getting `self`) to return early if the projectile is an instance of `AbstractArrow`:

```java
// Skip arrows - they have their own handler in AbstractArrowMixin
if (self instanceof AbstractArrow) {
    return;
}
```

Add the import for `net.minecraft.world.entity.projectile.arrow.AbstractArrow`.

This ensures:
- Arrows still get Speed/Glowing from AbstractArrowMixin (unchanged)
- Snowballs no longer get Speed/Glowing (they only get slowness/knockback from SnowballHitMixin for Bastion)
- Other non-arrow projectiles (eggs, potions, etc.) no longer get Speed/Glowing

Note: The velocity boost and enhanced gravity methods can remain as-is since they should apply to all player projectiles (including arrows).
  </action>
  <verify>
Run `./gradlew build` to verify compilation succeeds.
  </verify>
  <done>
ProjectileEntityMixin skips AbstractArrow instances in thc$applyHitEffects, snowballs no longer apply Speed/Glowing effects.
  </done>
</task>

</tasks>

<verification>
- Build succeeds: `./gradlew build`
- Manual test: Throw snowball at mob, confirm no Speed or Glowing effect applied
- Manual test: Shoot arrow at mob, confirm Speed and Glowing ARE applied (unchanged behavior)
</verification>

<success_criteria>
- Snowballs do not apply Speed or Glowing effects to targets
- Arrows continue to apply Speed III and Glowing (6s) to targets
- Build compiles without errors
</success_criteria>

<output>
After completion, create `.planning/quick/006-fix-snowball-proccing-arrow-effects/006-SUMMARY.md`
</output>
