# Architecture Research: v1.1 Extra Features Integration

**Researched:** 2026-01-18
**Confidence:** HIGH (based on existing THC codebase patterns)

## Summary

The v1.1 features integrate into the existing THC architecture using established patterns. The codebase follows a consistent separation: Java mixins for Minecraft class modifications, Kotlin objects for business logic/event handlers, and data files (JSON) for recipe/loot modifications.

**Key finding:** All four features fit naturally into existing patterns. No new infrastructure required.

## Integration Strategy

### 1. Drowning Modification

**Approach:** New mixin targeting `LivingEntity.baseTick()` or the `decreaseAirSupply()` method.

**Target class:** `net.minecraft.world.entity.LivingEntity`

**Method options:**
- `baseTick()` - called every tick, contains drowning check logic
- `decreaseAirSupply(int air)` - reduces air supply, called when underwater

**Recommended:** Mixin into `baseTick()` using `@Inject` at the point where drowning damage is applied. Use a tick counter to apply damage every 4th tick instead of every tick.

**Package location:** `thc.mixin.LivingEntityDrowningMixin.java`

**Note:** Could be merged into existing `LivingEntityMixin.java`, but a separate mixin keeps concerns isolated (buckler vs drowning). Recommend separate file for clarity.

**Why not extend existing LivingEntityMixin?**
- Existing mixin is ~235 lines focused on buckler mechanics
- Drowning is unrelated functionality
- Separate mixins improve maintainability and testing

### 2. Spear/Trident Removal

**Approach:** Multi-pronged data + runtime removal.

**Components:**

1. **Recipe removal** - Empty JSON file at `data/minecraft/recipe/trident.json`
   - Note: Trident has no vanilla recipe - this is defensive only

2. **Loot table removal** - Modify Drowned loot tables
   - Drowned hold tridents 6.25% of the time in Java Edition
   - Loot table at `data/minecraft/loot_table/entities/drowned.json` can override drops
   - Use LootTableEvents.MODIFY_DROPS (same pattern as shield removal in THC.kt)

3. **Mob spawn equipment** - Mixin to remove tridents from Drowned spawn
   - Target: `net.minecraft.world.entity.monster.Drowned`
   - Method: `populateDefaultEquipmentSlots()` or inject into spawn logic
   - Strip trident from equipment after natural spawn

4. **Vault loot** (optional) - Trial vault loot tables if present in 1.21

**Package location:**
- Mixin: `thc.mixin.DrownedMixin.java`
- Event handler: `thc.world.SpearRemoval.kt` or inline in `THC.kt`

**Files:**
```
src/main/resources/data/minecraft/loot_table/entities/drowned.json
src/main/java/thc/mixin/DrownedMixin.java (equipment)
```

### 3. Projectile System (Physics + Aggro)

**Approach:** Projectile physics and hit handling require mixins. Two separate concerns:

**A. Physics (velocity + gravity):**

**Target class:** `net.minecraft.world.entity.projectile.AbstractArrow` (covers arrows and tridents)

**Methods:**
- `tick()` - modify velocity each tick
- Initial velocity boost: inject at shoot/fire point
- Quadratic gravity: modify vertical velocity component in tick

**Implementation:**
```java
@Mixin(AbstractArrow.class)
public class AbstractArrowPhysicsMixin {
    // Boost initial velocity 20% at shoot
    // After 8 blocks traveled, apply quadratic gravity
}
```

**Tracking travel distance:** Use `@Unique` field to track ticks/distance since launch.

**B. Aggro + Effects on hit:**

**Target class:** Same `AbstractArrow` or the `onHitEntity()` method

**Method:** `onHitEntity(EntityHitResult)` - inject at HEAD or TAIL to apply effects

**Effects to apply:**
- Speed II for 6 seconds (120 ticks)
- Glowing for 6 seconds
- Redirect mob aggro to shooter

**Aggro redirection:** Use `Mob.setTarget()` for mobs in radius after hit

