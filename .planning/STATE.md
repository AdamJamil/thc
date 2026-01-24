# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-23)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge.
**Current focus:** v2.3 Monster Overhaul - 8 phases, 23 requirements

## Current Position

Phase: 41 (NBT Spawn Origin Tagging)
Plan: 1 of 1 complete
Status: Phase complete
Last activity: 2026-01-24 - Completed 41-01-PLAN.md (Spawn origin attachments)

Progress: [█████---] 63% - v2.3 Phase 41/44 complete (5 of 8 phases)

## Performance Metrics

**v1.0 Milestone:**
- Total plans completed: 13
- Average duration: 4.6 min
- Total execution time: ~1.0 hours
- Timeline: 3 days (Jan 15-17, 2026)

**v1.1 Milestone:**
- Total plans completed: 4
- Average duration: 3.75 min
- Total execution time: ~15 min
- Timeline: 1 day (Jan 18, 2026)

**v1.2 Milestone:**
- Plans completed: 5
- Total execution time: 22 min
- Timeline: 1 day (Jan 19, 2026)

**v1.3 Milestone:**
- Plans completed: 13
- Total execution time: ~46 min
- Timeline: 2 days (Jan 19-20, 2026)

**v2.0 Milestone:**
- Plans completed: 7
- Total execution time: ~22.5 min
- Timeline: 1 day (Jan 20, 2026)

**v2.1 Milestone:**
- Plans completed: 7
- Requirements: 20/20
- Total execution time: ~1 day
- Timeline: 1 day (Jan 22, 2026)

**v2.2 Milestone:**
- Plans completed: 9
- Total execution time: ~35 min
- Timeline: 2 days (Jan 22-23, 2026)

**v2.3 Milestone:**
- Phases: 37-44 (8 phases)
- Requirements: 23 (FR-01 through FR-23)
- Status: In progress (5 of 8 phases complete)
- Plans completed: 7
- Total execution time: ~46 min

**Cumulative:**
- 63 plans completed across 7 milestones
- ~4.9 hours total execution time
- 10 days from project start

## Accumulated Context

### Decisions

See milestone archives for full decision logs:
- .planning/milestones/v1.0-ROADMAP.md
- .planning/milestones/v1.1-ROADMAP.md
- .planning/milestones/v1.2-ROADMAP.md
- .planning/milestones/v1.3-ROADMAP.md
- .planning/milestones/v2.0-ROADMAP.md
- .planning/milestones/v2.1-ROADMAP.md
- .planning/milestones/v2.2-ROADMAP.md

