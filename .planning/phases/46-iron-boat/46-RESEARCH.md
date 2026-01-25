# Phase 46: Iron Boat - Research

**Researched:** 2026-01-25
**Domain:** Custom entity with fluid compatibility, custom item with fire resistance
**Confidence:** MEDIUM

## Summary

Implementing the iron boat requires creating a custom entity that extends vanilla Boat behavior to work on lava, a custom item with fire resistance, and appropriate mixins for damage immunity and fluid physics. The core technical challenges are: (1) making the boat entity float on lava using the same physics as water boats, (2) making the boat immune to fire/lava/mob damage, (3) making the dropped item entity fire-resistant and lava-floating, and (4) giving dropped items initial velocity toward the breaking player.

The recommended approach is to create a custom entity class extending `Boat`, override the fluid-checking methods to include lava alongside water, and override damage methods to filter allowed damage sources. The item should use `DataComponents.DAMAGE_RESISTANT` with the `#minecraft:is_fire` damage type tag for fire/lava immunity.

**Primary recommendation:** Extend vanilla `Boat` class with overridden `checkInWater()`, `getWaterLevelAbove()`, and `isUnderwater()` methods to treat lava the same as water. Override `hurt()` to only allow player attack damage.

## Standard Stack

The iron boat implementation uses existing THC patterns plus Minecraft vanilla entity architecture.

### Core
| Component | Version | Purpose | Why Standard |
|-----------|---------|---------|--------------|
| Custom Entity extending Boat | MC 1.21.11 | Lava-compatible boat | Vanilla Boat has all physics; just needs fluid type extension |
| Custom Item | Fabric API | Craftable item that spawns entity | THC pattern from THCItems.kt |
| DataComponents.DAMAGE_RESISTANT | MC 1.21+ | Fire/lava immunity for item entity | Modern MC component system |
| Mixin for entity rendering | Fabric | Custom texture when riding | Client-side texture override |

### Supporting
| Component | Purpose | When to Use |
|-----------|---------|-------------|
| FluidTags.LAVA | Lava fluid detection | In overridden fluid-checking methods |
| DamageSource filtering | Damage immunity | In overridden hurt() method |
| ItemEntity velocity | Initial trajectory | When boat drops item |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom Boat subclass | Mixin to vanilla Boat | Mixin more invasive; custom class cleaner for isolated behavior |
| DAMAGE_RESISTANT component | Mixin ItemEntity.hurt | Component is cleaner MC 1.21+ pattern |

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/thc/
├── entity/
│   └── IronBoat.kt           # Custom entity extending Boat
├── item/
│   ├── THCItems.kt           # Add IRON_BOAT registration
│   └── IronBoatItem.kt       # Custom item to spawn entity
src/main/java/thc/mixin/
├── IronBoatDamageMixin.java  # Override hurt() for damage immunity
└── client/
    └── IronBoatRendererMixin.java  # Texture override (client)
src/main/resources/
├── assets/thc/
│   ├── items/iron_boat.json         # Item model definition
│   ├── models/item/iron_boat.json   # Item model (icon texture)
│   ├── textures/item/iron_boat.png      # Entity texture (ALREADY EXISTS)
│   └── textures/item/iron_boat_icon.png # Inventory icon (ALREADY EXISTS)
├── data/thc/recipe/iron_boat.json   # Crafting recipe
└── assets/thc/lang/en_us.json       # Add translation
```

### Pattern 1: Custom Entity Extending Boat

**What:** Create IronBoat class that extends Boat with lava support
**When to use:** When behavior is mostly vanilla with specific overrides
**Example:**
```kotlin
// Based on TheObsidianBoat mod pattern
class IronBoat(type: EntityType<out Boat>, level: Level) : Boat(type, level) {

    // Override to check lava instead of/in addition to water
    override fun checkInWater(): Boolean {
        // Check both water AND lava
        val waterResult = super.checkInWater()
        if (waterResult) return true

        // Additional lava check
        val aabb = boundingBox
        val minX = Mth.floor(aabb.minX)
        val maxX = Mth.ceil(aabb.maxX)
        // ... scan for FluidTags.LAVA
        return foundLava
    }

