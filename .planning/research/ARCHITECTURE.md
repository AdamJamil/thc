# Architecture Research: Regional Spawn System

**Researched:** 2026-01-23
**Domain:** Minecraft 1.21.11 NaturalSpawner, mob caps, regional spawn distributions
**Confidence:** HIGH (verified via existing THC patterns, Mojang mappings, Fabric API docs)

## Summary

The regional spawn system integrates into THC's existing spawn infrastructure at multiple layers. The primary entry point is the existing `NaturalSpawnerMixin.isValidSpawnPostitionForType()` hook, extended with regional detection and probability-based mob replacement. NBT tagging for spawn origin uses Fabric's Attachment API (already established in THC). Monster cap partitioning requires a new mixin at `SpawnState` construction.

**Key finding:** The spawn lifecycle provides clear integration points - region detection happens once per spawn attempt, mob type selection happens in `spawnCategoryForPosition`, and entity finalization happens via `Mob.finalizeSpawn()`.

**Primary recommendation:** Layer the regional system on existing `NaturalSpawnerMixin` for position validation, add `Mob` mixin for spawn-time NBT tagging, and use `NaturalSpawner.SpawnState` mixin for cap partitioning.

---

## Existing THC Spawn Architecture

### Current Integration Points

| Component | File | Purpose |
|-----------|------|---------|
| `NaturalSpawnerMixin` | `mixin/NaturalSpawnerMixin.java` | Block spawns in claimed chunks |
| `MonsterSpawnLightMixin` | `mixin/MonsterSpawnLightMixin.java` | Bypass sky light for hostile spawns |
| `ClaimManager` | `claim/ClaimManager.kt` | Query chunk claim status |
| `THCAttachments` | `THCAttachments.java` | Entity attachment types |
| `VillageProtection` | `world/VillageProtection.kt` | Structure detection via `StructureManager` |

### Existing Patterns to Reuse

**Chunk-level validation (NaturalSpawnerMixin):**
```java
@Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
private static void thc$blockSpawnInBaseChunks(..., BlockPos.MutableBlockPos pos, ...) {
    ChunkPos chunkPos = new ChunkPos(pos);
    if (ClaimManager.INSTANCE.isClaimed(level.getServer(), chunkPos)) {
        cir.setReturnValue(false);
    }
}
```

**Structure detection (VillageProtection):**
```kotlin
val structureManager = level.structureManager()
val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
return structureAt.isValid
```

**Entity attachments (THCAttachments):**
```java
public static final AttachmentType<String> SPAWN_ORIGIN = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "spawn_origin"),
    builder -> builder.initializer(() -> null)
);
```

---

## NaturalSpawner Lifecycle

### Spawn Cycle Call Chain

```
ServerLevel.tick()
    -> ServerChunkCache.tickChunks()
        -> NaturalSpawner.spawnForChunk()                    [1. Entry point]
            -> createState() -> SpawnState                    [2. Cap counting]
            -> spawnCategoryForChunk()
                -> spawnCategoryForPosition()
                    -> isValidSpawnPostitionForType()         [3. Position validation - THC HOOK]
                        -> SpawnPlacements.checkSpawnRules()
                            -> Monster.checkMonsterSpawnRules()
                    -> getMobForSpawn()                       [4. Mob type selection]
                    -> Entity.add -> finalizeSpawn()          [5. Entity creation - THC HOOK]
```

### Key Methods and Parameters

**`NaturalSpawner.isValidSpawnPostitionForType()`**
- Parameters: `(ServerLevel, MobCategory, StructureManager, ChunkGenerator, SpawnerData, BlockPos.MutableBlockPos, double)`
- Returns: `boolean` - whether spawn is allowed
- **Integration:** Extend existing mixin for regional detection

**`NaturalSpawner.getMobForSpawn()`**
- Parameters: `(ServerLevel, MobCategory)`
- Returns: `Optional<SpawnerData>` - mob type to spawn
- **Potential integration:** Could redirect for mob replacement, but more complex

**`Mob.finalizeSpawn()`**
- Parameters: `(ServerLevelAccessor, DifficultyInstance, EntitySpawnReason, SpawnGroupData)`
- Returns: `SpawnGroupData`
- **Integration:** Best place for NBT tagging and equipment customization

**`NaturalSpawner.SpawnState` (inner class)**
- Tracks mob counts per category for cap calculation
- Constructor takes chunk count and player positions
- **Integration:** Mixin for cap partitioning

---

## Component Architecture

### New Components

