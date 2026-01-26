# Phase 50: Elytra Flight Changes - Research

**Researched:** 2026-01-25
**Domain:** Minecraft elytra physics, firework boost mechanics, Fabric mixins
**Confidence:** MEDIUM

## Summary

This phase requires two distinct modifications to elytra flight mechanics:

1. **Firework boost cancellation**: Prevent firework rockets from boosting player velocity during elytra flight. The boost happens in `FireworkRocketEntity.tick()` where the entity detects an attached gliding player and adds velocity based on the look direction.

2. **Pitch-based speed multipliers**: Modify elytra velocity deltas based on pitch. When the player is gliding (`isFallFlying() == true`), intercept the velocity change per tick and multiply by 2x when diving (pitch positive, looking down) or 1.8x when ascending (pitch negative, looking up).

**Primary recommendation:** Use two mixins: (1) `FireworkRocketEntityMixin` to cancel the velocity addition to attached players, and (2) `LivingEntityElytraMixin` or `PlayerElytraMixin` to modify velocity deltas during the `travel()` or `tickFallFlying()` method.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.100+ | Entity events, networking | Required for mod infrastructure |
| Mixin | 0.8.5+ | Bytecode injection | Standard for vanilla behavior modification |
| Fabric Loader | 0.18.4+ | Mod loading | Required runtime |

### Supporting

No additional libraries needed. This phase uses only vanilla Minecraft classes and mixin injections.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Mixin injection | Fabric Entity Events | Events don't expose fine-grained velocity control during flight |
| @ModifyVariable | @Redirect | @Redirect is more fragile, conflicts with other mods |

**Installation:**
```bash
# No new dependencies required - existing mod infrastructure
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/mixin/
    FireworkRocketEntityMixin.java  # Block firework boost
    PlayerElytraMixin.java          # Pitch-based speed multipliers
```

### Pattern 1: Firework Boost Cancellation

**What:** Intercept `FireworkRocketEntity.tick()` where it adds velocity to attached player
**When to use:** When the firework has an attached gliding entity and would normally boost them
**Approach:** Use `@Inject` at HEAD with check for attached player, or `@Redirect` the velocity addition call

The firework entity's tick method checks:
1. If there is an attached entity (shooter who used firework while gliding)
2. If that entity is gliding (`isFallFlying()`)
3. If so, it adds velocity based on the player's look direction

**Example (conceptual):**
```java
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {

    // Option A: Cancel the entire velocity addition
    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private void thc$cancelElytraBoost(LivingEntity entity, Vec3 velocity) {
        // Do nothing - cancel the boost
        // Firework still visually fires, just no velocity change
    }

    // Option B: Zero out the velocity if target is player
    @Redirect(...)
    private void thc$cancelElytraBoost(LivingEntity entity, Vec3 velocity) {
        if (entity instanceof Player) {
            return; // Cancel for players only
        }
        entity.addDeltaMovement(velocity);
    }
}
```

### Pattern 2: Pitch-Based Velocity Multipliers

**What:** Intercept elytra flight physics to multiply velocity delta based on pitch
**When to use:** Every tick while player is gliding
**Approach:** Inject into `LivingEntity.travel()` or capture velocity before/after and apply multiplier

Per CONTEXT.md decisions:
- Multipliers apply to the **delta** (speed change per tick), not absolute velocity
- Diving (positive pitch, looking down): multiply speed gain by 2x
- Ascending (negative pitch, looking up): multiply speed loss by 1.8x
- Sign of pitch determines which multiplier applies
- Applies whenever gliding flag is set

**Key insight from simulation code:**
```
- Gravity/lift: velY += -0.08 + sqrpitchcos * 0.06
- When pitch < 0 (looking up): yacc = hvel * -pitchsin * 0.04 (gains altitude, loses speed)
- Drag: velX *= 0.99; velY *= 0.98; velZ *= 0.99 (1-2% loss per tick)
```

