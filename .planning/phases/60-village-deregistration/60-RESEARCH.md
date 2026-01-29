# Phase 60: Village Deregistration - Research

**Researched:** 2026-01-28
**Domain:** Minecraft 1.21.11 Point of Interest (POI) system, village mechanics, villager AI brain/memory
**Confidence:** MEDIUM

## Summary

Phase 60 prevents beds and villagers in claimed chunks from registering to village mechanics. Minecraft's village system uses a Point of Interest (POI) manager that tracks beds (`minecraft:home`), job sites, and bells in chunk-based data structures. Villagers claim POI within 48 blocks using brain memory modules (`POTENTIAL_JOB_SITE`, `HOME`, `MEETING_POINT`). The system operates through two independent mechanisms: (1) POI registration when blocks are placed, and (2) villager discovery/claiming of registered POI.

**Key architectural decision:** Intercept POI addition and villager claiming at their respective sources using chunk position checks. Block POI registration when beds/job sites/bells are placed in claimed chunks, and prevent villager memory acquisition when villagers attempt to claim POI in claimed chunks.

**Primary recommendation:** Use HEAD injection with cancellation on POI addition methods and villager brain POI acquisition tasks. Leverage existing `ClaimManager.isClaimed()` check pattern from `NaturalSpawnerMixin` for consistency.

## Standard Stack

The established libraries/tools for village mechanics modification in Minecraft 1.21.11:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Mixin | 0.8.5+ | Bytecode injection framework | Required for Fabric modding, already in use |
| Fabric API | 0.119.2+ | Attachment API for entity data | Already used in THC for entity state |
| Minecraft | 1.21.11 | Target game version | Project constraint |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| ClaimManager | THC 2.6+ | Chunk claim checking | Already exists for base mechanics |
| ChunkPos | MC 1.21.11 | Chunk position representation | Standard for chunk-based checks |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| HEAD injection cancellation | Custom POI validation event | Events cleaner but Fabric doesn't provide POI events, would need custom event system |
| Chunk-based filtering | Block NBT tagging | NBT adds complexity, chunk check matches existing patterns |
| Dual interception (POI + villager) | Single point (POI only) | POI-only insufficient - villagers cache/remember existing POI from before chunk was claimed |

**Installation:**
Already in project - no additional dependencies needed.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── mixin/
│   ├── PoiManagerMixin.java         # NEW: Block POI registration in claimed chunks
│   ├── VillagerMixin.java           # EXTEND: Block POI claiming in claimed chunks
│   └── NaturalSpawnerMixin.java     # Existing: chunk claim check pattern reference
└── claim/
    └── ClaimManager.kt              # Existing: isClaimed() method
```

### Pattern 1: POI Registration Blocking via HEAD Injection

**What:** Prevent beds, job sites, and bells from registering as POI when placed in claimed chunks.

**When to use:** At POI creation time, when blocks are placed in the world.

**Example:**
```java
// Source: Existing NaturalSpawnerMixin.java pattern for chunk checks
@Mixin(PoiManager.class)
public class PoiManagerMixin {

    /**
     * Block POI registration for blocks placed in claimed chunks.
     *
     * Prevents beds (minecraft:home), job sites, and bells from being
     * added to the POI system when placed in claimed player bases.
     */
    @Inject(
        method = "add",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockPoiInClaimedChunks(
            BlockPos pos,
            Holder<PoiType> poiType,
            CallbackInfo ci) {

        // Get server instance (from PoiManager context)
        MinecraftServer server = /* access server from context */;

        // Check if chunk is claimed
        ChunkPos chunkPos = new ChunkPos(pos);
        if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
            ci.cancel(); // Block POI registration
        }
    }
}
```

**Integration note:** This blocks NEW POI registration. Existing POI from before a chunk was claimed will remain until explicitly removed or invalidated.

### Pattern 2: Villager POI Claiming via Memory Blocking

**What:** Prevent villagers from claiming POI (beds, job sites) in their brain memory when those POI are in claimed chunks.

**When to use:** When villagers attempt to acquire POI through their AI brain system.

**Context:** Villagers use brain memory modules to store POI locations:
- `MemoryModuleType.POTENTIAL_JOB_SITE` - discovered but unclaimed job sites
- `MemoryModuleType.HOME` - claimed bed position
- `MemoryModuleType.JOB_SITE` - claimed job site position
- `MemoryModuleType.MEETING_POINT` - village bell position

**Example:**
```java
// Source: Village mechanics research + brain memory system
@Mixin(Villager.class)
public class VillagerMixin {

