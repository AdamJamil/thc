# Stack Research: v2.3 Monster Overhaul and Regional Spawn System

**Researched:** 2026-01-23
**Minecraft Version:** 1.21.11
**Mappings:** Mojang Official
**Fabric API:** 0.141.0+1.21.11

## Summary

The v2.3 monster overhaul requires three categories of modifications:
1. **Global mob attribute changes** (speed increase) - Use `ServerEntityEvents.ENTITY_LOAD` event
2. **Spawn table modifications** (Zombie->Husk, Skeleton->Stray) - Use Fabric BiomeModifications API
3. **Entity-specific behavior mixins** (Ghast, Enderman, Vex, Phantom, Iron Golem) - Direct mixins to entity classes

**Primary recommendation:** Combine Fabric API events for entity load-time modifications with targeted mixins for behavior changes. Avoid BiomeModifications for spawn replacement (use NaturalSpawner mixin instead for finer control).

---

## 1. Mob Movement Speed Modification

### Recommended Approach: ServerEntityEvents.ENTITY_LOAD

Use Fabric API's `ServerEntityEvents.ENTITY_LOAD` to apply speed modifiers when mobs spawn or load from disk.

**Why this approach:**
- Catches all mob spawns (natural, spawner, spawn egg, commands)
- Single registration point, no per-entity-type mixins needed
- Already present in THC codebase patterns (threat system uses similar per-entity logic)
- Transient modifiers don't persist to disk (no save file bloat)

**Implementation Pattern:**

```java
// In THC mod initializer
ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
    if (!(entity instanceof Mob mob)) return;
    if (mob.getType().getCategory() != MobCategory.MONSTER) return;

    // Exclude creepers and baby zombies
    if (mob instanceof Creeper) return;
    if (mob instanceof Zombie zombie && zombie.isBaby()) return;

    AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speed == null) return;

    // Add 20% speed increase via transient modifier
    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thc", "monster_speed");
    AttributeModifier modifier = new AttributeModifier(
        id,
        0.2,  // 20% increase
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
    );
    speed.addOrUpdateTransientModifier(modifier);
});
```

**Key Classes (Mojang mappings 1.21.11):**

| Class | Package | Purpose |
|-------|---------|---------|
| `Attributes` | `net.minecraft.world.entity.ai.attributes` | Registry of attribute types |
| `Attributes.MOVEMENT_SPEED` | | Generic movement speed holder |
| `AttributeInstance` | `net.minecraft.world.entity.ai.attributes` | Per-entity attribute state |
| `AttributeModifier` | `net.minecraft.world.entity.ai.attributes` | Modifier with operation type |
| `AttributeModifier.Operation` | | `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL` |

**Confidence:** HIGH - Pattern verified in existing THC `ServerPlayerMixin.java` (uses `getAttribute(Attributes.MAX_HEALTH)`)

### Alternative: Per-Entity-Type Mixin (NOT RECOMMENDED)

Targeting each mob's `createAttributes()` static method would be cleaner but requires one mixin per mob type. Not scalable for "all monsters" requirement.

---

## 2. Spawn Table Replacement (Zombie->Husk, Skeleton->Stray)

### Recommended Approach: NaturalSpawner Mixin with Entity Substitution

**Why NOT BiomeModifications API:**
- BiomeModifications.removeSpawns() + addSpawn() works but affects ALL biomes
- Husks and Strays have temperature-specific behaviors that need preservation
- THC already has NaturalSpawner mixin infrastructure

**Strategy:** Intercept entity creation during natural spawn, substitute entity type.

**Mixin Target:**

