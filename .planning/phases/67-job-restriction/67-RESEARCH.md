# Phase 67: Job Restriction - Research

**Researched:** 2026-01-31
**Domain:** Minecraft villager profession restriction
**Confidence:** HIGH

## Summary

Phase 67 implements a profession restriction layer for villagers, limiting them to mason, librarian, butcher, and cartographer only. The implementation uses two complementary approaches: intercepting profession assignment at `Villager.setVillagerData()` to block disallowed professions at the data layer, and blocking POI registration for disallowed job blocks to prevent profession acquisition at the world layer.

The CONTEXT.md decisions lock in the `setVillagerData` interception approach, which provides defense-in-depth when combined with POI blocking. This is a restriction-only phase - no new capabilities, just blocking disallowed states. All scenarios are covered: natural spawns, job block acquisition, zombie villager cures, and NBT edits.

**Primary recommendation:** Create a single `VillagerProfessionMixin.java` that intercepts `setVillagerData()` with an allowed profession check, and extend existing `ServerLevelPoiMixin.java` to block POI registration for disallowed job blocks.

## Standard Stack

The established patterns for this domain:

### Core

| Class | Package | Purpose | Why Standard |
|-------|---------|---------|--------------|
| `Villager` | `net.minecraft.world.entity.npc.villager` | Main villager entity | Has `setVillagerData(VillagerData)` - single chokepoint for profession changes |
| `VillagerData` | `net.minecraft.world.entity.npc.villager` | Immutable record holding profession/type/level | Has `withProfession(Holder<VillagerProfession>)` for creating modified copies |
| `VillagerProfession` | `net.minecraft.world.entity.npc.villager` | Static ResourceKeys for all professions | MASON, LIBRARIAN, BUTCHER, CARTOGRAPHER, NONE, NITWIT |
| `PoiTypes` | `net.minecraft.world.entity.ai.village.poi` | POI registry for job blocks | Maps blocks to profession POIs |

### Supporting

| Class | Package | Purpose | When to Use |
|-------|---------|---------|-------------|
| `ServerLevelPoiMixin` | `thc.mixin` (existing) | Blocks POI registration | Extend for disallowed job block filtering |
| `BuiltInRegistries` | `net.minecraft.core.registries` | Registry access | Getting VillagerProfession holders |
| `ResourceKey` | `net.minecraft.resources` | Type-safe registry keys | Comparing profession identities |
| `Holder` | `net.minecraft.core` | Registry entry wrapper | VillagerData.profession() returns Holder |

### Profession to Job Block Mapping

| Profession | Job Block | POI Type | Status |
|------------|-----------|----------|--------|
| MASON | Stonecutter | MASON | ALLOWED |
| LIBRARIAN | Lectern | LIBRARIAN | ALLOWED |
| BUTCHER | Smoker | BUTCHER | ALLOWED |
| CARTOGRAPHER | Cartography Table | CARTOGRAPHER | ALLOWED |
| ARMORER | Blast Furnace | ARMORER | BLOCKED |
| CLERIC | Brewing Stand | CLERIC | BLOCKED |
| FARMER | Composter | FARMER | BLOCKED |
| FISHERMAN | Barrel | FISHERMAN | BLOCKED |
| FLETCHER | Fletching Table | FLETCHER | BLOCKED |
| LEATHERWORKER | Cauldron | LEATHERWORKER | BLOCKED |
| SHEPHERD | Loom | SHEPHERD | BLOCKED |
| TOOLSMITH | Smithing Table | TOOLSMITH | BLOCKED |
| WEAPONSMITH | Grindstone | WEAPONSMITH | BLOCKED |

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/thc/
  mixin/
    VillagerProfessionMixin.java    # NEW: Profession restriction at setVillagerData
    ServerLevelPoiMixin.java        # EXTEND: Add job block POI filtering
  villager/
    AllowedProfessions.java         # NEW: Constants + validation helpers
```

### Pattern 1: Profession Restriction via setVillagerData Mixin

**What:** Intercept all profession changes at the VillagerData setter
**When to use:** Any profession change must pass through this chokepoint

```java
// Source: THC v2.8 research, verified against MC 1.21.11 decompiled Villager.class
@Mixin(Villager.class)
public abstract class VillagerProfessionMixin {

    @Shadow public abstract VillagerData getVillagerData();

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void thc$restrictProfession(VillagerData newData, CallbackInfo ci) {
        Holder<VillagerProfession> newProf = newData.profession();

        // Extract profession key for comparison
        ResourceKey<VillagerProfession> profKey = newProf.unwrapKey().orElse(null);

        if (!AllowedProfessions.isAllowed(profKey)) {
            // For disallowed profession, force to NONE instead
            VillagerData current = this.getVillagerData();
            VillagerData fixed = current.withProfession(AllowedProfessions.getNoneHolder());

            // Set the fixed data and cancel original call
            ((Villager)(Object)this).setVillagerData(fixed);
            ci.cancel();
        }
    }
}
```

**Why this works:**
- All profession changes flow through `setVillagerData()` - AI job acquisition, NBT loading, commands, zombie cure
- Intercepting at HEAD allows modification before any side effects
- Setting to NONE instead of simply canceling prevents stuck state

### Pattern 2: Job Block POI Blocking via ServerLevelPoiMixin Extension

**What:** Extend existing POI blocking to filter out disallowed job blocks
**When to use:** Any block that grants a disallowed profession

```java
// Source: THC v2.6 ServerLevelPoiMixin, extended for profession filtering
@Mixin(ServerLevel.class)
public class ServerLevelPoiMixin {

