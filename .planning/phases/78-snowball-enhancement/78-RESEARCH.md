# Phase 78: Snowball Enhancement - Research

**Researched:** 2026-02-03
**Domain:** Projectile combat (snowball hit effects, AoE targeting, knockback)
**Confidence:** HIGH

## Summary

This phase implements enhanced snowball combat for Bastion class at Stage 4+. On snowball hit, the target mob and nearby hostile mobs (within 1.5 blocks) receive Slowness III for 2 seconds, plus the target gets knocked back ~1 block away from the thrower.

The implementation requires a new mixin targeting `Snowball.onHitEntity()` since `Snowball` extends `ThrowableItemProjectile` which extends `Projectile`, but the existing `ProjectileEntityMixin` targets the base `Projectile` class and `Snowball` overrides `onHitEntity` without calling super. The pattern follows existing arrow/projectile handling code with established boon gate checks.

**Primary recommendation:** Create a new `SnowballHitMixin` targeting `Snowball.onHitEntity()`, check for Bastion class + boon level >= 4, apply Slowness III (2s) to target and AoE mobs, and apply knockback to target only. Use established patterns from existing mixin code.

## Standard Stack

No new libraries or dependencies required. This phase uses existing infrastructure:

### Core
| Component | Location | Purpose |
|-----------|----------|---------|
| ClassManager | `thc.playerclass.ClassManager` | Check player class |
| StageManager | `thc.stage.StageManager` | Check boon level |
| PlayerClass | `thc.playerclass.PlayerClass` | Bastion enum constant |
| MobEffects | `net.minecraft.world.effect.MobEffects` | SLOWNESS effect |
| MobEffectInstance | `net.minecraft.world.effect.MobEffectInstance` | Effect application |

### Supporting
| Pattern | Example | When to Use |
|---------|---------|-------------|
| Boon gate check | Phase 76/77 pattern | Any class+stage gated feature |
| AoE mob search | `level.getEntitiesOfClass(Mob.class, bbox.inflate(radius), predicate)` | Finding nearby mobs |
| Effect application | `target.addEffect(new MobEffectInstance(...), source)` | Applying status effects |
| Knockback | `mob.setDeltaMovement(direction.x * strength, y, direction.z * strength)` | Pushing mobs away |

## Architecture Patterns

### Recommended Approach: New SnowballHitMixin

Create a dedicated mixin for snowball hit handling rather than modifying the existing `ProjectileEntityMixin`:

**Why a new mixin:**
1. `Snowball` overrides `onHitEntity()` without calling `super.onHitEntity()`
2. The existing `ProjectileEntityMixin` targets `Projectile.onHitEntity()` which Snowball doesn't call
3. Keeps snowball-specific logic isolated and maintainable

**File:** `src/main/java/thc/mixin/SnowballHitMixin.java`

```java
@Mixin(Snowball.class)
public abstract class SnowballHitMixin {
    private static final int THC_SLOWNESS_DURATION = 40;  // 2 seconds
    private static final int THC_SLOWNESS_AMPLIFIER = 2;  // Level III (0-indexed)
    private static final double THC_AOE_RADIUS = 1.5;
    private static final double THC_KNOCKBACK_STRENGTH = 0.4;  // Tunable for ~1 block

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void thc$applyEnhancedSnowballEffects(EntityHitResult result, CallbackInfo ci) {
        Snowball self = (Snowball) (Object) this;
        Entity owner = self.getOwner();

        // Gate: only player-thrown snowballs
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }

        // Gate: Bastion class + Stage 4+ (boon level >= 4)
        PlayerClass playerClass = ClassManager.getClass(player);
        if (playerClass != PlayerClass.BASTION) {  // Or TANK if Phase 75 not complete
            return;
        }
        if (StageManager.getBoonLevel(player) < 4) {
            return;
        }

        Entity hitEntity = result.getEntity();
        if (!(hitEntity instanceof Mob targetMob)) {
            return;
        }

        ServerLevel level = (ServerLevel) self.level();

        // Apply effects to target
        thc$applySlowness(targetMob, player);
        thc$applyKnockback(targetMob, player);

        // Apply effects to nearby hostile mobs
        thc$applyAoESlowness(level, targetMob, player);
    }
}
```

