# Phase 40: Entity-Specific Behaviors (Complex) - Research

**Researched:** 2026-01-24
**Domain:** Minecraft 1.21.11 Ghast and Enderman behavior modification, AI goals, projectile mechanics, explosion fire spread
**Confidence:** MEDIUM-HIGH

## Summary

Phase 40 implements complex entity behavior modifications for Ghasts and Endermen using multiple interception strategies. Ghast modifications require three distinct injection points: (1) AI goal modification for fire rate via RangedAttackGoal parameters, (2) projectile velocity boost via ENTITY_LOAD or shoot method interception, and (3) explosion fire spread via custom ExplosionDamageCalculator or explosion event. Enderman modifications use: (1) custom AI goal for proximity aggro within 3 blocks, and (2) hurtServer injection to trigger teleport-behind logic after damage exchange.

Key findings:
- Ghast fire rate controlled by RangedAttackGoal constructor parameters (currently 60 ticks, target 80 ticks = -25%)
- Ghast fireball velocity can be modified via LargeFireball.setDeltaMovement() at spawn (shoot method or ENTITY_LOAD)
- Fire spread radius requires explosion interception - vanilla creates fire on 1/3 of blocks, doubling this requires custom logic
- Enderman teleportation happens in teleportRandomly() method called from hurtServer - can inject teleport-behind logic here
- Enderman proximity aggro requires new custom AI goal added to targetSelector via registerGoals mixin

**Primary recommendation:** Use registerGoals mixin to modify Ghast RangedAttackGoal fire rate, ENTITY_LOAD event to boost fireball velocity 50%, custom ExplosionDamageCalculator for fire spread doubling, registerGoals mixin for Enderman proximity goal, and hurtServer injection for teleport-behind with 50% probability.

## Standard Stack

The established libraries/tools for complex entity behavior modification in THC:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.141.0+1.21.11 | ServerEntityEvents.ENTITY_LOAD | THC pattern for spawn-time modifications (MonsterModifications.kt) |
| Mixin | 0.8.5 (via Fabric Loader) | Mob.registerGoals, LivingEntity.hurtServer | AI goal injection and damage event interception |
| Mojang Mappings | 1.21.11 | Official class/method names | THC standard (Ghast, Enderman, LargeFireball, etc.) |
| Minecraft AI System | 1.21.11 | GoalSelector, RangedAttackGoal, custom Goal classes | Vanilla AI framework for mob behaviors |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Vec3 | net.minecraft.world.phys | Velocity manipulation, direction calculation | Fireball velocity boost, teleport-behind positioning |
| Attributes API | net.minecraft.world.entity.ai.attributes | MOVEMENT_SPEED modification | Alternative to AI goal for speed changes |
| Random | net.minecraft.util.RandomSource | 50% probability for teleport-behind | Enderman probabilistic behavior |
| AABB | net.minecraft.world.phys | Proximity detection (3 block range) | Enderman proximity aggro target search |
| Explosion | net.minecraft.world.level.Explosion | Fire spread logic | Ghast fireball explosion modification |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| registerGoals mixin | Replace entire Ghast.registerGoals | Mixin is surgical, full replace breaks mod compat |
| ENTITY_LOAD velocity | Mixin Ghast.performRangedAttack | ENTITY_LOAD simpler, performRangedAttack more precise |
| Custom AI goal for proximity | Expand EnderManLookForPlayerGoal range | Custom goal cleaner, expansion would break vanilla behavior |
| hurtServer teleport injection | Custom EnderMan.teleportRandomly override | Injection preserves vanilla logic, override replaces entirely |

**Installation:**
```bash
# Already in THC - no new dependencies
# Fabric API 0.141.0+1.21.11 provides ServerEntityEvents
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/
├── java/thc/
│   ├── mixin/
│   │   ├── GhastRegisterGoalsMixin.java       # FR-07, FR-08 (fire rate via RangedAttackGoal)
│   │   ├── EndermanRegisterGoalsMixin.java    # FR-11 (proximity aggro goal injection)
│   │   ├── EndermanHurtMixin.java             # FR-10 (teleport-behind on damage)
│   │   ├── LargeFireballExplosionMixin.java   # FR-09 (fire spread doubling)
│   └── entity/
│       ├── EndermanProximityAggroGoal.java    # FR-11 (custom AI goal)
├── kotlin/thc/
│   └── monster/
│       └── GhastProjectileModifier.kt         # FR-07 (fireball velocity boost via ENTITY_LOAD)
```