    @Inject(
        method = "updatePOIOnBlockStateChange",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockPoiInClaimedChunks(
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            CallbackInfo ci) {

        ServerLevel self = (ServerLevel) (Object) this;
        ChunkPos chunkPos = new ChunkPos(pos);

        // Existing: block all POI in claimed chunks
        if (ClaimManager.INSTANCE.isClaimed(self.getServer(), chunkPos)) {
            ci.cancel();
            return;
        }

        // NEW: block disallowed job site POI everywhere
        Block newBlock = newState.getBlock();
        if (AllowedProfessions.isDisallowedJobBlock(newBlock)) {
            ci.cancel();
        }
    }
}
```

### Pattern 3: AllowedProfessions Helper Class

**What:** Centralized constants and validation for allowed professions
**When to use:** Any code that needs to check profession validity

```java
// Source: THC architectural pattern for shared constants
public final class AllowedProfessions {

    private static final Set<ResourceKey<VillagerProfession>> ALLOWED = Set.of(
        VillagerProfession.MASON,
        VillagerProfession.LIBRARIAN,
        VillagerProfession.BUTCHER,
        VillagerProfession.CARTOGRAPHER,
        VillagerProfession.NONE,
        VillagerProfession.NITWIT
    );

    private static final Set<Block> DISALLOWED_JOB_BLOCKS = Set.of(
        Blocks.COMPOSTER,       // farmer
        Blocks.BREWING_STAND,   // cleric
        Blocks.SMITHING_TABLE,  // toolsmith
        Blocks.BLAST_FURNACE,   // armorer
        Blocks.FLETCHING_TABLE, // fletcher
        Blocks.CAULDRON,        // leatherworker (all cauldron variants)
        Blocks.WATER_CAULDRON,
        Blocks.LAVA_CAULDRON,
        Blocks.POWDER_SNOW_CAULDRON,
        Blocks.BARREL,          // fisherman
        Blocks.GRINDSTONE,      // weaponsmith
        Blocks.LOOM             // shepherd
    );

    public static boolean isAllowed(ResourceKey<VillagerProfession> profKey) {
        if (profKey == null) return true; // null = NONE, allowed
        return ALLOWED.contains(profKey);
    }

    public static boolean isDisallowedJobBlock(Block block) {
        return DISALLOWED_JOB_BLOCKS.contains(block);
    }

