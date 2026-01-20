# Stack Research: THC v1.1 Features

**Researched:** 2026-01-18
**Minecraft Version:** 1.21.11 (Mounts of Mayhem)
**Fabric API:** 0.141.0+1.21.11
**Domain:** Combat modifications, damage systems, projectile mechanics

## Drowning Damage Modification

### Target: LivingEntity.baseTick()

The drowning damage system works as follows:
- Air supply decreases each tick when underwater (from max 300 to 0, then -20)
- Damage triggers when air supply reaches -20 (every 20 ticks = 1 second)
- After damage, air supply resets to 0 and cycle repeats

**Mixin approach:**

```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityDrowningMixin {

    // Option 1: Modify air consumption rate
    @Inject(
        method = "baseTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAirSupply()I")
    )
    private void thc$modifyDrowningRate(CallbackInfo ci) {
        // Inject before air supply check to modify rate
    }

    // Option 2: Override getNextAirUnderwater to control depletion
    @Inject(method = "getNextAirUnderwater", at = @At("RETURN"), cancellable = true)
    private void thc$slowerAirDepletion(int air, CallbackInfoReturnable<Integer> cir) {
        // Example: Only deplete every other tick (halves drowning damage rate)
        if (((LivingEntity)(Object)this).tickCount % 2 == 0) {
            cir.setReturnValue(air); // Don't deplete this tick
        }
    }
}
```

**Method signatures (Yarn mappings):**
- `protected int getNextAirUnderwater(int air)` - Called each tick underwater
- `protected int getNextAirOnLand(int air)` - Called each tick on land (regeneration)
- `public void baseTick()` - Main tick loop where air management happens

**Recommended approach:** Override `getNextAirUnderwater` with `@Inject` at `RETURN` to conditionally skip air depletion ticks. This halves the effective drowning damage rate without modifying damage amounts.

**Confidence:** MEDIUM - Method signatures verified via Yarn 1.21 docs, but exact injection points need in-game testing.

---

## Spear Removal

### Item IDs

**Confirmed spear item IDs (minecraft namespace):**
- `minecraft:wooden_spear`
- `minecraft:stone_spear`
- `minecraft:copper_spear`
- `minecraft:iron_spear`
- `minecraft:golden_spear`
- `minecraft:diamond_spear`
- `minecraft:netherite_spear`

**Item tag:** `#minecraft:spears` (contains all spear variants)

**Confidence:** HIGH - Confirmed via Minecraft Wiki, follows standard naming convention.

### Crafting Removal

**Existing pattern in project:** `RecipeManagerMixin.java` demonstrates recipe removal.

```java
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(
        method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void thc$removeSpearRecipes(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<RecipeMap> cir) {
        RecipeMap recipes = cir.getReturnValue();
        Set<ResourceKey<Recipe<?>>> spearKeys = Set.of(
            ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("wooden_spear")),
            ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("stone_spear")),
            ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("copper_spear")),
            ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_spear")),
            ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("golden_spear")),
            ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("diamond_spear"))
            // Note: netherite_spear uses smithing table upgrade, different recipe type
        );

        Collection<RecipeHolder<?>> values = recipes.values();
        List<RecipeHolder<?>> filtered = new ArrayList<>(values.size());
        for (RecipeHolder<?> holder : values) {
            if (!spearKeys.contains(holder.id())) {
                filtered.add(holder);
            }
        }
        if (filtered.size() != values.size()) {
            cir.setReturnValue(RecipeMap.create(filtered));
        }
    }
}
```

**Note:** Netherite spear uses smithing table upgrade recipe, not crafting table. May need additional handling for `SmithingTransformRecipe`.

**Confidence:** HIGH - Pattern already working in codebase for shield removal.

### Mob Equipment Removal

Mobs that spawn with spears (as of 1.21.11):
- Zombies, Husks, Zombie Horsemen, Camel Husk Jockeys: iron spears
- Piglins, Zombified Piglins: golden spears

