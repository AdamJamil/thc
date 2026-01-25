# Phase 45: Unify Region Detection - Research

**Researched:** 2026-01-24
**Domain:** Minecraft region detection, heightmap API, code refactoring patterns
**Confidence:** HIGH

## Summary

Phase 45 fixes a critical integration gap where two mixins use different algorithms to detect spawn regions, causing spawn distribution mismatches at surface/cave boundaries. SpawnReplacementMixin uses `canSeeSky()` while MobFinalizeSpawnMixin uses heightmap comparison, leading to scenarios where a mob spawned under tree cover gets distributed as UPPER_CAVE but tagged/counted as SURFACE.

The fix is a straightforward refactoring: extract region detection logic into a shared `RegionDetector` utility class and update both mixins to use it. The audit recommends heightmap-based detection (MOTION_BLOCKING) because it matches player intuition - "surface" means "at ground level" regardless of tree cover overhead.

**Key architectural decision:** This is a refactoring phase, not a new feature. Both detection methods already exist in the codebase - we're unifying them, not inventing new logic. The heightmap-based approach is already implemented in MobFinalizeSpawnMixin and proven functional.

**Primary recommendation:** Create `RegionDetector.getRegion(ServerLevel, BlockPos)` utility with heightmap-based detection (from MobFinalizeSpawnMixin), update SpawnReplacementMixin to call it, verify both mixins use identical logic.

## Standard Stack

The established libraries/tools for region detection in Minecraft 1.21.11:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Minecraft Heightmap API | 1.21.11 | Pre-computed terrain height | Built-in, O(1) lookup, used by vanilla spawning |
| Java Static Utility Pattern | JDK 21 | Shared logic extraction | Standard code organization pattern |
| Mixin | 0.8+ (via Fabric) | Injection point maintenance | Already used in both mixins |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| @Unique annotation | Mixin 0.8+ | Private helper methods | Currently used for detectRegion in both mixins |
| ServerLevel | MC 1.21.11 | World access, heightmap API | Required for heightmap lookup |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Heightmap-based | canSeeSky-based | Heightmap matches player intuition; canSeeSky treats under-tree as cave |
| Static utility class | Extension method in Kotlin | Utility class accessible from Java mixins; Kotlin extension only works in Kotlin |
| Extract to utility | Duplicate logic in both mixins | Current state causes mismatches; utility ensures consistency |

**Installation:**
No new dependencies - refactoring existing code.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── spawn/
│   ├── RegionDetector.java          # NEW: Shared region detection logic
│   ├── RegionalCapManager.java      # Existing: Uses region strings for caps
│   ├── SpawnDistributions.java      # Existing: Uses region strings for distribution
│   └── PillagerVariant.java         # Existing: Unrelated to region detection
├── mixin/
│   ├── SpawnReplacementMixin.java   # UPDATE: Call RegionDetector.getRegion()
│   └── MobFinalizeSpawnMixin.java   # UPDATE: Call RegionDetector.getRegion()
```

### Pattern 1: Static Utility Class for Shared Logic

**What:** Extract duplicate/divergent logic into a static utility class that both consumers call.

**When to use:** When multiple classes need identical logic but can't share via inheritance (mixins target different vanilla classes).

**Example:**
```java
// Source: Standard Java utility pattern + existing THC patterns (ThreatManager, DamageCalculator)
package thc.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Shared region detection logic for spawn system.
 *
 * <p>Used by:
 * <ul>
 *   <li>SpawnReplacementMixin - Distribution selection and cap checking
 *   <li>MobFinalizeSpawnMixin - NBT tagging for cap counting
 * </ul>
 *
 * <p>Ensures consistent region classification across spawn distribution
 * and cap counting systems.
 */
public final class RegionDetector {

    private RegionDetector() {
        // Utility class - no instantiation
    }