| Component | Type | Purpose | Location |
|-----------|------|---------|----------|
| `SpawnRegion` | Enum | SURFACE / UPPER_CAVE / LOWER_CAVE | `spawn/SpawnRegion.java` |
| `RegionDetector` | Utility | Y-level + sky access -> region | `spawn/RegionDetector.java` |
| `SpawnDistributions` | Data | Per-region mob probability tables | `spawn/SpawnDistributions.java` |
| `MobFinalizeSpawnMixin` | Mixin | NBT tagging + equipment | `mixin/MobFinalizeSpawnMixin.java` |
| `SpawnStateMixin` | Mixin | Cap partitioning | `mixin/SpawnStateMixin.java` |
| `PillagerVariant` | Enum | MELEE / RANGED loadouts | `spawn/PillagerVariant.java` |

### Modified Components

| Component | Modification |
|-----------|--------------|
| `NaturalSpawnerMixin` | Add regional detection and spawn probability filtering |
| `THCAttachments` | Add SPAWN_ORIGIN attachment type |

### Component Dependency Graph

```
                    +------------------+
                    | NaturalSpawner   |
                    |     Mixin        |
                    +--------+---------+
                             |
            +----------------+----------------+
            |                                 |
            v                                 v
    +---------------+               +------------------+
    | RegionDetector|               | SpawnDistributions|
    +-------+-------+               +--------+---------+
            |                                |
            v                                v
    +---------------+               +------------------+
    |  SpawnRegion  |               |  Mob probability |
    |     Enum      |               |     tables       |
    +---------------+               +------------------+


    +------------------+            +------------------+
    | MobFinalizeSpawn |            |   SpawnState     |
    |      Mixin       |            |     Mixin        |
    +--------+---------+            +--------+---------+
             |                               |
             v                               v
    +------------------+            +------------------+
    | THCAttachments   |            | Per-region caps  |
    | SPAWN_ORIGIN     |            | 30%/40%/50%      |
    +------------------+            +------------------+
```

---

## Integration Strategy

### Question 1: Where to Intercept for Custom Distribution

**Answer:** Extend existing `NaturalSpawnerMixin.isValidSpawnPostitionForType()`

**Rationale:**
- Already hooks the right lifecycle point
- Has access to `BlockPos` for region detection
- Has access to `SpawnerData` for mob type filtering
- Can cancel spawn attempt to effectively "replace" distribution

**Implementation pattern:**
```java
@Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
private static void thc$regionalSpawnDistribution(
        ServerLevel level, MobCategory category, StructureManager structureManager,
        ChunkGenerator generator, SpawnerData spawnerData, BlockPos.MutableBlockPos pos,
        double squaredDistance, CallbackInfoReturnable<Boolean> cir) {

    // 1. Existing: base chunk blocking
    if (ClaimManager.INSTANCE.isClaimed(level.getServer(), new ChunkPos(pos))) {
        cir.setReturnValue(false);
        return;
    }

    // 2. New: regional distribution filtering
    if (category == MobCategory.MONSTER && level.dimension() == Level.OVERWORLD) {
        SpawnRegion region = RegionDetector.detect(level, pos);
        if (!SpawnDistributions.shouldSpawn(spawnerData.type(), region, level.random)) {
            cir.setReturnValue(false);
            return;
        }
    }
}
```

### Question 2: Region Detection Structure

**Answer:** Y-level primary, sky access secondary

**Region boundaries:**
| Region | Y Range | Sky Access | Description |
|--------|---------|------------|-------------|
| SURFACE | Y >= 56 | canSeeSky = true | Overworld surface |
| UPPER_CAVE | Y >= 0 | canSeeSky = false | Standard caves |
| LOWER_CAVE | Y < 0 | any | Deep dark territory |

**Implementation:**
```java
public enum SpawnRegion {
    SURFACE, UPPER_CAVE, LOWER_CAVE;

    public static SpawnRegion detect(ServerLevel level, BlockPos pos) {
        int y = pos.getY();

        if (y < 0) {
            return LOWER_CAVE;
        }

        if (y >= 56 && level.canSeeSky(pos)) {
            return SURFACE;
        }

        return UPPER_CAVE;
    }
}
```

**Edge cases:**
- Ravines (high Y, no sky): UPPER_CAVE (correct - exposed cave)
- Mountain caves (Y > 56, no sky): UPPER_CAVE (correct - cave in mountain)
- Ocean floor (Y < 56, has sky): Not applicable (underwater spawning is different)

### Question 3: Integration with Base Chunk Blocking