    /**
     * Block villagers from claiming beds in claimed chunks.
     *
     * Intercepts the villager's brain memory update when they attempt
     * to claim a bed, checking if the bed's chunk is claimed.
     */
    @Inject(
        method = "setMemory",  // Brain.setMemory or similar
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockPoiClaimInClaimedChunks(
            MemoryModuleType<?> type,
            Optional<?> value,
            CallbackInfo ci) {

        // Check if this is a POI-related memory (HOME, JOB_SITE, MEETING_POINT)
        if (type == MemoryModuleType.HOME ||
            type == MemoryModuleType.JOB_SITE ||
            type == MemoryModuleType.MEETING_POINT) {

            // Extract BlockPos from Optional (if present)
            if (value.isPresent() && value.get() instanceof GlobalPos globalPos) {
                BlockPos pos = globalPos.pos();
                ChunkPos chunkPos = new ChunkPos(pos);

                MinecraftServer server = /* get from villager context */;

                if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
                    ci.cancel(); // Block memory update
                }
            }
        }
    }
}
```

**CRITICAL:** This pattern requires identifying the correct method where villagers set POI memory. The exact method name and signature must be verified in Yarn mappings for 1.21.11.

### Pattern 3: Existing Chunk Claim Check Pattern

**What:** Reuse the established chunk claim check pattern from other THC systems.

**When to use:** Whenever chunk-based validation is needed.

**Example from existing codebase:**
```java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/NaturalSpawnerMixin.java
@Inject(
    method = "isValidSpawnPostitionForType",
    at = @At("HEAD"),
    cancellable = true
)
private static void thc$blockSpawnInBaseChunks(
        ServerLevel level,
        MobCategory category,
        StructureManager structureManager,
        ChunkGenerator generator,
        MobSpawnSettings.SpawnerData spawnerData,
        BlockPos.MutableBlockPos pos,
        double squaredDistance,
        CallbackInfoReturnable<Boolean> cir) {

    // Check if this chunk is claimed
    ChunkPos chunkPos = new ChunkPos(pos);
    if (ClaimManager.INSTANCE.isClaimed(level.getServer(), chunkPos)) {
        cir.setReturnValue(false);
    }
}
```

**Pattern characteristics:**
1. HEAD injection with cancellable callback
2. Convert BlockPos to ChunkPos
3. Call `ClaimManager.INSTANCE.isClaimed(server, chunkPos)`
4. Cancel/return early if chunk is claimed

### Anti-Patterns to Avoid

- **Block-level NBT tagging:** Don't add NBT tags to beds/job sites to mark them as "non-claimable". This adds data storage overhead and doesn't work with existing POI.

- **Post-claim cleanup:** Don't let villagers claim POI and then remove the claim later. Prevention at source is cleaner and avoids villager confusion.

- **Global POI filtering:** Don't filter POI queries globally - only filter registration and claiming in claimed chunks. Villages outside claimed territory must function normally.

- **Event-driven approach:** Fabric doesn't provide POI-specific events, so don't create custom event infrastructure when mixins suffice.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Chunk position calculation | Manual chunk coordinate math | ChunkPos(BlockPos) constructor | Handles bit shifting, negative coordinates correctly |
| Server instance access | Custom server reference system | Method parameter or context lookup | Minecraft methods already provide server context |
| POI type checking | String comparison of block names | Holder&lt;PoiType&gt; or POI registry | Type-safe, handles modded POI types |
| Chunk claim checking | Custom claim data structures | ClaimManager.isClaimed() | Already tested, integrated with base system |

**Key insight:** Minecraft's village system is complex with caching, pathfinding, and multi-chunk boundaries. Intercepting at the source (registration and claiming) is simpler than trying to filter cached/remembered POI.

## Common Pitfalls

### Pitfall 1: POI Persistence After Chunk Claiming

**What goes wrong:** Player claims a chunk that has existing beds. Villagers continue to use those beds because POI data persists.

**Why it happens:** POI data is saved to disk in `.mca` files in the `poi/` directory, separate from chunk data. Blocking new POI registration doesn't remove existing POI.

**How to avoid:**
1. Implement dual interception: block registration AND block claiming
2. Villager claim blocking catches attempts to use existing POI
3. Document that existing village mechanics may take time to "decay" after claiming

**Warning signs:**
- Villagers pathfinding into claimed chunks to reach beds
- "Claimed chunk doesn't count for village" test fails
- Green particles from villagers claiming beds in claimed chunks

**Recommended approach:**
```java
// Two-layer protection:
// 1. Block NEW POI registration (PoiManagerMixin)
// 2. Block villager claiming of EXISTING POI (VillagerMixin)
```

### Pitfall 2: Server Context Access in Mixins

**What goes wrong:** `ClaimManager.isClaimed()` requires a MinecraftServer instance, but mixin injection points don't always provide it.

**Why it happens:** Not all methods in PoiManager or Villager classes have direct server references.

**How to avoid:**
1. Check method signatures for ServerLevel/MinecraftServer parameters
2. Use `@Inject` with `@Local` capture for local variables containing server
3. Cast callback info to access instance fields if needed
4. For PoiManager, the manager is associated with a ServerLevel - find access path

**Warning signs:**
- Compilation errors: "cannot find symbol: server"
- NullPointerException when calling ClaimManager.isClaimed()
- Mixin fails to apply due to incompatible method signature

**Correct pattern:**
```java
// Option 1: Method parameter provides server
@Inject(method = "someMethod")
private void thc$check(ServerLevel level, /* other params */, CallbackInfo ci) {
    MinecraftServer server = level.getServer();
    // ...
}

