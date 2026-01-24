# Phase 43: Monster Cap Partitioning - Research

**Researched:** 2026-01-24
**Domain:** Minecraft 1.21.11 regional spawn caps, entity counting, NaturalSpawner mixin patterns
**Confidence:** HIGH

## Summary

Phase 43 implements partitioned monster caps that enforce independent spawn limits for each Overworld region (Surface, Upper Cave, Lower Cave). The implementation uses a fresh-count approach where each spawn cycle iterates loaded mobs to count by their SPAWN_REGION attachment, comparing against hard-coded absolute caps (21/28/35). This intercepts vanilla's cap checking via a mixin redirect on `SpawnState.canSpawnForCategory` or by injecting into `NaturalSpawner.spawnForChunk` before category spawning begins.

The existing infrastructure from Phase 41/42 provides SPAWN_REGION and SPAWN_COUNTED attachments already set on all naturally spawned Overworld monsters. The cap enforcement simply counts these tagged mobs and blocks spawn attempts when regional caps are reached.

**Primary recommendation:** Extend existing NaturalSpawnerMixin to perform regional cap counting at spawn cycle start, store counts in ThreadLocal, and use those counts to gate spawn attempts in the existing redirect before custom mob selection.

## Standard Stack

The established libraries/tools for spawn cap modification in Minecraft 1.21.11:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.119.2+ | Attachment API for entity state | Already used for SPAWN_REGION, SPAWN_COUNTED |
| Mixin | 0.8.5+ | Bytecode injection framework | Required for NaturalSpawner interception |
| Minecraft | 1.21.11 | Target game version | Project constraint |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Object2IntMap | fastutil | Efficient primitive int maps | Category counting (vanilla pattern) |
| ThreadLocal | JDK 21 | Cross-method state in static context | Pass counts between spawn cycle methods |
| EnumMap | JDK 21 | Region-keyed cap storage | Fast constant-key lookups |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Fresh count each cycle | Event-based counter tracking | Fresh count is simpler, always accurate; event tracking requires load/unload/death hooks |
| ThreadLocal for counts | Mixin capture locals | ThreadLocal is cleaner for static methods, explicit lifecycle |
| @Redirect on canSpawnForCategory | @Inject HEAD with cancellation | Redirect allows modifying return value; Inject only cancels |

**Installation:**
Already in project - no additional dependencies needed.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── spawn/
│   ├── RegionalCapManager.java    # NEW: counting + cap enforcement logic
│   ├── SpawnDistributions.java    # Existing: weighted selection
│   └── SpawnRegion.java           # Existing: region enum (OW_SURFACE, etc)
├── mixin/
│   ├── NaturalSpawnerMixin.java   # EXISTING: base chunk blocking
│   ├── SpawnReplacementMixin.java # EXTEND: add cap check before distribution
│   └── MobFinalizeSpawnMixin.java # Existing: NBT tagging
└── THCAttachments.java            # Existing: SPAWN_REGION, SPAWN_COUNTED
```

### Pattern 1: Fresh Count at Spawn Cycle Start

**What:** Count all loaded mobs with SPAWN_COUNTED=true grouped by SPAWN_REGION at the start of each spawn cycle.

**When to use:** At the beginning of NaturalSpawner.spawnForChunk, before any spawn attempts.

**Example:**
```java
// Source: User decision - fresh count each spawn cycle
public class RegionalCapManager {
    // ThreadLocal to pass counts from spawn cycle start to spawn attempts
    private static final ThreadLocal<Map<String, Integer>> REGIONAL_COUNTS =
        ThreadLocal.withInitial(HashMap::new);

    // Hard-coded caps per user decision
    private static final Map<String, Integer> REGIONAL_CAPS = Map.of(
        "OW_SURFACE", 21,
        "OW_UPPER_CAVE", 28,
        "OW_LOWER_CAVE", 35
    );