### Pattern 1: AI Goal Parameter Modification (Ghast Fire Rate)
**What:** Modify vanilla AI goal construction to change behavior parameters
**When to use:** Fire rate, attack timing, movement speed controlled by Goal constructor params
**Example:**
```java
// Source: THC MonsterThreatGoalMixin pattern (registerGoals TAIL injection)
@Mixin(Ghast.class)
public abstract class GhastRegisterGoalsMixin {
    @Shadow @Final protected GoalSelector goalSelector;

    /**
     * Modify Ghast fire rate by removing vanilla RangedAttackGoal and adding
     * new one with extended attack interval.
     *
     * Vanilla: RangedAttackGoal with 60 tick interval (3 seconds)
     * THC: 80 tick interval (4 seconds) = 25% slower fire rate
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$modifyFireRate(CallbackInfo ci) {
        Ghast self = (Ghast) (Object) this;

        // Remove vanilla RangedAttackGoal (priority 1)
        // Note: May need to iterate goalSelector.availableGoals to find and remove
        // Alternatively: use @Redirect on goalSelector.addGoal for RangedAttackGoal

        // Add modified RangedAttackGoal with 80 tick interval
        // Parameters: (mob, speedModifier, attackIntervalTicks, maxAttackDistance)
        this.goalSelector.addGoal(1, new RangedAttackGoal(
            self,
            1.0,      // Speed modifier (unchanged)
            80,       // Attack interval: 80 ticks = 4 seconds (was 60)
            64.0f     // Max attack distance (unchanged)
        ));
    }
}
```

**Challenge:** Removing existing goal requires iteration or @Redirect. See Pattern 1b for alternatives.

### Pattern 1b: AI Goal Removal Before Re-Adding
**What:** Clean removal of vanilla goals before adding modified versions
**When to use:** Modifying parameters of existing AI goals
**Example:**
```java
@Inject(method = "registerGoals", at = @At("TAIL"))
private void thc$modifyFireRate(CallbackInfo ci) {
    Ghast self = (Ghast) (Object) this;

    // Remove existing RangedAttackGoal via iteration
    // GoalSelector.availableGoals is a Set<WrappedGoal>
    // Need accessor mixin or reflection to access

    // Alternative: Use @Redirect on addGoal call specifically for RangedAttackGoal
    // This intercepts the vanilla addGoal(1, new RangedAttackGoal(...)) call
    // and replaces parameters
}
```

**Note:** May require accessor mixin for GoalSelector.availableGoals access. Verify in implementation phase.

### Pattern 2: Projectile Velocity Modification (Ghast Fireball Speed)
**What:** Boost projectile velocity at spawn time
**When to use:** Modifying projectile speed without changing AI goal launch logic
**Example:**
```kotlin
// Source: THC ProjectileEntityMixin.shoot velocity boost pattern
object GhastProjectileModifier {
    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity !is LargeFireball) return@register

            // Only modify ghast-shot fireballs (owner is Ghast)
            if (entity.owner !is Ghast) return@register

            // Boost velocity by 50% (1.5x multiplier)
            val velocity = entity.deltaMovement
            entity.setDeltaMovement(velocity.scale(1.5))
        }
    }
}
```

**Alternative:** Mixin Ghast.performRangedAttack to modify fireball velocity at creation.

### Pattern 3: Custom AI Goal for Proximity Aggro (Enderman)
**What:** Create custom AI goal that triggers aggro when player within range
**When to use:** New targeting behavior not covered by vanilla goals
**Example:**
```java
// Source: THC ThreatTargetGoal custom goal pattern
public class EndermanProximityAggroGoal extends Goal {
    private final EnderMan enderman;
    private static final double AGGRO_RANGE = 3.0;

    public EndermanProximityAggroGoal(EnderMan enderman) {
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Don't override if already has target
        if (this.enderman.getTarget() != null) return false;

        // Find nearest player within 3 blocks
        Player nearest = this.enderman.level()
            .getNearestPlayer(this.enderman, AGGRO_RANGE);

        if (nearest != null && !nearest.isSpectator() && !nearest.isCreative()) {
            this.enderman.setTarget(nearest);
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.enderman.getTarget() != null;
    }
}
```