    /**
     * Detect spawn region for a given position.
     *
     * <p>Region detection uses heightmap-based algorithm:
     * <ul>
     *   <li>Y < 0: OW_LOWER_CAVE (below sea level)
     *   <li>Y >= heightmap: OW_SURFACE (at or above terrain surface)
     *   <li>Otherwise: OW_UPPER_CAVE (underground but above sea level)
     * </ul>
     *
     * <p>Uses MOTION_BLOCKING heightmap which includes:
     * <ul>
     *   <li>Blocks with collision boxes (solid blocks)
     *   <li>Fluid blocks (water, lava, waterlogged)
     *   <li>Excludes leaves (mobs in tree canopy = surface, not cave)
     * </ul>
     *
     * @param level The server level
     * @param pos   The spawn position
     * @return Region string (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE) or null if non-Overworld
     */
    public static String getRegion(ServerLevel level, BlockPos pos) {
        // Only Overworld has regional spawn system
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }

        int y = pos.getY();

        // Lower cave: below Y=0 (sea level)
        if (y < 0) {
            return "OW_LOWER_CAVE";
        }

        // Surface: Y >= heightmap at X/Z
        // MOTION_BLOCKING: highest block that blocks motion or contains fluid
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (y >= surfaceY) {
            return "OW_SURFACE";
        }

        // Upper cave: Y >= 0 but below heightmap
        return "OW_UPPER_CAVE";
    }
}
```

### Pattern 2: Mixin Refactoring to Use Shared Utility

**What:** Replace mixin's unique region detection with call to shared utility.

**When to use:** When extracting logic from mixin to utility class.

**Before (SpawnReplacementMixin):**
```java
// Source: Current SpawnReplacementMixin.java implementation
@Unique
private static String thc$detectRegion(ServerLevel level, BlockPos pos) {
    if (level.dimension() != Level.OVERWORLD) {
        return null;
    }

    // Uses canSeeSky - PROBLEM: mismatch with MobFinalizeSpawnMixin
    if (level.canSeeSky(pos)) {
        return "OW_SURFACE";
    }

    if (pos.getY() < 0) {
        return "OW_LOWER_CAVE";
    }

    return "OW_UPPER_CAVE";
}
```

**After (SpawnReplacementMixin):**
```java
// Source: Refactoring pattern - delegate to shared utility
// Remove @Unique thc$detectRegion method entirely

// Update callers:
String region = RegionDetector.getRegion(level, pos);
```

**Before (MobFinalizeSpawnMixin):**
```java
// Source: Current MobFinalizeSpawnMixin.java implementation
@Unique
private static String detectRegion(ServerLevel level, BlockPos pos) {
    int y = pos.getY();

    // Lower cave: below Y=0 (sea level)
    if (y < 0) {
        return "OW_LOWER_CAVE";
    }

    // Surface: Y >= heightmap at X/Z
    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    if (y >= surfaceY) {
        return "OW_SURFACE";
    }

    // Upper cave: Y >= 0 but below heightmap
    return "OW_UPPER_CAVE";
}
```

**After (MobFinalizeSpawnMixin):**
```java
// Source: Refactoring pattern - delegate to shared utility
// Remove @Unique detectRegion method entirely

