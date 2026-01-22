# Village Structure Boundaries Research

**Researched:** 2026-01-22
**Domain:** Minecraft Structure API (Fabric 1.21.x)
**Confidence:** HIGH

## Summary

Minecraft provides a comprehensive structure API through `StructureManager` that supports efficient position-based structure queries. The existing codebase already uses `getStructureWithPieceAt()` for chunk-level village detection - this same method can be repurposed for per-block protection with minimal changes. The key insight is that `getStructureWithPieceAt(BlockPos, StructureTags.VILLAGE)` already checks if the BlockPos is within any village structure piece's bounding box, making the implementation straightforward.

**Primary recommendation:** Replace chunk-level village detection with per-block `getStructureWithPieceAt()` calls. No caching needed - the method is already optimized for this use case.

---

## Village Structures in Minecraft

### What Structures Count as "Village"

The `StructureTags.VILLAGE` tag (already used in codebase) includes all five village variants:

| Structure ID | Biome |
|--------------|-------|
| `minecraft:village_plains` | Plains, Meadow |
| `minecraft:village_desert` | Desert |
| `minecraft:village_savanna` | Savanna |
| `minecraft:village_snowy` | Snowy Plains |
| `minecraft:village_taiga` | Taiga |

**Source:** [Minecraft Wiki - Structure Tags](https://minecraft.wiki/w/Structure_tag_(Java_Edition))

Villages are jigsaw structures composed of multiple `StructurePiece` objects, each with its own `BoundingBox`. A village can span multiple chunks, with pieces distributed throughout.

### Structure Storage Hierarchy

```
StructureStart (one per structure instance)
├── Structure (type reference, e.g., village_plains)
├── ChunkPos (origin chunk)
├── BoundingBox (encompasses ALL pieces)
└── List<StructurePiece> (individual building/path pieces)
    ├── StructurePiece (house)
    │   └── BoundingBox
    ├── StructurePiece (path segment)
    │   └── BoundingBox
    └── ... more pieces
```

**Key classes (Mojang mappings, as used in THC codebase):**
- `net.minecraft.world.level.StructureManager` - Query interface
- `net.minecraft.world.level.levelgen.structure.StructureStart` - Instance of a generated structure
- `net.minecraft.world.level.levelgen.structure.StructurePiece` - Individual component
- `net.minecraft.world.level.levelgen.structure.BoundingBox` - 3D axis-aligned box

---

## Querying Structure Boundaries

### Method 1: Direct Position Check (Recommended)

The simplest approach uses `getStructureWithPieceAt()` which internally checks if the BlockPos falls within any structure piece's bounding box:

```kotlin
// Check if a BlockPos is inside any village structure piece
fun isInsideVillageStructure(level: ServerLevel, pos: BlockPos): Boolean {
    val structureManager = level.structureManager()
    val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
    return structureAt.isValid
}
```

**How it works internally:**
1. Finds all structure starts that reference the chunk containing `pos`
2. For each matching structure (village), iterates through its pieces
3. Checks if `pos` is inside any piece's `BoundingBox`
4. Returns the `StructureStart` if found (`.isValid` returns true)

**Confidence:** HIGH - This is the same API already used in `ChunkValidator.isVillageChunk()`.

### Method 2: Manual BoundingBox Check

If you need the actual bounding boxes (e.g., for debugging or visualization):

```kotlin
fun getVillageBoundingBoxesInChunk(level: ServerLevel, chunkPos: ChunkPos): List<BoundingBox> {
    val structureManager = level.structureManager()
    val boxes = mutableListOf<BoundingBox>()

    // Get all structure starts that reference this chunk
    val startsForChunk = structureManager.startsForStructure(
        SectionPos.of(chunkPos, 0),
        { holder -> holder.`is`(StructureTags.VILLAGE) }
    )

    for (start in startsForChunk) {
        // Get bounding boxes for each piece
        for (piece in start.pieces) {
            boxes.add(piece.boundingBox)
        }
    }

    return boxes
}
```

### Method 3: Using structureHasPieceAt (Alternative)

If you already have a `StructureStart` reference:

```kotlin
fun isInsideSpecificStructure(level: ServerLevel, pos: BlockPos, start: StructureStart): Boolean {
    return level.structureManager().structureHasPieceAt(pos, start)
}
```

### BoundingBox Contains Check

The `BoundingBox` class (Mojang mappings) provides:

```java
// Check if a position is inside the box
boolean isInside(Vec3i pos)  // Vec3i is parent of BlockPos
boolean isInside(int x, int y, int z)

// Get box dimensions
int minX(), minY(), minZ()
int maxX(), maxY(), maxZ()
BlockPos getCenter()
```

**Note:** In Yarn mappings this class is called `BlockBox` with method `contains()`.

---

## Implementation Strategy

### Current Implementation (Chunk-Level)

```kotlin
// VillageProtection.kt - current approach
if (ChunkValidator.isVillageChunk(serverLevel, chunkPos)) {
    // Block ALL breaks in chunk (too aggressive)
    return@register false
}
```

### Proposed Implementation (Position-Level)

```kotlin
// VillageProtection.kt - proposed change
fun isInsideVillageStructure(level: ServerLevel, pos: BlockPos): Boolean {
    val structureManager = level.structureManager()
    val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
    return structureAt.isValid
}

// In the event handler:
if (isInsideVillageStructure(serverLevel, pos)) {
    // Only block breaks INSIDE village structures
    return@register false
}
```

### Mixin Injection Point

**No mixin needed.** The existing `PlayerBlockBreakEvents.BEFORE` callback in `VillageProtection.kt` is the correct injection point. Just change the check from chunk-level to position-level.

### Performance Considerations

**Question:** Is calling `getStructureWithPieceAt()` on every block break too expensive?

**Answer:** No, it's designed for this use case.

**Why it's efficient:**
1. **Chunk-scoped lookup:** Only checks structures that reference the current chunk
2. **Early termination:** Returns immediately when a containing piece is found
3. **Already indexed:** Structure references are stored per-chunk during world generation
4. **Same as existing:** This is the exact same method already called in `ChunkValidator.isVillageChunk()` at line 134

**Existing code for reference:**
```kotlin
// ChunkValidator.kt line 134
val structureAt = structureManager.getStructureWithPieceAt(checkPos, StructureTags.VILLAGE)
if (structureAt.isValid) {
    return true
}
```

The only difference: Instead of sampling 63 positions (9 x 7 y-levels) per chunk, we check the exact break position once.

### Caching Considerations

**Do we need caching?** Probably not for v2.1.

**Why:**
- Single `getStructureWithPieceAt()` call is already O(pieces in chunk)
- Villages have ~20-50 pieces typically
- Block breaks are infrequent relative to tick rate
- Structures don't change after generation

**If performance becomes an issue (measure first):**
```kotlin
// Optional: Per-chunk structure piece cache
private val villageBoxCache = mutableMapOf<Long, List<BoundingBox>>()

fun getCachedVillageBoxes(level: ServerLevel, chunkPos: ChunkPos): List<BoundingBox> {
    val key = chunkPos.toLong()
    return villageBoxCache.getOrPut(key) {
        computeVillageBoxesForChunk(level, chunkPos)
    }
}

// Invalidate on dimension change or server stop
fun clearCache() {
    villageBoxCache.clear()
}
```

**Recommendation:** Start without caching. Add if profiling shows it's needed.

---

## Code Patterns

### Pattern 1: Simple Position Check

```kotlin
// VillageProtection.kt
object VillageProtection {
    fun register() {
        PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, blockEntity ->
            if (level.isClientSide) return@register true

            val serverLevel = level as ServerLevel

            // Check if position is inside village structure
            if (isInsideVillageStructure(serverLevel, pos)) {
                // Allow ores and allowlist blocks
                if (isOre(state)) return@register true
                if (WorldRestrictions.ALLOWED_BLOCKS.contains(state.block)) return@register true

                // Block the break
                return@register false
            }

            true // Allow break outside village structures
        }
    }

    private fun isInsideVillageStructure(level: ServerLevel, pos: BlockPos): Boolean {
        val structureManager = level.structureManager()
        val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
        return structureAt.isValid
    }
}
```

### Pattern 2: With Debug Logging

```kotlin
private fun isInsideVillageStructure(level: ServerLevel, pos: BlockPos): Boolean {
    val structureManager = level.structureManager()
    val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)

    if (structureAt.isValid) {
        logger.debug("Position $pos is inside village structure: ${structureAt.structure}")
        return true
    }

    return false
}
```

### Pattern 3: Getting All Village Boxes (For Debugging/Visualization)

```kotlin
fun debugPrintVillageBounds(level: ServerLevel, chunkPos: ChunkPos) {
    val structureManager = level.structureManager()
    val chunk = level.getChunk(chunkPos.x, chunkPos.z)

    for ((structure, start) in chunk.allStarts) {
        val holder = level.registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .wrapAsHolder(structure)

        if (holder.`is`(StructureTags.VILLAGE)) {
            logger.info("Village found at ${start.chunkPos}")
            logger.info("  Overall bounds: ${start.boundingBox}")
            logger.info("  Pieces (${start.pieces.size}):")
            for (piece in start.pieces) {
                logger.info("    - ${piece.javaClass.simpleName}: ${piece.boundingBox}")
            }
        }
    }
}
```

---

## Migration Path

### Before (Current - Chunk Level)
```
Player breaks block at (100, 45, 200)
├── Get chunk (6, 12)
├── ChunkValidator.isVillageChunk() samples 63 positions
├── Any hit? Block ALL breaks in chunk
└── Underground player can't break anything
```

### After (Proposed - Position Level)
```
Player breaks block at (100, 45, 200)
├── structureManager.getStructureWithPieceAt(pos, VILLAGE)
├── Check if pos is inside any piece's bounding box
├── Inside? Block break (unless ore/allowlist)
└── Outside? Allow break (including underground)
```

### What Changes

| File | Change |
|------|--------|
| `VillageProtection.kt` | Replace `ChunkValidator.isVillageChunk(chunkPos)` with `getStructureWithPieceAt(pos)` |
| `ChunkValidator.kt` | `isVillageChunk()` can remain for claim validation (prevent claiming village chunks) |

### Backward Compatibility

- Village chunks can still not be claimed (existing behavior preserved)
- Ores still breakable in villages (existing behavior preserved)
- Allowlist blocks still breakable in villages (existing behavior preserved)
- Only change: Non-structure underground areas now breakable

---

## Open Questions

### 1. Should paths be protected?

Village paths are `StructurePiece` objects with their own bounding boxes. The current approach would protect them. Options:

- **Protect all pieces (default):** Players can't break path blocks either
- **Filter by piece type:** Could exclude path pieces, but requires class inspection

**Recommendation:** Protect all pieces for v2.1. If path protection is undesirable, address in future iteration.

### 2. Vertical extent of protection

Village piece bounding boxes extend to their actual built height. A building with a 6-block-tall ceiling will protect 6 blocks vertically. Underground caves directly beneath buildings are unprotected (as intended).

However: Some structures have foundation blocks below ground level. These are included in the bounding box and will be protected.

**Recommendation:** Accept this behavior. Foundation protection is probably desirable anyway.

### 3. Edge cases

- **Partially generated villages:** Bounding boxes exist even if blocks weren't placed (rare)
- **Destroyed villages:** Bounding boxes persist until chunk regeneration
- **Village expansion mods:** Should work as long as they register proper structure pieces

---

## Sources

### Primary (HIGH confidence)
- [StructureManager JavaDocs (NeoForge 1.20.6)](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/minecraft/world/level/StructureManager.html) - Method signatures
- [StructureStart Yarn 1.21](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/structure/StructureStart.html) - Class structure
- [BlockBox Yarn 1.21](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/util/math/BlockBox.html) - Containment methods
- [Structure Tags Wiki](https://minecraft.wiki/w/Structure_tag_(Java_Edition)) - Village tag contents
- Existing codebase: `ChunkValidator.kt` lines 77-145 (working structure queries)

### Secondary (MEDIUM confidence)
- [Village Wiki](https://minecraft.wiki/w/Village) - Village biome/structure info
- [Forge StructureManager 1.19.3](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.19.3/net/minecraft/world/level/StructureManager.html) - Additional method details

### Tertiary (LOW confidence)
- WebSearch results on structure performance - General patterns, not MC-version-specific

---

## Metadata

**Confidence breakdown:**
- API accuracy for 1.21.x: HIGH - Verified against multiple official sources and existing working code
- Performance implications: MEDIUM - Based on API design analysis, not profiling
- Structure types enumerated: HIGH - Official wiki source
- Code patterns: HIGH - Derived from working codebase patterns

**Research date:** 2026-01-22
**Valid until:** Stable - Structure API is mature and unlikely to change significantly