**Injection:**
```java
@Mixin(EnderMan.class)
public abstract class EndermanRegisterGoalsMixin {
    @Shadow @Final protected GoalSelector targetSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$addProximityAggro(CallbackInfo ci) {
        EnderMan self = (EnderMan) (Object) this;
        // High priority (0) to trigger before vanilla targeting
        this.targetSelector.addGoal(0, new EndermanProximityAggroGoal(self));
    }
}
```

### Pattern 4: Teleport-Behind on Damage (Enderman)
**What:** Inject into damage event to trigger teleport with calculated behind-player position
**When to use:** Behavior triggered by damage exchange (both player→mob and mob→player)
**Example:**
```java
// Source: THC MobDamageThreatMixin.hurtServer TAIL pattern
@Mixin(EnderMan.class)
public abstract class EndermanHurtMixin {
    @Shadow public abstract boolean teleportTo(double x, double y, double z);

    /**
     * 50% chance to teleport behind player after damage exchange.
     * Triggers when:
     * - Player damages enderman (this.hurtServer called)
     * - Enderman damages player (separate injection on player.hurtServer)
     */
    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void thc$teleportBehindOnDamage(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        // Only if damage was dealt
        if (!cir.getReturnValue() || amount <= 0) return;

        // Only if attacker is player
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        // 50% chance
        if (level.random.nextFloat() >= 0.5f) return;

        // Calculate position 3 blocks behind player
        Vec3 playerPos = player.position();
        Vec3 playerLook = player.getLookAngle();
        Vec3 behind = playerPos.subtract(playerLook.scale(3.0));

        // Attempt teleport (vanilla teleportTo handles validity checks)
        this.teleportTo(behind.x, behind.y, behind.z);
    }
}
```

**Note:** Also need injection on ServerPlayer.hurtServer to catch enderman→player damage. Same logic, reversed source check.

### Pattern 5: Explosion Fire Spread Modification (Ghast Fireball)
**What:** Intercept explosion creation to modify fire spread behavior
**When to use:** Changing explosion radius, fire generation, or block interaction
**Example:**
```java
// Based on: Minecraft Explosion mechanics and web search findings
@Mixin(LargeFireball.class)
public abstract class LargeFireballExplosionMixin {

    /**
     * Modify fire spread when ghast fireball explodes.
     *
     * Vanilla: Fire generated on 1/3 of affected blocks
     * THC: Double the fire spread radius by modifying explosion parameters
     *
     * Note: This may require custom ExplosionDamageCalculator or
     * post-explosion fire placement logic.
     */
    @Inject(method = "onHit", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;explode(...)V"
    ), cancellable = true)
    private void thc$modifyExplosionFireSpread(
        HitResult hitResult,
        CallbackInfo ci
    ) {
        LargeFireball self = (LargeFireball) (Object) this;

        // Cancel vanilla explosion
        ci.cancel();

        // Create custom explosion with doubled fire spread
        // Option 1: Custom ExplosionDamageCalculator
        // Option 2: Vanilla explosion + manual fire placement

        // After vanilla explosion, scan larger radius and place additional fire
        // Vanilla fire spread: checks 1/3 of blocks in explosion sphere
        // THC: Place fire in ring around impact at 2x normal distance
    }
}
```

**Challenge:** Fire spread logic is complex. May need to place fire manually post-explosion. See Don't Hand-Roll section.

