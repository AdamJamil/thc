# Phase 49: Fluid Placement Mechanics - Research

**Researched:** 2026-01-25
**Domain:** Fluid placement modification, water flow physics, bucket behavior interception
**Confidence:** HIGH

## Summary

Implementing fluid placement restrictions requires two distinct technical approaches: (1) blocking lava bucket placement by intercepting BucketItem behavior for vanilla lava buckets, and (2) modifying copper bucket water placement to create flowing water instead of source blocks. The key challenge is understanding Minecraft's water physics - flowing water blocks have a `level` property (0-15 in BlockState, 1-7 for flowing, 8 for falling) that determines flow distance, and these blocks naturally drain when not sustained by adjacent sources.

The vanilla `waterSourceConversion` gamerule (added in 1.19.3) controls whether flowing water between two sources converts to a source block, but for THC's bucket economy, we need a different approach: placing water with `level=8` (falling/max height) causes it to flow downward and horizontally like natural springs, eventually draining away. This is superior to toggling the gamerule because it preserves natural water source generation in oceans/rivers while only affecting bucket placement.

**Primary recommendation:** For lava buckets, mixin to BucketItem.use() checking for Fluids.LAVA and returning FAIL. For copper water buckets, replace `Blocks.WATER.defaultBlockState()` with flowing water at level 8 using `Fluids.FLOWING_WATER.defaultFluidState().setValue(FlowingFluid.LEVEL, 8).createLegacyBlock()`. This creates max-height flowing water that spreads naturally and drains when not sustained.

## Standard Stack

Water placement modification uses Minecraft's FluidState and BlockState systems.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| FluidState API | MC 1.21.11 | Fluid properties (level, falling, source) | Vanilla fluid system for water physics |
| FlowingFluid.LEVEL | MC 1.21.11 | IntegerProperty controlling flow distance | Standard property for all flowing fluids |
| Mixin @Inject | Fabric API | Intercept BucketItem.use() for lava | Non-destructive vanilla behavior modification |
| BlockState.setValue() | MC 1.21.11 | Set level property on water blocks | Standard property modification |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| FluidState.createLegacyBlock() | MC 1.21.11 | Convert FluidState to BlockState | Placing fluids with specific properties |
| Fluids.FLOWING_WATER | MC 1.21.11 | Flowing water fluid instance | Creating non-source water |
| FluidTags.LAVA | MC 1.21.11 | Identify lava fluids | Blocking lava bucket placement |
| scheduleTick() | MC 1.21.11 | Queue block updates for flow physics | Automatic (water schedules its own ticks) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Flowing water level 8 | Gamerule waterSourceConversion=false | Gamerule affects ALL water globally, breaks oceans/rivers |
| Mixin to BucketItem | Custom lava bucket items | Must replace all vanilla lava buckets in world |
| FluidState approach | Direct BlockState level property | FluidState is cleaner, handles falling flag automatically |

## Architecture Patterns

### Recommended Implementation Structure
```
src/main/java/thc/mixin/
├── BucketItemLavaMixin.java        # Block vanilla lava bucket placement
src/main/kotlin/thc/item/
└── CopperWaterBucketItem.kt        # Modify to place flowing water
```

### Pattern 1: Blocking Lava Bucket Placement

**What:** Intercept BucketItem.use() and cancel for lava buckets
**When to use:** Blocking vanilla item behavior without replacing the item
**Example:**
```java
@Mixin(BucketItem.class)
public class BucketItemLavaMixin {

    @Inject(
        method = "use",
        at = @At("HEAD"),
        cancellable = true
    )
    private void thc$blockLavaPlacement(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack stack = player.getItemInHand(hand);

        // Check if this is a lava bucket
        if (stack.getItem() instanceof BucketItem bucketItem) {
            // BucketItem has a final Fluid field - use reflection or check NBT
            // Simpler: check if the item is Items.LAVA_BUCKET
            if (stack.getItem() == Items.LAVA_BUCKET) {
                // Cancel with FAIL (no animation, silent rejection)
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }
}
```

### Pattern 2: Placing Flowing Water (Not Source)

