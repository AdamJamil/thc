# Phase 48: Copper Bucket - Research

**Researched:** 2026-01-25
**Domain:** Custom bucket item with fluid restrictions, entity interaction handling
**Confidence:** HIGH

## Summary

Implementing the copper bucket requires creating three separate custom items (empty copper bucket, copper bucket of water, copper bucket of milk) that mimic vanilla bucket behavior with fluid restrictions. The core technical challenges are: (1) preventing lava/powder snow pickup by overriding the BucketItem `use()` method to return FAIL for restricted fluids, (2) handling cow milking through UseEntityCallback to replace vanilla bucket with copper bucket, and (3) creating separate filled bucket items since Minecraft treats different bucket states as distinct items rather than NBT variants.

The recommended approach is to create a custom CopperBucketItem extending Item (not BucketItem) with overridden `use()` that checks the target fluid before allowing pickup. For cow milking, register a UseEntityCallback that intercepts the interaction and replaces empty copper buckets with copper milk buckets. Water placement uses the same pattern as vanilla water buckets - override `use()` to place water sources.

**Primary recommendation:** Create three items (copper_bucket, copper_bucket_of_water, copper_bucket_of_milk), override `use()` in CopperBucketItem to check FluidState at target position and only proceed for water, use UseEntityCallback for cow milking to create copper milk bucket variant, and make filled buckets consumable/placeable with appropriate behavior.

## Standard Stack

The copper bucket implementation uses existing THC patterns plus Minecraft vanilla item architecture.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Custom Item class | MC 1.21.11 | Bucket items with restricted behavior | Vanilla BucketItem can't restrict fluids easily |
| Fabric UseEntityCallback | Fabric API | Cow milking interaction | Official Fabric event for entity interactions |
| Item.Properties | MC 1.21.11 | Item registration with stack size, remainder | Standard Minecraft item system |
| FluidState API | MC 1.21.11 | Fluid detection at block position | Vanilla fluid system for checking water/lava |

### Supporting
| Library | Purpose | When to Use |
|---------|---------|-------------|
| ClipContext.Fluid | Ray tracing to fluid surface | Finding where player is looking for bucket placement |
| InteractionResult | Return values for use() | Controlling client-server behavior and animation |
| Items.BUCKET as recipeRemainder | Empty bucket on consumption | Drinking milk or crafting with milk bucket |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Three separate items | Single item with NBT | Minecraft uses separate items; NBT approach non-standard |
| Custom use() override | Extend BucketItem + mixin | BucketItem hardcoded for specific fluids; custom cleaner |
| UseEntityCallback | Mixin to Cow.mobInteract | Callback is official Fabric API, mixin more invasive |

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/thc/
├── item/
│   ├── THCItems.kt                    # Add copper bucket registrations
│   ├── CopperBucketItem.kt            # Empty copper bucket
│   ├── CopperWaterBucketItem.kt       # Water-filled variant
│   └── CopperMilkBucketItem.kt        # Milk-filled variant
src/main/kotlin/thc/
├── THC.kt                             # Wire up UseEntityCallback
src/main/resources/
├── data/thc/recipe/
│   ├── copper_bucket.json             # Crafting recipe
└── assets/thc/
    ├── lang/en_us.json                # Translations
    ├── models/item/
    │   ├── copper_bucket.json         # Item models
    │   ├── copper_bucket_of_water.json
    │   └── copper_bucket_of_milk.json
    └── textures/item/
        ├── copper_bucket.png          # Item textures
        ├── copper_bucket_of_water.png
        └── copper_bucket_of_milk.png