### Anti-Patterns to Avoid
- **Modifying entity speed via attributes instead of AI goals:** Speed changes affect movement, but AI goals control attack timing. Use RangedAttackGoal parameters for fire rate.
- **Teleporting without validity checks:** EnderMan.teleportTo() includes built-in checks for valid positions. Don't reimplement pathfinding logic.
- **Hardcoding player direction assumptions:** Player look angle can point in any direction. Always use getLookAngle() for behind-player calculation.
- **Assuming ENTITY_LOAD fires only once:** Can fire on chunk load/unload cycles. Use idempotent modifications (check if already applied).
- **Ignoring explosion damage calculator:** Custom fire spread should use ExplosionDamageCalculator for proper integration with vanilla explosion mechanics.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| AI goal removal | Manual goal list iteration | @Redirect on addGoal call | Cleaner interception, no reflection needed |
| Valid teleport position | Custom raytrace/block checking | EnderMan.teleportTo() | Handles all edge cases (blocks, water, lava, void) |
| Behind-player calculation | Manual rotation math | player.getLookAngle().scale(-3) | Handles pitch/yaw correctly |
| Explosion mechanics | Custom explosion code | Level.explode() + ExplosionDamageCalculator | Handles block breaking, knockback, sound, particles |
| Fire spread pattern | Manual block iteration | Explosion.getToBlow() + BlockState.canCatchFire() | Vanilla logic for flammable detection |
| Proximity detection | Manual distance calculation | Level.getNearestPlayer(entity, range) | Optimized spatial queries, handles spectator/creative |
| 50% probability | Manual threshold comparison | random.nextBoolean() | Clearer intent, same performance |

**Key insight:** Minecraft's entity AI system is mature and handles edge cases. Ghast fire rate is RangedAttackGoal parameter, not custom timing logic. Enderman teleportation has built-in validity checking. Explosion fire spread has dedicated calculator interface. Use these systems rather than bypassing them.

## Common Pitfalls

### Pitfall 1: Ghast Fire Rate Goal Removal Race Condition
**What goes wrong:** Removing RangedAttackGoal in TAIL injection happens after vanilla adds it, but iteration order isn't guaranteed.
**Why it happens:** GoalSelector.availableGoals is a Set, not a List. Iteration order unpredictable.
**How to avoid:** Use @Redirect on goalSelector.addGoal specifically for RangedAttackGoal type, replacing parameters at injection time.
**Warning signs:** Ghasts sometimes fire at 3s, sometimes 4s. Solution: Redirect addGoal, don't iterate-and-remove.

### Pitfall 2: Fireball Velocity Boost Affects Player-Deflected Fireballs
**What goes wrong:** Player deflects fireball, it returns 50% faster than expected, too hard to deflect again.
**Why it happens:** ENTITY_LOAD fires for all LargeFireballs, including deflected ones. Owner changes to player on deflect.
**How to avoid:** Check fireball owner is Ghast, not player. Alternatively, tag fireballs at creation to track origin.
**Warning signs:** Testing deflection feels unbalanced. Solution: Verify owner instanceof Ghast before boosting.

### Pitfall 3: Enderman Teleport-Behind Places Enderman Inside Blocks
**What goes wrong:** Player standing against wall, behind-calculation puts enderman in solid block, teleport fails.
**Why it happens:** Simple subtraction doesn't check terrain. EnderMan.teleportTo() validates position, but returns false on failure.
**How to avoid:** EnderMan.teleportTo() already handles this. Check return value, attempt alternative positions if initial fails.
**Warning signs:** Teleport-behind seems to work <50% of time near walls. Solution: Fallback to random teleport if behind-teleport fails.

### Pitfall 4: Proximity Aggro Overrides Eye Contact Aggro
**What goes wrong:** Enderman aggros from proximity, player looks away, enderman stays aggro'd (expected: de-aggro).
**Why it happens:** Custom goal priority 0 overrides vanilla EnderManLookForPlayerGoal, which handles de-aggro.
**How to avoid:** Check in canContinueToUse() if player still within range OR enderman was angered by eye contact. Preserve vanilla state.
**Warning signs:** Endermen chase forever once proximity-aggro'd. Solution: Lower priority below vanilla goal, or check vanilla anger state.

### Pitfall 5: Fire Spread Doubling Affects All Explosions
**What goes wrong:** TNT, creepers, end crystals all spawn 2x fire. Only ghast fireballs should have expanded fire.
**Why it happens:** Mixing into Explosion or Level.explode affects all explosion sources.
**How to avoid:** Mixin LargeFireball.onHit specifically, check owner is Ghast. Don't mixin global explosion methods.
**Warning signs:** Creepers set fire everywhere. Solution: Target LargeFireball, not Explosion class.