    public static Holder<VillagerProfession> getNoneHolder() {
        return BuiltInRegistries.VILLAGER_PROFESSION
            .getHolderOrThrow(VillagerProfession.NONE);
    }
}
```

### Anti-Patterns to Avoid

- **Modifying Brain AI directly:** The profession assignment happens through Brain behaviors, but intercepting at that level is fragile. Use `setVillagerData()` as the chokepoint instead.

- **Tick-based scanning:** Don't scan all villagers periodically to reset professions. The `setVillagerData` interception catches all changes proactively.

- **Separate zombie villager handling:** The CONTEXT.md says zombie cure completes normally, profession assignment is rejected. This is already handled by `setVillagerData` interception - no separate ZombieVillager mixin needed.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Profession validation | Custom registry lookup | `VillagerProfession` static ResourceKeys | Already defined as `VillagerProfession.MASON` etc. |
| Job block detection | Manual block->profession mapping | `PoiTypes` registry | Minecraft maintains authoritative POI type mappings |
| NONE profession Holder | Hardcoded reference | `BuiltInRegistries.VILLAGER_PROFESSION.getHolderOrThrow(NONE)` | Type-safe registry access |
| Profession string IDs | String comparison | `ResourceKey.equals()` | Type-safe, registry-aware comparison |

**Key insight:** MC 1.21.11 uses Holder/ResourceKey patterns extensively. Never compare professions by string ID - use the ResourceKey directly.

## Common Pitfalls

### Pitfall 1: Zombie Cure Bypass

**What goes wrong:** Zombie villagers that haven't been traded with can change profession when cured, bypassing the POI blocking.

**Why it happens:** When a ZombieVillager is cured via `finishConversion()`, it creates a new Villager entity and calls `setVillagerData()` on it. If the zombie had a disallowed profession stored in NBT, the new villager would get that profession.

**How to avoid:** The `setVillagerData` interception catches this case. The zombie cure completes normally, but when the cured Villager's data is set with the disallowed profession, our mixin rejects it and forces NONE.

**Warning signs:** None - the interception handles this transparently. Test by curing a zombie villager near a brewing stand.

### Pitfall 2: Cauldron Variants

**What goes wrong:** Blocking `Blocks.CAULDRON` doesn't block water/lava/powder snow cauldrons, allowing leatherworker profession via filled cauldrons.

**Why it happens:** MC 1.21 has separate Block classes for each cauldron variant: `Blocks.CAULDRON`, `Blocks.WATER_CAULDRON`, `Blocks.LAVA_CAULDRON`, `Blocks.POWDER_SNOW_CAULDRON`.

**How to avoid:** Include ALL cauldron variants in the DISALLOWED_JOB_BLOCKS set.

**Warning signs:** Villagers becoming leatherworkers near water cauldrons.

### Pitfall 3: NBT Loading Race

**What goes wrong:** Villagers spawned from structure NBT (village structures) might have disallowed professions that bypass the mixin.

**Why it happens:** Entity NBT is loaded before the villager fully initializes, and `setVillagerData` is called during this process.

**How to avoid:** The `setVillagerData` interception handles NBT loading too - it's called regardless of the source. However, ensure the mixin priority is correct (default is fine).

**Warning signs:** Village structures spawning villagers with disallowed professions. Test by visiting a new village.

### Pitfall 4: NITWIT Confusion

**What goes wrong:** Treating NITWITs as needing special handling when they don't have a "profession" in the normal sense.

**Why it happens:** NITWITs have `VillagerProfession.NITWIT` which is technically a profession, but they can never work.

**How to avoid:** Include NITWIT in the ALLOWED set - they're fine as-is. They won't seek job blocks and won't gain trades. The CONTEXT.md notes this is likely a no-op.

**Warning signs:** None if handled correctly.

## Code Examples

Verified patterns from official sources:

### Checking Profession Key

```java
// Source: MC 1.21.11 VillagerData.java, verified via decompilation
public static boolean isProfessionMatch(VillagerData data, ResourceKey<VillagerProfession> expected) {
    Holder<VillagerProfession> prof = data.profession();
    return prof.is(expected); // Type-safe comparison via Holder.is()
}
```

### Getting NONE Profession Holder

```java
// Source: MC registry pattern
public static Holder<VillagerProfession> getNoneProfession() {
    return BuiltInRegistries.VILLAGER_PROFESSION.getHolderOrThrow(VillagerProfession.NONE);
}
```

### Creating Modified VillagerData

```java
// Source: MC 1.21.11 VillagerData record methods
VillagerData current = villager.getVillagerData();
VillagerData withNewProfession = current.withProfession(newProfessionHolder);
villager.setVillagerData(withNewProfession);
```

### Blocking POI for Specific Block (Existing Pattern)

```java
// Source: THC v2.6 ServerLevelPoiMixin.java (line 28-40)
@Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"), cancellable = true)
private void thc$blockPoiInClaimedChunks(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
    ServerLevel self = (ServerLevel) (Object) this;
    ChunkPos chunkPos = new ChunkPos(pos);

    if (ClaimManager.INSTANCE.isClaimed(self.getServer(), chunkPos)) {
        ci.cancel();
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| String profession IDs | ResourceKey + Holder | MC 1.17+ | All profession comparison must use ResourceKey/Holder patterns |
| VillagerData mutable | VillagerData record (immutable) | MC 1.20+ | Use `withProfession()` to create modified copies |
| Direct POI manipulation | updatePOIOnBlockStateChange | Stable since 1.14 | Single interception point for all POI changes |

**Deprecated/outdated:**
- String-based profession comparison: Use `ResourceKey` via `Holder.is()` instead
- Direct field modification of VillagerData: Use `with*` builder methods

## Open Questions

Things that couldn't be fully resolved:

1. **Logging detail level**
   - What we know: CONTEXT.md gives discretion on logging/debugging output
   - What's unclear: How verbose should blocked profession attempts be logged?
   - Recommendation: Log at DEBUG level with profession name and source (AI/NBT/cure)

2. **Naturally spawned villagers with professions**
   - What we know: Villages spawn villagers with pre-assigned professions in structure NBT
   - What's unclear: Do we want to log these conversions to NONE?
   - Recommendation: Log first time, then suppress duplicates for same villager UUID

## Sources

### Primary (HIGH confidence)
- MC 1.21.11 decompiled classes: Villager.java, VillagerData.java, VillagerProfession.java, PoiTypes.java
- THC codebase: ServerLevelPoiMixin.java, BrainPoiMemoryMixin.java (proven POI blocking patterns)
- THC v2.8 research: STACK.md (VillagerData API), ARCHITECTURE.md (mixin patterns), PITFALLS.md (edge cases)

### Secondary (MEDIUM confidence)
- MC 1.21.11 decompiled: ZombieVillager.finishConversion() (zombie cure flow)

### Tertiary (LOW confidence)
- None - all findings verified against primary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All APIs verified via MC 1.21.11 decompilation
- Architecture: HIGH - Direct reuse of proven THC patterns (ServerLevelPoiMixin, BrainPoiMemoryMixin)
- Pitfalls: HIGH - Zombie cure and cauldron variants verified against decompiled code

**Research date:** 2026-01-31
**Valid until:** Until MC version upgrade changes VillagerData/POI APIs (likely 6+ months)
