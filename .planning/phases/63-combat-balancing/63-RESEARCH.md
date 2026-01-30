# Phase 63: Combat Balancing - Research

**Researched:** 2026-01-29
**Domain:** Arrow recipe modification and mob projectile damage reduction
**Confidence:** HIGH

## Summary

Phase 63 requires three combat balance changes: increasing arrow craft yield, and reducing damage from Pillager crossbow arrows and Stray bow arrows. The codebase has established patterns for all three scenarios.

For the arrow recipe (CMBT-01), a simple data pack recipe override matches the existing ladder.json pattern - copy vanilla recipe JSON with modified count. For projectile damage reduction (CMBT-02, CMBT-03), the AbstractArrowMixin already demonstrates the pattern: intercept `onHitEntity` HEAD, check `getOwner()` entity type, modify `baseDamage` field before vanilla processing.

The key insight is that AbstractArrowMixin currently only handles player-shot arrows. Extending it to handle enemy arrows (Pillager, Stray) requires adding entity type checks for these specific mob owners and applying percentage-based damage reduction.

**Primary recommendation:** Create arrow recipe override in data pack; extend AbstractArrowMixin with Pillager/Stray owner checks to reduce baseDamage proportionally.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Data pack recipes | MC 1.21.11 | Recipe override | Existing pattern (ladder.json, bow.json) |
| Fabric Mixin | 0.12.5+ | Arrow damage interception | Existing AbstractArrowMixin pattern |
| AbstractArrowAccessor | existing | baseDamage field access | Already implemented in codebase |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| EntityType | MC 1.21.11 | Owner type checking | Distinguish Pillager/Stray from other mobs |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Recipe override JSON | RecipeManagerMixin | JSON simpler, less code, data-driven |
| Extend AbstractArrowMixin | Separate mixin per mob | Centralized arrow logic preferred |
| onHitEntity baseDamage mod | ENTITY_LOAD damage set | Owner not always set at ENTITY_LOAD |

## Architecture Patterns

### Recommended Project Structure
```
src/main/resources/data/minecraft/recipe/
└── arrow.json                 # Override: count 16 (from 4)

src/main/java/thc/mixin/
└── AbstractArrowMixin.java    # EXTEND: add Pillager/Stray damage reduction
```

### Pattern 1: Recipe Override JSON
**What:** Place recipe JSON in `data/minecraft/recipe/` to override vanilla recipe
**When to use:** CMBT-01 arrow yield increase
**Example:**
```json
// Source: Existing ladder.json pattern
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "#": "minecraft:stick",
    "X": "minecraft:flint",
    "Y": "minecraft:feather"
  },
  "pattern": [
    "X",
    "#",
    "Y"
  ],
  "result": {
    "count": 16,
    "id": "minecraft:arrow"
  }
}
```

### Pattern 2: Owner-Based Projectile Damage Modification
**What:** Check arrow owner entity type in onHitEntity, modify baseDamage before vanilla processing
**When to use:** CMBT-02 Pillager, CMBT-03 Stray damage reduction
**Example:**
```java
// Source: Existing AbstractArrowMixin pattern extended
@Inject(method = "onHitEntity", at = @At("HEAD"))
private void thc$reduceEnemyArrowDamage(EntityHitResult result, CallbackInfo ci) {
    AbstractArrow self = (AbstractArrow) (Object) this;
    Entity owner = self.getOwner();

    // Pillager crossbow arrows: reduce damage
    if (owner != null && owner.getType() == EntityType.PILLAGER) {
        // Vanilla: 5-7 damage (Hard), Target: 3-5
        // Multiplier: 4/6 = 0.667
        baseDamage = baseDamage * 0.667;
    }

    // Stray bow arrows: reduce damage
    if (owner != null && owner.getType() == EntityType.STRAY) {
        // Vanilla: 4-8 damage (Hard), Target: 2-4
        // Multiplier: 3/6 = 0.5
        baseDamage = baseDamage * 0.5;
    }
}
```

### Anti-Patterns to Avoid
- **Separate mixins per mob:** Keep all arrow damage logic in AbstractArrowMixin
- **Hardcoded damage values:** Use multipliers to preserve difficulty scaling
- **ENTITY_LOAD arrow modification:** Owner may not be set at spawn time for arrows

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Recipe yield change | RecipeManagerMixin | Data pack JSON override | Simpler, data-driven |
| Arrow damage detection | Custom projectile events | onHitEntity mixin | Already established pattern |
| Owner type check | instanceof chains | EntityType comparison | MC 1.21.11 pattern |

**Key insight:** Arrow baseDamage modification must happen in onHitEntity HEAD (before damage is applied), not at arrow spawn, because arrows may be spawned before owner is set.

## Common Pitfalls

### Pitfall 1: Owner Null at Spawn
**What goes wrong:** Checking owner at ENTITY_LOAD returns null
**Why it happens:** Arrows set owner after entity creation in some code paths
**How to avoid:** Check owner in onHitEntity, not at spawn/ENTITY_LOAD
**Warning signs:** Damage modification never triggers, owner checks fail

### Pitfall 2: Damage vs baseDamage Confusion
**What goes wrong:** Modifying wrong damage variable
**Why it happens:** AbstractArrow has both baseDamage (field) and damage calculation (method)
**How to avoid:** Modify the private baseDamage field via @Shadow, before vanilla calculates final damage
**Warning signs:** Damage unchanged, or changes don't match expected multiplier

