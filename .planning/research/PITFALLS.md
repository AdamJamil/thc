# Pitfalls Research: v2.0 Twilight System

**Researched:** 2026-01-20
**Milestone:** v2.0 Twilight System
**Confidence:** MEDIUM (verified with official docs, community sources, and existing codebase patterns)

## Summary

This document catalogs common mistakes when implementing the v2.0 features:
- Client-side sky/lighting rendering modifications (first client mixins in this codebase)
- Spawn mechanics enhancements (building on existing NaturalSpawnerMixin)
- Undead mob burning behavior changes
- Bee/mob AI modifications

The project has working server-side mixin patterns (MonsterThreatGoalMixin for AI, NaturalSpawnerMixin for spawning). v2.0 introduces client-side mixins for the first time, which requires additional care around client-server separation and rendering thread safety.

---

## Client Rendering Pitfalls

### SKY-01: Wrong Thread for Render Operations

**Risk:** Fabric requires render system calls to happen on the render thread. Client mixin code running on network thread or main thread causes crashes.

**Warning signs:**
- `java.lang.IllegalStateException: RenderSystem called from wrong thread`
- Crash during mixin apply in `MixinProcessor.applyMixins`
- Works in singleplayer, crashes in multiplayer

**Prevention:**
- Verify mixin targets render methods that naturally run on render thread
- For sky rendering, target methods in `LevelRenderer` or `SkyRendering` (1.21.4+)
- Never call RenderSystem methods from network packet handlers
- Test both singleplayer and dedicated server connections

**Phase:** Sky rendering phase - verify thread context early

---

### SKY-02: Resource Cleanup on Renderer Close

**Risk:** Custom render pipelines create GPU resources (vertex buffers, shaders). Failing to clean up causes memory leaks and crashes on world reload.

**Warning signs:**
- Memory usage grows on each world load
- Crash when joining different dimension
- GPU memory warnings in logs
- Crash on game exit

**Prevention:**
- Inject into `GameRenderer#close` to release resources:
  ```java
  @Inject(method = "close", at = @At("RETURN"))
  private void onGameRendererClose(CallbackInfo ci) {
      // Close allocator and vertex buffers
  }
  ```
- Test dimension changes (Nether portal, End portal)
- Test repeated world load/unload cycles

**Phase:** Sky rendering phase - implement cleanup from start