// Option 2: Access via instance field (requires @Accessor or @Shadow)
@Shadow
private ServerLevel level; // If PoiManager has level field

@Inject(method = "add")
private void thc$check(BlockPos pos, Holder<PoiType> type, CallbackInfo ci) {
    MinecraftServer server = this.level.getServer();
    // ...
}
```

### Pitfall 3: Villager Brain Memory Type Identification

**What goes wrong:** Attempting to block POI claiming by intercepting the wrong brain method or memory type.

**Why it happens:** Villager brain system uses multiple memory types and acquisition paths:
- `POTENTIAL_JOB_SITE` (discovered, not yet claimed)
- `HOME` (claimed bed)
- `JOB_SITE` (claimed job site)
- Tasks can update memory through different code paths

**How to avoid:**
1. Research Yarn mappings for Villager and Brain classes in 1.21.11
2. Identify the method where GlobalPos is set for HOME and JOB_SITE
3. May need multiple injection points (one for beds, one for job sites)
4. Test with game tests: place bed, spawn villager, verify no claiming

**Warning signs:**
- Mixin compiles but villagers still claim beds in claimed chunks
- Brain memory debugging shows HOME set despite mixin
- Injection point never executes (wrong method)

**Research needed:**
- Exact method signature for villager POI acquisition in 1.21.11
- Whether Brain.setMemory is the correct interception point
- Whether villager-specific task classes handle POI claiming

### Pitfall 4: Village Boundary Calculation Edge Cases

**What goes wrong:** Beds on the edge of a claimed chunk might be counted by villages in adjacent unclaimed chunks.

**Why it happens:** Villages span multiple chunks (32 blocks horizontal from center). A bed at chunk boundary might be in claimed chunk but visible to village centered in unclaimed chunk.

**How to avoid:**
This is actually CORRECT BEHAVIOR per requirements:
- Requirement: "Villagers and beds in unclaimed chunks still register to nearby villages"
- If village center is in unclaimed chunk, it should function normally
- Only beds PLACED IN claimed chunks should be blocked from registration

**Warning signs:**
- None - this is expected behavior
- Don't "fix" edge cases that match requirements

**Clarification:**
```
Unclaimed chunk (village center) | Claimed chunk (bed placed here)
----------------------------------|--------------------------------
Village POI search extends here   | Bed should NOT register as POI
Villagers can exist here          | Villagers here DON'T count
```

The chunk claim check ensures this: beds in claimed chunks don't register, regardless of nearby village locations.

### Pitfall 5: POI Manager Method Signature Variation

**What goes wrong:** Minecraft's POI manager has multiple `add` methods with different signatures. Injecting into the wrong one misses POI registration.

**Why it happens:** PoiManager likely has:
- `add(BlockPos, PoiType)` - direct addition
- `add(BlockPos, Holder<PoiType>)` - registry holder addition
- Internal helper methods

**How to avoid:**
1. Decompile or check Yarn mappings for exact method signatures
2. Use `@Inject` with precise method descriptor if multiple overloads exist
3. Test with multiple POI types (beds, job sites, bells) to ensure coverage

**Warning signs:**
- Mixin applies but beds still register
- Only some POI types are blocked (e.g., beds work but job sites blocked)
- Method injection target errors in logs

**Correct approach:**
```java
// Use method descriptor if needed to disambiguate
@Inject(
    method = "add(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;)V",
    at = @At("HEAD"),
    cancellable = true
)
```

## Code Examples

Verified patterns from research and existing codebase:

### Complete POI Registration Blocking

```java
// Source: NaturalSpawnerMixin.java pattern + POI research
package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.claim.ClaimManager;