**What:** Replace water source block placement with flowing water at max height
**When to use:** Creating finite water that drains naturally
**Example:**
```kotlin
// In CopperWaterBucketItem.kt
class CopperWaterBucketItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        val hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockPos = hitResult.blockPos
            val targetPos = blockPos.relative(hitResult.direction)

            if (level.mayInteract(player, blockPos) &&
                player.mayUseItemAt(targetPos, hitResult.direction, stack)) {

                if (!level.isClientSide()) {
                    // Place FLOWING water at max height (level 8 = falling)
                    val flowingWater = Fluids.FLOWING_WATER
                        .defaultFluidState()
                        .setValue(FlowingFluid.LEVEL, 8)
                        .createLegacyBlock()

                    level.setBlock(targetPos, flowingWater, 11)

                    // Schedule tick for water to start flowing
                    level.scheduleTick(targetPos, Fluids.FLOWING_WATER,
                        Fluids.FLOWING_WATER.getTickDelay(level))

                    // Return empty copper bucket
                    stack.shrink(1)
                    val emptyBucket = ItemStack(THCItems.COPPER_BUCKET)
                    if (!player.inventory.add(emptyBucket)) {
                        player.drop(emptyBucket, false)
                    }

                    // Play empty sound
                    level.playSound(null, player.x, player.y, player.z,
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f)
                }
                return InteractionResult.SUCCESS
            }
        }

        return InteractionResult.PASS
    }
}
```

### Pattern 3: Alternative - Simple Level 1 Flowing Water

**What:** Place flowing water at level 1 (lowest flow state)
**When to use:** Want water that drains quickly, minimum spread
**Example:**
```kotlin
// Simpler approach - level 1 flows minimally
val flowingWater = Fluids.FLOWING_WATER
    .defaultFluidState()
    .setValue(FlowingFluid.LEVEL, 1)
    .createLegacyBlock()

level.setBlock(targetPos, flowingWater, 11)
// Water will spread ~6 blocks and drain when source removed
```

### Anti-Patterns to Avoid

- **Using waterSourceConversion gamerule:** Affects ALL water globally, breaks natural ocean/river sources
- **Placing source block then scheduling removal:** Race condition with tick updates, unreliable
- **Placing air then using scheduleTick:** Water placement requires immediate block, not delayed
- **Using LEVEL 0:** Level 0 is a source block, defeats the purpose
- **Forgetting createLegacyBlock():** FluidState must be converted to BlockState before setBlock
- **Not scheduling tick:** Water won't start flowing without a scheduled tick

## Don't Hand-Roll

Problems with existing vanilla solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Water flow physics | Custom draining system | FlowingFluid.LEVEL + vanilla physics | Water already has level-based flow, drainage, spread |
| Finite water globally | Custom water replacement | Flowing water placement only | Preserves natural sources, affects only buckets |
| Lava bucket blocking | Replace all lava buckets | Mixin to BucketItem.use() | Simpler, works with existing items |
| Water level calculation | Manual neighbor checking | scheduleTick() auto-calculation | Vanilla physics handles it correctly |

**Key insight:** Minecraft's water system already has finite water mechanics built-in - flowing water (level 1-8) drains naturally when not sustained by sources. The challenge is NOT implementing finite water, but rather creating flowing water instead of sources when buckets are used.

## Common Pitfalls

### Pitfall 1: Confusing BlockState LEVEL vs FluidState LEVEL
**What goes wrong:** Water block has both BlockState level property and FluidState level property, using wrong one
**Why it happens:** Documentation unclear, both exist with same name
**How to avoid:** Use FluidState approach: `Fluids.FLOWING_WATER.defaultFluidState().setValue(FlowingFluid.LEVEL, X).createLegacyBlock()`
**Warning signs:** Water appears as source block despite setting level, or block placement fails

### Pitfall 2: Level Value Confusion (0-7 vs 0-15 vs 1-8)
**What goes wrong:** Wrong level value causes unexpected behavior
**Why it happens:** Different systems use different ranges
**How to avoid:** For flowing water, use 1-7 for standard flow (1=max spread, 7=minimal), 8 for falling/max height
**Warning signs:** Water doesn't flow, or creates source blocks, or disappears immediately

