# Pitfalls Research: v1.1 Mechanics Modification

**Researched:** 2026-01-18
**Milestone:** v1.1 Extra Features Batch 1
**Confidence:** MEDIUM (verified with official docs and community sources)

## Summary

This document catalogs common mistakes when implementing the v1.1 features:
- Drowning damage tick rate modification
- Spear/item removal from game systems
- Projectile velocity/gravity physics changes
- Mob aggro redirection on projectile hit
- Effect application on projectile impact

The project already has working patterns for mixins (LivingEntityMixin), recipe removal (RecipeManagerMixin), effect application (MiningFatigue.kt), and loot table modification. These pitfalls build on that experience.

---

## Drowning Modification Pitfalls

### DROWN-01: Tick Counter State Persistence

**Risk:** Drowning uses an `airSupply` counter that decrements each tick underwater. Modifying damage tick rate without understanding the counter leads to inconsistent behavior across respawn, dimension change, or logout.

**Warning signs:**
- Drowning damage still happens at original rate after respawn
- Air supply resets unexpectedly
- Different behavior in multiplayer vs singleplayer

**Prevention:**
- Hook into `LivingEntity.baseTick()` where airSupply is decremented
- Use `@ModifyVariable` or `@Redirect` on the damage interval check, not the counter itself
- Test respawn and dimension changes explicitly

**Phase:** Drowning damage phase - implement with explicit respawn test case

---

### DROWN-02: Water Breathing Interaction

**Risk:** Water Breathing effect prevents drowning entirely. If you modify drowning logic with a mixin, the interaction with this effect may break or bypass your changes.

**Warning signs:**
- Water Breathing no longer works
- Players still take modified drowning damage with Water Breathing
- Effect duration/amplifier affects damage rate unexpectedly

**Prevention:**
- Check `hasEffect(MobEffects.WATER_BREATHING)` early in your injection
- Study vanilla's condition order: effect check happens before air decrement
- Respiration enchantment also affects air, test helmet interactions

**Phase:** Drowning damage phase - test with Water Breathing potion

---

### DROWN-03: Damage Source Type Matters

**Risk:** Drowning damage uses `DamageTypes.DROWN` which bypasses armor (via damage type tags). Changing how/when damage is applied may accidentally bypass this or apply different damage type.

**Warning signs:**
- Drowning damage is now reduced by armor
- Drowning damage kills differently than vanilla
- Death messages are wrong

**Prevention:**
- Use `level.damageSources().drown()` to get proper damage source
- Don't create custom damage sources for drowning modifications
- Verify death messages in testing

**Phase:** Drowning damage phase - verify death message unchanged

---

### DROWN-04: Client-Server Desync on Air Display

**Risk:** Air supply affects the HUD bubble display on client. If server modifies drowning rate but client isn't aware, bubbles display incorrectly.

**Warning signs:**
- Bubbles disappear but damage doesn't happen
- Bubbles show full air but player takes damage
- Bubbles flicker or jump

**Prevention:**
- Modifications to airSupply must happen on both client and server
- Use `@Inject` at points that naturally sync (damage events auto-sync)
- If modifying only server, ensure display reads actual airSupply value

**Phase:** Drowning damage phase - visual verification in client

---

## Spear Removal Pitfalls

### SPEAR-01: Incomplete Removal Sources

**Risk:** Items can enter the game through multiple channels. Removing from one but not others leaves items obtainable.

**Sources to cover:**
1. Crafting recipes (RecipeManager)
2. Mob spawn equipment (Drowned naturally spawn with tridents)
3. Loot tables (fishing, ocean monuments, buried treasure)
4. Villager trades
5. Creative menu (for complete removal)
6. Existing world items (optional - despawn/delete)

**Warning signs:**
- Players still obtain item through unexpected source
- Item appears in creative tabs despite "removal"
- Mob drops still include item

