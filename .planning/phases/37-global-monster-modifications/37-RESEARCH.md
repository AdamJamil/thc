# Phase 37: Global Monster Modifications - Research

**Researched:** 2026-01-23
**Domain:** Minecraft 1.21.11 entity attributes, Fabric ServerEntityEvents, loot table modification
**Confidence:** HIGH

## Summary

Phase 37 implements global hostile mob modifications through four independent systems: (1) movement speed increase via attribute modifiers, (2) equipment drop removal via loot event interception, (3) baby zombie speed normalization via counter-modifier, and (4) iron ingot drop removal via loot filtering. All systems integrate with THC's existing mixin and event-driven architecture.

**Key finding:** THC already has established patterns for all required modifications. ServerPlayerMixin demonstrates attribute modification via `getAttribute(Attributes.MAX_HEALTH)`. THC.kt shows loot filtering via `LootTableEvents.MODIFY_DROPS` with `removedItems` set pattern. The v2.3 research validates ServerEntityEvents.ENTITY_LOAD for entity-wide attribute modification.

**Primary recommendation:** Use ServerEntityEvents.ENTITY_LOAD for speed modifications (one registration point, catches all spawns), extend existing LootTableEvents.MODIFY_DROPS handler for equipment/ingot removal, and apply baby zombie counter-modifier during entity load event.

## Standard Stack

### Core Libraries (Already in THC)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.141.0+1.21.11 | ServerEntityEvents.ENTITY_LOAD | Standard Fabric event system for entity lifecycle |
| Fabric API Loot v3 | 0.141.0+1.21.11 | LootTableEvents.MODIFY_DROPS | Standard loot table modification API |
| Mojang Mappings | 1.21.11 | Attributes.MOVEMENT_SPEED, AttributeModifier | Official Minecraft attribute system |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Mixin | 0.8.7 | (Not needed for this phase) | Only if event-based approach insufficient |

**Installation:**
Already installed. No new dependencies required.

## Architecture Patterns

### Recommended Project Structure

```
src/main/kotlin/thc/
├── THC.kt                    # Extend existing loot event handler
└── monster/
    └── MonsterModifications.kt   # New: speed & baby zombie normalization
```

### Pattern 1: Entity Attribute Modification via ServerEntityEvents.ENTITY_LOAD

**What:** Apply attribute modifiers when entities load into the world (spawn, chunk load, respawn)

**When to use:** For modifications that affect ALL instances of entity types, applied at load time

**Example:**
```kotlin
// Source: v2.3 STACK.md, existing ServerPlayerMixin.java pattern
ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
    if (entity !is Mob) return@register
    if (entity.type.category != MobCategory.MONSTER) return@register

    // Exclusions
    if (entity is Creeper) return@register  // FR-01 exclusion
    if (entity is Zombie && entity.isBaby) return@register  // FR-01, FR-04 exclusion
    if (entity is EnderDragon || entity is Wither) return@register  // Pitfall MOB-02

    // Apply 20% speed increase
    val speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED) ?: return@register
    val modifierId = ResourceLocation.fromNamespaceAndPath("thc", "monster_speed_boost")

    if (!speedAttr.hasModifier(modifierId)) {
        speedAttr.addTransientModifier(
            AttributeModifier(
                modifierId,
                0.2,  // 20% increase
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE  // Multiplies base by (1 + 0.2)
            )
        )
    }
}
```

**Key points:**
- Transient modifiers don't persist to disk (no save bloat)
- `hasModifier()` check prevents duplicate application
- `ADD_MULTIPLIED_BASE` operation: `final = base * (1 + 0.2)` = 20% increase
- ResourceLocation as ID prevents UUID collisions (Pitfall MOB-10)

### Pattern 2: Baby Zombie Speed Normalization

**What:** Remove vanilla BABY_SPEED_BONUS modifier or apply counter-modifier to normalize baby zombie speed to adult speed

**When to use:** For FR-04 baby zombie normalization requirement

