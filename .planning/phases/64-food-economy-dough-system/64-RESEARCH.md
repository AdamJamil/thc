# Phase 64: Food Economy - Dough System - Research

**Researched:** 2026-01-29
**Domain:** Minecraft Fabric modding - custom items, recipes, loot tables
**Confidence:** HIGH

## Summary

This phase implements a dough crafting system requiring cooking before bread becomes available, plus normalizing leather drops across farm animals. The implementation follows well-established patterns already present in the THC codebase.

All required patterns exist in the codebase:
- Recipe removal via `REMOVED_RECIPE_PATHS` in `RecipeManagerMixin.java`
- Custom item registration in `THCItems.kt` with `craftRemainder` for bucket preservation
- Shapeless recipes as JSON in `data/thc/recipe/`
- Item models as JSON in `assets/thc/models/item/`
- Loot table overrides in `data/minecraft/loot_table/entities/`
- Language keys in `assets/thc/lang/en_us.json`

**Primary recommendation:** Follow existing patterns exactly. The dough texture already exists (`dough.png`), and all infrastructure is in place.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.18.4+ | Minecraft modding framework | Project requirement |
| Kotlin | Project version | Item registration | Codebase convention |
| Java | 21+ | Mixin implementation | MC 1.21.11 requirement |

### Supporting
| Pattern | Purpose | When to Use |
|---------|---------|-------------|
| `REMOVED_RECIPE_PATHS` | Recipe removal | Any vanilla recipe that needs disabling |
| `craftRemainder` | Bucket preservation | Items that should return a container |
| Data pack loot tables | Loot modification | Override entity drops without code |
| Smelting recipe JSON | Cooking recipes | Furnace/smoker/campfire outputs |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Recipe JSON | Data generation | More code for simple recipes |
| Loot table override | Mixin | JSON is simpler for additive changes |

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/thc/item/
    THCItems.kt           # Item registration (add DOUGH here)

src/main/resources/
    assets/thc/
        lang/en_us.json   # Add "item.thc.dough": "Dough"
        models/item/
            dough.json    # Item model (standard generated pattern)
        textures/item/
            dough.png     # Already exists!
    data/
        minecraft/
            loot_table/entities/
                pig.json   # Override - add leather pool
                sheep.json # Override - add leather pool
        thc/recipe/
            dough.json           # Shapeless: 3 wheat + water bucket
            dough_copper.json    # Shapeless: 3 wheat + copper water bucket
            dough_smelting.json  # Smelting: dough -> bread
            dough_smoking.json   # Smoking: dough -> bread (faster)

src/main/java/thc/mixin/
    RecipeManagerMixin.java  # Add "bread" to REMOVED_RECIPE_PATHS