**Example (conceptual):**
```java
@Mixin(LivingEntity.class)
public abstract class PlayerElytraMixin {
    @Unique private double thc$velocityXBefore;
    @Unique private double thc$velocityYBefore;
    @Unique private double thc$velocityZBefore;

    @Inject(method = "travel", at = @At("HEAD"))
    private void thc$captureVelocityBefore(Vec3 movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player && player.isFallFlying()) {
            Vec3 vel = player.getDeltaMovement();
            thc$velocityXBefore = vel.x;
            thc$velocityYBefore = vel.y;
            thc$velocityZBefore = vel.z;
        }
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void thc$applyPitchMultipliers(Vec3 movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player) || !player.isFallFlying()) {
            return;
        }

        Vec3 velAfter = player.getDeltaMovement();
        double deltaX = velAfter.x - thc$velocityXBefore;
        double deltaY = velAfter.y - thc$velocityYBefore;
        double deltaZ = velAfter.z - thc$velocityZBefore;

        float pitch = player.getXRot(); // Positive = looking down, Negative = looking up

        // Determine multiplier based on pitch sign
        // Diving (looking down): pitch > 0, naturally gains speed, multiply by 2x
        // Ascending (looking up): pitch < 0, naturally loses speed, multiply by 1.8x
        double multiplier = pitch > 0 ? 2.0 : 1.8;

        // Apply multiplier to delta
        double newX = thc$velocityXBefore + deltaX * multiplier;
        double newY = thc$velocityYBefore + deltaY * multiplier;
        double newZ = thc$velocityZBefore + deltaZ * multiplier;

        player.setDeltaMovement(newX, newY, newZ);
        player.hurtMarked = true; // Sync to client
    }
}
```

### Anti-Patterns to Avoid

- **Modifying absolute velocity instead of delta:** Would break elytra physics entirely
- **Using @Redirect on multiple methods:** Creates mod conflicts; prefer @Inject when possible
- **Forgetting hurtMarked:** Velocity changes won't sync to client without this flag
- **Client-side velocity modification:** Changes must happen server-side; client only displays

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Elytra physics simulation | Custom flight physics | Vanilla elytra + multipliers | Vanilla physics are complex and well-tuned |
| Velocity sync | Manual network packets | `hurtMarked = true` | Built-in mechanism for velocity sync |
| Player gliding detection | Custom checks | `player.isFallFlying()` | Standard API method |
| Pitch reading | Manual rotation math | `player.getXRot()` | Built-in accessor |

**Key insight:** The goal is to amplify natural glide physics, not replace them. Let vanilla handle the complex physics; just multiply the delta afterward.

## Common Pitfalls

### Pitfall 1: Wrong Injection Point for Firework Boost

**What goes wrong:** Injecting too early or late misses the velocity addition
**Why it happens:** `FireworkRocketEntity.tick()` has multiple code paths
**How to avoid:** Use `@Redirect` on the specific `addDeltaMovement` call targeting the attached entity
**Warning signs:** Boost still works, or firework behavior completely breaks

### Pitfall 2: Client vs Server Velocity Modification

**What goes wrong:** Velocity changes don't persist or cause rubber-banding
**Why it happens:** Client world is read-only; modifications must happen on server
**How to avoid:** Check `!level.isClientSide()` or use `ServerPlayer` type check
**Warning signs:** Player snaps back to previous position, or changes only visible locally

### Pitfall 3: Forgetting hurtMarked Flag

**What goes wrong:** Velocity changes happen on server but client doesn't see them
**Why it happens:** Minecraft optimizes network traffic; velocity sync requires flag
**How to avoid:** Always set `entity.hurtMarked = true` after velocity changes
**Warning signs:** Server logs show correct values but player movement is unchanged

### Pitfall 4: Pitch Sign Confusion

**What goes wrong:** Multipliers applied in reverse
**Why it happens:** Minecraft's pitch is counterintuitive: positive = down, negative = up
**How to avoid:** Test with logging: `LOGGER.info("Pitch: " + player.getXRot())`
**Warning signs:** Player accelerates when looking up, slows when diving

### Pitfall 5: Neutral Pitch Zone

**What goes wrong:** Jitter or oscillation at pitch = 0
**Why it happens:** Sign changes rapidly near zero
**How to avoid:** Per CONTEXT.md, no discrete thresholds - sign determines multiplier. At exactly 0, use diving multiplier (or whichever is more generous)
**Warning signs:** Stuttering flight when looking at horizon

