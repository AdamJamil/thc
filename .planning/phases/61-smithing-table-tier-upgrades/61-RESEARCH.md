# Phase 61: Smithing Table Tier Upgrades - Research

**Researched:** 2026-01-28
**Domain:** Minecraft smithing table mechanics, custom recipe types, screen handler modification
**Confidence:** MEDIUM

## Summary

This phase requires implementing tier-based equipment upgrades at smithing tables that preserve enchantments and restore durability. The core technical challenge is that vanilla smithing recipes only consume 1 item per slot, but the requirements specify "count matches crafting recipe" (e.g., helmet needs 5 ingots, chestplate needs 8).

**Key findings:**
- Vanilla smithing uses `minecraft:smithing_transform` recipe type requiring template + base + addition
- Smithing recipes automatically preserve enchantments, NBT data, and custom names
- Recipes consume exactly 1 item from each slot (no multi-item consumption support)
- Two viable approaches: (1) Custom recipe type with material count validation, or (2) Mixin into SmithingMenu to validate/consume extra materials

**Primary recommendation:** Use SmithingMenu mixin to intercept crafting, validate material count in addition slot, and consume multiple items programmatically. This preserves vanilla recipe system compatibility while adding count validation.

## Standard Stack

The established libraries/tools for smithing table modification:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.102.0+1.21 | Mixin framework, events | Required for all Fabric mods |
| Mixin | 0.8+ | Bytecode manipulation | Standard for vanilla behavior modification |
| Fabric Data Generation | 0.102.0+1.21 | Recipe generation | Official Fabric approach for recipes |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| FabricRecipeProvider | 0.102.0+1.21 | Programmatic recipe creation | When generating multiple similar recipes |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| SmithingMenu mixin | Custom Recipe Type | More complex, requires serializer/type registration, but cleaner separation |
| JSON recipes | Programmatic generation | Simpler but less dynamic, harder to maintain many similar recipes |

**Installation:**
Already available via Fabric API dependency.

## Architecture Patterns

### Recommended Project Structure
```
src/main/
├── java/thc/mixin/
│   └── SmithingMenuMixin.java       # Intercept crafting, validate counts
├── kotlin/thc/smithing/
│   └── TierUpgradeRecipes.kt        # Recipe generation/registration
└── resources/data/
    ├── minecraft/recipe/
    │   ├── smithing_table_copper.json  # Alternative crafting recipe
    │   └── leather_to_copper_*.json    # Tier upgrade recipes
    └── thc/recipe/
        └── tier_upgrades/              # Custom smithing recipes

```

### Pattern 1: SmithingMenu Mixin for Count Validation
**What:** Inject into SmithingMenu to validate material count before allowing craft
**When to use:** When vanilla recipe format doesn't support required behavior
**Example:**
```java
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void thc$validateTierUpgrade(CallbackInfo ci) {
        ItemStack base = this.inputSlots.getItem(1);
        ItemStack addition = this.inputSlots.getItem(2);

        // Check if this is a tier upgrade recipe
        int requiredCount = getRequiredMaterialCount(base);
        if (requiredCount > 0 && addition.getCount() < requiredCount) {
            // Cancel craft - insufficient materials
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            ci.cancel();
            return;
        }
        // Let vanilla continue with 1-item consumption
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void thc$consumeExtraMaterials(Player player, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        // After vanilla consumes 1 item, consume the rest
        ItemStack base = this.inputSlots.getItem(1);
        int requiredCount = getRequiredMaterialCount(base);
        if (requiredCount > 1) {
            ItemStack addition = this.inputSlots.getItem(2);
            addition.shrink(requiredCount - 1); // Already consumed 1
        }
    }
}
```
**Source:** Adapted from AnvilMenuMixin pattern in THC codebase (line 29-65)

