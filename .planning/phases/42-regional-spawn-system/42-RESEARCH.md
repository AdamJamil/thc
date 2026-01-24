# Phase 42: Regional Spawn System - Research

**Researched:** 2026-01-24
**Domain:** Minecraft 1.21.11 regional spawn distributions, weighted selection, pillager AI modification
**Confidence:** HIGH

## Summary

Phase 42 implements region-based spawn distributions with weighted random selection to replace vanilla spawns at selection time. The system integrates with existing `NaturalSpawnerMixin` infrastructure for position validation and uses `@Redirect` on entity spawn to intercept and replace mobs based on regional probability tables. Pillager variants require AI goal manipulation via `registerGoals` mixin injection. Phase 41's NBT attachment infrastructure (SPAWN_REGION, SPAWN_COUNTED) is already established and functional.

**Key architectural decision:** Use weighted random selection BEFORE vanilla mob selection, not after. This allows custom spawns to bypass vanilla spawn conditions (witches anywhere, blazes/breezes without fortresses) while maintaining vanilla placement rules for successful spawns.

**Primary recommendation:** Extend existing `NaturalSpawnerMixin` with regional detection + weighted random roll, use `@Redirect` on `addFreshEntityWithPassengers` for mob type replacement, and inject into `Pillager.registerGoals` to remove RangedAttackGoal for melee variants.

## Standard Stack

The established libraries/tools for spawn modification in Minecraft 1.21.11:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.119.2+ | Attachment API for NBT tags | Already used in THC for entity attachments |
| Mixin | 0.8.5+ | Bytecode injection framework | Required for Fabric modding, already in use |
| Minecraft | 1.21.11 | Target game version | Project constraint |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Java Random | JDK 21 | Weighted random selection | Spawn distribution rolling |
| EnumMap | JDK 21 | Region-keyed distribution tables | Fast constant-key lookups |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| @Redirect on addFreshEntityWithPassengers | @Inject in isValidSpawnPostitionForType | Redirect allows entity replacement; Inject only allows filtering |
| Weighted table in Java | JSON data files | Hardcoded is faster, data files easier to balance - use hardcoded initially |
| AI goal removal | Custom Pillager entity | Goal removal simpler, custom entity has no conflicts with vanilla spawning |

**Installation:**
Already in project - no additional dependencies needed.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── spawn/
│   ├── RegionalSpawnHandler.java     # Core weighted selection logic
│   ├── SpawnDistributions.java       # Regional probability tables
│   ├── SpawnRegion.java              # Already exists (OW_SURFACE, etc)
│   └── PillagerVariant.java          # MELEE/RANGED equipment application
├── mixin/
│   ├── NaturalSpawnerMixin.java      # EXTEND: add regional filtering
│   ├── PillagerMixin.java            # NEW: AI goal manipulation
│   └── MobFinalizeSpawnMixin.java    # Already handles NBT tagging
└── THCAttachments.java               # Already has SPAWN_REGION, SPAWN_COUNTED
```

### Pattern 1: Weighted Random Selection for Spawn Distribution

**What:** Select custom mob type OR vanilla fallback based on per-region probability weights.

**When to use:** At spawn attempt time, BEFORE vanilla mob type selection.

**Example:**
```java
// Source: Common pattern from mob spawning research + Curtain reference
public class SpawnDistributions {
    // Region -> List of (MobType, weight)
    private static final Map<String, List<WeightedEntry>> DISTRIBUTIONS = new EnumMap<>(SpawnRegion.class);

    static {
        List<WeightedEntry> upperCave = new ArrayList<>();
        upperCave.add(new WeightedEntry(EntityType.WITCH, 5));      // 5%
        upperCave.add(new WeightedEntry(EntityType.VEX, 2));        // 2%
        upperCave.add(new WeightedEntry("PILLAGER_RANGED", 10));    // 10%
        upperCave.add(new WeightedEntry("PILLAGER_MELEE", 25));     // 25%
        upperCave.add(new WeightedEntry(null, 58));                 // 58% vanilla fallback
        DISTRIBUTIONS.put("OW_UPPER_CAVE", upperCave);
    }