### Pitfall 3: Stray Slowness Arrow Confusion
**What goes wrong:** Thinking Stray arrows have different base damage than Skeleton
**Why it happens:** Stray arrows add Slowness effect, but same base damage (4-8 Hard)
**How to avoid:** Apply same damage calculation approach; slowness is separate effect
**Warning signs:** None - this is documentation clarification

### Pitfall 4: Recipe Override Path
**What goes wrong:** Recipe not applied because wrong namespace/path
**Why it happens:** Must be `data/minecraft/recipe/arrow.json` to override vanilla
**How to avoid:** Use exact path `minecraft` namespace, exact filename `arrow.json`
**Warning signs:** Both recipes appear, or vanilla recipe still works

### Pitfall 5: Pillager Melee vs Ranged
**What goes wrong:** Melee pillager (iron sword) somehow affected by arrow changes
**Why it happens:** Confusion between melee and ranged variants
**How to avoid:** Arrow damage reduction only affects crossbow-using Pillagers (the ones shooting arrows); melee Pillagers don't shoot arrows
**Warning signs:** N/A - melee Pillagers don't have arrows to modify

## Code Examples

Verified patterns from codebase:

### Existing Recipe Override (ladder.json)
```json
// Source: /mnt/c/home/code/thc/src/main/resources/data/minecraft/recipe/ladder.json
{
  "type": "minecraft:crafting_shaped",
  "category": "building",
  "key": {
    "S": "minecraft:stick"
  },
  "pattern": [
    "S S",
    "SSS",
    "S S"
  ],
  "result": {
    "count": 16,
    "id": "minecraft:ladder"
  }
}
```

### Existing AbstractArrowMixin Pattern
```java
// Source: /mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractArrowMixin.java
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Shadow
    private double baseDamage;

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void thc$applyArrowHitEffects(EntityHitResult result, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        Entity owner = self.getOwner();

        if (!(owner instanceof ServerPlayer player)) {
            return; // Currently only handles player arrows
        }

        // ... player arrow handling
        baseDamage = modifiedValue; // Direct field modification
    }
}
```

### EntityType Owner Check Pattern
```java
// Source: PiglinCrossbowMixin pattern
if (shooter.getType() == EntityType.PILLAGER) {
    // Apply Pillager-specific logic
}
```

## Damage Calculation Reference

### Target Damage Values (from Requirements)

| Mob | Vanilla Damage (Hard) | Target Damage | Multiplier |
|-----|----------------------|---------------|------------|
| Pillager crossbow | 5-7 | 3-5 | ~0.667 (4/6 midpoint) |
| Stray bow | 4-8 | 2-4 | ~0.5 (3/6 midpoint) |

### Calculation Notes

Vanilla arrow damage is calculated as:
- Base damage (2.0 default) scaled by velocity
- Difficulty multiplier applied
- Power enchantment adds additional damage

For enemy arrows:
- Pillager crossbows: Higher base damage, consistent velocity
- Stray bows: Standard skeleton damage, variable charge

**Approach:** Apply multiplier to baseDamage before vanilla processing. This preserves all scaling factors while achieving target damage range.

### Multiplier Derivation

**Pillager (5-7 -> 3-5):**
- Midpoint vanilla: 6
- Midpoint target: 4
- Multiplier: 4/6 = 0.667

**Stray (4-8 -> 2-4):**
- Midpoint vanilla: 6
- Midpoint target: 3
- Multiplier: 3/6 = 0.5

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Custom arrow types | baseDamage modification | Mixin standard | No custom items needed |
| Separate mixin per mob | Centralized AbstractArrowMixin | THC pattern | Cleaner code organization |

**Deprecated/outdated:**
- None identified - patterns stable for MC 1.21.11

## Open Questions

Things that couldn't be fully resolved:

1. **Exact vanilla damage formula**
   - What we know: baseDamage scaled by velocity, difficulty affects final damage
   - What's unclear: Exact formula for crossbow vs bow damage calculation
   - Recommendation: Use multiplier approach, test in-game to verify target range

2. **Order of magnitude for multiplier precision**
   - What we know: 0.667 and 0.5 should achieve target ranges
   - What's unclear: Whether small precision differences matter
   - Recommendation: Use simple fractions (2/3 and 1/2), adjust if testing shows issues

## Sources

### Primary (HIGH confidence)
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractArrowMixin.java` - Existing arrow mixin pattern
- `/mnt/c/home/code/thc/src/main/resources/data/minecraft/recipe/ladder.json` - Recipe override pattern
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/PiglinCrossbowMixin.java` - EntityType check pattern
- [Minecraft Wiki - Skeleton](https://minecraft.wiki/w/Skeleton) - Damage values 4-8 (Hard)
- [Minecraft Wiki - Stray](https://minecraft.wiki/w/Stray) - Same damage as skeleton, adds Slowness

### Secondary (MEDIUM confidence)
- [Fabric Wiki - Mixin Examples](https://wiki.fabricmc.net/tutorial:mixin_examples) - @ModifyArg patterns
- [Forge JavaDocs AbstractSkeleton](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.19.3/net/minecraft/world/entity/monster/AbstractSkeleton.html) - Method signatures

### Tertiary (LOW confidence)
- WebSearch results for exact damage formulas - not fully verified against 1.21.11

## Metadata

**Confidence breakdown:**
- Recipe override: HIGH - Exact existing pattern (ladder.json)
- AbstractArrowMixin extension: HIGH - Existing pattern, simple extension
- Damage multipliers: MEDIUM - Based on wiki values, may need runtime tuning
- Pillager/Stray detection: HIGH - EntityType comparison is established pattern

**Research date:** 2026-01-29
**Valid until:** 2026-02-28 (30 days - stable Minecraft version)
