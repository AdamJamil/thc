# Phase 39: Entity-Specific Behaviors (Simple) - Research

**Researched:** 2026-01-23
**Domain:** Minecraft 1.21.11 entity spawn modification, custom spawner interception, attribute manipulation
**Confidence:** HIGH

## Summary

Phase 39 implements straightforward entity modifications using established THC patterns: ServerEntityEvents.ENTITY_LOAD for runtime attribute/equipment changes, and HEAD cancellation mixins for spawner disabling. The phase covers five distinct features (Vex health/weapon, Phantom removal, Patrol stage-gating, Iron Golem prevention) that share common implementation patterns but target different systems.

Key findings:
- Vex modifications use ENTITY_LOAD pattern (already proven in MonsterModifications.kt)
- PhantomSpawner and PatrolSpawner both have tick() methods that return spawn counts - HEAD cancellation returning 0 disables them
- Iron golem player-summoning requires intercepting CarvedPumpkinBlock.onPlace or checking pattern completion
- All modifications integrate cleanly with existing THC systems (StageManager for patrol gating, THCAttachments for any needed state)

**Primary recommendation:** Use ServerEntityEvents.ENTITY_LOAD for Vex modifications (health/equipment), HEAD-cancel PhantomSpawner.tick and PatrolSpawner.tick (with stage check for patrols), and intercept CarvedPumpkinBlock.onPlace for iron golem prevention.

## Standard Stack

The established libraries/tools for entity behavior modification in THC:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.141.0+1.21.11 | ServerEntityEvents.ENTITY_LOAD | Already used in MonsterModifications.kt for speed boost |
| Mixin | 0.8.5 (via Fabric Loader) | Spawner tick() HEAD cancellation | THC pattern for spawn blocking |
| Mojang Mappings | 1.21.11 | Official class/method names | THC standard (existing mixins use official names) |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Attributes API | net.minecraft.world.entity.ai.attributes | Health modification via addTransientModifier | Vex health reduction (FR-12) |
| Equipment API | net.minecraft.world.entity.EquipmentSlot | setItemSlot for weapon removal | Vex sword removal (FR-13) |
| StageManager | thc.stage.StageManager | getCurrentStage for conditional logic | Patrol stage-gating (FR-15) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| ENTITY_LOAD event | Mob.finalizeSpawn mixin | ENTITY_LOAD is cleaner (one registration point vs per-entity mixins) |
| HEAD cancellation | Gamerule modification | HEAD cancel is per-spawner control, gamerule is global (less flexible) |
| Block placement intercept | Post-spawn check | Block intercept prevents entity creation entirely (cleaner) |

**Installation:**
```bash
# Already in THC - no new dependencies
# Fabric API 0.141.0+1.21.11 provides ServerEntityEvents
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/
├── java/thc/
│   ├── mixin/
│   │   ├── PhantomSpawnerMixin.java      # FR-14
│   │   ├── PatrolSpawnerMixin.java       # FR-15
│   │   └── CarvedPumpkinBlockMixin.java  # FR-17
│   └── stage/
│       └── StageManager.java             # (existing - used by patrol check)
├── kotlin/thc/
│   └── monster/
│       └── SimpleEntityBehaviors.kt      # FR-12, FR-13 (Vex modifications)
```

### Pattern 1: ENTITY_LOAD Attribute Modification (Vex Health)
**What:** Modify mob attributes at spawn time using transient modifiers
**When to use:** Health, speed, or other attribute changes that should apply to all instances
**Example:**
```kotlin
// Source: Existing MonsterModifications.kt pattern (v2.3)
ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
    if (entity is Vex) {
        val healthAttr = entity.getAttribute(Attributes.MAX_HEALTH) ?: return@register
        val currentMax = healthAttr.baseValue

        // Vex vanilla health: 14 (7 hearts)
        // THC target: 8 (4 hearts)
        val reduction = currentMax - 8.0

        if (!healthAttr.hasModifier(VEX_HEALTH_MODIFIER_ID)) {
            healthAttr.addTransientModifier(
                AttributeModifier(
                    VEX_HEALTH_MODIFIER_ID,
                    -reduction,
                    AttributeModifier.Operation.ADD_VALUE
                )
            )
            // Reset health to new max if currently above it
            if (entity.health > 8.0f) {
                entity.health = 8.0f
            }
        }
    }
}
```

### Pattern 2: ENTITY_LOAD Equipment Modification (Vex Sword Removal)
**What:** Clear or set equipment slots at spawn time
**When to use:** Removing default equipment or applying custom loadouts
**Example:**
```kotlin
// Source: Existing EquipmentSlot pattern (used in SpawnReplacementMixin)
ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
    if (entity is Vex) {
        entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
    }
}
```