    /**
     * Count all SPAWN_COUNTED mobs by region.
     * Called at start of spawn cycle.
     */
    public static void countMobsByRegion(ServerLevel level) {
        Map<String, Integer> counts = new HashMap<>();

        // Iterate all loaded entities (reuse vanilla's iteration pattern)
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;

            // Only count MONSTER category with SPAWN_COUNTED=true
            if (mob.getType().getCategory() != MobCategory.MONSTER) continue;

            Boolean counted = mob.getAttached(THCAttachments.SPAWN_COUNTED);
            if (counted == null || !counted) continue;

            String region = mob.getAttached(THCAttachments.SPAWN_REGION);
            if (region != null) {
                counts.merge(region, 1, Integer::sum);
            }
        }

        REGIONAL_COUNTS.set(counts);
    }

    /**
     * Check if region cap allows spawning.
     * Returns true if spawn should proceed.
     */
    public static boolean canSpawnInRegion(String region) {
        if (region == null) return true; // Non-Overworld uses vanilla caps

        Integer cap = REGIONAL_CAPS.get(region);
        if (cap == null) return true; // Unknown region, allow

        int current = REGIONAL_COUNTS.get().getOrDefault(region, 0);
        return current < cap;
    }

    /**
     * Clear ThreadLocal after spawn cycle completes.
     */
    public static void clearCounts() {
        REGIONAL_COUNTS.remove();
    }
}
```

### Pattern 2: Integration with Existing Spawn Replacement

**What:** Check regional cap BEFORE rolling custom distribution.

**When to use:** In SpawnReplacementMixin redirect, as first check after passenger check.

**Example:**
```java
// Source: Existing SpawnReplacementMixin integration point
@Redirect(...)
private static void thc$replaceWithSurfaceVariant(ServerLevel level, Entity entity) {
    // Preserve passenger checks (existing)
    if (!entity.getPassengers().isEmpty() || entity.getVehicle() != null) {
        level.addFreshEntityWithPassengers(entity);
        return;
    }

    BlockPos pos = entity.blockPosition();
    String region = thc$detectRegion(level, pos);

    // NEW: Regional cap check
    if (region != null && !RegionalCapManager.canSpawnInRegion(region)) {
        // Cap reached for this region - skip spawn entirely
        // Do not fall back to vanilla, do not spawn
        return;
    }

    // EXISTING: Regional distribution roll
    if (region != null) {
        SpawnDistributions.MobSelection selection = SpawnDistributions.selectMob(region, level.random);
        // ... rest of existing logic
    }

    // EXISTING: Vanilla fallback with surface variants
    Entity entityToSpawn = thc$getReplacementEntity(level, entity);
    level.addFreshEntityWithPassengers(entityToSpawn);
}
```

### Pattern 3: Spawn Cycle Hook via @Inject

**What:** Hook start and end of spawn cycle to count/clear.

**When to use:** At spawnForChunk HEAD and RETURN.

**Example:**
```java
// Source: fabric-carpet NaturalSpawnerMixin pattern
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

    @Inject(
        method = "spawnForChunk",
        at = @At("HEAD")
    )
    private static void thc$countRegionsAtCycleStart(
            ServerLevel level, LevelChunk chunk,
            NaturalSpawner.SpawnState state,
            boolean spawnMonsters, boolean spawnCreatures, boolean spawnAquatic,
            CallbackInfo ci) {

        // Only count if spawning monsters in Overworld
        if (spawnMonsters && level.dimension() == Level.OVERWORLD) {
            RegionalCapManager.countMobsByRegion(level);
        }
    }

    @Inject(
        method = "spawnForChunk",
        at = @At("RETURN")
    )
    private static void thc$clearCountsAtCycleEnd(
            ServerLevel level, LevelChunk chunk,
            NaturalSpawner.SpawnState state,
            boolean spawnMonsters, boolean spawnCreatures, boolean spawnAquatic,
            CallbackInfo ci) {

        RegionalCapManager.clearCounts();
    }
}
```

### Anti-Patterns to Avoid

- **Modifying SpawnState counts:** Don't try to modify vanilla's Object2IntMap in SpawnState. THC's regional caps are ADDITIONAL to vanilla's global cap, not a replacement. User decision: three independent caps, no global cap on top.

- **Position-based counting:** User decision specifies mobs count against their SPAWN_REGION tag, NOT current position. A mob that wanders between regions still counts against its original spawn region.

- **Event-based counter maintenance:** User explicitly chose fresh-count approach over event tracking. Don't add load/unload/death listeners to maintain counters.

- **Re-rolling on cap fail:** If regional cap is reached, skip the spawn entirely. No fallback to other regions or vanilla selection.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Entity iteration | Manual chunk iteration | level.getAllEntities() | Handles all loaded chunks, chunk loading state |
| Cross-method state | Static field | ThreadLocal | Thread-safe for async chunk loading, clean lifecycle |
| Attachment reading | NBT parsing | getAttached(THCAttachments.X) | Type-safe, existing infrastructure |
| Region detection | Y-level checks at count time | Read SPAWN_REGION attachment | User decision: count by tag, not current position |

**Key insight:** The counting logic is simple because Phase 41/42 already tag every naturally spawned monster with SPAWN_REGION and SPAWN_COUNTED. Cap enforcement just reads these existing tags.

## Common Pitfalls

### Pitfall 1: Counting Non-Monster Categories

**What goes wrong:** Including non-MONSTER mobs in regional counts causes cap to fill incorrectly.

**Why it happens:** SPAWN_REGION is set on ALL naturally spawned mobs (via MobFinalizeSpawnMixin), but only MONSTER category should count toward MONSTER caps.

**How to avoid:** Always check `mob.getType().getCategory() == MobCategory.MONSTER` AND `SPAWN_COUNTED == true` before counting.

**Warning signs:**
- Caps filling faster than expected
- Regional counts higher than visible monsters
- Passive mobs affecting spawn rates

**Correct pattern:**
```java
if (mob.getType().getCategory() != MobCategory.MONSTER) continue;
Boolean counted = mob.getAttached(THCAttachments.SPAWN_COUNTED);
if (counted == null || !counted) continue;
// NOW count
```

### Pitfall 2: Counting Mobs Without Tags

**What goes wrong:** Counting mobs that lack SPAWN_REGION/SPAWN_COUNTED (spawner mobs, command-spawned, structure mobs).

**Why it happens:** Not all mobs have these attachments - only NATURAL/CHUNK_GENERATION spawns in Overworld.

**How to avoid:** Check for null attachment values. Spec says: only mobs with `spawnSystem.counted == true` contribute to caps.

**Warning signs:**
- Caps counting zombie spawner mobs
- Structure dungeon mobs affecting caps
- Command-spawned mobs filling cap

**Correct pattern from existing code:**
```java
// MobFinalizeSpawnMixin only sets these for NATURAL/CHUNK_GENERATION in Overworld
// So null means "not from natural spawn system"
Boolean counted = mob.getAttached(THCAttachments.SPAWN_COUNTED);
if (counted == null || !counted) continue;
```

### Pitfall 3: ThreadLocal Leaks

**What goes wrong:** ThreadLocal not cleared after spawn cycle, causing stale counts or memory leaks.

**Why it happens:** Early return paths in mixin skip RETURN injection.

**How to avoid:** Use try/finally pattern or ensure RETURN injection fires for all exit paths.

**Warning signs:**
- Counts accumulating across cycles
- Stale counts after dimension change
- Memory growth over time

**Correct pattern:**
```java
@Inject(method = "spawnForChunk", at = @At("HEAD"))
private static void thc$startCycle(...) {
    // Count BEFORE any spawning
    RegionalCapManager.countMobsByRegion(level);
}