**Answer:** Check order matters - base chunks first, then regional filtering

**Current flow:**
```
isValidSpawnPostitionForType
    -> Base chunk check (existing)
    -> Return false if claimed
```

**Extended flow:**
```
isValidSpawnPostitionForType
    -> Base chunk check (existing)     <- Keep first
    -> Return false if claimed
    -> Regional distribution check     <- Add second
    -> Return false if mob filtered
    -> (vanilla validation continues)
```

**Code organization:**
Keep in single mixin with clear separation:
```java
// PHASE 1: Base protection (existing)
if (isBaseChunk(level, pos)) {
    cir.setReturnValue(false);
    return;
}

// PHASE 2: Regional distribution (new)
if (shouldApplyRegionalFilter(level, category)) {
    SpawnRegion region = RegionDetector.detect(level, pos);
    if (!SpawnDistributions.shouldSpawn(spawnerData.type(), region)) {
        cir.setReturnValue(false);
        return;
    }
}
```

### Question 4: NBT Tagging on Entity Spawn

**Answer:** Mixin to `Mob.finalizeSpawn()` with attachment

**Why `finalizeSpawn()` over alternatives:**
| Option | Pros | Cons |
|--------|------|------|
| `finalizeSpawn()` | Called for all natural spawns, entity already exists | Slightly after creation |
| Entity constructor | Earliest possible | No world context, no spawn type info |
| `ServerEntityEvents.ENTITY_LOAD` | Fabric API event | Fires for ALL entity loads, not just spawns |
| `addFreshEntityWithPassengers` | Right before world add | Requires redirect, complex |

**Implementation:**
```java
@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void thc$tagSpawnOrigin(
            ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return; // Only tag natural spawns
        }

        Mob self = (Mob) (Object) this;
        BlockPos pos = self.blockPosition();

        if (level.getLevel().dimension() == Level.OVERWORLD) {
            SpawnRegion region = RegionDetector.detect((ServerLevel) level.getLevel(), pos);
            self.setAttached(THCAttachments.SPAWN_ORIGIN, region.name());
        }
    }
}
```

**Attachment definition:**
```java
// In THCAttachments.java
public static final AttachmentType<String> SPAWN_ORIGIN = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "spawn_origin"),
    builder -> builder.initializer(() -> null)
    // NOT persistent - we don't need to save this
);
```

### Question 5: Partitioned Monster Cap Counting

**Answer:** Mixin to `NaturalSpawner.createState()` or `SpawnState` constructor

**Vanilla mob cap calculation:**
```java
// In NaturalSpawner
SpawnState state = NaturalSpawner.createState(
    spawnFriendlies,
    spawnEnemies,
    spawnableChunkCount,
    /* mob counts from world */
);

// Cap = BASE_CAP * chunkCount / 289
// MONSTER base cap = 70
```

**Partitioning approach:**

Option A: **Separate cap tracking per region** (Complex)
- Track mob counts per region
- Apply per-region caps
- Requires significant SpawnState modification

Option B: **Global cap with regional weighting** (Simpler)
- Keep vanilla cap behavior
- Regional distribution handles balance
- More compatible with vanilla

**Recommendation:** Option B initially

The regional distribution system already controls spawn probabilities. If cap partitioning proves necessary for gameplay balance, it can be added later by:

1. Creating custom `SpawnState` wrapper that tracks per-region counts
2. Injecting into `spawnCategoryForChunk` to pass region-aware cap data

**Deferred implementation sketch:**
```java
@Mixin(NaturalSpawner.class)
public class RegionalCapMixin {
    @Unique
    private static ThreadLocal<Map<SpawnRegion, Integer>> thc$regionCounts =
        ThreadLocal.withInitial(HashMap::new);

    @Inject(method = "spawnCategoryForChunk", at = @At("HEAD"))
    private static void thc$trackRegionalCap(
            MobCategory category, ServerLevel level, ...) {
        // Track counts per region
        // Check against partitioned cap before allowing spawn
    }
}
```

### Question 6: Structure Spawn Bypass

**Answer:** Check `EntitySpawnReason` in distribution filter, not in position validation

**Structure spawns use different paths:**
- Pillager outposts: `EntitySpawnReason.STRUCTURE`
- Woodland mansions: `EntitySpawnReason.STRUCTURE`
- Raids: `EntitySpawnReason.EVENT`
- Spawners: `EntitySpawnReason.SPAWNER`