```

### Pattern 1: Custom Bucket Item with Fluid Restriction

**What:** Override `use()` to check fluid type before allowing pickup
**When to use:** Bucket that works with some fluids but not others
**Example:**
```kotlin
// CopperBucketItem.kt - Empty copper bucket
class CopperBucketItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)

        // Ray trace to find target block
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockPos = hitResult.blockPos
            val fluidState = level.getFluidState(blockPos)

            // Check if it's water (allowed)
            if (fluidState.is(FluidTags.WATER) && fluidState.isSource) {
                if (!level.isClientSide) {
                    // Remove water source block
                    level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11)

                    // Replace bucket with water bucket
                    stack.shrink(1)
                    val waterBucket = ItemStack(THCItems.COPPER_BUCKET_OF_WATER)
                    if (!player.inventory.add(waterBucket)) {
                        player.drop(waterBucket, false)
                    }

                    // Play sound
                    level.playSound(null, player.x, player.y, player.z,
                        SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f)
                }
                return InteractionResult.sidedSuccess(level.isClientSide)
            }

            // Lava, powder snow, other fluids - silent fail
            if (fluidState.is(FluidTags.LAVA) || !fluidState.isEmpty) {
                return InteractionResult.FAIL
            }
        }

        return InteractionResult.PASS
    }
}
```

### Pattern 2: Water Bucket Placement

**What:** Place water source blocks from filled copper bucket
**When to use:** Consumable bucket that places fluid
**Example:**
```kotlin
// CopperWaterBucketItem.kt
class CopperWaterBucketItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockPos = hitResult.blockPos
            val targetPos = blockPos.relative(hitResult.direction)

            if (level.mayInteract(player, blockPos) && player.mayUseItemAt(targetPos, hitResult.direction, stack)) {
                if (!level.isClientSide) {
                    // Place water
                    level.setBlock(targetPos, Blocks.WATER.defaultBlockState(), 11)

                    // Return empty copper bucket
                    stack.shrink(1)
                    val emptyBucket = ItemStack(THCItems.COPPER_BUCKET)
                    if (!player.inventory.add(emptyBucket)) {
                        player.drop(emptyBucket, false)
                    }

                    // Play sound
                    level.playSound(null, player.x, player.y, player.z,
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f)
                }
                return InteractionResult.sidedSuccess(level.isClientSide)
            }
        }

        return InteractionResult.PASS
    }
}
```

### Pattern 3: Milk Bucket Consumption

**What:** Drinkable milk bucket using vanilla finish-using mechanics
**When to use:** Consumable bucket that requires drinking animation
**Example:**
```kotlin
// CopperMilkBucketItem.kt
class CopperMilkBucketItem(properties: Properties) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        player.startUsingItem(hand)
        return InteractionResult.CONSUME
    }

    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        if (entity is ServerPlayer) {
            // Remove all status effects (vanilla milk behavior)
            entity.removeAllEffects()
        }

        // Return empty copper bucket
        return if (entity is Player && !entity.abilities.instabuild) {
            stack.shrink(1)
            if (stack.isEmpty) {
                ItemStack(THCItems.COPPER_BUCKET)
            } else {
                if (!entity.inventory.add(ItemStack(THCItems.COPPER_BUCKET))) {
                    entity.drop(ItemStack(THCItems.COPPER_BUCKET), false)
                }
                stack
            }
        } else {
            stack
        }
    }

    override fun getUseAnimation(stack: ItemStack): ItemUseAnimation {
        return ItemUseAnimation.DRINK
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return 32  // Same as vanilla milk bucket
    }
}
```

### Pattern 4: Cow Milking with UseEntityCallback

**What:** Intercept cow interaction to create copper milk bucket
**When to use:** Custom bucket should work with cow milking
**Example:**
```kotlin
// In THC.kt onInitialize()
UseEntityCallback.EVENT.register { player, level, hand, entity, hitResult ->
    val stack = player.getItemInHand(hand)

    // Check if player is holding empty copper bucket and targeting a cow
    if (stack.item == THCItems.COPPER_BUCKET && entity is Cow && !entity.isBaby) {
        if (!level.isClientSide) {
            // Play milking sound
            player.playSound(SoundEvents.COW_MILK, 1.0f, 1.0f)

            // Replace copper bucket with copper milk bucket
            stack.shrink(1)
            val milkBucket = ItemStack(THCItems.COPPER_BUCKET_OF_MILK)
            if (!player.inventory.add(milkBucket)) {
                player.drop(milkBucket, false)
            }
        }

        return@register InteractionResult.sidedSuccess(level.isClientSide)
    }

    InteractionResult.PASS
}
```

### Pattern 5: Three-Item Registration

**What:** Register empty, water-filled, and milk-filled variants separately
**When to use:** Minecraft's bucket model (separate items, not NBT)
**Example:**
```kotlin
// In THCItems.kt
@JvmField
val COPPER_BUCKET: Item = register("copper_bucket") { key ->
    CopperBucketItem(
        Item.Properties()
            .setId(key)
            .stacksTo(16)  // Can stack empty buckets
    )
}

@JvmField
val COPPER_BUCKET_OF_WATER: Item = register("copper_bucket_of_water") { key ->
    CopperWaterBucketItem(
        Item.Properties()
            .setId(key)
            .stacksTo(1)
            .craftRemainder(COPPER_BUCKET)  // Returns empty bucket when used in crafting
    )
}