**Source:** [Fabric Documentation - World Rendering](https://docs.fabricmc.net/develop/rendering/world)

---

### SKY-03: Extraction vs Drawing Phase Confusion

**Risk:** Fabric's modern rendering separates extraction (collecting data) from drawing (GPU execution). Calling draw operations during extraction phase causes crashes or visual corruption.

**Warning signs:**
- Rendering flickers or shows garbage
- Crash with buffer state errors
- Works initially then fails after a few frames

**Prevention:**
- Extraction phase: Populate BufferBuilder with vertices only
- Drawing phase: Build buffer, upload to GPU, execute draw calls
- Don't mix the phases - store state between them
- Use `buffer.buildOrThrow()` only in draw phase

**Phase:** Sky rendering phase - follow two-phase pattern

**Source:** [Fabric Documentation - World Rendering](https://docs.fabricmc.net/develop/rendering/world)

---

### SKY-04: LevelRenderer Changes in 1.21.x

**Risk:** Minecraft 1.21.x refactored `LevelRenderer` significantly. Sodium and other mods alter method signatures. Mixins targeting old signatures fail.

**Warning signs:**
- Mixin fails to apply with method not found
- Conflict with Sodium or Iris installed
- Works in dev, fails with shader mods

**Prevention:**
- Target 1.21.11 specifically (current project version)
- In 1.21.4+, sky rendering moved to dedicated `SkyRendering` class
- Document target method signatures explicitly in mixin comments
- Test with Sodium installed (common mod users have)
- Use `@WrapOperation` instead of `@Redirect` for compatibility

**Phase:** Sky rendering phase - verify target class/method exists

**Source:** [GitHub Issue - Malilib LevelRenderer Conflict](https://github.com/sakura-ryoko/malilib/issues/62)

---

### SKY-05: DimensionEffects Registration Timing

**Risk:** Custom dimension effects must be registered before dimension loads. Late registration causes fallback to default sky rendering.

**Warning signs:**
- Custom sky doesn't appear in custom dimensions
- Works after `/reload` but not initially
- Different behavior on server restart vs client reconnect

**Prevention:**
- Register dimension effects in `ClientModInitializer.onInitializeClient()`
- Use `DimensionRenderingRegistry.registerDimensionEffects()` for custom dimension JSON
- For overworld modifications, mixin directly rather than registering effects

**Phase:** Sky rendering phase - registration in client initializer

**Source:** [Fabric API - DimensionRenderingRegistry](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/client/rendering/v1/DimensionRenderingRegistry.html)

---

### SKY-06: Client-Only Code in Common Mixin

**Risk:** Putting client-only classes in common mixins causes server crash with `Cannot load class net.minecraft.client.X in environment type SERVER`.

**Warning signs:**
- Server crashes on startup
- `NoClassDefFoundError` for client classes
- Works in dev (combined client+server) but fails on dedicated server

**Prevention:**
- Use separate mixin config files: `thc.mixins.json` (server) and `thc.client.mixins.json` (client)
- Project already has this structure - add client rendering mixins to `thc.client.mixins.json` only
- Never import `net.minecraft.client.*` in server mixins
- Use `@Environment(EnvType.CLIENT)` annotation for safety

**Phase:** All client phases - verify mixin config placement

---

## Spawn Modification Pitfalls

### SPAWN-01: Incomplete Spawn Blocking Scope

**Risk:** Current `NaturalSpawnerMixin` blocks `isValidSpawnPostitionForType`. Other spawn methods (`spawnCategoryForPosition`, `spawnCategoryForChunk`) may bypass this.

**Warning signs:**
- Some mob types still spawn in blocked areas
- Works for zombies but not for other categories
- Spawn eggs and spawners still work (this may be intentional)

**Prevention:**
- Existing `isValidSpawnPostitionForType` injection is correct for natural spawns
- If adding new spawn conditions, verify they use same code path
- Spawner and spawn egg bypasses are likely intentional (they don't use NaturalSpawner)
- Document which spawn types are blocked vs allowed

**Phase:** Spawn modification phase - enumerate spawn types explicitly

**Source:** [GitHub - fabric-carpet NaturalSpawnerMixin](https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/mixins/NaturalSpawnerMixin.java)

---

### SPAWN-02: Performance of Spawn Checks

**Risk:** Spawn position validity is checked many times per tick. Expensive checks (database lookups, complex calculations) cause lag.

**Warning signs:**
- TPS drops in loaded chunks
- Server lag when many players online
- Profiler shows spawn check methods taking excessive time

**Prevention:**
- Current `ClaimManager.INSTANCE.isClaimed()` should use cached chunk data
- Avoid database queries in spawn checks - cache claim status per chunk
- Consider chunk position lookup instead of block position (cheaper)
- Benchmark with 50+ chunks loaded

**Phase:** Spawn modification phase - profile with many chunks

---

### SPAWN-03: Mixin Signature Mismatch

**Risk:** `isValidSpawnPostitionForType` has specific parameter order. Mixin with wrong signature silently fails or causes crashes on MC version change.

**Warning signs:**
- `Mixin signature mismatch` warnings in logs
- Spawn blocking doesn't work after MC update
- Method found but injection doesn't fire

**Prevention:**
- Current signature matches 1.21.11:
  ```java
  (ServerLevel, MobCategory, StructureManager, ChunkGenerator,
   MobSpawnSettings.SpawnerData, BlockPos.MutableBlockPos, double,
   CallbackInfoReturnable<Boolean>)
  ```
- Document exact signature in mixin comments
- Verify signature after any MC version bump

**Phase:** Spawn modification phase - verify after updates

**Source:** [Modrinth - Fabric per player spawns changelog](https://modrinth.com/mod/fabric-per-player-spawns/changelog)

---

### SPAWN-04: Category-Specific Spawn Logic

**Risk:** Different `MobCategory` types (MONSTER, CREATURE, AMBIENT, etc.) have different spawn rules. Changes to one category may not apply to others.

**Warning signs:**
- Zombies blocked but fish spawn
- Passive animals still spawn in base
- Different behavior day vs night

**Prevention:**
- Current implementation checks all categories (good)
- If filtering by category, document which categories are affected
- Note: WATER_CREATURE and WATER_AMBIENT spawn differently than land mobs
- Test each category explicitly

**Phase:** Spawn modification phase - test per-category

---

## Mob Burning Pitfalls

### BURN-01: isSunBurnTick Check Order

**Risk:** Undead burning uses `isSunBurnTick()` which checks: daytime, sky exposure, no helmet, not in water. Modifying wrong condition has unexpected effects.

**Warning signs:**
- Undead don't burn even when intended
- Helmet no longer protects
- Water no longer protects
- Burning happens in caves

**Prevention:**
- To prevent ALL sun burning: override `isSunBurnTick()` return value
- To prevent in specific areas: check location in `aiStep()` before fire is set
- Don't modify vanilla checks for helmet/water protection
- Target `Mob` class for `isSunBurnTick()` (zombies and skeletons inherit from Mob)

**Phase:** Burning prevention phase - understand check sequence first

---

### BURN-02: Helmet Durability Side Effect

**Risk:** When undead wear helmets, the helmet takes durability damage (50% chance per tick) instead of the mob. Preventing burning may need to also prevent this durability loss.

**Warning signs:**
- Helmets on mobs in twilight zone break over time
- Mob stops being protected after helmet breaks
- Different behavior with/without armor

**Prevention:**
- If preventing burning entirely, helmet durability loss is moot
- If using helmet to prevent burning, monitor helmet durability
- Vanilla: Helmets have 50% chance to lose 1 durability per burn tick

**Phase:** Burning prevention phase - decide helmet interaction

**Source:** [Minecraft Wiki - Zombie](https://minecraft.fandom.com/wiki/Zombie)

---

### BURN-03: Visual Fire Effect Desync

**Risk:** Burning has visual fire effect on client. Preventing server damage without preventing client effect causes visual desync.

**Warning signs:**
- Mob appears on fire but takes no damage
- Fire effect flickers on/off
- Different visual on different clients

**Prevention:**
- Use `setRemainingFireTicks(0)` or `-1` to fully prevent fire
- Or prevent fire from being set in the first place (cleaner)
- Don't just absorb the damage - prevent the fire state
- Client syncs fire state automatically if using entity data

**Phase:** Burning prevention phase - prevent fire state, not just damage

---

### BURN-04: Multiple Fire Sources

**Risk:** Mobs can catch fire from multiple sources: sun, lava, fire blocks, fire aspect enchant. Preventing sun burning shouldn't prevent other fire.

**Warning signs:**
- Mobs can't be set on fire by players
- Fire aspect doesn't work
- Lava doesn't burn mobs

**Prevention:**
- Hook specifically into `aiStep()` sun burning logic, not general `setRemainingFireTicks()`
- Sun burning is applied in `Mob.aiStep()` after `isSunBurnTick()` check
- Other fire sources use different code paths

**Phase:** Burning prevention phase - target sun-specific code path

---

### BURN-05: Phantom and Other Non-Mob Undead

**Risk:** Phantoms burn in sunlight but aren't `Mob` subclass in the same way. Other undead variants (strays, husks) may have custom burning logic.

**Warning signs:**
- Works on zombies/skeletons but not phantoms
- Husks still burn (they shouldn't in vanilla either)
- Drowned behave differently underwater

**Prevention:**
- Zombie, Skeleton, Phantom all use `isSunBurnTick()` but at different points
- Husks never burn (override `isSunBurnTick()` to return false)
- Drowned burn normally unless in water
- Test each undead mob type explicitly

**Phase:** Burning prevention phase - enumerate undead types

---

## Bee AI Pitfalls

### BEE-01: Brain vs Goal AI System

**Risk:** Bees use both Brain-based and Goal-based AI depending on version and behavior. Modifying wrong system doesn't affect behavior.

**Warning signs:**
- Goal modification doesn't change behavior
- Works in one MC version but not another
- Bee ignores custom goals

**Prevention:**
- In modern versions, bees use Brain-based AI for complex behavior
- Check `Bee.registerGoals()` and `Bee.getBrain()` for which system controls what
- Home location memory uses Brain system (`MemoryModuleType`)
- Pollination uses Goal system

**Phase:** Bee AI phase - determine which system to target

**Source:** [Modrinth - Brainier Bees](https://modrinth.com/mod/brainier-bees)

---

### BEE-02: Bee Wander Goal Flying Into Void

**Risk:** Bees' wander goal can path them away from solid blocks in skyblock scenarios, causing them to fly into the void or get stuck.

**Warning signs:**
- Bees fly away from hive and don't return
- Bees get stuck at build limit
- Mob cap fills with stuck bees

**Prevention:**
- This is a vanilla bug (MC-206401)
- If modifying bee AI, consider limiting wander range from solid blocks
- Or use existing fix mod pattern (Brainier Bees, Bumblegum)
- Test in skyblock-style void world

**Phase:** Bee AI phase - consider void edge cases

**Source:** [Modrinth - Brainier Bees](https://modrinth.com/mod/brainier-bees)

---

### BEE-03: Bee Home Memory Persistence

**Risk:** Bees remember their home hive. Modifying AI without accounting for home memory causes bees to forget their hive or fail to return.

**Warning signs:**
- Bees pollinate but don't return to hive
- Bees return to wrong hive
- Bees forget hive on chunk reload

**Prevention:**
- Home position stored in Brain memory: `MemoryModuleType.HOME`
- Don't clear brain memories when modifying other behavior
- If moving hives, update bee memories
- Test chunk unload/reload cycle

**Phase:** Bee AI phase - preserve home memory

---

### BEE-04: Baby Bee Suffocation

**Risk:** Smaller (baby) bees can suffocate in blocks due to pathfinding placing them inside blocks.

**Warning signs:**
- Baby bees die randomly
- Death messages show suffocation
- Happens near flowering plants

**Prevention:**
- Consider implementing suffocation damage prevention for bees
- Or fix pathfinding to account for smaller hitbox
- Realistic Bees mod has `preventBeeSuffocationDamage` config

**Phase:** Bee AI phase - consider baby bee edge case

**Source:** [CurseForge - Realistic Bees](https://www.curseforge.com/minecraft/mc-mods/realistic-bees)

---

### BEE-05: Goal Priority Conflicts

**Risk:** GoalSelector runs goals by priority. Adding goals with wrong priority causes existing behavior to break.

**Warning signs:**
- Bees stop pollinating
- Bees don't attack when stung
- Custom goal never runs

**Prevention:**
- Check existing goal priorities before adding new ones
- Same priority = first registered wins (if competing for same flags)
- Lower number = higher priority
- Don't add priority 0 goals unless they should override everything
- Document goal priorities in code comments

**Phase:** Bee AI phase - survey existing priorities

**Source:** [Fabric Yarn API - GoalSelector](https://maven.fabricmc.net/docs/yarn-21w05b+build.11/net/minecraft/entity/ai/goal/GoalSelector.html)

---

## Compatibility Concerns

### COMPAT-01: Mixin Nesting Limitations

**Risk:** `@Redirect` and `@ModifyConstant` cannot be nested (multiple mods targeting same point). This is the #1 compatibility issue in Fabric mods.

**Warning signs:**
- Crash when another mod is installed
- `Mixin conflict` errors in console
- Feature works alone, breaks with modpack

**Prevention:**
- Use MixinExtras alternatives that CAN chain:
  - Replace `@Redirect` with `@WrapOperation`
  - Replace `@ModifyConstant` with `@ModifyExpressionValue`
  - Avoid `@Overwrite` entirely; use `@WrapMethod`
- Project already follows this pattern (good) - continue it

**Phase:** All phases - always use chaining-compatible annotations

**Source:** [Fabric Wiki - Modding Tips](https://wiki.fabricmc.net/tutorial:modding_tips)

---

### COMPAT-02: Sodium/Iris Rendering Conflicts

**Risk:** Sodium heavily modifies rendering pipeline. Iris adds shader support. Both can conflict with custom rendering mixins.

**Warning signs:**
- Visual glitches with Sodium installed
- Shader pack disables custom sky
- Crash in world renderer with shader mods

**Prevention:**
- Test with Sodium and Iris installed
- Consider soft dependencies: detect their presence and adjust behavior
- Iris can disable specific features (`features.render.world.sky.*`)
- Document known incompatibilities

**Phase:** Sky rendering phase - test with Sodium/Iris

---

### COMPAT-03: Other Mob AI Mods

**Risk:** Mods like "AI Improvements", "Better AI", etc. modify same mob goals. Conflicts can cause mobs to stand still or behave erratically.

**Warning signs:**
- Mobs stop moving when other AI mod installed
- Goals fire multiple times
- Mob behavior inconsistent

**Prevention:**
- Use `@Unique` prefix on custom fields: `thc$fieldName`
- Avoid modifying vanilla goal priorities (add new goals instead)
- Document which vanilla methods are modified
- Test with popular AI mods

**Phase:** Bee AI and burning phases - unique field prefixes

**Source:** [Fabric Wiki - Modding Tips](https://wiki.fabricmc.net/tutorial:modding_tips)

---

### COMPAT-04: Spawn Control Mods

**Risk:** Mods like "Custom Spawns", "Spawn Balance Utility" also modify NaturalSpawner. Multiple spawn modifications can conflict.

**Warning signs:**
- Spawn blocking doesn't work with modpack
- Double-blocking causes crashes
- Mod-added mobs still spawn in blocked areas

**Prevention:**
- Use early injection (`@At("HEAD")`) and cancel if blocking
- Check if other mods use same injection point
- Consider config option to disable spawn blocking for mod compatibility

**Phase:** Spawn modification phase - early cancellation

---

## Debug Difficulty

### DEBUG-01: Client Rendering Hard to Test

**Risk:** Client rendering issues only manifest visually. Game tests can't verify sky appearance or rendering correctness.

**Warning signs:**
- Tests pass but feature looks wrong
- Works in one scenario, wrong in another
- Subtle color/lighting differences

**Prevention:**
- Manual visual testing required for all rendering changes
- Document expected visual appearance with screenshots
- Test in different times of day, weather conditions, dimensions
- Test with different video settings (Fabulous, Fast, Fancy)

**Phase:** Sky rendering phase - visual verification checklist

**Source:** [Fabric Documentation - Debugging](https://docs.fabricmc.net/develop/debugging)

---

### DEBUG-02: Multiplayer-Only Sync Issues

**Risk:** Client-server desync only appears in true multiplayer, not integrated server (singleplayer).

**Warning signs:**
- Works in singleplayer, broken in multiplayer
- Different players see different things
- Rubber-banding effects

**Prevention:**
- Test on dedicated server, not just singleplayer
- Use two clients connected to same server
- Watch both client perspectives simultaneously
- Check that entity data syncs properly

**Phase:** All client phases - dedicated server testing

---

### DEBUG-03: Hotswapping Limitations for Mixins

**Risk:** Mixin changes require full restart, unlike regular code which can hotswap in debug mode.

**Warning signs:**
- Code changes don't take effect
- Must restart for every mixin tweak
- Long iteration cycles

**Prevention:**
- Configure Run Configuration for mixin hotswapping (complex setup)
- Or accept restart requirement for mixin development
- Test non-mixin code separately to speed iteration
- Use breakpoints to verify mixin is applying

**Phase:** All phases - expect restart for mixin changes

**Source:** [Fabric Documentation - Debugging](https://docs.fabricmc.net/develop/debugging)

---

### DEBUG-04: Bee Behavior Hard to Observe

**Risk:** Bee AI operates over minutes (pollination cycle, return to hive). Bugs manifest slowly and intermittently.

**Warning signs:**
- Bees "seem fine" in short tests
- Issue reported by players after hours of gameplay
- Can't reproduce reliably

**Prevention:**
- Use time acceleration (`/time add 1000`) to speed up cycles
- Add debug logging to bee goals (remove before release)
- Test multiple bee population simultaneously
- Set up automated bee farm to observe long-term behavior

**Phase:** Bee AI phase - accelerated time testing

---

### DEBUG-05: Spawn Rate Hard to Verify

**Risk:** Spawn blocking affects probability over time. Hard to prove spawns are correctly blocked vs. "just didn't spawn yet".

**Warning signs:**
- "I think it's working" - no definitive proof
- Rare mob still spawns (was it the block or just RNG?)
- Different results on different test runs

**Prevention:**
- Use spectator mode to observe spawn attempts
- Set up spawn proof chamber (only blocked spawns possible)
- Log spawn check results with chunk coordinates
- Statistical testing: many spawns over time, compare rates

**Phase:** Spawn modification phase - statistical verification

---

## Critical Path

Pitfalls most likely to cause rework, ordered by risk:

1. **SKY-06: Client-Only Code in Common Mixin** - Server crash on startup (most catastrophic)
2. **COMPAT-01: Mixin Nesting Limitations** - Breaks with other mods (user-facing)
3. **SKY-02: Resource Cleanup** - Memory leak on dimension change
4. **BEE-01: Brain vs Goal AI** - Wrong system = no effect
5. **BURN-04: Multiple Fire Sources** - Accidentally breaks fire aspect

**Recommended implementation order:**
1. Sky rendering (most complex, most risk, establish client mixin patterns)
2. Spawn modifications (build on existing working pattern)
3. Burning prevention (clear injection point, isolated)
4. Bee AI (depends on understanding AI system selection)

---

## Sources

### Primary (HIGH confidence)
- [Fabric Documentation - World Rendering](https://docs.fabricmc.net/develop/rendering/world) - Render pipeline requirements
- [Fabric Wiki - Modding Tips](https://wiki.fabricmc.net/tutorial:modding_tips) - Mixin compatibility patterns
- [Fabric Documentation - Debugging](https://docs.fabricmc.net/develop/debugging) - Debug techniques
- [Fabric API - DimensionRenderingRegistry](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/client/rendering/v1/DimensionRenderingRegistry.html) - Sky registration API

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Zombie](https://minecraft.fandom.com/wiki/Zombie) - Burning mechanics
- [Minecraft Wiki - Mob AI](https://minecraft.wiki/w/Mob_AI) - Goal vs Brain systems
- [Modrinth - Brainier Bees](https://modrinth.com/mod/brainier-bees) - Bee AI fixes
- [GitHub - fabric-carpet NaturalSpawnerMixin](https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/mixins/NaturalSpawnerMixin.java) - Spawn mixin patterns
- [Modrinth - Mixin Conflict Helper](https://modrinth.com/mod/mixin-conflict-helper) - Diagnosing conflicts

### Tertiary (LOW confidence - needs validation)
- GitHub issue discussions on Sodium/LevelRenderer changes
- CurseForge mod descriptions for bee fixes
- Community reports on spawn blocking implementations

---

## Metadata

**Phase mapping:**
| Pitfall Group | Phase |
|---------------|-------|
| SKY-* | Client sky rendering |
| SPAWN-* | Spawn modification |
| BURN-* | Undead burning prevention |
| BEE-* | Bee AI modification |
| COMPAT-* | All phases |
| DEBUG-* | All phases |

**Research date:** 2026-01-20
**Valid until:** 2026-02-20 (30 days - client mixins are stable domain but need version verification)

**New patterns for this codebase:**
- First client-side mixins (establish patterns carefully)
- First rendering modifications (new complexity domain)
- Building on existing AI pattern (MonsterThreatGoalMixin)
- Building on existing spawn pattern (NaturalSpawnerMixin)
