# Phase 56: Acquisition Gating - Research

**Researched:** 2026-01-28
**Domain:** Minecraft loot table modification, enchantment book drops, stage-based enchantment gating
**Confidence:** HIGH

## Summary

This phase requires two complementary systems:
1. **Filtering**: Remove stage 3+ enchantment books and enchanted items from chest/fishing loot
2. **Adding**: Add enchantment book drops to specific mob loot tables

The codebase already uses `LootTableEvents.MODIFY_DROPS` to filter items from all loot tables (shields, spears, bows, etc.) and apply enchantment corrections. This existing pattern extends naturally to filter stage 3+ enchantments. Mob loot table modifications should use data pack JSON files (already the pattern for mob drops), with new pools for enchanted book drops.

**Primary recommendation:** Extend the existing `LootTableEvents.MODIFY_DROPS` handler to filter stage 3+ items, and add new loot table JSON pools to mob entity files for enchanted book drops.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric Loot API v3 | 0.x | Runtime loot modification | Already used in THC.kt for MODIFY_DROPS |
| Data Pack JSON | 1.21.11 | Static loot table definitions | Already used for all mob/chest loot |
| DataComponents | 1.21+ | Item enchantment inspection | Already used in EnchantmentEnforcement.kt |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| EnchantmentEnforcement | THC | Stage classification | Check if enchant is stage 3+ |
| ItemEnchantments | MC 1.21+ | Enchantment data access | Read stored/active enchantments |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MODIFY_DROPS filter | Data pack replacement | Data pack is more static, harder to handle dynamic stage checks |
| JSON loot pools | MODIFY event injection | JSON is cleaner for additive changes, code would be verbose |