**Package location:**
- `thc.mixin.AbstractArrowPhysicsMixin.java` (physics)
- `thc.mixin.AbstractArrowAggroMixin.java` (effects) OR combine into single mixin
- `thc.projectile.ProjectileEffects.kt` (helper for effect application)

**Recommendation:** Single mixin file `AbstractArrowMixin.java` with clear section separation via comments.

### 4. Aggro + Effects

Already covered in section 3B above. The logic lives in:
- Mixin for hook point (projectile hit detection)
- Kotlin helper for effect application logic

**Effect application pattern (from existing codebase):**
```kotlin
// Similar to LivingEntityMixin stun effect
MobEffectInstance(MobEffects.SPEED, 120, 1)  // Speed II
MobEffectInstance(MobEffects.GLOWING, 120, 0)  // Glowing
```

## Package Organization

```
thc/
  mixin/
    LivingEntityMixin.java       # Existing - buckler damage reduction
    LivingEntityDrowningMixin.java  # NEW - drowning tick rate
    DrownedMixin.java            # NEW - remove trident equipment
    AbstractArrowMixin.java      # NEW - physics + aggro effects
    access/
      ItemAccessor.java          # Existing

  world/
    MiningFatigue.kt             # Existing
    VillageProtection.kt         # Existing
    WorldRestrictions.kt         # Existing
    SpearRemoval.kt              # NEW (optional, could be in THC.kt)

  projectile/                    # NEW package
    ProjectileEffects.kt         # Effect application + aggro redirect logic

data/minecraft/
  loot_table/entities/
    drowned.json                 # NEW - strip trident drops
```

**Alternative (minimal):** Skip `projectile/` package, put logic directly in mixin or `world/` package. Recommend new package for future expansion (snowballs, ender pearls, etc).

## Mixin Map

| Mixin | Target Class | Purpose | Injection Point |
|-------|--------------|---------|-----------------|
| LivingEntityMixin | LivingEntity | Buckler damage reduction | `hurtServer` HEAD/TAIL |
| **LivingEntityDrowningMixin** | LivingEntity | Reduce drowning tick rate 4x | `baseTick` custom |
| **DrownedMixin** | Drowned | Strip trident from spawn equipment | `populateDefaultEquipmentSlots` TAIL |
| **AbstractArrowMixin** | AbstractArrow | Physics + aggro effects | `tick` + `onHitEntity` |
| ServerPlayerMixin | ServerPlayer | Max health management | `tick` HEAD |
| RecipeManagerMixin | RecipeManager | Shield recipe removal | `prepare` RETURN |
| FoodDataMixin | FoodData | Halve natural regen | `tick` ModifyArg |
| SnowballItemMixin | Items | Snowball stack size | `<clinit>` TAIL |
| AbstractVillagerMixin | AbstractVillager | Remove shield/bell trades | `getOffers` RETURN |
| BellBlockMixin | BellBlock | Land plot drops | (assumed) |

**New mixins in bold.** Total: 4 new mixins for v1.1 features.

## Build Order

Dependencies between features determine implementation order:

### Phase 1: Drowning Modification
**Why first:**
- Standalone feature, no dependencies
- Simplest mixin (single injection point)
- Quick win, easy to verify

**Depends on:** Nothing

### Phase 2: Spear Removal
**Why second:**
- Mostly data files (JSON)
- One simple mixin
- Can use existing LootTableEvents pattern from shield removal

**Depends on:** Nothing (but benefits from Phase 1 pattern establishment)

### Phase 3: Projectile Physics
**Why third:**
- More complex mixin work
- Need to track projectile state (distance traveled)
- Foundation for Phase 4

**Depends on:** Nothing directly, but establishing tick tracking enables Phase 4

### Phase 4: Projectile Aggro/Effects
**Why last:**
- Depends on projectile hit detection
- Most complex logic (mob targeting, radius search)
- Builds on Phase 3 mixin infrastructure