**Example:**
```kotlin
// In same ServerEntityEvents.ENTITY_LOAD handler
ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
    if (entity is Zombie && entity.isBaby) {
        val speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED) ?: return@register

        // Vanilla applies 1.5x speed via "minecraft:baby" modifier
        // Counter with -0.5 ADD_MULTIPLIED_BASE to normalize
        val normalizeId = ResourceLocation.fromNamespaceAndPath("thc", "baby_zombie_normalize")

        if (!speedAttr.hasModifier(normalizeId)) {
            speedAttr.addTransientModifier(
                AttributeModifier(
                    normalizeId,
                    -0.5,  // Counters vanilla +0.5 modifier
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            )
        }
        // Result: base * (1 + 0.5 - 0.5) = base * 1.0 = normalized speed
    }
}
```

**Alternative approach:** Remove vanilla modifier by ID
```kotlin
// If vanilla modifier ID is known (needs verification in decompiled source)
val vanillaBabyModifier = ResourceLocation.fromNamespaceAndPath("minecraft", "baby")
speedAttr.removeModifier(vanillaBabyModifier)
```

### Pattern 3: Loot Drop Filtering via LootTableEvents.MODIFY_DROPS

**What:** Remove items from entity drop lists during loot generation

**When to use:** For FR-02 equipment removal and FR-05 iron ingot removal

**Example:**
```kotlin
// Source: Existing THC.kt lines 99-105
// Extend existing handler, don't create new registration
LootTableEvents.MODIFY_DROPS.register { _, _, drops ->
    // Existing THC logic for totem replacement
    val hadTotem = drops.any { it.`is`(Items.TOTEM_OF_UNDYING) }

    // FR-02: Equipment removal (armor, weapons)
    // FR-05: Iron ingot removal
    val monsterDropRemovals = setOf(
        // Equipment (from zombies, skeletons)
        Items.LEATHER_HELMET,
        Items.LEATHER_CHESTPLATE,
        Items.LEATHER_LEGGINGS,
        Items.LEATHER_BOOTS,
        Items.CHAINMAIL_HELMET,
        Items.CHAINMAIL_CHESTPLATE,
        Items.CHAINMAIL_LEGGINGS,
        Items.CHAINMAIL_BOOTS,
        Items.IRON_HELMET,
        Items.IRON_CHESTPLATE,
        Items.IRON_LEGGINGS,
        Items.IRON_BOOTS,
        Items.GOLDEN_HELMET,
        Items.GOLDEN_CHESTPLATE,
        Items.GOLDEN_LEGGINGS,
        Items.GOLDEN_BOOTS,
        Items.DIAMOND_HELMET,
        Items.DIAMOND_CHESTPLATE,
        Items.DIAMOND_LEGGINGS,
        Items.DIAMOND_BOOTS,
        Items.WOODEN_SWORD,
        Items.STONE_SWORD,
        Items.IRON_SWORD,
        Items.GOLDEN_SWORD,
        Items.DIAMOND_SWORD,
        Items.BOW,  // Skeleton drops (already removed by ranged gating, but defensive)

        // FR-05: Zombie/husk iron ingots
        Items.IRON_INGOT
    )

    drops.removeIf { stack -> monsterDropRemovals.any { stack.`is`(it) } }

    // Existing totem replacement
    if (hadTotem) {
        drops.add(THCItems.BLAST_TOTEM.defaultInstance)
    }
}
```

**Key points:**
- Extends existing handler in THC.kt, don't create duplicate registration
- `removeIf` with `any` checks each stack against removal set
- Includes armor AND weapons (FR-02 comprehensive coverage)
- Iron ingot covers both zombie and husk drops (FR-05)

### Anti-Patterns to Avoid

- **Don't create separate ENTITY_LOAD registrations per entity type** - Single handler with type checks is cleaner
- **Don't use persistent modifiers for global changes** - Transient modifiers prevent save file bloat (Pitfall MOB-07)
- **Don't modify loot tables via datapack override** - Event-based filtering integrates with other mods, datapack override conflicts
- **Don't apply speed boost to baby zombies** - They're already 1.5x faster, stacking makes them unbeatable (Pitfall MOB-01)
- **Don't forget boss exclusions** - Wither and EnderDragon have hardcoded behaviors that break with speed changes (Pitfall MOB-02)

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Entity attribute modification | Custom NBT speed tracking | AttributeModifier system | Vanilla system handles operation types, stacking, persistence |
| Loot table filtering | Mixin to Mob.dropAllDeathLoot() | LootTableEvents.MODIFY_DROPS | Event-based approach compatible with other mods |
| Baby zombie detection | NBT tag checking | `instanceof Zombie && isBaby()` | Type-safe, works with modded zombies |
| Equipment slot checking | Manual ItemStack iteration | Built-in drops list in event | Fabric event provides mutable list |

