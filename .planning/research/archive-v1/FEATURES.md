# Features Research: v1.1 Vanilla Mechanics

**Researched:** 2026-01-18
**Domain:** Minecraft 1.21.11 vanilla mechanics modification
**Confidence:** HIGH (verified via official wiki, Fabric API docs)

## Summary

Research into four vanilla mechanics systems that v1.1 will modify: drowning, spears (Mounts of Mayhem), projectile physics, and mob aggro. All systems have clear modification points via mixins or Fabric API events.

**Key findings:**
- Drowning uses `getNextAirUnderwater()` method - can slow by returning higher values
- Spears from Mounts of Mayhem have known loot tables and mob spawn equipment - removable via datapack overrides and mob spawn mixins
- Projectile physics apply gravity/drag per-tick in predictable order - injectable via mixin
- Mob aggro uses `setTarget()` and GoalSelector - straightforward programmatic redirection

## Drowning Mechanics

### Vanilla Behavior

**Air Supply System:**
- `getAir()` returns current air in ticks (integer)
- `getMaxAir()` returns 300 (15 seconds at 20 ticks/second)
- Air displayed as 10 bubbles (30 ticks per bubble)
- Air decreases by 1 per tick when submerged

**Drowning Trigger:**
- When `air` reaches -20, damage is dealt
- After damage, air resets to 0
- Damage: 2 HP (1 heart) per occurrence
- Effectively: damage every 20 ticks (1 second) once drowning

**Air Recovery:**
- When player exits water: air regenerates at rate of 1 bubble per 4 ticks
- Full recovery: 300 ticks (15 seconds) to go from 0 to full

**LivingEntity Methods (Mojang mappings):**
```java
// In LivingEntity.baseTick():
if (this.isAlive() && this.isUnderWater() && !this.canBreatheUnderwater()) {
    this.setAir(this.decreaseAirSupply(this.getAir()));
    if (this.getAir() == -20) {
        this.setAir(0);
        // spawn bubbles
        this.hurt(this.damageSources().drown(), 2.0F);
    }
} else {
    this.setAir(this.getMaxAir());
}

// decreaseAirSupply default:
protected int decreaseAirSupply(int air) {
    // Respiration enchantment logic
    return air - 1;
}
```

**Relevant Constants:**
| Constant | Value | Meaning |
|----------|-------|---------|
| Max air | 300 | 15 seconds underwater |
| Damage threshold | -20 | When damage occurs |
| Damage amount | 2 HP | 1 heart per tick of drowning |
| Recovery rate | 1/4 ticks | ~4 bubbles per second |

**Edge Cases:**
- Water Breathing potion prevents air decrease entirely
- Respiration enchantment: `x/(x+1)` chance to NOT decrease air per tick
- Conduit Power regenerates air while underwater
- Aquatic mobs (fish, dolphins, drowned) immune to drowning
- Undead mobs immune to drowning (zombie converts to drowned instead)

### Modification Target

**Goal:** 4x slower drowning (60 seconds underwater instead of 15)

**Approach 1: Modify air decrease rate (RECOMMENDED)**
Mixin to `LivingEntity.getNextAirUnderwater()` (yarn) or override `decreaseAirSupply()`:
```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void thc$slowDrowning(int air, CallbackInfoReturnable<Integer> cir) {
        // Only decrease every 4th tick (randomized for smoothness)
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.getRandom().nextInt(4) != 0) {
            cir.setReturnValue(air); // No decrease this tick
        }
        // Otherwise let vanilla handle (air - 1)
    }
}
```

**Approach 2: Increase max air**
Set `getMaxAir()` to return 1200 instead of 300.
- Downside: HUD would show 40 bubbles (visual clutter)
- Upside: Simpler implementation

**Recommendation:** Approach 1 - maintains vanilla HUD, players intuitively understand bubble drain rate is slower.

**Verification:** Test that player can stay underwater ~60s before first damage tick.