```java
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMobReplacementMixin {

    /**
     * Redirect getRandomSpawnMobAt to substitute entity types.
     *
     * Method signature (Mojang 1.21.11):
     * static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(
     *     ServerLevel level,
     *     StructureManager structureManager,
     *     ChunkGenerator generator,
     *     MobCategory category,
     *     RandomSource random,
     *     BlockPos pos
     * )
     */
    @Inject(
        method = "getRandomSpawnMobAt",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void thc$substituteMonsterTypes(
        ServerLevel level,
        StructureManager structureManager,
        ChunkGenerator generator,
        MobCategory category,
        RandomSource random,
        BlockPos pos,
        CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> cir
    ) {
        Optional<MobSpawnSettings.SpawnerData> result = cir.getReturnValue();
        if (result.isEmpty()) return;

        MobSpawnSettings.SpawnerData data = result.get();
        EntityType<?> replacement = null;

        if (data.type() == EntityType.ZOMBIE) {
            replacement = EntityType.HUSK;
        } else if (data.type() == EntityType.SKELETON) {
            replacement = EntityType.STRAY;
        }

        if (replacement != null) {
            // Create new SpawnerData with substituted type
            cir.setReturnValue(Optional.of(new MobSpawnSettings.SpawnerData(
                replacement,
                data.getWeight(),
                data.minCount,
                data.maxCount
            )));
        }
    }
}
```

**Key Classes:**

| Class | Package | Purpose |
|-------|---------|---------|
| `NaturalSpawner` | `net.minecraft.world.level` | Handles natural mob spawning |
| `MobSpawnSettings.SpawnerData` | `net.minecraft.world.level.biome` | Spawn entry (type, weight, count) |
| `EntityType` | `net.minecraft.world.entity` | Entity type registry |

**Confidence:** HIGH - NaturalSpawner mixin pattern verified in existing THC codebase

### Alternative: BiomeModifications API

For reference, the Fabric API approach:

```java
// In mod initializer - NOT RECOMMENDED for this use case
BiomeModifications.create(ResourceLocation.fromNamespaceAndPath("thc", "spawn_replacement"))
    .add(ModificationPhase.REPLACEMENTS,
         BiomeSelectors.foundInOverworld(),
         (selectionContext, modificationContext) -> {
             SpawnSettingsContext spawns = modificationContext.getSpawnSettings();
             spawns.removeSpawnsOfEntityType(EntityType.ZOMBIE);
             spawns.addSpawn(MobCategory.MONSTER,
                 new MobSpawnSettings.SpawnerData(EntityType.HUSK, 95, 2, 4));
         });
```

**Why this is problematic:** BiomeModifications runs at world load, permanently alters biome data. The NaturalSpawner approach is per-spawn-attempt and allows conditional logic (stage-gating, region-based).

---

## 3. Ghast Projectile Modification

### Target: Ghast.GhastShootFireballGoal Inner Class

Ghasts use a custom AI goal (not RangedAttackMob interface) to shoot fireballs.

**Mixin Targets:**

```java
@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
public abstract class GhastFireballMixin {

    @Shadow @Final private Ghast ghast;

    /**
     * Modify fireball speed by redirecting LargeFireball constructor.
     *
     * LargeFireball constructor (Mojang 1.21.11):
     * LargeFireball(Level level, LivingEntity shooter, Vec3 movement, int explosionPower)
     */
    @Redirect(
        method = "tick",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/world/entity/projectile/LargeFireball;"
        )
    )
    private LargeFireball thc$fasterFireball(
        Level level,
        LivingEntity shooter,
        Vec3 movement,
        int explosionPower
    ) {
        // 50% faster projectile
        Vec3 boostedMovement = movement.scale(1.5);
        return new LargeFireball(level, shooter, boostedMovement, explosionPower);
    }

    /**
     * Reduce attack cooldown for faster fire rate.
     * The chargeTime field controls time between shots.
     */
    @Shadow private int chargeTime;

    @Inject(method = "tick", at = @At("HEAD"))
    private void thc$reduceCooldown(CallbackInfo ci) {
        // Vanilla charges to 20 ticks, fire at chargeTime >= 20
        // We want 25% less time, so speed up charging
        // This will be called every tick, so add extra charge progress
        if (this.chargeTime > 0 && this.chargeTime < 20) {
            // Add 33% extra charge per tick (1.33 * 15 ticks = 20)
            // Effectively reduces time from 20 ticks to ~15 ticks (25% reduction)
            this.chargeTime += (int)(this.chargeTime * 0.05); // Incremental boost
        }
    }
}
```