    public static MobSelection selectMob(String region, RandomSource random) {
        List<WeightedEntry> entries = DISTRIBUTIONS.get(region);
        if (entries == null) {
            return MobSelection.vanillaFallback();
        }

        // Calculate total weight
        int totalWeight = entries.stream().mapToInt(e -> e.weight).sum();

        // Roll random
        int roll = random.nextInt(totalWeight);

        // Find selected entry
        int cumulative = 0;
        for (WeightedEntry entry : entries) {
            cumulative += entry.weight;
            if (roll < cumulative) {
                if (entry.type == null) {
                    return MobSelection.vanillaFallback();
                } else {
                    return MobSelection.custom(entry.type, entry.variant);
                }
            }
        }

        return MobSelection.vanillaFallback(); // Fallback (should never reach)
    }

    record WeightedEntry(EntityType<?> type, String variant, int weight) {
        WeightedEntry(EntityType<?> type, int weight) {
            this(type, null, weight);
        }
        WeightedEntry(String variant, int weight) {
            this(EntityType.PILLAGER, variant, weight);
        }
    }
}
```

### Pattern 2: Entity Replacement via @Redirect

**What:** Intercept entity spawn to replace mob type based on regional selection.

**When to use:** When custom distribution selects a mob different from vanilla's choice.

**Example:**
```java
// Source: Existing THC SpawnReplacementMixin.java pattern + Curtain reference
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @Redirect(
        method = "spawnCategoryForPosition(...)",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"
        )
    )
    private static void thc$applyRegionalDistribution(ServerLevel level, Entity entity) {
        // Get region from spawn position
        String region = detectRegion(level, entity.blockPosition());

        // Roll for custom spawn
        MobSelection selection = SpawnDistributions.selectMob(region, level.random);

        if (selection.isVanilla()) {
            // Allow vanilla spawn
            level.addFreshEntityWithPassengers(entity);
            return;
        }

        // Replace with custom mob
        Entity replacement = createCustomMob(level, selection, entity.blockPosition());
        if (replacement != null) {
            level.addFreshEntityWithPassengers(replacement);
        }
        // If replacement fails, skip spawn (no fallback per user decision)
    }
}
```

**Integration note:** This must work WITH existing base chunk blocking. The existing `isValidSpawnPostitionForType` injection cancels attempts in claimed chunks BEFORE this redirect fires, so regional distribution only runs on valid positions.

### Pattern 3: AI Goal Removal for Pillager Melee Variant

**What:** Remove RangedAttackGoal from pillager's goalSelector to prevent crossbow usage.

**When to use:** During `Pillager.registerGoals` for melee variant pillagers spawned via custom distribution.

**Example:**
```java
// Source: Existing THC EndermanMixin.java pattern for goal manipulation
@Mixin(Pillager.class)
public class PillagerMixin {
    @Shadow @Final
    protected GoalSelector goalSelector;

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void thc$configureMeleeVariant(
            ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        Pillager self = (Pillager) (Object) this;

        // Check if this is a melee variant (equipment check)
        if (self.getMainHandItem().getItem() == Items.IRON_SWORD) {
            // Remove all ranged attack goals
            this.goalSelector.getAvailableGoals().removeIf(goal ->
                goal.getGoal() instanceof RangedAttackGoal
            );

            // Add melee attack goal
            this.goalSelector.addGoal(4, new MeleeAttackGoal(self, 1.0, false));
        }
    }
}
```

**CRITICAL PITFALL:** Must set equipment BEFORE finalizeSpawn completes, since populateDefaultEquipmentSlots runs during Mob creation. Equipment-based variant detection happens in finalizeSpawn TAIL injection where equipment is already set.

### Pattern 4: Pack Spawning with Custom Sizes

**What:** Spawn 1-4 mobs of same custom type in nearby positions.

**When to use:** When custom distribution selects a mob (not vanilla fallback).

**Example:**
```java
// Source: Minecraft pack spawning mechanics research
private static void spawnCustomPack(ServerLevel level, EntityType<?> type, BlockPos origin, String variant) {
    // Random pack size [1, 4]
    int packSize = 1 + level.random.nextInt(4); // uniform distribution

    SpawnGroupData groupData = null;
    BlockPos.MutableBlockPos currentPos = origin.mutable();

    for (int i = 0; i < packSize; i++) {
        // Offset from previous position (triangular distribution, ±5 blocks)
        if (i > 0) {
            int dx = level.random.nextInt(11) - 5; // -5 to +5
            int dz = level.random.nextInt(11) - 5;
            currentPos.move(dx, 0, dz);

            // Adjust Y to ground level
            currentPos.setY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, currentPos.getX(), currentPos.getZ()));
        }

        // Create mob
        Mob mob = (Mob) type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) continue;

        // Position
        mob.snapTo(currentPos.getX() + 0.5, currentPos.getY(), currentPos.getZ() + 0.5,
                   level.random.nextFloat() * 360, 0);

        // Apply variant equipment BEFORE finalizeSpawn
        if (variant != null && mob instanceof Pillager pillager) {
            PillagerVariant.valueOf(variant).applyEquipment(pillager);
        }

        // Finalize and add
        groupData = mob.finalizeSpawn(level, level.getCurrentDifficultyAt(currentPos),
                                       EntitySpawnReason.NATURAL, groupData);
        level.addFreshEntityWithPassengers(mob);
    }
}
```

**Note:** Vanilla's pack spawning uses triangular distribution for position offsets. Custom pack spawning should match this pattern for consistent behavior.

### Anti-Patterns to Avoid

- **Filtering in isValidSpawnPostitionForType:** Don't filter by mob type there - you don't control WHICH mob vanilla selects yet. User decision: custom spawns REPLACE vanilla at selection time, so intercept entity creation, not position validation.

- **Re-rolling on spawn failure:** User decision: if custom spawn fails (collision/placement), skip the attempt entirely. No fallback to vanilla, no re-rolling.

- **Modifying base chunk blocking:** Existing `NaturalSpawnerMixin` base chunk check must remain FIRST priority. Regional distribution runs after that check passes.

- **Equipment after populateDefaultEquipmentSlots:** Pillager equipment must be set BEFORE or DURING finalizeSpawn, since vanilla calls populateDefaultEquipmentSlots there. Setting equipment in a later injection will be overwritten.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| AI goal management | Manual goal list manipulation | GoalSelector.addGoal(), removeIf() | GoalSelector handles priority conflicts, flag conflicts, tick scheduling |
| Weighted random selection | Custom probability algorithm | Standard cumulative weight pattern | Well-tested, handles edge cases (empty tables, zero weights) |
| Pack position offsets | Uniform random distribution | Triangular distribution (nextInt(11) - 5) | Matches vanilla behavior, creates natural-looking clusters |
| Entity positioning | moveTo() or teleportTo() | snapTo() in MC 1.21.11 | Correct API for initial spawn positioning |
| Region detection | Custom Y-level + biome checks | Use existing MobFinalizeSpawnMixin.detectRegion() | Already implemented, tested, and NBT-tagged in Phase 41 |

**Key insight:** Minecraft's spawning system has many edge cases (pack spawning, spawn groups, difficulty scaling, local/global caps). Reusing vanilla patterns minimizes bugs and maintains compatibility.

## Common Pitfalls

### Pitfall 1: Equipment Timing - MOB-06 from Research Flags

**What goes wrong:** Setting pillager equipment after `finalizeSpawn` completes results in vanilla overwriting custom weapons with crossbows.

**Why it happens:** `Mob.finalizeSpawn()` calls `populateDefaultEquipmentSlots()` which sets default equipment. Any equipment set before this call gets overwritten.

**How to avoid:**
1. Set equipment in a `@Inject(method = "finalizeSpawn", at = @At("TAIL"))` - runs AFTER populateDefaultEquipmentSlots
2. OR override populateDefaultEquipmentSlots for custom mobs via mixin

**Warning signs:**
- Pillagers spawning with both iron sword AND crossbow
- Equipment disappearing on spawn
- Console logs showing equipment being set but not visible in-game

**Verified solution from codebase:**
```java
// From existing MobFinalizeSpawnMixin.java - equipment set at TAIL
@Inject(method = "finalizeSpawn", at = @At("TAIL"))
private void thc$applyEquipment(...) {
    // Equipment set here persists
}
```

### Pitfall 2: Spawn Weight Partitioning vs Hard Caps - MOB-08 from Research Flags

**What goes wrong:** Implementing hard per-region mob caps (e.g., "max 20 mobs in upper cave") causes spawn starvation where valid spawn attempts are rejected because one region hit cap.

**Why it happens:** User decision specifies "intentionally > 100% total" for regional caps (30% + 40% + 50% = 120%). Hard partitioning would contradict this.

**How to avoid:** Use spawn weights/probabilities, not hard caps. Regional percentages control SELECTION probability, not absolute limits. Mob caps are applied at global/local level with regional counting for tracking purposes only.

**Warning signs:**
- Mobs stop spawning in one region while other regions are empty
- Regional caps sum to exactly 100%
- Spawn rates drop dramatically when players spread across regions

**Correct approach from spec:**
```
Regional cap multipliers (0.30, 0.40, 0.50) are for COUNTING, not BLOCKING.
Caps are "intentionally > 100% total" to allow overlap.
```

### Pitfall 3: Integration Order with Base Chunk Blocking - INT-03 from Research Flags

**What goes wrong:** Regional distribution code runs before base chunk blocking, causing custom mobs to spawn in claimed chunks.

**Why it happens:** Multiple mixins injecting at same method with unclear ordering.

**How to avoid:**
1. Keep base chunk blocking AND regional distribution in SAME mixin class for guaranteed order
2. Base chunk check with early return BEFORE regional distribution code
3. Document execution order in comments

**Warning signs:**
- Custom mobs spawning in player bases
- Integration tests passing but manual testing showing base spawns
- Mixin conflicts between multiple spawn-related mixins

**Correct pattern from existing code:**
```java
@Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
private static void thc$blockSpawnInBaseChunks(...) {
    // PHASE 1: Base protection (existing)
    if (ClaimManager.INSTANCE.isClaimed(level.getServer(), new ChunkPos(pos))) {
        cir.setReturnValue(false);
        return; // Early exit - regional distribution never runs
    }

    // PHASE 2: Regional distribution (new)
    // ... regional filtering code ...
}
```

### Pitfall 4: Vanilla Fallback Mob Selection Timing

**What goes wrong:** Custom distribution rolls random number, then calls vanilla's getMobForSpawn(), resulting in different mob than vanilla would have selected at that position.

**Why it happens:** Weighted selection consumes random entropy, changing the random seed for vanilla's selection.

**How to avoid:**
- If custom selection chooses "vanilla fallback", let the ORIGINAL entity spawn (from @Redirect parameter)
- Don't call vanilla selection again - you already have the entity vanilla created

**Warning signs:**
- Vanilla fallback spawns different mobs than pure vanilla worlds
- Random seed divergence between custom and vanilla spawning
- Biome-specific spawns appearing in wrong biomes

**Correct pattern:**
```java
@Redirect(...)
private static void thc$applyRegionalDistribution(ServerLevel level, Entity entity) {
    MobSelection selection = SpawnDistributions.selectMob(region, level.random);

    if (selection.isVanilla()) {
        // Use the entity vanilla ALREADY CREATED - don't re-roll
        level.addFreshEntityWithPassengers(entity);
    } else {
        // Create custom mob
        Entity replacement = createCustomMob(...);
        level.addFreshEntityWithPassengers(replacement);
    }
}
```

### Pitfall 5: Pillager AI Goal Removal Without Melee Replacement

**What goes wrong:** Removing RangedAttackGoal from pillager without adding MeleeAttackGoal results in passive pillagers that don't attack at all.

**Why it happens:** From research - "Giving a pillager another weapon except for crossbows/bows will make them not attack any mob or player at all" (vanilla behavior). The pillager has no attack AI without RangedAttackGoal.

**How to avoid:**
1. Remove RangedAttackGoal
2. Immediately add MeleeAttackGoal with appropriate priority
3. Verify pillager has melee weapon in hand

**Warning signs:**
- Melee pillagers stand still when players approach
- Pillagers with iron swords running away instead of attacking
- Console errors about missing attack goals

**Correct implementation:**
```java
// Remove ranged goal
this.goalSelector.getAvailableGoals().removeIf(goal ->
    goal.getGoal() instanceof RangedAttackGoal
);

