# Phase 65: Enchantment Compatibility - Research

**Researched:** 2026-01-29
**Domain:** Minecraft enchantment exclusivity, data-driven enchantment system, tag overrides
**Confidence:** HIGH

## Summary

Phase 65 removes mutual exclusivity between protection enchantments (protection, blast_protection, fire_protection, projectile_protection) and damage enchantments (sharpness, smite, bane_of_arthropods) so they can all coexist on the same gear.

The implementation is purely data-driven via tag overrides - no mixins required. Minecraft 1.21+ uses data-driven enchantments where exclusivity is controlled by:
1. The `exclusive_set` field in each enchantment's JSON definition (e.g., `"exclusive_set": "#minecraft:exclusive_set/armor"`)
2. Tag files that list enchantments in each exclusive set (e.g., `data/minecraft/tags/enchantment/exclusive_set/armor.json`)

The `Enchantment.areCompatible()` static method checks if either enchantment's `exclusiveSet` HolderSet contains the other. By overriding the vanilla tags with empty sets (`"values": []`), we break the exclusivity link and allow stacking.

**Primary recommendation:** Override `data/minecraft/tags/enchantment/exclusive_set/armor.json` and `data/minecraft/tags/enchantment/exclusive_set/damage.json` with empty value arrays using `"replace": true`. This is a data-only change requiring no code.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Minecraft Data Pack | 1.21+ | Tag overrides for enchantment exclusivity | Data-driven enchantment system introduced in 1.21 |
| Fabric Loader | - | Mod resource loading | Loads mod data packs automatically |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| None needed | - | - | Pure data pack solution |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Tag override with `replace: true` | Mixin on `Enchantment.areCompatible()` | Tag override is simpler, data-only, no compilation. Mixin would be overkill for this use case |
| Empty tag values | Individual enchantment JSON overrides | Would need to copy entire enchantment definition; tag override is cleaner |

**Installation:** No code changes - add JSON files to `src/main/resources/data/minecraft/tags/enchantment/exclusive_set/`

## Architecture Patterns

### Recommended Project Structure
```
src/main/resources/
└── data/
    └── minecraft/
        └── tags/
            └── enchantment/
                └── exclusive_set/
                    ├── armor.json    # Override: empty values array
                    └── damage.json   # Override: empty values array
```

### Pattern 1: Tag Override with Replace
**What:** Override vanilla tag files to empty the exclusive set
**When to use:** When removing all entries from a vanilla tag
**Example:**
```json
// Source: Minecraft Wiki - Data pack tag format
// File: src/main/resources/data/minecraft/tags/enchantment/exclusive_set/armor.json
{
  "replace": true,
  "values": []
}
```

### Pattern 2: Enchantment Effect Stacking
**What:** Minecraft's data-driven effect system naturally stacks multiple enchantment effects
**When to use:** Understanding why stacked enchantments work
**How it works:**
- Protection enchantments use `"minecraft:damage_protection"` effect with `"type": "minecraft:add"`
- Damage enchantments use `"minecraft:damage"` effect with `"type": "minecraft:add"`
- Multiple enchantments with additive effects on the same item stack their values
- No code needed - the effect system handles stacking automatically

### Anti-Patterns to Avoid
- **Don't copy entire enchantment JSONs:** Only override the tag files; modifying enchantment definitions is unnecessary and error-prone
- **Don't mixin `areCompatible()`:** Tag overrides achieve the same result with zero code
- **Don't use `"replace": false`:** Must use `"replace": true` to clear vanilla entries; `false` would merge and keep exclusivity

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Removing enchantment exclusivity | Mixin on `Enchantment.areCompatible()` | Tag override with `replace: true` | Data-driven solution is simpler and guaranteed to work |
| Stacking enchantment effects | Custom damage/protection calculation | Vanilla effect system | Effects already stack additively when on same item |
| Per-enchantment exclusivity removal | Individual enchantment JSON overrides | Tag files | Single tag file affects all enchantments in set |

**Key insight:** Since 1.21, Minecraft's enchantment system is fully data-driven. Tag overrides are the canonical way to modify enchantment relationships.

## Common Pitfalls

### Pitfall 1: Forgetting `"replace": true`
**What goes wrong:** Tag values merge with vanilla instead of replacing
**Why it happens:** Default behavior (`replace: false`) merges entries
**How to avoid:** Always use `"replace": true` when intending to empty a tag
**Warning signs:** Enchantments still mutually exclusive after adding tag file

### Pitfall 2: Wrong Directory Path
**What goes wrong:** Tag override not loaded
**Why it happens:** Fabric/Minecraft require exact namespace and path matching
**How to avoid:** Path must be exactly `data/minecraft/tags/enchantment/exclusive_set/armor.json`
**Warning signs:** No errors, but exclusivity unchanged

### Pitfall 3: Assuming Code is Needed
**What goes wrong:** Creating unnecessary mixin when data pack suffices
**Why it happens:** Older Minecraft versions required code for enchantment modifications
**How to avoid:** Check if the change can be data-driven first (it can for exclusivity)
**Warning signs:** Writing a mixin that duplicates data pack capability