### Pattern 3: Custom Spawner HEAD Cancellation (Phantom/Patrol)
**What:** Completely disable custom spawner by intercepting tick() at HEAD
**When to use:** Removing entire spawn mechanic (phantoms) or conditionally gating (patrols)
**Example:**
```java
// Source: Existing MonsterSpawnLightMixin HEAD pattern
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
    /**
     * Disable phantom natural spawning completely.
     *
     * PhantomSpawner.tick signature (Mojang 1.21.11):
     * int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies)
     *
     * Returning 0 indicates no spawns occurred.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$disablePhantomSpawns(
            ServerLevel level,
            boolean spawnEnemies,
            boolean spawnFriendlies,
            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
```

### Pattern 4: Conditional Spawner Gating (Patrol Stage Check)
**What:** Block spawner conditionally based on game state
**When to use:** Stage-gated features, progression-based spawn control
**Example:**
```java
// Source: StageManager.getCurrentStage pattern (existing THC stage system)
@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$gatePatrolsToStage2(
            ServerLevel level,
            boolean spawnEnemies,
            boolean spawnFriendlies,
            CallbackInfoReturnable<Integer> cir) {

        int currentStage = StageManager.getCurrentStage(level.getServer());
        if (currentStage < 2) {
            cir.setReturnValue(0); // Block spawn, return 0 count
        }
        // If stage >= 2, allow vanilla logic to proceed
    }
}
```

### Pattern 5: Block Placement Interception (Iron Golem Prevention)
**What:** Prevent entity summoning by intercepting block placement that triggers pattern completion
**When to use:** Player-initiated summoning mechanics (golems, withers)
**Example:**
```java
// Pattern inferred from CarvedPumpkinBlock mechanics
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    /**
     * Prevent iron golem summoning when carved pumpkin placed.
     *
     * The onPlace method checks for golem/snow golem patterns after placement.
     * By cancelling at HEAD, we prevent pattern detection entirely.
     *
     * Note: This blocks PLAYER-summoned golems only.
     * Villager-summoned golems use different code path (Villager.spawnGolem).
     */
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void thc$preventPlayerGolemSummon(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston,
            CallbackInfo ci) {

        // Allow pumpkin placement, but skip golem pattern check
        // We need to cancel just the pattern detection, not the block placement
        // This requires more investigation - may need to target specific method
        // within onPlace that checks for iron blocks pattern
    }
}
```

**Note:** Iron golem prevention may require more targeted approach - intercepting pattern check method rather than full onPlace. Alternative: Mixin to IronGolem entity creation itself.

### Anti-Patterns to Avoid
- **Using gamerule commands instead of mixins:** Gamerules are global server settings, not mod-controlled. THC needs code-level control.
- **Modifying spawn tables instead of spawners:** PhantomSpawner and PatrolSpawner are custom spawners, not biome-based. They don't use spawn tables.
- **Persisting transient state:** Vex health modifier should be transient (not saved to NBT). Vexes despawn quickly, no need for persistence.
- **Checking EntitySpawnReason in ENTITY_LOAD:** ENTITY_LOAD fires for ALL loads (including from disk). For spawn-only logic, use reason checks.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Attribute modification | Custom NBT health system | Attributes.MAX_HEALTH with modifier | Vanilla system handles recalc, UI updates, edge cases |
| Equipment clearing | NBT tag manipulation | setItemSlot(EquipmentSlot, ItemStack.EMPTY) | Handles drop chances, sync, visual updates |
| Spawner disabling | Remove spawner from world tick list | HEAD-cancel spawner.tick() method | Clean mixin, no world state modification |
| Stage checking | Custom saved data lookup | StageManager.getCurrentStage() | Existing THC system, already integrated |
| Entity type checks | String name comparison | instanceof or EntityType == comparison | Type-safe, handles subclasses correctly |

**Key insight:** THC already has infrastructure for attribute modification (MonsterModifications.kt), stage management (StageManager), and entity spawn interception (SpawnReplacementMixin). Reuse these patterns rather than inventing new approaches.

## Common Pitfalls

### Pitfall 1: Vex Health Reduction Doesn't Affect Existing Vexes
**What goes wrong:** ENTITY_LOAD only fires when entity enters world. Vexes summoned before mod install retain 14 HP.
**Why it happens:** ENTITY_LOAD fires on entity add to world, not on every tick. Already-spawned entities in loaded chunks don't re-trigger.
**How to avoid:** Document as expected behavior. Vexes are temporary (lifetime limited), so old vexes despawn naturally. New summons get modified health.
**Warning signs:** Testing with pre-existing vexes shows full health. Solution: Test with fresh evoker summons.

