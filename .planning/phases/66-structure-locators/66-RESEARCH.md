# Phase 66: Structure Locators - Research

**Researched:** 2026-01-30
**Domain:** Custom compass items with structure-finding behavior (Minecraft 1.21.11 Fabric)
**Confidence:** HIGH

## Summary

Structure locators are custom compass-style items that point to specific structures. The implementation leverages Minecraft's existing `lodestone_tracker` component and `range_dispatch` item model system introduced in 1.21.4. The core challenge is dynamically locating structures server-side using `ServerLevel.findNearestMapStructure()` and updating the lodestone_tracker component to point the compass needle toward the found position.

The recommended approach is to create a base `StructureLocatorItem` class that:
1. Overrides `inventoryTick()` to periodically search for structures
2. Updates the `lodestone_tracker` component with found coordinates
3. Uses vanilla compass rendering via `range_dispatch` model definitions

**Primary recommendation:** Use the lodestone_tracker component with `tracked: false` and periodically update the target position. This reuses vanilla compass rendering without custom client-side code.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.141.0+1.21.11 | Mod loader and API hooks | Already in use |
| DataComponents | MC 1.21+ | Item component storage | Vanilla system, proven in codebase |
| lodestone_tracker | MC 1.21+ | Compass targeting component | Built-in compass pointing mechanism |

### Supporting
| Component | Purpose | When to Use |
|-----------|---------|-------------|
| `DataComponents.LODESTONE_TRACKER` | Store target coordinates | All locator items |
| `LodestoneTracker` record | Target position + dimension + tracked flag | Component value type |
| `ServerLevel.findNearestMapStructure()` | Locate nearest structure | Server-side structure search |
| `TagKey<Structure>` or `ResourceKey<Structure>` | Structure identifier | Structure type specification |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| lodestone_tracker + vanilla compass | Custom component + custom rendering | Much more work, no benefit |
| inventoryTick polling | Event-based updates | Events would be more complex for periodic search |
| Per-item structure search | Cached server-side results | Premature optimization, likely unnecessary |

**Installation:**
No additional dependencies required - uses vanilla Minecraft APIs available through Fabric.

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/thc/
├── item/
│   ├── StructureLocatorItem.kt      # Base class with structure search logic
│   └── THCItems.kt                  # Registration (add 6 locators)
└── locator/
    └── StructureLocators.kt         # Structure type definitions and search config