@JvmField
val COPPER_BUCKET_OF_MILK: Item = register("copper_bucket_of_milk") { key ->
    CopperMilkBucketItem(
        Item.Properties()
            .setId(key)
            .stacksTo(1)
            .craftRemainder(COPPER_BUCKET)
    )
}
```

### Anti-Patterns to Avoid

- **Extending BucketItem for restricted buckets:** BucketItem hardcoded to specific fluids, override is difficult
- **Using single item with NBT for states:** Minecraft uses separate items for bucket variants; NBT approach breaks vanilla patterns
- **Forgetting source-only check:** `ClipContext.Fluid.SOURCE_ONLY` ensures only full source blocks picked up
- **Not handling inventory full:** Always check if `player.inventory.add()` succeeds, drop item otherwise
- **Silent success on lava:** Return `InteractionResult.FAIL` for restricted fluids to prevent animation

## Don't Hand-Roll

Problems with existing solutions in vanilla or established patterns:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bucket item system | Single item with metadata/NBT | Separate items for each state | Minecraft's design pattern, vanilla does this |
| Fluid detection | Manual block type checks | FluidState.is(FluidTags.X) | Handles modded fluids, water-loggable blocks |
| Entity interaction | Mixin to Cow.mobInteract | UseEntityCallback.EVENT | Official Fabric API, less invasive |
| Drinking animation | Custom animation system | ItemUseAnimation.DRINK + finishUsingItem | Vanilla pattern, client-side animation built-in |
| Inventory management | Custom item addition | player.inventory.add() + drop fallback | Handles full inventory, creative mode |

**Key insight:** Minecraft treats bucket states as completely separate items (water_bucket, lava_bucket, milk_bucket are distinct item IDs). Don't try to unify them with NBT - follow vanilla's pattern of separate items with `craftRemainder` pointing back to empty bucket.

## Common Pitfalls

### Pitfall 1: BucketItem Constructor Expects Fluid
**What goes wrong:** Extending BucketItem requires passing a Fluid in constructor, but we don't have a custom fluid
**Why it happens:** BucketItem designed for fluids that have flowing/still variants and fluid blocks
**How to avoid:** Don't extend BucketItem - create custom Item class with overridden `use()` method
**Warning signs:** Compiler errors about missing Fluid parameter, or null fluid causing crashes

### Pitfall 2: Forgetting to Check isSource
**What goes wrong:** Bucket picks up flowing water, not just source blocks
**Why it happens:** FluidState can be source or flowing; buckets should only pick up source
**How to avoid:** Check `fluidState.isSource` before allowing pickup
**Warning signs:** Bucket works on water streams, infinite water generation bugs

### Pitfall 3: Client-Server Desync on Item Replacement
**What goes wrong:** Bucket appears to fill on client but doesn't on server, or vice versa
**Why it happens:** Item replacement logic only runs on one side
**How to avoid:** Wrap item manipulation in `if (!level.isClientSide)` block, return `sidedSuccess(level.isClientSide)`
**Warning signs:** Items duplicate or disappear, bucket state flickers

### Pitfall 4: UseEntityCallback Return Value
**What goes wrong:** Cow milking works but player also attacks cow, or animation doesn't play
**Why it happens:** Wrong InteractionResult - PASS allows fallthrough, FAIL prevents animation
**How to avoid:** Return `sidedSuccess(level.isClientSide)` on success, `PASS` when not handling
**Warning signs:** Milking works but cow gets damaged, or arm doesn't swing

### Pitfall 5: Stack Shrink Without Checking Empty
**What goes wrong:** Empty ItemStack left in hand after bucket consumed
**Why it happens:** `stack.shrink(1)` doesn't replace stack when count reaches 0
**How to avoid:** Check `stack.isEmpty` after shrink and return replacement stack in `finishUsingItem`
**Warning signs:** Invisible item in hand after drinking milk, inventory shows 0x bucket

### Pitfall 6: Silent Fail on Lava Gives No Feedback
**What goes wrong:** Player tries to pick up lava, nothing happens, appears broken
**Why it happens:** Requirement specifies silent fail (no sound, no message)
**How to avoid:** This is correct per CONTEXT.md - document it clearly, ensure FAIL return prevents arm swing
**Warning signs:** None - this is intended behavior per user decision

## Code Examples

Verified patterns from vanilla Minecraft and Fabric:

### Crafting Recipe (Standard Bucket Pattern)
```json
// data/thc/recipe/copper_bucket.json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "C": "minecraft:copper_ingot"
  },
  "pattern": [
    "C C",
    " C "
  ],
  "result": {
    "count": 1,
    "id": "thc:copper_bucket"
  }
}
```

### Item Model Definitions
```json
// assets/thc/models/item/copper_bucket.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "thc:item/copper_bucket"
  }
}

// assets/thc/models/item/copper_bucket_of_water.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "thc:item/copper_bucket_of_water"
  }
}