### Pattern 2: Recipe Generation with FabricRecipeProvider
**What:** Programmatically generate smithing recipes during data generation
**When to use:** When creating many similar recipes (all tier upgrades)
**Example:**
```kotlin
class TierUpgradeRecipes : FabricRecipeProvider {
    override fun buildRecipes(output: RecipeOutput) {
        // Leather → Copper armor upgrades
        tierUpgrade(output, Items.LEATHER_HELMET, Items.COPPER_HELMET,
            Items.COPPER_INGOT, 5)
        tierUpgrade(output, Items.LEATHER_CHESTPLATE, Items.COPPER_CHESTPLATE,
            Items.COPPER_INGOT, 8)
        // ... more upgrades
    }

    private fun tierUpgrade(output: RecipeOutput, base: Item, result: Item,
                           material: Item, count: Int) {
        // Note: count parameter stored as metadata, validated by mixin
        SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(Items.BARRIER), // Template slot (dummy)
            Ingredient.of(base),
            Ingredient.of(material),
            RecipeCategory.COMBAT,
            result
        ).unlocks("has_base", has(base))
         .save(output, Identifier.of("thc", "upgrade_${result.id}"))
    }
}
```
**Source:** Fabric documentation on recipe generation

### Pattern 3: Durability Restoration
**What:** Reset item durability to maximum after upgrade
**When to use:** All tier upgrades (requirement SMTH-09)
**Example:**
```kotlin
fun restoreDurability(itemStack: ItemStack): ItemStack {
    // Remove damage component (restores to full durability)
    itemStack.remove(DataComponents.DAMAGE)
    return itemStack
}
```
**Source:** Minecraft Wiki - Data component format

### Pattern 4: Enchantment Preservation
**What:** Smithing recipes automatically preserve enchantments via vanilla behavior
**When to use:** All tier upgrades (requirement SMTH-08)
**Implementation:** Use `minecraft:smithing_transform` recipe type - enchantments copy automatically from base item to result. No custom code needed.

**Source:** Minecraft Wiki - Smithing Table documentation

### Anti-Patterns to Avoid
- **Custom Recipe Type without proper serialization:** RecipeSerializer implementation is complex and error-prone. Mixin approach is simpler.
- **Modifying template slot behavior:** Templates are deeply integrated into vanilla UI. Use dummy template (barrier block) rather than trying to make template-less recipes.
- **Manual enchantment copying:** Vanilla already does this. Don't duplicate the logic.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Recipe JSON generation | String concatenation | FabricRecipeProvider | Type safety, validation, consistency |
| Enchantment transfer | Manual NBT copying | Vanilla smithing behavior | Handles edge cases (conflicts, levels, curses) |
| Durability calculation | Manual max damage lookup | DataComponents.DAMAGE removal | Works with modded items, respects component system |
| Material count validation | Custom inventory handler | SmithingMenu mixin | Preserves vanilla UI, less code |

**Key insight:** Minecraft's component system (1.21+) handles most item state management automatically. Don't recreate what vanilla already does via components and recipe systems.

## Common Pitfalls

### Pitfall 1: Assuming Multi-Item Recipe Support
**What goes wrong:** Vanilla smithing recipes consume exactly 1 item per slot. JSON "count" field is ignored.
**Why it happens:** Smithing table predates multi-count recipe support and was never updated.
**How to avoid:** Use mixin to validate count before craft and consume extra items after craft (see Pattern 1).
**Warning signs:** Recipe accepts craft with only 1 ingot when 5 are required.

### Pitfall 2: Template Slot Required
**What goes wrong:** Smithing table UI enforces template slot - cannot create template-less recipes.
**Why it happens:** 1.20+ changed smithing from 2-slot to 3-slot with mandatory template.
**How to avoid:** Use dummy template (barrier block or similar) that's never displayed/required. Mixin can bypass template validation.
**Warning signs:** UI shows empty template slot that blocks crafting.

### Pitfall 3: Durability Not Restoring
**What goes wrong:** Upgraded item retains original durability (half-broken stays half-broken).
**Why it happens:** Vanilla smithing preserves durability by default (netherite upgrade behavior).
**How to avoid:** Explicitly remove DataComponents.DAMAGE in mixin after upgrade completes (see Pattern 3).
**Warning signs:** Player upgrades damaged item, result is still damaged.

