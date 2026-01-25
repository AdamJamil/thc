# THC (True Hardcore)

## What This Is

A Minecraft mod that creates a "true hardcore" experience where players must always take meaningful risks to achieve anything. The mod replaces tedium-as-difficulty with risk-as-progression through multiple interconnected systems including combat overhaul (buckler replacing shields), territorial base claiming mechanics, threat-based aggro management, world difficulty tuning, and a twilight system creating perpetual hostility where mobs spawn in daylight and the sky stays at dusk.

## Core Value

Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.

## Requirements

### Validated

- Buckler combat system — existing
  - Five tiers (stone through netherite) with poise mechanics
  - Parry windows, damage reduction, durability system
  - HUD overlay showing poise bar above armor
  - Environmental damage exclusions
  - Lethal parry save mechanism
- Shield replacement — existing
  - Shields removed from all loot tables
  - Bucklers registered and craftable
- Testing infrastructure — existing
  - Smoke test system (100 tick validation)
  - Game tests for buckler mechanics
  - Debug output gated by tags
- Base claiming system — v1.0
  - Bell rings drop land plot books (first ring per bell)
  - Land plots claim chunks with terrain flatness validation
  - Base areas allow unrestricted building
  - Combat blocked in bases ("No violence indoors!")
  - Mining fatigue outside bases (1.4^x stacking, 12s decay)
  - Village chunks protected (no breaking except ores/allowlist)
  - Allowlist-only placement outside bases with adjacency rules
  - Bells indestructible (bedrock-like hardness)
- Crafting tweaks — v1.0
  - Ladder recipe yields 16 (instead of 3)
  - Snowballs stack to 64 (instead of 16)
  - Snow block ↔ 9 snowballs conversion
- Drowning modification — v1.1
  - Drowning damage ticks every 4 seconds (instead of 1)
- Spear removal — v1.1
  - Spears removed from crafting, loot tables, and mob drops
- Projectile combat — v1.1
  - Hit effects: Speed II and Glowing (6s) on target
  - Aggro redirection to shooter
  - Enhanced physics: 20% faster launch, gravity increase after 8 blocks
- Parry stun improvements — v1.2
  - Stun range increased to 3 blocks
  - ~1 block knockback on stunned enemies
- XP economy restriction — v1.2
  - XP orbs only from mob deaths and experience bottles
  - Blocked: ores, breeding, fishing, trading, smelting
- Tiered arrows — v1.2
  - Vanilla arrow renamed to "Flint Arrow" with custom texture
  - Iron Arrow (+1 damage), Diamond Arrow (+2), Netherite Arrow (+3)
  - Anvil crafting: 64 flint arrows + material = 64 tiered arrows
- Combat rebalancing — v1.3
  - Arrow hits cause Speed IV (up from Speed II), no knockback on monsters
  - Sweeping edge enchantment disabled
  - All melee damage reduced by 75%
- Wind charge mobility — v1.3
  - Breeze rods yield 12 wind charges (up from 4)
  - Wind charges boost player 50% higher
  - One-time fall damage negation after self-boost
- Ranged weapon gating — v1.3
  - Bows require 3 breeze rods + 3 string (no sticks)
  - Crossbows require breeze rod + diamond (no sticks/iron)
  - Bows and crossbows removed from all loot tables and mob drops
- Threat system — v1.3
  - Per-mob threat maps (player → threat value)
  - Damage propagates threat to all mobs within 15 blocks
  - Threat decays 1 per second per player
  - Arrow hits add +10 bonus threat
  - Mobs target highest-threat player (threshold 5, unless revenge)
  - Target switching only on revenge or strictly higher threat
- World difficulty — v1.3
  - Mob griefing disabled (no creeper block damage, no enderman pickup)
  - Smooth stone drops cobblestone without silk touch
  - Regional difficulty always maximum (max inhabited time, full moon)
  - No natural mob spawns in base chunks
- Twilight time system — v2.0
  - Server time flows normally (doDaylightCycle active)
  - Night-lock code removed
- Twilight visuals — v2.0
  - Client sees perpetual dusk sky (locked at ~13000 ticks)
  - Overworld only (Nether/End unaffected)
- Undead sun immunity — v2.0
  - Zombies, skeletons, phantoms do not burn in sunlight
  - Fire aspect, lava, and other fire sources still damage undead
- Hostile spawn bypass — v2.0
  - Hostile mobs spawn regardless of sky light level
  - Block light still affects spawn density (torch protection preserved)
- Bee always-work — v2.0
  - Bees work continuously regardless of time of day
  - Bees work continuously regardless of weather (rain)
  - Bees still return to hive when nectar-full (behavior preserved)
