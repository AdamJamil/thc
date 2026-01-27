---
phase: 53-enforcement-foundation
plan: 03
subsystem: enchantment-fire
tags: [flame, fire-aspect, fire-damage, mixin, attachment]

dependency-graph:
  requires: []
  provides:
    - FIRE_SOURCE attachment for fire damage tracking
    - Custom fire damage rates for Flame and Fire Aspect
  affects:
    - Combat damage calculations
    - Enchantment effectiveness

tech-stack:
  added: []
  patterns:
    - Entity attachment for temporary state tracking
    - Mixin injection for damage rate modification
    - Accumulator pattern for fractional damage

key-files:
  created:
    - src/main/java/thc/mixin/LivingEntityFireMixin.java
    - src/main/java/thc/mixin/FlameIgniteMixin.java
    - src/main/java/thc/mixin/FireAspectIgniteMixin.java
  modified:
    - src/main/java/thc/THCAttachments.java
    - src/main/resources/thc.mixins.json

decisions:
  - id: FIRE-01
    choice: "Use accumulator pattern for Fire Aspect 1.5 dmg/s"
    reason: "Minecraft only supports whole number damage, so we alternate 1 and 2 HP per second"
  - id: FIRE-02
    choice: "Set 140 ticks (7s) fire duration for 6 damage ticks"
    reason: "First tick is immune, so 140 ticks = 6 actual damage applications"
  - id: FIRE-03
    choice: "Non-persistent FIRE_SOURCE attachment"
    reason: "Fire is temporary, no need to persist across world save/load"

metrics:
  duration: 25 min
  completed: 2026-01-27
---

# Phase 53 Plan 03: Fire Enchantment Damage Summary

Custom fire damage rates for Flame (6 HP) and Fire Aspect (9 HP) enchantments with source tracking.

## Completed Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add FIRE_SOURCE attachment | 7586482 | THCAttachments.java |
| 2 | Create LivingEntityFireMixin | 7586482 | LivingEntityFireMixin.java |
| 3 | Create fire source tracking mixins | 7586482 | FlameIgniteMixin.java, FireAspectIgniteMixin.java, thc.mixins.json |

## What Was Built

### FIRE_SOURCE Attachment
Added non-persistent attachment to THCAttachments for tracking fire source type:
- Values: "flame", "fire_aspect", or null (normal fire)
- Non-persistent because fire is temporary
- Used by LivingEntityFireMixin to determine damage rate

### LivingEntityFireMixin
Custom fire damage implementation:
- Injects into baseTick() to intercept fire damage ticks
- Flame: No modification needed (vanilla 1 dmg/s)
- Fire Aspect: Uses accumulator pattern for 1.5 dmg/s
  - Accumulates 0.5 extra damage each second
  - Deals extra 1 HP when accumulated >= 1.0
  - Results in alternating 1 HP and 2 HP per second

### FlameIgniteMixin
Tracks Flame arrow hits and sets fire duration:
- Injects into AbstractArrow.doPostHurtEffects()
- Sets FIRE_SOURCE to "flame" when arrow is on fire
- Sets fire duration to 140 ticks (7 seconds for 6 damage ticks)

### FireAspectIgniteMixin
Tracks Fire Aspect melee attacks and sets fire duration:
- Injects into Player.attack() at TAIL
- Sets FIRE_SOURCE to "fire_aspect" if target on fire and no existing source
- Sets fire duration to 140 ticks

## Implementation Details

### Fire Duration Calculation
- Vanilla fire damage occurs when `remainingFireTicks % 20 == 0`
- First tick (tick 140) triggers damage check but entity has immunity
- 140 ticks = 7 seconds = 6 actual damage applications
- This achieves the 6s fire duration requirement

### Accumulator Pattern for Fire Aspect
Fire Aspect needs 1.5 HP/s but Minecraft only supports whole damage:
```
Second 1: vanilla 1 HP + accumulator 0.5 (no extra damage)
Second 2: vanilla 1 HP + accumulator 1.0 -> extra 1 HP, reset to 0
Second 3: vanilla 1 HP + accumulator 0.5 (no extra damage)
...
```
Over 6 seconds: 6 HP vanilla + 3 HP extra = 9 HP total

## Verification

- [x] `./gradlew build` succeeds
- [x] THCAttachments.java contains FIRE_SOURCE attachment
- [x] LivingEntityFireMixin.java exists with accumulator pattern
- [x] FlameIgniteMixin.java exists with arrow tracking
- [x] FireAspectIgniteMixin.java exists with melee tracking
- [x] thc.mixins.json contains all three new mixins

## Success Criteria Met

- [x] LVL-04: Flame deals 1 dmg/s for 6 seconds (6 HP total)
- [x] LVL-05: Fire Aspect deals 1.5 dmg/s for 6 seconds (9 HP total)
- [x] FIRE_SOURCE attachment tracks enchantment type
- [x] Fire duration set correctly for 6 damage ticks

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

Completed:
- LVL-04 (Flame): 6 HP total fire damage
- LVL-05 (Fire Aspect): 9 HP total fire damage

Remaining for Phase 53:
- Additional enchantment enforcements if any remaining plans