### Pitfall 4: Recipe Conflicts with Vanilla
**What goes wrong:** Custom recipes interfere with existing netherite upgrade recipes.
**Why it happens:** RecipeManager loads all recipes, last one wins for duplicate IDs.
**How to avoid:** Use unique namespaced recipe IDs (thc:upgrade_copper_helmet not minecraft:copper_helmet). Don't override vanilla recipes.
**Warning signs:** Netherite upgrade stops working after adding custom recipes.

### Pitfall 5: Material Count Not Visible to Player
**What goes wrong:** Player doesn't know how many materials are needed.
**Why it happens:** Vanilla recipe book/JEI shows recipe ingredients but not stack counts for smithing.
**How to avoid:** Either accept vanilla behavior (player discovers via trial) or add custom tooltip to smithing screen showing required count.
**Warning signs:** Player confusion about material requirements.

## Code Examples

Verified patterns from official sources:

### Smithing Transform Recipe JSON
```json
{
  "type": "minecraft:smithing_transform",
  "template": {
    "item": "minecraft:barrier"
  },
  "base": {
    "item": "minecraft:leather_helmet"
  },
  "addition": {
    "item": "minecraft:copper_ingot"
  },
  "result": {
    "id": "minecraft:copper_helmet"
  }
}
```
**Source:** Minecraft Wiki - Recipe format, adapted for tier upgrades

### Alternative Crafting Recipe
```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "#": "#minecraft:planks",
    "@": "minecraft:copper_ingot"
  },
  "pattern": [
    "@@",
    "##",
    "##"
  ],
  "result": {
    "count": 1,
    "id": "minecraft:smithing_table"
  }
}
```
**File:** `data/thc/recipe/smithing_table_copper.json`
**Source:** Existing THC smithing_table.json recipe, modified for copper

### Material Count Lookup
```kotlin
object TierUpgradeMaterials {
    private val ARMOR_COUNTS = mapOf(
        // Helmet: 5 ingots
        Items.LEATHER_HELMET to 5,
        Items.COPPER_HELMET to 5,
        Items.IRON_HELMET to 5,
        Items.DIAMOND_HELMET to 5,

        // Chestplate: 8 ingots
        Items.LEATHER_CHESTPLATE to 8,
        Items.COPPER_CHESTPLATE to 8,
        Items.IRON_CHESTPLATE to 8,
        Items.DIAMOND_CHESTPLATE to 8,

        // Leggings: 7 ingots
        Items.LEATHER_LEGGINGS to 7,
        Items.COPPER_LEGGINGS to 7,
        Items.IRON_LEGGINGS to 7,
        Items.DIAMOND_LEGGINGS to 7,

        // Boots: 4 ingots
        Items.LEATHER_BOOTS to 4,
        Items.COPPER_BOOTS to 4,
        Items.IRON_BOOTS to 4,
        Items.DIAMOND_BOOTS to 4
    )

    private val TOOL_COUNTS = mapOf(
        // All tools: 3 material + 2 sticks (only count material)
        Items.WOODEN_PICKAXE to 3,
        Items.STONE_PICKAXE to 3,
        Items.COPPER_PICKAXE to 3,
        Items.IRON_PICKAXE to 3,
        Items.DIAMOND_PICKAXE to 3,
        // Repeat for axe, shovel, hoe, sword...
    )

    fun getRequiredCount(baseItem: Item): Int {
        return ARMOR_COUNTS[baseItem]
            ?: TOOL_COUNTS[baseItem]
            ?: 0  // 0 means not a tier upgrade recipe
    }
}
```
**Source:** Vanilla crafting recipe material counts

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 2-slot smithing (pre-1.20) | 3-slot with template | Minecraft 1.20 | Must handle template slot in all recipes |
| NBT-based item data | Data Components | Minecraft 1.20.5 | Use DataComponents API, not CompoundTag |
| Custom RecipeType mixins | FabricRecipeProvider | Fabric API 0.70+ | Cleaner data generation, less boilerplate |
| isEnchantable() checks | isSupportedItem() | Minecraft 1.21 | Works with already-enchanted items |