**Prevention:**
- Enumerate ALL sources before implementation
- Use existing patterns: `RecipeManagerMixin` for recipes, `AbstractVillagerMixin` for trades
- Use `LootTableEvents.MODIFY` (v3 API) to filter loot pools
- Mob equipment requires spawn event interception

**Phase:** Spear removal phase - verify each source independently

---

### SPEAR-02: Mob Equipment Spawn Timing

**Risk:** Drowned spawn with tridents. Equipment is assigned during mob spawn finalization. Intercepting too early or too late misses equipment assignment.

**Warning signs:**
- Some drowned still spawn with tridents
- Equipment removal works in game tests but not natural spawns
- Different behavior in different biomes

**Prevention:**
- Hook `Mob.finalizeSpawn()` or `ENTITY_LOAD` events
- Drowned specifically: check both `MAINHAND` and `OFFHAND` slots
- Test natural ocean spawns, not just `/summon` commands

**Phase:** Spear removal phase - test natural ocean spawns

---

### SPEAR-03: Loot Table Source Filtering

**Risk:** `LootTableEvents.MODIFY` fires for all loot tables including datapack overrides. Modifying user-provided tables can cause unexpected behavior.

**Warning signs:**
- Datapack loot tables also modified
- Mod compatibility issues
- Server admins can't customize loot

**Prevention:**
- Use `source.isBuiltin()` check to only modify vanilla tables
- Document which tables are modified for compatibility
- Example pattern:
  ```java
  LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
      if (!source.isBuiltin()) return;
      // Only modify vanilla tables
  });
  ```

**Phase:** Spear removal phase - implement source filtering

---

### SPEAR-04: Existing Items in World

**Risk:** Items already in world (chests, player inventories, item frames) persist after recipe/loot removal. Players can still use/trade them.

**Warning signs:**
- Players in existing worlds still have item
- Item duplication via death/inventory manipulation
- Item persistence across mod update

**Prevention:**
- Decide policy: allow existing vs. delete existing
- If allowing: document that existing items are grandfathered
- If deleting: use `ServerTickEvents` to scan and remove (invasive)
- For soft removal: disable item functionality instead of existence

**Phase:** Spear removal phase - document policy decision

---

## Projectile Physics Pitfalls

### PROJ-01: Physics Calculation Order Changed in 1.21.2+

**Risk:** Minecraft 1.21.2 changed throwable projectile physics order from "Position, Drag, Acceleration" to "Acceleration, Drag, Position". Code written for old order produces different trajectories.

**Warning signs:**
- Projectiles don't land where expected
- Different behavior on version update
- Calculations that worked in testing fail in production

**Prevention:**
- Target 1.21.11 specifically (current project version)
- Don't copy physics code from older tutorials
- Test at long range (32+ blocks) where small errors compound

**Phase:** Projectile physics phase - long-range trajectory tests

---

### PROJ-02: Velocity vs Delta Movement Confusion

**Risk:** Minecraft uses `deltaMovement` (velocity per tick) not "velocity" in physics sense. Multiplying by wrong factors produces extreme speeds.

**Warning signs:**
- Projectiles move too fast or too slow
- Damage becomes extreme (velocity affects arrow damage)
- Projectiles clip through blocks at high speed

**Prevention:**
- 20% velocity boost = multiply deltaMovement by 1.2
- deltaMovement is already per-tick, don't multiply by tick rate
- Arrow damage scales with speed - cap damage if needed

**Phase:** Projectile physics phase - verify damage at new velocity

---

### PROJ-03: Client-Server Motion Desync

**Risk:** Projectile motion predicted on client, authoritative on server. Modifying only one side causes visual desync.

**Warning signs:**
- Projectile visually goes one place, hits another
- Rubber-banding projectiles
- Different behavior in singleplayer vs multiplayer

**Prevention:**
- Modify both client and server projectile logic
- Or only modify server and let client prediction catch up
- Test in actual multiplayer, not just integrated server

