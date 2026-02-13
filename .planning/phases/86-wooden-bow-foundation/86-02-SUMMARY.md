---
phase: 86-wooden-bow-foundation
plan: 02
subsystem: combat
tags: [bow, arrow-damage, tipped-arrow, mixin, projectile]

# Dependency graph
requires:
  - phase: 86-01
    provides: "BowType enum, ProjectileEntityMixin bow type tagging, thc$getBowTypeTag() accessor"
provides:
  - "50% damage multiplier for wooden bow arrows via BowTypeTagAccess interface"
  - "Glowing effect removal from all player arrow hits (DMG-05)"
  - "Tipped arrow restriction on wooden bow with regular arrow fallback"
  - "BowTypeTagAccess duck interface for cross-mixin bow type data"
  - "BowItemMixin for bow-specific arrow selection gating"
affects: [87-blaze-bow, 88-breeze-bow]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "BowTypeTagAccess duck interface for cross-mixin bow type data transfer"
    - "BowItemMixin @Redirect on Player.getProjectile for arrow type gating"
    - "Per-bow damage multiplier via bow type tag lookup"

key-files:
  created:
    - src/main/java/thc/bow/BowTypeTagAccess.java
    - src/main/java/thc/mixin/BowItemMixin.java
  modified:
    - src/main/java/thc/mixin/AbstractArrowMixin.java
    - src/main/java/thc/mixin/ProjectileEntityMixin.java
    - src/main/resources/thc.mixins.json
    - src/main/resources/assets/thc/lang/en_us.json

key-decisions:
  - "BowTypeTagAccess duck interface for cross-mixin data (standard Mixin pattern, avoids reflection)"
  - "TippedArrowItem instanceof check for tipped detection (simpler than PotionContents component check)"
  - "Actionbar message shown only when no regular arrow found (not on every swap)"

patterns-established:
  - "BowTypeTagAccess: duck interface enabling AbstractArrowMixin to read bow type from ProjectileEntityMixin"
  - "@Redirect on Player.getProjectile() for per-bow arrow type restrictions"

# Metrics
duration: 7min
completed: 2026-02-13
---

# Phase 86 Plan 02: Wooden Bow Damage and Restrictions Summary

**50% damage multiplier for wooden bow arrows, Glowing removal from all arrow hits, tipped arrow blocking via inventory search with actionbar feedback**

## Performance

- **Duration:** 7 min
- **Started:** 2026-02-13T02:31:13Z
- **Completed:** 2026-02-13T02:38:44Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Wooden bow arrows deal 50% additional damage reduction (stacking with 0.13 base and class multiplier)
- Glowing effect completely removed from player arrow hits (Speed III remains)
- BowTypeTagAccess duck interface enables clean cross-mixin bow type data transfer
- Tipped arrows cannot be fired from wooden bow -- regular arrow consumed instead
- If player has only tipped arrows and no regular arrows, bow does not fire
- Actionbar message "Your bow can't fire tipped arrows" shown when blocked with no fallback

## Task Commits

Each task was committed atomically:

1. **Task 1: Apply 50% damage for Wooden Bow arrows and remove Glowing** - `7b76d16` (feat)
2. **Task 2: Block tipped arrows from Wooden Bow** - `aaa8765` (feat)

## Files Created/Modified
- `src/main/java/thc/bow/BowTypeTagAccess.java` - Duck interface with `thc$getBowTypeTag()` for cross-mixin bow type access
- `src/main/java/thc/mixin/BowItemMixin.java` - Redirects arrow selection to block tipped arrows for wooden bow
- `src/main/java/thc/mixin/AbstractArrowMixin.java` - Added bow damage multiplier (0.5x for wooden), removed Glowing effect
- `src/main/java/thc/mixin/ProjectileEntityMixin.java` - Implements BowTypeTagAccess interface
- `src/main/resources/thc.mixins.json` - Registered BowItemMixin
- `src/main/resources/assets/thc/lang/en_us.json` - Added tipped arrow blocked translation key

## Decisions Made
- Used BowTypeTagAccess duck interface (standard Mixin pattern) rather than reflection or duplicate fields for cross-mixin bow type access
- Used `instanceof TippedArrowItem` for tipped arrow detection rather than checking PotionContents component -- simpler and more direct
- Actionbar message only shown when no regular arrow fallback exists (avoids spam when silently swapping to regular arrow)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 86 (Wooden Bow Foundation) fully complete
- BowTypeTagAccess and BowType infrastructure ready for phases 87 (Blaze Bow) and 88 (Breeze Bow)
- Future bows can add damage multipliers by adding cases to the bow type tag check in AbstractArrowMixin
- Tipped arrow restriction pattern in BowItemMixin can be extended per-bow-type as needed

## Self-Check: PASSED

All 6 files verified present. Both task commits (7b76d16, aaa8765) verified in git log.

---
*Phase: 86-wooden-bow-foundation*
*Completed: 2026-02-13*