### Pitfall 6: Teleport-Behind Triggers on Environmental Damage
**What goes wrong:** Enderman stands in water, takes damage, teleports behind... nobody (no attacker).
**Why it happens:** hurtServer fires for all damage types, including environmental. source.getEntity() is null for drowning.
**How to avoid:** Check attacker is non-null and instanceof ServerPlayer before calculating behind-position.
**Warning signs:** NullPointerException in teleport logic. Solution: Guard check on attacker type.

### Pitfall 7: Ghast Fireball Velocity Boost Breaks Vanilla Physics
**What goes wrong:** 1.5x velocity makes fireballs pass through blocks due to tick-based collision detection.
**Why it happens:** Minecraft collision detection assumes max projectile speed. Too-fast projectiles skip collision checks.
**How to avoid:** Test 50% boost (1.5x) extensively. If issues arise, reduce to 1.3x or 1.2x increments.
**Warning signs:** Fireballs pass through nether rack walls. Solution: Lower boost multiplier, verify collision works.

### Pitfall 8: Multiple Endermen Teleport-Behind Simultaneously
**What goes wrong:** Player damages enderman A, then enderman B damages player. Both teleport behind player at same position, overlapping.
**Why it happens:** Both trigger 50% chance independently, no position conflict resolution.
**How to avoid:** EnderMan.teleportTo() checks if position occupied. Second teleport fails naturally. Document this as expected.
**Warning signs:** Sometimes only one enderman appears behind. Solution: This is fine, random nature of 50% chance.

## Code Examples

Verified patterns from existing THC codebase and web research:

### Ghast Fire Rate Modification (FR-08)
```java
// Based on: MonsterThreatGoalMixin.registerGoals pattern
// Target: Reduce fire rate by 25% (60 ticks → 80 ticks)
package thc.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modify Ghast fire rate from 3 seconds to 4 seconds.
 *
 * <p>Vanilla Ghast registers RangedAttackGoal with 60 tick interval.
 * THC increases to 80 ticks (25% slower fire rate).
 *
 * <p>Note: This requires removing the vanilla goal first, then adding
 * modified version. May need accessor mixin for availableGoals access.
 */
@Mixin(Ghast.class)
public abstract class GhastRegisterGoalsMixin {
    @Shadow @Final protected GoalSelector goalSelector;

    /**
     * Replace vanilla RangedAttackGoal with modified 80-tick interval version.
     *
     * <p>Challenge: Removing existing goal requires iteration or @Redirect.
     * Implementation will determine best approach.
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$modifyFireRate(CallbackInfo ci) {
        Ghast self = (Ghast) (Object) this;

        // TODO: Remove vanilla RangedAttackGoal (priority 1)
        // Option A: Iterate availableGoals (needs accessor)
        // Option B: Use @Redirect on addGoal call

        // Add modified goal with 80 tick interval
        this.goalSelector.addGoal(1, new RangedAttackGoal(
            self,
            1.0,      // Speed modifier
            80,       // Attack interval: 80 ticks = 4 seconds
            64.0f     // Attack radius
        ));
    }
}
```

### Ghast Fireball Velocity Boost (FR-07)
```kotlin
// Based on: ProjectileEntityMixin velocity boost pattern
// Target: 50% faster fireballs (1.5x velocity)
package thc.monster

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.projectile.LargeFireball

/**
 * Boost ghast fireball velocity by 50%.
 *
 * FR-07: Ghast fireballs travel 50% faster while remaining deflectable.
 */
object GhastProjectileModifier {
    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity !is LargeFireball) return@register

            // Only boost ghast-shot fireballs
            val owner = entity.owner
            if (owner !is Ghast) return@register

            // Apply 50% velocity boost (1.5x multiplier)
            val velocity = entity.deltaMovement
            entity.setDeltaMovement(velocity.scale(1.5))
        }
    }
}
```