## Spear System (Mounts of Mayhem)

### Vanilla Spears

**Spear Tiers:**
| Tier | Jab Damage | Attack Speed | Durability | Charge Multiplier |
|------|------------|--------------|------------|-------------------|
| Wooden | 1 | 1.54 | 59 | 0.7x |
| Golden | 1 | 1.05 | 32 | 0.7x |
| Stone | 2 | 1.33 | 131 | 0.82x |
| Copper | 2 | 1.18 | 190 | 0.82x |
| Iron | 3.5 | 1.05 | 250 | 0.95x |
| Diamond | 4 | 0.95 | 1561 | 1.075x |
| Netherite | 5 | 0.87 | 2031 | 1.2x |

**Crafting Recipes (7 recipes to remove):**
- Diagonal pattern: Material + Stick + Stick
- Netherite: Diamond spear + Netherite upgrade smithing template + Netherite ingot

**Recipe IDs to remove:**
- `minecraft:wooden_spear`
- `minecraft:stone_spear`
- `minecraft:copper_spear`
- `minecraft:iron_spear`
- `minecraft:golden_spear`
- `minecraft:diamond_spear`
- `minecraft:netherite_spear_smithing`

**Mob Spawn Equipment:**
| Mob | Spear Type | Spawn Conditions |
|-----|------------|------------------|
| Zombie | Iron | Natural spawn (some %) |
| Husk | Iron | Natural spawn (some %) |
| Zombie Villager | Iron | Natural spawn (some %) |
| Zombified Piglin | Golden | Natural spawn (some %) |
| Piglin | Golden | Natural spawn (some %) |
| Zombie Horseman | Iron | Zombie riding zombie horse |
| Camel Husk Jockey | Iron | Husk riding camel in desert |

**Loot Tables Containing Spears:**
| Structure | Spear Type | Chance |
|-----------|------------|--------|
| Ocean Ruins (small) | Stone | 28.5% |
| Ocean Ruins (large) | Stone | 26.3% |
| Village Weaponsmith | Copper | 30.6% |
| Village Weaponsmith | Iron | 22.9% |
| Buried Treasure | Iron | 16.7% |
| Bastion Remnant | Diamond | 15.2% |
| End City | Enchanted Diamond | 12.7% |

**Loot Table Paths to Override:**
- `data/minecraft/loot_table/chests/buried_treasure.json`
- `data/minecraft/loot_table/chests/village/village_weaponsmith.json`
- `data/minecraft/loot_table/chests/bastion_treasure.json`
- `data/minecraft/loot_table/chests/end_city_treasure.json`
- `data/minecraft/loot_table/chests/underwater_ruin_small.json`
- `data/minecraft/loot_table/chests/underwater_ruin_big.json`

### Removal Scope

**1. Crafting Recipes (7 items):**
Extend existing `RecipeManagerMixin` pattern to filter spear recipes:
```java
private static final Set<String> REMOVED_RECIPES = Set.of(
    "shield", "wooden_spear", "stone_spear", "copper_spear",
    "iron_spear", "golden_spear", "diamond_spear",
    "netherite_spear_smithing"
);

// In thc$removeShieldRecipe (rename to thc$removeRecipes):
if (!REMOVED_RECIPES.contains(holder.id().location().getPath())) {
    filtered.add(holder);
}
```

**2. Loot Tables (6 files):**
Create override JSON files in `data/minecraft/loot_table/chests/` that copy vanilla tables minus spear entries.

Alternative: Use `LootTableEvents.MODIFY` to filter entries at runtime:
```java
LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
    if (source.isBuiltin() && isSpearLootTable(key)) {
        // Filter out spear entries from pools
    }
});
```