**Current spawn types that pass through `isValidSpawnPostitionForType`:**
- `EntitySpawnReason.NATURAL` - Regular mob spawning
- `EntitySpawnReason.CHUNK_GENERATION` - Initial chunk population

**Structure spawns do NOT go through `isValidSpawnPostitionForType`** - they use direct entity creation. This means:

1. **No bypass needed** in NaturalSpawnerMixin - structures already bypass it
2. **Structure detection** only needed if we want to apply regional rules TO structure spawns
3. **Current design** naturally allows structure spawns to proceed unmodified

**If bypass detection needed later:**
```java
// In MobFinalizeSpawnMixin
if (reason == EntitySpawnReason.STRUCTURE) {
    // Don't apply regional tagging/modification
    return;
}
```

### Question 7: Pillager Variant Equipment

**Answer:** Handle in `MobFinalizeSpawnMixin` for natural spawns, check entity type

**Equipment loadouts:**
| Variant | Main Hand | Off Hand | Armor |
|---------|-----------|----------|-------|
| MELEE | Iron Sword | Shield | Iron Helmet, Chainmail Chest |
| RANGED | Crossbow | - | Leather Helmet |

**Implementation:**
```java
@Inject(method = "finalizeSpawn", at = @At("TAIL"))
private void thc$applyPillagerVariant(
        ServerLevelAccessor level, DifficultyInstance difficulty,
        EntitySpawnReason reason, SpawnGroupData groupData,
        CallbackInfoReturnable<SpawnGroupData> cir) {

    Mob self = (Mob) (Object) this;

    if (!(self instanceof Pillager pillager)) {
        return;
    }

    if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
        return; // Only modify natural spawns
    }

    // 50/50 variant selection (or based on region)
    PillagerVariant variant = level.getRandom().nextBoolean()
        ? PillagerVariant.MELEE
        : PillagerVariant.RANGED;

    variant.applyEquipment(pillager);
}
```

**PillagerVariant enum:**
```java
public enum PillagerVariant {
    MELEE {
        @Override
        public void applyEquipment(Pillager pillager) {
            pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            pillager.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            pillager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            pillager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        }
    },
    RANGED {
        @Override
        public void applyEquipment(Pillager pillager) {
            // Default crossbow already equipped
            pillager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        }
    };

    public abstract void applyEquipment(Pillager pillager);
}
```

### Question 8: Distribution Table Data Structures

**Answer:** EnumMap with probability weights per entity type

**Data structure:**
```java
public class SpawnDistributions {
    // Region -> (EntityType -> probability weight)
    private static final Map<SpawnRegion, Map<EntityType<?>, Float>> WEIGHTS =
        new EnumMap<>(SpawnRegion.class);

    static {
        // Surface: zombies common, creepers rare
        Map<EntityType<?>, Float> surface = new HashMap<>();
        surface.put(EntityTypes.ZOMBIE, 0.0f);  // Replaced by Husk
        surface.put(EntityTypes.HUSK, 1.0f);
        surface.put(EntityTypes.SKELETON, 0.0f); // Replaced by Stray
        surface.put(EntityTypes.STRAY, 1.0f);
        surface.put(EntityTypes.CREEPER, 0.8f);
        surface.put(EntityTypes.SPIDER, 1.0f);
        surface.put(EntityTypes.PILLAGER, 0.3f); // Rare surface pillagers
        WEIGHTS.put(SpawnRegion.SURFACE, surface);

        // Upper cave: mixed distribution
        // ...

        // Lower cave: warden territory
        // ...
    }

    public static boolean shouldSpawn(EntityType<?> type, SpawnRegion region, RandomSource random) {
        Map<EntityType<?>, Float> regionWeights = WEIGHTS.get(region);
        if (regionWeights == null) {
            return true; // Unknown region, allow spawn
        }

        Float weight = regionWeights.get(type);
        if (weight == null) {
            return true; // Unknown mob for this region, allow spawn
        }

        return random.nextFloat() < weight;
    }
}
```

**Alternative: Data-driven via JSON**
```json
{
  "surface": {
    "minecraft:husk": 1.0,
    "minecraft:stray": 1.0,
    "minecraft:creeper": 0.8,
    "minecraft:spider": 1.0
  },
  "upper_cave": {
    "minecraft:zombie": 0.5,
    "minecraft:husk": 0.5,
    "minecraft:skeleton": 0.5,
    "minecraft:stray": 0.5,
    "minecraft:creeper": 1.0
  },
  "lower_cave": {
    "minecraft:zombie": 1.0,
    "minecraft:skeleton": 1.0,
    "minecraft:creeper": 0.5,
    "minecraft:warden": 0.1
  }
}
```