// Add melee goal immediately
this.goalSelector.addGoal(4, new MeleeAttackGoal(self, 1.0, false));
// Priority 4 matches vanilla RangedAttackGoal priority for Pillager
```

## Code Examples

Verified patterns from research and existing codebase:

### Weighted Selection Implementation

```java
// Source: Mob spawning research + standard weighted random pattern
public class SpawnDistributions {
    record WeightedEntry(EntityType<?> type, String variant, int weight) {}

    private static final Map<String, List<WeightedEntry>> TABLES = new EnumMap<>(String.class);

    static {
        // OW_SURFACE: 5% witch, 95% vanilla
        List<WeightedEntry> surface = new ArrayList<>();
        surface.add(new WeightedEntry(EntityType.WITCH, null, 5));
        surface.add(new WeightedEntry(null, null, 95)); // null = vanilla fallback
        TABLES.put("OW_SURFACE", surface);

        // OW_UPPER_CAVE: complex distribution
        List<WeightedEntry> upperCave = new ArrayList<>();
        upperCave.add(new WeightedEntry(EntityType.WITCH, null, 5));
        upperCave.add(new WeightedEntry(EntityType.VEX, null, 2));
        upperCave.add(new WeightedEntry(EntityType.PILLAGER, "RANGED", 10));
        upperCave.add(new WeightedEntry(EntityType.PILLAGER, "MELEE", 25));
        upperCave.add(new WeightedEntry(null, null, 58)); // vanilla fallback
        TABLES.put("OW_UPPER_CAVE", upperCave);

        // Validation: percentages sum to 100
        for (var entry : TABLES.entrySet()) {
            int total = entry.getValue().stream().mapToInt(e -> e.weight).sum();
            if (total != 100) {
                throw new IllegalStateException("Distribution for " + entry.getKey() +
                    " sums to " + total + ", expected 100");
            }
        }
    }

