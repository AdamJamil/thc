# Stack Research: v2.0 Twilight System

**Researched:** 2026-01-20
**Minecraft Version:** 1.21.11
**Mappings:** Mojang Official (not Yarn)
**Fabric API:** 0.141.0+1.21.11

## Summary

The twilight visual system requires decoupling client-side sky rendering from server time. Minecraft separates visual time (sky angle, brightness) from gameplay time (mob schedules, spawning checks). The recommended approach: mixin client-side `Level` methods for visuals, mixin server-side `Mob`/`Monster` methods for mechanics.

**Primary recommendation:** Use `@Inject` with `cancellable=true` at `RETURN` to modify time-related return values. Avoid `@Redirect` for compatibility with shaders (Iris/Optifine).

---

## Client Rendering Hooks

Methods that control visual time-of-day. These are CLIENT-SIDE ONLY - modifying them does not affect server mechanics.

### Primary Targets (Level class hierarchy)

| Method | Class | Signature | Purpose |
|--------|-------|-----------|---------|
| `getSunAngle` | `Level` | `float getSunAngle(float partialTick)` | Controls sun/moon position in sky |
| `getTimeOfDay` | `Level` (via `LevelTimeAccess`) | `float getTimeOfDay(float partialTick)` | Normalized time (0-1) for rendering |
| `getSkyDarken` | `Level` | `int getSkyDarken()` | Sky darkness level (0-15) |
| `updateSkyBrightness` | `Level` | `void updateSkyBrightness()` | Recalculates ambient light |

### Client-Specific Methods (ClientLevel)

| Method | Signature | Purpose |
|--------|-----------|---------|
| `getSkyColor` | `Vec3 getSkyColor(Vec3 cameraPos, float partialTick)` | Sky color calculation |
| `getCloudColor` | `Vec3 getCloudColor(float partialTick)` | Cloud tint |
| `getStarBrightness` | `float getStarBrightness(float partialTick)` | Star visibility |
| `setDayTime` | `void setDayTime(long time)` | Client-side time setter (called from server packets) |
| `setGameTime` | `void setGameTime(long time)` | Game tick time |

### Twilight Lock Strategy

To lock visuals to dusk (twilight), override in `ClientLevel`:

```java
@Mixin(ClientLevel.class)
public abstract class ClientLevelTwilightMixin extends Level {

    // Dusk is approximately tick 12000-13000 (sun setting)
    private static final long TWILIGHT_TIME = 12500L;

    @Inject(method = "getSunAngle", at = @At("RETURN"), cancellable = true)
    private void thc$lockSunAngle(float partialTick, CallbackInfoReturnable<Float> cir) {
        // Calculate angle as if time is always TWILIGHT_TIME
        float timeOfDay = (TWILIGHT_TIME % 24000L) / 24000.0F;
        float angle = timeOfDay + 0.25F; // Offset for noon alignment
        if (angle > 1.0F) angle -= 1.0F;
        cir.setReturnValue(angle * (float)(Math.PI * 2));
    }
}
```