**Fire Spread Modification:** Requires `LargeFireball` mixin to increase explosion fire spread.

```java
@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin {

    /**
     * Increase fire spread by modifying onHitBlock behavior.
     *
     * LargeFireball calls Explosion.interactsWithBlocks() which handles fire.
     * The explosionPower field (set via constructor) affects radius.
     * Fire spread is handled in onHit -> explode -> doExplosionB
     */
    @Inject(method = "onHit", at = @At("TAIL"))
    private void thc$spreadMoreFire(HitResult result, CallbackInfo ci) {
        LargeFireball self = (LargeFireball)(Object)this;
        Level level = self.level();
        if (level.isClientSide) return;

        BlockPos center = BlockPos.containing(self.position());
        // Place additional fire in 5 block radius (double vanilla ~2-3)
        for (int i = 0; i < 12; i++) {
            BlockPos firePos = center.offset(
                level.random.nextInt(11) - 5,
                level.random.nextInt(5) - 2,
                level.random.nextInt(11) - 5
            );
            if (level.getBlockState(firePos).isAir() &&
                level.getBlockState(firePos.below()).isSolidRender(level, firePos.below())) {
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
}
```

**Key Classes:**

| Class | Package | Purpose |
|-------|---------|---------|
| `Ghast` | `net.minecraft.world.entity.monster` | Ghast entity |
| `Ghast$GhastShootFireballGoal` | (inner class) | AI goal for shooting |
| `LargeFireball` | `net.minecraft.world.entity.projectile` | Ghast fireball projectile |

**Confidence:** MEDIUM - Inner class mixin pattern is standard, but exact method names need verification in decompiled source. The `tick()` method in GhastShootFireballGoal handles the charging and firing logic.

---

## 4. Enderman Teleport-Behind Mechanic

### Target: EnderMan.hurtServer() or Custom AI Goal

Enderman teleportation on damage is handled in `LivingEntity.hurtServer()` -> `EnderMan.customServerAiStep()` and `teleportTowards()`.

**Mixin Approach:**

```java
@Mixin(EnderMan.class)
public abstract class EnderManTeleportMixin {

    @Shadow protected abstract boolean teleportTowards(Entity target);
    @Shadow protected abstract boolean teleport();

    /**
     * After taking damage, 50% chance to teleport behind attacker.
     *
     * hurtServer signature (Mojang 1.21.11):
     * boolean hurtServer(ServerLevel level, DamageSource source, float amount)
     */
    @Inject(
        method = "hurtServer",
        at = @At("RETURN")
    )
    private void thc$teleportBehindAttacker(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) return; // Damage wasn't applied

        Entity attacker = source.getEntity();
        if (attacker == null) return;
        if (!(attacker instanceof LivingEntity living)) return;

        EnderMan self = (EnderMan)(Object)this;

        // 50% chance
        if (self.getRandom().nextFloat() > 0.5f) return;

        // Calculate position behind attacker
        Vec3 attackerLook = living.getLookAngle();
        Vec3 behindPos = living.position().subtract(attackerLook.scale(2.0));

        // Attempt teleport to behind position
        self.teleportTo(behindPos.x, behindPos.y, behindPos.z);
    }

    /**
     * Proximity aggro: aggro when player within 3 blocks.
     * Hook into customServerAiStep or use a separate tick injection.
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void thc$proximityAggro(ServerLevel level, CallbackInfo ci) {
        EnderMan self = (EnderMan)(Object)this;
        if (self.getTarget() != null) return; // Already has target

        // Find nearest player within 3 blocks
        Player nearest = level.getNearestPlayer(self, 3.0);
        if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
            self.setTarget(nearest);
        }
    }
}
```

