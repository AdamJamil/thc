# Phase 41: NBT Spawn Origin Tagging - Research

**Researched:** 2026-01-24
**Domain:** Fabric Attachment API, Entity NBT tagging, Minecraft spawn lifecycle
**Confidence:** HIGH

## Summary

NBT spawn origin tagging uses Fabric's Attachment API to attach persistent metadata to entities at spawn time. The established pattern in THC already demonstrates both persistent attachments (MAX_HEALTH with Codec.DOUBLE) and non-persistent attachments (MOB_THREAT as session-only Map). For Phase 41, we need two attachments: SPAWN_REGION (which region the mob spawned in) and SPAWN_COUNTED (whether it counts toward regional caps).

The spawn lifecycle provides a clear integration point: `Mob.finalizeSpawn()` is called for all natural spawns and receives an `EntitySpawnReason` parameter, allowing us to filter for NATURAL spawns. Region detection uses heightmap comparison (`world.getTopY(Heightmap.Type.MOTION_BLOCKING)`) combined with Y-level thresholds, as decided in the phase context.

**Key finding:** Fabric's Attachment API with persistent Codecs provides NBT storage automatically. String attachments (via `Codec.STRING`) are the simplest implementation, while byte encoding (via `Codec.BYTE` with enum ordinal mapping) offers minimal storage overhead. The roadmap suggests byte encoding for efficiency.

**Primary recommendation:** Use `Mob.finalizeSpawn()` mixin to tag entities, attach via THCAttachments with persistent codecs, detect region via heightmap comparison, and filter to NATURAL + MONSTER spawns only.

## Standard Stack

The established libraries/tools for entity NBT tagging in Fabric:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.119.2+1.21.5 | Attachment API for entity data | Official Fabric data attachment system since 1.20+ |
| Mojang Codec | Built-in (DFU) | Serialization via NBT/JSON | Minecraft's standard serialization framework |
| Mixin | 0.8+ (via Fabric) | Method injection for spawn hooks | Standard Fabric/Forge modding approach |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| NbtOps | Built-in | Codec → NBT conversion | When using Codec-based persistence |
| StreamCodec | Built-in (1.21+) | Network synchronization | Only if syncing to clients (not needed for Phase 41) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Attachment API | Custom NBT writing | More control, much more code, no auto-persistence |
| Codec persistence | Manual CompoundTag | Fragile, error-prone, deprecated pattern |
| Mixin | Fabric Events | ServerEntityEvents.ENTITY_LOAD fires after spawn (too late for filtering) |

**Installation:**
```gradle
// Already in THC build.gradle - no new dependencies
fabric-api:0.119.2+1.21.5
```

## Architecture Patterns

### Recommended Attachment Pattern (from THCAttachments.java)

**Persistent String Attachment:**
```java
// Source: THCAttachments.java (existing pattern)
public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "player_class"),
    builder -> {
        builder.initializer(() -> null);
        builder.persistent(Codec.STRING);
        builder.copyOnDeath();
    }
);
```

**Non-persistent Primitive Attachment:**
```java
// Source: THCAttachments.java (existing pattern)
public static final AttachmentType<Boolean> BUCKLER_BROKEN = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "buckler_broken"),
    builder -> builder.initializer(() -> Boolean.FALSE)
);
```

### Pattern 1: Entity Spawn Tagging via finalizeSpawn

**What:** Inject into `Mob.finalizeSpawn()` to tag entities at spawn time
**When to use:** Need spawn-time metadata with access to EntitySpawnReason
**Example:**
```java
// Source: Adapted from https://gist.github.com/TelepathicGrunt/3784f8a8b317bac11039474012de5fb4
// and existing SpawnReplacementMixin.java pattern
@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void thc$tagSpawnOrigin(
            ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        // Filter to NATURAL spawns only
        if (reason != EntitySpawnReason.NATURAL &&
            reason != EntitySpawnReason.CHUNK_GENERATION) {
            return;
        }

        Mob self = (Mob) (Object) this;

        // Tag with region
        String region = detectRegion((ServerLevel) level.getLevel(), self.blockPosition());
        self.setAttached(THCAttachments.SPAWN_REGION, region);

        // Tag if counted (NATURAL + MONSTER category)
        boolean counted = self.getType().getCategory() == MobCategory.MONSTER;
        self.setAttached(THCAttachments.SPAWN_COUNTED, counted);
    }
}
```