**Phase:** Projectile physics phase - multiplayer test

---

### PROJ-04: Quadratic Gravity Implementation

**Risk:** "Quadratic gravity after 8 blocks" requires tracking projectile travel distance. Each projectile needs state to track distance traveled.

**Warning signs:**
- Gravity applies inconsistently
- First projectile works, subsequent don't
- Memory leak from uncleared state

**Prevention:**
- Use persistent data on projectile entity (NBT or entity field)
- Calculate distance from spawn position, not per-tick accumulation
- Clean up state when projectile removed
- Consider: is "8 blocks traveled" or "8 blocks from shooter" intended?

**Phase:** Projectile physics phase - clarify distance definition first

---

### PROJ-05: Drag and Gravity Interaction

**Risk:** Projectiles have both gravity (downward acceleration) and drag (velocity multiplier ~0.99/tick). Changing gravity without accounting for drag produces unexpected arcs.

**Warning signs:**
- Projectiles curve strangely
- Terminal velocity reached too quickly
- Projectiles float instead of fall

**Prevention:**
- Understand vanilla physics: gravity = 0.05 blocks/tick^2, drag = 0.99/tick
- Quadratic gravity increases the 0.05 factor, doesn't change drag
- Test at various angles and distances

**Phase:** Projectile physics phase - trajectory validation

---

### PROJ-06: Different Projectile Classes

**Risk:** `AbstractArrow` (arrows, tridents) and `ThrowableProjectile` (snowballs, eggs) have different physics and collision detection.

**Warning signs:**
- Changes work for arrows but not snowballs (or vice versa)
- Collision detection differs
- Some projectiles use raycast, others use hitbox inflation

**Prevention:**
- Identify which projectiles need modification (player projectiles = both types?)
- ThrowableProjectile inflates entity hitboxes by 0.3 blocks each direction
- AbstractArrow uses raycast for entity collision
- May need separate mixins for each class hierarchy

**Phase:** Projectile physics phase - enumerate projectile types first

---

## Aggro System Pitfalls

### AGGRO-01: Goal-Based vs Brain-Based AI

**Risk:** Different mobs use different AI systems. Zombies/skeletons use Goal-based AI, Piglins/Wardens use Brain-based AI. Changing target in one system doesn't affect the other.

**Warning signs:**
- Works on zombies but not piglins
- Works on some mobs in same category but not others
- Aggro appears to work but mob doesn't actually path toward target

**Prevention:**
- Goal-based: modify `mob.setTarget()`
- Brain-based: modify `MemoryModuleType.ATTACK_TARGET`
- Test on both system types
- Some mobs (Goats) have custom behavior mixins

**Phase:** Aggro system phase - test zombie (goal) and piglin (brain)

---

### AGGRO-02: Target Validation After Set

**Risk:** After `setTarget()`, mob's targeting goals validate the target. Invalid targets (wrong entity type, too far, can't see) get cleared immediately.

**Warning signs:**
- Target set but mob doesn't attack
- Mob briefly looks at target then returns to player
- Works at close range, fails at long range

**Prevention:**
- Ensure target is valid: right entity type, in range, has line of sight
- Check `NearestAttackableTargetGoal` conditions for the mob type
- May need to temporarily modify goal's target predicate

**Phase:** Aggro system phase - verify target persistence

---

### AGGRO-03: lastHurtBy vs Target