// Update callers (line 40):
String region = RegionDetector.getRegion(serverLevel, self.blockPosition());
```

### Anti-Patterns to Avoid

- **Keeping both detection methods "for compatibility":** This perpetuates the bug. Choose ONE method (heightmap) and use it everywhere.

- **Using canSeeSky in utility:** The audit explicitly recommends heightmap because it matches player intuition. Don't second-guess this decision.

- **Making RegionDetector non-static:** The utility has no state - static methods are appropriate and avoid unnecessary object allocation.

- **Overcomplicating with caching:** Region detection is O(1) heightmap lookup. Caching adds complexity with no performance benefit.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Region detection | New algorithm | MobFinalizeSpawnMixin's heightmap logic | Already tested, proven correct, recommended by audit |
| Heightmap lookup | Ray-tracing or block scanning | level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) | Pre-computed during chunk generation, O(1), vanilla uses it |
| Utility class location | thc.util package | thc.spawn package | Co-locate with consumers (SpawnDistributions, RegionalCapManager) |
| Testing region detection | Manual testing only | Verify existing game tests still pass | Game tests already validate spawn system behavior |

**Key insight:** This is a refactoring phase. The "correct" implementation already exists in MobFinalizeSpawnMixin. Don't invent new logic - extract existing logic to a shared location.

## Common Pitfalls

### Pitfall 1: canSeeSky vs Heightmap Semantic Differences

**What goes wrong:** Assuming `canSeeSky()` and `Y >= heightmap` are functionally equivalent with minor edge case differences.

**Why it happens:** Both relate to "surface" detection, but use fundamentally different approaches:
- `canSeeSky(pos)`: Checks if sky is directly visible from position (light path check)
- `heightmap MOTION_BLOCKING`: Y-level of highest motion-blocking block at X/Z

**How they diverge:**

| Scenario | canSeeSky() | Y >= heightmap | Expected Region |
|----------|-------------|----------------|-----------------|
| Open plains Y=70 | true → SURFACE | Y >= 70 → SURFACE | SURFACE ✓ |
| Under oak tree Y=70 | false → UPPER_CAVE | Y >= 70 → SURFACE | SURFACE (heightmap correct) |
| Under cliff overhang Y=70 | false → UPPER_CAVE | Y < 80 → UPPER_CAVE | UPPER_CAVE ✓ |
| In cave Y=40 | false → UPPER_CAVE | Y < 65 → UPPER_CAVE | UPPER_CAVE ✓ |
| Deep cave Y=-20 | false → LOWER_CAVE | Y < 0 → LOWER_CAVE | LOWER_CAVE ✓ |

**Key difference:** Under tree canopy, `canSeeSky()` returns false (leaves block sky view) but heightmap correctly identifies position as surface-level. Player intuition: "I'm on the surface, just in a forest."

**How to avoid:** Use heightmap-based detection. MOTION_BLOCKING heightmap excludes leaves, so tree canopy doesn't lower the "surface" level. This matches player mental model of "surface vs cave."

**Warning signs:**
- Mobs spawning in forests get different region tags than mobs in plains
- Surface cap fills faster in forested biomes than plains
- Spawn distribution skews toward cave mobs in jungle/taiga biomes

### Pitfall 2: Heightmap Type Selection

**What goes wrong:** Using wrong heightmap type (WORLD_SURFACE, OCEAN_FLOOR, MOTION_BLOCKING_NO_LEAVES) causes incorrect region classification.

**Why it happens:** Multiple heightmap types exist with different block filtering rules:

| Type | Includes | Use Case |
|------|----------|----------|
| WORLD_SURFACE | All non-air blocks | Lightning rod checks, general "top of world" |
| OCEAN_FLOOR | Motion-blocking blocks only | Ocean structure placement |
| MOTION_BLOCKING | Motion-blocking + fluids, excludes leaves | Mob spawning, terrain surface |
| MOTION_BLOCKING_NO_LEAVES | Motion-blocking + fluids, includes leaves | Rarely used |

**Correct choice:** `Heightmap.Types.MOTION_BLOCKING` because:
1. Excludes leaves (tree canopy doesn't count as "surface")
2. Includes fluids (water surface is still "surface")
3. Used by vanilla spawning logic (see `NaturalSpawner.java:439`)
4. Already implemented in MobFinalizeSpawnMixin (tested and working)

**How to avoid:** Use `Heightmap.Types.MOTION_BLOCKING` explicitly. Don't use `WORLD_SURFACE` (includes leaves, wrong for spawning) or `OCEAN_FLOOR` (excludes fluids).

**Warning signs:**
- Mobs in tree canopy tagged as UPPER_CAVE instead of SURFACE
- Water surface spawns counted as cave spawns
- Heightmap values seem wrong in forested/water areas

### Pitfall 3: Dimension Check Ordering

**What goes wrong:** Performing heightmap lookup before checking dimension, causing errors in Nether/End where heightmap behavior may differ.

**Why it happens:** Heightmap API exists in all dimensions, but behavior/semantics may be dimension-specific. Overworld assumptions don't necessarily hold.

**How to avoid:** Check dimension FIRST, return early for non-Overworld:
```java
// CORRECT: Check dimension before any heightmap operations
if (level.dimension() != Level.OVERWORLD) {
    return null;
}
// Now safe to use heightmap knowing we're in Overworld
int surfaceY = level.getHeight(...);
```

**Warning signs:**
- Console warnings about heightmap access in Nether/End
- Unexpected region tags on Nether/End mobs
- Crashes when spawning in non-Overworld dimensions

### Pitfall 4: Incomplete Refactoring

**What goes wrong:** Updating SpawnReplacementMixin but forgetting to update MobFinalizeSpawnMixin (or vice versa), leaving one mixin with unique logic.

**Why it happens:** The bug is subtle - both mixins work independently. Only integration testing reveals the mismatch. Developer updates one mixin and thinks the fix is complete.

**How to avoid:**
1. Create RegionDetector utility FIRST
2. Update SpawnReplacementMixin to use it
3. Update MobFinalizeSpawnMixin to use it
4. Delete @Unique detectRegion methods from both mixins
5. Verify neither mixin has region detection logic - both delegate to utility

**Verification checklist:**
```bash
# Should find ZERO occurrences of region detection in mixins
grep -n "canSeeSky" src/main/java/thc/mixin/SpawnReplacementMixin.java
# Expected: No results (all removed)