- Villager twilight — v2.0
  - Villagers always attempt to stay inside/go to bed (night behavior active)
  - Villagers behave as if always night for schedule purposes
- Bed mechanics — v2.0
  - Beds usable at any time (no time-of-day restriction)
  - Sleeping does not skip time or advance day/night cycle
  - Beds still set spawn point when used
- Blast Totem — v2.1
  - Custom item replaces Totem of Undying in all loot sources
  - Custom texture (blast_totem.png)
- Furnace gating — v2.1
  - Furnace recipe requires blaze powder (Nether progression gate)
  - Blast furnace recipe requires blast totem (Evoker progression gate)
  - Natural furnace/blast furnace spawns removed from villages
- Village structure protection — v2.1
  - Block breaking restricted to structure bounding boxes only
  - Underground traversal below villages unrestricted
  - Ore and allowlist exceptions preserved
- Saturation-tiered healing system — v2.1
  - Eating: saturation = max(food_sat, current_sat), 64 tick eating duration
  - Exhaustion: 4.0 exhaustion removes 1.21 saturation (21% faster drain)
  - Healing requires hunger ≥ 18 (9 bars)
  - Saturation tiers: T5 (6.36+) +1 heart/s, T4 (2.73+) +0.5 heart/s, T3 (1.36+) +3/16, T2 (0.45+) +1/8, T1 (<0.45) +1/16
  - Vanilla natural regeneration disabled
- Food economy overhaul — v2.2
  - Smoker gated behind iron (recipe + village removal)
  - Apples drop from all 9 leaf types at 5x rate
  - Instant crop maturation on bonemeal use
  - Bone yields 6 bonemeal (doubled from vanilla)
  - Food removals: suspicious stew, mushroom stew, beetroot soup, sugarcane→sugar
  - Complete hunger/saturation rebalancing (29 items across 4 tiers)
  - Hearty Stew: renamed rabbit stew (10 hunger, 6.36 saturation)
  - Honey Apple: new item (8 hunger, 2.73 saturation)
- Class system — v2.2
  - /selectClass <tank|melee|ranged|support> command (base chunks only)
  - Tank: +1 heart, x2.5 melee, x1 ranged
  - Melee: +0.5 hearts, x4 melee, x1 ranged
  - Ranged: no health change, x1 melee, x5 ranged
  - Support: no health change, x1 melee, x3 ranged
  - Permanent selection, persists across death
- Stage system — v2.2
  - /advanceStage operator command (5 stages max)
  - Server-wide stage, per-player boon level
  - Late-joiner sync (new players get current stage as boon level)
  - Boon level scaffolding for future expansion
- Monster overhaul — v2.3
  - 20% speed increase (except creepers/baby zombies)
  - Equipment/iron drops removed from monsters
  - Zombie → Husk, Skeleton → Stray surface replacements
  - Ghast: faster fireballs, slower fire rate, expanded fire spread
  - Enderman: teleport-behind, proximity aggro
  - Vex: reduced health, no sword
  - Phantom spawns disabled, Patrols stage-gated (stage 2+)
  - Iron golem summon prevention
  - Regional spawn system (Surface/Upper Cave/Lower Cave)
  - Custom mob distributions with witches, pillagers, blazes, breezes, vindicators, evokers
  - Pillager MELEE/RANGED variants
  - Partitioned monster caps (30%/40%/50%)
  - NBT spawn origin tagging
  - Damage rebalancing for 6 mobs (Vex, Vindicator, Magma Cube, Blaze, Piglin, Evoker fangs)

### Active

- Iron boats — v2.4
  - Craftable with 5 iron ingots + magma cream (minecart shape)
  - Works on lava like regular boats work on water
  - Item drop is lava-proof, floats, flies to player when broken
  - Custom textures (iron_boat.png, iron_boat_icon.png)
- Saddle removal — v2.4
  - Removed from all chest loot tables
  - Mobs no longer drop saddles
  - Cannot be crafted
- Bucket changes — v2.4
  - Lava buckets cannot be placed
  - Copper buckets craftable (water/milk only)
  - Water placement creates non-source water at max height
- Elytra changes — v2.4
  - Fireworks no longer propel during flight
  - 2x speed gain when diving, 1.8x speed loss when ascending
- Brewing removal — v2.4
  - Brewing stands removed from natural spawns
  - Brewing stands cannot be crafted
  - Potions removed from piglin bartering