**Equipment spawning is hardcoded** in `Mob.finalizeSpawn()` and `populateDefaultEquipmentSlots()`, NOT determined by loot tables.

**Mixin approach:**

```java
@Mixin(Mob.class)
public abstract class MobEquipmentMixin {

    @Inject(
        method = "finalizeSpawn",
        at = @At("TAIL")
    )
    private void thc$removeSpearEquipment(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason spawnReason,
        @Nullable SpawnGroupData groupData,
        CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        Mob self = (Mob)(Object)this;
        ItemStack mainHand = self.getMainHandItem();
        if (mainHand.is(ItemTags.SPEARS)) { // Use #spears tag
            self.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }
}
```

**Alternative:** Target specific mob classes (ZombieEntity, PiglinEntity) with more surgical mixins if `ItemTags.SPEARS` doesn't exist.

**Confidence:** MEDIUM - `finalizeSpawn` method signature verified, but `ItemTags.SPEARS` existence needs verification. May need to check item directly against spear item list.

### Loot Table Removal

**Spear loot table locations (1.21.11):**

| Location | Spear Type | Loot Table Path |
|----------|------------|-----------------|
| Ocean Ruins | Stone Spear | `minecraft:chests/underwater_ruin_small`, `minecraft:chests/underwater_ruin_big` |
| Buried Treasure | Iron Spear | `minecraft:chests/buried_treasure` |
| Weaponsmith | Copper/Iron Spear | `minecraft:chests/village/village_weaponsmith` |
| End City | Diamond Spear | `minecraft:chests/end_city_treasure` |

**Fabric API approach (LootTableEvents):**

```java
public class THCLootTableModifier implements ModInitializer {
    private static final Set<Identifier> SPEAR_IDS = Set.of(
        Identifier.of("minecraft", "wooden_spear"),
        Identifier.of("minecraft", "stone_spear"),
        Identifier.of("minecraft", "copper_spear"),
        Identifier.of("minecraft", "iron_spear"),
        Identifier.of("minecraft", "golden_spear"),
        Identifier.of("minecraft", "diamond_spear"),
        Identifier.of("minecraft", "netherite_spear")
    );

    @Override
    public void onInitialize() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // Only modify vanilla tables, not datapack overrides
            if (source.isBuiltin()) {
                // Unfortunately, MODIFY event doesn't support removal directly
                // Must use REPLACE for removal scenarios
            }
        });

        // For removal, use datapack override approach instead
        // Place empty pool JSON files in: data/minecraft/loot_table/chests/...
    }
}
```

**Recommended approach:** Use data generation to create replacement loot table JSON files that exclude spears. This is simpler and more compatible than runtime modification.

**Data generation pattern:**
```java
public class THCLootTableProvider extends SimpleFabricLootTableProvider {
    // Override vanilla loot tables with versions that exclude spears
}
```

**Confidence:** MEDIUM - LootTableEvents.MODIFY doesn't support removal cleanly. Datapack override approach is more reliable but requires knowing exact vanilla loot table structure.

---

## Projectile Interception

### No Fabric API Event Exists

Fabric API does NOT provide a projectile hit event. Must use mixins.

**Available Fabric events (insufficient):**
- `ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY` - Too late, only after kill
- `ServerLivingEntityEvents.ALLOW_DAMAGE` - Fires for all damage, not projectile-specific

**Mixin target: Projectile.onHit()**

```java
@Mixin(Projectile.class)
public abstract class ProjectileHitMixin {

    @Inject(
        method = "onHit",
        at = @At("HEAD")
    )
    private void thc$onProjectileHit(HitResult hitResult, CallbackInfo ci) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity target = entityHit.getEntity();
            Projectile self = (Projectile)(Object)this;
            Entity shooter = self.getOwner();

            // Custom logic here
        }
    }
}
```

**For entity-specific hit detection:**