### Pitfall 4: Breaking Other Mod Compatibility
**What goes wrong:** `replace: true` can break other mods that modify the same tag
**Why it happens:** Replace overwrites ALL entries, including those from other mods
**How to avoid:** For THC, this is intentional - we want empty exclusive sets. If future compatibility is needed, consider mixin approach
**Warning signs:** Other mods' enchantment modifications not working

## Code Examples

Verified patterns from Minecraft data pack format:

### Empty Armor Exclusive Set
```json
// File: src/main/resources/data/minecraft/tags/enchantment/exclusive_set/armor.json
// Purpose: Remove exclusivity between protection, blast_protection, fire_protection, projectile_protection
{
  "replace": true,
  "values": []
}
```

### Empty Damage Exclusive Set
```json
// File: src/main/resources/data/minecraft/tags/enchantment/exclusive_set/damage.json
// Purpose: Remove exclusivity between sharpness, smite, bane_of_arthropods (and impaling, density, breach)
{
  "replace": true,
  "values": []
}
```

### How areCompatible() Works (Reference Only)
```java
// Source: Decompiled Enchantment.class bytecode
// This method is called by LecternEnchanting.kt and EnchantmentMenuMixin.java
public static boolean areCompatible(Holder<Enchantment> a, Holder<Enchantment> b) {
    // Returns false if:
    // 1. Same enchantment (a.equals(b))
    // 2. a's exclusiveSet contains b
    // 3. b's exclusiveSet contains a
    //
    // With empty exclusive sets, only #1 returns false
    // Result: Different enchantments always compatible
}
```

### Existing THC Compatibility Check Pattern
```java
// Source: EnchantmentMenuMixin.java lines 226-231
// This code will automatically allow stacking once exclusive sets are empty
for (Holder<Enchantment> existing : existingEnchants.keySet()) {
    if (!Enchantment.areCompatible(enchantHolder, existing)) {
        return false;
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hardcoded enchantment exclusivity | Data-driven via `exclusive_set` field | MC 1.21 | Exclusivity now modifiable via data packs |
| `Enchantment.checkCompatibility()` method | `Enchantment.areCompatible()` + HolderSet | MC 1.21 | Uses registry holder pattern |
| Direct exclusivity modification required mixin | Tag overrides via data packs | MC 1.21 | Zero code solution available |

**Deprecated/outdated:**
- `checkCompatibility()` method: Replaced by `areCompatible()` with HolderSet-based exclusivity

## Open Questions

Things that couldn't be fully resolved:

1. **Other Exclusive Sets Affected by `exclusive_set/damage`**
   - What we know: The `damage.json` tag includes `impaling`, `density`, and `breach` in addition to the core three (sharpness, smite, bane_of_arthropods)
   - What's unclear: Whether ENCH-02 intends to also remove exclusivity for impaling/density/breach
   - Recommendation: Proceed with emptying the entire `damage` exclusive set since impaling and density are in THC's REMOVED_ENCHANTMENTS list anyway. Breach is new in 1.21 and likely also intended to stack.

2. **Mod Compatibility with `replace: true`**
   - What we know: Using `replace: true` overwrites entries from other mods
   - What's unclear: Whether THC players commonly use mods that add enchantments to these exclusive sets
   - Recommendation: Proceed with `replace: true` since THC is designed as a comprehensive overhaul. Document this behavior for users.

## Sources

### Primary (HIGH confidence)
- `/mnt/c/home/code/thc/data/minecraft/tags/enchantment/exclusive_set/armor.json` - Vanilla armor exclusive set (protection enchantments)
- `/mnt/c/home/code/thc/data/minecraft/tags/enchantment/exclusive_set/damage.json` - Vanilla damage exclusive set (damage enchantments)
- `/mnt/c/home/code/thc/data/minecraft/enchantment/protection.json` - Protection enchantment definition showing `exclusive_set` field
- `/mnt/c/home/code/thc/data/minecraft/enchantment/sharpness.json` - Sharpness enchantment definition showing `exclusive_set` field
- Decompiled `Enchantment.class` bytecode - `areCompatible()` method implementation
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/EnchantmentMenuMixin.java` - Existing THC pattern for compatibility checks
- `/mnt/c/home/code/thc/src/main/kotlin/thc/lectern/LecternEnchanting.kt` - Existing THC pattern for compatibility checks

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Data pack](https://minecraft.wiki/w/Data_pack) - Tag format documentation
- [Fabric Wiki - Tags](https://fabricmc.net/wiki/tutorial:tags) - Fabric tag handling

### Tertiary (LOW confidence)
- None - all critical findings verified from codebase analysis

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Verified from vanilla data files and decompiled bytecode
- Architecture: HIGH - Data-pack tag override is documented Minecraft feature
- Pitfalls: HIGH - Verified through analysis of how `areCompatible()` uses `exclusiveSet`
- Effect stacking: HIGH - Verified from enchantment JSON effect definitions using additive types

**Research date:** 2026-01-29
**Valid until:** ~90 days (stable Minecraft 1.21+ data-driven enchantment API)