src/main/resources/
├── assets/thc/
│   ├── items/                       # Item model definitions (compass range_dispatch)
│   │   ├── fortress_locator.json
│   │   ├── bastion_locator.json
│   │   ├── trial_chamber_locator.json
│   │   ├── pillager_outpost_locator.json
│   │   ├── ancient_city_locator.json
│   │   └── stronghold_locator.json
│   ├── models/item/                 # 16 models per locator (16 compass angles)
│   │   ├── fortress_locator_00.json through fortress_locator_15.json
│   │   └── ... (repeat for each locator type)
│   └── textures/item/               # 6 unique base textures
│       ├── fortress_locator.png
│       └── ...
```

### Pattern 1: Component-Based Compass Targeting
**What:** Store target position in lodestone_tracker component, let vanilla handle needle rendering
**When to use:** Always for compass-style items
**Example:**
```kotlin
// Source: Minecraft Wiki lodestone_tracker component format
val target = LodestoneTracker(
    Optional.of(GlobalPos.of(dimension, targetPos)),
    false  // tracked=false: don't remove component when no lodestone exists
)
stack.set(DataComponents.LODESTONE_TRACKER, target)
```

### Pattern 2: Periodic Structure Search in inventoryTick
**What:** Override inventoryTick() to search for structures periodically
**When to use:** When item needs server-side updates while in inventory
**Example:**
```kotlin
// Source: Fabric documentation + vanilla Item API
override fun inventoryTick(
    stack: ItemStack,
    level: Level,
    entity: Entity,
    slot: Int,
    selected: Boolean
) {
    if (level.isClientSide || entity !is Player) return

    // Throttle searches: only every 20 ticks (1 second)
    if (level.gameTime % 20 != 0L) return

    val serverLevel = level as ServerLevel
    val searchRadius = 100  // chunks

    val found = serverLevel.findNearestMapStructure(
        structureTag,           // TagKey<Structure> for target
        entity.blockPosition(), // search origin
        searchRadius,
        false                   // skipKnownStructures
    )

    if (found != null) {
        updateCompassTarget(stack, serverLevel.dimension(), found)
    } else {
        clearCompassTarget(stack)  // triggers random spin
    }
}
```

### Pattern 3: Dimension-Aware Locators
**What:** Check dimension before searching, clear target if in wrong dimension
**When to use:** All locators - they only work in intended dimension
**Example:**
```kotlin
// Overworld locators check: dimension == Level.OVERWORLD
// Nether locators check: dimension == Level.NETHER
if (serverLevel.dimension() != expectedDimension) {
    clearCompassTarget(stack)  // Needle spins randomly
    return
}
```

### Anti-Patterns to Avoid
- **Custom compass rendering:** Don't implement client-side needle rendering - use lodestone_tracker + vanilla system
- **Storing position in custom component:** Use lodestone_tracker, not a custom THC component
- **Continuous structure search:** Throttle to ~1 second intervals, structure search is expensive
- **Caching found structure permanently:** Structure might be destroyed or player moves - periodic refresh is correct

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Compass needle rendering | Custom client-side rotation | lodestone_tracker + range_dispatch | 102-frame procedural needle is complex |
| Target tracking | Custom component | DataComponents.LODESTONE_TRACKER | Vanilla handles all edge cases |
| Spinning needle animation | Custom animation code | Set target to null in lodestone_tracker | Vanilla behavior when target invalid |
| Structure finding | Custom world scan | ServerLevel.findNearestMapStructure() | Already optimized for chunk-based search |
| Dimension checking | Custom logic | Compare Level.dimension() | Standard API |

**Key insight:** Minecraft 1.21.4+ provides complete compass infrastructure via lodestone_tracker and range_dispatch models. Custom rendering would be significant effort with no benefit.

## Common Pitfalls

### Pitfall 1: Performance - Searching Every Tick
**What goes wrong:** Structure search is expensive; calling every tick causes lag
**Why it happens:** inventoryTick() runs every tick for every item in inventory
**How to avoid:** Throttle searches to every 20+ ticks using `level.gameTime % 20 == 0`
**Warning signs:** Server TPS drops when players hold locators

### Pitfall 2: Client/Server Confusion
**What goes wrong:** Structure search runs on client, fails silently
**Why it happens:** inventoryTick() runs on both sides; Level vs ServerLevel confusion
**How to avoid:** Early return if `level.isClientSide`; cast to ServerLevel only after check
**Warning signs:** Locators work in singleplayer but not on servers

### Pitfall 3: Lodestone Tracking Removes Component
**What goes wrong:** Component disappears when no lodestone at target position
**Why it happens:** Default `tracked: true` causes vanilla to remove component
**How to avoid:** Always set `tracked: false` in LodestoneTracker constructor
**Warning signs:** Locator suddenly stops pointing, becomes regular compass

### Pitfall 4: Wrong Structure Identifier
**What goes wrong:** findNearestMapStructure returns null even when structure nearby
**Why it happens:** Using wrong TagKey or ResourceKey; structure names changed between versions
**How to avoid:** Verify structure identifiers against `/locate structure` command names
**Warning signs:** Specific locator never finds anything

### Pitfall 5: Missing Item Model Definition
**What goes wrong:** Compass needle doesn't animate; shows static texture
**Why it happens:** 1.21.4+ requires items/*.json with range_dispatch, not just models/*.json
**How to avoid:** Create proper items/[locator].json with compass range_dispatch property
**Warning signs:** Item renders but needle never moves

## Code Examples

Verified patterns from official sources and codebase analysis:

### Structure Locator Item Class
```kotlin
// Source: Based on vanilla CompassItem + codebase patterns
class StructureLocatorItem(
    properties: Properties,
    private val structureTag: TagKey<Structure>,
    private val expectedDimension: ResourceKey<Level>
) : Item(properties) {

    override fun inventoryTick(
        stack: ItemStack,
        level: Level,
        entity: Entity,
        slot: Int,
        selected: Boolean
    ) {
        // Server-side only
        if (level.isClientSide || entity !is Player) return

        // Throttle: search once per second
        if (level.gameTime % 20 != 0L) return

        val serverLevel = level as ServerLevel

        // Dimension check - wrong dimension = spinning needle
        if (serverLevel.dimension() != expectedDimension) {
            clearTarget(stack)
            return
        }

        // Search within 100 chunks
        val found = serverLevel.findNearestMapStructure(
            structureTag,
            entity.blockPosition(),
            100,  // search radius in chunks
            false // don't skip known structures
        )

        if (found != null) {
            setTarget(stack, serverLevel.dimension(), found)
        } else {
            clearTarget(stack)
        }
    }

    private fun setTarget(stack: ItemStack, dimension: ResourceKey<Level>, pos: BlockPos) {
        val tracker = LodestoneTracker(
            Optional.of(GlobalPos.of(dimension, pos)),
            false  // tracked=false: keep component even without lodestone
        )
        stack.set(DataComponents.LODESTONE_TRACKER, tracker)
    }

    private fun clearTarget(stack: ItemStack) {
        // Setting empty target causes random spin (vanilla behavior)
        val tracker = LodestoneTracker(Optional.empty(), false)
        stack.set(DataComponents.LODESTONE_TRACKER, tracker)
    }
}
```

### Item Registration Pattern
```kotlin
// Source: THCItems.kt pattern in codebase
@JvmField
val FORTRESS_LOCATOR: Item = register("fortress_locator") { key ->
    StructureLocatorItem(
        Item.Properties().setId(key).stacksTo(1),
        StructureTags.FORTRESS,  // TagKey<Structure> for nether fortress
        Level.NETHER             // Expected dimension
    )
}
```

### Item Model Definition (range_dispatch compass)
```json
// Source: Minecraft Wiki Items_model_definition
// File: assets/thc/items/fortress_locator.json
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:compass",
    "target": "lodestone",
    "wobble": true,
    "scale": 1.0,
    "fallback": {
      "type": "minecraft:model",
      "model": "thc:item/fortress_locator_00"
    },
    "entries": [
      { "threshold": 0.0, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_00" } },
      { "threshold": 0.0625, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_01" } },
      { "threshold": 0.125, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_02" } },
      { "threshold": 0.1875, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_03" } },
      { "threshold": 0.25, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_04" } },
      { "threshold": 0.3125, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_05" } },
      { "threshold": 0.375, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_06" } },
      { "threshold": 0.4375, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_07" } },
      { "threshold": 0.5, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_08" } },
      { "threshold": 0.5625, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_09" } },
      { "threshold": 0.625, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_10" } },
      { "threshold": 0.6875, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_11" } },
      { "threshold": 0.75, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_12" } },
      { "threshold": 0.8125, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_13" } },
      { "threshold": 0.875, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_14" } },
      { "threshold": 0.9375, "model": { "type": "minecraft:model", "model": "thc:item/fortress_locator_15" } }
    ]
  }
}
```

### Individual Model File Pattern
```json
// Source: Codebase model pattern
// File: assets/thc/models/item/fortress_locator_00.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "thc:item/fortress_locator_00"
  }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ModelPredicateProviderRegistry | Items model definition (range_dispatch) | MC 1.21.4 | Model predicates removed; use JSON-based range_dispatch |
| NBT-based item data | Data Components | MC 1.20.5 | lodestone_tracker is now a component, not NBT |
| Custom compass rendering | lodestone_tracker + vanilla | MC 1.21+ | No need for client-side code |

**Deprecated/outdated:**
- `ModelPredicateProviderRegistry`: Removed in 1.21.4 - use items model definition JSON instead
- NBT compound tags: Replaced by DataComponents system

## Structure Identifiers

Based on `/locate structure` command and research:

| Structure | Identifier | TagKey/ResourceKey | Dimension |
|-----------|------------|-------------------|-----------|
| Nether Fortress | `minecraft:fortress` | Likely `StructureTags.FORTRESS` or needs ResourceKey | Nether |
| Bastion Remnant | `minecraft:bastion_remnant` | Needs verification | Nether |
| Trial Chambers | `minecraft:trial_chambers` | `StructureTags.TRIAL_CHAMBERS` (has tag) | Overworld |
| Pillager Outpost | `minecraft:pillager_outpost` | Needs verification | Overworld |
| Ancient City | `minecraft:ancient_city` | Needs verification | Overworld |
| Stronghold | `minecraft:stronghold` | `StructureTags.EYE_OF_ENDER_LOCATED` (contains stronghold) | Overworld |

**Note:** Some structures may not have dedicated TagKey constants in StructureTags. Alternative approach: create custom TagKey or use ResourceKey directly with registry lookup. Verify during implementation against actual Minecraft 1.21.11 API.

## Open Questions

Things that couldn't be fully resolved:

1. **Exact StructureTags availability**
   - What we know: StructureTags.VILLAGE works; some structures have tags for maps
   - What's unclear: Which of the 6 target structures have pre-defined TagKey constants
   - Recommendation: During implementation, check StructureTags class for constants. If missing, create custom TagKey with `TagKey.create(Registries.STRUCTURE, Identifier.of("minecraft", "fortress"))` or use ResourceKey-based lookup

2. **Texture generation approach**
   - What we know: Need 16 directional models per locator (16 angles)
   - What's unclear: Best approach for creating 6x16=96 texture variants
   - Recommendation: Create base texture with needle, rotate programmatically or create single base + 16 needle overlays. Claude's discretion per CONTEXT.md.

3. **findNearestMapStructure signature verification**
   - What we know: Method exists on ServerLevel; takes TagKey<Structure>, BlockPos, int, boolean
   - What's unclear: Exact parameter order and types in 1.21.11 Yarn mappings
   - Recommendation: Verify method signature in IDE during implementation; may need to check Yarn vs Mojang mapping names

## Sources

### Primary (HIGH confidence)
- [Minecraft Wiki - Items model definition](https://minecraft.wiki/w/Items_model_definition) - range_dispatch, compass property
- [Minecraft Wiki - lodestone_tracker component](https://minecraft.wiki/w/Data_component_format/lodestone_tracker) - Component structure
- [Fabric Documentation - Custom Data Components](https://docs.fabricmc.net/develop/items/custom-data-components) - Component API patterns

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Structure tags](https://minecraft.wiki/w/Structure_tag_(Java_Edition)) - Available structure tags
- [NeoForge ServerLevel Javadocs](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/server/level/ServerLevel.html) - findNearestMapStructure signature
- [Fabric Wiki - Model Predicate Providers](https://wiki.fabricmc.net/tutorial:model_predicate_providers) - Deprecation notice for 1.21.4+

### Tertiary (LOW confidence)
- WebSearch results for structure identifiers - verify against actual API
- [Explorer's Compass mod](https://github.com/MattCzyr/ExplorersCompass) - Reference implementation (didn't analyze source directly)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - lodestone_tracker and range_dispatch are well-documented vanilla features
- Architecture: HIGH - Pattern follows vanilla CompassItem behavior
- Pitfalls: MEDIUM - Based on general Minecraft modding knowledge; specific edge cases may exist
- Structure identifiers: MEDIUM - Command names known; exact TagKey availability needs verification

**Research date:** 2026-01-30
**Valid until:** 60 days (stable vanilla APIs)
