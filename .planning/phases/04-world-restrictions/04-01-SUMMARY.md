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
    - 26-coordinate adjacency enforcement for placement separation
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
    - Chebyshev distance computation (max of |dx|,|dy|,|dz|)

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
    **Adjacency Rule as Chebyshev Distance**: Implemented as 26-coordinate cube
    (max of |dx|,|dy|,|dz| <= 26) = 53x53x53 search space = 148,877 block checks
    per placement attempt. Trade-off: exact interpretation vs performance.
    Acceptable because placement frequency is low and checks are O(n) block state reads.
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
  3. Enforces 26-coordinate adjacency for non-exempt blocks (torches/ladders can be placed together)
  4. Fails silently on restriction violations

  Integration: Registered in THC.onInitialize() after BasePermissions, following
  established event handler pattern. Compilation verified.

one_liner: "Block placement restrictions with 34-block allowlist and 26-coordinate adjacency enforcement"

tasks_completed: 3
commits: 2
files_created: 1
files_modified: 1

duration_minutes: 8

completed: 2026-01-16

performance:
  plan_execution_time_seconds: 480
  average_task_time_seconds: 160
  build_time_seconds: 90

verification_passed:
  - ./gradlew build succeeds without errors
  - WorldRestrictions.kt exists with ALLOWED_BLOCKS set containing 34 blocks
  - ADJACENCY_EXEMPT_BLOCKS defined with torches and ladders
  - UseBlockCallback handler checks base area, allowlist, and adjacency
  - checkAdjacency scans 26-coordinate Chebyshev distance
  - THC.kt imports and registers WorldRestrictions
  - Placement restrictions ready for in-game testing

next_phase_readiness:
  - WorldRestrictions implementation complete and integrated
  - Ready for phase 04-02 (terrain restriction verification and edge cases)
  - No blockers identified
  - Adjacent chunk claiming support remains for phase 05+