**Installation:**
No additional dependencies needed - all APIs already in use.

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/thc/
├── enchant/
│   └── EnchantmentEnforcement.kt  # Add stage 3+ classification method
├── THC.kt                          # Extend MODIFY_DROPS handler
│
data/minecraft/loot_table/entities/
├── drowned.json     # Add aqua_affinity, depth_strider, frost_walker, respiration pools
├── spider.json      # Add bane_of_arthropods pool
├── husk.json        # Add smite pool
├── stray.json       # Add smite pool
├── blaze.json       # Add fire_protection pool
├── magma_cube.json  # Add flame, fire_aspect pools
│
data/minecraft/loot_table/gameplay/fishing/
└── treasure.json    # Remove enchanted book entry (or filter via MODIFY_DROPS)
```

### Pattern 1: Stage 3+ Detection
**What:** Classify enchantments by stage for filtering
**When to use:** When checking if an item should be removed from loot
**Example:**
```kotlin
// Source: EnchantmentEnforcement.kt (existing pattern)
fun isStage3Plus(enchantId: String?): Boolean {
    if (enchantId == null) return false
    // Stage 1-2 includes: mending, unbreaking, efficiency, fortune, silk_touch, lure, luck_of_the_sea
    return !STAGE_1_2_ENCHANTMENTS.contains(enchantId) &&
           !REMOVED_ENCHANTMENTS.contains(enchantId)
}
```

### Pattern 2: MODIFY_DROPS Filtering
**What:** Runtime removal of items from loot drops
**When to use:** When removing items that can appear in multiple loot tables
**Example:**
```kotlin
// Source: THC.kt lines 171-182 (existing pattern)
LootTableEvents.MODIFY_DROPS.register { _, _, drops ->
    drops.removeIf { stack ->
        // Remove enchanted books with stage 3+ enchantments
        if (stack.`is`(Items.ENCHANTED_BOOK)) {
            val stored = stack.get(DataComponents.STORED_ENCHANTMENTS)
            stored?.entrySet()?.any { entry ->
                val enchantId = entry.key.unwrapKey().orElse(null)?.identifier()?.toString()
                isStage3Plus(enchantId)
            } == true
        } else {
            // Remove items with stage 3+ enchantments
            val enchants = stack.get(DataComponents.ENCHANTMENTS)
            enchants?.entrySet()?.any { entry ->
                val enchantId = entry.key.unwrapKey().orElse(null)?.identifier()?.toString()
                isStage3Plus(enchantId)
            } == true
        }
    }
}
```

### Pattern 3: Enchanted Book Drop Pool (JSON)
**What:** Add enchanted book with specific enchantment to mob loot
**When to use:** Adding new drops to existing mob loot tables
**Example:**
```json
// Source: Minecraft loot table schema
{
  "bonus_rolls": 0.0,
  "conditions": [
    {
      "condition": "minecraft:entity_properties",
      "entity": "this",
      "predicate": {
        "flags": {
          "is_baby": false
        }
      }
    },
    {
      "condition": "minecraft:random_chance_with_enchanted_bonus",
      "enchantment": "minecraft:looting",
      "enchanted_chance": {
        "type": "minecraft:linear",
        "base": 0.035,
        "per_level_above_first": 0.0
      },
      "unenchanted_chance": 0.025
    }
  ],
  "entries": [
    {
      "type": "minecraft:item",
      "name": "minecraft:enchanted_book",
      "functions": [
        {
          "function": "minecraft:set_enchantments",
          "enchantments": {
            "minecraft:aqua_affinity": 1
          }
        }
      ]
    }
  ],
  "rolls": 1.0
}
```

### Anti-Patterns to Avoid
- **Replacing entire loot tables:** Use additive pools, not full replacements (loses vanilla updates)
- **killed_by_player condition:** Requirements say "any death source", not player-only
- **Single pool for multiple books:** Use separate pools so each book rolls independently

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Enchantment level normalization | Custom level setter | EnchantmentEnforcement.correctStack() | Already handles level normalization |
| Stage classification | Hardcoded checks | STAGE_1_2_ENCHANTMENTS set | Centralized, maintainable |
| Loot filtering | Per-table data packs | MODIFY_DROPS event | Catches all tables, dynamic |

**Key insight:** The existing EnchantmentEnforcement.kt and LootTableEvents.MODIFY_DROPS pattern in THC.kt already handle 90% of the work. Extend, don't rebuild.

## Common Pitfalls

### Pitfall 1: Forgetting to Update STAGE_1_2_ENCHANTMENTS
**What goes wrong:** Lure and luck_of_the_sea get filtered as stage 3+ (they're fishing rod enchantments not in current list)
**Why it happens:** CONTEXT.md specifies these should be stage 1-2 but current EnchantmentEnforcement.kt doesn't include them
**How to avoid:** Update STAGE_1_2_ENCHANTMENTS before implementing filter
**Warning signs:** Fishing rod enchantments disappearing from lectern drops

### Pitfall 2: Using linear Looting Bonus Instead of Flat
**What goes wrong:** Looting III gives +3% instead of flat +1%
**Why it happens:** Default random_chance_with_enchanted_bonus uses per_level_above_first
**How to avoid:** Set per_level_above_first to 0.0, base to (unenchanted_chance + 0.01)
**Warning signs:** Drop rates scaling with looting level

### Pitfall 3: Baby Mobs Dropping Books
**What goes wrong:** Baby zombies/husks drop enchanted books
**Why it happens:** Missing is_baby: false condition
**How to avoid:** Add entity_properties condition with flags.is_baby: false
**Warning signs:** Excessive book drops from zombie pigmen spawners

### Pitfall 4: Books Not Getting Level Normalized
**What goes wrong:** Dropped enchanted books have wrong levels (e.g., Fire Aspect II instead of I)
**Why it happens:** JSON set_enchantments bypasses EnchantmentEnforcement
**How to avoid:** Set correct levels in JSON matching INTERNAL_LEVELS, or rely on MODIFY_DROPS correctStack()
**Warning signs:** Inconsistent enchantment levels on mob-dropped vs table-applied books

### Pitfall 5: Filtering Removes Vanilla Stage 1-2 Books
**What goes wrong:** Mending books disappear from Ancient City loot
**Why it happens:** Filter is too aggressive, catches all enchanted books
**How to avoid:** Check each enchantment on the book, only filter if ANY is stage 3+
**Warning signs:** No enchanted books appearing anywhere

## Code Examples

Verified patterns from official sources and existing codebase:

### Flat Looting Bonus (2.5% base, 3.5% with any Looting)
```json
// Source: Minecraft Wiki random_chance_with_enchanted_bonus
{
  "condition": "minecraft:random_chance_with_enchanted_bonus",
  "enchantment": "minecraft:looting",
  "enchanted_chance": {
    "type": "minecraft:linear",
    "base": 0.035,
    "per_level_above_first": 0.0
  },
  "unenchanted_chance": 0.025
}
```

### Adults-Only Drop Condition
```json
// Source: data/minecraft/loot_table/entities/zombie.json lines 153-158
{
  "condition": "minecraft:entity_properties",
  "entity": "this",
  "predicate": {
    "flags": {
      "is_baby": false
    }
  }
}
```

### Set Specific Enchantment on Book
```json
// Source: Minecraft Wiki Item modifier set_enchantments
{
  "type": "minecraft:item",
  "name": "minecraft:enchanted_book",
  "functions": [
    {
      "function": "minecraft:set_enchantments",
      "enchantments": {
        "minecraft:aqua_affinity": 1
      }
    }
  ]
}
```

### Check for Stage 3+ Enchantment (Kotlin)
```kotlin
// Source: EnchantmentEnforcement.kt (pattern extension)
fun hasStage3PlusEnchantment(enchantments: ItemEnchantments?): Boolean {
    if (enchantments == null || enchantments.isEmpty) return false
    return enchantments.entrySet().any { entry ->
        val enchantId = entry.key.unwrapKey().orElse(null)?.identifier()?.toString()
        enchantId != null &&
            !STAGE_1_2_ENCHANTMENTS.contains(enchantId) &&
            !REMOVED_ENCHANTMENTS.contains(enchantId)
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| set_nbt for enchantments | set_enchantments function | MC 1.20.5+ | Components replaced NBT |
| LootTableEvents.MODIFY | LootTableEvents.MODIFY_DROPS | Fabric API v3 | Simpler for filtering |
| Separate treasure tag | #minecraft:on_random_loot | MC 1.21+ | Combined tag for loot enchants |

**Deprecated/outdated:**
- `set_nbt` function: Use `set_enchantments` or `set_components` instead
- `LootTableEvents.MODIFY` for filtering: MODIFY_DROPS is cleaner for removal

## Open Questions

Things that couldn't be fully resolved:

1. **Inverted is_baby condition syntax**
   - What we know: `is_baby: false` works in flags predicate
   - What's unclear: Whether to use inverted condition wrapper or direct false value
   - Recommendation: Use direct `"is_baby": false` in flags (confirmed in zombie.json)

2. **Magma Cube size check for books**
   - What we know: Magma cream only drops from size >= 2 cubes
   - What's unclear: Should books follow same size restriction?
   - Recommendation: Add same size condition for consistency (size >= 2)

## Sources

### Primary (HIGH confidence)
- `/mnt/c/home/code/thc/src/main/kotlin/thc/THC.kt` - Existing MODIFY_DROPS pattern
- `/mnt/c/home/code/thc/src/main/kotlin/thc/enchant/EnchantmentEnforcement.kt` - Stage classification
- `/mnt/c/home/code/thc/data/minecraft/loot_table/entities/zombie.json` - is_baby condition syntax
- `/mnt/c/home/code/thc/data/minecraft/loot_table/entities/husk.json` - random_chance_with_enchanted_bonus syntax

### Secondary (MEDIUM confidence)
- [Fabric API LootTableEvents v3](https://maven.fabricmc.net/docs/fabric-api-0.129.0+1.21.7/net/fabricmc/fabric/api/loot/v3/LootTableEvents.html) - MODIFY_DROPS API
- [Minecraft Wiki Loot Table](https://minecraft.wiki/w/Loot_table) - Condition and function syntax
- [Minecraft Wiki Item Modifier](https://minecraft.wiki/w/Item_modifier) - set_enchantments function

### Tertiary (LOW confidence)
- WebSearch results for flat looting bonus - confirmed by codebase examples

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All patterns already exist in codebase
- Architecture: HIGH - Direct extension of existing MODIFY_DROPS and JSON patterns
- Pitfalls: MEDIUM - Some based on logical inference from requirements

**Research date:** 2026-01-28
**Valid until:** 60 days (stable patterns, MC 1.21.11 specific)

---

## Implementation Checklist

For planner reference:

- [ ] Update STAGE_1_2_ENCHANTMENTS to include lure, luck_of_the_sea
- [ ] Add isStage3Plus() helper to EnchantmentEnforcement
- [ ] Extend MODIFY_DROPS to filter stage 3+ enchanted books
- [ ] Extend MODIFY_DROPS to filter items with stage 3+ enchantments
- [ ] Add drowned.json pools (4 enchantments, 2.5% each)
- [ ] Add spider.json pool (bane_of_arthropods, 2.5%)
- [ ] Add husk.json pool (smite, 2.5%)
- [ ] Add stray.json pool (smite, 2.5%)
- [ ] Add blaze.json pool (fire_protection, 2.5%)
- [ ] Add magma_cube.json pools (flame 5%, fire_aspect 5%)
- [ ] Verify fishing/treasure.json books get filtered

### Loot Table Pool Template

For each mob drop, use this pool structure:
```json
{
  "bonus_rolls": 0.0,
  "conditions": [
    {
      "condition": "minecraft:entity_properties",
      "entity": "this",
      "predicate": {
        "flags": {
          "is_baby": false
        }
      }
    },
    {
      "condition": "minecraft:random_chance_with_enchanted_bonus",
      "enchantment": "minecraft:looting",
      "enchanted_chance": {
        "type": "minecraft:linear",
        "base": BASE_PLUS_LOOTING,
        "per_level_above_first": 0.0
      },
      "unenchanted_chance": BASE_CHANCE
    }
  ],
  "entries": [
    {
      "type": "minecraft:item",
      "name": "minecraft:enchanted_book",
      "functions": [
        {
          "function": "minecraft:set_enchantments",
          "enchantments": {
            "ENCHANTMENT_ID": LEVEL
          }
        }
      ]
    }
  ],
  "rolls": 1.0
}
```

Where:
- BASE_CHANCE = 0.025 for 2.5%, 0.05 for 5%
- BASE_PLUS_LOOTING = 0.035 for 2.5%+1%, 0.06 for 5%+1%
- ENCHANTMENT_ID = full minecraft:xxx id
- LEVEL = value from INTERNAL_LEVELS or default 1