**Key Methods (Mojang 1.21.11):**

| Method | Signature | Purpose |
|--------|-----------|---------|
| `teleportTowards` | `boolean teleportTowards(Entity target)` | Teleport toward a target entity |
| `teleport` | `boolean teleport()` | Random teleport |
| `teleportTo` | `void teleportTo(double x, double y, double z)` | Direct position teleport (inherited from Entity) |
| `customServerAiStep` | `void customServerAiStep(ServerLevel level)` | Per-tick AI hook |

**Confidence:** MEDIUM - Method names based on standard Mojang mapping patterns. Enderman-specific methods need verification.

---

## 5. Equipment Manipulation at Spawn Time

### Recommended Approach: ServerEntityEvents.ENTITY_LOAD

Same event used for speed modification, add equipment logic.

**Implementation:**

```java
ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
    // Vex iron sword removal
    if (entity instanceof Vex vex) {
        vex.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        return;
    }

    // Pillager variant equipment
    if (entity instanceof Pillager pillager) {
        // Use NBT tag to determine variant (see Section 8)
        String origin = pillager.getPersistentData().getString("thc:spawn_origin");

        if ("upper_cave".equals(origin)) {
            // MELEE variant: iron axe
            pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        } else if ("lower_cave".equals(origin)) {
            // RANGED variant: keep crossbow (default)
        }
    }
});
```

**Key Classes:**

| Class | Package | Purpose |
|-------|---------|---------|
| `EquipmentSlot` | `net.minecraft.world.entity` | Equipment slot enum |
| `Mob.setItemSlot` | `net.minecraft.world.entity` | Set equipment |

**Confidence:** HIGH - Equipment manipulation pattern used in multiple existing mods

---

## 6. NaturalSpawner Spawn Attempt Interception for Regional Distribution

### Target: NaturalSpawner.spawnCategoryForPosition

This is where individual spawn attempts happen. Intercept here for region-based logic.

**Mixin:**

```java
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerRegionalMixin {

    /**
     * spawnCategoryForPosition signature (Mojang 1.21.11):
     * static void spawnCategoryForPosition(
     *     MobCategory category,
     *     ServerLevel level,
     *     ChunkAccess chunk,
     *     BlockPos pos,
     *     NaturalSpawner.SpawnPredicate filter,
     *     NaturalSpawner.AfterSpawnCallback callback
     * )
     */
    @Inject(
        method = "spawnCategoryForPosition",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void thc$regionalSpawnDistribution(
        MobCategory category,
        ServerLevel level,
        ChunkAccess chunk,
        BlockPos pos,
        NaturalSpawner.SpawnPredicate filter,
        NaturalSpawner.AfterSpawnCallback callback,
        CallbackInfo ci
    ) {
        if (category != MobCategory.MONSTER) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        // Determine region based on Y level
        SpawnRegion region = SpawnRegion.fromY(pos.getY());

        // Roll for region-specific spawn table
        // This replaces vanilla spawn logic with THC regional logic
        Optional<EntityType<?>> mobType = THCSpawnTables.rollSpawn(level, pos, region);

        if (mobType.isPresent()) {
            // Spawn the mob ourselves and cancel vanilla logic
            Mob mob = (Mob) mobType.get().create(level, EntitySpawnReason.NATURAL);
            if (mob != null) {
                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                           level.random.nextFloat() * 360.0F, 0.0F);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                                  EntitySpawnReason.NATURAL, null);

                // Tag with spawn origin (see Section 8)
                mob.getPersistentData().putString("thc:spawn_origin", region.name().toLowerCase());

                level.addFreshEntityWithPassengers(mob);
                callback.run(mob, chunk);
            }
            ci.cancel();
        }
    }
}
```