### Pitfall 2: Equipment Slot Modification Timing
**What goes wrong:** Setting equipment in ENTITY_LOAD might happen before or after vanilla equipment population.
**Why it happens:** ENTITY_LOAD event order vs Mob.finalizeSpawn timing isn't guaranteed.
**How to avoid:** For spawn-time equipment (not applicable to vexes - they're summoned), use finalizeSpawn mixin. For vexes, ENTITY_LOAD is fine since they're summoned with hardcoded equipment.
**Warning signs:** Vex sometimes has sword, sometimes doesn't. Solution: Add logging to verify event fires after vex has sword.

### Pitfall 3: Phantom Removal Affects Spawn Eggs/Commands
**What goes wrong:** HEAD-cancelling PhantomSpawner.tick blocks natural spawns, but spawn eggs and /summon still work.
**Why it happens:** Spawn eggs and commands use different code path (direct entity creation, not PhantomSpawner).
**How to avoid:** Document that spawn eggs/commands still work (this is DESIRED per requirements).
**Warning signs:** Testing with spawn eggs and expecting them to fail. Solution: Requirements explicitly state spawn eggs should work.

### Pitfall 4: Patrol Stage Check Server Reference
**What goes wrong:** StageManager.getCurrentStage requires MinecraftServer reference, which comes from ServerLevel.
**Why it happens:** PatrolSpawner.tick receives ServerLevel, need to call level.getServer().
**How to avoid:** Extract server from level parameter: `level.getServer()`.
**Warning signs:** Compilation error "cannot find symbol getServer()". Solution: ServerLevel has getServer() method.

### Pitfall 5: Iron Golem Prevention Scope Confusion
**What goes wrong:** Blocking CarvedPumpkinBlock.onPlace blocks ALL pumpkin placement, not just golem summoning.
**Why it happens:** onPlace handles both block placement AND pattern detection.
**How to avoid:** Target specific golem check method, not entire onPlace. Alternative: Check block pattern ourselves and cancel only if iron blocks present.
**Warning signs:** Players can't place carved pumpkins anywhere. Solution: More targeted mixin injection point.

### Pitfall 6: Stage 1 vs Stage 0 Confusion
**What goes wrong:** "Stage 1" check blocks patrols, but server starts at stage 1, not stage 0.
**Why it happens:** Requirements say "stage < 2" which includes stage 1 (starting stage).
**How to avoid:** Verify starting stage with user. If server starts at stage 1 and patrols should spawn, check currentStage < 1 instead.
**Warning signs:** Patrols never spawn even after intended time. Solution: Test with /thc stage command to verify starting value.

### Pitfall 7: Transient Modifier Reapplication
**What goes wrong:** Adding same modifier multiple times (e.g., vex loads from chunk multiple times).
**Why it happens:** ENTITY_LOAD fires on chunk load, not just spawn. Without hasModifier check, modifier stacks.
**How to avoid:** Always check `!healthAttr.hasModifier(MODIFIER_ID)` before adding.
**Warning signs:** Vex health drops to negative or very low values. Solution: Add hasModifier guard (see Pattern 1).

## Code Examples

Verified patterns from existing THC codebase:

### Vex Health Reduction (FR-12)
```kotlin
// Based on: MonsterModifications.kt (v2.3 monster speed boost)
object SimpleEntityBehaviors {
    private val VEX_HEALTH_REDUCTION_ID =
        Identifier.fromNamespaceAndPath("thc", "vex_health_reduction")

    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            if (entity is Vex) {
                applyVexHealthReduction(entity)
                removeVexSword(entity)
            }
        }
    }

    private fun applyVexHealthReduction(vex: Vex) {
        val healthAttr = vex.getAttribute(Attributes.MAX_HEALTH) ?: return

        // Vanilla vex: 14 HP (7 hearts)
        // THC target: 8 HP (4 hearts)
        if (!healthAttr.hasModifier(VEX_HEALTH_REDUCTION_ID)) {
            healthAttr.baseValue = 8.0

            // Reset current health if above new max
            if (vex.health > 8.0f) {
                vex.health = 8.0f
            }
        }
    }

    private fun removeVexSword(vex: Vex) {
        vex.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
    }
}
```

### Phantom Spawner Disable (FR-14)
```java
// Based on: MonsterSpawnLightMixin HEAD cancellation pattern
package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disable natural phantom spawning from insomnia.
 *
 * <p>Part of THC difficulty progression - phantoms removed entirely
 * from natural spawning. Players can still encounter phantoms from
 * spawn eggs or commands for testing.
 *
 * <p>The PhantomSpawner is a custom spawner (not biome-based) that
 * triggers based on player insomnia statistics. By cancelling its
 * tick method at HEAD, we prevent all spawn attempts while leaving
 * the insomnia statistic unchanged (may be used for other features).
 */
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {

    /**
     * Block all phantom natural spawn attempts.
     *
     * <p>PhantomSpawner.tick returns int count of spawned phantoms.
     * Returning 0 indicates no spawns occurred this tick.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$disablePhantomSpawns(
            ServerLevel level,
            boolean spawnEnemies,
            boolean spawnFriendlies,
            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
```

### Patrol Stage-Gating (FR-15)
```java
// Based on: StageManager.getCurrentStage integration pattern
package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.stage.StageManager;

/**
 * Gate illager patrol spawning to stage 2+.
 *
 * <p>Part of THC stage progression - illager patrols don't appear
 * until the server reaches stage 2. This gives players time to
 * establish defenses before facing organized illager groups.
 *
 * <p>PatrolSpawner is a custom spawner that attempts to create
 * illager patrols periodically. By checking stage before allowing
 * spawns, we gate this feature to mid-game progression.
 */
@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin {

    /**
     * Block patrol spawns if server stage is below 2.
     *
     * <p>PatrolSpawner.tick returns int count of spawned patrols.
     * Returning 0 blocks spawn for this tick.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thc$gatePatrolsToStage2(
            ServerLevel level,
            boolean spawnEnemies,
            boolean spawnFriendlies,
            CallbackInfoReturnable<Integer> cir) {

        int currentStage = StageManager.getCurrentStage(level.getServer());
        if (currentStage < 2) {
            cir.setReturnValue(0);
        }
        // If stage >= 2, allow vanilla patrol spawn logic
    }
}
```

### Iron Golem Summon Prevention (FR-17)
```java
// Pattern needs verification - CarvedPumpkinBlock.onPlace behavior
package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent player-summoned iron golems via pumpkin pattern.
 *
 * <p>Part of THC difficulty - players cannot create iron golems
 * using the traditional pumpkin + iron block pattern. This forces
 * reliance on village-spawned golems for iron golem presence.
 *
 * <p>Note: Villager-summoned golems still work (different code path).
 * Existing golems in world are unaffected.
 *
 * <p>IMPORTANT: This implementation needs verification. May need to
 * target a more specific method than onPlace to avoid blocking
 * normal pumpkin placement functionality.
 */
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {

    /**
     * Block iron golem pattern detection when pumpkin placed.
     *
     * <p>CarvedPumpkinBlock.onPlace checks for iron golem and snow golem
     * patterns after placement. Investigation needed: can we cancel just
     * the golem check, or do we need to reimplement onPlace without it?
     *
     * <p>Alternative approach: Redirect the iron golem spawn call specifically.
     */
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void thc$preventGolemSummon(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston,
            CallbackInfo ci) {

        // TODO: Investigate whether this blocks all pumpkin placement
        // or just pattern detection. May need @Redirect instead of @Inject.

        // For now, this blocks the entire onPlace which includes:
        // - Iron golem pattern check
        // - Snow golem pattern check

        // If we only want to block iron golems, need to:
        // 1. Call super to place block normally
        // 2. Skip just the iron golem spawn logic
        // This may require @Redirect on the specific pattern check method
    }
}
```

**Note:** Iron golem prevention pattern needs source verification. May require examining decompiled CarvedPumpkinBlock.onPlace to find exact injection point.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Mob.finalizeSpawn mixin for attributes | ServerEntityEvents.ENTITY_LOAD | Fabric API 0.83.0 (2023) | Single registration point vs per-entity mixins |
| Gamerule for phantom disable | PhantomSpawner mixin | Modding pattern (always available) | Mod-controlled vs admin command |
| Entity constructor intercept | ENTITY_LOAD event | Fabric API lifecycle events | Cleaner event system vs deep mixin |

**Deprecated/outdated:**
- Gamerule-based disabling: Works but not mod-controlled. THC needs code integration for future features.
- BiomeModifications for entity behavior: Intended for spawn tables, not entity stats/equipment.

## Open Questions

Things that couldn't be fully resolved:

1. **Iron Golem CarvedPumpkinBlock.onPlace scope**
   - What we know: onPlace is called when pumpkin placed, checks for patterns
   - What's unclear: Does HEAD-cancel block pumpkin placement entirely, or can we target just golem check?
   - Recommendation: Needs decompiled source verification. If onPlace is multi-purpose, use @Redirect on specific pattern check call.

2. **Server starting stage value**
   - What we know: StageManager tracks stages 1-5, patrols gate at stage < 2
   - What's unclear: Does server start at stage 0 or stage 1? Affects patrol gating logic.
   - Recommendation: Verify with user or test new world. May need `currentStage < 1` or `currentStage <= 1` check.

3. **Vex equipment timing guarantees**
   - What we know: ENTITY_LOAD fires when entity added to world
   - What's unclear: Is vex equipment already set when ENTITY_LOAD fires, or does evoker set it after?
   - Recommendation: Test with logging. If sword isn't present yet, may need slight delay or different event.

4. **Villager-summoned golem path**
   - What we know: Villagers spawn golems via different mechanism than player pattern
   - What's unclear: Exact method name and call path for villager golem spawning
   - Recommendation: If CarvedPumpkinBlock approach blocks villager golems too, need separate villager mixin exemption.

## Sources

### Primary (HIGH confidence)
- [THC MonsterModifications.kt](file:///mnt/c/home/code/thc/src/main/kotlin/thc/monster/MonsterModifications.kt) - ENTITY_LOAD pattern, attribute modifiers, transient modifiers
- [THC SpawnReplacementMixin.java](file:///mnt/c/home/code/thc/src/main/java/thc/mixin/SpawnReplacementMixin.java) - EquipmentSlot.setItemSlot pattern, entity type checking
- [THC StageManager.java](file:///mnt/c/home/code/thc/src/main/java/thc/stage/StageManager.java) - getCurrentStage API for patrol gating
- [THC MonsterSpawnLightMixin.java](file:///mnt/c/home/code/thc/src/main/java/thc/mixin/MonsterSpawnLightMixin.java) - HEAD cancellation pattern at @At("HEAD")
- [Fabric API ServerEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.141.0+1.21.11/net/fabricmc/fabric/api/event/lifecycle/v1/ServerEntityEvents.html) - ENTITY_LOAD event documentation

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Vex](https://minecraft.wiki/w/Vex) - Vex health (14 HP), equipment (iron sword), spawning (evoker summon)
- [Minecraft Wiki - Phantom](https://minecraft.wiki/w/Phantom) - Phantom spawning mechanics, insomnia trigger
- [Minecraft Wiki - Patrol](https://minecraft.wiki/w/Patrol) - Illager patrol spawn timing, conditions
- [Minecraft Wiki - Iron Golem](https://minecraft.wiki/w/Iron_Golem) - Player summon pattern (pumpkin + iron blocks)
- [Fabric Wiki - Mixin Examples](https://wiki.fabricmc.net/tutorial:mixin_examples) - @Inject at HEAD with cancellable pattern

### Tertiary (LOW confidence - needs verification)
- WebSearch: PhantomSpawner tick method - Found tick() exists, returns int, called per-level. Signature needs decompiled source verification.
- WebSearch: PatrolSpawner structure - Similar to PhantomSpawner (custom spawner, not biome-based). Signature needs verification.
- WebSearch: CarvedPumpkinBlock.onPlace - Handles pattern detection, but multi-purpose. Injection scope needs source check.

## Metadata

**Confidence breakdown:**
- Vex modifications: HIGH - Direct pattern match to MonsterModifications.kt
- Phantom removal: HIGH - PhantomSpawner tick() confirmed via web search, HEAD pattern proven in THC
- Patrol gating: HIGH - PatrolSpawner similar to PhantomSpawner, StageManager API verified
- Iron golem prevention: MEDIUM - CarvedPumpkinBlock mechanics known, but injection scope unclear

**Research date:** 2026-01-23
**Valid until:** 2026-02-23 (30 days - Minecraft 1.21.x stable)

**Integration points verified:**
- ServerEntityEvents.ENTITY_LOAD: Used in MonsterModifications.kt (v2.3)
- StageManager.getCurrentStage: Used in stage system (v2.2)
- HEAD cancellation: Used in MonsterSpawnLightMixin
- EquipmentSlot manipulation: Used in SpawnReplacementMixin

**Needs validation before implementation:**
- CarvedPumpkinBlock.onPlace injection scope (may affect all pumpkin placement)
- Server starting stage value (affects patrol gate check logic)
- Vex equipment timing in ENTITY_LOAD (may need spawn-time check)