### Pattern 2: Region Detection via Heightmap

**What:** Determine spawn region from heightmap + Y-level thresholds
**When to use:** Need to classify spawn location (surface/cave/depth)
**Example:**
```java
// Source: Phase 41 CONTEXT.md decision + Minecraft heightmap system
// Reference: https://minecraft.wiki/w/Heightmap
private static String detectRegion(ServerLevel level, BlockPos pos) {
    int y = pos.getY();

    // Lower cave: below Y=0 (sea level)
    if (y < 0) {
        return "OW_LOWER_CAVE";
    }

    // Surface: Y >= heightmap at X/Z (handles overhangs correctly)
    int surfaceY = level.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
    if (y >= surfaceY) {
        return "OW_SURFACE";
    }

    // Upper cave: Y >= 0 but below heightmap
    return "OW_UPPER_CAVE";
}
```

**MOTION_BLOCKING heightmap:** Stores Y-level of highest block that blocks motion (has collision box) or contains fluid. Excludes leaves. Server-side only. This correctly classifies mobs under cliffs as cave spawns (not surface).

### Pattern 3: Byte-Encoded Enum Attachment (Optional Optimization)

**What:** Use byte ordinal instead of string for minimal NBT bloat
**When to use:** Large-scale tagging where storage efficiency matters
**Example:**
```java
// Source: https://docs.minecraftforge.net/en/latest/datastorage/codecs/
// Enum ordinal encoding concept from NetworkBuffer.Enum pattern
public enum SpawnRegion {
    OW_SURFACE,        // ordinal = 0
    OW_UPPER_CAVE,     // ordinal = 1
    OW_LOWER_CAVE;     // ordinal = 2

    // Helper for byte codec
    public static final Codec<SpawnRegion> CODEC =
        Codec.BYTE.xmap(
            b -> values()[b],
            region -> (byte) region.ordinal()
        );
}

// Attachment with byte codec
public static final AttachmentType<SpawnRegion> SPAWN_REGION = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "spawn_region"),
    builder -> {
        builder.initializer(() -> null);
        builder.persistent(SpawnRegion.CODEC);
    }
);
```

**Storage comparison:**
- String "OW_UPPER_CAVE": ~15 bytes (TAG_String overhead + 13 chars)
- Byte ordinal: ~3 bytes (TAG_Byte overhead + 1 byte value)
- **Savings:** ~12 bytes per entity (80% reduction)

### Anti-Patterns to Avoid

- **Manual NBT writing:** Don't bypass Attachment API to write CompoundTags directly. Attachment API handles persistence, versioning, and edge cases automatically. (Source: Fabric docs emphasize using modern Codecs over manual NBT manipulation)

