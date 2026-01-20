# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-20)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge.
**Current focus:** v2.0 Twilight Hardcore

## Current Position

Phase: 18 of 23 (Twilight Visuals)
Plan: Not started
Status: Ready to plan
Last activity: 2026-01-20 — Phase 17 complete (night lock removed)

Progress: ████████████████░░░░ 74% (17/23 phases complete)

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

**v2.0 Milestone (in progress):**
- Plans completed: 1
- Total execution time: ~2 min

**Cumulative:**
- 36 plans completed across 5 milestones
- ~2.5 hours total execution time
- 6 days from project start to v1.3 ship

## Accumulated Context

### Decisions

See milestone archives for full decision logs:
- .planning/milestones/v1.0-ROADMAP.md
- .planning/milestones/v1.1-ROADMAP.md
- .planning/milestones/v1.2-ROADMAP.md
- .planning/milestones/v1.3-ROADMAP.md

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

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-01-20
Stopped at: Completed 17-01-PLAN.md
Resume file: None
Next: Run /gsd:plan-phase 18
