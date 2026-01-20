# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-19)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge.
**Current focus:** v1.3 Extra Features Batch 3

## Current Position

Phase: 15 of 16 (Threat System)
Plan: 3 of 4
Status: In progress
Last activity: 2026-01-19 — Completed 15-03-PLAN.md (Threat Decay)

Progress: ██████░░░░ 3/5 phases (60%)

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

**v1.3 Milestone (in progress):**
- Plans completed: 8
- Total execution time: ~22 min

**Cumulative:**
- 30 plans completed across 3+ milestones
- ~2.1 hours total execution time
- 5 days from project start to v1.2 ship

## Accumulated Context

### Decisions

See milestone archives for full decision logs:
- .planning/milestones/v1.0-ROADMAP.md
- .planning/milestones/v1.1-ROADMAP.md
- .planning/milestones/v1.2-ROADMAP.md

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

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-01-19
Stopped at: Completed 15-03-PLAN.md (Threat Decay)
Resume file: None
Next: Execute 15-04-PLAN.md (Threat Targeting)