@Inject(method = "spawnForChunk", at = @At("RETURN"))
private static void thc$endCycle(...) {
    // Always clear, even on exceptions (RETURN fires on all exits)
    RegionalCapManager.clearCounts();
}
```

### Pitfall 4: Cap Check Location

**What goes wrong:** Checking caps AFTER custom mob selection or in wrong mixin.

**Why it happens:** Multiple injection points in spawn system - easy to pick wrong one.

**How to avoid:** Check cap in SpawnReplacementMixin redirect, BEFORE rolling SpawnDistributions.selectMob(). This matches existing architecture where:
1. NaturalSpawnerMixin HEAD blocks base chunks
2. SpawnReplacementMixin redirect does regional distribution
3. Add cap check at START of redirect

**Warning signs:**
- Mobs spawning above cap
- Custom mobs spawning but vanilla blocked (or vice versa)
- Inconsistent cap enforcement

**Correct integration point:**
```java
// In SpawnReplacementMixin.thc$replaceWithSurfaceVariant()
// AFTER passenger check
// BEFORE region detection and distribution roll
if (region != null && !RegionalCapManager.canSpawnInRegion(region)) {
    return; // Block spawn, don't add entity
}
```

### Pitfall 5: Vanilla Cap Interaction

**What goes wrong:** Regional caps blocking spawns that vanilla would allow, or vice versa.

**Why it happens:** Confusion about whether THC replaces or extends vanilla caps.

**How to avoid:** Per user decision, THC has THREE INDEPENDENT regional caps with NO global cap on top. Vanilla's global cap (70) does NOT apply to THC regions. Each region can be at 100% simultaneously (84 total mobs possible).

**Warning signs:**
- Total mobs stuck at 70
- Vanilla cap errors in console
- Regions blocking each other

**User decision spec:**
```
- Three independent regional caps, no global cap applies on top
- All three regions can be at 100% simultaneously (84 total mobs possible)
- Hard-coded absolute values: Surface 21, Upper Cave 28, Lower Cave 35
```

## Code Examples

Verified patterns from existing codebase and research:

### Entity Iteration for Counting

```java
// Source: Existing THC pattern from LivingEntityMixin + MobDamageThreatMixin
// Using level.getAllEntities() for full entity iteration
public static void countMobsByRegion(ServerLevel level) {
    Map<String, Integer> counts = new HashMap<>();

    // getAllEntities() returns Iterable<Entity> covering all loaded entities
    for (Entity entity : level.getAllEntities()) {
        if (!(entity instanceof Mob mob)) continue;
        if (mob.getType().getCategory() != MobCategory.MONSTER) continue;

        Boolean counted = mob.getAttached(THCAttachments.SPAWN_COUNTED);
        if (counted == null || !counted) continue;

        String region = mob.getAttached(THCAttachments.SPAWN_REGION);
        if (region != null) {
            counts.merge(region, 1, Integer::sum);
        }
    }

    REGIONAL_COUNTS.set(counts);
}
```

### Cap Check Integration

```java
// Source: Integration point in existing SpawnReplacementMixin
// Add at line ~85, after passenger check, before region detection
@Redirect(...)
private static void thc$replaceWithSurfaceVariant(ServerLevel level, Entity entity) {
    // Preserve passenger checks (existing - line 76-79)
    if (!entity.getPassengers().isEmpty() || entity.getVehicle() != null) {
        level.addFreshEntityWithPassengers(entity);
        return;
    }

    BlockPos pos = entity.blockPosition();

    // Step 1.5 NEW: Regional cap check (Overworld only)
    String region = thc$detectRegion(level, pos);
    if (region != null && !RegionalCapManager.canSpawnInRegion(region)) {
        // Regional cap reached - do not spawn this entity
        // Per spec: "fail the attempt (no fallback)"
        return;
    }

    // Step 2: Regional distribution roll (existing - line 84-94)
    // ... existing code unchanged ...
}
```

### Spawn Cycle Hooks

```java
// Source: fabric-carpet pattern adapted for THC
// Add to existing NaturalSpawnerMixin or new dedicated mixin
@Inject(
    method = "spawnForChunk",
    at = @At("HEAD")
)
private static void thc$initRegionalCounts(
        ServerLevel level, LevelChunk chunk,
        NaturalSpawner.SpawnState state,
        boolean spawnFriendlies, boolean spawnMonsters, boolean spawnMisc,
        CallbackInfo ci) {

    // Only count for Overworld monster spawning
    if (level.dimension() == Level.OVERWORLD && spawnMonsters) {
        RegionalCapManager.countMobsByRegion(level);
    }
}

