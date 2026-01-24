# Phase 44: Damage Rebalancing - Research

**Researched:** 2026-01-24
**Domain:** Mob damage modification via attributes and mixin interception
**Confidence:** HIGH

## Summary

Phase 44 requires tuning damage output for 6 specific mobs to match THC balance. The codebase already has established patterns for attribute modification (MonsterModifications, SimpleEntityBehaviors) and damage interception (AbstractArrowMixin, PlayerAttackMixin). This phase extends these patterns to new targets.

Three categories of damage modification are required:
1. **Melee damage (Vex, Vindicator, Magma Cube)** - Use ATTACK_DAMAGE attribute modifiers on ENTITY_LOAD
2. **Projectile damage (Blaze, Piglin)** - Intercept fireball hit and arrow shoot methods
3. **Evoker fangs** - Intercept EvokerFangs.dealDamageTo to reduce fixed damage

**Primary recommendation:** Extend MonsterModifications.kt or create DamageRebalancing.kt to apply ATTACK_DAMAGE modifiers for melee mobs; create SmallFireballMixin and EvokerFangsMixin for projectile/fang damage.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric Mixin | 0.12.5+ | Bytecode modification | Standard for MC behavior changes |
| Fabric API | 0.100.x | ServerEntityEvents.ENTITY_LOAD | Already used for monster speed |
| Attributes API | 1.21.11 | AttributeModifier | Already used for speed boost |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| AbstractArrowAccessor | existing | Arrow baseDamage access | For Piglin arrow modification |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| ENTITY_LOAD | finalizeSpawn mixin | ENTITY_LOAD catches all spawn sources; prefer it |
| @ModifyArg | @Redirect | @ModifyArg is simpler for single-value changes |

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/thc/monster/
├── MonsterModifications.kt   # Speed boost (existing)
├── SimpleEntityBehaviors.kt  # Vex health/sword (existing)
├── GhastModifications.kt     # Fireball velocity (existing)
└── DamageRebalancing.kt      # NEW: Melee damage modifiers

src/main/java/thc/mixin/
├── SmallFireballMixin.java   # NEW: Blaze fireball damage
├── EvokerFangsMixin.java     # NEW: Fang damage reduction
└── PiglinCrossbowMixin.java  # NEW: Arrow damage boost (if needed)
```

### Pattern 1: ATTACK_DAMAGE Attribute Modifier (Melee Damage)
**What:** Apply multiplicative modifier to mob's ATTACK_DAMAGE attribute on spawn
**When to use:** Vex, Vindicator, Magma Cube damage changes
**Example:**
```kotlin
// Source: Established pattern in MonsterModifications.kt
private val VEX_DAMAGE_ID = Identifier.fromNamespaceAndPath("thc", "vex_damage_reduction")