    // Fire immunity
    override fun fireImmune(): Boolean = true
    override fun isOnFire(): Boolean = false
}
```

### Pattern 2: Entity Type Registration (Fabric)

**What:** Register custom entity type with Fabric
**When to use:** Any custom entity
**Example:**
```kotlin
// In THCEntities.kt
object THCEntities {
    val IRON_BOAT: EntityType<IronBoat> = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("thc", "iron_boat"),
        EntityType.Builder.create(::IronBoat, MobCategory.MISC)
            .sized(1.375f, 0.5625f)  // Same as vanilla boat
            .build("iron_boat")
    )

    fun init() {
        // Registration happens at object init
    }
}
```

### Pattern 3: Custom Item with Entity Spawn

**What:** Item that spawns boat entity on use
**When to use:** Boat-like placeable entities
**Example:**
```kotlin
// Similar to vanilla BoatItem
class IronBoatItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val boat = IronBoat(THCEntities.IRON_BOAT, level)
            boat.setPos(hitResult.location.x, hitResult.location.y, hitResult.location.z)
            // ... setup boat
            if (!level.isClientSide) {
                level.addFreshEntity(boat)
            }
            stack.shrink(1)
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
        }
        return InteractionResultHolder.pass(stack)
    }
}
```

### Pattern 4: Fire-Resistant Item Drop

**What:** Register item with DAMAGE_RESISTANT component for fire/lava immunity
**When to use:** Items that should survive lava
**Example:**
```kotlin
// In THCItems.kt registration
val IRON_BOAT: Item = register("iron_boat") { key ->
    IronBoatItem(
        Item.Properties()
            .setId(key)
            .stacksTo(1)
            .component(DataComponents.DAMAGE_RESISTANT,
                DamageResistant(DamageTypeTags.IS_FIRE))
    )
}
```

### Pattern 5: Initial Velocity Toward Player on Drop

**What:** When boat is destroyed, give dropped item initial velocity toward player
**When to use:** Item should fly toward breaker
**Example:**
```kotlin
// In IronBoat.dropItem or via mixin
override fun destroy(source: DamageSource) {
    val item = ItemEntity(level(), x, y, z, ItemStack(THCItems.IRON_BOAT))

    // Calculate direction to player who broke it
    val attacker = source.entity
    if (attacker != null) {
        val direction = attacker.position().subtract(position()).normalize()
        val speed = 0.5  // Initial speed
        item.setDeltaMovement(direction.x * speed, 0.3, direction.z * speed)
    }

    level().addFreshEntity(item)
}
```

### Anti-Patterns to Avoid

- **Modifying vanilla Boat class directly via mixin:** Creates compatibility issues; custom subclass is cleaner
- **Using NBT for fire resistance:** MC 1.21+ uses DataComponents; NBT is legacy
- **Hardcoding damage type checks:** Use DamageTypeTags for maintainability
- **Forgetting hurtMarked sync:** When setting velocity, must set `hurtMarked = true` for client sync

## Don't Hand-Roll

Problems with existing solutions in vanilla or established patterns:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Boat physics | Custom physics system | Extend vanilla Boat | Boat has complex buoyancy, friction, collision already |
| Fire-resistant items | Custom ItemEntity damage handling | DataComponents.DAMAGE_RESISTANT | MC 1.21+ standard pattern |
| Fluid level detection | Manual block scanning | FluidState.getHeight() | Vanilla handles edge cases |
| Entity registration | Manual registry manipulation | EntityType.Builder + Registry.register | Fabric standard pattern |

**Key insight:** Vanilla Boat already handles water physics perfectly. The only change needed is to make the fluid-checking methods also accept lava. Don't rewrite physics.

## Common Pitfalls

### Pitfall 1: Passengers Burning in Lava
**What goes wrong:** Player riding iron boat still takes lava damage
**Why it happens:** Boat entity is immune, but passenger damage is separate
**How to avoid:** Either give passengers fire resistance effect while riding, or mixin to passenger damage checks
**Warning signs:** Testing shows boat survives but player dies

### Pitfall 2: Boat Sinks Instead of Floats on Lava
**What goes wrong:** Boat entity falls through lava surface
**Why it happens:** Only overriding checkInWater() but not getWaterLevelAbove() and related methods
**How to avoid:** Override ALL fluid-related methods: checkInWater(), getWaterLevelAbove(), isUnderwater(), floatBoat()
**Warning signs:** Boat visually enters lava but continues falling

### Pitfall 3: Client-Server Desync on Entity Spawn
**What goes wrong:** Boat appears then disappears, or renders in wrong position
**Why it happens:** Entity spawned only on server, or spawn packet not sent
**How to avoid:** Use level.addFreshEntity() which handles both sides; ensure EntityType has proper tracking
**Warning signs:** Boat visible briefly then vanishes; boat position jitters

### Pitfall 4: Item Not Fire-Resistant
**What goes wrong:** Dropped iron boat item burns in lava
**Why it happens:** DAMAGE_RESISTANT component not set, or using wrong damage type tag
**How to avoid:** Verify component is set in Item.Properties; use DamageTypeTags.IS_FIRE (includes lava)
**Warning signs:** Item entity burns when thrown in lava

### Pitfall 5: Custom Entity Renderer Not Registered
**What goes wrong:** Boat is invisible or renders as missing texture
**Why it happens:** EntityRendererRegistry not called on client, or wrong model layer
**How to avoid:** Register renderer in ClientModInitializer; ensure texture path matches
**Warning signs:** "Missing texture" purple/black, or completely invisible entity

## Code Examples

### Entity Registration with Fabric
```kotlin
// THCEntities.kt
object THCEntities {
    val IRON_BOAT: EntityType<IronBoat> = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("thc", "iron_boat"),
        EntityType.Builder.create(::IronBoat, MobCategory.MISC)
            .sized(1.375f, 0.5625f)
            .build("iron_boat")
    )

    fun init() {
        // Called from THC.onInitialize()
    }
}
```

### Crafting Recipe (Minecart Shape)
```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "I": "minecraft:iron_ingot",
    "M": "minecraft:magma_cream"
  },
  "pattern": [
    "I I",
    "IMI",
    "III"
  ],
  "result": {
    "count": 1,
    "id": "thc:iron_boat"
  }
}
```

### Damage Immunity Override
```java
// In IronBoat entity or via mixin
@Override
public boolean hurt(DamageSource source, float amount) {
    // Only allow player attack damage
    if (source.getEntity() instanceof Player) {
        return super.hurt(source, amount);
    }
    // Block: lava, fire, mob attacks, cacti, collisions
    return false;
}
```

### Item Model Definition
```json
// assets/thc/models/item/iron_boat.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "thc:item/iron_boat_icon"
  }
}
```

### Client Renderer Registration (Fabric)
```kotlin
// In THCClient.kt (ClientModInitializer)
object THCClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(THCEntities.IRON_BOAT) { context ->
            IronBoatRenderer(context)
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Item.Properties.fireResistant() | DataComponents.DAMAGE_RESISTANT | MC 1.21 | Must use component system |
| NBT for item data | DataComponents | MC 1.20.5+ | All item properties are components |
| ResourceLocation | Identifier | MC 1.21.11 Fabric | API renamed |

**Deprecated/outdated:**
- `Item.Properties.fireResistant()` - replaced by DAMAGE_RESISTANT component
- Direct NBT manipulation for item properties - use DataComponents

## Open Questions

1. **Passenger Fire Protection**
   - What we know: Boat can be immune, passengers can still burn
   - What's unclear: Best approach - effect vs damage cancellation mixin
   - Recommendation: Apply Fire Resistance effect to passengers while seated in iron boat

2. **Entity Texture vs Item Texture**
   - What we know: Iron boat needs entity texture for riding AND item icon for inventory
   - What's unclear: Whether vanilla BoatRenderer can be extended or needs full replacement
   - Recommendation: Start with custom renderer using iron_boat.png; may need client mixin if renderer architecture changed

3. **Item Floating on Lava Surface**
   - What we know: Netherite items float on lava
   - What's unclear: Whether DAMAGE_RESISTANT alone causes floating, or if additional logic needed
   - Recommendation: Test DAMAGE_RESISTANT first; if item sinks, may need ItemEntity mixin for buoyancy

## Sources

### Primary (HIGH confidence)
- Existing THC codebase patterns (THCItems.kt, FoodStatsModifier.kt, entity events)
- [Fabric Wiki - Creating an Entity](https://wiki.fabricmc.net/tutorial:entity) - Entity registration patterns

### Secondary (MEDIUM confidence)
- [TheObsidianBoat GitHub](https://github.com/nanite/TheObsidianBoat/blob/1.19/src/main/java/com/unrealdinnerbone/obsidianboat/entity/ObsidianBoatEntity.java) - Lava boat implementation reference (MC 1.19, may need updates)
- [Minecraft Wiki - Data component format/damage_resistant](https://minecraft.wiki/w/Data_component_format/damage_resistant) - DAMAGE_RESISTANT component
- [Minecraft Wiki - Damage type tag (Java Edition)](https://minecraft.wiki/w/Damage_type_tag_(Java_Edition)) - IS_FIRE tag contents
- [Fabric Documentation - Custom Data Components](https://docs.fabricmc.net/develop/items/custom-data-components) - DataComponents in Fabric

### Tertiary (LOW confidence)
- WebSearch results for boat physics and entity rendering - general patterns, need verification
- Training data knowledge of Boat class internals - verify against actual decompiled source

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM - DataComponents pattern verified, entity registration pattern from codebase
- Architecture: MEDIUM - Based on TheObsidianBoat mod (1.19) + THC patterns, may need MC 1.21.11 adjustments
- Pitfalls: MEDIUM - Based on common mod development issues and search results

**Research date:** 2026-01-25
**Valid until:** 30 days (stable domain, entity API relatively stable in MC 1.21.x)