@Inject(
    method = "spawnForChunk",
    at = @At("RETURN")
)
private static void thc$clearRegionalCounts(
        ServerLevel level, LevelChunk chunk,
        NaturalSpawner.SpawnState state,
        boolean spawnFriendlies, boolean spawnMonsters, boolean spawnMisc,
        CallbackInfo ci) {

    RegionalCapManager.clearCounts();
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Global mob cap only | Per-player local caps + global | MC 1.18 | Better spawn distribution |
| Single global monster cap | Regional partitioning (THC custom) | This phase | Prevents surface monopolization |
| Event-based counter tracking | Fresh count each cycle | User decision | Simpler, always accurate |
| Position-based region | Tag-based region (persist at spawn) | User decision | Mobs count in original region |

**Deprecated/outdated:**
- **Modifying SpawnState.groupToCount:** Don't inject custom counts into vanilla's tracking. Use parallel system.
- **SpawnDensityCapper interception:** Not needed - THC uses independent caps, not modified vanilla caps.

## Open Questions

Things that couldn't be fully resolved:

1. **getAllEntities() performance**
   - What we know: Returns Iterable over all loaded entities. Used successfully in existing THC code.
   - What's unclear: Performance impact with many loaded chunks. Spec says "acceptable overhead."
   - Recommendation: Use as-is. Profile if performance issues arise. Could optimize with getEntitiesOfClass(Mob.class) if needed.

2. **spawnForChunk method signature in 1.21.11**
   - What we know: Method exists, called per-chunk in spawn tick. Parameters include SpawnState.
   - What's unclear: Exact parameter names/order may vary between mapping versions.
   - Recommendation: Check yarn/mojang mappings for 1.21.11. The boolean parameters control which categories spawn.

3. **ThreadLocal cleanup on exception**
   - What we know: @Inject at RETURN fires on normal return. Unclear on exceptions.
   - What's unclear: Whether exceptions in spawn cycle would skip RETURN injection.
   - Recommendation: Test exception scenarios. If issues, wrap counting in try/finally at call site.

## Sources

### Primary (HIGH confidence)
- Existing THC codebase patterns:
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/NaturalSpawnerMixin.java` - Base chunk blocking pattern
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/SpawnReplacementMixin.java` - Regional distribution integration
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/MobFinalizeSpawnMixin.java` - NBT tagging
  - `/mnt/c/home/code/thc/src/main/java/thc/THCAttachments.java` - SPAWN_REGION, SPAWN_COUNTED
- `/mnt/c/home/code/thc/.planning/phases/43-monster-cap-partitioning/43-CONTEXT.md` - User decisions
- `/mnt/c/home/code/thc/.planning/MILESTONE_EXTRA_FEATURES_BATCH_7.md` - Full specification

### Secondary (MEDIUM confidence)
- [Fabric yarn SpawnHelper.Info](https://maven.fabricmc.net/docs/yarn-1.21+build.1/net/minecraft/world/SpawnHelper.Info.html) - isBelowCap, getGroupToCount signatures
- [fabric-carpet NaturalSpawnerMixin](https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/mixins/NaturalSpawnerMixin.java) - Spawn cycle hook patterns
- [Curtain NaturalSpawnerMixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java) - 1.21 mixin patterns
- [Technical Minecraft Wiki - Mob Caps](https://techmcdocs.github.io/pages/GameMechanics/MobCap/) - Vanilla cap mechanics

### Tertiary (LOW confidence)
- [Minecraft Wiki - Mob spawning](https://minecraft.wiki/w/Mob_spawning) - General spawn mechanics overview
- [NaturalSpawner.SpawnState Forge docs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/level/NaturalSpawner.SpawnState.html) - Older version reference

## Metadata

**Confidence breakdown:**
- Cap structure: HIGH - User decisions explicit, implementation straightforward
- Counting approach: HIGH - getAllEntities() used elsewhere in codebase, attachment reading verified
- Integration points: HIGH - Existing SpawnReplacementMixin provides clear hook location
- Spawn cycle hooks: MEDIUM - Pattern from fabric-carpet, exact 1.21.11 signature needs verification
- Performance: MEDIUM - User accepted "acceptable overhead", no profiling data

**Research date:** 2026-01-24
**Valid until:** Minecraft 1.21.x (attachment API and mixin patterns stable within minor versions)
**Phase dependencies:** Phase 41 (NBT tagging), Phase 42 (regional distribution) - COMPLETE

**Notes for planner:**
1. Phase 41/42 infrastructure (SPAWN_REGION, SPAWN_COUNTED) is already functional
2. User decisions are firm: fresh count, tag-based tracking, absolute caps (21/28/35)
3. Regional caps are INDEPENDENT - no global cap applies
4. Integration point is SpawnReplacementMixin redirect, before distribution roll
5. Only Overworld has regional caps; Nether/End use vanilla unchanged