**Recommendation:** Start with hardcoded EnumMap for simplicity, consider data-driven later if balancing requires frequent iteration.

---

## Data Flow Diagram

### Natural Spawn Flow with THC Regional System

```
ServerLevel.tick()
     |
     v
NaturalSpawner.spawnForChunk()
     |
     +---> createState() -----> [Future: Regional cap tracking]
     |
     v
spawnCategoryForChunk()
     |
     v
spawnCategoryForPosition()
     |
     v
isValidSpawnPostitionForType() ----+
     |                              |
     |  [THC NaturalSpawnerMixin]   |
     |                              |
     +---> Base chunk check --------+
     |     (existing)               |
     |                              |
     +---> Region detection --------+
     |     RegionDetector.detect()  |
     |                              |
     +---> Distribution filter -----+
     |     SpawnDistributions       |
     |     .shouldSpawn()           |
     |                              |
     v                              |
[Vanilla position validation] <-----+
     |
     v
getMobForSpawn() -> SpawnerData
     |
     v
Entity creation
     |
     v
Mob.finalizeSpawn() ----+
     |                   |
     |  [THC Mixin]      |
     |                   |
     +---> NBT tagging --+
     |     SPAWN_ORIGIN  |
     |                   |
     +---> Pillager -----+
     |     equipment     |
     |                   |
     v                   |
Entity added to world <--+
```

---

## Build Order

### Phase 1: Region Detection Foundation
1. Create `SpawnRegion` enum
2. Create `RegionDetector` utility
3. Unit test region detection logic

### Phase 2: Distribution Tables
4. Create `SpawnDistributions` class with hardcoded weights
5. Implement `shouldSpawn()` probability logic
6. Define initial distribution values

### Phase 3: Spawn Filtering
7. Extend `NaturalSpawnerMixin` with regional filtering
8. Preserve existing base chunk blocking
9. Add dimension check (Overworld only)

### Phase 4: NBT Tagging
10. Add `SPAWN_ORIGIN` to `THCAttachments`
11. Create `MobFinalizeSpawnMixin`
12. Tag spawned mobs with region

### Phase 5: Pillager Variants
13. Create `PillagerVariant` enum
14. Add equipment application in `MobFinalizeSpawnMixin`
15. Implement variant selection logic

### Phase 6: Cap Partitioning (Deferred)
16. Evaluate if needed after playtesting
17. If needed: Create `SpawnStateMixin` for regional tracking

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Distribution balance off | HIGH | MEDIUM | Iterative tuning, data-driven option |
| Performance from region detection | LOW | LOW | Single Y-level + sky check per spawn attempt |
| Mixin conflicts with other mods | LOW | HIGH | Use `@At("HEAD")` with early return |
| Cap partitioning complexity | MEDIUM | MEDIUM | Defer until proven necessary |
| Structure spawns affected | LOW | MEDIUM | Reason check in finalizeSpawn |

---

## Sources

### Primary (HIGH confidence)
- Existing THC mixin patterns (`NaturalSpawnerMixin.java`, `MonsterSpawnLightMixin.java`)
- [Mojang mappings via Fabric Loom](https://github.com/FabricMC/intermediary) (1.21.11)
- [Fabric Biome Modification API](https://deepwiki.com/FabricMC/fabric/9.1-biome-modification-api)
- [Fabric Wiki - Mixin Injects](https://wiki.fabricmc.net/tutorial:mixin_injects)

### Secondary (MEDIUM confidence)
- [Minecraft Mob Spawning Analysis](https://blog.bithole.dev/blogposts/mob-spawning/)
- [Curtain NaturalSpawnerMixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java)
- [Fabric API ServerEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html)
- [Minecraft Wiki - Mob spawning](https://minecraft.wiki/w/Mob_spawning)

### Verification Notes
- Method signatures verified against existing THC mixins
- Call chains traced from NaturalSpawner through to entity creation
- Pattern compatibility confirmed with existing mixin architecture

---

## Metadata

**Confidence breakdown:**
- Integration points: HIGH - existing mixin patterns validate approach
- Region detection: HIGH - simple Y-level + sky check
- NBT tagging: HIGH - Attachment API already used in THC
- Distribution tables: MEDIUM - probability logic straightforward, balance needs tuning
- Cap partitioning: MEDIUM - deferred, design only

**Research date:** 2026-01-23
**Valid until:** Minecraft 1.21.x (method names stable within minor versions)