/**
 * Prevent POI registration in claimed chunks.
 *
 * Beds, job sites, and bells placed in claimed chunks should not
 * register to the POI system, preventing villagers from counting
 * them towards village mechanics.
 */
@Mixin(PoiManager.class)
public class PoiManagerMixin {

    // Shadow field to access server context (may need adjustment based on actual PoiManager structure)
    // Alternative: capture from method parameter if available

    @Inject(
        method = "add",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockPoiInClaimedChunks(
            BlockPos pos,
            Holder<PoiType> poiType,
            CallbackInfo ci) {

        // TODO: Obtain MinecraftServer instance
        // This requires research into PoiManager's structure
        // Options:
        //   1. PoiManager is created by ServerLevel, might have level field
        //   2. Method parameters might include ServerLevel
        //   3. ThreadLocal or other context access

        MinecraftServer server = null; // PLACEHOLDER - needs implementation

        if (server != null) {
            ChunkPos chunkPos = new ChunkPos(pos);
            if (ClaimManager.INSTANCE.isClaimed(server, chunkPos)) {
                ci.cancel(); // Block POI registration
            }
        }
    }
}
```

### Chunk Claim Check (Existing Pattern)

```java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/NaturalSpawnerMixin.java
// Demonstrates the established pattern for chunk claiming checks

@Inject(
    method = "isValidSpawnPostitionForType",
    at = @At("HEAD"),
    cancellable = true
)
private static void thc$blockSpawnInBaseChunks(
        ServerLevel level,
        MobCategory category,
        StructureManager structureManager,
        ChunkGenerator generator,
        MobSpawnSettings.SpawnerData spawnerData,
        BlockPos.MutableBlockPos pos,
        double squaredDistance,
        CallbackInfoReturnable<Boolean> cir) {

    // Standard pattern:
    // 1. Convert BlockPos to ChunkPos
    ChunkPos chunkPos = new ChunkPos(pos);

    // 2. Check claim status
    if (ClaimManager.INSTANCE.isClaimed(level.getServer(), chunkPos)) {
        // 3. Cancel/return early
        cir.setReturnValue(false);
    }
}
```

### Villager POI Claiming (Conceptual - Requires Method Research)

```java
// Source: Village mechanics research + brain memory system
// NOTE: This is CONCEPTUAL - exact method needs verification in Yarn mappings

package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.claim.ClaimManager;

import java.util.Optional;

/**
 * Prevent villagers from claiming POI in claimed chunks.
 *
 * Even if POI exists (from before chunk was claimed), villagers
 * should not be able to claim beds or job sites in claimed chunks.
 */
@Mixin(Villager.class)
public class VillagerPoiClaimMixin {

    // PLACEHOLDER: Exact method needs research
    // Possible targets:
    //   - Brain.setMemory
    //   - Villager-specific acquisition task
    //   - AcquirePoiTask or similar