### Pattern 1: Hostile Mob Targeting Filter

Per CONTEXT.md decisions, only affect "hostile mobs (entities currently targeting a player)". This means:
- Must be `MobCategory.MONSTER`
- Must have `mob.getTarget() instanceof Player`

```java
@Unique
private static boolean thc$isHostileMobTargetingPlayer(Mob mob) {
    if (mob.getType().getCategory() != MobCategory.MONSTER) {
        return false;
    }
    return mob.getTarget() instanceof Player;
}
```

This filter:
- Excludes neutral mobs (wolves, iron golems, zombified piglins when passive)
- Excludes passive mobs
- Excludes players
- Only affects mobs actively aggressive toward a player

### Pattern 2: Knockback Away From Thrower

Per CONTEXT.md: "Knockback direction is away from the thrower, not the impact point."

```java
@Unique
private static void thc$applyKnockback(Mob target, ServerPlayer thrower) {
    // Direction: from thrower to mob (away from thrower)
    Vec3 direction = target.position().subtract(thrower.position()).normalize();

    // Apply horizontal knockback + slight upward lift
    target.setDeltaMovement(
        direction.x * THC_KNOCKBACK_STRENGTH,
        0.2,  // Slight upward
        direction.z * THC_KNOCKBACK_STRENGTH
    );
    target.hurtMarked = true;  // Force sync to client
}
```

The `hurtMarked = true` flag ensures the velocity change syncs to the client for visual feedback.

### Pattern 3: AoE Effect Application

```java
@Unique
private static void thc$applyAoESlowness(ServerLevel level, Mob target, ServerPlayer source) {
    AABB area = target.getBoundingBox().inflate(THC_AOE_RADIUS);

    for (Mob nearby : level.getEntitiesOfClass(Mob.class, area,
            SnowballHitMixin::thc$isHostileMobTargetingPlayer)) {
        // Skip the target itself (already has slowness)
        if (nearby == target) {
            continue;
        }
        thc$applySlowness(nearby, source);
    }
}

@Unique
private static void thc$applySlowness(Mob mob, ServerPlayer source) {
    mob.addEffect(
        new MobEffectInstance(MobEffects.SLOWNESS, THC_SLOWNESS_DURATION, THC_SLOWNESS_AMPLIFIER),
        source
    );
}
```

### Anti-Patterns to Avoid

- **Targeting all mobs:** Per CONTEXT.md, only target hostile mobs currently targeting a player. Don't use just `MobCategory.MONSTER` filter alone.
- **Bundling effects:** Per CONTEXT.md, slowness and knockback are independent. If a mob is immune to one, the other should still apply.
- **Checking target for AoE:** The 1.5 block radius is centered on the hit mob's position, not the impact point.
- **Modifying ProjectileEntityMixin:** Snowball overrides onHitEntity without super call, so projectile mixin won't catch snowball hits.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Finding nearby mobs | Custom entity iteration | `level.getEntitiesOfClass(Mob.class, bbox.inflate(radius), predicate)` | Efficient spatial query |
| Class check | Direct attachment read | `ClassManager.getClass(player)` | Type-safe enum conversion |
| Stage check | Direct attachment read | `StageManager.getBoonLevel(player)` | Handles null with default 0 |
| Effect application | Manual effect logic | `mob.addEffect(new MobEffectInstance(...), source)` | Handles vanilla stacking |
| Velocity sync | Manual packet | `mob.hurtMarked = true` | Forces client sync |

## Common Pitfalls

### Pitfall 1: Snowball Hits Not Detected

**What goes wrong:** Enhanced effects never apply even when conditions are met.

**Why it happens:** Targeting `Projectile.onHitEntity()` in a mixin, but `Snowball` overrides `onHitEntity()` without calling super.

**How to avoid:** Create mixin targeting `Snowball.class` directly, not `Projectile.class`.

**Warning signs:** All snowballs behave vanilla, no slowness/knockback ever applied.

### Pitfall 2: Wrong Mob Filter (Neutral Mobs Affected)

**What goes wrong:** Wolves, iron golems get slowed/knocked back by snowballs.

**Why it happens:** Using only `MobCategory.MONSTER` filter without checking if mob is targeting a player.

