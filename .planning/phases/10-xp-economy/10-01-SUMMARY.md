---
phase: 10-xp-economy
plan: 01
subsystem: gameplay
tags: [mixin, xp, economy, combat-only-xp]

# Dependency graph
requires:
  - phase: 04-world-restrictions
    provides: Mixin patterns and registry structure
provides:
  - XP blocked from ore mining (Block.popExperience cancellation)
  - XP blocked from animal breeding (ExperienceOrb.award redirect)
  - XP blocked from fishing (ExperienceOrb.award redirect)
  - XP blocked from villager trading (rewardTradeXp cancellation)
  - XP blocked from furnace smelting (ExperienceOrb.award redirect)
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "HEAD cancellation for method-level XP blocking"
    - "@Redirect for ExperienceOrb.award interception"

key-files:
  created:
    - src/main/java/thc/mixin/BlockXpMixin.java
    - src/main/java/thc/mixin/AnimalBreedingXpMixin.java
    - src/main/java/thc/mixin/FishingXpMixin.java
    - src/main/java/thc/mixin/VillagerTradeXpMixin.java
    - src/main/java/thc/mixin/FurnaceXpMixin.java
  modified:
    - src/main/resources/thc.mixins.json

key-decisions:
  - "Use complete cancellation (HEAD inject + cancel) rather than zero-value redirects"
  - "Experience bottles remain functional - not blocked"
  - "Target AbstractVillager for trading XP (not Villager class)"

patterns-established:
  - "XP blocking via HEAD cancellation: Block.popExperience, AbstractVillager.rewardTradeXp"
  - "XP blocking via @Redirect: ExperienceOrb.award() calls in Animal, FishingHook, AbstractFurnaceBlockEntity"

# Metrics
duration: 6min
completed: 2026-01-19
---

# Phase 10 Plan 01: XP Economy Restriction Summary

**Five mixins block all passive XP sources (ores, breeding, fishing, trading, furnaces) while preserving combat XP and experience bottles**

## Performance

- **Duration:** 6 min
- **Started:** 2026-01-19T19:46:36Z
- **Completed:** 2026-01-19T19:53:02Z
- **Tasks:** 6
- **Files modified:** 6

## Accomplishments
- Ore mining XP completely blocked via Block.popExperience HEAD cancellation
- Animal breeding XP blocked via ExperienceOrb.award redirect in Animal.finalizeSpawnChildFromBreeding
- Fishing XP blocked via ExperienceOrb.award redirect in FishingHook.retrieve
- Villager trading XP blocked via AbstractVillager.rewardTradeXp HEAD cancellation
- Furnace smelting XP blocked via ExperienceOrb.award redirect in AbstractFurnaceBlockEntity.createExperience
- Experience bottles remain fully functional (not blocked)

## Task Commits

Each task was committed atomically:

1. **Task 1: Block ore mining XP** - `9aeb7bf` (feat)
2. **Task 2: Block animal breeding XP** - `07c4da4` (feat)
3. **Task 3: Block fishing XP** - `70f381c` (feat)
4. **Task 4: Block villager trading XP** - `0bf41f4` (feat)
5. **Task 5: Block furnace smelting XP** - `6c95e54` (feat)
6. **Task 6: Register all XP mixins** - `5b6abb5` (chore)

## Files Created/Modified
- `src/main/java/thc/mixin/BlockXpMixin.java` - Blocks ore mining XP via popExperience HEAD cancel
- `src/main/java/thc/mixin/AnimalBreedingXpMixin.java` - Blocks breeding XP via ExperienceOrb.award redirect
- `src/main/java/thc/mixin/FishingXpMixin.java` - Blocks fishing XP via ExperienceOrb.award redirect
- `src/main/java/thc/mixin/VillagerTradeXpMixin.java` - Blocks trading XP via rewardTradeXp HEAD cancel
- `src/main/java/thc/mixin/FurnaceXpMixin.java` - Blocks smelting XP via ExperienceOrb.award redirect
- `src/main/resources/thc.mixins.json` - Registered all 5 new XP blocking mixins

## Decisions Made
- **Complete cancellation approach:** Used HEAD injection with cancel for method-level blocking (Block.popExperience, AbstractVillager.rewardTradeXp) and @Redirect to no-op for ExperienceOrb.award calls within methods (Animal, FishingHook, AbstractFurnaceBlockEntity)
- **Experience bottles preserved:** Intentionally did NOT create ExperienceBottleXpMixin - bottles are rare/expensive items representing earned gameplay
- **AbstractVillager target:** Used AbstractVillager class (net.minecraft.world.entity.npc.villager.AbstractVillager) instead of Villager, following existing mixin pattern

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Initial VillagerTradeXpMixin compilation failed due to incorrect import path (`net.minecraft.world.entity.npc.Villager` does not exist in this Minecraft version). Fixed by using `net.minecraft.world.entity.npc.villager.AbstractVillager` based on existing AbstractVillagerMixin pattern.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- XP economy restriction complete
- Players can only gain XP from combat (mob kills) or experience bottles
- Ready for testing in-game to verify all blocked sources

---
*Phase: 10-xp-economy*
*Completed: 2026-01-19*