```

### Pattern 1: Simple Item Registration
**What:** Register a non-functional item (no special behavior)
**When to use:** Items that are purely crafting ingredients
**Example:**
```kotlin
// Source: THCItems.kt - SOUL_DUST pattern
@JvmField
val DOUGH: Item = register("dough") { key ->
    Item(
        Item.Properties()
            .setId(key)
            .stacksTo(64)
    )
}
```

### Pattern 2: Shapeless Recipe with Bucket Preservation
**What:** Recipe that returns the bucket after crafting
**When to use:** Any recipe using filled buckets
**Example:**
```json
// Source: honey_apple.json pattern + craftRemainder from item properties
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    "minecraft:wheat",
    "minecraft:wheat",
    "minecraft:wheat",
    "minecraft:water_bucket"
  ],
  "result": {
    "count": 1,
    "id": "thc:dough"
  }
}
```
**Note:** Bucket preservation is handled by `craftRemainder` on the item, not in the recipe JSON.

### Pattern 3: Smelting Recipe
**What:** Furnace/smoker cooking recipe
**When to use:** Items that need to be cooked
**Example:**
```json
// Source: Minecraft Wiki + Fabric documentation
{
  "type": "minecraft:smelting",
  "category": "food",
  "ingredient": "thc:dough",
  "result": "minecraft:bread",
  "experience": 0.35,
  "cookingtime": 200
}
```

### Pattern 4: Smoking Recipe (Faster Cooking)
**What:** Smoker-specific recipe (2x faster)
**When to use:** Food items that should cook faster in smoker
**Example:**
```json
{
  "type": "minecraft:smoking",
  "category": "food",
  "ingredient": "thc:dough",
  "result": "minecraft:bread",
  "experience": 0.35,
  "cookingtime": 100
}
```

### Pattern 5: Loot Table Pool Addition
**What:** Add a new drop to an entity without replacing existing drops
**When to use:** Adding leather drops to pig/sheep
**Example:**
```json
// Source: cow.json leather pool
{
  "type": "minecraft:item",
  "functions": [
    {
      "add": false,
      "count": {
        "type": "minecraft:uniform",
        "max": 2.0,
        "min": 0.0
      },
      "function": "minecraft:set_count"
    },
    {
      "count": {
        "type": "minecraft:uniform",
        "max": 1.0,
        "min": 0.0
      },
      "enchantment": "minecraft:looting",
      "function": "minecraft:enchanted_count_increase"
    }
  ],
  "name": "minecraft:leather"
}
```

### Anti-Patterns to Avoid
- **Don't use Mixin for loot table additions:** Data pack overrides are cleaner and don't require code
- **Don't hardcode bucket return in recipe:** Use `craftRemainder` on the item instead
- **Don't create separate items for iron vs copper bucket recipes:** Use two recipe files with different ingredients

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Recipe removal | Custom mixin per recipe | `REMOVED_RECIPE_PATHS` set | Centralized, declarative |
| Bucket preservation | Recipe JSON logic | `craftRemainder` on Item | Vanilla behavior, automatic |
| Loot table changes | LootTableEvents | JSON override | Simpler, no code needed |
| Item textures | Programmatic generation | PNG files | Standard approach |

**Key insight:** This phase is 95% data-driven. The only code change is adding the DOUGH item to THCItems.kt and "bread" to REMOVED_RECIPE_PATHS.

## Common Pitfalls

### Pitfall 1: Forgetting Both Bucket Types
**What goes wrong:** Player crafts with iron water bucket, gets nothing back
**Why it happens:** Only creating recipe for copper water bucket
**How to avoid:** Create two recipes: `dough.json` (iron) and `dough_copper.json` (copper)
**Warning signs:** Testing only with one bucket type

### Pitfall 2: Wrong Smelting XP/Time
**What goes wrong:** Dough takes too long to cook or gives wrong XP
**Why it happens:** Using arbitrary values instead of vanilla standards
**How to avoid:** Use vanilla bread-equivalent values (0.35 XP, 200 ticks furnace, 100 ticks smoker)
**Warning signs:** Compare with vanilla food cooking times

### Pitfall 3: Sheep Loot Table Complexity
**What goes wrong:** Wool drops break when adding leather
**Why it happens:** Sheep loot table has complex color-based wool drop logic
**How to avoid:** Add leather as a new pool, don't modify wool pool structure
**Warning signs:** Sheep dropping wrong wool colors or no wool

### Pitfall 4: Missing Smoking Recipe
**What goes wrong:** Dough only cooks in furnace, not smoker
**Why it happens:** Assuming smelting recipe works for all heating blocks
**How to avoid:** Create separate `minecraft:smoking` recipe
**Warning signs:** Smoker rejects dough item

### Pitfall 5: craftRemainder Not Working
**What goes wrong:** Buckets consumed in crafting
**Why it happens:** Custom bucket item missing `craftRemainder` property
**How to avoid:** Verify copper water bucket has `.craftRemainder(COPPER_BUCKET)` in THCItems.kt
**Warning signs:** Already set correctly in codebase (verified)

## Code Examples

Verified patterns from the THC codebase:

### Recipe Removal (RecipeManagerMixin.java)
```java
// Source: RecipeManagerMixin.java lines 22-43
@Unique
private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
    "shield",
    "wooden_spear",
    // ... existing entries ...
    "bread"  // ADD THIS
);
```

### Item Registration (THCItems.kt)
```kotlin
// Source: THCItems.kt SOUL_DUST pattern
@JvmField
val DOUGH: Item = register("dough") { key ->
    Item(
        Item.Properties()
            .setId(key)
            .stacksTo(64)
    )
}