**3. Mob Spawn Equipment (6+ mob types):**
Mixin to mob initialization methods to remove/replace spear equipment:
```java
@Mixin(ZombieEntity.class) // and similar for Husk, ZombifiedPiglin, etc.
public abstract class ZombieEntityMixin {
    @Inject(method = "initialize", at = @At("TAIL"))
    private void thc$removeSpear(/* params */, CallbackInfoReturnable<SpawnGroupData> cir) {
        // Check if holding spear, replace with nothing or sword
    }
}
```

**Verification:**
- Cannot craft any spear
- Spears do not appear in structure chests
- Zombies/Piglins spawn without spears
- Existing spears in world still function (no crash)

## Projectile Physics

### Vanilla Calculation

**Physics Order Per Tick:**
1. Apply acceleration (gravity)
2. Apply drag (velocity multiplier)
3. Update position

**Note:** Changed in Java Edition 1.21.2 - previously was Position, Drag, Acceleration.

**Gravity and Drag Values:**
| Projectile Type | Gravity (blocks/tick^2) | Drag (multiplier) |
|-----------------|-------------------------|-------------------|
| Arrow | -0.05 | 0.99 |
| Trident | -0.05 | 0.99 |
| Snowball | -0.03 | 0.99 |
| Egg | -0.03 | 0.99 |
| Ender Pearl | -0.03 | 0.99 |
| Splash Potion | -0.05 | 0.99 |
| Experience Bottle | -0.07 | 0.99 |
| Llama Spit | -0.06 | 0.99 |
| Fireball | +0.10 (upward/recoil) | 0.95 |

**Velocity Calculation (simplified):**
```java
// Each tick:
velocity.y += gravity;  // Apply gravity
velocity.x *= drag;
velocity.y *= drag;
velocity.z *= drag;
position += velocity;
```

**Initial Velocities:**
- Thrown items (snowball, egg, pearl): power = 1.5 (approx 1.5 blocks/tick initial)
- Splash potions: power = 0.5, -20 degree pitch offset
- Fully charged arrow: ~3.0 blocks/tick (approx 60 m/s)

**Terminal Velocities:**
| Entity | Terminal Velocity |
|--------|-------------------|
| Arrow | 100 blocks/second |
| Player (falling) | 78.4 blocks/second |
| Item | 40 blocks/second |

**Water Behavior:**
- In water: drag becomes 0.6-0.8 (much higher resistance)
- Arrows effectively stop within ~9 ticks underwater

### Modification Points

**Goal:** 20% velocity boost + quadratic gravity after 8 blocks

**Approach: Mixin to ProjectileEntity tick:**
```java
@Mixin(Projectile.class)  // or specific subclass
public abstract class ProjectileMixin {
    @Unique
    private int thc$ticksAlive = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void thc$modifyPhysics(CallbackInfo ci) {
        Projectile self = (Projectile)(Object)this;
        if (!isPlayerProjectile(self)) return;

        thc$ticksAlive++;
        Vec3 velocity = self.getDeltaMovement();

        // 20% velocity boost on launch (tick 1)
        if (thc$ticksAlive == 1) {
            self.setDeltaMovement(velocity.scale(1.2));
        }

        // Quadratic gravity after 8 blocks (~16 ticks for snowball)
        if (hasFlownDistance(self, 8.0)) {
            double extraGravity = (thc$ticksAlive - 16) * 0.001; // Quadratic ramp
            self.setDeltaMovement(velocity.add(0, -extraGravity, 0));
        }
    }
}
```

**Distance Tracking:**
Track spawn position and current position to calculate distance flown.

**Edge Cases:**
- Projectiles in water (already heavily slowed)
- Projectiles hitting blocks (no modification needed)
- Projectiles from dispensers (only modify player-thrown)

**Verification:**
- Snowball travels further initially but drops faster after 8 blocks
- Arrow behavior similar
- Non-player projectiles unchanged

## Aggro System

### Vanilla Targeting

