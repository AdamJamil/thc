# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-15)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.
**Current focus:** Phase 4 - World Restrictions

## Current Position

Phase: 4 of 5 (World Restrictions)
Plan: 2 of 3 completed (04-02-PLAN.md)
Status: In progress
Last activity: 2026-01-16 - Completed 04-02-PLAN.md

Progress: ████████░░░░░ 62% (8/13 plans completed)

## Performance Metrics

**Velocity:**
- Total plans completed: 8
- Average duration: 5.5 min
- Total execution time: 0.73 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-land-plot-system | 2 | 6min | 3min |
| 02-chunk-claiming-core | 3 | 16min | 5.3min |
| 03-base-area-permissions | 1 | 6min | 6min |
| 04-world-restrictions | 2 | 14min | 7min |

**Recent Trend:**
- Last 5 plans: 02-02 (8min), 02-03 (3min), 03-01 (6min), 04-01 (8min), 04-02 (6min)
- Trend: Stable (6.2min avg)

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

| Decision | Phase | Rationale |
|----------|-------|-----------|
| Land plot book non-stackable (stacksTo(1)) | 01-01 | Maintains scarcity and value as currency |
| Bells removed from villager trades | 01-01 | Forces world exploration for land plot economy |
| Added to tools creative tab | 01-01 | Follows game convention for utility items |
| Bell activation stored on block entity | 01-02 | Follows attachment pattern, per-bell independence |
| UseBlockCallback returns SUCCESS | 01-02 | Preserves vanilla bell behavior (sound, villagers) |
| Book drops at bell center +1Y | 01-02 | Predictable item spawning location |
| SavedDataType with Codec (1.21.11 pattern) | 02-01 | Modern API - no Factory, automatic serialization |
| Chunks stored as Long via ChunkPos.toLong() | 02-01 | Compact storage, fast lookup |
| DataFixTypes.LEVEL for claim data | 02-01 | Appropriate for world-scoped custom data |
| StructureTags.VILLAGE for village detection | 02-02 | Single tag covers all village types without maintenance |
| Multi-position sampling for village detection | 02-02 | Villages span chunks, need reliable detection |
| ValidationResult sealed class | 02-02 | Type-safe results with exhaustive pattern matching |
| Action bar for claim messages | 02-03 | Less intrusive than chat, appropriate for quick feedback |
| Validation order: claimed->village->terrain | 02-03 | Fail fast on simple checks before expensive terrain scan |
| Server access via ServerLevel.server | 03-01 | player.server is private in 1.21.11, cast level instead |
| UseItemCallback returns InteractionResult | 03-01 | Not InteractionResultHolder like item.use() |
| Block allowlist strategy: 34 essential utility blocks | 04-01 | Crafting, storage, lighting across categories for meaningful out-of-base building |
| Adjacency rule = 26 neighboring blocks | 04-01 | "26 coordinates" means 3x3x3 cube minus center, only 26 checks per placement |
| Silent failure for non-allowlist blocks | 04-01 | Per spec PLACE-02/03, return FAIL with no message, players learn through iteration |
| MobEffects.MINING_FATIGUE (1.21 naming) | 04-02 | DIG_SLOWDOWN renamed in Minecraft 1.21, discovered via decompiled class |
| 12-second (240 tick) fatigue duration | 04-02 | Natural decay interval - effect expires and level drops if player stops mining |

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-01-16
Stopped at: Completed 04-02-PLAN.md
Resume file: None
Next: Phase 04-03 (World Restrictions - Village Protections)