### Enderman Proximity Aggro Goal (FR-11)
```java
// Based on: ThreatTargetGoal custom AI goal pattern
// Target: Aggro within 3 blocks regardless of eye contact
package thc.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Custom AI goal for enderman proximity aggro.
 *
 * <p>Endermen aggro if player within 3 blocks, regardless of eye contact.
 * This supplements vanilla eye contact aggro, not replaces it.
 */
public class EndermanProximityAggroGoal extends TargetGoal {
    private static final double AGGRO_RANGE = 3.0;
    private final EnderMan enderman;

    public EndermanProximityAggroGoal(EnderMan enderman) {
        super(enderman, false);
        this.enderman = enderman;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Don't override existing target
        if (this.enderman.getTarget() != null) return false;

        // Find nearest player within 3 blocks
        Player nearest = this.enderman.level()
            .getNearestPlayer(this.enderman, AGGRO_RANGE);

        if (nearest != null && !nearest.isSpectator() && !nearest.isCreative()) {
            this.enderman.setTarget(nearest);
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue targeting until vanilla AI takes over or target invalid
        return this.enderman.getTarget() != null;
    }
}
```

**Injection into Enderman:**
```java
package thc.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.entity.EndermanProximityAggroGoal;

@Mixin(EnderMan.class)
public abstract class EndermanRegisterGoalsMixin {
    @Shadow @Final protected GoalSelector targetSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$addProximityAggro(CallbackInfo ci) {
        EnderMan self = (EnderMan) (Object) this;
        // Priority 1 - runs after vanilla targeting (priority 2+)
        this.targetSelector.addGoal(1, new EndermanProximityAggroGoal(self));
    }
}
```

### Enderman Teleport-Behind on Damage (FR-10)
```java
// Based on: MobDamageThreatMixin.hurtServer pattern
// Target: 50% chance teleport 3 blocks behind player after damage
package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Enderman teleports behind player after damage exchange.
 *
 * <p>FR-10: 50% chance to teleport 3 blocks behind player when:
 * - Player damages enderman
 * - Enderman damages player (separate mixin)
 */
@Mixin(EnderMan.class)
public abstract class EndermanHurtMixin {
    @Shadow protected abstract boolean teleportTo(double x, double y, double z);

    /**
     * Teleport behind player when damaged by player.
     *
     * <p>50% probability, 3 blocks behind based on player look direction.
     */
    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void thc$teleportBehindOnPlayerDamage(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        // Only if damage dealt
        if (!cir.getReturnValue() || amount <= 0) return;

        // Only if attacker is player
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        // 50% chance
        if (!level.random.nextBoolean()) return;

        // Calculate position 3 blocks behind player
        Vec3 playerPos = player.position();
        Vec3 playerLook = player.getLookAngle();
        Vec3 behind = playerPos.subtract(playerLook.scale(3.0));

        // Attempt teleport (built-in validity checks)
        boolean success = this.teleportTo(behind.x, behind.y, behind.z);

        // If behind-teleport fails (solid blocks), vanilla randomTeleport handles it
        // No fallback needed - enderman will teleport randomly as vanilla behavior
    }
}
```

**Note:** Also need mixin on ServerPlayer.hurtServer to catch enderman→player damage. Same pattern, check source.getEntity() instanceof EnderMan.