    @Inject(
        method = "METHOD_NAME_TBD", // Research needed
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockPoiClaimInClaimedChunks(
            /* Parameters TBD based on actual method */
            CallbackInfo ci) {

        // Conceptual logic:
        // 1. Identify if this is POI-related memory (HOME, JOB_SITE)
        // 2. Extract BlockPos from memory value
        // 3. Check if chunk is claimed
        // 4. Cancel if claimed

        // Implementation requires exact method signature from Yarn 1.21.11
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual POI file editing | Runtime POI filtering via mixin | Modding ecosystem | Dynamic control without save file manipulation |
| NBT tags on blocks | Chunk-based POI validation | Mod-specific (2026) | Cleaner, matches existing claim system |
| Post-claim villager manipulation | Prevention at POI registration | Mod-specific | Fewer edge cases, cleaner behavior |

**Deprecated/outdated:**
- **NBT tags for "player bed" vs "villager bed":** Suggested in Minecraft Feedback but never implemented. Chunk-based approach more flexible.
- **Manual POI file deletion:** Pre-modding approach. Runtime filtering preferred.

## Open Questions

Things that couldn't be fully resolved:

1. **PoiManager server context access**
   - What we know: PoiManager is created per ServerLevel, manages POI data
   - What's unclear: Whether PoiManager stores ServerLevel reference, or if we need to capture it from method context
   - Recommendation: Decompile PoiManager class to identify server access path. May need @Shadow field or @Local capture.

2. **Exact villager POI acquisition method**
   - What we know: Villagers use Brain with MemoryModuleType.HOME and .JOB_SITE
   - What's unclear: Which method is the correct interception point for villager claiming
   - Recommendation: Check Yarn mappings for `AcquirePoi` task or `Brain.setMemory` calls in Villager class. May need multiple injection points for different POI types.

3. **POI cleanup for existing villages**
   - What we know: POI data persists in `.mca` files even after chunk is claimed
   - What's unclear: Whether to implement cleanup of existing POI, or rely on dual-layer blocking (registration + claiming)
   - Recommendation: Start with dual-layer blocking only. Cleanup adds complexity and may not be necessary if claiming is blocked.

4. **Village boundary edge cases**
   - What we know: Villages span 32 blocks from center, can overlap multiple chunks
   - What's unclear: Whether villages centered in unclaimed chunks should be able to "see" beds in adjacent claimed chunks
   - Recommendation: Current approach (block POI registration in claimed chunks) handles this correctly - beds in claimed chunks never register, regardless of nearby village locations.

5. **Method signatures for 1.21.11**
   - What we know: General POI and Brain patterns from research
   - What's unclear: Exact method names and signatures in Minecraft 1.21.11 with Yarn mappings
   - Recommendation: Consult Yarn mappings documentation at maven.fabricmc.net or use IDE with Fabric development setup to verify exact signatures before implementation.

## Sources

### Primary (HIGH confidence)
- Existing THC codebase patterns:
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/NaturalSpawnerMixin.java` - Chunk claim check pattern
  - `/mnt/c/home/code/thc/src/main/kotlin/thc/claim/ClaimManager.kt` - Chunk claim API
- [Minecraft Wiki - Village mechanics](https://minecraft.wiki/w/Village_mechanics) - Village formation, POI claiming mechanics
- [Minecraft Wiki - Point of Interest](https://minecraft.wiki/w/Point_of_Interest) - POI data structure, registration mechanics

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Mob AI](https://minecraft.wiki/w/Mob_AI) - Brain system, memory modules, tasks
- [Villagers.md Gist](https://gist.github.com/orlp/db1ca6dbb82727c4a939c95694a52b81) - Villager POI claiming mechanics (48 block radius, pathfinding requirements)
- [Village Mechanic - Bedrock Wiki](https://wiki.bedrock.dev/entities/village-mechanic) - POI discovery (16h/4v range), communication patterns
- [Fabric Yarn API - BedBlock](https://maven.fabricmc.net/docs/yarn-1.21.5+build.1/net/minecraft/block/BedBlock.html) - Bed block structure
- [Fabric Yarn API - Brain package](https://maven.fabricmc.net/docs/yarn-1.18-rc4+build.1/net/minecraft/entity/ai/brain/package-summary.html) - Brain memory module system

### Tertiary (LOW confidence - needs verification)
- [Villager Memory Tweaks mod](https://www.curseforge.com/minecraft/mc-mods/villager-memory-tweaks-vmt) - Example mod modifying villager POI memory
- [NBT tag for POI - Minecraft Feedback](https://feedback.minecraft.net/hc/en-us/community/posts/360043636392-NBT-tag-for-POI-to-remove-from-the-POI-detection-for-villagers) - Community suggestions (not implemented)
- [SpigotMC - 1.14 Villager Memory](https://www.spigotmc.org/threads/1-14-villager-memory.388618/) - Discussion of villager memory mechanics

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All tools already in use, no new dependencies
- Architecture patterns (chunk claim check): HIGH - Existing pattern verified in codebase
- Architecture patterns (POI interception): MEDIUM - Pattern is sound but exact method signatures need verification
- Villager brain interception: LOW - Conceptual approach valid but requires method research
- Server context access: LOW - Multiple possible approaches, needs codebase investigation

**Research date:** 2026-01-28
**Valid until:** Minecraft 1.21.x (method signatures stable within minor versions, but verify for 1.21.11 specifically)

**Critical unknowns for planner:**
1. PoiManager's server context access path - requires decompilation or Yarn mapping review
2. Exact villager POI claiming method for injection - requires Yarn mapping review
3. Whether PoiManager.add has multiple overloads requiring specific method descriptors

**Notes for planner:**
1. This phase requires more technical research during implementation than typical phases
2. Consider splitting into two tasks: (1) POI registration blocking, (2) Villager claiming blocking
3. Game tests are CRITICAL - this is hard to verify manually
4. May need to read Yarn mappings or decompiled code during planning to resolve unknowns
5. The dual-layer approach (POI registration + villager claiming) provides defense-in-depth against edge cases
