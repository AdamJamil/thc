# Domain Pitfalls: v2.3 Monster Overhaul

**Researched:** 2026-01-23
**Milestone:** v2.3 Monster Overhaul
**Confidence:** MEDIUM-HIGH (mix of verified Minecraft mechanics and Fabric-specific patterns from existing codebase)

## Summary

This document catalogs common mistakes when implementing the v2.3 monster overhaul features:
- Global mob speed modifications (20% faster, with exclusions)
- Spawn table replacements (Zombie->Husk, Skeleton->Stray)
- Mob behavior modifications (Ghast, Enderman, Vex)
- Equipment spawn timing (Pillager variants)
- Regional spawn system with cap partitioning
- NBT spawn origin tagging

The project has working patterns for spawn modification (NaturalSpawnerMixin), mob attribute modification (ServerPlayerMixin for health), and entity tagging (THCAttachments). v2.3 extends these patterns to mob-specific modifications.

---

## Critical Pitfalls

Mistakes that cause rewrites, server crashes, or major gameplay breaks.

### MOB-01: Global Speed Modifier Breaks Baby Zombies

**Risk:** Applying a flat 20% speed increase to all mobs makes baby zombies absurdly fast. Baby zombies already have a 1.5x speed multiplier in vanilla (base 0.23 * 1.5 = 0.345 vs adult 0.23). A naive 20% boost compounds this to effectively 1.8x adult speed, making them nearly impossible to hit.

**Why it happens:** Baby zombie speed is handled via attribute modifier in `ZombieEntity.isBaby()` which applies `BABY_SPEED_BONUS` modifier. If you apply your own modifier on top via `finalizeSpawn` or entity creation, both stack multiplicatively.

**Warning signs:**
- Baby zombies outrun sprinting players
- Combat balance destroyed for early game
- Players report "impossible" difficulty

**Prevention:**
```java
// In your speed modifier mixin/event handler:
if (mob instanceof Zombie zombie && zombie.isBaby()) {
    return; // Skip baby zombies entirely - they're already fast
}
// Or apply reduced multiplier:
double multiplier = (mob instanceof Zombie z && z.isBaby()) ? 1.0 : 1.2;
```

**Detection:** Test with `/summon zombie ~ ~ ~ {IsBaby:1}` and measure movement speed. Compare to adult zombie. Baby should be ~1.5x adult, not ~1.8x.

**Phase to address:** Phase 1 (Speed modifications) - add explicit baby zombie exclusion check