- Armor rebalancing — v2.4
  - New armor values: leather 7, copper 10, iron 15, diamond 18, netherite 10
  - New toughness values: leather 0, copper 0, iron 0, diamond 4, netherite 6
  - Half armor points allowed per piece

### Out of Scope

- Vanilla hardcore mode integration — THC defines its own hardcore ruleset independent of vanilla permadeath
- Multiplayer territory conflict resolution — focus is single-player or cooperative server experience for now
- Buckler visual effects beyond existing implementation — parry system complete as-is
- Tipped tiered arrows — complexity deferred
- Threat persistence across chunk unload — threat is ephemeral by design
- Weather visual override — rain during dusk is atmospheric, acceptable
- Nether/End twilight — these dimensions have their own aesthetics
- Shader-specific fixes — basic compatibility expected, deep Iris integration deferred
- Phantom spawning changes — phantoms now removed from natural spawns in v2.3

## Context

**Technical Environment:**
- Minecraft 1.21.11
- Fabric Loader 0.18.4+
- Fabric API + Fabric Language Kotlin
- Java 21+
- Existing codebase with working buckler system

**Design Philosophy:**
- Risk/reward over tedium/grind
- Multiple interconnected systems (combat, territory, twilight)
- Implementation and design execute in parallel - new systems added iteratively as designed
- Game tests preferred for verification, manual testing as fallback

**Existing Architecture:**
- Kotlin + Java mixed codebase (~4,045 LOC)
- Mixins for vanilla behavior modification
- Attachment API for player and mob state
- Client/server networking for state sync
- Fabric GameTest framework integration
- Data generation for recipes/models

## Constraints

- **Minecraft API conventions**: Research best practices frequently - Fabric/Minecraft conventions change between versions and documentation is incomplete. Avoid failed iteration cycles by validating approaches before implementation.
- **Testing**: Implement game tests where possible. Run existing smoke tests before claiming implementation complete.
- **Performance**: Chunk checking for base/village status happens frequently during gameplay. Needs efficient lookup.
- **Compatibility**: Must work with Minecraft 1.21.11 Fabric - no version drift.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Buckler replaces shields entirely | Supports risk/reward philosophy - active defense (parry) vs passive (shield block) | Good |
| Twilight system over permanent night | Allows normal time flow while maintaining hostility, better gameplay variety | Good |
| Iterative system design | Allows parallel design/implementation, faster iteration | Good |
| Attachments for player state | Fabric API standard for entity data persistence | Good |
| GameTest over manual testing | Automated verification prevents regressions | Good |
| Threat as session-scoped state | Mobs forget threat on unload, keeps threat tactical not strategic | Good |
| Boolean attachment for one-time effects | Wind charge fall negation tracks state without complex logic | Good |
| HEAD inject for spawn blocking | NaturalSpawner.isValidSpawnPostitionForType interception | Good |
| BedRule redirect for sleep | Cleanest approach for 24/7 bed usage | Good |
| Brain schedule redirect for villagers | Force night activity without modifying multiple AI systems | Good |
| Position-based village protection | Structure bounding boxes vs chunk detection for granular protection | Good |
| ThreadLocal for cross-injection state | Store saturation in HEAD, compare in RETURN for cap behavior | Good |
| HEAD cancellation for FoodData.tick() | Complete control over exhaustion and healing logic | Good |
| Fixed interval + variable heal amount | Simpler than variable intervals for saturation tier healing | Good |

## Current Milestone: v2.4 Extra Features Batch 8

**Goal:** Add mobility and survival mechanics (iron boats, copper buckets, elytra changes) while removing trivializing features (brewing, easy saddles) and rebalancing armor progression.

**Previous milestone:** v2.3 Extra Features Batch 7 (Monster Overhaul) shipped 2026-01-25

## Current State

**Latest Ship:** v2.3 Extra Features Batch 7 (2026-01-25)

**Codebase:**
- ~6,807 LOC Kotlin/Java
- Mixed mixin + event-driven architecture
- 71 plans across 45 phases in 8 milestones
- Attachment patterns for player state, mob threat, one-time effects, class, boon level, spawn region
- Client visual overrides for twilight sky
- Comprehensive spawn/behavior modifications (10+ mob types)
- Position-based structure protection
- Saturation-tiered healing system
- Food economy with cooking progression gates
- Class system with permanent role differentiation
- Stage system with boon level scaffolding
- Regional spawn system with partitioned caps
- Custom mob distributions per Overworld region

**Known issues:**
- PlayerSleepMixin broken from MC 1.21.11 upgrade (blocks smoke test, not functionality)

**Technical debt:** None identified

---
*Last updated: 2026-01-25 after v2.4 milestone started*