**How to avoid:** Use compound filter:
```java
mob.getType().getCategory() == MobCategory.MONSTER && mob.getTarget() instanceof Player
```

**Warning signs:** Player's wolf gets slowed when fighting alongside player.

### Pitfall 3: Knockback Direction Inconsistent

**What goes wrong:** Knockback direction varies based on snowball trajectory.

**Why it happens:** Calculating direction from impact point instead of thrower position.

**How to avoid:** Use `target.position().subtract(thrower.position())` not `target.position().subtract(snowball.position())`.

**Warning signs:** Mobs sometimes knocked toward player instead of away.

### Pitfall 4: Knockback Too Strong or Too Weak

**What goes wrong:** Mobs fly too far or barely move.

**Why it happens:** Knockback strength not tuned correctly. The value is velocity, not blocks.

**How to avoid:** Use ~0.4 horizontal strength for approximately 1 block knockback. The existing parry stun uses `0.5` horizontal for a similar effect. Test and tune.

**Warning signs:** Knockback distance doesn't match ~1 block requirement.

### Pitfall 5: AoE Affects Target Twice

**What goes wrong:** Target mob gets double slowness duration.

**Why it happens:** AoE loop doesn't skip the direct hit target.

**How to avoid:** Check `if (nearby == target) continue;` in AoE loop.

**Warning signs:** Target has longer slowness than nearby mobs.

### Pitfall 6: Effect Applied on Client Side

**What goes wrong:** Effects don't actually apply or cause errors.

**Why it happens:** Checking conditions on client side where attachments may not be synced.

**How to avoid:** Check `self.level() instanceof ServerLevel` or ensure inject only runs server-side.

**Warning signs:** Client console errors, effects not visible to other players.

## Code Examples

### Existing Effect Application Pattern

```java
// From LivingEntityMixin.java line 196-201
MobEffectInstance slow = new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, 5);
for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(3.0D),
    entity -> entity.getType().getCategory() == MobCategory.MONSTER)) {
    mob.addEffect(slow, player);
}
```

### Existing Knockback Pattern

```java
// From LivingEntityMixin.java line 202-204
Vec3 direction = mob.position().subtract(player.position()).normalize();
mob.setDeltaMovement(direction.x * 0.5, 0.2, direction.z * 0.5);
mob.hurtMarked = true;
```

### Existing Boon Gate Pattern

```java
// From Phase 76/77 research
if (player instanceof ServerPlayer serverPlayer) {
    PlayerClass playerClass = ClassManager.getClass(serverPlayer);
    int boonLevel = StageManager.getBoonLevel(serverPlayer);
    if (playerClass != PlayerClass.BASTION || boonLevel < 4) {  // Stage 4+
        return;  // Not enhanced
    }
}
```

### Vanilla Snowball onHitEntity

The vanilla `Snowball.onHitEntity()` method:
1. Checks if entity is Blaze (deals 3 damage) or other (0 damage)
2. Calls `entity.hurt(DamageSource.thrownProjectile(...), damage)`
3. Does NOT call `super.onHitEntity()`

This means any `Projectile.onHitEntity()` mixin won't intercept snowball hits.

## Effect Values

### Slowness III (2 seconds)
- Amplifier: 2 (0-indexed, so level III)
- Duration: 40 ticks (2 seconds)
- Effect: 60% speed reduction

### Knockback Tuning
| Strength | Approx Distance | Notes |
|----------|-----------------|-------|
| 0.3 | ~0.5 blocks | Too weak |
| 0.4 | ~1 block | Target for SNOW-03 |
| 0.5 | ~1.2 blocks | Existing parry stun |
| 0.6 | ~1.5 blocks | Too strong |

Recommendation: Start with 0.4, test in-game, tune as needed.

## Particle/Sound Considerations

Per CONTEXT.md "Claude's Discretion" section, particle/sound effects are optional.

**Options:**
1. **No additional effects** - Simple, minimal code
2. **Frost particles on impact** - `ParticleTypes.ITEM_SNOWBALL` or `ParticleTypes.SNOWFLAKE`
3. **Subtle sound** - `SoundEvents.PLAYER_HURT_FREEZE` at low volume