**Deprecated/outdated:**
- **RecipeManager.apply() mixin:** Replaced by Fabric Data Generation API (official approach as of Fabric 0.70+)
- **Manual JSON file creation:** Use FabricRecipeProvider for programmatic generation
- **CompoundTag.putInt("Damage"):** Use DataComponents.DAMAGE instead (1.20.5+)

## Open Questions

Things that couldn't be fully resolved:

1. **Template slot bypass strategy**
   - What we know: Vanilla UI requires template slot, can use dummy item (barrier block)
   - What's unclear: Whether mixin can fully bypass template requirement or if dummy template must be obtainable
   - Recommendation: Use barrier block as template (creative-only), implement mixin to allow crafting without template in slot. Test thoroughly.

2. **Material count display to player**
   - What we know: Recipe book and JEI don't show stack counts for smithing ingredients
   - What's unclear: Whether custom tooltip/UI modification is worth implementation cost
   - Recommendation: Start without count display (player discovers via trial), add tooltip later if needed. Low priority.

3. **Copper tool registration**
   - What we know: Copper armor exists as vanilla Items (COPPER_HELMET, etc.)
   - What's unclear: Whether copper tools (COPPER_PICKAXE, COPPER_AXE, etc.) exist in vanilla 1.21.11 or need custom registration
   - Recommendation: Verify vanilla Items registry. If missing, copper tools must be registered as custom items before implementing upgrades.

## Sources

### Primary (HIGH confidence)
- [Minecraft Wiki - Smithing](https://minecraft.wiki/w/Smithing) - Enchantment preservation, durability behavior
- [Minecraft Wiki - Data component format](https://minecraft.wiki/w/Data_component_format) - Component system for item data
- [Minecraft Wiki - Recipe](https://minecraft.wiki/w/Recipe) - Recipe JSON format
- [Fabric Documentation - Recipe Generation](https://docs.fabricmc.net/develop/data-generation/recipes) - FabricRecipeProvider usage
- [Fabric Wiki - Creating a Custom Recipe Type](https://wiki.fabricmc.net/tutorial:recipe_type) - RecipeType implementation

### Secondary (MEDIUM confidence)
- [Microsoft Learn - Smithing Transform Recipe](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/recipereference/examples/recipedefinitions/minecraftrecipe_smithingtransform) - Bedrock format (adapted for Java)
- [NeoForged docs - Built-In Recipe Types](https://docs.neoforged.net/docs/resources/server/recipes/builtin/) - Cross-platform recipe documentation
- [Forge Forums - Recipe ingredient count](https://forums.minecraftforge.net/topic/118685-1192-get-stack-count-from-ingredient-of-a-recipe/) - Confirms count not natively supported

### Secondary (MEDIUM confidence) - Ecosystem Discovery
- [The Copper Age – Minecraft Wiki](https://minecraft.wiki/w/The_Copper_Age) - Copper equipment release info
- [Minecraft 1.21.9 Update: All New Features for January 2026](https://www.findingdulcinea.com/minecraft-copper-age-update-1-21-9-details/) - Copper tools/armor stats
- [Minecraft Copper Armor & Tools: Ultimate 2026 Crafting Guide](https://www.ofzenandcomputing.com/minecraft-copper-armor-tools/) - Copper progression context

### Tertiary (LOW confidence)
- Multiple WebSearch results about smithing table mechanics - general information, not authoritative for implementation details

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using official Fabric API and Mixin framework (proven in THC codebase)
- Architecture: MEDIUM - SmithingMenu mixin approach is proven but count validation pattern is custom
- Pitfalls: MEDIUM - Based on community discussions and mod examples, not official documentation
- Copper tools: LOW - Unclear if vanilla items or custom registration needed (requires verification)

**Research date:** 2026-01-28
**Valid until:** 2026-02-28 (30 days - relatively stable domain)
**Minecraft version:** 1.21.11 (context specified)
**Fabric API version:** 0.102.0+1.21 (based on search results)