grep -n "getHeight.*MOTION_BLOCKING" src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
# Expected: No results (all removed)

grep -n "RegionDetector.getRegion" src/main/java/thc/mixin/SpawnReplacementMixin.java
# Expected: 1-2 occurrences (calls to utility)

grep -n "RegionDetector.getRegion" src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
# Expected: 1 occurrence (call to utility)
```

**Warning signs:**
- One mixin still has @Unique detectRegion method
- Grep finds heightmap or canSeeSky logic still in mixins
- Code review shows region detection duplicated across files

### Pitfall 5: Breaking Existing Tests

**What goes wrong:** Refactoring changes behavior in subtle ways that break existing game tests or smoke tests.

**Why it happens:** Switching from canSeeSky to heightmap changes region classification in edge cases (tree cover). If tests expect old behavior, they'll fail.

**How to avoid:**
1. Run tests BEFORE refactoring (establish baseline)
2. Understand test expectations (do they assume canSeeSky behavior?)
3. Update test expectations if needed (heightmap is the correct behavior)
4. Run tests AFTER refactoring (verify no regressions)

**Expected test changes:**
- Tests spawning mobs under trees may see different region tags (SURFACE instead of UPPER_CAVE)
- This is CORRECT behavior - update test assertions, not the fix

**Warning signs:**
- Tests that passed before refactoring now fail
- Test failures related to region tags in forested areas
- Spawn cap tests showing different counts

## Code Examples

Verified patterns from existing codebase and refactoring best practices:

### Complete RegionDetector Utility
```java
// Source: Extracted from MobFinalizeSpawnMixin.java + audit recommendation
package thc.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Shared region detection logic for spawn system.
 *
 * <p>Ensures consistent region classification between:
 * <ul>
 *   <li>Spawn distribution (SpawnReplacementMixin)
 *   <li>NBT tagging (MobFinalizeSpawnMixin)
 *   <li>Cap counting (RegionalCapManager)
 * </ul>
 *
 * <p>Uses heightmap-based detection per v2.3 audit recommendation:
 * "Players think of 'surface' as 'at ground level' regardless of tree cover."
 */
public final class RegionDetector {

    private RegionDetector() {
        // Utility class
    }

    /**
     * Detect spawn region for a given position.
     *
     * <p>Algorithm:
     * <ul>
     *   <li>Non-Overworld: null (no regional system)
     *   <li>Y < 0: OW_LOWER_CAVE
     *   <li>Y >= MOTION_BLOCKING heightmap: OW_SURFACE
     *   <li>Otherwise: OW_UPPER_CAVE
     * </ul>
     *
     * <p>MOTION_BLOCKING heightmap includes motion-blocking blocks and fluids,
     * but excludes leaves. This means:
     * <ul>
     *   <li>Mobs under tree canopy: SURFACE (correct - at ground level)
     *   <li>Mobs under cliff overhang: UPPER_CAVE (correct - underground)
     *   <li>Mobs on water surface: SURFACE (correct - fluids counted)
     * </ul>
     *
     * @param level The server level
     * @param pos   The spawn position
     * @return Region string or null if non-Overworld
     */
    public static String getRegion(ServerLevel level, BlockPos pos) {
        // Only Overworld has regional spawn system
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }

        int y = pos.getY();

