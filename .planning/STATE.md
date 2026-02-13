# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-12)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge.
**Current focus:** v3.4 Bow Overhaul — Phase 88 (Breeze Bow)

## Current Position

Phase: 87 of 88 (Blaze Bow) -- COMPLETE
Plan: 1 of 1 complete
Status: Phase 87 complete
Last activity: 2026-02-13 — Completed 87-01 (Blaze Bow item, class gate, fire-on-hit)

Progress: 18 milestones (17 shipped + 1 active), 135 plans across 87 phases

## Performance Metrics

**Cumulative:**
- 132 plans completed across 17 shipped milestones
- ~9 hours total execution time
- 28 days from project start

**Recent Milestones:**
- v3.3: 3 plans, ~10 min (3 phases)
- v3.2: 3 plans, ~15 min (3 phases)
- v3.1: 7 plans, ~33 min (5 phases)
- v3.0: 4 plans, ~23 min (3 phases)

## Accumulated Context

### Decisions

See PROJECT.md Key Decisions table and milestone archives for full decision logs.

- **86-01:** BowType stored as @Unique fields on ProjectileEntityMixin (runtime-only, no serialization)
- **86-01:** Drag applied multiplicatively per tick: max(0.8, 1.0 - dragFactor * ticks)
- **86-02:** BowTypeTagAccess duck interface for cross-mixin bow type data (standard Mixin practice)
- **86-02:** TippedArrowItem instanceof for tipped detection (simpler than PotionContents component check)
- **86-02:** Actionbar message only shown when no regular arrow fallback available
- **87-01:** Draw speed 1.5x via releaseUsing charge scaling (dividing actualCharge by 1.5f)
- **87-01:** Fire-on-hit via setRemainingFireTicks(60) -- refreshes on re-hit naturally
- **87-01:** 100% damage for blaze_bow arrows -- no bow-specific multiplier entry defaults to 1.0

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

Last session: 2026-02-13
Stopped at: Completed 87-01-PLAN.md (phase 87 complete)
Resume file: None
Next: Plan phase 88 (Breeze Bow)