**Region Enum:**

```java
public enum SpawnRegion {
    SURFACE(64, 320),      // Y >= 64
    UPPER_CAVE(32, 63),    // Y 32-63
    LOWER_CAVE(-64, 31);   // Y < 32

    private final int minY, maxY;

    public static SpawnRegion fromY(int y) {
        if (y >= 64) return SURFACE;
        if (y >= 32) return UPPER_CAVE;
        return LOWER_CAVE;
    }
}
```

**Confidence:** HIGH - Based on [Curtain mod mixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java) which uses same injection pattern.

---

## 7. Per-Region Monster Cap Enforcement

### Target: NaturalSpawner.SpawnState or Custom Logic

Monster caps are calculated in `NaturalSpawner.createState()` and checked in `spawnForChunk()`.

**Approach:** Modify `SpawnState.canSpawnForCategory()` or track caps separately.

**Mixin:**

```java
@Mixin(NaturalSpawner.SpawnState.class)
public abstract class SpawnStateCapMixin {

    /**
     * canSpawnForCategory signature (Mojang 1.21.11):
     * boolean canSpawnForCategory(MobCategory category, ChunkPos pos)
     */
    @Inject(
        method = "canSpawnForCategory",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$partitionedCapCheck(
        MobCategory category,
        ChunkPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (category != MobCategory.MONSTER) return;

        // Get current region based on chunk center Y (approximate)
        // This is imprecise - better to track per-spawn in regional spawn logic
        int spawnCount = THCSpawnTracker.getRegionalCount(SpawnRegion.SURFACE);
        int cap = THCSpawnTracker.getRegionalCap(SpawnRegion.SURFACE);

        if (spawnCount >= cap) {
            cir.setReturnValue(false);
        }
    }
}
```

**Better Approach:** Track regional mob counts using a separate system that counts existing mobs by Y level, rather than intercepting vanilla cap logic. This avoids fighting with vanilla's chunk-based cap system.

**Confidence:** LOW - Cap partitioning is complex. May need custom spawn tracking rather than modifying vanilla SpawnState.

---

## 8. NBT Data Attachment on Entity Spawn

### Recommended Approach: Persistent Data Container

Minecraft entities have a `persistentData` CompoundTag for mod data that persists across save/load.

**Implementation:**

```java
// In spawn interception code
Mob mob = (Mob) entityType.create(level, EntitySpawnReason.NATURAL);
mob.getPersistentData().putString("thc:spawn_origin", "surface");
mob.getPersistentData().putLong("thc:spawn_time", level.getGameTime());
```

**Reading Later:**

```java
String origin = mob.getPersistentData().getString("thc:spawn_origin");
```

**Alternative:** Use Fabric Attachments API for cleaner typed access, but NBT persistent data is simpler for string tags.

**Confidence:** HIGH - Standard Minecraft pattern, used extensively in mods

---

## 9. Iron Golem Spawning Prevention

### Target: Villager.trySpawnGolem or VillagerGolemSensor

Iron golems are spawned by villagers through brain sensors and behaviors.

**Mixin Approach - Villager summonGolem:**

```java
@Mixin(Villager.class)
public abstract class VillagerGolemPreventionMixin {

    /**
     * trySpawnGolem or summonGolem (name varies by mapping):
     * Called when villagers attempt to spawn an iron golem.
     */
    @Inject(
        method = "spawnGolem",  // Verify exact method name
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$preventGolemSpawn(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
```

**Alternative - Target the sensor:**

```java
@Mixin(targets = "net.minecraft.world.entity.ai.sensing.VillagerGolemSensor")
public abstract class GolemSensorMixin {

    /**
     * Prevent the sensor from detecting golem spawn conditions.
     */
    @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
    private void thc$disableGolemSensor(ServerLevel level, Villager villager, CallbackInfo ci) {
        ci.cancel();
    }
}
```

