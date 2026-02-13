# THC (True Hardcore)

## What This Is

A Minecraft mod that creates a "true hardcore" experience where players must always take meaningful risks to achieve anything. The mod replaces tedium-as-difficulty with risk-as-progression through multiple interconnected systems including combat overhaul (buckler replacing shields), territorial base claiming mechanics, threat-based aggro management, world difficulty tuning, a twilight system creating perpetual hostility where mobs spawn in daylight and the sky stays at dusk, and a stage-gated enchantment system where powerful enchantments require specific mob farming rather than chest looting.

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
- Enchantment removal — v2.5
  - 12 enchantments removed: loyalty, impaling, riptide, infinity, knockback, punch, quick charge, lunge, thorns, wind burst, multishot, density
  - Purged from loot tables, mob spawns, and equipment
- Single-level enchantments — v2.5
  - All enchantments display without level suffix (no I/II/III)
  - Flame deals 6 HP over 6 seconds (1 dmg/s)
  - Fire Aspect deals 9 HP over 6 seconds (1.5 dmg/s via accumulator)
- Lectern enchanting — v2.5
  - Stage 1-2 books placeable on lecterns (mending, unbreaking, efficiency, fortune, silk_touch, lure, luck_of_the_sea)
  - Right-click with compatible gear to enchant (level 10+, costs 3 levels)
  - Book remains on lectern (unlimited use)
  - Stage 3+ books rejected
- Enchanting table overhaul — v2.5
  - New recipe: iron blocks + soul dust + book
  - Requires 15 bookshelves to function
  - Book-slot mechanic replaces RNG (book determines enchantment)
  - Stage 3: level 20 minimum, Stage 4-5: level 30 minimum
  - Costs 3 levels, book remains (unlimited use)
- Acquisition gating — v2.5
  - Stage 3+ enchantments removed from Overworld chest loot
  - Mob drops: drowned (aqua affinity, depth strider, frost walker, respiration), spider (bane of arthropods), husk/stray (smite), blaze (fire protection), magma cube (flame, fire aspect)
  - 2.5% base drop rate, +1% with Looting
- Soul economy — v2.6
  - Soul dust drops from all 6 illager types (20% + 1% Looting)
  - 4 soul dust crafts 1 soul soil (2x2 pattern)
- Mining fatigue exemptions — v2.6
  - Gravel always drops flint with shovel
  - Exempts: flowers, grass, glass, beds, ores, gravel, all placeable blocks
- Combat tuning — v2.6
  - Arrow hit speed reduced from Speed IV to Speed III
  - Melee pillager damage buffed from 4.5 to 6.5
- Spawn distribution — v2.6
  - Wither skeletons spawn in deepslate caves at 15%
  - Deepslate pillager weight reduced from 25% to 20%
  - Deepslate vanilla fallback reduced from 45% to 35%
- Proximity threat — v2.6
  - Dealing X damage adds ceil(X/4) threat to mobs within 5 blocks of player
  - Direct damage target excluded from proximity threat
- Village deregistration — v2.6
  - Beds/workstations in claimed chunks don't register POI
  - Villagers cannot claim POI in claimed chunks (memory blocking)
- Smithing table upgrades — v2.6
  - Leather→copper→iron→diamond armor upgrades with enchantment preservation
  - Wooden→stone→copper→iron→diamond tool upgrades with crafting-equivalent costs
  - Diamond→netherite unchanged (vanilla passthrough)
  - Alternative copper recipe for smithing table
- Revival system — v3.0
  - Death interception via Fabric API ALLOW_DEATH event
  - Downed state with spectator mode and 50-block tether
  - Revival progress (0.5/tick, Support 1.0/tick) preserved on interruption
  - Revival completion: 50% HP, 6 hunger, green particles
  - Server-to-client sync with 60-degree look cone
  - Radial progress ring HUD centered on cursor
- Bastion class boons — v3.1
  - Tank → Bastion rename throughout codebase
  - Boon 1 (Stage 2+): Buckler restricted to Bastion class
  - Boon 2 (Stage 3+): Parry threat propagation + sweeping edge enabled
  - Boon 3 (Stage 4+): Enhanced snowballs with AoE Slowness III and knockback
  - Boon 4 (Stage 5+): Land boat placement, hostile mob trapping (4s breakout), copper recipes, 16-stack

- Enemy mob health bars — v3.3
  - Three-layer floating health bar (empty/full/absorption) above hostile mobs
  - Billboard positioning 0.5 blocks above mob heads, 32-block range
  - Visibility gating: hidden at full HP with no effects/absorption
  - Mob status effects rendered left-to-right above health bar
  - Duration overlay with sub-tick drain and per-mob duration tracking
  - Video Settings scaling slider (2-20%) with file persistence

### Active

