# v2.1 Research Summary

**Researched:** 2026-01-22
**Scope:** Targeted research for saturation-tiered healing and village structure protection refinement

## Executive Summary

v2.1 introduces two mechanically complex features requiring deep vanilla system understanding:

1. **Saturation-Tiered Healing** - Complete overhaul of Minecraft's food/regeneration system
2. **Village Structure Protection (Revised)** - Per-block protection replacing chunk-level blocking

Both features have clear implementation paths with HIGH confidence. The healing system is more complex (5 mixin injection points), while structure protection is nearly trivial (single method change).

---

## Key Findings

### Healing System (HEALING-MECHANICS.md)

**Vanilla mechanics:**
- `FoodData.tick(ServerPlayer)` handles all food/healing per tick
- Exhaustion threshold: 4.0 → removes 1.0 saturation (or 1 hunger if depleted)
- Two healing modes: "saturation boost" (10 tick interval) and "slow healing" (80 tick interval)
- Eating duration: 32 ticks (1.6s) via `Consumable.consumeSeconds`

**THC implementation approach:**
1. Disable vanilla natural regeneration (redirect heal() calls to no-op)
2. Modify exhaustion processing: 4.0 → 1.21 saturation instead of 1.0
3. Override saturation application: `max(food_sat, current_sat)` instead of additive
4. Implement custom per-tick healing based on saturation tiers
5. Double eating duration via `Item.getUseDuration()` mixin

**Critical insight:** Project already has `FoodDataMixin` targeting heal amount - this will be replaced with complete override.

### Village Structure Protection (STRUCTURE-BOUNDARIES.md)

**Key finding:** The API already does what we need.

```kotlin
val structureAt = structureManager.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
if (structureAt.isValid) { /* block is inside village structure */ }
```

This is the **same method** already used in `ChunkValidator.kt` line 134. The change is simply calling it on the break position instead of sampling chunk positions.

**No caching needed** - method is optimized for per-position queries, early-termination on hit.

---

## Confidence Assessment

| Feature | Research Confidence | Implementation Complexity |
|---------|--------------------|-----------------------------|
| Healing system | HIGH | MEDIUM-HIGH (5 mixins, complete override) |
| Structure protection | HIGH | LOW (single method change) |
| Blast totem/loot | N/A - familiar pattern | LOW |
| Furnace gating | N/A - familiar pattern | LOW |

---

## Implications for Roadmap

### Suggested Phase Structure

**Phase 24: Furnace Gating**
- Remove natural furnace/blast furnace spawns
- New furnace recipe (blaze powder + 8 cobblestone)
- Blast totem item and texture
- Blast furnace recipe (furnace + blast totem)
- Loot table replacement (totem → blast totem)

**Rationale:** Straightforward loot table + recipe work. Quick win to start milestone.

**Phase 25: Village Structure Protection**
- Replace chunk-level check with position-level `getStructureWithPieceAt()`
- Single file change in `VillageProtection.kt`
- Preserves ore/allowlist exceptions

**Rationale:** Minimal code change with high impact. Research confirms trivial implementation.

**Phase 26: Saturation-Tiered Healing**
- Split into 3-4 plans due to complexity:
  - Plan 1: Disable vanilla regen, implement custom healing tick
  - Plan 2: Modify exhaustion processing (1.21 saturation loss)
  - Plan 3: Override saturation application (max instead of add)
  - Plan 4: Double eating duration

**Rationale:** Healing system is interconnected - each change affects others. Separate plans allow verification at each step. Order matters: disable vanilla first, then modify mechanics.

### Phase Ordering Rationale

1. **Furnace first** - Independent, no dependencies, validates basic modding patterns still work
2. **Structure protection second** - Single method change, validates structure API understanding
3. **Healing last** - Most complex, benefits from warm-up on simpler phases

### Research Flags for Phases

- Phase 24 (Furnace): No research needed - standard recipe/loot patterns
- Phase 25 (Structure): No research needed - API verified, code patterns documented
- Phase 26 (Healing): Research complete - see HEALING-MECHANICS.md for mixin targets

---

## Open Questions Resolved

| Question | Resolution |
|----------|------------|
| How to disable vanilla regen? | Redirect `heal()` calls in FoodData.tick() to no-op |
| Where is saturation applied? | `FoodData.add()` - use @Redirect on Mth.clamp |
| How to check structure bounds? | `getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid` |
| Performance of per-block checks? | Acceptable - method designed for this use case |

## Open Questions Remaining

| Question | Recommendation |
|----------|----------------|
| Protect village paths? | Yes (default) - address in future if undesirable |
| Peaceful mode regeneration? | Preserve - only modify normal food-based healing |
| Regeneration potion? | Preserve - separate from food system |
| Exhaustion cost for custom healing? | Use vanilla ratio: 6.0 per HP healed |

---

## Research Files

| File | Contents |
|------|----------|
| HEALING-MECHANICS.md | FoodData class analysis, mixin targets, implementation code patterns |
| STRUCTURE-BOUNDARIES.md | Structure API, getStructureWithPieceAt usage, migration path |

---

*Research completed: 2026-01-22*
*Valid until: Stable through 1.21.x series*