**Key Classes:**

| Class | Package | Purpose |
|-------|---------|---------|
| `Villager` | `net.minecraft.world.entity.npc` | Villager entity |
| `IronGolem` | `net.minecraft.world.entity.animal` | Iron golem entity |
| `VillagerGolemSensor` | `net.minecraft.world.entity.ai.sensing` | Detects golem spawn conditions |

**Existing Mod Reference:** [No Iron Golems Spawn](https://modrinth.com/mod/no-iron-golems-spawn) mod does this for Fabric 1.21.

**Confidence:** MEDIUM - Method name needs verification. The brain/sensor system in 1.21 may have different structure than earlier versions.

---

## 10. Phantom Natural Spawn Removal

### Target: PhantomSpawner or doInsomnia gamerule

Phantoms spawn via `PhantomSpawner`, not through `NaturalSpawner`.

**Cleanest Approach - Disable PhantomSpawner:**

```java
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {

    /**
     * tick signature (Mojang 1.21.11):
     * int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies)
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$disablePhantomSpawns(
        ServerLevel level,
        boolean spawnEnemies,
        boolean spawnFriendlies,
        CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(0);
    }
}
```

**Alternative - Target spawn predicate:**

```java
@Mixin(Phantom.class)
public abstract class PhantomSpawnMixin {

    /**
     * checkPhantomSpawnRules is the spawn predicate.
     */
    @Inject(
        method = "checkPhantomSpawnRules",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void thc$blockPhantomSpawn(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
```

**Key Classes:**

| Class | Package | Purpose |
|-------|---------|---------|
| `PhantomSpawner` | `net.minecraft.world.level.levelgen` | Custom spawner for phantoms |
| `Phantom` | `net.minecraft.world.entity.monster` | Phantom entity |

**Confidence:** HIGH - PhantomSpawner approach is straightforward and used by existing mods.

---

## 11. Illager Patrol Stage-Gating

### Target: PatrolSpawner

Illager patrols use `PatrolSpawner`, similar to phantom spawning.

**Mixin:**

```java
@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$gatePatrolsToStage2(
        ServerLevel level,
        boolean spawnEnemies,
        boolean spawnFriendlies,
        CallbackInfoReturnable<Integer> cir
    ) {
        // Check THC stage system
        int currentStage = THCStageManager.getStage(level.getServer());
        if (currentStage < 2) {
            cir.setReturnValue(0);
        }
    }
}
```

**Key Classes:**

| Class | Package | Purpose |
|-------|---------|---------|
| `PatrolSpawner` | `net.minecraft.world.level.levelgen` | Custom spawner for patrols |

**Confidence:** HIGH - Same pattern as PhantomSpawner

---

## Integration with Existing THC Systems

### Existing NaturalSpawnerMixin

Current mixin blocks spawns in base chunks. New regional logic should respect this:

```java
// In regional spawn logic
if (ClaimManager.INSTANCE.isClaimed(level.getServer(), new ChunkPos(pos))) {
    ci.cancel();
    return;
}
// Then proceed with regional spawn logic
```

### Stage System Integration

For patrol gating and future stage-dependent spawns:

```java
// Access from THCStageManager (existing)
int stage = THCStageManager.getStage(server);
```

### Threat System Integration

New mobs spawned via regional system should still participate in threat:

```java
// Threat is added via MobDamageThreatMixin on damage events
// No additional integration needed for spawning
```

---

## What NOT to Do

### DO NOT: Use BiomeModifications for Spawn Replacement

**Why:** BiomeModifications permanently alters biome data at world load. THC needs per-spawn conditional logic for:
- Stage gating
- Regional distribution
- Future dynamic adjustments

### DO NOT: Override Entity createAttributes()

**Why:** Requires one mixin per entity type. ServerEntityEvents.ENTITY_LOAD is cleaner.

### DO NOT: Modify NaturalSpawner.SpawnState for Caps

**Why:** Vanilla cap system is chunk-based, not region-based. Fighting it causes edge cases. Implement separate regional cap tracking.

### DO NOT: Use @Overwrite for Inner Classes

**Why:** Inner class mixins are fragile. Use @Inject/@Redirect with careful target descriptors.

---

## Verification Checklist

Before implementation, verify in decompiled 1.21.11 source:

- [ ] `Ghast$GhastShootFireballGoal.tick()` method structure
- [ ] `EnderMan.teleportTowards()` exact signature
- [ ] `Villager.spawnGolem()` or equivalent method name
- [ ] `PhantomSpawner.tick()` parameters
- [ ] `NaturalSpawner.getRandomSpawnMobAt()` return type handling
- [ ] `MobSpawnSettings.SpawnerData` constructor parameters (record vs class)

---

## Confidence Assessment

| Feature | Confidence | Reason |
|---------|------------|--------|
| Mob speed via ENTITY_LOAD | HIGH | Existing pattern in THC |
| Spawn replacement via NaturalSpawner | HIGH | Existing mixin infrastructure |
| Ghast fireball modification | MEDIUM | Inner class mixin, needs verification |
| Enderman teleport-behind | MEDIUM | Method signatures need verification |
| Equipment at spawn | HIGH | Standard pattern |
| Regional spawn distribution | HIGH | Based on Curtain mod pattern |
| Regional cap enforcement | LOW | Complex, may need custom tracking |
| NBT tagging | HIGH | Standard Minecraft pattern |
| Iron golem prevention | MEDIUM | Method name needs verification |
| Phantom removal | HIGH | Straightforward spawner intercept |
| Patrol stage-gating | HIGH | Same pattern as phantom |

---

## Sources

### Primary (HIGH confidence)
- [Fabric API ServerEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.141.0+1.21.11/net/fabricmc/fabric/api/event/lifecycle/v1/ServerEntityEvents.html) - Entity load event
- [Fabric API BiomeModificationContext](https://maven.fabricmc.net/docs/fabric-api-0.141.0+1.21.11/net/fabricmc/fabric/api/biome/v1/BiomeModificationContext.SpawnSettingsContext.html) - Spawn settings API
- [NeoForge Attributes Documentation](https://docs.neoforged.net/docs/entities/attributes/) - Attribute modifier system
- [Fabric Entity Attributes Documentation](https://docs.fabricmc.net/develop/entities/attributes) - Fabric-specific attribute patterns
- [Curtain Mod NaturalSpawnerMixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java) - Spawn interception pattern
- Existing THC codebase (`ServerPlayerMixin.java`, `NaturalSpawnerMixin.java`)

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Attribute](https://minecraft.wiki/w/Attribute) - Attribute mechanics
- [Minecraft Wiki - Iron Golem](https://minecraft.wiki/w/Iron_Golem) - Golem spawning mechanics
- [Technical Minecraft Wiki - Iron Golem Spawning](https://techmcdocs.github.io/pages/GameMechanics/IronGolemSpawningMechanics/) - Detailed spawn mechanics
- [Minecraft Wiki - Phantom](https://minecraft.wiki/w/Phantom) - Phantom spawn conditions
- [No Iron Golems Spawn Mod](https://modrinth.com/mod/no-iron-golems-spawn) - Reference implementation

### Tertiary (LOW confidence)
- [Ghast.GhastShootFireballGoal JavaDoc](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.19.3/net/minecraft/world/entity/monster/Ghast.GhastShootFireballGoal.html) - Outdated version, structure reference only
- WebSearch results for Enderman teleport methods - General patterns only

---

## Metadata

**Research date:** 2026-01-23
**Valid until:** 2026-02-23 (30 days - active development)
**Requires validation:**
- Method signatures against decompiled 1.21.11 source
- Inner class mixin compatibility
- Regional cap tracking approach