(None — planning next milestone)

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
- Kotlin + Java mixed codebase (~14,865 LOC)
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
| String IDs for enchantment comparison | Stable across registry reloads vs Holder references | Good |
| Non-persistent FIRE_SOURCE attachment | Fire is temporary, no need to persist | Good |
| Accumulator pattern for Fire Aspect | Minecraft only supports whole damage, alternate 1/2 HP for 1.5 avg | Good |
| 140 ticks fire duration | First tick immune, 140 = 6 actual damage applications | Good |
| Book stays in slot after enchanting | Unlimited uses for both lectern and table | Good |
| Flat +1% Looting bonus | Simpler than scaling per level for mob book drops | Good |
| Brain schedule redirect for villagers | Force night activity without modifying multiple AI systems | Good |
| Position-based village protection | Structure bounding boxes vs chunk detection for granular protection | Good |
| ThreadLocal for cross-injection state | Store saturation in HEAD, compare in RETURN for cap behavior | Good |
| HEAD cancellation for FoodData.tick() | Complete control over exhaustion and healing logic | Good |
| Fixed interval + variable heal amount | Simpler than variable intervals for saturation tier healing | Good |
| ResourceKey-based priority map for effects | Stable identity comparison via unwrapKey() | Good |
| originalDurations mutableMap pattern | Track initial duration for drain ratio, reset on refresh | Good |
| Sub-tick interpolation for HUD animations | Smooth visual drain between ticks via partialTick | Good |
| Ratio-based proportional HUD scaling | All sizes derived from frame size for smooth scaling at any % | Good |
| Simple text file config persistence | Zero extra dependencies vs JSON/TOML for single value | Good |
| CameraAccessor yRot/xRot for billboard rotation | Direct camera rotation access for world-space billboard rendering | Good |
| EntityTypeTest.forClass for mob querying | Efficient area-based entity filtering by type | Good |
| Per-entity duration tracking keyed by entityId + effect name | Accurate drain ratios per mob without global state | Good |
| WIDTH_PER_PERCENT dynamic scaling | Slider value * constant = world-space width, simple and proportional | Good |

## Current State

**Latest Ship:** v3.3 Enemy Health Bars (2026-02-12)

**Codebase:**
- ~14,865 LOC Kotlin/Java
- Mixed mixin + event-driven architecture
- 131 plans across 85 phases in 17 milestones
- Attachment patterns for player state, mob threat, one-time effects, class, boon level, spawn region, fire source
- Client visual overrides for twilight sky
- Comprehensive spawn/behavior modifications (10+ mob types)
- Position-based structure protection
- Saturation-tiered healing system
- Food economy with cooking progression gates
- Class system with permanent role differentiation
- Stage system with boon level scaffolding
- Regional spawn system with partitioned caps
- Custom mob distributions per Overworld region (including wither skeletons in deepslate)
- Iron boat entity for lava navigation
- Copper bucket system (water/milk only)
- Fluid placement restrictions (no lava, flowing water)
- Skill-based elytra flight (no fireworks, pitch multipliers)
- Brewing economy removal
- Armor progression rebalancing (5 tiers)
- Stage-gated enchantment system (lectern for 1-2, table for 3+)
- Mob-specific enchantment book drops
- Custom fire enchantment damage (Flame 6HP, Fire Aspect 9HP)
- Soul economy (illager drops + soul soil crafting)
- Expanded mining fatigue exemptions (flowers, grass, glass, beds, ores, gravel)
- Proximity threat propagation (player-centered, 5 blocks)
- Village deregistration in claimed chunks (POI + memory blocking)
- Smithing table tier upgrades (armor + tools with enchantment preservation)
- Mining fatigue cap at level 10 — v2.7
- Poise meter icon scaling with spacing — v2.7
- Bell ringing fix (PASS instead of SUCCESS) — v2.7
- Arrow recipe 16x yield — v2.7
- Pillager/Stray arrow damage reduction — v2.7
- Dough crafting system (wheat + water bucket → dough → bread) — v2.7
- Leather drops from pigs and sheep — v2.7
- Protection enchantments stackable (all 4 types) — v2.7
- Damage enchantments stackable (smite/sharpness/bane) — v2.7
- Villager job restrictions (mason/librarian/butcher/cartographer only) — v2.8
- Manual villager leveling with stage gates and emerald cost — v2.8
- Trade cycling (reroll current rank trades with emerald) — v2.8
- Custom trade tables for all 4 allowed villager types — v2.8
- Structure locator items for cartographer trades — v2.8
- Rail recipe changes (copper alternative, 64x yields) — v2.8
- Downed state attachment and death interception — v3.0
- Revival progress system with class-based speed bonus — v3.0
- Revival progress HUD with server-client sync — v3.0
- BoonGate utility for class + stage gate checks — v3.1
- Buckler class + stage restriction (Bastion Stage 2+) — v3.1
- Parry threat propagation (Bastion Stage 3+) — v3.1
- Conditional sweeping edge (Bastion Stage 3+) — v3.1
- Enhanced snowball with AoE slowness and knockback (Bastion Stage 4+) — v3.1
- Boat land placement gate (Bastion Stage 5+) — v3.1
- Hostile mob boat trapping with timed breakout — v3.1
- Boat stack size increase (16) and copper recipes — v3.1
- Effects GUI HUD with priority-sorted status effects in bottom-left corner — v3.2
- Layered frame rendering: effect_frame.png, 2x vanilla icon, green duration overlay, roman numerals — v3.2
- Smooth per-tick duration drain with sub-tick interpolation — v3.2
- Video Settings scaling slider (2-20% of screen width) with file persistence — v3.2
- Three-layer mob health bar renderer with billboard quads (empty/HP/absorption) — v3.3
- World-space status effect icons above health bar with shared rendering constants — v3.3
- Per-mob effect duration tracking with sub-tick interpolation — v3.3
- MobHealthBarConfig with Video Settings slider and dynamic renderer scaling — v3.3

**Known issues:**
- PlayerSleepMixin broken from MC 1.21.11 upgrade (blocks smoke test, not functionality)

**Technical debt:** None identified

---
*Last updated: 2026-02-12 after v3.3 milestone completion*