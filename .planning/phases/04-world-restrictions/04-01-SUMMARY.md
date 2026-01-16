---
phase: 04
plan: 01
type: execute
subsystem: world-mechanics
tags: [block-placement, allowlist, adjacency-restrictions]

dependency_graph:
  requires:
    - phase: 02
      plan: 01
      provides: ClaimManager and claim persistence
    - phase: 03
      plan: 01
      provides: BasePermissions and event pattern
  provides:
    - Block placement restrictions outside base areas
    - 34-block allowlist for world building
    - Adjacency enforcement (cannot place allowlist blocks next to each other)
  affects:
    - Future phases: Territorial progression mechanics
    - Gameplay: Player base expansion restrictions

tech_stack:
  added:
    - UseBlockCallback Fabric event (player.UseBlockEvent pattern)
    - BlockItem.block field access for placement validation
  patterns:
    - Fabric event registration (UseBlockCallback.EVENT.register)
    - ServerLevel casting from World for server access
    - 26-neighbor adjacency check (3x3x3 cube minus center)

key_files:
  created:
    - src/main/kotlin/thc/world/WorldRestrictions.kt (159 lines)
  modified:
    - src/main/kotlin/thc/THC.kt (2 lines added)

decisions_made:
  - |
    **Block Allowlist Strategy**: 34 essential utility blocks across categories
    (anvils, crafting stations, storage, lighting) for meaningful out-of-base building.
    Torches and ladders exempt from adjacency to support utility placement.
  - |
    **Adjacency Rule as 26 Neighboring Blocks**: "26 coordinates" means the 26
    positions adjacent to a block (3x3x3 cube minus center = face + edge + corner
    neighbors). Only 26 block checks per placement - very efficient. This prevents
    allowlist blocks from being placed directly adjacent to each other.
  - |
    **Silent Failure for Non-Allowlist Blocks**: Per PLACE-02 spec, return
    InteractionResult.FAIL with no message. Consistent with PLACE-03 (silent failure).
    Players learn through iteration.
  - |
    **Base Area Override**: All blocks allowed inside base chunks (delegated to
    BasePermissions). Placement restrictions apply only outside bases, creating
    risk/reward trade-off: base building is unrestricted but expensive, outside
    building is restricted but fast to deploy.

summary: |
  Implemented block placement restrictions enforcing risk-based territorial
  progression. WorldRestrictions singleton registers UseBlockCallback handler that:
  1. Allows all blocks inside base chunks (via ClaimManager.isInBase check)
  2. Restricts non-base placement to 34 allowlist blocks (crafting stations, storage, utilities)
  3. Enforces adjacency for non-exempt blocks - cannot place next to another allowlist block (26 neighbors)
  4. Fails silently on restriction violations

  The "26 coordinates" adjacency rule means the 26 neighboring positions (3x3x3 cube - center).

  Integration: Registered in THC.onInitialize() after BasePermissions, following
  established event handler pattern. Compilation verified.

one_liner: "Block placement restrictions with 34-block allowlist and 26-coordinate adjacency enforcement"

tasks_completed: 3
commits: 3
files_created: 1
files_modified: 1

duration_minutes: 10

completed: 2026-01-16

performance:
  plan_execution_time_seconds: 600
  average_task_time_seconds: 200
  build_time_seconds: 90

deviations:
  - |
    **[Rule 1 - Bug] Fixed critical adjacency check range**
    - Found during: Re-execution verification
    - Issue: checkAdjacency used range -26..26, checking 148,876 blocks (53^3-1) instead of 26
    - Fix: Changed to -1..1 range per plan spec (26 neighboring blocks = 3x3x3 cube minus center)
    - Files modified: src/main/kotlin/thc/world/WorldRestrictions.kt
    - Commit: cf90778

verification_passed:
  - ./gradlew build succeeds without errors
  - WorldRestrictions.kt exists with ALLOWED_BLOCKS set containing 34 blocks
  - ADJACENCY_EXEMPT_BLOCKS defined with torches and ladders
  - UseBlockCallback handler checks base area, allowlist, and adjacency
  - checkAdjacency scans 26 neighboring blocks (3x3x3 cube minus center) using -1..1 range
  - THC.kt imports and registers WorldRestrictions
  - Placement restrictions ready for in-game testing

next_phase_readiness:
  - WorldRestrictions implementation complete and integrated
  - Ready for phase 04-02 (terrain restriction verification and edge cases)
  - No blockers identified
  - Adjacent chunk claiming support remains for phase 05+