private fun applyVexDamage(mob: Mob) {
    if (mob !is Vex) return
    val attackAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE) ?: return
    if (!attackAttr.hasModifier(VEX_DAMAGE_ID)) {
        // Multiply base damage by 0.296 to get ~4 from 13.5
        attackAttr.addTransientModifier(
            AttributeModifier(VEX_DAMAGE_ID, -0.704, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        )
    }
}
```

### Pattern 2: @ModifyArg for Damage Method Calls
**What:** Intercept damage value passed to hurt() method
**When to use:** Evoker fangs, Blaze fireballs where damage is hardcoded
**Example:**
```java
// Source: Fabric Mixin documentation
@Mixin(EvokerFangs.class)
public abstract class EvokerFangsMixin {
    @ModifyArg(
        method = "dealDamageTo",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
        index = 1
    )
    private float thc$reduceFangDamage(float original) {
        // Reduce from 6 to 2.5 (0.417 multiplier)
        return original * 0.417f;
    }
}
```

### Pattern 3: Arrow baseDamage Modification
**What:** Set baseDamage when arrow is shot from specific mob type
**When to use:** Piglin arrow damage increase
**Example:**
```java
// Source: Existing AbstractArrowMixin pattern
@Mixin(Piglin.class)
public abstract class PiglinCrossbowMixin {
    // Option A: Inject at crossbow shoot time
    // Option B: Use ENTITY_LOAD on Arrow with owner check
}
```

### Anti-Patterns to Avoid
- **Modifying DamageSource:** Don't create custom damage sources just for damage value changes
- **Hardcoded damage values:** Use multipliers based on vanilla values, not fixed replacements
- **Separate handlers per mob:** Group similar patterns (all melee in one handler)

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Melee damage reduction | Custom hurt() mixin | ATTACK_DAMAGE modifier | Attribute system handles difficulty scaling |
| Projectile damage | Override entire projectile class | @ModifyArg on hurt() call | Cleaner, less breakage |
| Entity type checks | instanceof chains | EntityType comparison | MC 1.21.11 pattern from STATE.md |

**Key insight:** ATTACK_DAMAGE attribute modifiers automatically preserve difficulty scaling (Easy/Normal/Hard multipliers). Hardcoding damage values would require manual scaling.

## Common Pitfalls

### Pitfall 1: Difficulty Scaling Confusion
**What goes wrong:** Damage values from wiki are for specific difficulty, modifications don't scale correctly
**Why it happens:** Wiki shows Easy/Normal/Hard values, not base attribute values
**How to avoid:** Use ADD_MULTIPLIED_TOTAL operation to scale final damage rather than replacing base
**Warning signs:** Damage is too high on Easy, too low on Hard

### Pitfall 2: Magma Cube Size Ignoring
**What goes wrong:** Only modifying large cube, medium/small have wrong damage
**Why it happens:** Magma cube damage = size + 2 (size 3=5, size 1=3, size 0=2 base)
**How to avoid:** Apply same multiplier to all sizes (preserves ratio)
**Warning signs:** Small magma cubes still deal full damage

### Pitfall 3: Piglin Brute Confusion
**What goes wrong:** Piglin Brutes affected when only crossbow Piglins should be
**Why it happens:** Piglin Brutes are separate EntityType, don't use crossbows
**How to avoid:** Target EntityType.PIGLIN only, not PIGLIN_BRUTE; verify crossbow wielding
**Warning signs:** Brute melee damage changes when it shouldn't

### Pitfall 4: Evoker Fang Summoner Check
**What goes wrong:** Applying damage based on whether Evoker is owner
**Why it happens:** Thinking fang damage should only change for Evoker-summoned fangs
**How to avoid:** Per CONTEXT.md: "Apply to ALL fangs regardless of summoner"
**Warning signs:** Command-summoned fangs deal different damage

### Pitfall 5: SmallFireball vs LargeFireball
**What goes wrong:** Modifying wrong fireball class
**Why it happens:** Blaze uses SmallFireball, Ghast uses LargeFireball
**How to avoid:** Target SmallFireball for Blaze, LargeFireball already handled for velocity
**Warning signs:** Ghast damage changes instead of Blaze

## Code Examples

Verified patterns from codebase:

### Existing ATTACK_DAMAGE Pattern (from MonsterModifications.kt)
```kotlin
// Source: /mnt/c/home/code/thc/src/main/kotlin/thc/monster/MonsterModifications.kt
private val SPEED_BOOST_ID = Identifier.fromNamespaceAndPath("thc", "monster_speed_boost")

val speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
if (!speedAttr.hasModifier(SPEED_BOOST_ID)) {
    speedAttr.addTransientModifier(
        AttributeModifier(SPEED_BOOST_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    )
}
```

### Entity Type Check Pattern
```kotlin
// Source: MonsterModifications.kt
if (mob.type == EntityType.ZOMBIE && mob.isBaby) return
```

### Arrow baseDamage Modification Pattern
```java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractArrowMixin.java
@Shadow
private double baseDamage;

@Inject(method = "onHitEntity", at = @At("HEAD"))
private void thc$applyArrowHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
    // Modify baseDamage before vanilla processing
    baseDamage = modifiedValue;
}
```

### Existing SimpleEntityBehaviors Pattern
```kotlin
// Source: /mnt/c/home/code/thc/src/main/kotlin/thc/monster/SimpleEntityBehaviors.kt
ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
    if (entity is Vex) {
        modifyVex(entity)
    }
}
```

## Damage Calculation Reference

### Target Damage Values (from CONTEXT.md)

| Mob | Vanilla Damage (Hard) | Target Damage | Multiplier |
|-----|----------------------|---------------|------------|
| Vex (armed) | 13.5 | ~4 | 0.296 |
| Vindicator (armed) | 19.5 | ~11.7 | 0.600 |
| Evoker fangs | 9 (Hard), 6 (Normal) | ~2.5 | 0.417 (on Normal base) |
| Blaze fireball | 7.5 (Hard), 5 (Normal) | ~3.8 | 0.760 (on Normal base) |
| Piglin arrow | ~4 base | ~8 | 2.0x |
| Large Magma cube | 9 (Hard), 6 (Normal) | ~4.7 | 0.783 (on Normal base) |

### Minecraft Difficulty Scaling
```
Hard = 1.5 * Normal
Easy = min(Normal, 0.5 * Normal + 1)
```

**Note:** ATTACK_DAMAGE base values in Minecraft are typically defined as the "Normal" difficulty damage before the 1.5x Hard multiplier. The wiki values for Hard difficulty are already scaled.

### Attribute Modifier Operations
```java
// ADD_VALUE: base + modifier
// ADD_MULTIPLIED_BASE: base * (1 + sum(modifiers))
// ADD_MULTIPLIED_TOTAL: (base * (1 + base_mods)) * (1 + total_mods)

// For damage reduction:
// To reduce 13.5 to 4: use -0.704 with ADD_MULTIPLIED_TOTAL
// 13.5 * (1 + (-0.704)) = 13.5 * 0.296 = 4.0
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Replace entity class | ENTITY_LOAD + attributes | Fabric API standard | Non-invasive modification |
| @Overwrite methods | @ModifyArg/Variable | Mixin best practice | Better compatibility |

**Deprecated/outdated:**
- ResourceLocation: Use Identifier.fromNamespaceAndPath in MC 1.21.11
- Custom DamageSource for scaling: Attributes handle difficulty scaling automatically

## Open Questions

Things that couldn't be fully resolved:

1. **Exact base damage values**
   - What we know: Wiki provides difficulty-scaled values (Easy/Normal/Hard)
   - What's unclear: Exact attribute base values in code (likely Normal values)
   - Recommendation: Use multipliers on observable Hard values, test in-game

2. **Piglin arrow modification approach**
   - What we know: Need to boost from ~4 to ~8 damage
   - What's unclear: Best injection point (ENTITY_LOAD on arrow vs crossbow shoot)
   - Recommendation: Try ENTITY_LOAD with owner check, fall back to CrossbowAttackMob mixin

3. **Evoker fang method signature**
   - What we know: dealDamageTo method exists per CONTEXT.md decision
   - What's unclear: Exact MC 1.21.11 method signature for mixin target
   - Recommendation: Verify in decompiled source during implementation

## Sources

### Primary (HIGH confidence)
- MonsterModifications.kt, SimpleEntityBehaviors.kt, AbstractArrowMixin.java - Existing codebase patterns
- https://minecraft.wiki/w/Vex - Damage values: 5.5/9/13.5 (Easy/Normal/Hard armed)
- https://minecraft.wiki/w/Vindicator - Damage values: 7.5/13/19.5 (Easy/Normal/Hard armed)
- https://minecraft.wiki/w/Magma_Cube - Damage formula: size + 2, scaling by difficulty
- https://minecraft.wiki/w/Blaze - Fireball damage: 3.5/5/7.5 (Easy/Normal/Hard)
- https://minecraft.wiki/w/Evoker - Fang damage: 4/6/9 (Easy/Normal/Hard Java)
- https://minecraft.wiki/w/Piglin - Arrow damage: 2-5 HP range
- https://minecraft.wiki/w/Damage - Difficulty formula: Hard = 1.5*Normal

### Secondary (MEDIUM confidence)
- https://wiki.fabricmc.net/tutorial:mixin_examples - @ModifyArg pattern documentation

### Tertiary (LOW confidence)
- None - all findings verified with primary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using established codebase patterns
- Architecture: HIGH - Extending existing MonsterModifications pattern
- Pitfalls: HIGH - Based on wiki damage documentation and codebase analysis
- Damage calculations: MEDIUM - Wiki values are authoritative but may need runtime verification

**Research date:** 2026-01-24
**Valid until:** 2026-02-24 (30 days - stable Minecraft version)
