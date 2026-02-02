---
phase: 73-01
title: "Revival Progress System"
subsystem: downed
tags: [revival, tick-processing, attachments, class-bonus]

dependency-graph:
  requires:
    - 72-01: Downed state foundation (DownedState, DOWNED_LOCATION)
  provides:
    - REVIVAL_PROGRESS attachment for tracking progress on downed players
    - RevivalState accessor with get/set/add/clear methods
    - Server tick processor for progress accumulation
    - Class-based progress rate (Support 2x speed)
  affects:
    - 73-02: Revival completion (will check progress >= 1.0)
    - 74: Revival UI (will sync progress to client)

tech-stack:
  added: []
  patterns:
    - State accessor pattern (RevivalState follows BucklerState)
    - Server tick processing (processRevival in END_SERVER_TICK)
    - Squared distance check (distanceToSqr for performance)

file-tracking:
  key-files:
    created:
      - src/main/java/thc/downed/RevivalState.java
    modified:
      - src/main/java/thc/THCAttachments.java
      - src/main/kotlin/thc/THC.kt

decisions:
  - decision: "Store progress on downed player"
    rationale: "Multiple revivers naturally stack by adding to same value"
    outcome: "Simplifies multi-reviver support"
  - decision: "0.01 rate for Support, 0.005 for others"
    rationale: "Corresponds to 5s and 10s revival times at 20 ticks/sec"
    outcome: "Support class gets meaningful 2x speed bonus"
  - decision: "Use distanceToSqr with 4.0 threshold"
    rationale: "Avoids sqrt operation; 4.0 = 2 blocks squared"
    outcome: "Efficient proximity check"

metrics:
  duration: "4 min"
  completed: "2026-02-02"
---

# Phase 73 Plan 01: Revival Progress System Summary

Revival progress accumulates when alive players sneak within 2 blocks of downed location, with Support class getting 2x speed.

## What Was Built

### REVIVAL_PROGRESS Attachment
Non-persistent Double attachment (0.0 to 1.0) stored on downed player. Session-scoped - does not survive server restart.

```java
// THCAttachments.java
public static final AttachmentType<Double> REVIVAL_PROGRESS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "revival_progress"),
    builder -> builder.initializer(() -> 0.0D)
);
```

### RevivalState Accessor
Follows BucklerState pattern with get/set/add/clear methods:

```java
public static double getProgress(ServerPlayer player)
public static void setProgress(ServerPlayer player, double value)
public static void addProgress(ServerPlayer player, double amount)  // Capped at 1.0
public static void clearProgress(ServerPlayer player)
```

### Server Tick Processor
Added `processRevival(server)` to END_SERVER_TICK handler. Logic:

1. Filter to find all downed players (early exit if none)
2. For each non-downed player who is sneaking:
   - Determine progress rate: 0.01 for Support, 0.005 for others
   - Check proximity to each downed player (2 blocks = 4.0 squared)
   - Call `RevivalState.addProgress(downed, progressRate)` if in range

Multi-reviver stacking happens naturally: two revivers = two `addProgress` calls per tick = 2x progress rate. Server tick is single-threaded so no thread safety concerns.

## Key Behaviors

| Behavior | Implementation |
|----------|----------------|
| Proximity | `reviver.position().distanceToSqr(downedLoc) <= 4.0` |
| Sneaking | `reviver.isShiftKeyDown` |
| Support 2x | `ClassManager.getClass(reviver) == PlayerClass.SUPPORT` |
| Multi-reviver | Natural stacking via addProgress per reviver per tick |
| Progress persistence | Does not reset when reviver leaves |

## Progress Rates

| Class | Rate/tick | Time to 100% |
|-------|-----------|--------------|
| Support | 0.01 | 5 seconds (100 ticks) |
| Non-Support | 0.005 | 10 seconds (200 ticks) |
| 2 Non-Support | 0.01 | 5 seconds |
| Support + Non-Support | 0.015 | 3.3 seconds |

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| f6c2ef5 | feat | Add REVIVAL_PROGRESS attachment and RevivalState accessor |
| 429d94f | feat | Add server tick processor for revival progress |

## Next Phase Readiness

**73-02: Revival Completion** can now:
- Check `RevivalState.getProgress(downed) >= 1.0` to trigger completion
- Clear progress with `RevivalState.clearProgress(downed)` on revival
- Access downed location via existing `DownedState.getDownedLocation(downed)`

**Phase 74: Revival UI** can sync progress to client for radial progress display.
