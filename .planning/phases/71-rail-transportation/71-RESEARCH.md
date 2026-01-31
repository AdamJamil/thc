# Phase 71: Rail Transportation - Research

**Researched:** 2026-01-31
**Domain:** Minecraft recipe modification (JSON data pack recipes)
**Confidence:** HIGH

## Summary

Phase 71 requires modifying rail crafting recipes to make rail transportation cheaper and more accessible. The implementation is straightforward JSON recipe file work with no code changes needed.

Key findings:
1. Vanilla rail recipes are already extracted in the project (`data/minecraft/recipe/*.json`)
2. Overriding vanilla recipes requires placing files in `data/minecraft/recipe/` namespace
3. Alternative ingredients can be specified using arrays in the key section
4. The project already has established patterns for yield increases (ladder 16x, arrow 16x)

**Primary recommendation:** Create two separate recipe files for rails (iron and copper variants), override vanilla powered rail recipe with increased yield. No custom tags needed.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric Data Pack | N/A | Recipe JSON files | Minecraft-native approach, no mod code |

### Supporting

No additional libraries needed - pure JSON recipe definitions.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Two separate rail recipes | Custom item tag `#thc:rail_materials` containing iron + copper | Tag approach more elegant but adds complexity; two recipes is simpler and matches dough pattern |
| Array syntax `["minecraft:iron_ingot", "minecraft:copper_ingot"]` | Separate recipe files | Array in one file is cleaner but this project uses separate files for alternative recipes (see dough.json + dough_copper.json) |

## Architecture Patterns

### Recommended File Structure

```
src/main/resources/data/
├── minecraft/recipe/
│   ├── rail.json           # Override: iron rails, yield 64
│   └── powered_rail.json   # Override: yield 64 (keep gold)
└── thc/recipe/
    └── rail_copper.json    # Alternative: copper rails
```

### Pattern 1: Override Vanilla Recipe (Increased Yield)

**What:** Replace vanilla recipe with modified version
**When to use:** Changing yield or ingredients of existing vanilla items
**Example:**

```json
// Source: Existing project pattern from ladder.json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "#": "minecraft:stick",
    "X": "minecraft:iron_ingot"
  },
  "pattern": [
    "X X",
    "X#X",
    "X X"
  ],
  "result": {
    "count": 64,
    "id": "minecraft:rail"
  }
}
```

### Pattern 2: Alternative Recipe (Different Namespace)

**What:** Add new recipe that produces same vanilla item
**When to use:** Adding alternative crafting path without removing original
**Example:**

```json
// Source: Existing project pattern from dough_copper.json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "#": "minecraft:stick",
    "X": "minecraft:copper_ingot"
  },
  "pattern": [
    "X X",
    "X#X",
    "X X"
  ],
  "result": {
    "count": 64,
    "id": "minecraft:rail"
  }
}
```

### Anti-Patterns to Avoid

- **Using `minecraft` namespace for alternative recipes:** Vanilla namespace overrides, not adds. Use `thc` namespace for additional recipes.
- **Creating custom tags for single-use cases:** Two recipe files is simpler than creating and maintaining a custom tag when only used in one place.
- **Changing activator_rail or detector_rail:** Requirements only specify rail and powered_rail. Don't expand scope.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Recipe alternatives | Custom crafting logic in code | JSON recipe files | Minecraft handles recipe matching natively |
| Multiple valid inputs | Ingredient validation code | Array syntax or separate recipes | Recipe system already supports this |

**Key insight:** Recipe modifications are pure data. No mixin, no event handler, no code needed.

## Common Pitfalls

### Pitfall 1: Wrong Namespace for Overrides

**What goes wrong:** Recipe doesn't override vanilla if placed in wrong namespace
**Why it happens:** Confusion between adding vs overriding
**How to avoid:**
- Override vanilla = `data/minecraft/recipe/`
- Add alternative = `data/thc/recipe/`
**Warning signs:** Both original and modified recipes appear in recipe book

### Pitfall 2: Missing Category Field