**Target Selection Components:**
1. **GoalSelector** - Manages AI goals with priorities
2. **TargetGoal classes** - Specific targeting behaviors:
   - `ActiveTargetGoal<T>` - Finds targets by entity class
   - `RevengeGoal` - Targets attacker after being hit
   - `TrackTargetGoal` - Maintains current target

**How Mobs Pick Targets:**

1. **Initial Target Selection:**
   - Mobs check `ActiveTargetGoal` criteria each tick
   - Lower priority goals override higher ones
   - Visibility and range checks applied

2. **Target Criteria:**
   - Entity class (player, iron golem, etc.)
   - Distance (follow range attribute)
   - Line of sight
   - Custom predicates (e.g., light level for zombies)

3. **Revenge Targeting:**
   - When damaged, `RevengeGoal` triggers
   - Sets attacker as target
   - May call allies (wolves, piglins, etc.)

**Key API Methods (MobEntity):**
```java
getTarget()           // Returns current LivingEntity target
setTarget(LivingEntity) // Sets new target
getGoalSelector()     // Access to AI goals
getTargetSelector()   // Access to targeting goals
```

**Goal Priority System:**
- Lower number = higher priority
- Goals compete for control flags (MOVE, LOOK, JUMP, TARGET)
- Running goal replaced only by lower-priority goal that can start

### Redirection Methods

**Goal:** Player projectile hit causes mob to target shooter

**Approach 1: Set Target Directly (RECOMMENDED)**
```java
// In projectile hit handler:
if (target instanceof Mob mob && source instanceof ServerPlayer player) {
    mob.setTarget(player);
}
```

**Approach 2: Simulate Damage Attribution**
The damage source contains attacker info - mobs naturally revenge-target attackers.
```java
DamageSource source = damageSources().thrown(projectile, player);
target.hurt(source, 0.0F); // Zero damage but sets last attacker
```

**Approach 3: Add Custom Revenge Goal**
For persistent aggro:
```java
// Custom goal that maintains target longer
mob.targetSelector.addGoal(1, new CustomRevengeGoal(mob, player));
```

**For v1.1 Implementation:**
Since projectiles will apply Speed II + Glowing (which involves `addEffect()`), the aggro can be set during the same hit event:
```java
// In onEntityHit callback:
if (hitEntity instanceof Mob mob) {
    Entity owner = this.getOwner();
    if (owner instanceof ServerPlayer player) {
        // Apply effects
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1)); // Speed II for 6s
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0)); // Glowing for 6s

        // Redirect aggro
        mob.setTarget(player);
    }
}
```

**Edge Cases:**
- Passive mobs (pigs, cows) - cannot have targets, skip aggro logic
- Bosses (Wither, Dragon) - immune to many effects, may ignore setTarget
- Tamed mobs - should probably not aggro owner
- Player in creative mode - mobs typically can't target

## Potion Effects

### Speed II

**Effect Details:**
- ID: `minecraft:speed`
- Amplifier: 1 (Speed II = amplifier 1, Speed I = amplifier 0)
- Movement bonus: +20% per level = +40% for Speed II
- Affects walking, sprinting, swimming
- FOV expansion (visual only)

**Duration Options:**
- Potion: 1:30 (1800 ticks)
- Arrow: 22 seconds (440 ticks)
- Custom: Any duration via `MobEffectInstance`

**For v1.1:** 6 seconds = 120 ticks

**Application:**
```java
new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1) // Speed II for 6s
```

