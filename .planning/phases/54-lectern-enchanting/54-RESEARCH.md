# Phase 54: Lectern Enchanting - Research

**Researched:** 2026-01-27
**Domain:** Minecraft block interaction, enchantment system, block entity modification
**Confidence:** HIGH

## Summary

Lectern enchanting requires intercepting vanilla lectern interactions to allow enchanted books placement and gear enchanting. The vanilla `LecternBlock` has two distinct interaction methods: `useItemOn()` handles item-in-hand interactions (where we intercept enchanted book placement and gear enchanting), and `useWithoutItem()` handles empty-hand interactions (which opens the book UI).

The implementation strategy is to use Fabric's `UseBlockCallback` event to intercept lectern interactions BEFORE vanilla processing. This allows us to:
1. Place enchanted books on empty lecterns (instead of vanilla's LECTERN_BOOKS tag check)
2. Apply enchantments when right-clicking with compatible gear
3. Remove books with shift+right-click
4. Block stage 3+ books from being placed

**Primary recommendation:** Use `UseBlockCallback` with `InteractionResult.SUCCESS` to consume interactions for custom behavior, returning `InteractionResult.PASS` to allow vanilla handling when not applicable.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.18.4+ | `UseBlockCallback` event for block interaction | Standard Fabric event system, already used in THC |
| Minecraft 1.21.11 | 1.21.11 | `LecternBlock`, `LecternBlockEntity`, `DataComponents` | Target version |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| DataComponents.STORED_ENCHANTMENTS | MC 1.21.11 | Read enchantments from enchanted books | Checking book contents |
| DataComponents.ENCHANTMENTS | MC 1.21.11 | Read/write enchantments on gear | Applying enchantments to items |
| EnchantmentHelper.updateEnchantments() | MC 1.21.11 | Modify item enchantments immutably | Adding enchantment to gear |
| SoundEvents.ENCHANTMENT_TABLE_USE | MC 1.21.11 | Enchanting success sound | Feedback |
| ParticleTypes.ENCHANT | MC 1.21.11 | Enchanting particles | Visual feedback |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| UseBlockCallback | Mixin on LecternBlock.useItemOn | Mixin is more complex, callback already proven pattern in THC |
| Custom block entity | Vanilla LecternBlockEntity with state attachment | Custom BE requires registration, sync; attachment simpler |
| Store book in NBT | Store book reference in attachment | NBT already exists in vanilla BE |

## Architecture Patterns

### Recommended Project Structure
```
src/
├── main/kotlin/thc/lectern/
│   ├── LecternEnchanting.kt    # Main handler with register() + interaction logic
│   └── LecternState.kt         # State tracking for which lectern has which book
├── main/java/thc/mixin/
│   └── LecternBlockEntityMixin.java  # Optional: modify book storage behavior
```

### Pattern 1: UseBlockCallback Interception
**What:** Register callback to intercept lectern right-clicks before vanilla processing
**When to use:** When adding custom behavior to existing blocks without replacing them
**Example:**
```kotlin
// Source: THC BellHandler.kt pattern
UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
    val pos = hitResult.blockPos
    val blockState = level.getBlockState(pos)

    if (!blockState.`is`(Blocks.LECTERN)) {
        return@register InteractionResult.PASS
    }

    // Server-side processing
    if (level.isClientSide) {
        return@register InteractionResult.SUCCESS // Consume on client
    }

    val itemInHand = player.getItemInHand(hand)

    // Handle enchanted book placement
    if (itemInHand.item == Items.ENCHANTED_BOOK) {
        // ... placement logic
        return@register InteractionResult.SUCCESS
    }

    // Handle gear enchanting
    if (hasEnchantedBook(level, pos) && isEnchantableGear(itemInHand)) {
        // ... enchanting logic
        return@register InteractionResult.SUCCESS
    }

    InteractionResult.PASS // Let vanilla handle
}
```

### Pattern 2: Block State + Block Entity for Book Storage
**What:** Use vanilla `HAS_BOOK` state and `LecternBlockEntity.book` field for storage
**When to use:** When leveraging existing vanilla mechanics
**Example:**
```kotlin
// Check if lectern has book
val hasBook = blockState.getValue(LecternBlock.HAS_BOOK)

// Get book from block entity
val blockEntity = level.getBlockEntity(pos) as? LecternBlockEntity
val storedBook = blockEntity?.book ?: ItemStack.EMPTY
```

### Pattern 3: Enchantment Compatibility Check
**What:** Use Enchantment.canEnchant() and existing enchantment check
**When to use:** Validating if enchantment can be applied to item
**Example:**
```kotlin
// Source: Minecraft EnchantmentHelper + Enchantment classes
fun canApplyEnchantment(book: ItemStack, target: ItemStack): Boolean {
    val storedEnchants = book.get(DataComponents.STORED_ENCHANTMENTS) ?: return false
    val targetEnchants = target.get(DataComponents.ENCHANTMENTS) ?: ItemEnchantments.EMPTY

    for (entry in storedEnchants.entrySet()) {
        val enchantHolder = entry.key

        // Check if already has this enchantment
        if (targetEnchants.getLevel(enchantHolder) > 0) {
            return false // Already has it
        }

        // Check if enchantment supports this item type
        if (!enchantHolder.value().canEnchant(target)) {
            return false // Incompatible item
        }

        // Check compatibility with existing enchantments
        for (existing in targetEnchants.keySet()) {
            if (!Enchantment.areCompatible(enchantHolder, existing)) {
                return false // Conflicts with existing
            }
        }
    }
    return true
}
```

### Pattern 4: Experience Level Manipulation
**What:** Check and modify player experience levels
**When to use:** Level requirements and costs
**Example:**
```kotlin
// Check level requirement
if (player.experienceLevel < 10) {
    player.displayClientMessage(
        Component.literal("You must be level 10!"),
        true  // actionbar
    )
    return@register InteractionResult.FAIL
}

// Deduct levels
player.giveExperienceLevels(-3)
```

### Anti-Patterns to Avoid
- **Don't store enchantment data in custom attachment when vanilla block entity exists:** The `LecternBlockEntity.book` field already persists the ItemStack
- **Don't use mixin when callback suffices:** UseBlockCallback is cleaner and already used for similar features
- **Don't forget client-side return:** Must return SUCCESS on client to prevent desync

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Enchantment compatibility | Manual incompatibility checks | `Enchantment.areCompatible()` | Handles exclusive sets, curse interactions |
| Adding enchantment to item | Manual ItemStack component mutation | `EnchantmentHelper.updateEnchantments()` | Handles both ENCHANTMENTS and STORED_ENCHANTMENTS correctly |
| Book persistence | Custom attachment for lectern state | Vanilla `LecternBlockEntity.book` field | Already persists, renders, drops on break |
| Item type to enchantment matching | Hardcoded item→enchant map | `Enchantment.canEnchant(ItemStack)` | Uses data-driven supportedItems HolderSet |

**Key insight:** Vanilla's lectern already stores books and has HAS_BOOK state. The challenge is only intercepting interactions and changing what books are accepted.

## Common Pitfalls

### Pitfall 1: Vanilla Lectern Consuming Interaction First
**What goes wrong:** Vanilla `useItemOn` runs before callback, placing writable books
**Why it happens:** Event ordering or using wrong callback priority
**How to avoid:** UseBlockCallback runs BEFORE vanilla block.use(); ensure we return SUCCESS to consume
**Warning signs:** Writable books being placed on lectern instead of enchanted books

### Pitfall 2: Client-Server Desync on Book Visual
**What goes wrong:** Client doesn't see book on lectern after placement
**Why it happens:** Not properly updating block state or forgetting client-side return
**How to avoid:**
1. Return `InteractionResult.SUCCESS` on client
2. Call `level.setBlock()` with flag 3 for client update
3. Use `blockEntity.setChanged()` after modification
**Warning signs:** Book invisible until relog or chunk reload

### Pitfall 3: Enchanted Book Not Rendering on Lectern
**What goes wrong:** Book placed but not visible on lectern
**Why it happens:** Vanilla LecternBlockEntity.hasBook() checks for WRITABLE_BOOK_CONTENT or WRITTEN_BOOK_CONTENT, not STORED_ENCHANTMENTS
**How to avoid:** Either:
1. Store enchanted book AND set HAS_BOOK state (book won't render as readable but lectern shows it)
2. Accept that enchanted books show as "empty lectern with book" visually
**Warning signs:** Lectern model doesn't update

### Pitfall 4: Book Drop on Lectern Break
**What goes wrong:** Enchanted book doesn't drop when lectern broken
**Why it happens:** `LecternBlockEntity.preRemoveSideEffects()` handles this, but only if book stored in vanilla field
**How to avoid:** Store book in vanilla `LecternBlockEntity.book` field, not custom attachment
**Warning signs:** Losing enchanted books when lectern destroyed

### Pitfall 5: Shift-Click Removal Not Working
**What goes wrong:** Shift+right-click doesn't remove book
**Why it happens:** Not checking `player.isShiftKeyDown()` in callback
**How to avoid:** Check shift state and handle book removal explicitly
**Warning signs:** No way to retrieve book from lectern

## Code Examples

Verified patterns from official sources and existing THC code:

### Reading Enchantments from Book
```kotlin
// Source: EnchantmentHelper.getComponentType, DataComponents
val storedEnchants = book.get(DataComponents.STORED_ENCHANTMENTS)
if (storedEnchants != null && !storedEnchants.isEmpty) {
    for (entry in storedEnchants.entrySet()) {
        val enchantHolder = entry.key  // Holder<Enchantment>
        val level = entry.intValue     // int
        val enchantId = enchantHolder.unwrapKey().orElse(null)?.identifier()?.toString()
        // Use enchantId for stage checking, etc.
    }
}
```

### Adding Enchantment to Gear
```kotlin
// Source: EnchantmentHelper.updateEnchantments
EnchantmentHelper.updateEnchantments(targetItem) { mutable ->
    for (entry in storedEnchants.entrySet()) {
        mutable.set(entry.key, entry.intValue)
    }
}
```

### Setting Lectern Book State
```kotlin
// Source: LecternBlock.placeBook, LecternBlock.resetBookState
val blockEntity = level.getBlockEntity(pos) as? LecternBlockEntity ?: return
blockEntity.setBook(enchantedBook.copy())  // Store copy, keep original

// Update block state to show book
val newState = blockState.setValue(LecternBlock.HAS_BOOK, true)
level.setBlock(pos, newState, 3)  // Flag 3 = update clients
```

### Playing Enchanting Feedback
```kotlin
// Source: SoundEvents, ServerLevel
val serverLevel = level as? ServerLevel ?: return

// Sound
serverLevel.playSound(
    null,  // all players hear
    pos,
    SoundEvents.ENCHANTMENT_TABLE_USE,
    SoundSource.BLOCKS,
    1.0f,
    1.0f
)

// Particles - spawn around the item position
val random = serverLevel.random
for (i in 0 until 10) {
    serverLevel.sendParticles(
        ParticleTypes.ENCHANT,
        player.x + random.nextGaussian() * 0.3,
        player.y + 1.0 + random.nextGaussian() * 0.3,
        player.z + random.nextGaussian() * 0.3,
        1,  // count
        0.0, 0.0, 0.0,  // offset
        0.0  // speed
    )
}
```

### Checking Experience Level
```kotlin
// Source: Player.java fields and methods
val playerLevel = player.experienceLevel

if (playerLevel < 10) {
    player.displayClientMessage(
        Component.literal("You must be level 10!").withStyle(ChatFormatting.RED),
        true  // actionbar
    )
    return@register InteractionResult.FAIL
}

// Deduct 3 levels
player.giveExperienceLevels(-3)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NBT compound tags | DataComponents | MC 1.20.5 | Components are type-safe, cleaner API |
| ItemStack.getTag() | ItemStack.get(DataComponent) | MC 1.20.5 | Must use component system |
| EnchantmentList manual | ItemEnchantments.Mutable | MC 1.21+ | Builder pattern for modifications |

**Deprecated/outdated:**
- `ItemStack.getTag()` / `ItemStack.getOrCreateTag()`: Replaced by DataComponents
- Manual NBT enchantment manipulation: Use ItemEnchantments.Mutable

## Stage 1-2 Enchantments List

Based on THC's enchantment stage system, the following enchantments should be allowed on lecterns (stage 1-2):

**From EnchantmentEnforcement.INTERNAL_LEVELS (these have custom internal levels):**
- minecraft:efficiency (level 3)
- minecraft:sharpness (level 1)
- minecraft:power (level 1)
- minecraft:protection (level 1)
- minecraft:fortune (level 3)
- minecraft:looting (level 3)
- minecraft:unbreaking (level 3)
- minecraft:feather_falling (level 4)

**Standard stage 1-2 enchantments (need hardcoded list):**
This list needs to be defined. The CONTEXT.md says "Hardcoded list of stage 1-2 enchantments (list defined in existing spec/requirements)". The planner should reference the full enchantment stage specification or define the list in the plan.

Candidates for stage 1-2 (basic, early-game):
- Sharpness, Power, Protection (damage/defense basics)
- Efficiency, Unbreaking (tool basics)
- Feather Falling (survival basic)
- Fire Protection, Blast Protection, Projectile Protection (defense variants)
- Smite, Bane of Arthropods (mob-specific damage)
- Piercing (crossbow basic)

## Open Questions

Things that couldn't be fully resolved:

1. **Complete Stage 1-2 Enchantment List**
   - What we know: Some enchantments defined in EnchantmentEnforcement.INTERNAL_LEVELS
   - What's unclear: Full list of which enchantments are stage 1-2 vs stage 3+
   - Recommendation: Define hardcoded set in implementation, reference THC enchantment spec

2. **Enchanted Book Visual on Lectern**
   - What we know: Vanilla checks WRITABLE/WRITTEN_BOOK_CONTENT for rendering
   - What's unclear: Whether enchanted books render on lectern at all
   - Recommendation: Test in-game; if no visual, accept as limitation or consider item frame alternative

3. **Multiple Enchantments on Single Book**
   - What we know: Books can have multiple enchantments
   - What's unclear: Should lectern apply all enchantments from multi-enchant book, or just first?
   - Recommendation: Apply all enchantments (natural behavior), but check each for compatibility

## Sources

### Primary (HIGH confidence)
- Minecraft 1.21.11 decompiled sources: LecternBlock.java, LecternBlockEntity.java, EnchantmentHelper.java
- THC existing code: BellHandler.kt (UseBlockCallback pattern), EnchantmentEnforcement.kt (enchantment handling)

### Secondary (MEDIUM confidence)
- Fabric API documentation for UseBlockCallback event ordering
- Minecraft Wiki for enchantment compatibility rules

### Tertiary (LOW confidence)
- None - all findings verified from primary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using established patterns already in THC codebase
- Architecture: HIGH - UseBlockCallback pattern proven, vanilla block entity well understood
- Pitfalls: HIGH - Verified against decompiled vanilla source code

**Research date:** 2026-01-27
**Valid until:** ~60 days (stable Minecraft 1.21.11 API)