### Pitfall 3: Forgetting scheduleTick After Placement
**What goes wrong:** Water block appears but doesn't start flowing
**Why it happens:** Water needs a tick update to calculate neighbors and begin flow
**How to avoid:** Always call `level.scheduleTick(pos, fluid, tickDelay)` after placing flowing water
**Warning signs:** Water appears frozen, doesn't spread or drain

### Pitfall 4: Blocking Lava Pickup Instead of Placement
**What goes wrong:** Player can still place lava from existing buckets
**Why it happens:** Copper bucket already blocks pickup (Phase 48), but vanilla lava buckets can be placed
**How to avoid:** Mixin to BucketItem.use() checking for Items.LAVA_BUCKET
**Warning signs:** Player finds lava bucket in chest, can place it

### Pitfall 5: Water Creates Infinite Sources Anyway
**What goes wrong:** Placed flowing water becomes source through 2x2 formation
**Why it happens:** waterSourceConversion gamerule still active (default true)
**How to avoid:** This is CORRECT per vanilla behavior - multiple flowing waters CAN create sources if arranged properly
**Warning signs:** None - this is intended Minecraft behavior, not a bug

### Pitfall 6: Flowing Water Spreads Too Far
**What goes wrong:** Level 8 water spreads 7 blocks horizontally, feels too generous
**Why it happens:** Level 8 is max height (falling water), which flows full distance
**How to avoid:** Use lower level values (1-3) for more restrictive spread, or accept vanilla spread distance
**Warning signs:** Water economy feels too easy, players exploit bucket placement

## Code Examples

Verified patterns from research and vanilla Minecraft:

### Water Flow Mechanics
```
Source block: level = 0 (infinite, doesn't drain)
Flowing water: level = 1-7 (drains when not sustained)
  Level 1: flows 6 more blocks (7 total from source)
  Level 2: flows 5 more blocks
  ...
  Level 7: flows 0 more blocks (end of stream)
Falling water: level = 8 (flows downward, resets to level 1 at new elevation)
```

### FluidState to BlockState Conversion
```kotlin
// Create flowing water at specific level
val fluidState = Fluids.FLOWING_WATER.defaultFluidState()
    .setValue(FlowingFluid.LEVEL, 8)  // Max height/falling water

val blockState = fluidState.createLegacyBlock()

level.setBlock(pos, blockState, 11)  // 11 = Block.UPDATE_ALL | Block.UPDATE_CLIENTS
level.scheduleTick(pos, Fluids.FLOWING_WATER, Fluids.FLOWING_WATER.getTickDelay(level))
```

### Detecting Lava Bucket in Mixin
```java
@Inject(method = "use", at = @At("HEAD"), cancellable = true)
private void thc$blockLavaPlacement(
        Level level, Player player, InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir) {

    ItemStack stack = player.getItemInHand(hand);

    if (stack.getItem() == Items.LAVA_BUCKET) {
        // Silent fail - no animation, no feedback
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
```

### Water Level Property Access
```kotlin
// Check if block is flowing water vs source
val blockState = level.getBlockState(pos)
val fluidState = blockState.fluidState

if (fluidState.is(FluidTags.WATER)) {
    if (fluidState.isSource) {
        // Level 0 - source block
    } else {
        val flowLevel = fluidState.getValue(FlowingFluid.LEVEL)
        // Level 1-8 - flowing water
    }
}
```