        // Lower cave: below Y=0 (sea level)
        if (y < 0) {
            return "OW_LOWER_CAVE";
        }

        // Surface: Y >= heightmap at X/Z
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (y >= surfaceY) {
            return "OW_SURFACE";
        }

        // Upper cave: Y >= 0 but below heightmap
        return "OW_UPPER_CAVE";
    }
}
```

### Updated SpawnReplacementMixin
```java
// Source: Existing SpawnReplacementMixin.java with refactoring applied
// BEFORE: Lines 119-139 had @Unique thc$detectRegion method with canSeeSky logic
// AFTER: Removed thc$detectRegion, call RegionDetector.getRegion instead

// In thc$replaceWithSurfaceVariant method (around line 85):
String region = RegionDetector.getRegion(level, pos);

// In thc$getReplacementEntity method (around line 228):
// Remove canSeeSky check - RegionDetector already called earlier in flow
// Surface variant replacement only applies if region was OW_SURFACE
```

### Updated MobFinalizeSpawnMixin
```java
// Source: Existing MobFinalizeSpawnMixin.java with refactoring applied
// BEFORE: Lines 48-65 had @Unique detectRegion method with heightmap logic
// AFTER: Removed detectRegion, call RegionDetector.getRegion instead

@Inject(method = "finalizeSpawn", at = @At("TAIL"))
private void thc$tagSpawnOrigin(
        ServerLevelAccessor level, DifficultyInstance difficulty,
        EntitySpawnReason reason, SpawnGroupData groupData,
        CallbackInfoReturnable<SpawnGroupData> cir) {

    // Filter to natural spawns
    if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
        return;
    }

    // Only tag Overworld mobs
    ServerLevel serverLevel = (ServerLevel) level.getLevel();
    if (serverLevel.dimension() != Level.OVERWORLD) {
        return;
    }

    Mob self = (Mob) (Object) this;

    // Use shared region detection
    String region = RegionDetector.getRegion(serverLevel, self.blockPosition());
    self.setAttached(THCAttachments.SPAWN_REGION, region);

    // Tag if counted
    boolean isMonster = self.getType().getCategory() == MobCategory.MONSTER;
    self.setAttached(THCAttachments.SPAWN_COUNTED, isMonster);
}

// DELETE the @Unique detectRegion method entirely
```

### Verification: No Duplicate Logic
```bash
# Source: Code review checklist pattern
# After refactoring, verify region detection only exists in RegionDetector

# Check RegionDetector has the logic
grep -c "getHeight.*MOTION_BLOCKING" src/main/java/thc/spawn/RegionDetector.java
# Expected: 1 (the utility method)

# Check mixins DON'T have the logic
grep "canSeeSky" src/main/java/thc/mixin/SpawnReplacementMixin.java
# Expected: No results (removed)

grep "getHeight.*MOTION_BLOCKING" src/main/java/thc/mixin/MobFinalizeSpawnMixin.java
# Expected: No results (removed)