### Ghast Fireball Fire Spread Doubling (FR-09)
```java
// Pattern needs verification - explosion fire mechanics complex
// Based on: web search findings about Explosion.getToBlow() and fire placement
package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Double fire spread radius for ghast fireball explosions.
 *
 * <p>FR-09: Ghast fireball explosions have 100% larger fire spread radius.
 * Vanilla creates fire on ~1/3 of blocks in explosion sphere. THC places
 * additional fire blocks in expanded radius.
 *
 * <p>Note: This implementation places fire post-explosion rather than
 * modifying explosion calculator. Simpler but may need refinement.
 */
@Mixin(LargeFireball.class)
public abstract class LargeFireballExplosionMixin {

    /**
     * Place additional fire blocks after vanilla explosion.
     *
     * <p>Vanilla explosion handles block breaking, knockback, sound.
     * This mixin adds extra fire placement in 2x radius ring.
     */
    @Inject(method = "onHit", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;explode(...)V",
        shift = At.Shift.AFTER
    ))
    private void thc$expandFireSpread(HitResult hitResult, CallbackInfo ci) {
        LargeFireball self = (LargeFireball) (Object) this;

        // Only expand fire for ghast-shot fireballs
        if (!(self.getOwner() instanceof net.minecraft.world.entity.monster.Ghast)) return;

        // Get impact position
        BlockPos impactPos = BlockPos.containing(hitResult.getLocation());

        // Place fire in expanded radius (vanilla radius ~3, THC radius ~6)
        if (self.level() instanceof ServerLevel level) {
            // Iterate 6 block radius, place fire on valid blocks
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    for (int dz = -6; dz <= 6; dz++) {
                        // Skip inner vanilla radius (already handled)
                        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        if (dist < 3.0 || dist > 6.0) continue;

                        BlockPos firePos = impactPos.offset(dx, dy, dz);

                        // Only place fire on air blocks above solid blocks
                        if (level.getBlockState(firePos).isAir()) {
                            BlockPos below = firePos.below();
                            if (level.getBlockState(below).isSolid()) {
                                // 33% chance (match vanilla fire spread probability)
                                if (level.random.nextFloat() < 0.33f) {
                                    level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**Note:** This approach is brute-force. May need optimization or ExplosionDamageCalculator approach for production.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual goal list iteration | @Redirect on addGoal | Mixin API evolution (2020+) | Cleaner injection, no reflection |
| Custom teleport logic | EnderMan.teleportTo() | Always available | Vanilla handles validity, collision, sound |
| Manual explosion fire | ExplosionDamageCalculator | MC 1.16+ | Custom explosion logic integration |
| ServerTickEvent for proximity | Custom AI Goal | Fabric API 0.40+ | Proper integration with entity AI |
| Velocity modification in tick | ENTITY_LOAD event | Fabric API 0.83+ (2023) | Single-point modification, no tick overhead |

**Deprecated/outdated:**
- Manual GoalSelector iteration: @Redirect is cleaner
- Custom pathfinding for teleport: teleportTo() exists
- Global explosion modification: Target specific projectile types

## Open Questions

Things that couldn't be fully resolved:

1. **Ghast RangedAttackGoal removal mechanism**
   - What we know: Need to remove vanilla goal before adding modified version
   - What's unclear: Best mixin approach - @Redirect vs accessor + iteration
   - Recommendation: Implement @Redirect approach first, fall back to accessor if issues. Verify in decompiled Ghast.registerGoals.

2. **Fire spread exact vanilla mechanics**
   - What we know: Vanilla creates fire on ~1/3 of explosion blocks, checks solid block below
   - What's unclear: Exact probability calculation, interaction with ExplosionDamageCalculator
   - Recommendation: Test brute-force approach (Pattern 5 example). If performance issues, investigate ExplosionDamageCalculator.

3. **Enderman teleport-behind failure rate**
   - What we know: teleportTo() validates position, returns false if invalid
   - What's unclear: How often behind-calculation hits solid blocks in real gameplay
   - Recommendation: Test extensively. If <30% success rate, add fallback positions (behind-left, behind-right).

4. **Fireball velocity boost affects deflection**
   - What we know: 50% faster fireballs still deflectable (user requirement)
   - What's unclear: Does 1.5x velocity break deflection timing or collision detection
   - Recommendation: Playtest thoroughly. If deflection breaks, reduce to 1.3x or 1.2x increments.

5. **Multiple endermen teleport-behind overlap**
   - What we know: Multiple endermen can trigger 50% chance simultaneously
   - What's unclear: Should we prevent overlap, or is random nature acceptable
   - Recommendation: Accept overlap as intended randomness. teleportTo() handles collision naturally.

6. **Proximity aggro priority vs vanilla eye contact**
   - What we know: Proximity goal priority affects interaction with vanilla targeting
   - What's unclear: Should proximity override eye contact, or supplement it
   - Recommendation: Priority 1 (after vanilla priority 2), supplements rather than replaces. Test for conflicts.

## Sources

### Primary (HIGH confidence)
- [THC MonsterThreatGoalMixin](file:///mnt/c/home/code/thc/src/main/java/thc/mixin/MonsterThreatGoalMixin.java) - registerGoals TAIL injection, targetSelector.addGoal pattern
- [THC ProjectileEntityMixin](file:///mnt/c/home/code/thc/src/main/java/thc/mixin/ProjectileEntityMixin.java) - getDeltaMovement/setDeltaMovement velocity modification, shoot method injection
- [THC MobDamageThreatMixin](file:///mnt/c/home/code/thc/src/main/java/thc/mixin/MobDamageThreatMixin.java) - hurtServer TAIL injection, damage event handling
- [THC MonsterModifications.kt](file:///mnt/c/home/code/thc/src/main/kotlin/thc/monster/MonsterModifications.kt) - ENTITY_LOAD pattern for attribute modification
- [THC SimpleEntityBehaviors.kt](file:///mnt/c/home/code/thc/src/main/kotlin/thc/monster/SimpleEntityBehaviors.kt) - ENTITY_LOAD for entity-specific modifications

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Ghast](https://minecraft.wiki/w/Ghast) - Fire rate (60 ticks = 3 seconds), RangedAttackGoal usage
- [Minecraft Wiki - Enderman](https://minecraft.wiki/w/Enderman) - Teleportation on damage (64 attempts), teleport behind behavior
- [Minecraft Wiki - Fireball](https://minecraft.wiki/w/Fireball) - LargeFireball entity, ExplosionPower NBT, fire spread mechanics
- [Minecraft Wiki - Explosion](https://minecraft.wiki/w/Explosion) - Fire generation on 1/3 of blocks, solid block requirement
- [Fabric Wiki - Mixin Injects](https://wiki.fabricmc.net/tutorial:mixin_injects) - @Inject, @Redirect patterns (updated 2026-01-01)
- [GoalSelector Javadoc](https://maven.fabricmc.net/docs/yarn-21w05b+build.11/net/minecraft/entity/ai/goal/GoalSelector.html) - availableGoals structure, addGoal method

### Tertiary (LOW confidence - needs verification)
- WebSearch: RangedAttackGoal parameters - Found attackInterval parameter exists, signature needs source verification
- WebSearch: Enderman teleportRandomly method - Method exists and called from hurtServer, exact signature unclear
- WebSearch: Ghast performRangedAttack - Creates LargeFireball, exact creation parameters need verification
- WebSearch: ExplosionDamageCalculator - Interface exists for custom explosion logic, integration approach needs testing

## Metadata

**Confidence breakdown:**
- Ghast velocity boost: HIGH - Direct pattern match to ProjectileEntityMixin
- Ghast fire rate: MEDIUM - RangedAttackGoal confirmed, removal mechanism needs verification
- Ghast fire spread: MEDIUM - Explosion mechanics known, exact implementation approach unclear
- Enderman proximity aggro: HIGH - Custom AI goal pattern proven in ThreatTargetGoal
- Enderman teleport-behind: HIGH - hurtServer injection proven, teleportTo() method verified
- Overall: MEDIUM-HIGH - Core patterns verified, some implementation details need testing

**Research date:** 2026-01-24
**Valid until:** 2026-02-24 (30 days - Minecraft 1.21.x stable)

**Integration points verified:**
- ServerEntityEvents.ENTITY_LOAD: Used in MonsterModifications.kt, SimpleEntityBehaviors.kt
- Mob.registerGoals: Used in MonsterThreatGoalMixin.java
- LivingEntity.hurtServer: Used in MobDamageThreatMixin.java, LivingEntityMixin.java
- getDeltaMovement/setDeltaMovement: Used in ProjectileEntityMixin.java, AbstractArrowMixin.java

**Needs validation before implementation:**
- Ghast RangedAttackGoal removal approach (@Redirect vs accessor)
- Fire spread implementation (brute-force vs ExplosionDamageCalculator)
- Fireball velocity 1.5x doesn't break deflection mechanics
- Enderman teleport-behind success rate in real terrain
- Proximity aggro goal priority interaction with vanilla targeting
- Player-as-enderman-target damage trigger (need ServerPlayer.hurtServer mixin)

**Known from CONTEXT.md decisions:**
- Fire spread pattern: Claude's discretion (ring vs scatter)
- Deflected fireballs: Must also create expanded fire area
- Fire duration: Vanilla (no extended burn times)
- Implementation details: All at Claude's discretion