**Recommendation:** No additional effects initially. The slowness effect has its own visual indicator. Keep implementation simple and add effects later if desired.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Snowball = useless in combat | Enhanced crowd control | This phase | Bastion gets utility tool |

**Existing snowball behavior preserved:**
- Non-Bastion: vanilla snowball (no effects except Blaze damage)
- Bastion Stage 1-3: vanilla snowball
- All players: 64 stack size (from existing SnowballItemMixin)

## Open Questions

### Resolved

1. **Q: Should AoE affect the direct hit target?**
   A: Target gets slowness from direct hit, AoE affects others. Skip target in AoE loop.

2. **Q: What about mobs immune to effects?**
   A: Per CONTEXT.md, apply effects independently. Slowness and knockback each attempt separately.

3. **Q: Does PlayerClass.BASTION exist or is it still TANK?**
   A: Phase 75 renames TANK to BASTION. Check which constant exists; logic is the same.

### None Remaining

All implementation details are resolved by CONTEXT.md decisions and existing codebase patterns.

## Files to Modify

| File | Change |
|------|--------|
| `src/main/java/thc/mixin/SnowballHitMixin.java` | NEW: Snowball hit enhancement mixin |
| `src/main/resources/thc.mixins.json` | Add SnowballHitMixin to mixins array |

## Testing Strategy

### Manual Tests

1. **Non-Bastion snowball does nothing special**
   - Select any non-Bastion class
   - Throw snowball at zombie
   - Expect: No slowness, no knockback (vanilla behavior)

2. **Bastion Stage 3 snowball does nothing special**
   - Select Bastion, advance to Stage 3
   - Throw snowball at zombie
   - Expect: No slowness, no knockback (vanilla behavior)

3. **Bastion Stage 4+ snowball slows target**
   - Select Bastion, advance to Stage 4
   - Throw snowball at zombie
   - Expect: Zombie has Slowness III (2s), visible particles on mob

4. **Bastion Stage 4+ snowball knocks back target**
   - Select Bastion, advance to Stage 4
   - Throw snowball at zombie facing player
   - Expect: Zombie pushed ~1 block away from player

5. **AoE affects nearby hostile mobs**
   - Select Bastion, advance to Stage 4
   - Spawn multiple zombies close together
   - Throw snowball at one
   - Expect: All zombies within 1.5 blocks get Slowness III

6. **Neutral mobs NOT affected**
   - Select Bastion, advance to Stage 4
   - Spawn wolf (neutral, not targeting player)
   - Throw snowball near wolf
   - Expect: Wolf not slowed (not targeting a player)

7. **Players NOT affected**
   - Multiplayer: have another player stand near target
   - Throw snowball at nearby zombie
   - Expect: Other player not slowed or knocked back

8. **Effect stacking uses vanilla behavior**
   - Throw snowball at zombie
   - Immediately throw another snowball
   - Expect: Duration resets (vanilla behavior), no custom stacking

## Sources

### Primary (HIGH confidence)
- Existing codebase: `LivingEntityMixin.java` - AoE effect + knockback pattern
- Existing codebase: `ProjectileEntityMixin.java` - Projectile hit handling
- Existing codebase: `AbstractArrowMixin.java` - Arrow-specific hit handling pattern
- Existing codebase: `ClassManager.java`, `StageManager.java` - Boon gate pattern
- [Fabric Wiki: Creating Custom Projectiles](https://wiki.fabricmc.net/tutorial:projectiles) - onHitEntity pattern
- [NeoForge Javadocs: Snowball class](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/world/entity/projectile/Snowball.html) - Class hierarchy

### Secondary (MEDIUM confidence)
- Phase 76, 77 RESEARCH.md - Boon gate implementation patterns

## Metadata

**Confidence breakdown:**
- Mixin targeting: HIGH - clear class hierarchy, existing arrow pattern to follow
- Boon gate: HIGH - exact pattern exists in codebase (Phase 76/77)
- Effect application: HIGH - exact pattern exists (parry stun)
- Knockback: HIGH - exact pattern exists (parry knockback)
- Hostile mob filter: HIGH - derived from CONTEXT.md decisions + existing patterns

**Research date:** 2026-02-03
**Valid until:** No expiration (uses internal codebase patterns)

---

*Research confidence: HIGH - all information sourced from existing codebase patterns and official Fabric documentation*