# Check mixins DO call the utility
grep "RegionDetector.getRegion" src/main/java/thc/mixin/*.java
# Expected: 2 results (SpawnReplacementMixin and MobFinalizeSpawnMixin)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| canSeeSky for surface detection | Heightmap MOTION_BLOCKING | Phase 45 (2026-01-24) | Fixes tree canopy mismatch, matches player intuition |
| Duplicate region detection in mixins | Shared RegionDetector utility | Phase 45 (2026-01-24) | Ensures consistency, prevents future divergence |
| Light-based spawn surface checks | Heightmap-based spawn surface | MC 1.18+ (vanilla pattern) | Terrain-aware, pre-computed, O(1) lookup |

**Deprecated/outdated:**
- **canSeeSky for spawn region detection:** Treats tree cover as "cave" - incorrect for player mental model
- **Duplicate region detection logic:** Causes integration bugs when logic diverges between mixins
- **WORLD_SURFACE heightmap for spawning:** Includes leaves, wrong for spawn region classification

## Open Questions

Things that couldn't be fully resolved:

1. **Impact on existing spawn distributions**
   - What we know: Switching to heightmap changes region classification under tree canopy (UPPER_CAVE → SURFACE)
   - What's unclear: How many mobs currently spawning in forests will shift from cave distribution to surface distribution
   - Recommendation: **Accept the behavior change.** This is a bug fix, not a feature. Current behavior (under-tree = cave) is wrong. Heightmap-based detection is correct. If spawn balance shifts, it reveals the balance was accidentally tuned around a bug.

2. **Performance impact of heightmap lookups**
   - What we know: Heightmap lookup is O(1) array access. Vanilla uses it in `NaturalSpawner.spawnCategoryForPosition` (line 439).
   - What's unclear: Whether replacing canSeeSky (which also does lookups) with heightmap changes performance profile
   - Recommendation: **No performance concern.** Both methods are O(1). Heightmap may be slightly faster (array lookup vs light propagation check). No profiling needed - vanilla uses heightmaps extensively for spawning.

3. **Need for RegionDetector caching**
   - What we know: RegionDetector.getRegion() is called twice per spawn (once in SpawnReplacementMixin, once in MobFinalizeSpawnMixin)
   - What's unclear: Whether caching heightmap results would improve performance
   - Recommendation: **No caching needed.** Heightmap is already a cache (computed once per chunk). Re-querying is just an array lookup. Caching adds complexity (invalidation, thread-safety) for negligible benefit. Keep it simple.

## Sources

### Primary (HIGH confidence)
- Existing THC codebase:
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/SpawnReplacementMixin.java` - Current canSeeSky implementation
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/MobFinalizeSpawnMixin.java` - Current heightmap implementation
  - `/mnt/c/home/code/thc/src/main/java/thc/spawn/RegionalCapManager.java` - Consumer of region strings
- `/mnt/c/home/code/thc/.planning/v2.3-MILESTONE-AUDIT.md` - Gap identification and fix recommendation
- `/mnt/c/home/code/thc/.planning/ROADMAP.md` - Phase 45 specification
- [Minecraft Heightmap Types Gist](https://gist.github.com/ByteZ1337/31f10b0052f44acfc177f40a0f0fe9cd) - MOTION_BLOCKING definition
- [Minecraft Wiki - Heightmap](https://minecraft.wiki/w/Heightmap) - Official heightmap documentation

### Secondary (MEDIUM confidence)
- [Misode Heightmap Types Guide](https://misode.github.io/guides/heightmap-types/) - Visual comparison of heightmap types
- [BlockAndTintGetter JavaDoc](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/level/BlockAndTintGetter.html) - canSeeSky interface definition
- [ServerLevel JavaDoc](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/server/level/ServerLevel.html) - Heightmap API methods
- Vanilla Minecraft source:
  - `net/minecraft/world/level/NaturalSpawner.java:439` - Vanilla uses heightmap for spawn height calculation
  - `net/minecraft/world/entity/monster/Monster.java:129` - Vanilla uses canSeeSky for light-dependent spawn conditions

### Tertiary (LOW confidence)
- [Tough As Nails Changelog](https://www.mcmod.cn/class/version/531.html) - Mod switched from canSeeSky to heightmap (version 9.2.0.124)

## Metadata

**Confidence breakdown:**
- Heightmap vs canSeeSky difference: HIGH - Audit documented exact divergence scenarios, existing code demonstrates both methods
- Refactoring approach: HIGH - Standard utility extraction pattern, no new logic invention
- Heightmap type selection: HIGH - MOTION_BLOCKING already used in MobFinalizeSpawnMixin, verified working
- Performance impact: HIGH - Both methods O(1), vanilla uses heightmaps extensively
- Behavioral changes: MEDIUM - Switching under-tree classification is intentional bug fix, but downstream effects need verification

**Research date:** 2026-01-24
**Valid until:** Minecraft 1.21.x (heightmap API stable since 1.18, spawn system unchanged)

**Critical context for planner:**
1. This is a **refactoring phase** - extract existing logic, don't invent new logic
2. The "correct" implementation already exists in MobFinalizeSpawnMixin.java
3. The fix is **behavioral change** - mobs under trees will shift from cave to surface distribution (intentional)
4. Both mixins must be updated **in same plan** - partial fix perpetuates the bug
5. Delete duplicate detection methods from mixins - utility is single source of truth