## Code Examples

Verified patterns from existing codebase (HIGH confidence):

### Velocity Modification with Sync
```java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/WindChargePlayerBoostMixin.java
// Apply 50% Y velocity boost
double boostedY = velocity.y * THC_BOOST_MULTIPLIER;
player.setDeltaMovement(velocity.x, boostedY, velocity.z);
player.hurtMarked = true;  // CRITICAL: sync to client
```

### HEAD + TAIL Injection for Delta Capture
```java
// Source: Project pattern from FoodDataMixin
// Capture state at HEAD, process at TAIL
@Inject(method = "travel", at = @At("HEAD"))
private void thc$captureBeforeState(CallbackInfo ci) {
    // Store "before" values
}

@Inject(method = "travel", at = @At("TAIL"))
private void thc$applyAfterProcessing(CallbackInfo ci) {
    // Calculate delta and apply multiplier
}
```

### @Redirect for Method Call Interception
```java
// Source: Standard mixin pattern
@Redirect(
    method = "tick",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
)
private void thc$interceptVelocityAdd(LivingEntity entity, Vec3 velocity) {
    // Either cancel or modify the call
}
```

### ServerPlayer Type Check Pattern
```java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/ProjectileEntityMixin.java
if (!(owner instanceof ServerPlayer player)) {
    return;
}
// Now safe to modify server-side state
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `velocityModified` flag | `hurtMarked` flag | MC 1.18+ | Use hurtMarked for velocity sync |
| `getMotion()` | `getDeltaMovement()` | Mojang mappings | Same method, different name in mappings |
| `setMotion()` | `setDeltaMovement()` | Mojang mappings | Same method, different name in mappings |

**Deprecated/outdated:**
- `velocityModified`: Use `hurtMarked` instead for consistent behavior

## Open Questions

1. **Exact tick() structure in FireworkRocketEntity**
   - What we know: It checks for attached gliding entity and adds velocity
   - What's unclear: Exact method signature for @Redirect target in 1.21.11
   - Recommendation: Decompile or use Yarn mappings to verify target descriptor

2. **Riptide trident interaction**
   - What we know: Per CONTEXT.md, riptide should work normally
   - What's unclear: Whether riptide velocity goes through same code path
   - Recommendation: Test after implementation; may need explicit riptide exclusion

3. **Multiple firework usage**
   - What we know: Players can spam fireworks
   - What's unclear: Whether blocking one cancels all pending boosts
   - Recommendation: Each firework entity is independent; blocking one doesn't affect others

## Sources

### Primary (HIGH confidence)
- Existing codebase mixins: WindChargePlayerBoostMixin.java, ProjectileEntityMixin.java, FoodDataMixin.java
- Fabric Wiki Mixin Examples: https://wiki.fabricmc.net/tutorial:mixin_examples

### Secondary (MEDIUM confidence)
- Yarn API docs (LivingEntity 1.21): https://maven.fabricmc.net/docs/yarn-1.21+build.9/net/minecraft/entity/LivingEntity.html
- Yarn API docs (FireworkRocketEntity): https://maven.fabricmc.net/docs/yarn-1.21+build.9/net/minecraft/entity/projectile/FireworkRocketEntity.html
- Fabric API Elytra PR #1815: https://github.com/FabricMC/fabric/pull/1815
- FabricMC velocity discussion: https://github.com/orgs/FabricMC/discussions/1949
- Elytra simulation code analysis: https://gist.github.com/samsartor/a7ec457aca23a7f3f120

### Tertiary (LOW confidence)
- Minecraft Wiki (Elytra): https://minecraft.wiki/w/Elytra
- Minecraft Wiki (Firework Rocket): https://minecraft.wiki/w/Firework_Rocket

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Uses established codebase patterns
- Architecture: MEDIUM - Injection points need verification against 1.21.11 decompiled source
- Pitfalls: HIGH - Common issues well-documented in codebase and community

**Research date:** 2026-01-25
**Valid until:** 2026-02-25 (30 days - stable domain, patterns unlikely to change)