```java
@Mixin(AbstractArrow.class)
public abstract class ArrowHitMixin {

    @Inject(
        method = "onHitEntity",
        at = @At("HEAD")
    )
    private void thc$onArrowHitEntity(EntityHitResult result, CallbackInfo ci) {
        Entity target = result.getEntity();
        AbstractArrow self = (AbstractArrow)(Object)this;
        Entity shooter = self.getOwner();

        // Apply effects, change aggro, etc.
    }
}
```

**Key methods:**
- `Projectile.onHit(HitResult)` - General hit handler
- `AbstractArrow.onHitEntity(EntityHitResult)` - Arrow-specific entity hit
- `AbstractArrow.onHitBlock(BlockHitResult)` - Arrow-specific block hit
- `ThrowableProjectile.onHit(HitResult)` - Snowballs, eggs, potions

**Confidence:** HIGH - Method signatures verified via Fabric wiki tutorials and Forge javadocs.

---

## Projectile Physics

### Velocity and Gravity Modification

**Key physics facts:**
- Gravity: 0.05 blocks/tick^2 (arrows)
- Drag: 0.99 multiplier per tick
- Order (1.21.2+): Acceleration, Drag, Position

**Mixin approach for velocity:**

```java
@Mixin(AbstractArrow.class)
public abstract class ArrowPhysicsMixin {

    // Modify initial velocity when shot
    @Inject(
        method = "shootFromRotation",
        at = @At("TAIL")
    )
    private void thc$modifyInitialVelocity(
        Entity shooter, float pitch, float yaw, float roll, float speed, float divergence,
        CallbackInfo ci
    ) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        Vec3 velocity = self.getDeltaMovement();
        // Example: increase velocity by 50%
        self.setDeltaMovement(velocity.scale(1.5));
    }
}
```

**Mixin approach for gravity:**

```java
@Mixin(Projectile.class)
public abstract class ProjectileGravityMixin {

    // Option 1: Disable gravity entirely
    @Inject(method = "tick", at = @At("HEAD"))
    private void thc$disableGravity(CallbackInfo ci) {
        ((Projectile)(Object)this).setNoGravity(true);
    }

    // Option 2: Modify gravity amount (more complex)
    // Target the tick method where deltaMovement.y is modified
}
```

**Method signatures:**
- `public void shootFromRotation(Entity shooter, float pitch, float yaw, float roll, float speed, float divergence)`
- `public void shoot(double x, double y, double z, float speed, float divergence)`
- `public void tick()` - Physics update each tick
- `public void setNoGravity(boolean noGravity)` - Inherited from Entity

**For custom gravity values:** Must inject into `tick()` method after gravity is applied and adjust `deltaMovement.y` accordingly. This is complex because gravity application varies by projectile type.

**Confidence:** MEDIUM - Basic patterns verified, but exact injection points for gravity modification need testing.

---

## Aggro + Effects Application

### Applying Potion Effects

**Standard pattern (used in existing LivingEntityMixin):**

```java
MobEffectInstance speedEffect = new MobEffectInstance(
    MobEffects.MOVEMENT_SPEED,  // Effect type
    20 * 10,                     // Duration in ticks (10 seconds)
    1,                           // Amplifier (0 = level I, 1 = level II)
    false,                       // Ambient (beacon-style)
    true,                        // Visible in inventory
    true                         // Show icon
);
target.addEffect(speedEffect, sourceEntity); // Second param is source for death message
```

**For Speed II and Glowing on projectile hit:**

```java
@Inject(method = "onHitEntity", at = @At("TAIL"))
private void thc$applyAggroEffects(EntityHitResult result, CallbackInfo ci) {
    Entity target = result.getEntity();
    if (!(target instanceof LivingEntity living)) return;

    Projectile self = (Projectile)(Object)this;
    Entity shooter = self.getOwner();

    // Speed II (amplifier 1) for 10 seconds
    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1), shooter);

    // Glowing for 10 seconds
    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), shooter);
}
```

**Confidence:** HIGH - Pattern already used successfully in project's LivingEntityMixin.

### Redirecting Aggro

**MobEntity targeting API:**