// assets/thc/models/item/copper_bucket_of_milk.json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "thc:item/copper_bucket_of_milk"
  }
}
```

### Translations
```json
// assets/thc/lang/en_us.json
{
  "item.thc.copper_bucket": "Copper Bucket",
  "item.thc.copper_bucket_of_water": "Copper Bucket of Water",
  "item.thc.copper_bucket_of_milk": "Copper Bucket of Milk"
}
```

### FluidState Check Pattern
```kotlin
// Checking if target is water source
val fluidState = level.getFluidState(blockPos)
if (fluidState.is(FluidTags.WATER) && fluidState.isSource) {
    // Safe to pick up water
}

// Checking if target is lava (to reject)
if (fluidState.is(FluidTags.LAVA)) {
    return InteractionResult.FAIL  // Silent rejection
}
```

### Item Registration in Creative Tab
```kotlin
// In THCItems.kt init()
ItemGroupEvents.modifyEntriesEvent(toolsTabKey).register { entries ->
    entries.accept(COPPER_BUCKET)
    entries.accept(COPPER_BUCKET_OF_WATER)
    entries.accept(COPPER_BUCKET_OF_MILK)
    // ... other items
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Single bucket item + NBT | Separate items per state | Always (vanilla pattern) | Must create 3 items, not 1 |
| Item.Properties.recipeRemainder() | Item.Properties.craftRemainder() | MC 1.21 | Use craftRemainder in modern code |
| PlayerInteractEvent (Forge) | UseEntityCallback.EVENT (Fabric) | N/A (platform) | Fabric uses callbacks, not events |

**Deprecated/outdated:**
- Extending BucketItem for custom fluid behavior: BucketItem is final or has hardcoded fluid checks
- Using NBT to differentiate bucket states: Not the Minecraft way

## Open Questions

1. **Can copper buckets be used in cauldrons?**
   - What we know: Vanilla buckets can fill/empty cauldrons
   - What's unclear: Whether custom buckets need special handling for cauldrons
   - Recommendation: Start without cauldron support - requirements don't mention it

2. **Should copper buckets work with dispensers?**
   - What we know: Vanilla buckets can be dispensed to pick up/place fluids
   - What's unclear: Whether this is expected behavior for copper buckets
   - Recommendation: Skip dispenser support initially - not in requirements

3. **Do copper water buckets create source blocks or flowing water?**
   - What we know: Vanilla water buckets create source blocks
   - What's unclear: REQUIREMENTS.md says "copper bucket can scoop and place water (fills, empties normally)"
   - Recommendation: Create source blocks to match vanilla water bucket behavior

## Sources

### Primary (HIGH confidence)
- [Fabric Wiki - Listening to Events](https://wiki.fabricmc.net/tutorial:callbacks) - UseEntityCallback documentation
- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events) - Official event system
- [UseEntityCallback Source Code](https://github.com/FabricMC/fabric/blob/1.17/fabric-events-interaction-v0/src/main/java/net/fabricmc/fabric/api/event/player/UseEntityCallback.java) - API contract
- [Minecraft Wiki - Milk Bucket](https://minecraft.wiki/w/Milk_Bucket) - Milk bucket mechanics
- [Minecraft Wiki - Bucket](https://minecraft.wiki/w/Bucket) - General bucket mechanics
- Existing THC codebase (THCItems.kt, IronBoatItem.kt, LandPlotItem.kt) - Registration and use() patterns

### Secondary (MEDIUM confidence)
- [BucketItem Yarn 1.21.4 API](https://maven.fabricmc.net/docs/yarn-1.21.4+build.1/net/minecraft/item/BucketItem.html) - BucketItem class structure
- [Fabric Wiki - Creating a fluid](https://wiki.fabricmc.net/tutorial:fluids) - Custom bucket creation
- [Milk Bucket Mechanics](https://www.digminecraft.com/food_recipes/make_milk.php) - Milk bucket creation process
- [Cow Milking Documentation](https://www.digminecraft.com/getting_started/how_to_milk_cow.php) - Entity interaction

### Tertiary (LOW confidence)
- Various forum posts about custom buckets - General patterns verified against official docs
- Minecraft Item IDs reference - Item ID confirmation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Fabric callbacks are official API, existing THC patterns verified
- Architecture: HIGH - Three-item pattern matches vanilla, use() override is standard Item pattern
- Pitfalls: HIGH - Based on vanilla bucket behavior and common modding mistakes

**Research date:** 2026-01-25
**Valid until:** 30 days (stable domain, bucket mechanics unlikely to change in 1.21.x)
