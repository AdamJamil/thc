# Feature Landscape: Monster Overhaul (v2.3)

**Domain:** Minecraft mob behavior modification, spawn system manipulation
**Researched:** 2026-01-23
**Confidence:** MEDIUM-HIGH
**Target Milestone:** v2.3 Extra Features Batch 7

---

## Executive Summary

The v2.3 monster overhaul involves eight major feature categories. Research reveals that most features are well-understood with established modding patterns, but regional spawn distribution and cap partitioning require careful implementation to avoid performance issues and vanilla spawn system conflicts.

**Key findings:**
- Husk/Stray are drop-in replacements for Zombie/Skeleton (same AI, equipment inheritance)
- Movement speed modification via attributes is cleaner than tick-based approaches
- Ghast fireballs use `ExplosionPower` NBT and velocity delta for control
- Enderman teleport-behind is vanilla behavior that can be enhanced
- Equipment must be set during `finalizeSpawn()` lifecycle
- Fabric attachments persist to NBT automatically when configured with codec
- Mob cap manipulation requires careful counting to avoid spawn lockup

---

## Table Stakes

Features users expect for a "monster overhaul" mod. Missing these makes the mod feel incomplete.

| Feature | Why Expected | Complexity | Dependencies | Notes |
|---------|--------------|------------|--------------|-------|
| Movement speed increase | Core difficulty modifier | **LOW** | None | Use attribute modifiers, not tick-based |
| Zombie -> Husk replacement | Stated requirement | **MEDIUM** | Spawn system hook | Must preserve pack spawning behavior |
| Skeleton -> Stray replacement | Stated requirement | **MEDIUM** | Spawn system hook | Same approach as Husk |
| Damage rebalancing | Core difficulty tuning | **LOW** | Existing damage mixin | 7 mobs, attribute modification |
| Phantom spawn removal | Stated requirement | **LOW** | Spawn event cancellation | Simple condition check |
| Iron golem summon prevention | Stated requirement | **LOW** | Mixin on summon logic | Block carving pattern detection |
| Equipment removal on drops | Stated requirement | **LOW** | Loot table or drop event | Prevent armor/weapon drops |

### Movement Speed Modification (LOW Complexity)

**Expected behavior:** 20% faster movement for all mobs except creepers and baby zombies.

**Implementation approach:**
- Use Minecraft's attribute system via `Attributes.MOVEMENT_SPEED`
- Apply modifier during entity spawn/load using `addPermanentModifier()`
- Modifier uses operation `MULTIPLY_BASE` with value `0.2` for 20% increase
- Filter by entity type to exclude creepers and baby zombies

**Edge cases:**
- Baby zombies already have +0.5 speed bonus (vanilla) - verify this stacks correctly
- Speed modifiers are cumulative - ensure no double-application on chunk reload
- Use static modifier ID for idempotent application/removal

