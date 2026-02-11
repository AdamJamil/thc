# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-10)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge.
**Current focus:** v3.3 Enemy Health Bars

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-02-10 — Milestone v3.3 started

## Performance Metrics

**Cumulative:**
- 128 plans completed across 16 shipped milestones
- ~9 hours total execution time
- 24 days from project start

**Recent Milestones:**
- v3.2: 3 plans, ~15 min (3 phases)
- v3.1: 7 plans, ~33 min (5 phases)
- v3.0: 4 plans, ~23 min (3 phases)
- v2.8: 11 plans, ~45 min (6 phases)

## Accumulated Context

### Decisions

See milestone archives for full decision logs:
- .planning/milestones/v3.2-ROADMAP.md (previous)

Key patterns established:
- Stage-gated boons follow ClassManager + StageManager pattern
- BoonGate utility for shared class + stage checks
- Projectile-specific mixin targeting (Snowball.class not Projectile.class)
- UUID-to-tick Map for ephemeral tracking in mixins
- Accessor invoker pattern for protected method access
- ResourceKey-based priority map for effect sorting in HUD rendering
- Vanilla mob effect icon texture path: textures/mob_effect/{path}.png
- originalDurations mutableMap pattern for tracking initial effect duration and computing drain ratio
- Sub-tick interpolation via deltaTracker.getGameTimeDeltaPartialTick for smooth HUD animations
- OptionInstance IntRange slider with save-on-change for Video Settings injection
- Ratio-based proportional HUD scaling (all sizes derived from frame size via ratios)
- 12-param blit overload when render size differs from texture source size (decouples UV sampling from screen pixels)

### Pending Todos

None.

### Blockers/Concerns

**Minecraft 1.21.11 Mixin Compatibility**
- PlayerSleepMixin broken after MC version upgrade
- Status: Non-blocking for compilation and development

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 002 | Auto-assign villager jobs when job block placed | 2026-01-31 | 682e3e2 | [002](./quick/002-auto-assign-villager-jobs-on-block-place/) |
| 003 | Render downed player body with red particles | 2026-02-02 | 915f156 | [003](./quick/003-render-downed-player-body/) |
| 004 | Add red beacon beam at downed player location | 2026-02-03 | 971faea | [004](./quick/004-when-a-player-is-downed-on-top-of-the-re/) |
| 006 | Fix snowballs proccing arrow-only effects | 2026-02-03 | 8e3c3e2 | [006](./quick/006-fix-snowball-proccing-arrow-effects/) |
| 007 | Fix effects GUI scaling, overlay, infinite filter, vanilla HUD | 2026-02-10 | 209be5c | [007](./quick/7-fix-v3-2-effects-gui-scaling-math-green-/) |
| 008 | Fix effects GUI overlay alpha (35%) and frame-relative positioning | 2026-02-10 | b25f91a | [008](./quick/8-fix-effects-gui-overlay-transparency-mid/) |

## Session Continuity

Last session: 2026-02-10
Stopped at: Completed quick-8 (effects GUI overlay alpha + positioning)
Resume file: None
Next: /gsd:new-milestone for next milestone