### Schedule Tick Pattern
```kotlin
// Water needs tick update to start flowing
level.scheduleTick(
    targetPos,                                    // Position
    Fluids.FLOWING_WATER,                         // Fluid type
    Fluids.FLOWING_WATER.getTickDelay(level)     // Default delay (5 ticks)
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Gamerule waterSourceConversion | Place flowing water only from buckets | MC 1.19.3 | Gamerule added 1.19.3, but local control better |
| Block metadata for water level | BlockState LEVEL property | MC 1.13 (flattening) | Use BlockState.setValue(), not setData() |
| Fluids.WATER vs FLOWING_WATER | Same, still separate instances | Always | Two fluid types for source vs flowing |
| scheduleTick required | Still required | Always | Water doesn't auto-update on placement |

**Deprecated/outdated:**
- `block.setData((byte) level)` for water level - use BlockState.setValue() post-1.13
- Assuming flowing water disappears instantly - it flows and drains over multiple ticks
- Using NBT to mark bucket contents - vanilla buckets use separate item IDs

## Open Questions

1. **What level value for copper bucket water placement?**
   - What we know: Level 1-7 standard flow, level 8 falling/max height
   - What's unclear: Which feels right for game balance?
   - Recommendation: Start with level 8 (max spread), tune down if too generous

2. **Should placed water have FALLING flag?**
   - What we know: Level 8 auto-sets falling behavior
   - What's unclear: If explicit falling flag needed
   - Recommendation: Use level 8, which implies falling - no separate flag needed

3. **Do we need to prevent waterSourceConversion?**
   - What we know: Two flowing water blocks can create source if positioned right
   - What's unclear: Is this exploitable/undesirable?
   - Recommendation: Accept vanilla behavior initially - if exploited, revisit

4. **Should scheduleTick be called for client side?**
   - What we know: setBlock happens server-side only (wrapped in isClientSide check)
   - What's unclear: If client needs separate tick scheduling
   - Recommendation: Server-side only - client gets update via block sync

## Sources

### Primary (HIGH confidence)
- [Minecraft Wiki - Water](https://minecraft.wiki/w/Water) - Water mechanics, flow distance, level properties
- [Minecraft Wiki - Block States](https://minecraft.wiki/w/Block_states) - Level property definition (0-15 range)
- [Minecraft Wiki - Fluids](https://minecraft.wiki/w/Fluid) - Source vs flowing, level system, tick mechanics
- [Minecraft Wiki - Game Rules](https://minecraft.wiki/w/Game_rule) - waterSourceConversion gamerule (1.19.3+)
- [Forge Forums - How to place flowing water in world](https://forums.minecraftforge.net/topic/89954-how-to-place-flowing-water-in-world/) - FluidState.createLegacyBlock() method
- [SpigotMC - Distinguish flowing water and source](https://www.spigotmc.org/threads/how-to-distinguish-flowing-water-and-a-water-source.215552/) - FluidState.isSource detection
- [FlowingFluid Javadoc (Forge 1.19.3)](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.19.3/net/minecraft/world/level/material/FlowingFluid.html) - LEVEL property definition
- Existing THC codebase - CopperBucketItem.kt, CopperWaterBucketItem.kt patterns

### Secondary (MEDIUM confidence)
- [Fabric Documentation - Block States](https://docs.fabricmc.net/develop/blocks/blockstates) - setValue() method usage
- [Fabric Wiki - Make a Block Waterloggable](https://wiki.fabricmc.net/tutorial:waterloggable) - FluidState API examples
- [WaterFluid.Flowing Yarn API](https://maven.fabricmc.net/docs/yarn-1.21.6+build.1/net/minecraft/fluid/WaterFluid.Flowing.html) - API structure
- [Planet Minecraft - setblock water level](https://www.planetminecraft.com/forums/help/javaedition/how-do-you-use-setblock-to-put-water-at-a-certain-level-672493/) - Command block examples

### Tertiary (LOW confidence)
- [Finite Water mod](https://modrinth.com/mod/finitewater) - Mod preventing infinite water (approach unknown)
- [No More Infinite Water mod](https://www.curseforge.com/minecraft/mc-mods/no-more-inifinite-water) - Uses biome tags + gamerule
- [Flowing Fluids mod](https://modrinth.com/mod/flowing-fluids) - Realistic physics mod (advanced)
- Various forum posts about water mechanics - General patterns cross-verified

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - FluidState and FlowingFluid.LEVEL are core vanilla APIs
- Architecture: HIGH - Pattern verified through forum examples and vanilla behavior
- Pitfalls: MEDIUM - Based on common modding mistakes, not exhaustive testing

**Research date:** 2026-01-25
**Valid until:** 30 days (stable domain, water mechanics unchanged since 1.13 flattening)
