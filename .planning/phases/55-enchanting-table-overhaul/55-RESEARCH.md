# Phase 55: Enchanting Table Overhaul - Research

**Researched:** 2026-01-28
**Domain:** Minecraft enchanting table GUI modification, mixin on EnchantmentMenu, recipe override, custom item registration
**Confidence:** HIGH

## Summary

Phase 55 transforms enchanting tables from RNG-based enchanting (3 random enchantment buttons) into deterministic book-slot mechanics where the player places an enchanted book into a modified slot and it applies that specific enchantment. This requires:

1. A new `Soul Dust` item (texture already exists at `assets/thc/textures/item/soul_dust.png`)
2. A new enchanting table recipe using Soul Dust (ISI/SBS/ISI with Iron Block and Book)
3. Removing the vanilla enchanting table recipe via `RecipeManagerMixin`
4. Heavily modifying `EnchantmentMenu` behavior via mixin to replace the RNG system with deterministic book-slot enchanting
5. Bookshelf count enforcement (16 minimum) that silently disables the UI

The most complex part is the `EnchantmentMenu` mixin. Vanilla EnchantmentMenu has:
- `enchantSlots` container with slot 0 (item to enchant) and slot 1 (lapis lazuli)
- `costs[3]` array storing the XP cost of each of the 3 enchantment buttons
- `enchantClue[3]` and `levelClue[3]` for tooltip display
- `method_17411` that counts bookshelves and generates the 3 random enchantment options
- `clickMenuButton(player, buttonId)` that applies the chosen enchantment
- `slotsChanged()` that triggers recalculation when items change
- `getGoldCount()` that returns the lapis count from slot 1 (misleading name - it's the "fuel" count)
- `EMPTY_SLOT_LAPIS_LAZULI` static field for the placeholder icon

The approach is to mixin into `EnchantmentMenu` at HEAD of the critical methods, replacing vanilla RNG logic with deterministic book-based enchanting.

**Primary recommendation:** Mixin into `EnchantmentMenu` with HEAD cancellation on `method_17411` (cost calculation), `clickMenuButton` (enchantment application), and `slotsChanged` (validation refresh). Replace `EMPTY_SLOT_LAPIS_LAZULI` placeholder with `book_slot.png`. Bookshelf count validation reuses `EnchantingTableBlock.isValidBookShelf` and `BOOKSHELF_OFFSETS`.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.18.4+ | Mixin framework for EnchantmentMenu modification | Standard Fabric mixin approach |
| Minecraft 1.21.11 | 1.21.11 | EnchantmentMenu, EnchantingTableBlock, DataComponents | Target version |
| Kotlin | JVM 21 | Implementation language for game logic (THCItems, EnchantmentEnforcement) | Project standard |
| Java | JVM 21 | Mixin classes (EnchantmentMenuMixin) | Mixin package uses Java |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| DataComponents.STORED_ENCHANTMENTS | MC 1.21.11 | Read enchantments from enchanted books | Book slot validation |
| DataComponents.ENCHANTMENTS | MC 1.21.11 | Read/write enchantments on gear | Applying enchantments |
| EnchantmentHelper.updateEnchantments() | MC 1.21.11 | Modify item enchantments immutably | Adding enchantment to gear |
| EnchantingTableBlock.BOOKSHELF_OFFSETS | MC 1.21.11 | List of 15 valid bookshelf positions around table | Bookshelf counting |
| EnchantingTableBlock.isValidBookShelf() | MC 1.21.11 | Validates single bookshelf position | Per-offset validation |
| Enchantment.canEnchant(ItemStack) | MC 1.21.11 | Checks if enchantment supports item type | Compatibility validation |
| Enchantment.areCompatible() | MC 1.21.11 | Checks enchantment conflict sets | Stacking validation |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| EnchantmentMenu mixin | UseBlockCallback | Mixin needed because we must change GUI behavior (slots, buttons, costs). Callback only handles block interaction, not menu contents |
| Single EnchantmentMenuMixin | Separate classes for cost calc, click handling | Single mixin is cleaner for tightly coupled menu state |
| Custom menu class | Mixin on vanilla EnchantmentMenu | Custom menu requires re-registering MenuType; mixin preserves all vanilla GUI layout |

## Architecture Patterns

### Recommended Project Structure
```
src/
├── main/java/thc/mixin/
│   └── EnchantmentMenuMixin.java    # Mixin: HEAD cancel on method_17411, clickMenuButton, slotsChanged
├── main/kotlin/thc/enchant/
│   ├── EnchantmentEnforcement.kt    # EXTENDED: Add stage classification (3 vs 4-5), level/cost helpers
│   └── EnchantmentTableHandler.kt   # NEW: Core logic for book validation, compatibility checks, enchant application
├── main/kotlin/thc/item/
│   └── THCItems.kt                  # EXTENDED: Register SOUL_DUST item
├── main/resources/data/
│   ├── thc/recipe/enchanting_table.json     # New recipe: ISI/SBS/ISI
│   └── minecraft/recipe/ (removed via RecipeManagerMixin)
├── main/resources/assets/thc/
│   ├── models/item/soul_dust.json
│   └── lang/en_us.json              # Add soul_dust entry
```

### Pattern 1: EnchantmentMenu HEAD Cancellation
**What:** Override core menu methods to replace RNG with deterministic book-based logic
**When to use:** When vanilla menu behavior must be completely replaced while keeping GUI layout
**Example:**
```java
// Source: THC AnvilMenuMixin pattern
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {

    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow public final int[] costs;
    @Shadow public final int[] enchantClue;
    @Shadow public final int[] levelClue;

    // Dummy constructor required for extending AbstractContainerMenu
    protected EnchantmentMenuMixin() { super(null, 0); }

    // HEAD cancel on the cost-calculation method to replace RNG with book-based costs
    @Inject(method = "method_17411", at = @At("HEAD"), cancellable = true)
    private void thc$calculateBookEnchantCosts(
            ItemStack itemStack,
            Level level,
            BlockPos pos,
            CallbackInfo ci) {

        // Count bookshelves using vanilla helper
        int bookshelfCount = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                bookshelfCount++;
            }
        }

        // Get book from slot 1
        ItemStack book = enchantSlots.getItem(1);

        // ... determine costs based on book and bookshelf count ...

        ci.cancel();
    }

    // HEAD cancel on clickMenuButton to replace RNG enchant with deterministic
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void thc$applyBookEnchantment(
            Player player,
            int buttonId,
            CallbackInfoReturnable<Boolean> cir) {

        // ... validate book, level, compatibility, then apply ...

        cir.setReturnValue(true);
        cir.cancel();
    }
}
```

### Pattern 2: Bookshelf Counting (Reuse Vanilla Static Methods)
**What:** Count valid bookshelves using `EnchantingTableBlock.BOOKSHELF_OFFSETS` + `isValidBookShelf()`
**When to use:** Whenever we need the bookshelf count around an enchanting table
**Example:**
```java
// Source: EnchantmentMenu.method_17411 bytecode analysis - uses these exact calls
int bookshelfCount = 0;
for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
    if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
        bookshelfCount++;
    }
}
// bookshelfCount is 0-15 range (BOOKSHELF_OFFSETS has 15 entries)
```

### Pattern 3: Stage Classification for Enchanting Table
**What:** Classify enchantments into stage tiers for level requirement and cost determination
**When to use:** When applying enchantments with tiered requirements
**Key Design:**
```kotlin
// In EnchantmentEnforcement.kt (extending existing file)
fun getStageForEnchantment(enchantId: String?): Int {
    if (enchantId == null) return 0
    return when {
        STAGE_1_2_ENCHANTMENTS.contains(enchantId) -> 1  // Stage 1-2
        STAGE_3_ENCHANTMENTS.contains(enchantId) -> 3
        STAGE_4_5_ENCHANTMENTS.contains(enchantId) -> 4
        else -> 3  // Default unknown to stage 3 (safe fallback)
    }
}

fun getLevelRequirementForStage(stage: Int): Int = when {
    stage <= 2 -> 10   // Stage 1-2: level 10
    stage == 3 -> 20   // Stage 3: level 20
    else -> 30         // Stage 4-5: level 30
}
// Cost is always 3 levels regardless of stage (per CONTEXT.md)
```

### Pattern 4: Slot Placeholder Override
**What:** Replace the lapis lazuli placeholder icon with book_slot.png for the book slot
**When to use:** When changing what placeholder icon displays in an empty enchanting slot
**Implementation:**
The `EMPTY_SLOT_LAPIS_LAZULI` field is a static Identifier used as the placeholder texture for slot 1. We override it at class loading time via a static block in the mixin or by modifying the slot's behavior. The texture `book_slot.png` already exists at `assets/thc/textures/item/`.

### Anti-Patterns to Avoid
- **Don't create a custom menu class:** This would require re-registering the MenuType and is much heavier than mixin
- **Don't use UseBlockCallback for GUI modification:** Callback intercepts block interaction, not the menu/container state
- **Don't use getGoldCount() for bookshelf validation:** Despite its name, this returns lapis count from slot 1, not bookshelf count
- **Don't assume bookshelfCount can exceed 15:** BOOKSHELF_OFFSETS has exactly 15 positions; requirement is 16, which means ALL 15 offsets must be valid PLUS the rules must be reconsidered (see Open Questions)

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bookshelf position validation | Custom position list | `EnchantingTableBlock.BOOKSHELF_OFFSETS` + `isValidBookShelf()` | Handles obstruction checks, exact vanilla rules |
| Enchantment compatibility | Manual incompatibility map | `Enchantment.areCompatible()` | Handles exclusive sets, curse interactions |
| Adding enchantment to item | Manual ItemStack component mutation | `EnchantmentHelper.updateEnchantments()` | Type-safe, handles both ENCHANTMENTS and STORED_ENCHANTMENTS |
| Item type to enchantment matching | Hardcoded item->enchant map | `Enchantment.canEnchant(ItemStack)` | Uses data-driven supportedItems HolderSet |
| Stage classification | Simple if/else in mixin | Utility methods in EnchantmentEnforcement | Reusable, testable, follows existing pattern |

**Key insight:** Vanilla's bookshelf scanning and enchantment compatibility are data-driven and handle edge cases. Reuse them entirely.

## Common Pitfalls

### Pitfall 1: Bookshelf Count Maximum is 15, Not 16
**What goes wrong:** Assuming you can reach 16 bookshelves when BOOKSHELF_OFFSETS only has 15 entries
**Why it happens:** The 15 positions form a ring at 2-block distance. The requirement says "16 bookshelves" but the max detectable is 15.
**How to avoid:** The threshold check should be `bookshelfCount >= 15` (all slots filled) to satisfy the "effectively 16 bookshelves" requirement, OR the 15-max may mean the CONTEXT requirement means "all 15 valid positions filled". Verify by testing with 15 bookshelves.
**Warning signs:** Players can never reach 16 bookshelves with vanilla placement rules.

### Pitfall 2: method_17411 Parameter Binding
**What goes wrong:** Mixin method signature doesn't match obfuscation or intermediary names
**Why it happens:** method_17411 is an intermediary name that may change or may not map correctly in all Loom versions
**How to avoid:** Use the full method descriptor in the @Inject targets string. From bytecode analysis:
```java
method = "method_17411(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
```
**Warning signs:** Mixin not applying, compilation errors about target method not found

### Pitfall 3: EnchantmentMenu Inner Classes ($1, $2, $3) Are Slot Definitions
**What goes wrong:** Trying to modify slot behavior without understanding inner class slot types
**Why it happens:** EnchantmentMenu$1 is the item slot, EnchantmentMenu$2 and $3 are different slot constraint classes
**How to avoid:** Don't mixin inner classes. Instead, use HEAD cancellation on the main menu methods to intercept before slot logic runs.
**Warning signs:** Items not accepted into slots, or wrong items accepted

### Pitfall 4: costs[] Array Must Be Set or GUI Buttons Are Invisible
**What goes wrong:** Setting costs[i] = 0 hides the button entirely; must be > 0 for button to show
**Why it happens:** Vanilla GUI renders enchantment buttons only when cost > 0
**How to avoid:** When book is valid and requirements met, set costs[0] = the level requirement (e.g., 20). When disabled, set all costs to 0. Only one button is used (index 0) in our deterministic system, so costs[1] and costs[2] stay at 0.
**Warning signs:** No enchantment button visible even with valid book in slot

### Pitfall 5: slotsChanged Fires on Every Item Movement
**What goes wrong:** Expensive recalculation on every shift-click or drag
**Why it happens:** slotsChanged fires whenever any slot in the container changes
**How to avoid:** Only perform bookshelf counting and enchantment validation when the relevant slots (0 and 1) actually changed. Cache the enchantment book reference.
**Warning signs:** Performance issues with many bookshelves or complex enchantments

### Pitfall 6: Client-Server Desync on Button Disabled State
**What goes wrong:** Client shows button as clickable but server rejects the click
**Why it happens:** cost values sync via DataSlot but our custom validation state may not
**How to avoid:** Set costs[0] = 0 on the server when requirements not met; the costs array syncs to client automatically via the DataSlot shared array mechanism (visible in bytecode: `DataSlot.shared(costs)`)
**Warning signs:** Client shows button, server says "can't enchant"

## Code Examples

Verified patterns from existing THC code and bytecode analysis:

### Counting Bookshelves Around Enchanting Table
```java
// Source: EnchantmentMenu.method_17411 bytecode - exact pattern used by vanilla
int bookshelfCount = 0;
for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
    if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
        bookshelfCount++;
    }
}
```

### Reading Enchantment from Book in GUI Slot
```java
// Source: LecternEnchanting.kt pattern adapted for menu slot
ItemStack book = enchantSlots.getItem(1);  // Slot 1 = former lapis slot
if (book.is(Items.ENCHANTED_BOOK)) {
    ItemEnchantments storedEnchants = book.get(DataComponents.STORED_ENCHANTMENTS);
    if (storedEnchants != null && !storedEnchants.isEmpty) {
        // Get first (and ideally only) enchantment
        for (Map.Entry<Holder<Enchantment>, Integer> entry : storedEnchants.entrySet()) {
            Holder<Enchantment> enchantHolder = entry.getKey();
            String enchantId = enchantHolder.unwrapKey().orElse(null).identifier().toString();
            // Use enchantId for stage lookup, etc.
        }
    }
}
```

### Applying Enchantment (Reuse LecternEnchanting Pattern)
```kotlin
// Source: LecternEnchanting.kt lines 147-151 - exact same pattern
EnchantmentHelper.updateEnchantments(targetItem) { mutable ->
    for (entry in storedEnchants.entrySet()) {
        mutable.set(entry.key, entry.intValue)
    }
}

// Deduct levels
player.giveExperienceLevels(-3)  // Always costs 3 levels per CONTEXT
```

### Compatibility Validation (Full Chain)
```kotlin
// Source: LecternEnchanting.kt lines 111-143 - complete compatibility check
val targetEnchants = itemInHand.get(DataComponents.ENCHANTMENTS) ?: ItemEnchantments.EMPTY

for (entry in storedEnchants.entrySet()) {
    val enchantHolder = entry.key

    // Already has this enchantment?
    if (targetEnchants.getLevel(enchantHolder) > 0) {
        return false  // Already applied
    }

    // Can this enchantment go on this item?
    if (!enchantHolder.value().canEnchant(itemInHand)) {
        return false  // Incompatible item type
    }

    // Conflicts with existing enchantments?
    for (existing in targetEnchants.keySet()) {
        if (!Enchantment.areCompatible(enchantHolder, existing)) {
            return false  // Conflicting enchantments
        }
    }
}
```

### Recipe JSON Structure (New Enchanting Table)
```json
// dest: src/main/resources/data/thc/recipe/enchanting_table.json
// Pattern: ISI/SBS/ISI where I=Iron Block, S=Soul Dust, B=Book
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "I": "minecraft:iron_block",
    "S": "thc:soul_dust",
    "B": "minecraft:book"
  },
  "pattern": [
    "ISI",
    "SBS",
    "ISI"
  ],
  "result": {
    "id": "minecraft:enchanting_table"
  }
}
```

### Soul Dust Item Registration
```kotlin
// Source: THCItems.kt pattern - extend existing file
@JvmField
val SOUL_DUST: Item = register("soul_dust") { key ->
    Item(
        Item.Properties()
            .setId(key)
            .stacksTo(64)
    )
}

// In init(): add to tools tab
ItemGroupEvents.modifyEntriesEvent(toolsTabKey).register { entries ->
    entries.accept(SOUL_DUST)
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NBT compound tags | DataComponents | MC 1.20.5 | Components are type-safe, cleaner API |
| ItemStack.getTag() | ItemStack.get(DataComponent) | MC 1.20.5 | Must use component system |
| ResourceLocation | Identifier | MC 1.21.11 | API rename |
| EnchantmentList manual | ItemEnchantments.Mutable | MC 1.21+ | Builder pattern for modifications |
| Random enchantment selection | Deterministic book-based | Phase 55 (THC) | Removes RNG, player chooses exact outcome |

**Deprecated/outdated:**
- `ItemStack.getTag()` / `ItemStack.getOrCreateTag()`: Replaced by DataComponents
- Manual NBT enchantment manipulation: Use ItemEnchantments.Mutable
- `ResourceLocation`: Use `Identifier` in MC 1.21.11

## Open Questions

Things that couldn't be fully resolved:

1. **Bookshelf Count: 15 vs 16 Maximum**
   - What we know: `EnchantingTableBlock.BOOKSHELF_OFFSETS` has exactly 15 entries (verified from bytecode). The CONTEXT requires "16 bookshelves minimum."
   - What's unclear: Whether the requirement means "all 15 valid positions filled" (which is the physical maximum), or if 16 is a typo/oversight.
   - Recommendation: Implement threshold as `>= 15` (all valid bookshelf positions filled). This satisfies the spirit of "16 bookshelves" by requiring maximum surrounding bookshelf infrastructure. If the user explicitly wants a count of 16, this would be impossible with vanilla placement rules, and we should raise this during planning.

2. **Single Enchantment per Book Enforcement**
   - What we know: Enchanted books can carry multiple enchantments. The CONTEXT says "Book placed in slot determines exact enchantment applied."
   - What's unclear: Should we reject multi-enchantment books, or apply all enchantments from the book at once?
   - Recommendation: Accept only single-enchantment books (reject multi-enchantment with a message). This matches "deterministic" intent and simplifies cost/tooltip display. The button tooltip shows one enchantment name.

3. **Placeholder Icon Override Mechanism**
   - What we know: `EMPTY_SLOT_LAPIS_LAZULI` is a static Identifier field. The `book_slot.png` texture exists.
   - What's unclear: Whether overriding the static field via mixin is the cleanest approach, or if the slot's hasItem check handles display differently.
   - Recommendation: Override `EMPTY_SLOT_LAPIS_LAZULI` in a `@Shadow` + `@Final` to remove the final and replace with `thc:item/book_slot`. If that's too fragile, accept lapis icon and note as cosmetic debt.

4. **Stage 3+ Enchantment Definition Sets**
   - What we know: STAGE_1_2_ENCHANTMENTS is defined with 5 enchantments (mending, unbreaking, efficiency, fortune, silk_touch). Everything else is implicitly stage 3+.
   - What's unclear: Which enchantments are stage 3 vs stage 4 vs stage 5 (for the two-tier level requirement).
   - Recommendation: Define STAGE_4_5_ENCHANTMENTS explicitly (e.g., flame, fire_aspect, looting, feather_falling, respiration, aqua_affinity, depth_strider, frost_walker). Everything else defaults to stage 3. This allows the level 20 vs 30 split.

## Sources

### Primary (HIGH confidence)
- EnchantmentMenu.class bytecode analysis - slot structure, method signatures, cost array, bookshelf counting
- EnchantingTableBlock.class - BOOKSHELF_OFFSETS, isValidBookShelf static method
- THC codebase: LecternEnchanting.kt (Phase 54 - proven enchantment application pattern)
- THC codebase: EnchantmentEnforcement.kt (stage classification, enchantment IDs)
- THC codebase: AnvilMenuMixin.java (menu mixin pattern with @Shadow fields)
- THC codebase: RecipeManagerMixin.java (recipe removal pattern)
- THC codebase: THCItems.kt (item registration pattern)

### Secondary (MEDIUM confidence)
- Phase 54 RESEARCH.md and PLAN.md (proven enchantment patterns and gotchas)
- Phase 55 CONTEXT.md (user decisions that constrain implementation)
- THC ROADMAP.md (phase requirements TBL-01 through TBL-06)

### Tertiary (LOW confidence)
- None - all findings verified from primary bytecode analysis and existing codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using established patterns already in THC codebase, confirmed via bytecode
- Architecture: HIGH - EnchantmentMenu mixin pattern follows proven AnvilMenuMixin and LecternEnchanting patterns
- Pitfalls: HIGH - Verified against decompiled vanilla EnchantmentMenu bytecode
- Open questions: MEDIUM - Bookshelf count (15 vs 16) and stage 4-5 set need user clarification

**Research date:** 2026-01-28
**Valid until:** ~60 days (stable Minecraft 1.21.11 API)