Key patterns established:
- SavedDataType with Codec for persistent state
- Multi-position sampling for structure detection
- Mixin + event-driven architecture for vanilla behavior modification
- Accessor mixin pattern for immutable component modification
- Counter-based damage rate modification via hurtServer mixin
- REMOVED_RECIPE_PATHS: Set-based recipe filtering in RecipeManagerMixin
- removedItems: Combined set for multi-item loot table filtering
- Projectile hit modification via onHitEntity inject with owner check
- Projectile physics: shoot TAIL + tick HEAD injections with @Unique spawn tracking
- Vec3 directional knockback with hurtMarked for velocity sync
- XP blocking: HEAD cancellation for method-level blocking, @Redirect for ExperienceOrb.award interception
- Anvil recipe interception: HEAD injection on createResult with @Shadow field access
- Post-hit modification: TAIL injection on onHitEntity for velocity/knockback changes after vanilla processing
- MobCategory filtering for monster-only effects
- @ModifyVariable damage reduction: ordinal=0 STORE for first float in method
- @Redirect enchantment nullification: intercept helper method to return 0
- TAIL injection for post-explosion velocity modification with hurtMarked sync
- Boolean attachment for one-time state tracking (wind charge boost)
- Non-persistent Map<UUID, Double> attachment for session-scoped threat storage
- Static utility class pattern for attachment CRUD operations (ThreatManager)
- Timestamp attachment for rate-limiting operations (THREAT_LAST_DECAY pattern)
- Lazy decay via method call (vs tick mixin) for efficient threat decay
- LivingEntity mixin with Mob filter for inherited method targeting
- AABB.inflate for area-based entity queries with MobCategory filtering
- TargetGoal extension with Flag.TARGET for custom targeting AI
- @Shadow @Final for protected GoalSelector access in Mob mixin
- Type check in mixin callback for class-specific goal injection (Monster filter)
- GameRules boolean modification: world.gameRules.set(GameRules.RULE_NAME, value, server)
- Silk touch conditional loot: minecraft:alternatives with match_tool predicate
- Natural spawn blocking: HEAD inject on NaturalSpawner.isValidSpawnPostitionForType
- Chunk claim check from Java: ClaimManager.INSTANCE.isClaimed(server, chunkPos)
- Difficulty override: HEAD inject on getCurrentDifficultyAt returning custom DifficultyInstance
- Client visual override: Level mixin with instanceof ClientLevel check for client-only effects
- Interface method targeting: Fabric Loom remapping warning expected when method from interface (LevelTimeAccess)
- Sun burn prevention: HEAD inject on Mob.isSunBurnTick with cancellable=true returning false
- Sky light spawn bypass: HEAD inject on Monster.isDarkEnoughToSpawn with block light preservation via DimensionType
- Environment attribute redirect: @Redirect on EnvironmentAttributeReader.getValue for attribute-specific behavior override (BEES_STAY_IN_HIVE)
- Brain schedule time redirect: @Redirect on Brain.updateActivityFromSchedule with constant time for forced activity
- BedRule redirect: @Redirect on EnvironmentAttributeSystem.getValue for sleep restriction bypass
- Sleep time skip prevention: @Redirect on GameRules.get for ADVANCE_TIME in sleep block
- Loot table item replacement: detect before remove, add replacement after (MODIFY_DROPS pattern)
- @Redirect setBlock for structure block filtering: intercept ServerLevelAccessor.setBlock in placeInWorld
- Position-based structure protection: getStructureWithPieceAt(pos) for precise bounding box checks
- ThreadLocal for paired injection state: store in HEAD, use in RETURN (eating saturation cap)
- HEAD cancellation with ci.cancel() for complete method replacement (FoodData.tick)
- Accessor interface expansion for multiple private fields on same target class
- getSaturationLevel() tier mapping for variable healing rates (descending threshold checks)
- Fixed interval (5 ticks) + variable heal amount for smoother tiered healing
- Loot table override for universal item drops (apples from all leaves)
- CropBlock performBonemeal HEAD injection for instant crop maturation
- DefaultItemComponentEvents.MODIFY for vanilla item component modification at startup
- FoodProperties.Builder saturationModifier formula: targetSat / (nutrition * 2)
- Context.modify for custom THC items in FoodStatsModifier (not just vanilla Items)
- Translation override for vanilla item rename (item.minecraft.rabbit_stew)
- Persistent string attachment for player class selection (PLAYER_CLASS)
- Class selection permanence via hasClass() check in setClass()
- Base chunk restriction for class selection command
- @Redirect on getAttributeValue for attribute-specific modification (SWEEPING_DAMAGE_RATIO in MC 1.21.11)
- Post-reduction multiplier application: class multipliers applied after base damage reduction
- Server-only class lookup: instanceof ServerPlayer check for attachment access
- Integer attachment with copyOnDeath: BOON_LEVEL persistent state surviving respawn
- Static utility for dual state management: StageManager handles server-wide stage + per-player boon level
- Actionbar broadcast pattern: displayClientMessage(message, true) for server-wide announcements
- Null-safe integer attachment getter: return default 0 when attachment is null
- Operator command pattern: .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) for MC 1.21.11
- Late-joiner initialization: JOIN event with hasClass() check to distinguish new vs returning players
- Minecraft 1.21.11 permission system: Permissions constants (COMMANDS_GAMEMASTER, COMMANDS_ADMIN, etc.) replace hasPermissionLevel()
- ServerEntityEvents.ENTITY_LOAD for entity spawn modification (monster speed in v2.3)
- Transient AttributeModifiers for runtime-only stat changes (no save bloat)
- EntityType comparison for type-specific behavior in MC 1.21.11 (`mob.type == EntityType.ZOMBIE` not `instanceof`)
- Identifier API replaces ResourceLocation in MC 1.21.11 (Identifier.fromNamespaceAndPath)
- Counter-modifiers for negating vanilla bonuses (baby zombie speed normalization)
- @Redirect on addFreshEntityWithPassengers for spawn-time entity replacement (NaturalSpawner)
- canSeeSky(BlockPos) for surface vs underground determination
- Entity subpackage paths in MC 1.21.11: monster.zombie.Zombie, monster.skeleton.Skeleton
- snapTo() replaces moveTo() for entity positioning in MC 1.21.11
- Passenger check for jockey preservation (!getPassengers().isEmpty() || getVehicle() != null)
- HEAD cancellation on custom spawners for complete spawn removal (PhantomSpawner, PatrolSpawner)
- Stage-conditional spawning via StageManager.getCurrentStage check in mixin
- Block pattern detection + cancellation for summon prevention (CarvedPumpkinBlock.trySpawnGolem)
- Level.isClientSide() method in MC 1.21.11 (not field access)
- BaseValue modification for permanent health changes (simpler than AttributeModifier for idempotent operations)
- Equipment removal via setItemSlot(EquipmentSlot, ItemStack.EMPTY) for idempotent gear clearing
- SimpleEntityBehaviors pattern: separate object for simple entity-specific modifications
- Inner class mixin target: targets = "pkg.Outer$InnerClass" syntax for inner class modification
- @ModifyConstant for single constant value replacement in methods
- ENTITY_LOAD for projectile velocity modification at spawn time
- TAIL inject on onHit for post-explosion fire placement
- Invoker accessor pattern: @Invoker annotation for calling private methods in mixin targets (EnderManAccessor)
- Vec3 behind-position calculation: playerPos.subtract(playerLook.scale(distance)) for flanking behavior
- Random chance behavior: level.random.nextBoolean() for 50% probability checks
- Mob.finalizeSpawn TAIL injection for spawn-time attachment setting
- getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) for surface detection
- Persistent String attachment with null default (absence = not tagged)

### Pending Todos

None.

### Blockers/Concerns

**Minecraft 1.21.11 Mixin Compatibility**
- PlayerSleepMixin broken after MC version upgrade (PlayerAttackMixin fixed in 35-01)
- Build succeeds but runtime smoke test cannot complete
- Class system fully implemented (35-01, 35-02) but in-game testing blocked
- Status: Non-blocking for compilation and development, blocks in-game testing

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 001 | Update honey apple recipe to 8x batch | 2026-01-23 | d53a983 | [001-honey-apple-recipe-8x](./quick/001-honey-apple-recipe-8x/) |

## Session Continuity

Last session: 2026-01-24
Stopped at: Completed phase 41 (NBT Spawn Origin Tagging)
Resume file: None
Next: /gsd:discuss-phase 42