**Key insight:** Minecraft's attribute system already handles all edge cases: operation order, modifier stacking, UUID collisions, persistence control. Building custom speed tracking duplicates hundreds of lines of vanilla code and breaks mod compatibility.

## Common Pitfalls

### Pitfall 1: Baby Zombie Speed Stacking (MOB-01)

**What goes wrong:** Applying 20% speed boost to baby zombies makes them absurdly fast. Vanilla baby zombies are already 1.5x adult speed. Your boost stacks: `base * 1.5 * 1.2 = 1.8x adult speed`, making them nearly impossible to hit.

**Why it happens:** Baby zombie speed is applied via attribute modifier in vanilla `Zombie` class. Your modifier stacks multiplicatively with vanilla's `minecraft:baby` modifier.

**How to avoid:**
```kotlin
// Explicit exclusion in speed boost handler
if (entity is Zombie && entity.isBaby) return@register
```

**Warning signs:**
- Baby zombies outrun sprinting players
- Early game becomes impossible
- Combat testing reveals unbeatable encounters

**Source:** [Pitfall MOB-01 in v2.3 PITFALLS.md](https://minecraft.wiki/w/Attribute) - modifier stacking behavior

### Pitfall 2: Boss Movement Speed Modification (MOB-02)

**What goes wrong:** Wither and EnderDragon have hardcoded behaviors assuming specific movement speeds. Speed changes cause:
- Wither charge attack animation desync
- EnderDragon flight path calculation errors (doesn't use movement attributes for flight)
- Boss fight progression breaking

**Why it happens:** Boss entities have complex AI with timing assumptions. EnderDragon flight is mathematically calculated, not attribute-based.

**How to avoid:**
```kotlin
// Exclude all boss entities
if (entity is EnderDragon || entity is Wither) return@register
// Also consider mini-bosses if present
if (entity is ElderGuardian) return@register
```

**Warning signs:**
- Wither charge doesn't match visual
- Dragon flight becomes erratic
- Boss fights fail to progress phases

**Source:** [Pitfall MOB-02 in v2.3 PITFALLS.md](https://minecraft.wiki/w/Boss)

### Pitfall 3: Attribute Modifier UUID Collision (MOB-10)

**What goes wrong:** Using random UUIDs for each modifier application causes stacking. Using hardcoded UUID strings risks conflicts with other mods.

**Why it happens:** AttributeModifier uses UUID/ResourceLocation to identify uniqueness. Same ID = same modifier = no stacking (correct). Random ID = new modifier = stacking (wrong).

**How to avoid:**
```kotlin
// Use ResourceLocation with mod namespace
val modifierId = ResourceLocation.fromNamespaceAndPath("thc", "monster_speed_boost")
// Check before applying
if (!speedAttr.hasModifier(modifierId)) {
    speedAttr.addTransientModifier(AttributeModifier(modifierId, 0.2, Operation.ADD_MULTIPLIED_BASE))
}
```

**Warning signs:**
- Inconsistent speed between mobs of same type
- Attribute debug shows multiple modifiers
- Speed values vary on reload

**Source:** [Pitfall MOB-10 in v2.3 PITFALLS.md](https://minecraft.wiki/w/Attribute)

### Pitfall 4: Equipment Drop Removal Incomplete (FR-02)

**What goes wrong:** Only removing weapon drops but not armor, or vice versa. Zombies/skeletons can spawn with full armor sets AND weapons.

**Why it happens:** Mob equipment includes 5 slots: MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET. Vanilla populates multiple slots based on difficulty.

**How to avoid:**
```kotlin
// Comprehensive removal set covering ALL equipment types
val equipmentRemovals = setOf(
    // All armor tiers
    Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
    Items.CHAINMAIL_HELMET, /* ... full chainmail set ... */
    Items.IRON_HELMET, /* ... full iron set ... */
    Items.GOLDEN_HELMET, /* ... full gold set ... */
    Items.DIAMOND_HELMET, /* ... full diamond set ... */
    // All weapon types mobs can spawn with
    Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
    Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.BOW
)
```

**Warning signs:**
- Killing 50 zombies still yields occasional helmet drops
- Skeletons drop bows despite weapon removal intent
- Acceptance criteria fails: "Zero armor/weapon drops"

### Pitfall 5: Creeper Exclusion Type Checking (MOB-09)

**What goes wrong:** Using exact type matching misses charged creepers or modded creeper variants.

**Why it happens:** Charged creeper is a state, not a subtype. Modded creepers may extend vanilla Creeper class.

**How to avoid:**
```kotlin
// Use instanceof to catch all Creeper subclasses
if (entity is Creeper) return@register  // Catches vanilla + charged + modded variants

// WRONG: Exact type check
if (entity.type == EntityType.CREEPER) { /* misses subclasses */ }
```

**Warning signs:**
- Charged creepers move faster than normal creepers
- Mod-added creeper variants affected inconsistently

## Code Examples

Verified patterns from THC codebase and Fabric documentation:

### Monster Speed Increase (FR-01)

```kotlin
// In new file: src/main/kotlin/thc/monster/MonsterModifications.kt
package thc.monster

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Zombie

object MonsterModifications {
    private val SPEED_BOOST_ID = ResourceLocation.fromNamespaceAndPath("thc", "monster_speed_boost")
    private val BABY_NORMALIZE_ID = ResourceLocation.fromNamespaceAndPath("thc", "baby_zombie_normalize")

    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity !is Mob) return@register
            if (entity.type.category != MobCategory.MONSTER) return@register

            // FR-01: 20% speed increase with exclusions
            applySpeedBoost(entity)

            // FR-04: Baby zombie normalization
            normalizeBabyZombieSpeed(entity)
        }
    }

    private fun applySpeedBoost(mob: Mob) {
        // Exclusions: Creepers, baby zombies, bosses
        if (mob is Creeper) return
        if (mob is Zombie && mob.isBaby) return
        if (mob is EnderDragon || mob is WitherBoss) return

        val speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED) ?: return

        if (!speedAttr.hasModifier(SPEED_BOOST_ID)) {
            speedAttr.addTransientModifier(
                AttributeModifier(
                    SPEED_BOOST_ID,
                    0.2,  // 20% increase
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            )
        }
    }

    private fun normalizeBabyZombieSpeed(mob: Mob) {
        if (mob !is Zombie || !mob.isBaby) return

        val speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED) ?: return

        // Counter vanilla +0.5 modifier with -0.5
        if (!speedAttr.hasModifier(BABY_NORMALIZE_ID)) {
            speedAttr.addTransientModifier(
                AttributeModifier(
                    BABY_NORMALIZE_ID,
                    -0.5,  // Counters vanilla minecraft:baby modifier
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
            )
        }
    }
}
```

### Loot Drop Removal (FR-02, FR-05)

```kotlin
// Extend existing LootTableEvents.MODIFY_DROPS in THC.kt
// Add to existing removedItems set:
val removedItems = setOf(
    Items.SHIELD,
    Items.WOODEN_SPEAR,
    // ... existing items ...
    Items.TOTEM_OF_UNDYING,

    // FR-02: Equipment drops
    Items.LEATHER_HELMET,
    Items.LEATHER_CHESTPLATE,
    Items.LEATHER_LEGGINGS,
    Items.LEATHER_BOOTS,
    Items.CHAINMAIL_HELMET,
    Items.CHAINMAIL_CHESTPLATE,
    Items.CHAINMAIL_LEGGINGS,
    Items.CHAINMAIL_BOOTS,
    Items.IRON_HELMET,
    Items.IRON_CHESTPLATE,
    Items.IRON_LEGGINGS,
    Items.IRON_BOOTS,
    Items.GOLDEN_HELMET,
    Items.GOLDEN_CHESTPLATE,
    Items.GOLDEN_LEGGINGS,
    Items.GOLDEN_BOOTS,
    Items.DIAMOND_HELMET,
    Items.DIAMOND_CHESTPLATE,
    Items.DIAMOND_LEGGINGS,
    Items.DIAMOND_BOOTS,
    Items.WOODEN_SWORD,
    Items.STONE_SWORD,
    Items.IRON_SWORD,
    Items.GOLDEN_SWORD,
    Items.DIAMOND_SWORD,
    Items.BOW,

    // FR-05: Iron ingot from zombies/husks
    Items.IRON_INGOT
)
// Existing MODIFY_DROPS handler already processes this set
```

### Registration in THC.kt

```kotlin
// In THC.kt onInitialize(), add:
MonsterModifications.register()
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Mixin to each entity type | ServerEntityEvents.ENTITY_LOAD | Fabric API 0.14+ (2020) | Single registration point, catches all spawns |
| Datapack loot table override | LootTableEvents.MODIFY_DROPS | Fabric API 0.79+ (2023) | Event-based compatible with other mods |
| Persistent attribute modifiers | Transient modifiers | Minecraft 1.16+ | No save file bloat |
| UUID for modifier IDs | ResourceLocation | Minecraft 1.21+ | Namespace collision prevention |

**Deprecated/outdated:**
- `EntityAttributeModifier(UUID, ...)` constructor - use `AttributeModifier(ResourceLocation, ...)` in 1.21+
- LootTableEvents v2 - use v3 API in Fabric API 0.129+ for 1.21.7+

## Open Questions

Things that couldn't be fully resolved:

1. **Baby Zombie Counter-Modifier Effectiveness**
   - What we know: Vanilla applies `minecraft:baby` modifier with +0.5 ADD_MULTIPLIED_BASE
   - What's unclear: Whether counter-modifier stacks correctly or if vanilla modifier should be removed directly
   - Recommendation: Implement counter-modifier first (simpler), test speed equality, fall back to modifier removal if counter doesn't work

2. **Charged Creeper Speed Exclusion**
   - What we know: Charged creeper is state data, not entity subtype
   - What's unclear: Whether `entity is Creeper` catches charged state correctly
   - Recommendation: Test with `/summon creeper ~ ~ ~ {powered:1b}`, verify speed unchanged

## Sources

### Primary (HIGH confidence)

- [Fabric API ServerEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.81.1+1.20/net/fabricmc/fabric/api/event/lifecycle/v1/ServerEntityEvents.html) - ENTITY_LOAD event signature
- [Fabric API LootTableEvents v3](https://maven.fabricmc.net/docs/fabric-api-0.129.0+1.21.7/net/fabricmc/fabric/api/loot/v3/LootTableEvents.html) - MODIFY_DROPS event
- [Minecraft Wiki - Attribute](https://minecraft.wiki/w/Attribute) - movement_speed attribute, modifier operations, baby zombie speed
- THC ServerPlayerMixin.java - Attribute modification pattern (`getAttribute(Attributes.MAX_HEALTH)`)
- THC.kt lines 99-105 - Loot filtering pattern with removedItems set
- v2.3 STACK.md - ServerEntityEvents.ENTITY_LOAD implementation pattern

### Secondary (MEDIUM confidence)

- [Fabric Documentation - Entity Attributes](https://docs.fabricmc.net/develop/entities/attributes) - General attribute usage
- [Fabric Wiki - Adding to Loot Tables](https://wiki.fabricmc.net/tutorial:adding_to_loot_tables) - Loot modification examples
- v2.3 PITFALLS.md - MOB-01 through MOB-14 verified pitfalls

### Tertiary (LOW confidence)

- Baby zombie modifier removal approach - needs testing to verify counter-modifier vs direct removal

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in THC, versions verified in gradle.properties
- Architecture: HIGH - Patterns directly from THC codebase (ServerPlayerMixin, THC.kt)
- Speed modification: HIGH - ServerEntityEvents.ENTITY_LOAD pattern verified in v2.3 research
- Loot filtering: HIGH - Exact pattern exists in THC.kt, just extend removedItems set
- Baby zombie normalization: MEDIUM - Counter-modifier approach logical but needs testing

**Research date:** 2026-01-23
**Valid until:** 2026-03-23 (60 days - Minecraft 1.21.x stable, Fabric API stable)