**Immune Entities:**
- Wither
- Ender Dragon
- Flying mobs (partially - flight unaffected)
- Shulkers (don't move normally)

### Glowing

**Effect Details:**
- ID: `minecraft:glowing`
- Visual: White outline visible through blocks
- Team color: Outline matches team color if entity has team
- Amplifier: Irrelevant (only level 0 matters)

**Duration:**
- Spectral arrow: 10 seconds (200 ticks)
- Bell on illagers: 3 seconds (60 ticks)
- Custom: Any duration

**For v1.1:** 6 seconds = 120 ticks

**Application:**
```java
new MobEffectInstance(MobEffects.GLOWING, 120, 0) // Glowing for 6s
```

**Immune Entities:**
- Wither
- Ender Dragon
- Dropped items
- Display entities
- Interactions

**Visual Behavior:**
- Outline merges with nearby glowing entities
- Visible only through solid blocks (not void)
- Shows through entity models with holes

### Combined Application

```java
// Apply both effects:
if (target instanceof LivingEntity living) {
    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1)); // Speed II
    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));        // Glowing
}
```

**Effect Stacking:**
- Multiple applications refresh duration
- Same effect from different sources: highest amplifier wins
- Speed from beacon + potion: beacon level takes priority while in range

## Implementation Summary

| Feature | Modification Point | Difficulty |
|---------|-------------------|------------|
| Drowning 4x slower | Mixin `decreaseAirSupply` | LOW |
| Remove spear crafting | Extend RecipeManagerMixin | LOW |
| Remove spear loot | Datapack override OR LootTableEvents | MEDIUM |
| Remove spear mob equipment | Mixin mob `initialize` | MEDIUM |
| Projectile velocity boost | Mixin projectile `tick` | MEDIUM |
| Projectile gravity curve | Mixin projectile `tick` | MEDIUM |
| Aggro redirection | `setTarget()` in hit handler | LOW |
| Speed II + Glowing | `addEffect()` in hit handler | LOW |

## Sources

### Primary (HIGH confidence)
- [Minecraft Wiki - Damage](https://minecraft.wiki/w/Damage) - Drowning mechanics
- [Minecraft Wiki - Projectile](https://minecraft.wiki/w/Projectile) - Physics values
- [Minecraft Wiki - Speed](https://minecraft.wiki/w/Speed) - Effect mechanics
- [Minecraft Wiki - Glowing](https://minecraft.wiki/w/Glowing) - Effect mechanics
- [Minecraft Wiki - Spear](https://minecraft.wiki/w/Spear) - All spear stats
- [Minecraft Wiki - Java Edition 1.21.11](https://minecraft.wiki/w/Java_Edition_1.21.11) - Mounts of Mayhem changes
- [Fabric API LootTableEvents](https://maven.fabricmc.net/docs/fabric-api-0.129.0+1.21.7/net/fabricmc/fabric/api/loot/v3/LootTableEvents.html)
- [Yarn API - MobEntity](https://maven.fabricmc.net/docs/yarn-20w51a+build.9/net/minecraft/entity/mob/MobEntity.html)
- [Yarn API - Entity](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html)
- [Yarn API - LivingEntity](https://maven.fabricmc.net/docs/yarn-1.21+build.9/net/minecraft/entity/LivingEntity.html)

### Secondary (MEDIUM confidence)
- [Fabric Wiki - Adding to Loot Tables](https://wiki.fabricmc.net/tutorial:adding_to_loot_tables)
- [Fabric Wiki - Mixin Examples](https://fabricmc.net/wiki/tutorial:mixin_examples)
- [Minecraft Wiki - Calculators/Projectile motion](https://minecraft.wiki/w/Calculators/Projectile_motion)

### Tertiary (LOW confidence)
- Community forum discussions on mixin patterns
- Blog posts on projectile physics calculations

## Open Questions

1. **Exact spear spawn rates:** What percentage of zombies/piglins spawn with spears vs swords? May need to examine vanilla spawn code or test empirically.

2. **Projectile distance tracking:** What's the cleanest way to track distance traveled? Options: spawn position attachment, cumulative velocity sum, tick counter with assumed average velocity.

3. **Loot table modification vs replacement:** Should we use `LootTableEvents.MODIFY` with filtering (cleaner) or JSON override (simpler but conflicts with other mods)?

---
*Research conducted: 2026-01-18*
*Valid until: ~30 days (stable vanilla mechanics)*