**What goes wrong:** Recipe may not categorize correctly in recipe book
**Why it happens:** Category is optional but helpful
**How to avoid:** Include `"category": "misc"` for rails (matches vanilla)
**Warning signs:** Recipe appears in wrong tab or no tab

### Pitfall 3: Count vs Yield Confusion

**What goes wrong:** Misunderstanding what "64 per craft" means
**Why it happens:** Vanilla rail uses 6 iron ingots for 16 rails
**How to avoid:**
- Requirement says "yield 64 per recipe" not "use fewer materials"
- Keep same input counts, just increase output count to 64
**Warning signs:** Recipe feels unbalanced (too few inputs for 64 outputs)

## Code Examples

### Rail Recipe Override (Iron, 64 yield)

```json
// File: src/main/resources/data/minecraft/recipe/rail.json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "#": "minecraft:stick",
    "X": "minecraft:iron_ingot"
  },
  "pattern": [
    "X X",
    "X#X",
    "X X"
  ],
  "result": {
    "count": 64,
    "id": "minecraft:rail"
  }
}
```

### Rail Copper Alternative Recipe

```json
// File: src/main/resources/data/thc/recipe/rail_copper.json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "#": "minecraft:stick",
    "X": "minecraft:copper_ingot"
  },
  "pattern": [
    "X X",
    "X#X",
    "X X"
  ],
  "result": {
    "count": 64,
    "id": "minecraft:rail"
  }
}
```

### Powered Rail Recipe Override (64 yield)

```json
// File: src/main/resources/data/minecraft/recipe/powered_rail.json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "#": "minecraft:stick",
    "R": "minecraft:redstone",
    "X": "minecraft:gold_ingot"
  },
  "pattern": [
    "X X",
    "X#X",
    "XRX"
  ],
  "result": {
    "count": 64,
    "id": "minecraft:powered_rail"
  }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `recipes/` folder | `recipe/` folder | MC 1.21 | Path changed, must use singular |
| Object syntax `{"item": "..."}` | String syntax `"minecraft:item"` | MC 1.20+ | Simplified key definitions |

**Deprecated/outdated:**
- `recipes/` (plural) folder: Changed to `recipe/` (singular) in 1.21
- Verbose object syntax for simple items: Can now use direct string IDs

## Open Questions

None. Implementation is straightforward with established patterns in this codebase.

## Requirements Mapping

| Requirement | Implementation |
|-------------|----------------|
| RAIL-01: Rails craftable with copper OR iron | Two recipes: override `minecraft:rail` (iron) + add `thc:rail_copper` |
| RAIL-02: Rails yield 64 per recipe (4x vanilla) | Set `count: 64` in result (vanilla = 16) |
| RAIL-03: Powered rails yield 64 per recipe (~10x vanilla) | Override `minecraft:powered_rail` with `count: 64` (vanilla = 6) |

## Sources

### Primary (HIGH confidence)

- Project codebase analysis:
  - `/mnt/c/home/code/thc/data/minecraft/recipe/rail.json` - Vanilla recipe structure
  - `/mnt/c/home/code/thc/data/minecraft/recipe/powered_rail.json` - Vanilla powered rail
  - `/mnt/c/home/code/thc/src/main/resources/data/minecraft/recipe/ladder.json` - Yield increase pattern
  - `/mnt/c/home/code/thc/src/main/resources/data/thc/recipe/dough_copper.json` - Alternative recipe pattern
- [Minecraft Wiki - Recipe](https://minecraft.wiki/w/Recipe) - JSON format specification
- [Fabric Wiki - Crafting Recipes](https://wiki.fabricmc.net/tutorial:recipes) - Fabric recipe patterns

### Secondary (MEDIUM confidence)

- None needed - all patterns verified in codebase

### Tertiary (LOW confidence)

- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Pure JSON, no external libraries
- Architecture: HIGH - Existing patterns in codebase, verified
- Pitfalls: HIGH - Based on Minecraft documentation and project analysis

**Research date:** 2026-01-31
**Valid until:** Indefinite (recipe JSON format is stable)