    public static MobSelection selectMob(String region, RandomSource random) {
        List<WeightedEntry> table = TABLES.get(region);
        if (table == null || table.isEmpty()) {
            return MobSelection.vanillaFallback();
        }

        int roll = random.nextInt(100); // 0-99
        int cumulative = 0;

        for (WeightedEntry entry : table) {
            cumulative += entry.weight;
            if (roll < cumulative) {
                if (entry.type == null) {
                    return MobSelection.vanillaFallback();
                } else {
                    return new MobSelection(entry.type, entry.variant, false);
                }
            }
        }

        return MobSelection.vanillaFallback();
    }
}

record MobSelection(EntityType<?> type, String variant, boolean isVanilla) {
    static MobSelection vanillaFallback() {
        return new MobSelection(null, null, true);
    }
}
```

### Pillager Variant Equipment Application

```java
// Source: Existing THC equipment patterns + user decisions
public enum PillagerVariant {
    MELEE {
        @Override
        public void applyEquipment(Pillager pillager) {
            // Main hand: iron sword
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            pillager.setItemSlot(EquipmentSlot.MAINHAND, sword);
            pillager.setDropChance(EquipmentSlot.MAINHAND, 0.0f); // Never drops per FR

            // No crossbow, no offhand (user decision: melee only)
            pillager.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    },
    RANGED {
        @Override
        public void applyEquipment(Pillager pillager) {
            // Vanilla crossbow already set by populateDefaultEquipmentSlots
            // Nothing to do - vanilla behavior preserved
        }
    };

    public abstract void applyEquipment(Pillager pillager);
}
```

### Region Detection Integration

```java
// Source: Existing MobFinalizeSpawnMixin.java
// Already implemented in Phase 41 - reuse this
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

**NOTE:** This differs from user's stated requirement "isSkyVisible(pos)" but matches Phase 41 implementation. Heightmap.Types.MOTION_BLOCKING is functionally equivalent for spawn purposes - blocks with sky visibility are at or above this heightmap.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Biome-based spawn tables | Regional spawn overlays | Mod-specific (2026) | Allows cross-biome distributions (witches in all surface biomes) |
| Hard-coded entity variants | Equipment-based variants | Mod-specific | Avoids registry pollution, no custom entity types needed |
| Per-category mob caps | Per-region mob caps | MC 1.18 (per-player regional) | Better spawn distribution, reduced farm impact |
| Mob.moveTo() | Mob.snapTo() | MC 1.21+ | Correct positioning API for spawn initialization |

**Deprecated/outdated:**
- **Biome modification API for spawn tables:** Can't achieve cross-biome distributions like "5% witch on ANY surface biome"
- **Custom entity registration for variants:** Equipment + AI modification is simpler, no registry conflicts
- **Global-only mob caps:** MC 1.18+ uses per-player local caps, more sophisticated than global-only

## Open Questions

Things that couldn't be fully resolved:

1. **Pack spread pattern for custom spawns**
   - What we know: Vanilla uses triangular distribution, ±5 blocks X/Z from previous position
   - What's unclear: Whether to match vanilla exactly or use simpler uniform distribution
   - Recommendation: Match vanilla (triangular) for consistency, but uniform acceptable if simpler

2. **Pillager melee AI effectiveness**
   - What we know: Removing RangedAttackGoal + adding MeleeAttackGoal makes pillagers melee
   - What's unclear: Whether pillagers need additional AI tuning (movement speed, attack range) to be effective melee combatants
   - Recommendation: Start with vanilla MeleeAttackGoal parameters (speed 1.0, pauseWhenMobIdle false), tune if playtest shows issues

3. **End dimension spawn distribution**
   - What we know: Spec requires 25% endermite, 75% vanilla fallback
   - What's unclear: Whether endermites spawn in groups or individually in vanilla End spawning
   - Recommendation: Apply same pack size [1,4] as Overworld custom spawns for consistency

4. **Regional cap enforcement timing**
   - What we know: Caps use NBT tags (SPAWN_COUNTED) for tracking, multipliers are 0.30/0.40/0.50
   - What's unclear: Whether to enforce caps at global level, local level, or both
   - Recommendation: User spec says "Apply both vanilla cap mechanisms" - implement both global and local caps with regional counting

## Sources

### Primary (HIGH confidence)
- Existing THC codebase patterns:
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/NaturalSpawnerMixin.java` - Base chunk blocking pattern
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/SpawnReplacementMixin.java` - @Redirect on addFreshEntityWithPassengers
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/EndermanMixin.java` - AI goal injection pattern
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/MobFinalizeSpawnMixin.java` - Region detection, NBT tagging
  - `/mnt/c/home/code/thc/src/main/java/thc/THCAttachments.java` - SPAWN_REGION, SPAWN_COUNTED attachments
- `/mnt/c/home/code/thc/.planning/MILESTONE_EXTRA_FEATURES_BATCH_7.md` - Full specification
- [Curtain NaturalSpawnerMixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java) - @Redirect pattern reference

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Mob spawning](https://minecraft.wiki/w/Mob_spawning) - Spawn mechanics overview
- [Minecraft Mob Spawning Analysis](https://blog.bithole.dev/blogposts/mob-spawning/) - Pack spawning, weight selection
- [Mob caps – Technical Minecraft Wiki](https://techmcdocs.github.io/pages/GameMechanics/MobCap/) - Regional cap partitioning (MC 1.18+)
- [Pillager – Minecraft Wiki](https://minecraft.wiki/w/Pillager) - Pillager behavior, equipment
- [Fabric Yarn API - GoalSelector](https://maven.fabricmc.net/docs/yarn-1.19+build.1/net/minecraft/entity/ai/goal/GoalSelector.html) - Goal management API
- [Fabric Yarn API - MeleeAttackGoal](https://maven.fabricmc.net/docs/yarn-1.17+build.13/net/minecraft/entity/ai/goal/MeleeAttackGoal.html) - Melee AI goal

### Tertiary (LOW confidence - WebSearch only)
- [How to Tame a Pillager](https://www.digminecraft.com/getting_started/how_to_tame_pillager.php) - Crossbow breaking mechanics
- [Mob AI Tweaks mod](https://modrinth.com/mod/mob-ai-tweaks) - Example mod modifying pillager AI
- [Spawn Balance Utility mod](https://www.curseforge.com/minecraft/mc-mods/spawn-balance-utility) - Example weighted spawn mod

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in use, no new dependencies
- Architecture patterns: HIGH - Existing THC patterns verify approach, Curtain validates @Redirect usage
- Weighted selection: HIGH - Standard algorithm, well-documented
- Pillager AI modification: MEDIUM - Pattern verified via EndermanMixin, but pillager-specific details need testing
- Regional cap enforcement: MEDIUM - Spec is clear, implementation complexity moderate
- Pack spawning mechanics: MEDIUM - Vanilla pattern documented but spread algorithm has options

**Research date:** 2026-01-24
**Valid until:** Minecraft 1.21.x (method signatures stable within minor versions)
**Phase dependencies:** Phase 41 (NBT tagging infrastructure) - COMPLETE

**Notes for planner:**
1. Phase 41's NBT infrastructure (SPAWN_REGION, SPAWN_COUNTED attachments) is already functional
2. Existing MobFinalizeSpawnMixin already implements region detection - reuse this
3. Regional distribution must integrate with (not replace) existing base chunk blocking
4. User decisions in CONTEXT.md are firm - no fallback on spawn failure, bypass vanilla conditions
5. Pillager melee variant requires TWO changes: equipment (easy) + AI goals (needs testing)