**Risk:** Mobs track both `target` (what they're attacking) and `lastHurtBy` (what hurt them). HurtByTargetGoal uses lastHurtBy to set target. Both need to be set for consistent behavior.

**Warning signs:**
- Mob attacks wrong entity
- Mob switches between targets erratically
- Revenge mechanics don't trigger on aggro target

**Prevention:**
- Set both: `mob.setTarget(newTarget)` and `mob.setLastHurtByMob(newTarget)`
- Or use damage event that naturally sets lastHurtBy
- Test that mob remembers target after being hit

**Phase:** Aggro system phase - verify revenge consistency

---

### AGGRO-04: Glowing Effect Visibility

**Risk:** Glowing effect (Speed II + Glowing 6s) requires proper client sync. If effect applied server-only, visual glowing may not appear.

**Warning signs:**
- Effect applied but mob doesn't glow
- Glowing visible to some players but not others
- Effect duration seems wrong

**Prevention:**
- `addEffect()` handles sync automatically on LivingEntity
- Test in multiplayer that all players see glow
- Glowing effect uses separate outline rendering - may have shader compat issues

**Phase:** Aggro system phase - multiplayer visual test

---

### AGGRO-05: Owner/Shooter Resolution

**Risk:** Projectile's "owner" (shooter) must be resolved correctly to redirect aggro. Owner can be null, dead, or no longer in world.

**Warning signs:**
- NullPointerException on hit
- Wrong entity gets aggro
- Works for arrows but not snowballs

**Prevention:**
- Always null-check `projectile.getOwner()`
- Verify owner is still alive and in same dimension
- ThrowableProjectile and AbstractArrow both have `getOwner()` but from different parent

**Phase:** Aggro system phase - null safety checks

---

## Effect Application Pitfalls

### EFFECT-01: onHit vs onEntityHit

**Risk:** Projectile collision has multiple hooks: `onHit(HitResult)`, `onEntityHit(EntityHitResult)`, `onBlockHit(BlockHitResult)`. Using wrong one misses cases.

**Warning signs:**
- Effect applies when hitting ground but not entities
- Effect applies to entities but crashes on block hit
- Double application (hit block behind entity)

**Prevention:**
- `onEntityHit` for entity-specific effects
- Check `hitResult` type before casting
- Verify not hitting block behind entity (arrow pass-through)

**Phase:** Effect application phase - test both entity and block hits

---

### EFFECT-02: Self-Application

**Risk:** When applying effects on projectile hit, careless code can apply effect to shooter instead of target.

**Warning signs:**
- Shooter gets Speed II instead of target
- Effects apply to both shooter and target
- Effect applies on throw, not hit

**Prevention:**
- `EntityHitResult.getEntity()` is the HIT entity (target)
- `projectile.getOwner()` is the shooter
- Double-check parameter order in `addEffect()` calls

**Phase:** Effect application phase - verify target identity

---

### EFFECT-03: Effect Duration Mismatch

**Risk:** MobEffectInstance duration is in ticks, not seconds. 6 seconds = 120 ticks, not 6.

**Warning signs:**
- Effect lasts too short or too long
- Effect seems instant (1 tick duration)
- Duration doesn't match design spec

**Prevention:**
- Always calculate: `seconds * 20 = ticks`
- 6 seconds = 120 ticks
- Speed II + Glowing 6s: `new MobEffectInstance(MobEffects.SPEED, 120, 1)` (amplifier 1 = level II)

**Phase:** Effect application phase - verify duration

---

### EFFECT-04: Amplifier Off-By-One

**Risk:** MobEffect amplifier 0 = Level I, amplifier 1 = Level II. Speed II requires amplifier 1, not 2.

**Warning signs:**
- Speed III instead of Speed II
- Effect too weak (Level I instead of II)
- Confusion in code review

**Prevention:**
- Comment amplifier meaning: `// amplifier 1 = Speed II`
- Speed II: `new MobEffectInstance(MobEffects.SPEED, ticks, 1)`
- Test effect icon shows correct level in inventory

**Phase:** Effect application phase - verify effect level

---

## General Integration Pitfalls

### INT-01: Mixin Injection Point Version Specificity

**Risk:** Mixin injection points like `@At(value="INVOKE", target="...")` reference specific method signatures that change between Minecraft versions.

**Warning signs:**
- Mixin fails to apply after Minecraft update
- "defaultRequire" violations in logs
- Runtime crash vs compile success

**Prevention:**
- Project targets 1.21.11 specifically
- Document exact method signatures in comments
- Run full test suite after any Minecraft version bump
- Note: Minecraft 26.1 will be unobfuscated - mappings strategy changes

**Phase:** All phases - verify against 1.21.11 specifically

---

### INT-02: Accessor Mixin Necessity

**Risk:** Some fields/methods are private and immutable. Direct field access fails; accessor mixins required.

**Warning signs:**
- Compile error accessing private field
- "cannot assign to final field" errors
- Changes don't persist

**Prevention:**
- Project already uses `access.ItemAccessor` pattern
- Check field visibility before writing mixin
- Use `@Accessor` for getters, `@Invoker` for methods
- Kotlin's backtick syntax for reserved words: `` `is`(BlockTags.COAL_ORES) ``

**Phase:** All phases - identify private fields early

---

### INT-03: Event Registration Order

**Risk:** Fabric events fire in registration order. Late registration may miss events or conflict with other handlers.

**Prevention:**
- Register in mod initializer, not lazily
- Document event dependencies
- Test with other mods installed

**Phase:** All phases - centralized registration in initializer

---

### INT-04: Test Infrastructure Gaps

**Risk:** Game tests can't cover all scenarios. Visual effects, multiplayer sync, and natural spawns need manual testing.

**Warning signs:**
- Tests pass but feature broken in gameplay
- Works in dev, fails in production
- Multiplayer-only bugs

**Prevention:**
- Game tests for logic (damage calculations, effect application)
- Manual tests for: visuals, multiplayer sync, natural world generation
- Document manual test procedures

**Phase:** All phases - maintain test coverage plan

---

## Critical Path

Pitfalls that are most likely to cause rework, ordered by risk:

1. **PROJ-04: Quadratic Gravity Implementation** - Requires state tracking design decision before coding
2. **SPEAR-01: Incomplete Removal Sources** - Must enumerate ALL sources first
3. **AGGRO-01: Goal-Based vs Brain-Based AI** - Different mobs need different approaches
4. **PROJ-06: Different Projectile Classes** - May need multiple implementations
5. **EFFECT-02: Self-Application** - Easy to get wrong, hard to notice in testing

**Recommended implementation order:**
1. Drowning (isolated, well-defined injection points)
2. Spear removal (build on existing removal patterns)
3. Effect application (simpler projectile hook)
4. Aggro redirection (depends on effect working)
5. Projectile physics (most complex, needs design clarification)

---

## Sources

### Primary (HIGH confidence)
- [Fabric Wiki - Mixin Injects](https://wiki.fabricmc.net/tutorial:mixin_injects) - Injection point documentation
- [Fabric API LootTableEvents](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/loot/v2/LootTableEvents.html) - Loot table modification API
- [Minecraft Wiki - Projectile Motion](https://minecraft.wiki/w/Calculators/Projectile_motion) - Physics values
- [Fabric Documentation - Events](https://docs.fabricmc.net/develop/events) - Event registration patterns

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Mob AI](https://minecraft.wiki/w/Mob_AI) - Goal vs brain AI systems
- [Minecraft Wiki - Drowned](https://minecraft.wiki/w/Drowned) - Spawn equipment mechanics
- [Forge Javadocs - LivingEntity](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/world/entity/LivingEntity.html) - Entity methods

### Tertiary (LOW confidence - needs validation)
- Minecraft Forum discussions on projectile physics
- MCreator tutorials on effect application
- Community mod compatibility observations

---

## Metadata

**Phase mapping:**
| Pitfall Group | Phase |
|---------------|-------|
| DROWN-* | Drowning damage modification |
| SPEAR-* | Spear removal |
| PROJ-* | Projectile physics overhaul |
| AGGRO-* | Aggro redirection system |
| EFFECT-* | Effect application |
| INT-* | All phases |

**Research date:** 2026-01-18
**Valid until:** 2026-02-18 (30 days - stable domain)