**Confidence:** MEDIUM - Pattern verified from [Evernight mod description](https://www.curseforge.com/minecraft/mc-mods/evernight), method signatures from [NeoForge 1.21 JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/).

### Alternative: DimensionRenderingRegistry (Fabric API)

Fabric API provides `DimensionRenderingRegistry` for custom sky rendering:

```java
// In client entrypoint
DimensionRenderingRegistry.registerSkyRenderer(
    Level.OVERWORLD,  // RegistryKey<World>
    (worldRenderContext) -> {
        // Custom sky rendering code
        // Return true to prevent vanilla sky, false to continue
    }
);
```

**Methods:**
- `registerSkyRenderer(RegistryKey<World> key, SkyRenderer renderer)` - Override entire sky
- `getSkyRenderer(RegistryKey<World> key)` - Retrieve registered renderer

**Warning:** As of Fabric API 1.21.9+, world rendering events have been removed. Use direct mixins instead for maximum compatibility.

**Confidence:** HIGH - From [Fabric API JavaDocs](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/client/rendering/v1/DimensionRenderingRegistry.html)

---

## Server Mechanic Hooks

Methods that control gameplay mechanics. These are SERVER-SIDE - they affect actual game logic regardless of visuals.

### Undead Sun Burning

| Method | Class | Signature | Purpose |
|--------|-------|-----------|---------|
| `isSunBurnTick` | `Mob` | `protected boolean isSunBurnTick()` | Checks if mob should burn this tick |

**Implementation to disable sun burning:**

```java
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void thc$disableSunBurn(CallbackInfoReturnable<Boolean> cir) {
        // Check if this is an undead mob type we want to protect
        Mob self = (Mob)(Object)this;
        if (self.getType().is(EntityTypeTags.UNDEAD)) {
            cir.setReturnValue(false);
        }
    }
}
```

**Note:** `isSunBurnTick()` checks:
1. Is it daytime (sky light)
2. Is mob exposed to sky
3. Is mob wearing a helmet
4. Random chance per tick

**Confidence:** HIGH - Method signature confirmed in [Forge 1.18.2 JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/Mob.html), behavior documented on [Minecraft Wiki](https://minecraft.wiki/w/Light)

### Hostile Spawn Light Checks

The existing `NaturalSpawnerMixin` in THC targets `isValidSpawnPostitionForType`. For sky-light-ignoring spawns:

| Method | Class | Signature | Purpose |
|--------|-------|-----------|---------|
| `isValidSpawnPostitionForType` | `NaturalSpawner` | `static boolean isValidSpawnPostitionForType(ServerLevel, MobCategory, StructureManager, ChunkGenerator, MobSpawnSettings.SpawnerData, BlockPos.MutableBlockPos, double)` | Main spawn position validation |
| `checkSpawnRules` | `SpawnPlacements` | Type-specific predicates | Per-mob-type spawn checks |

**Spawn light mechanics (post-1.18):**
- Hostile mobs require block light = 0 AND sky light <= 7
- To allow spawning regardless of sky light, intercept where sky light is checked

**Strategy:** Rather than modifying `NaturalSpawner`, override per-mob-type spawn predicates or modify the light level check result.

```java
// In NaturalSpawner or spawn placement checks:
// Sky light check: level.getBrightness(LightLayer.SKY, pos)
// Block light check: level.getBrightness(LightLayer.BLOCK, pos)
```

**Confidence:** MEDIUM - General mechanics from [Minecraft Wiki Light](https://minecraft.wiki/w/Light), specific method signatures need verification in decompiled source.

### Bee Time-of-Day Behavior

| Method | Class | Signature | Purpose |
|--------|-------|-----------|---------|
| `wantsToEnterHive` | `Bee` | `boolean wantsToEnterHive()` | Returns true at night/rain |
| Inner goals | `Bee.BeeGoToKnownFlowerGoal`, `Bee.BeeEnterHiveGoal`, etc. | Various | Behavioral AI |

**Bee behavior timing:**
- Bees work during daytime only (leave hive when `isDay()`)
- Return to hive at night or during rain
- Inner goal classes control flower-seeking and hive-entering

**Strategy to make bees always work:**

```java
@Mixin(Bee.class)
public abstract class BeeTwilightMixin {

    @Inject(method = "wantsToEnterHive", at = @At("HEAD"), cancellable = true)
    private void thc$beesNeverSleep(CallbackInfoReturnable<Boolean> cir) {
        // Bees never want to enter hive due to time (still enter when full of pollen)
        Bee self = (Bee)(Object)this;
        if (!self.hasNectar()) {
            cir.setReturnValue(false);
        }
    }
}
```

**Confidence:** MEDIUM - Method list from [Forge 1.18.2 Bee JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/animal/Bee.html), behavior from [Minecraft Wiki Bee](https://minecraft.wiki/w/Bee)

---

## Mixin Strategy

### Injection Point Recommendations

| Goal | Injection Type | Why |
|------|---------------|-----|
| Override return value | `@Inject(at = @At("RETURN"), cancellable = true)` | Safe, allows reading original value |
| Completely replace | `@Inject(at = @At("HEAD"), cancellable = true)` | Skip original computation |
| Modify parameter | `@ModifyArg` or `@ModifyVariable` | Targeted modification |
| Conditional logic | `@Inject` with early return | Check condition, optionally cancel |

### Avoid @Redirect and @Overwrite

**Compatibility risk:** Shader mods (Iris, Optifine) and dimension mods also target rendering methods. `@Redirect` fails when multiple mods target the same call site. `@Overwrite` is incompatible by design.

**Instead use:**
- `@Inject` with `cancellable = true` - Multiple mods can inject
- `@ModifyReturnValue` (MixinExtras) - Allows chaining modifications

### Client vs Server Mixin Separation

THC already uses `splitEnvironmentSourceSets()` in `build.gradle`:

| Source Set | Mixin Config | Target Classes |
|------------|--------------|----------------|
| `src/main/java` | `thc.mixins.json` | Server + common (Mob, NaturalSpawner, Bee) |
| `src/client/java` | `thc.client.mixins.json` | Client only (ClientLevel, LevelRenderer) |

**Rule:** Never put client class imports in `src/main`. Classloader will crash on dedicated server.

---

## What NOT to Hook

Methods that would cause cascading issues or break other systems:

### DO NOT Modify

| Method | Class | Why |
|--------|-------|-----|
| `tickTime()` | `ServerLevel` | Breaks all time-dependent mechanics (villager schedules, crop growth, raid timing) |
| `setDayTime()` on server | `ServerLevel` | Syncs to all clients, would freeze actual game time |
| `getDayTime()` broadly | `Level` | Used by spawning, AI, crops - only override in rendering contexts |
| `isDay()` / `isNight()` globally | `Level` | Used by too many systems (phantoms, villagers, cats) |
| `render()` in `LevelRenderer` | `LevelRenderer` | Too broad, shader incompatibility |
| `DimensionType` time properties | `DimensionType` | Affects data packs, would require custom dimension |

### Safe Isolation Pattern

**Client visuals:** Override `getSunAngle`, `getTimeOfDay`, `getSkyDarken` ONLY on `ClientLevel`
**Server mechanics:** Target specific methods per feature (isSunBurnTick, wantsToEnterHive, spawn predicates)

This preserves:
- Villager schedules (use actual server time)
- Phantom spawning (insomnia timer uses real time)
- Crop growth (real tick-based)
- Raid timing (real time)
- Bed sleep mechanics (real time)

---

## Implementation Checklist

### Phase 1: Client Twilight Visuals
- [ ] Create `ClientLevelTwilightMixin` in `src/client/java/thc/mixin/client/`
- [ ] Override `getSunAngle()` to return dusk angle
- [ ] Override `getTimeOfDay()` to return dusk time
- [ ] Test with Iris shaders for compatibility
- [ ] Add config option to disable

### Phase 2: Undead Sun Immunity
- [ ] Create `MobSunBurnMixin` in `src/main/java/thc/mixin/`
- [ ] Override `isSunBurnTick()` for undead types
- [ ] Verify zombies/skeletons don't burn
- [ ] Test that fire from other sources still works

### Phase 3: Hostile Spawning (Sky Light Ignore)
- [ ] Research spawn placement predicate system
- [ ] Create targeted mixin for Monster spawn checks
- [ ] Verify doesn't affect claimed chunk blocking
- [ ] Test spawn rates in lit outdoor areas

### Phase 4: Bee Always-Work
- [ ] Create `BeeTwilightMixin`
- [ ] Override `wantsToEnterHive()` time check
- [ ] Verify bees still enter when carrying nectar
- [ ] Test pollination cycles

---

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Client time methods | MEDIUM | Signatures from Forge/NeoForge JavaDocs (1.18-1.21), not directly verified for 1.21.11 |
| `isSunBurnTick` | HIGH | Method confirmed in multiple JavaDocs versions |
| Bee methods | MEDIUM | Method list from 1.18.2 docs, may have changed |
| Spawn light checks | LOW | General mechanics known, specific method targets need verification |
| Mixin strategy | HIGH | Standard Fabric patterns, widely documented |
| What NOT to hook | HIGH | Based on understanding of time-dependent systems |

### Verification Needed

1. **Confirm method signatures in 1.21.11:** Decompile with Mojang mappings to verify exact signatures haven't changed
2. **Test shader compatibility:** Verify Iris doesn't conflict with `getSunAngle` mixin
3. **Spawn predicate system:** Research `SpawnPlacements` registry for proper hostile spawn modification

---

## Sources

### Primary (HIGH confidence)
- [NeoForge 1.21.0 ServerLevel JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/server/level/ServerLevel.html) - Time method signatures
- [Forge 1.19.3 Level JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.19.3/net/minecraft/world/level/Level.html) - Sky/time methods
- [Fabric API DimensionRenderingRegistry](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/client/rendering/v1/DimensionRenderingRegistry.html) - Sky renderer API
- [Fabric Wiki Mixin Examples](https://wiki.fabricmc.net/tutorial:mixin_examples) - Injection patterns
- [Yarn 1.21.4 SkyRendering](https://maven.fabricmc.net/docs/yarn-1.21.4+build.1/net/minecraft/client/render/SkyRendering.html) - Sky rendering methods

### Secondary (MEDIUM confidence)
- [Evernight Mod](https://www.curseforge.com/minecraft/mc-mods/evernight) - Pattern for client-side time override (no source available)
- [Forge 1.18.2 Mob JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/Mob.html) - `isSunBurnTick` signature
- [Forge 1.18.2 Bee JavaDocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/animal/Bee.html) - Bee goal methods
- [Minecraft Wiki Light](https://minecraft.wiki/w/Light) - Spawn light mechanics
- [Minecraft Wiki Bee](https://minecraft.wiki/w/Bee) - Bee behavior timing

### Tertiary (LOW confidence)
- General web search results for spawn predicate methods (needs decompiled source verification)

---

## Metadata

**Research date:** 2026-01-20
**Valid until:** 2026-02-20 (30 days - stable domain)
**Requires validation:**
- Method signatures against actual 1.21.11 decompiled source
- Spawn placement predicate system details
- Shader mod compatibility testing