**Depends on:** Phase 3 mixin exists (can share file)

**Alternative order:** Phases 3 and 4 could be combined into single implementation since they share the same mixin file and are logically coupled.

## Shared Infrastructure

### No new infrastructure required

The existing codebase provides all needed patterns:
- Mixin injection patterns (HEAD, TAIL, ModifyVariable, ModifyArg)
- Effect application pattern (`MobEffectInstance`)
- Event registration pattern (`THC.kt` initializer)
- Loot table modification pattern (`LootTableEvents.MODIFY_DROPS`)

### Potential shared utilities (optional)

If projectile system expands in future:

```kotlin
// thc/projectile/ProjectileUtils.kt
object ProjectileUtils {
    fun applyHitEffects(target: LivingEntity, shooter: Entity?)
    fun redirectAggro(target: LivingEntity, shooter: Entity?, radius: Double)
}
```

For v1.1 scope, inline implementation in mixin is sufficient.

## Data Files Required

| File | Type | Purpose |
|------|------|---------|
| `data/minecraft/loot_table/entities/drowned.json` | Override | Remove trident drops |

**Note:** Trident has no vanilla crafting recipe in Java Edition, so no recipe override needed.

## Configuration Touchpoints

Files requiring modification:

1. **thc.mixins.json** - Add new mixin classes:
   ```json
   "mixins": [
     ...,
     "LivingEntityDrowningMixin",
     "DrownedMixin",
     "AbstractArrowMixin"
   ]
   ```

2. **THC.kt** - Register SpearRemoval if using event handler approach

## Testing Strategy

| Feature | Test Type | Method |
|---------|-----------|--------|
| Drowning | Game test | Spawn player underwater, verify damage timing |
| Spear removal | Game test + manual | Spawn Drowned, verify no trident |
| Projectile physics | Manual | Fire arrow, observe arc change |
| Projectile aggro | Game test | Hit mob with arrow, verify effects + targeting |

Game tests recommended for Drowned spawn and projectile effects (deterministic). Physics may require manual verification due to visual nature.

## Risk Assessment

| Feature | Risk | Mitigation |
|---------|------|------------|
| Drowning mixin | LOW | Simple injection, well-understood |
| Drowned equipment | MEDIUM | Need to find exact spawn method in 1.21 |
| Projectile physics | MEDIUM | Gravity calculation needs tuning |
| Projectile aggro | LOW | Existing patterns from LivingEntityMixin |

**Drowned equipment risk:** The exact method name may have changed between Minecraft versions. Need to verify against 1.21.11 source via IDE.

## Sources

### Primary (HIGH confidence)
- Existing THC codebase patterns (verified by reading source)
- [LivingEntity NeoForge Javadocs](https://lexxie.dev/neoforge/1.21.1/net/minecraft/world/entity/LivingEntity.html) - drowning methods
- [Minecraft Wiki - Drowned](https://minecraft.wiki/w/Drowned) - spawn rates, equipment chances

### Secondary (MEDIUM confidence)
- [Fabric Wiki - Mixin Introduction](https://wiki.fabricmc.net/tutorial:mixin_introduction) - general patterns
- [Fabric Wiki - Mixin Examples](https://fabricmc.net/wiki/tutorial:mixin_examples)
- [Modding Tutorials - Projectiles](https://moddingtutorials.org/1.19.2/arrows/)

### Tertiary (LOW confidence, need validation)
- Exact method names for Drowned spawn equipment (verify in IDE)
- AbstractArrow tick method structure (verify in IDE)

## Metadata

**Confidence breakdown:**
- Drowning approach: HIGH - well-documented LivingEntity methods
- Spear removal: HIGH - uses existing loot table + mixin patterns
- Projectile physics: MEDIUM - needs implementation verification
- Aggro/effects: HIGH - similar to existing buckler stun code

**Research date:** 2026-01-18
**Valid until:** Until Minecraft version change or major Fabric API update
