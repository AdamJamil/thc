# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-15)

**Core value:** Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.
**Current focus:** Phase 5 - Testing (after bugfix phases)

## Current Position

Phase: 4.2 of 5 (Bug Fixes 2 - COMPLETE)
Plan: 1 of 1 completed
Status: Phase 4.2 complete, ready for Phase 5
Last activity: 2026-01-16 - Completed 04.2-01 bugfix plan

Progress: ████████████░ 92% (12/13 plans completed)

## Performance Metrics

**Velocity:**
- Total plans completed: 12
- Average duration: 4.8 min
- Total execution time: 1.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-land-plot-system | 2 | 6min | 3min |
| 02-chunk-claiming-core | 3 | 16min | 5.3min |
| 03-base-area-permissions | 1 | 6min | 6min |
| 04-world-restrictions | 4 | 21min | 5.25min |
| 04.1-bugfixes | 1 | 3min | 3min |
| 04.2-bugfixes-2 | 1 | 4min | 4min |

**Recent Trend:**
- Last 5 plans: 04-02 (6min), 04-03 (5min), 04-04 (2min), 04.1-01 (3min), 04.2-01 (4min)
- Trend: Stable (4.0min avg)

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
| Ore detection via BlockTags | 04-03 | Vanilla tags cover all ore variants (regular + deepslate) without maintenance |
| VillageProtection before MiningFatigue | 04-03 | Handler order matters - blocked breaks shouldn't trigger fatigue |
| Check effect.duration <= 1 for expiration | 04-04 | Catches imminent expiration before effect is removed by game |
| Remove/reapply for decay | 04-04 | Effect can't be modified in place, must remove and add new at lower amplifier |
| MC 1.21+ items need both model and item definition | 04.1-01 | models/item/*.json for rendering, items/*.json for item system |
| BellProtection registered before BellHandler | 04.1-01 | Early event chain interception ensures protection |
| sendAllDataToRemote() for inventory sync | 04.1-01 | More reliable than broadcastChanges() for immediate correction |
| chunk.allStarts + center sampling for village detection | 04.1-01 | Hybrid approach catches both origins and extended structures |
| Target BlockBehaviour not BellBlock for getDestroyProgress | 04.2-01 | BellBlock doesn't override method, must inject at parent class |
| 63-point sampling (9 positions x 7 Y levels) for villages | 04.2-01 | Comprehensive coverage of village chunks at all typical heights |
| Mixin over event handler for bell protection | 04.2-01 | Prevents mining animation from starting rather than canceling mid-flight |

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

## Session Continuity

Last session: 2026-01-16
Stopped at: Completed 04.2-01 bugfix plan
Resume file: None
Next: Phase 5 Testing