**Source:** [Minecraft Wiki - Attribute](https://minecraft.wiki/w/Attribute) - modifier stacking behavior

---

### MOB-02: Speed Modifier Affects Bosses

**Risk:** The Wither and Ender Dragon have hardcoded behaviors that assume specific movement speeds. Modifying their movement attribute can cause:
- Wither charge attack desyncs from animation
- Ender Dragon flight path calculations break (doesn't use standard movement attributes)
- Boss fights become trivial or impossible
- Server performance issues from broken pathfinding loops

**Why it happens:** Boss mobs extend `Mob` but have extensive hardcoded behavior in their AI goals that don't dynamically respond to attribute changes. The Ender Dragon specifically doesn't even use standard movement attributes for its flight - it calculates positions mathematically.

**Warning signs:**
- Wither charge animation doesn't match actual movement
- Dragon flight paths become erratic
- Boss fights fail to progress phases correctly

**Prevention:**
```java
// Always exclude boss mobs from attribute modifications:
if (mob instanceof EnderDragon || mob instanceof Wither) {
    return; // Never modify boss attributes
}
// Also consider Elder Guardian (mini-boss with specific attack timing):
if (mob instanceof ElderGuardian) {
    return;
}
```

**Detection:** Spawn Wither in test world, observe charge attack timing. Dragon is harder to test but verify crystal-targeting flight patterns still work.

**Phase to address:** Phase 1 (Speed modifications) - add boss entity type exclusion list

**Source:** [Minecraft Wiki - Boss](https://minecraft.wiki/w/Boss) - boss immunity mechanics

---

### MOB-03: Zombie to Husk Replacement Loses Drowned Conversion

**Risk:** If you replace ALL zombie spawns with husks, players lose the quick drowned conversion mechanic. The conversion chain is: Husk -> Zombie (30s) -> Drowned (30s more). Direct zombie-to-drowned is 45 seconds total. Husks require 90+ seconds.

**Why it happens:** Vanilla zombies that spawn near water can convert to drowned in 45 seconds. Husks must first convert to regular zombies, effectively doubling conversion time. This significantly reduces drowned population in ocean biomes.

**Consequences:**
- Drowned spawns reduced dramatically in underwater areas
- Trident acquisition becomes harder (drowned are primary trident source)
- Ocean biome difficulty reduced unintentionally

**Prevention:**
For THC, this may actually be DESIRED - reducing trident availability fits the ranged weapon gating design. **Document as intentional side effect.** If NOT desired:
```java
// Only replace surface spawns, preserve underwater zombie spawns:
if (spawnPos.getY() < level.getSeaLevel() - 5 && biome.isOcean()) {
    return EntityType.ZOMBIE; // Allow zombies underwater for drowned conversion
}
return EntityType.HUSK;
```

**Detection:** Monitor drowned population in ocean biomes over extended gameplay. Check trident drop rates in ocean monuments and underwater exploration.

**Phase to address:** Phase 2 (Spawn replacement) - document as intentional OR add biome/depth exception

**Source:** [Minecraft Wiki - Drowned](https://minecraft.wiki/w/Drowned), [Minecraft Wiki - Husk](https://minecraft.wiki/w/Husk)

---

### MOB-04: Enderman Teleport-Behind Creates Infinite Loop

**Risk:** When implementing "teleport behind player after taking damage," naive implementations can cause infinite loops if:
- The teleport destination is invalid (inside blocks)
- The enderman takes damage from the teleport itself (fall damage, suffocation)
- Multiple rapid damage events trigger multiple teleports per tick
- No valid position exists behind player (player against wall)

**Why it happens:** Enderman teleportation uses randomized position finding with retry logic. If you force a specific target position that's invalid, the vanilla retry loop can spin indefinitely. If the enderman teleports into a block and takes suffocation damage, this triggers another teleport, creating an infinite loop.

**Warning signs:**
- Server TPS drops when enderman takes damage
- Endermen stuck inside walls or underground
- Endermen teleporting rapidly back and forth
- Potential server freeze with many aggro'd endermen

**Prevention:**
```java
// 1. Limit teleport attempts with finite retry
int maxAttempts = 10;
boolean success = false;
for (int i = 0; i < maxAttempts && !success; i++) {
    Vec3 targetPos = calculateBehindPlayerPosition(player, offset: i * 0.5);
    success = enderman.teleportTo(targetPos.x, targetPos.y, targetPos.z);
}

// 2. Add cooldown to prevent rapid re-triggers
private static final UUID TELEPORT_COOLDOWN = UUID.fromString("...");
long lastTeleport = enderman.getAttachedOrDefault(THCAttachments.LAST_TELEPORT, 0L);
if (currentTick - lastTeleport < 40) { // 2 second cooldown
    return; // Skip if teleported recently
}

// 3. Validate destination before any attempt
BlockPos targetBlock = new BlockPos(targetPos);
if (!level.getBlockState(targetBlock).isAir() ||
    !level.getBlockState(targetBlock.above()).isAir()) {
    return; // Don't attempt if destination is blocked
}
```

**Detection:** Spawn 20+ endermen, aggro all of them, back against a wall. Watch for TPS drops or stuck entities with F3 debug.

**Phase to address:** Phase 3 (Enderman behavior) - implement with retry limit, cooldown, and position validation

**Source:** [Minecraft Forum - Enderman Teleporting](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/modification-development/1433221-enderman-teleporting-question) - community warnings about infinite loops

---

### MOB-05: Ghast Fireball Speed Change Causes Client Desync

**Risk:** Modifying projectile velocity server-side without proper sync causes the client to predict fireball position incorrectly. Players see fireballs in wrong locations, making deflection timing luck-based or impossible.

**Why it happens:** Minecraft uses client-side prediction for projectiles. The client calculates expected position based on initial velocity packet. If server modifies velocity AFTER initial spawn sync, client prediction diverges from server reality.

**Warning signs:**
- Fireballs appear to teleport or rubber-band
- Deflection timing becomes luck-based (visual doesn't match hitbox)
- Players report "broken" ghast fights, fireballs hitting from wrong direction
- F3+B hitbox display shows misalignment with visual

**Prevention:**
```java
// Option 1: Modify velocity at spawn time BEFORE first client sync
// Inject into Ghast shooting logic:
@Inject(
    method = "performRangedAttack",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
        shift = At.Shift.BEFORE
    )
)
private void modifyFireballSpeed(...) {
    // fireball velocity is set BEFORE addFreshEntity, so modify here
    Vec3 motion = fireball.getDeltaMovement();
    fireball.setDeltaMovement(motion.scale(1.5)); // 50% faster
}

// Option 2: Override at Ghast AI goal level (cleanest approach)
// Mixin to Ghast$GhastShootFireballGoal and modify the velocity
// parameters passed to LargeFireball constructor
```

**Detection:** Fight ghasts, use F3+B to show hitboxes. Verify fireball visual position matches hitbox. Try to deflect - should feel responsive, not random.

**Phase to address:** Phase 3 (Ghast projectile modifications) - modify velocity at spawn time, not after

**Source:** [Technical Minecraft Wiki - Desync](https://technical-minecraft.fandom.com/wiki/Client/server_desynchronization)

---

### MOB-06: Equipment Spawn Timing vs Loot Table Conflict

**Risk:** If you set equipment on mobs at spawn time via mixin, but vanilla logic ALSO sets equipment (for zombies, skeletons, pillagers), you get:
- Equipment set twice (wasted computation)
- Vanilla equipment overwrites your custom equipment
- Drop table includes wrong items
- Visual/mechanical mismatch (mob holds one thing, drops another)

**Why it happens:** Minecraft's mob equipment flow in `finalizeSpawn()`:
1. `populateDefaultEquipmentSlots()` called - vanilla sets equipment based on difficulty
2. Equipment chances set
3. Additional spawn logic runs

If you inject equipment BEFORE `populateDefaultEquipmentSlots`, vanilla will overwrite it. If you inject equipment AFTER the method finishes, the mob may have already synced wrong equipment to clients.

**Warning signs:**
- Mobs spawn with wrong equipment
- Equipment flickers on spawn
- Loot drops don't match visually-held items
- Pillager variants don't have correct weapons

**Prevention:**
```java
// Inject AFTER populateDefaultEquipmentSlots completes:
@Inject(
    method = "finalizeSpawn",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/monster/Monster;populateDefaultEquipmentSlots(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/DifficultyInstance;)V",
        shift = At.Shift.AFTER
    )
)
private void setCustomEquipment(ServerLevelAccessor level, DifficultyInstance difficulty,
                                 SpawnReason reason, SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
    // Clear existing mainhand, set our equipment
    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    this.setItemSlot(EquipmentSlot.MAINHAND, createPillagerWeapon());

    // IMPORTANT: Set drop chance to 0 if you don't want it to drop
    this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
}
```

**Detection:** Kill 100 mobs of modified type, verify drop table exactly matches equipped items. Check for duplicate or unexpected items. Verify visual equipment matches at spawn time.

**Phase to address:** Phase 4 (Pillager variants) - inject at correct point in finalizeSpawn, manage drop chances explicitly

**Source:** [Fabric Wiki - Adding Items to Vanilla Mob Loot](https://wiki.fabricmc.net/tutorial:adding_to_loot_tables)

---

## Moderate Pitfalls

Mistakes that cause technical debt, balance issues, or multiplayer bugs.

### MOB-07: NBT Spawn Origin Tag Bloats Chunk Saves

**Risk:** Adding NBT tags to EVERY spawned mob for spawn origin tracking accumulates in chunk save files. With high mob caps and frequent spawning, this can:
- Increase world save sizes significantly (20+ bytes per mob)
- Slow chunk load/save operations
- Risk "NBT tag too big" errors in extreme cases

**Why it happens:** Each mob entity is serialized to NBT when chunk saves. Custom persistent tags are included. With thousands of mobs across loaded chunks, even small tags add up. A long string like "surface_overworld_y64" is ~25 bytes per mob.

**Warning signs:**
- World file grows faster than expected
- Chunk loading/saving causes brief stutters
- Level.dat or region files grow unusually large

**Prevention:**
```java
// Option 1: Use minimal encoding - bytes instead of strings
// Instead of: "spawn_origin" -> "surface_overworld_y64"
// Use: "so" -> (byte)0  (0=surface, 1=upper_cave, 2=lower_cave)
AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "spawn_origin"),
    builder -> {
        builder.initializer(() -> (byte) -1);
        builder.persistent(Codec.BYTE);  // Only 1 byte per mob
    }
);

// Option 2: Don't persist - use transient attachment if only needed at runtime
AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "spawn_origin"),
    builder -> builder.initializer(() -> (byte) 0)
    // NOTE: No .persistent() - data lost on chunk unload, but no save impact
);

// Option 3: Only tag mobs that need it (e.g., for special loot)
// Don't tag every mob if tag is only for debugging
```

**Detection:** Create test world, spawn 10,000 mobs with NBT tags, force save, check file size vs vanilla. Use NBTExplorer to audit mob entities in region files.

**Phase to address:** Phase 6 (NBT tagging) - use byte encoding, evaluate if persistence is truly needed for gameplay

**Source:** [Fabric Wiki - Persistent State](https://wiki.fabricmc.net/tutorial:persistent_states)

---

### MOB-08: Regional Cap Partitioning Causes Underspawning

**Risk:** Splitting the hostile mob cap (70) into regions (Surface 30%, Upper Cave 40%, Lower Cave 50% = 120% total) seems like it creates more capacity, but naive implementation causes underspawning because:
- Mobs can wander between regions after spawn
- Chunk boundary checks fail at region transitions
- Global cap still applies, making regional "caps" meaningless

**Why it happens:** Minecraft's spawn system counts mobs globally across loaded chunks, then attempts spawns per-chunk. If you enforce "surface can only have 21 mobs" but count mobs by current Y position (not spawn position), a cave mob wandering to surface throws off your count. Or regional checks at Y boundaries create "dead zones" where neither region allows spawns.

**Warning signs:**
- Some regions feel empty compared to others
- Mob farms in "wrong" region produce nothing
- Inconsistent difficulty across Y-levels
- Transition zones (Y=32, Y=-32) have no spawns

**Prevention:**
```java
// Approach 1: Don't partition cap, partition spawn ATTEMPTS
// Let vanilla mob cap work normally, but weight WHERE spawns happen
@Inject(method = "spawnForChunk", ...)
private void filterSpawnByRegion(ServerLevel level, ChunkPos chunk, ...) {
    // Determine spawn Y-level based on regional weights, not hard caps
    int targetY = selectSpawnY(chunk,
        surfaceWeight: 0.30,
        upperCaveWeight: 0.40,
        lowerCaveWeight: 0.50  // Higher weight = more spawns in that region
    );
}

// Approach 2: Track mobs by SPAWN region tag, not current position
// Use NBT tag set at spawn time (MOB-07), count by tag not by Y
Map<SpawnRegion, Integer> regionCounts = new HashMap<>();
for (Mob mob : loadedMobs) {
    SpawnRegion region = mob.getAttached(THCAttachments.SPAWN_REGION);
    regionCounts.merge(region, 1, Integer::sum);
}
```

**Detection:** AFK at Y=64 (surface) for 10 minutes, count mob spawns. Repeat at Y=0 (upper cave) and Y=-40 (lower cave). Compare to expected regional ratios.

**Phase to address:** Phase 5 (Regional spawning) - partition spawn weights not caps; OR use tag-based counting

**Source:** [Technical Minecraft Wiki - Mob Caps](https://techmcdocs.github.io/pages/GameMechanics/MobCap/)

---

### MOB-09: Creeper Speed Exclusion Edge Cases

**Risk:** Excluding creepers from speed boost requires correct mob type checking. Issues:
- Charged creepers are a STATE not a subtype - both should be excluded
- Modded creepers (from other mods) may or may not extend vanilla Creeper
- Using registry type check instead of instanceof misses subtypes

**Why it happens:** Minecraft's Creeper has charged state as NBT data/entity data, not a subclass. If your exclusion uses exact type matching, you catch vanilla creepers but might miss mod variants. If you use instanceof, you catch all Creeper extensions.

**Prevention:**
```java
// CORRECT: Use instanceof to catch vanilla + mod creepers
if (mob instanceof Creeper) {
    return; // Excludes all creeper variants including charged, modded
}

// WRONG: Type check misses subclasses
if (mob.getType() == EntityType.CREEPER) {
    // This might miss modded creeper variants
}
```

**Detection:** Spawn charged creeper (`/summon creeper ~ ~ ~ {powered:1b}`), verify speed unchanged. If other mods installed, verify their creepers also excluded.

**Phase to address:** Phase 1 (Speed modifications) - use instanceof, document creeper exclusion

---

### MOB-10: Attribute Modifier UUID Collision

**Risk:** When applying speed modifiers via `AttributeModifier`, using hardcoded UUIDs causes:
- Same modifier applied twice doesn't stack (by design, but confusing)
- Mod conflicts if another mod uses same UUID accidentally
- Modifier not properly tracked for removal on mob unload

**Why it happens:** Minecraft uses UUID/ResourceLocation to identify attribute modifiers uniquely. Same ID = same modifier = no stacking, only latest value used. Random UUID per application = stacking (usually wrong). Consistent UUID per modifier TYPE is correct.

**Warning signs:**
- Speed boost applies inconsistently between mobs
- Some mobs visibly faster than others of same type
- Attribute debug shows multiple modifiers where one expected

**Prevention:**
```java
// Use consistent UUID per modifier TYPE, shared across all mobs
private static final ResourceLocation SPEED_MODIFIER_ID =
    ResourceLocation.fromNamespaceAndPath("thc", "monster_speed_boost");

// Apply only once, check before adding:
AttributeInstance speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
if (speedAttr != null && !speedAttr.hasModifier(SPEED_MODIFIER_ID)) {
    speedAttr.addTransientModifier(new AttributeModifier(
        SPEED_MODIFIER_ID,
        0.2, // 20% increase
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    ));
}
```

**Detection:** Spawn 10 zombies of same type, use F3 entity debug to verify all have identical movement speed attribute values.

**Phase to address:** Phase 1 (Speed modifications) - use consistent ResourceLocation IDs, transient modifiers

**Source:** [Minecraft Wiki - Attribute](https://minecraft.wiki/w/Attribute) - modifier UUID uniqueness

---

### MOB-11: Stray Replacement Loses Skeleton Variants

**Risk:** Replacing skeletons with strays in spawn tables affects:
- Spider Jockey skeleton riders (might spawn stray on spider - looks weird)
- Skeleton Horse traps (lightning spawns skeleton horsemen)
- Any mod-added skeleton spawn contexts

**Why it happens:** Spider jockeys specifically spawn `EntityType.SKELETON` as rider via hardcoded logic. If your spawn replacement hooks all skeleton spawns, jockeys get strays. Skeleton trap horses are similar.

**Warning signs:**
- Spider jockeys have strays on them (visual inconsistency)
- Skeleton horse traps spawn stray horsemen
- Mod compatibility issues with skeleton-spawning content

**Prevention:**
```java
// Check spawn context/reason before replacing:
if (spawnReason == MobSpawnType.JOCKEY) {
    return EntityType.SKELETON; // Keep vanilla skeleton for jockeys
}
// Skeleton horse traps use different spawn event - verify behavior
// May need to check spawn group data for jockey flag
```

**Detection:** Trigger spider jockey spawns (rare, may need spawn egg manipulation), verify rider type. Test skeleton trap horses during thunderstorms.

**Phase to address:** Phase 2 (Spawn replacement) - preserve jockey context, test trap horses

---

## Minor Pitfalls

Annoyances that waste time if not anticipated.

### MOB-12: Vex Health Reduction Affects Evoker Behavior

**Risk:** Reducing vex health to 4 hearts (from 14) means they die faster. Evokers have fixed summon cooldowns - they don't track active vex count dynamically. Result: More vex churn, more summon animations, more visual chaos.

**Why it happens:** Evoker summon cooldown is timer-based, not vex-count-based. Lower vex HP = faster vex death = more room for new summons = evoker constantly summoning.

**Consequences:**
- Evoker fights have more vex spawning events
- Potentially overwhelming vex count mid-fight
- More chaotic combat (may be desired for THC difficulty)

**Prevention:** This is likely fine for THC's difficulty goals. Monitor in playtesting. If overwhelming:
```java
// Option: Increase evoker summon cooldown proportionally
// Or: Limit max concurrent vexes per evoker via AI goal modification
```

**Detection:** Fight evoker, count total vexes spawned vs vanilla behavior. Subjective "overwhelming" test.

**Phase to address:** Phase 3 (Vex modifications) - monitor in playtesting, adjust cooldown if needed

---

### MOB-13: Phantom Removal Affects Insomnia Mechanic

**Risk:** Removing phantom natural spawns also removes the insomnia penalty. Players can skip sleeping indefinitely with no consequence. `insomnia` stat still accumulates but triggers nothing.

**Why it happens:** Phantoms are the ONLY consequence for not sleeping. No phantoms = no insomnia penalty.

**Consequences:**
- Beds become purely spawn points (may be intended - THC beds already work 24/7)
- Insomnia statistic accumulates pointlessly

**Prevention:** For THC, this is likely INTENTIONAL - beds already work 24/7 for spawn points, twilight removes day/night cycle importance. **Document as intentional design choice.**

Optionally, clear insomnia stat on bed use to prevent pointless accumulation:
```java
// In bed use handler:
player.getStatistics().setStatistic(Stats.CUSTOM.get(Stats.TIME_SINCE_REST), 0);
```

**Phase to address:** Phase 3 (Phantom removal) - document as intentional, optionally clear stat

---

### MOB-14: Illager Patrol Stage-Gating Edge Case

**Risk:** If patrol spawns are blocked at stage < 2, but patrols that spawned at stage 2+ persist when somehow encountering stage 1 players (shouldn't happen, but edge case), those existing patrols remain.

**Why it happens:** Stage is server-wide persistent state. Patrols spawn and persist normally once spawned. Your gate only affects NEW spawns, not existing entities. Stage should only advance, never decrease - but edge cases include server rollbacks.

**Prevention:** For THC, stage only advances (monotonic) - this is a non-issue. Document the constraint:
```java
// Stage should NEVER decrease. If it could (e.g., server rollback):
// Option: On stage decrease event, despawn existing patrols
// For THC: Not needed since stage is monotonically increasing
```

**Phase to address:** Phase 3 (Patrol gating) - document stage as monotonically increasing

---

## Integration with Existing THC Systems

### INT-01: Speed Modifier vs Parry Stun Interaction

**Risk:** THC's parry stun applies Slowness 5 to nearby monsters (75% speed reduction). If mobs are 20% faster baseline, effective stunned speed is higher: `0.23 * 1.2 * 0.25 = 0.069` vs vanilla `0.23 * 0.25 = 0.0575`. Stunned mobs still move ~20% faster.

**Why it happens:** Slowness is multiplicative on final speed. Faster base = faster stunned speed.

**Consequences:**
- Parry window may feel slightly less effective
- Stunned mobs can still close small gaps faster

**Prevention:** Probably fine - stun is still very effective (75% reduction). Playtest parry timing. If parry feels weaker:
```java
// Option: Increase stun slowness level for THC mobs
// Or: Apply additional speed debuff specifically during stun
```

**Detection:** Parry test with 3 zombies, time how long until they reach melee range vs vanilla. Should be similar effective control time.

**Phase to address:** Phase 1 (Speed modifications) - playtest stun effectiveness, adjust if needed

---

### INT-02: Threat System with Regional Spawns

**Risk:** THC's threat system propagates damage to mobs within 15 blocks. Regional spawning creates distinct mob "bands" at different Y levels. Threat propagation is spatial (distance-based), not region-based.

**Why it happens:** Threat range is 15 blocks horizontally AND vertically. Fighting at Y=32 (surface/cave boundary) could aggro mobs from both surface and upper cave regions simultaneously.

**Consequences:**
- Fighting at region boundaries may pull mobs from multiple Y-bands
- Threat management becomes more complex in vertical spaces (caves near surface)
- Not a bug - threat is spatial by design

**Prevention:** This is WORKING AS DESIGNED. Threat is intentionally spatial, not regionally segmented. Document that Y-level boundaries don't affect threat propagation.

**Detection:** Fight at Y=32 (cave/surface boundary), verify threat propagates to mobs regardless of their spawn region.

**Phase to address:** Phase 5 (Regional spawning) - document threat interaction, no code change needed

---

### INT-03: NaturalSpawnerMixin Integration

**Risk:** THC already has `NaturalSpawnerMixin` blocking spawns in claimed chunks. New regional spawn logic must integrate with, not conflict with, existing mixin. Two separate inject points in NaturalSpawner could interfere.

**Why it happens:** Both systems modify spawn behavior:
- Existing: `isValidSpawnPostitionForType` - blocks base chunks
- New: Likely needs to modify spawn location selection for regional distribution

**Warning signs:**
- Mixin conflicts cause crashes at startup
- Logic ordering issues (region check before or after base check?)
- Claimed chunks no longer properly protected

**Prevention:**
```java
// Option 1: Extend existing NaturalSpawnerMixin
// Add regional logic to same mixin file, shared context

// Option 2: Separate mixins with clear ordering
// Check base claim FIRST (cheap), then regional logic
@Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"))
private static void thc$checkSpawnValidity(...) {
    // 1. Base chunk check (existing logic - returns false if claimed)
    if (ClaimManager.INSTANCE.isClaimed(level.getServer(), chunk)) {
        cir.setReturnValue(false);
        return;
    }
    // 2. Regional logic applied only to unclaimed chunks
    // (don't need to check if claimed - already handled above)
}
```

**Detection:** After regional system implemented, verify claimed chunks STILL block ALL spawns (regression test).

**Phase to address:** Phase 5 (Regional spawning) - integrate into or coordinate with existing NaturalSpawnerMixin

---

## Phase-Specific Warning Summary

| Phase | Topic | Likely Pitfall | Mitigation |
|-------|-------|----------------|------------|
| 1 | Speed modifications | MOB-01: Baby zombie breakage | Explicit exclusion check |
| 1 | Speed modifications | MOB-02: Boss breakage | Boss entity type exclusion |
| 1 | Speed modifications | MOB-10: UUID collision | Consistent ResourceLocation IDs |
| 1 | Speed modifications | INT-01: Parry stun interaction | Playtest stun effectiveness |
| 2 | Zombie->Husk | MOB-03: Drowned conversion loss | Document intentional or depth exception |
| 2 | Skeleton->Stray | MOB-11: Jockey variant loss | Check spawn reason context |
| 3 | Enderman teleport | MOB-04: Infinite loop | Retry limit, cooldown, validation |
| 3 | Ghast projectile | MOB-05: Client desync | Modify at spawn time |
| 3 | Vex modifications | MOB-12: Evoker behavior | Monitor, adjust cooldown if needed |
| 3 | Phantom removal | MOB-13: Insomnia mechanic | Document as intentional |
| 3 | Patrol gating | MOB-14: Stage edge case | Document monotonic stage |
| 4 | Pillager equipment | MOB-06: Loot table conflict | Inject after populateDefaultEquipmentSlots |
| 5 | Regional spawning | MOB-08: Cap underspawning | Weight spawns, don't hard-partition caps |
| 5 | Regional spawning | INT-03: Mixin conflict | Integrate with existing NaturalSpawnerMixin |
| 5 | Regional spawning | INT-02: Threat interaction | Document spatial threat behavior |
| 6 | NBT tagging | MOB-07: Save bloat | Byte encoding, evaluate persistence need |

---

## Sources

### HIGH Confidence (Official/Verified)
- [Minecraft Wiki - Attribute](https://minecraft.wiki/w/Attribute) - modifier UUID behavior, stacking rules
- [Minecraft Wiki - Drowned](https://minecraft.wiki/w/Drowned) - conversion chain mechanics
- [Minecraft Wiki - Husk](https://minecraft.wiki/w/Husk) - husk->zombie->drowned timing (30s + 30s)
- [Minecraft Wiki - Boss](https://minecraft.wiki/w/Boss) - boss immunity mechanics
- [Minecraft Wiki - Mob Spawning](https://minecraft.wiki/w/Mob_spawning) - cap mechanics, spawn flow
- [Fabric Wiki - Persistent State](https://wiki.fabricmc.net/tutorial:persistent_states) - attachment persistence
- [Fabric Wiki - Adding to Loot Tables](https://wiki.fabricmc.net/tutorial:adding_to_loot_tables) - equipment/loot interaction

### MEDIUM Confidence (Community Verified)
- [Technical Minecraft Wiki - Mob Caps](https://techmcdocs.github.io/pages/GameMechanics/MobCap/) - per-player and regional cap details
- [Technical Minecraft Wiki - Desync](https://technical-minecraft.fandom.com/wiki/Client/server_desynchronization) - projectile prediction
- [Minecraft Forum - Enderman Teleport](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/modification-development/1433221-enderman-teleporting-question) - infinite loop warning

### LOW Confidence (Single Source/Needs Validation)
- Vex summon rate impact - theoretical, needs playtesting
- Regional cap partitioning edge cases - inferred from mob cap mechanics
- Modded creeper compatibility - varies by mod implementation

---

## Metadata

**Phase mapping:**
| Pitfall Group | Phase |
|---------------|-------|
| MOB-01, MOB-02, MOB-09, MOB-10 | Speed modifications |
| MOB-03, MOB-11 | Spawn replacement |
| MOB-04, MOB-05, MOB-12, MOB-13, MOB-14 | Mob behavior modifications |
| MOB-06 | Equipment/Pillager variants |
| MOB-07, MOB-08 | Regional spawning & NBT |
| INT-* | Integration with existing systems |

**Research date:** 2026-01-23
**Milestone:** v2.3 Monster Overhaul
**Building on patterns from:**
- `NaturalSpawnerMixin` - spawn blocking in claimed chunks
- `THCAttachments` - entity data attachment pattern
- `ServerPlayerMixin` - attribute modification pattern
- `ThreatManager` - mob state tracking pattern