**Verified sources:**
- [Fabric Entity Attributes Documentation](https://docs.fabricmc.net/develop/entities/attributes)
- [Minecraft Wiki - Attribute](https://minecraft.wiki/w/Attribute)

**Confidence:** HIGH

---

### Husk/Stray Replacement (MEDIUM Complexity)

**Expected behavior:** Husks replace zombies, Strays replace skeletons in all spawn tables.

**Technical reality:**
- Husk extends Zombie; Stray extends AbstractSkeleton
- Both inherit ALL parent AI, equipment mechanics, and targeting behavior
- Husks: Inflict Hunger effect, immune to sun burning, natural armor rating 2
- Strays: Fire Slowness arrows, make echoing sounds

**Equipment inheritance:**
- Equipment spawning happens in `finalizeSpawn()` -> `populateDefaultEquipmentSlots()`
- Husks/Strays use their parent's equipment tables (randomized gear)
- Equipment drop rates: 8.5% base, modified by Looting
- Picked-up equipment always drops at 100%

**AI inheritance:**
- Full pathfinding, targeting, and attack patterns from parent
- Husk: Same zombie pursuit (40 blocks), door-breaking on Hard
- Stray: Same skeleton ranged AI, underwater melee fallback

**Implementation approach:**
- Mixin on `NaturalSpawner.getRandomSpawnMobAt()` or similar
- When zombie selected, substitute husk entity type
- When skeleton selected, substitute stray entity type
- Preserve SpawnGroupData for pack consistency (all pack members same type)

**Edge cases:**
- Reinforcement spawning (zombie calls for help) - may spawn zombies, not husks
- Spawn eggs - should these spawn replacements? (Probably NO - keep vanilla)
- Structure spawners - excluded per spec (fortress, monument use vanilla pools)
- Conversion mechanics - husks underwater become zombies (30s + 15s conversion)

**Verified sources:**
- [Minecraft Wiki - Husk](https://minecraft.wiki/w/Husk)
- [Minecraft Wiki - Stray](https://minecraft.wiki/w/Stray)

**Confidence:** HIGH

---

## Differentiators

Features that set THC apart from generic difficulty mods. Not expected, but highly valued.

| Feature | Value Proposition | Complexity | Dependencies | Notes |
|---------|-------------------|------------|--------------|-------|
| Regional spawn distributions | Unique cave/surface threat profiles | **HIGH** | Spawn system deep hook | Novel system design |
| Partitioned monster caps | Prevents spawn lockup, enables regional density | **HIGH** | Vanilla cap interception | Requires NBT tracking |
| Pillager variants (MELEE/RANGED) | Combat variety in caves | **MEDIUM** | Equipment at spawn | New equipment loadouts |
| Ghast projectile tuning | Nether threat adjustment | **MEDIUM** | Entity tick/shoot hook | Speed, rate, explosion |
| Enderman teleport-behind | Enhanced tactical threat | **MEDIUM** | AI/damage hook | 50% trigger chance |
| Enderman proximity aggro | Zone denial mechanic | **LOW** | AI goal injection | 3-block radius |
| NBT spawn origin tagging | Debugging + cap tracking | **MEDIUM** | Entity spawn hook | Persistent per-mob data |
| Illager patrol stage-gating | Progression-locked threats | **LOW** | Existing stage system | Stage check on patrol spawn |

### Regional Spawn Distributions (HIGH Complexity)

**Value proposition:** Distinct threat profiles per region (surface witch, upper cave pillagers, lower cave blazes/breeze).

**Implementation approach:**
- Hook into spawn selection after vanilla biome pool query
- Apply probability roll against custom distribution
- On custom hit: override entity type, use custom pack size [1,4]
- On vanilla fallback: preserve biome selection

**Technical requirements:**
1. Region detection: `isSkyVisible(pos)` + `pos.y < 0` checks
2. Probability sampling: cumulative distribution function
3. Entity type substitution: create custom entity, transfer spawn context
4. Pack consistency: all pack members use same selection result

**Edge cases:**
- Border positions (Y=0 caves) - use sky visibility as tiebreaker
- Vanilla fallback when custom mob can't place - fail attempt, no re-roll
- Structure bypass - fortress/monument/mansion use vanilla pools unchanged

**Verified sources:**
- [Minecraft Wiki - Mob spawning](https://minecraft.wiki/w/Mob_spawning)

**Confidence:** MEDIUM (novel system, limited prior art)

---

### Partitioned Monster Caps (HIGH Complexity)

**Value proposition:** Prevents surface zombies from consuming cap needed for cave spawns.

**Vanilla cap mechanics:**
- Formula: `globalCap = mobCap * chunks / 289` (mobCap = 70 for monsters)
- Single-player: always 70 (289/289 chunks)
- Mobs counted: all non-persistent loaded monsters
- Excluded: nametagged, spawner-generated, summoned

**THC partitioning:**
- OW_SURFACE: 30% of vanilla cap (21 mobs single-player)
- OW_UPPER_CAVE: 40% of vanilla cap (28 mobs single-player)
- OW_LOWER_CAVE: 50% of vanilla cap (35 mobs single-player)
- Total: 120% intentional overflow

**Implementation requirements:**
1. NBT tagging: `spawnSystem.region` and `spawnSystem.counted` on spawn
2. Count partitioning: track per-region counts during vanilla iteration
3. Cap comparison: replace vanilla cap check with regional cap
4. Counting rule: only mobs with `counted=true` contribute

**Edge cases:**
- Mobs wandering between regions - counted by SPAWN origin, not current position
- Mob conversion (husk -> zombie underwater) - preserve original region tag
- Chunk unload/reload - NBT persists, counts recalculated on load
- Cross-dimension - Nether/End use vanilla caps unchanged

**Verified sources:**
- [Technical Minecraft Wiki - Mob Caps](https://techmcdocs.github.io/pages/GameMechanics/MobCap/)
- [Minecraft Wiki - Spawn limit](https://minecraft.wiki/w/Spawn_limit)

**Confidence:** MEDIUM (complex vanilla interaction)

---

### Pillager Variants (MEDIUM Complexity)

**Value proposition:** Combat variety - melee pillagers as frontline, ranged as support.

**Implementation approach:**
- Pillager is single entity type - variants are equipment loadouts
- MELEE: iron sword in main hand, no crossbow
- RANGED: crossbow (vanilla behavior)
- Equipment set during `finalizeSpawn()` lifecycle

**Spawn lifecycle for equipment:**
1. Entity created
2. `finalizeSpawn()` called with DifficultyInstance
3. Inside finalizeSpawn: `populateDefaultEquipmentSlots()` called
4. Then: `populateDefaultEquipmentEnchantments()` for enchants

**Implementation hook:**
- Listen for pillager spawn in custom distribution
- After entity creation, before `addToWorld()`
- Set equipment via `setItemSlot(EquipmentSlot.MAINHAND, stack)`
- Clear crossbow if MELEE variant

**Edge cases:**
- Equipment drop chance - should MELEE pillagers drop swords? (Spec says no monster drops)
- Patrol pillagers - use vanilla behavior (RANGED by default)
- Raid pillagers - excluded (structure spawn, vanilla behavior)

**Verified sources:**
- [Forge Javadocs - Mob.finalizeSpawn()](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/Mob.html)
- [Mob Gear Mod](https://www.curseforge.com/minecraft/mc-mods/mob-gear) - equipment-at-spawn pattern

**Confidence:** HIGH

---

### Ghast Projectile Tuning (MEDIUM Complexity)

**Value proposition:** Faster, more dangerous ghast encounters in Nether.

**Target modifications:**
- Projectile speed: +50% velocity
- Fire rate: -25% (shoot every 4 seconds instead of 3)
- Fire spread: +100% explosion radius

**Fireball properties:**
- `ExplosionPower` NBT tag: default 1, controls explosion size
- Velocity: set via `setDeltaMovement()` or initial direction vector
- Trajectory: straight line, no gravity, infinite range
- Damage: 6 HP projectile + up to 22.5 HP explosion (Hard)

**Implementation approach:**
1. **Speed increase:** Mixin on ghast fireball creation, multiply velocity vector by 1.5
2. **Fire rate:** Mixin on ghast AI goal, modify shoot cooldown from 60 ticks to 80 ticks
3. **Explosion radius:** Set `ExplosionPower` to 2 on fireball creation (100% larger)

**Key methods (likely targets):**
- `Ghast.GhastShootFireballGoal` - controls targeting and shooting
- `LargeFireball` constructor or spawn - set NBT/velocity

**Edge cases:**
- Deflected fireballs - should maintain modified speed? (Probably YES)
- Player-summoned fireballs - should these use vanilla behavior? (YES)
- Fire spread on deflection - uses same ExplosionPower

**Verified sources:**
- [Minecraft Wiki - Fireball](https://minecraft.wiki/w/Fireball)
- [Minecraft Wiki - Ghast](https://minecraft.wiki/w/Ghast)

**Confidence:** MEDIUM (AI goal timing needs code verification)

---

### Enderman Teleport-Behind (MEDIUM Complexity)

**Value proposition:** Tactical threat - can't simply turn and face after hit.

**Vanilla teleport mechanics:**
- Triggers: damage, projectile hit, water contact, random
- Range: 32 blocks per axis (64x64x64 cube centered on enderman)
- Destination: movement-blocking block with 3 non-solid blocks above
- Behind-player behavior: ALREADY EXISTS in vanilla when hit ("usually teleports a few blocks behind the player")

**THC modification:**
- Make behind-player teleport more reliable (50% chance per spec)
- Also trigger on damage dealt TO player (not just damage received)

**Implementation approach:**
1. Hook damage events (both directions)
2. On trigger, check 50% probability
3. Calculate position 3 blocks behind player's facing direction
4. Validate destination (solid base, air above, not water)
5. Execute teleport to valid location (or skip if no valid spot)

**Teleport destination validation:**
- Block below must have movement-blocking properties
- 3 blocks of vertical clearance required
- Not waterlogged
- Integer Y distance from current position

**Edge cases:**
- Player against wall - teleport in front instead? (Probably fail)
- Multiple endermen attacking - each rolls independently
- Water behind player - fail teleport, use vanilla random instead

**Verified sources:**
- [Minecraft Wiki - Enderman](https://minecraft.wiki/w/Enderman)

**Confidence:** HIGH (well-documented vanilla mechanics)

---

### NBT Spawn Origin Tagging (MEDIUM Complexity)

**Value proposition:** Enables cap partitioning, debugging, future analytics.

**Schema:**
```
spawnSystem.region: String enum (OW_SURFACE, OW_UPPER_CAVE, OW_LOWER_CAVE, NETHER, END, OTHER)
spawnSystem.counted: Boolean (true only for NATURAL + MONSTER + THC-processed)
```

**Fabric attachment approach (existing in codebase):**
- Use `AttachmentRegistry.create()` with persistent codec
- Attach to Entity target type
- Automatic serialization to NBT on chunk save
- Automatic deserialization on chunk load

**Implementation using existing THCAttachments pattern:**
```java
public static final AttachmentType<String> SPAWN_REGION = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "spawn_region"),
    builder -> {
        builder.initializer(() -> "OTHER");
        builder.persistent(Codec.STRING);
    }
);
```

**Lifecycle:**
1. Entity spawns (any SpawnReason)
2. Before `addToWorld()`, calculate region from position
3. Set `spawnSystem.region` attachment
4. If NATURAL + MONSTER + THC-processed: set `counted = true`
5. Entity saves to chunk with NBT data

**Edge cases:**
- Mob conversion (husk -> zombie) - attachment NOT copied by default
- Need to handle `ServerLivingEntityEvents.MOB_CONVERSION` to preserve
- Existing mobs without tags - treat as `counted = false`

**Verified sources:**
- [Fabric API - AttachmentType](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/attachment/v1/AttachmentType.html)
- Existing THCAttachments.java in codebase

**Confidence:** HIGH (existing pattern in codebase)

---

## Anti-Features

Features to explicitly NOT build. Common mistakes in this domain.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Tick-based movement modification | Performance overhead, inconsistent | Use attribute modifiers |
| Global time freeze for spawn control | Breaks time-dependent mechanics | Use spawn event hooks |
| Entity replacement via kill+summon | Loses NBT, causes flicker | Substitute entity type during spawn selection |
| Finite vex lifetime | Deviates from spec, adds complexity | Accept vex as-is (health nerf only) |
| Mob-specific cooldowns/limits | Complexity explosion | Use cap partitioning instead |
| Real-time cap polling | Performance killer | Count during vanilla iteration |
| Per-chunk NBT storage for spawn data | Overkill, entity attachment sufficient | Use entity attachments |
| Structure spawn pool modification | Breaks vanilla progression | Bypass structure spawns entirely |
| Nametagged mob cap inclusion | Breaks vanilla farms | Exclude as vanilla does |

### Why NOT Tick-Based Movement

**The trap:** Modifying position directly each tick seems simpler than attribute system.

**Why it fails:**
1. **Performance:** 20+ mobs * 20 ticks/second = 400+ calculations/second
2. **Interpolation:** Client interpolates movement, causing jitter
3. **Pathfinding conflict:** AI expects attribute-based speed
4. **Stacking issues:** Other mods may also modify position

**Correct approach:** Single attribute modifier applied once, calculated every tick by vanilla.

---

### Why NOT Kill+Summon Replacement

**The trap:** On zombie spawn, kill it and summon husk at same position.

**Why it fails:**
1. **Visual flicker:** Entity appears then disappears
2. **NBT loss:** SpawnGroupData, equipment randomization lost
3. **Sound spam:** Death sound plays
4. **Pack breaking:** Pack members may get different treatment

**Correct approach:** Substitute entity type BEFORE creation, not after.

---

## Feature Dependencies

```
Stage System (existing v2.2)
    |
    v
Illager Patrol Stage-Gating
    - Requires stage >= 2 for patrol spawns

Spawn System Hook
    |
    +-- Husk/Stray Replacement
    |
    +-- Regional Distributions
    |       |
    |       v
    |   Pillager Variants (equipment at spawn)
    |
    +-- NBT Spawn Origin Tagging
            |
            v
        Partitioned Monster Caps

Damage Mixin (existing)
    |
    v
Movement Speed Modification
Damage Rebalancing

AI Goal Injection Pattern
    |
    +-- Enderman Proximity Aggro
    |
    +-- Enderman Teleport-Behind
    |
    +-- Ghast Fire Rate Modification
```

---

## MVP Recommendation

For MVP implementation, prioritize in this order:

**Phase 1 - Foundation (must have for testing):**
1. NBT spawn origin tagging - enables all cap/distribution features
2. Movement speed modification - simple, high impact
3. Damage rebalancing - simple, uses existing patterns

**Phase 2 - Replacements:**
4. Husk/Stray replacement - core feature, medium complexity
5. Equipment removal from drops - simple loot modification
6. Phantom spawn removal - simple condition
7. Iron golem prevention - simple mixin

**Phase 3 - Regional System:**
8. Regional spawn distributions - core differentiator
9. Partitioned monster caps - requires NBT foundation
10. Pillager variants - requires distribution system

**Phase 4 - Behavior Modifications:**
11. Ghast projectile tuning - isolated, testable
12. Enderman teleport-behind - builds on vanilla
13. Enderman proximity aggro - AI goal injection
14. Vex health/weapon changes - simple attribute/equipment
15. Illager patrol stage-gating - simple stage check

**Defer to post-MVP:**
- None identified - all features in spec are achievable

---

## Complexity Summary

| Complexity | Features |
|------------|----------|
| **LOW** | Movement speed, damage rebalancing, phantom removal, golem prevention, equipment drops, proximity aggro, patrol gating, vex changes |
| **MEDIUM** | Husk/Stray replacement, pillager variants, ghast tuning, enderman teleport, NBT tagging |
| **HIGH** | Regional distributions, partitioned caps |

---

## Sources

### Primary (HIGH Confidence)
- [Minecraft Wiki - Husk](https://minecraft.wiki/w/Husk)
- [Minecraft Wiki - Stray](https://minecraft.wiki/w/Stray)
- [Minecraft Wiki - Enderman](https://minecraft.wiki/w/Enderman)
- [Minecraft Wiki - Ghast](https://minecraft.wiki/w/Ghast)
- [Minecraft Wiki - Fireball](https://minecraft.wiki/w/Fireball)
- [Minecraft Wiki - Mob spawning](https://minecraft.wiki/w/Mob_spawning)
- [Fabric Entity Attributes Documentation](https://docs.fabricmc.net/develop/entities/attributes)
- [Fabric API - AttachmentType](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/attachment/v1/AttachmentType.html)

### Secondary (MEDIUM Confidence)
- [Technical Minecraft Wiki - Mob Caps](https://techmcdocs.github.io/pages/GameMechanics/MobCap/)
- [Forge Javadocs - Mob](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/Mob.html)
- [NeoForge - FinalizeSpawnEvent](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.20.6-neoforge/net/neoforged/neoforge/event/entity/living/FinalizeSpawnEvent.html)
- [Husk Spawn Mod](https://www.curseforge.com/minecraft/mc-mods/husk-spawn) - reference implementation

### Tertiary (LOW Confidence - WebSearch only)
- [Mob Gear Mod](https://www.curseforge.com/minecraft/mc-mods/mob-gear) - equipment at spawn
- [Spawn Balance Utility](https://www.curseforge.com/minecraft/mc-mods/spawn-balance-utility) - spawn weight control

---

## Metadata

**Research type:** Feature Landscape
**Research date:** 2026-01-23
**Valid until:** ~90 days (stable Minecraft 1.21.x)
**Downstream consumer:** Roadmap creation for v2.3 milestone
