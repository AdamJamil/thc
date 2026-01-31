---
type: quick
id: "002"
description: Auto-assign villager jobs on block place
files_modified:
  - src/main/java/thc/villager/AllowedProfessions.java
  - src/main/kotlin/thc/villager/JobBlockAssignment.kt
  - src/main/kotlin/thc/THC.kt
autonomous: true
---

<objective>
When a player places a job block (stonecutter, smoker, cartography table, or lectern), automatically assign the corresponding profession to the nearest unemployed villager within 5 blocks.

Purpose: Streamlines villager job assignment without requiring players to wait for AI pathfinding or manage POI acquisition.
Output: JobBlockAssignment.kt handler registered in mod init.
</objective>

<context>
@.planning/PROJECT.md
@src/main/java/thc/villager/AllowedProfessions.java (profession constants, getNoneHolder)
@src/main/kotlin/thc/villager/VillagerInteraction.kt (existing villager interaction patterns)
@src/main/kotlin/thc/world/WorldRestrictions.kt (UseBlockCallback pattern for block placement)
@src/main/kotlin/thc/THC.kt (registration pattern)
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add job block to profession mapping</name>
  <files>src/main/java/thc/villager/AllowedProfessions.java</files>
  <action>
Add a static map from Block to ResourceKey<VillagerProfession> for the 4 allowed job blocks:
- Blocks.STONECUTTER -> VillagerProfession.MASON
- Blocks.SMOKER -> VillagerProfession.BUTCHER
- Blocks.CARTOGRAPHY_TABLE -> VillagerProfession.CARTOGRAPHER
- Blocks.LECTERN -> VillagerProfession.LIBRARIAN

Add a public static method `getProfessionForJobBlock(Block block)` that returns the profession key or null if not a job block.
  </action>
  <verify>File compiles: `./gradlew compileJava --quiet`</verify>
  <done>AllowedProfessions.java has job block mapping and lookup method.</done>
</task>

<task type="auto">
  <name>Task 2: Create job block placement handler</name>
  <files>src/main/kotlin/thc/villager/JobBlockAssignment.kt</files>
  <action>
Create JobBlockAssignment.kt as a Kotlin object with:

1. `register()` function that registers a UseBlockCallback.EVENT handler

2. Handler logic:
   - Skip if client side (world.isClientSide)
   - Check if placed item is a BlockItem
   - Get the block being placed via (item as BlockItem).block
   - Call AllowedProfessions.getProfessionForJobBlock(block)
   - If null (not a job block), return PASS
   - Get placement position: hitResult.blockPos.relative(hitResult.direction)
   - Find nearest unemployed villager within 5 blocks:
     - Use level.getEntitiesOfClass(Villager::class.java, AABB.ofSize(pos.center, 10.0, 10.0, 10.0))
     - Filter to villagers where villagerData.profession matches NONE
     - Sort by distance to placement pos
     - Take first or return PASS if none found
   - Assign profession:
     - Get profession Holder from registry: level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(professionKey)
     - Set villager data: villager.villagerData = villager.villagerData.withProfession(profHolder)
   - Play feedback: SoundEvents.VILLAGER_WORK_MASON (or generic VILLAGER_YES) and HAPPY_VILLAGER particles
   - Return PASS (let vanilla block placement proceed)

Note: Return PASS always - we're adding behavior, not blocking placement. Use ServerLevel cast for sendParticles.
  </action>
  <verify>File compiles: `./gradlew compileKotlin --quiet`</verify>
  <done>JobBlockAssignment.kt created with UseBlockCallback handler that finds nearest unemployed villager and assigns profession.</done>
</task>

<task type="auto">
  <name>Task 3: Register handler in mod init</name>
  <files>src/main/kotlin/thc/THC.kt</files>
  <action>
1. Add import: import thc.villager.JobBlockAssignment
2. Call JobBlockAssignment.register() in onInitialize(), after VillagerInteraction.register()
  </action>
  <verify>Full build: `./gradlew build --quiet` succeeds</verify>
  <done>JobBlockAssignment registered during mod initialization.</done>
</task>

</tasks>

<verification>
- `./gradlew build` passes
- In-game test: Place stonecutter near unemployed villager, verify villager becomes mason with particles/sound
</verification>

<success_criteria>
- Placing any of the 4 job blocks assigns the corresponding profession to the nearest unemployed villager within 5 blocks
- Only unemployed villagers (profession = NONE) are affected
- Existing villagers with jobs are not changed
- Particles and sound provide feedback when assignment occurs
</success_criteria>