```java
// Get current target
LivingEntity currentTarget = mob.getTarget();

// Set new target
mob.setTarget(newTarget);

// Check if mob can target something
boolean canTarget = mob.canTarget(potentialTarget);
```

**Mixin for aggro redirection on projectile hit:**

```java
@Inject(method = "onHitEntity", at = @At("TAIL"))
private void thc$redirectAggro(EntityHitResult result, CallbackInfo ci) {
    Entity target = result.getEntity();
    if (!(target instanceof Mob mob)) return;

    Projectile self = (Projectile)(Object)this;
    Entity shooter = self.getOwner();

    if (shooter instanceof LivingEntity livingShooter) {
        // Make the mob target the shooter
        mob.setTarget(livingShooter);
    }
}
```

**Important caveats:**
1. `setTarget()` is immediate but mob AI may override it next tick
2. For persistent aggro, may need to also add to mob's anger/revenge system
3. Some mobs (neutral mobs) use different targeting mechanics

**For persistent aggro on neutral mobs:** May need to set NBT anger data (`AngryAt` tag) or use mob-specific APIs.

**Confidence:** HIGH - Method signatures verified via Fabric Yarn docs.

---

## What NOT To Do

### Don't Use LootTableEvents.MODIFY for Removal

The MODIFY event is designed for adding to loot tables, not removing. The API doesn't expose pool entries for removal. Use datapack overrides instead.

### Don't Target Entity.damage() for Projectile Detection

The `Entity.damage()` method is overridden inconsistently across entity types. Some don't call super. Use projectile-specific methods instead.

### Don't Assume ItemTags.SPEARS Exists

This tag may not be exposed in the Items API. Check at runtime or hardcode the item list.

### Don't Modify deltaMovement Directly in TAIL of tick()

By TAIL of tick(), the position has already been updated. Modify velocity at HEAD or use specific injection points.

---

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Drowning damage | MEDIUM | Method signatures verified, injection points need testing |
| Spear item IDs | HIGH | Confirmed via official wiki and standard naming |
| Recipe removal | HIGH | Working pattern exists in codebase |
| Mob equipment removal | MEDIUM | finalizeSpawn exists but ItemTags.SPEARS uncertain |
| Loot table removal | MEDIUM | Fabric API limited; datapack approach recommended |
| Projectile hit | HIGH | Method signatures verified via multiple sources |
| Projectile physics | MEDIUM | Basic API verified, gravity modification complex |
| Effects application | HIGH | Working pattern in codebase |
| Aggro redirection | HIGH | MobEntity API well-documented |

---

## Sources

### Primary (HIGH confidence)
- [Yarn 1.21 LivingEntity API](https://maven.fabricmc.net/docs/yarn-1.21+build.9/net/minecraft/entity/LivingEntity.html)
- [Fabric Wiki - Projectiles Tutorial](https://wiki.fabricmc.net/tutorial:projectiles)
- [Fabric API LootTableEvents](https://maven.fabricmc.net/docs/fabric-api-0.129.0+1.21.7/net/fabricmc/fabric/api/loot/v3/LootTableEvents.html)
- [Minecraft Wiki - Spear](https://minecraft.wiki/w/Spear)
- [Minecraft Wiki - Java Edition 1.21.11](https://minecraft.wiki/w/Java_Edition_1.21.11)

### Secondary (MEDIUM confidence)
- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events)
- [Forge JavaDocs - Projectile](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/projectile/Projectile.html)
- [Minecraft Wiki - Geared Mobs](https://minecraft.wiki/w/Geared_mobs)
- [Minecraft Wiki - Damage](https://minecraft.wiki/w/Damage)

### Tertiary (LOW confidence)
- WebSearch results for mob equipment spawning patterns
- Community modding tutorials (moddingtutorials.org)

---

## Metadata

**Research date:** 2026-01-18
**Valid until:** 2026-02-18 (30 days - stable domain)
**Project mappings:** Mojang (based on existing mixin imports)