- **ServerEntityEvents.ENTITY_LOAD for spawn tagging:** This event fires when entities are loaded into the world (after spawn complete), not during spawn. By then, spawn reason is lost and entity is already in world. Use `finalizeSpawn` instead. (Source: https://docproject.github.io/fabricmc_fabric/)

- **canSeeSky for surface detection:** The phase context decided on heightmap comparison instead. `canSeeSky(pos)` checks if sky is directly visible, which fails under overhangs. Heightmap comparison (`pos.getY() >= world.getTopY(...)`) correctly classifies mobs under cliffs as cave spawns.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Entity NBT persistence | Custom writeAdditionalSaveData override | Fabric Attachment API with persistent(Codec) | Auto-handles persistence, versioning, copyOnDeath, and edge cases like mob conversion |
| Enum serialization | Manual ordinal→byte conversion | Codec.BYTE.xmap() or StringRepresentable | Handles invalid values, provides compile-time safety, integrates with DFU system |
| Region detection | Ray-tracing or light level checks | Heightmap.Type.MOTION_BLOCKING | Pre-computed by Minecraft during chunk generation, O(1) lookup, handles complex terrain |
| Spawn filtering | Custom spawn event system | EntitySpawnReason parameter in finalizeSpawn | Already provided by Minecraft, distinguishes NATURAL/SPAWNER/STRUCTURE/etc. |

**Key insight:** Attachment API was introduced in Fabric 0.95+ (1.20.4+) specifically to replace manual NBT handling patterns. It's not a convenience wrapper—it's the recommended approach. Custom NBT writing bypasses crucial systems like attachment copying during entity conversion (zombie→drowned) or player respawn.

## Common Pitfalls

### Pitfall 1: Tagging All Entity Types Instead of Filtering

**What goes wrong:** Attachment applied to every entity (items, projectiles, minecarts), not just mobs. NBT bloat on non-mob entities, potential crashes if attachment code assumes Mob-specific behavior.

**Why it happens:** `@Mixin(Entity.class)` is broader than `@Mixin(Mob.class)`. Or filtering by MobCategory happens after attachment is set.

**How to avoid:**
1. Use `@Mixin(Mob.class)` not `@Mixin(Entity.class)`
2. Check `MobCategory.MONSTER` before setting SPAWN_COUNTED
3. Filter spawn reason FIRST (return early if not NATURAL)

**Warning signs:** NBT editor shows `spawnSystem.region` on items/arrows, or console errors when accessing Mob-specific methods on Entity instances.

### Pitfall 2: Attachment Not Persisting Across Restarts

**What goes wrong:** Spawn region tag present during session but disappears after server restart.

**Why it happens:** Forgot to call `.persistent(Codec)` in AttachmentRegistry builder. Without this, attachment is session-only (like MOB_THREAT map in THC).

**How to avoid:** Always use builder pattern:
```java
builder.persistent(Codec.STRING);  // For String attachments
builder.persistent(Codec.BOOL);    // For Boolean attachments
```

**Warning signs:** Tags work in same session but NBT viewer shows missing tags after `/reload` or server restart. No error messages—silent data loss.

### Pitfall 3: Heightmap Type Mismatch

**What goes wrong:** Using wrong heightmap type causes incorrect region classification. For example, `WORLD_SURFACE` includes leaves, so mobs in jungle canopy count as surface.

**Why it happens:** Multiple heightmap types available: WORLD_SURFACE, MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES, OCEAN_FLOOR. Each has different block filtering.

**How to avoid:** Use `Heightmap.Type.MOTION_BLOCKING` as decided in phase context. This type:
- Includes blocks with collision boxes
- Excludes leaves (mobs in trees → cave spawns)
- Includes fluids (waterlogged blocks)
- Server-side only (available in ServerLevel)

**Warning signs:** Mobs spawning in trees tagged as OW_SURFACE instead of OW_UPPER_CAVE. Surface detection works in plains but fails in forests.

### Pitfall 4: EntitySpawnReason Coverage Gaps

**What goes wrong:** Assuming NATURAL is the only spawn reason to tag. Chunk generation spawns (`CHUNK_GENERATION`) are missed, leading to inconsistent tagging.

**Why it happens:** Spawn reasons are broader than expected. NATURAL = ongoing spawning, CHUNK_GENERATION = initial population, but both are "natural" from gameplay perspective.

**How to avoid:** Check for both reasons:
```java
if (reason != EntitySpawnReason.NATURAL &&
    reason != EntitySpawnReason.CHUNK_GENERATION) {
    return;  // Skip SPAWNER, STRUCTURE, BREEDING, etc.
}
```

**Warning signs:** Newly generated chunks have untagged mobs, but mobs spawning later (after player activity) are tagged correctly.

### Pitfall 5: Dimension Filtering Too Late

**What goes wrong:** Nether/End mobs get tagged with OW_SURFACE/etc., or dimension check happens after expensive heightmap lookup.

**Why it happens:** Region detection logic doesn't check dimension first. Heightmap behavior in Nether/End may differ or be undefined.

**How to avoid:** Phase context says "Claude's discretion" on Nether/End. Options:
1. **Skip tagging:** `if (level.dimension() != Level.OVERWORLD) return;`
2. **Tag with OTHER:** Add NETHER/END/OTHER region values (unified code path)

Choose based on code simplicity. Caps are Overworld-only (per Phase 43), so Nether/End tags have no functional impact—choose simpler code.

**Warning signs:** Console warnings about heightmap access in Nether, or tags like "OW_SURFACE" on Nether mobs.

## Code Examples

Verified patterns from official sources and existing THC codebase:

### Complete Attachment Registration
```java
// Source: THCAttachments.java (existing pattern)
public final class THCAttachments {
    // String attachment with persistence
    public static final AttachmentType<String> SPAWN_REGION = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("thc", "spawn_region"),
        builder -> {
            builder.initializer(() -> null);
            builder.persistent(Codec.STRING);
        }
    );

    // Boolean attachment with persistence
    public static final AttachmentType<Boolean> SPAWN_COUNTED = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("thc", "spawn_counted"),
        builder -> {
            builder.initializer(() -> Boolean.FALSE);
            builder.persistent(Codec.BOOL);
        }
    );

    public static void init() {
        // Ensures static initialization has run
    }
}
```

### Heightmap-Based Region Detection
```java
// Source: Phase 41 CONTEXT.md + https://minecraft.wiki/w/Heightmap
private static String detectRegion(ServerLevel level, BlockPos pos) {
    // Overworld-only (phase context decision)
    if (level.dimension() != Level.OVERWORLD) {
        return null;  // Or "OTHER" / "NETHER" / "END" if desired
    }

    int y = pos.getY();

    // Boundary 1: Lower cave (below sea level)
    if (y < 0) {
        return "OW_LOWER_CAVE";
    }

    // Boundary 2: Surface (Y >= heightmap)
    // MOTION_BLOCKING: highest block that blocks motion or contains fluid
    int surfaceY = level.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
    if (y >= surfaceY) {
        return "OW_SURFACE";
    }

    // Upper cave: Y >= 0 but below heightmap
    return "OW_UPPER_CAVE";
}
```

### MobCategory Filtering
```java
// Source: MobCategory enum (vanilla Minecraft)
// Check if mob is MONSTER category
Mob self = (Mob) (Object) this;
MobCategory category = self.getType().getCategory();

boolean isMonster = (category == MobCategory.MONSTER);

// MobCategory values:
// MONSTER - hostile mobs (zombies, skeletons, creepers, etc.)
// CREATURE - passive mobs (cows, pigs, sheep, etc.)
// AMBIENT - bats
// WATER_CREATURE - squid, dolphins
// etc.
```

### Complete finalizeSpawn Mixin
```java
// Source: Adapted from TelepathicGrunt guide + SpawnReplacementMixin.java
@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void thc$tagSpawnOrigin(
            ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        // Filter 1: Only natural spawns (not spawners, structures, breeding, etc.)
        if (reason != EntitySpawnReason.NATURAL &&
            reason != EntitySpawnReason.CHUNK_GENERATION) {
            return;
        }

        Mob self = (Mob) (Object) this;
        ServerLevel serverLevel = (ServerLevel) level.getLevel();

        // Filter 2: Overworld only (or handle other dimensions)
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }

        // Tag: SPAWN_REGION
        String region = detectRegion(serverLevel, self.blockPosition());
        if (region != null) {
            self.setAttached(THCAttachments.SPAWN_REGION, region);
        }

        // Tag: SPAWN_COUNTED (only MONSTER category counts)
        boolean isMonster = self.getType().getCategory() == MobCategory.MONSTER;
        self.setAttached(THCAttachments.SPAWN_COUNTED, isMonster);
    }

    @Unique
    private static String detectRegion(ServerLevel level, BlockPos pos) {
        int y = pos.getY();

        if (y < 0) {
            return "OW_LOWER_CAVE";
        }

        int surfaceY = level.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (y >= surfaceY) {
            return "OW_SURFACE";
        }

        return "OW_UPPER_CAVE";
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual NBT via writeAdditionalSaveData | Fabric Attachment API with Codec | 1.20.4+ (Fabric API 0.95.0) | Simpler code, automatic persistence, handles edge cases |
| canSeeSky for surface detection | Heightmap.Type.MOTION_BLOCKING | Always available, but pattern emerged | Correct overhang handling, O(1) lookup |
| ServerEntityEvents.ENTITY_LOAD for spawn hooks | Mixin to finalizeSpawn | Fabric Events API 1.16+, but spawn-specific | Access to EntitySpawnReason, pre-world-add timing |
| String-only enum serialization | Codec.BYTE.xmap for ordinal encoding | DFU Codecs (1.13+), optimized pattern | 80% storage reduction for enums |

**Deprecated/outdated:**
- **Entity.getEntityData() / getPersistentData():** Forge-specific API. Fabric uses Attachment API instead.
- **IForgeEntity.getPersistentData():** Forge-only. Not available in Fabric.
- **Custom NBT via CompoundTag:** Still works but not recommended. Attachment API provides type safety, versioning, and auto-persistence.

## Open Questions

Things that couldn't be fully resolved:

1. **Byte encoding vs. String attachment**
   - What we know: Roadmap says "byte-encoded region attachment to minimize save bloat." Byte encoding saves ~12 bytes per entity (80% reduction). String is simpler to implement and debug.
   - What's unclear: Whether storage savings matter at expected entity counts. Typical server: ~500 loaded entities, ~6 KB saved. Large server: ~5000 entities, ~60 KB saved. Is this worth the complexity?
   - Recommendation: **Start with String (Codec.STRING) for simplicity.** If profiling shows NBT bloat issues, migrate to byte encoding. Migration is trivial: change Codec and add xmap function. Mark this as "premature optimization" until proven necessary.

2. **Nether/End tagging decision**
   - What we know: Phase 43 (cap partitioning) is Overworld-only. Nether/End tags have no functional impact. Context says "Claude's discretion."
   - What's unclear: Whether unified code path (tag everything) or early-return (skip other dimensions) is cleaner.
   - Recommendation: **Skip Nether/End tagging** (early return if dimension != OVERWORLD). Simpler code, no wasted storage, avoids potential heightmap edge cases in other dimensions. If future phases need dimension tracking, add then.

3. **CHUNK_GENERATION vs. NATURAL spawn reason**
   - What we know: Both are "natural" spawns from gameplay perspective. CHUNK_GENERATION = initial chunk population, NATURAL = ongoing spawning.
   - What's unclear: Whether CHUNK_GENERATION mobs should count toward regional caps (Phase 43).
   - Recommendation: **Tag both equally.** They're both natural environmental spawns. If Phase 43 reveals cap balancing issues, can filter by spawn reason at that point. Better to have the data and not need it than vice versa.

## Sources

### Primary (HIGH confidence)
- [Fabric Data Attachments Documentation](https://docs.fabricmc.net/develop/data-attachments) - Official Fabric docs on Attachment API
- [Fabric AttachmentType API (0.119.2+1.21.5)](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/attachment/v1/AttachmentType.html) - API reference for persistent(), copyOnDeath()
- Existing THCAttachments.java - Established patterns in codebase
- Existing SpawnReplacementMixin.java - canSeeSky pattern for spawn detection
- [Minecraft Wiki - Heightmap](https://minecraft.wiki/w/Heightmap) - MOTION_BLOCKING heightmap type specification
- Phase 41 CONTEXT.md - User decisions on region boundaries and heightmap usage

### Secondary (MEDIUM confidence)
- [Minecraft Heightmap Types Guide](https://misode.github.io/guides/heightmap-types/) - Visual explanation of heightmap differences
- [TelepathicGrunt Mixin Guide](https://gist.github.com/TelepathicGrunt/3784f8a8b317bac11039474012de5fb4) - finalizeSpawn mixin example
- [Fabric Codecs Documentation](https://docs.fabricmc.net/develop/codecs) - Codec fundamentals
- [Forge Codecs Documentation](https://docs.minecraftforge.net/en/latest/datastorage/codecs/) - Enum serialization patterns (applicable to Fabric)

### Tertiary (LOW confidence - flagged for validation)
- [MobFilter Mod](https://github.com/pcal43/mob-filter) - Community example of MobCategory filtering (not official)
- [Fabric Wiki - Events Index](https://wiki.fabricmc.net/tutorial:event_index) - Community wiki, may be outdated
- WebSearch: "NetworkBuffer.Enum ordinal encoding" - Concept verified but implementation details need testing

## Metadata

**Confidence breakdown:**
- Attachment API usage: HIGH - Existing THCAttachments.java validates pattern, official Fabric docs confirm
- Heightmap-based region detection: HIGH - Phase context decision + official Minecraft wiki spec
- finalizeSpawn mixin: HIGH - Existing SpawnReplacementMixin.java uses similar pattern (different injection point but same lifecycle)
- Byte encoding optimization: MEDIUM - Concept verified via Codec docs, but marked as "premature optimization" until proven necessary
- EntitySpawnReason filtering: MEDIUM - Official enum values confirmed, but CHUNK_GENERATION handling is judgment call

**Research date:** 2026-01-24
**Valid until:** Minecraft 1.21.x minor versions (Attachment API stable since 1.20.4, heightmap types unchanged since 1.18)