// In init() function:
ItemGroupEvents.modifyEntriesEvent(foodTabKey).register { entries ->
    entries.accept(HONEY_APPLE)
    entries.accept(DOUGH)  // ADD THIS
}
```

### Item Model JSON
```json
// Source: soul_dust.json pattern
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "thc:item/dough"
  }
}
```

### Shapeless Recipe JSON
```json
// Source: honey_apple.json pattern
{
  "type": "minecraft:crafting_shapeless",
  "category": "food",
  "ingredients": [
    "minecraft:wheat",
    "minecraft:wheat",
    "minecraft:wheat",
    "minecraft:water_bucket"
  ],
  "result": {
    "count": 1,
    "id": "thc:dough"
  }
}
```

### Smelting Recipe JSON
```json
// Source: Minecraft Wiki, verified for 1.21
{
  "type": "minecraft:smelting",
  "category": "food",
  "ingredient": "thc:dough",
  "result": "minecraft:bread",
  "experience": 0.35,
  "cookingtime": 200
}
```

### Leather Drop Pool (for pig/sheep)
```json
// Source: cow.json leather pool (exact copy)
{
  "bonus_rolls": 0.0,
  "entries": [
    {
      "type": "minecraft:item",
      "functions": [
        {
          "add": false,
          "count": {
            "type": "minecraft:uniform",
            "max": 2.0,
            "min": 0.0
          },
          "function": "minecraft:set_count"
        },
        {
          "count": {
            "type": "minecraft:uniform",
            "max": 1.0,
            "min": 0.0
          },
          "enchantment": "minecraft:looting",
          "function": "minecraft:enchanted_count_increase"
        }
      ],
      "name": "minecraft:leather"
    }
  ],
  "rolls": 1.0
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NBT for item data | Data Components | MC 1.20.5+ | Use DataComponents API |
| Recipe data gen | JSON files | Always valid | Both work, JSON simpler for this |
| LootTableEvents | JSON overrides | Always valid | JSON preferred for additive changes |

**Deprecated/outdated:**
- None relevant to this phase

## Open Questions

Things that couldn't be fully resolved:

1. **Exact smelting XP value**
   - What we know: Vanilla food XP ranges from 0.1 (kelp) to 1.0 (cooked meat)
   - Recommendation: Use 0.35 (matches most cooked foods like porkchop)
   - Confidence: MEDIUM - reasonable default, can adjust

2. **Ingredient tag vs explicit items**
   - What we know: Recipe can use tag like `#minecraft:buckets_water` or explicit items
   - Recommendation: Use explicit items (`minecraft:water_bucket`, `thc:copper_bucket_of_water`) for clarity
   - Confidence: HIGH - simpler and more predictable

## Sources

### Primary (HIGH confidence)
- THCItems.kt - Existing item registration patterns
- RecipeManagerMixin.java - Recipe removal pattern
- honey_apple.json - Shapeless recipe pattern
- cow.json, pig.json, sheep.json - Loot table structures
- soul_dust.json - Item model pattern
- en_us.json - Language key pattern

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Recipe](https://minecraft.wiki/w/Recipe) - Smelting recipe format
- [Misode Recipe Generator](https://misode.github.io/recipe/) - Recipe JSON validation
- [Fabric Wiki - Recipe Types](https://fabricmc.net/wiki/tutorial:recipe_types_introduction) - Recipe type documentation

### Tertiary (LOW confidence)
- None - all patterns verified in codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All patterns exist in codebase
- Architecture: HIGH - Direct extension of existing patterns
- Pitfalls: HIGH - Based on codebase analysis and vanilla behavior

**Research date:** 2026-01-29
**Valid until:** 60 days (stable domain, no Minecraft updates expected)

## Implementation Checklist

Based on research, the implementation requires:

1. **Code changes (2 files):**
   - `RecipeManagerMixin.java`: Add "bread" to `REMOVED_RECIPE_PATHS`
   - `THCItems.kt`: Register DOUGH item, add to food creative tab

2. **New files (6 total):**
   - `data/thc/recipe/dough.json` - Iron water bucket recipe
   - `data/thc/recipe/dough_copper.json` - Copper water bucket recipe
   - `data/thc/recipe/dough_smelting.json` - Furnace recipe
   - `data/thc/recipe/dough_smoking.json` - Smoker recipe
   - `assets/thc/models/item/dough.json` - Item model
   - `assets/thc/lang/en_us.json` - Add dough translation

3. **Override files (2 total):**
   - `data/minecraft/loot_table/entities/pig.json` - Add leather pool
   - `data/minecraft/loot_table/entities/sheep.json` - Add leather pool

4. **Existing files (verified present):**
   - `assets/thc/textures/item/dough.png` - Texture already exists!

**Estimated complexity:** Low - all patterns established, mostly JSON configuration
