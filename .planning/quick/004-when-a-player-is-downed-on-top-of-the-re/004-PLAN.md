---
quick: 004
type: execute
description: Add red beacon light at downed player location
autonomous: true
files_modified:
  - src/client/kotlin/thc/client/DownedBodyRenderer.kt
  - src/client/java/thc/client/BeaconBeamHelper.java
---

<objective>
Add a red beacon light beam at downed player locations that disappears on revival.

Purpose: Beacon beams are visible from much greater distances than particles, making it easier to locate downed teammates in the world.
Output: Red beacon beam renders at each downed player location, visible until revival.
</objective>

<context>
@.planning/PROJECT.md
@src/client/kotlin/thc/client/DownedBodyRenderer.kt — existing downed renderer
@src/client/java/thc/client/DownedPlayersClientState.java — tracks downed player positions
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create beacon beam rendering helper</name>
  <files>src/client/java/thc/client/BeaconBeamHelper.java</files>
  <action>
Create a Java helper class to render beacon-style beams at arbitrary world positions.

The helper should:
1. Accept PoseStack, WorldRenderContext, position (x,y,z), and color (ARGB int)
2. Use RenderTypes.beaconBeam() with Minecraft's vanilla beacon_beam.png texture
3. Render two overlapping rotated quad planes (inner beam + outer glow)
4. Beam should extend from ground (y position) up to build limit (~320 blocks)
5. Use semi-transparent rendering for the glow layer

Key implementation details:
- Texture location: Identifier.withDefaultNamespace("textures/entity/beacon_beam.png")
- Inner beam: Solid color, thin (~0.2 blocks wide)
- Outer glow: Translucent, wider (~0.5 blocks wide)
- Both beams use animated UV scrolling based on game time for shimmer effect
- Vertex format: position, color, UV, overlay, light, normal

Reference vanilla beacon rendering approach:
- Two layers: inner (opaque) and outer (translucent)
- Each layer is 4 quads forming a rectangular prism
- UV scrolling creates the animated shimmer

Color parameter should allow specifying red (0xFFFF0000) for downed players.
  </action>
  <verify>
Class compiles without errors: `./gradlew compileJava`
  </verify>
  <done>BeaconBeamHelper.java exists with renderBeam() method accepting position, color, and render context</done>
</task>

<task type="auto">
  <name>Task 2: Integrate beacon beam into DownedBodyRenderer</name>
  <files>src/client/kotlin/thc/client/DownedBodyRenderer.kt</files>
  <action>
Modify the existing AFTER_ENTITIES callback in DownedBodyRenderer to also render a red beacon beam at each downed player location.

In the WorldRenderEvents.AFTER_ENTITIES callback, after rendering the downed body:
1. Call BeaconBeamHelper.renderBeam() for each downed player
2. Pass the player's position (info.x, info.y, info.z)
3. Pass red color: 0xFFFF0000 (or a more appropriate red like 0xFFE61919 to match particles)
4. Pass the PoseStack and WorldRenderContext

The beam should render at the same position as the particle effects and body, extending upward.

Since downed players are tracked in DownedPlayersClientState and removed on revival, the beam will automatically disappear when the player is revived (same lifecycle as existing particles and body rendering).
  </action>
  <verify>
Build succeeds: `./gradlew build`
  </verify>
  <done>DownedBodyRenderer calls BeaconBeamHelper.renderBeam() for each downed player in the AFTER_ENTITIES callback</done>
</task>

<task type="auto">
  <name>Task 3: Test beacon beam rendering</name>
  <files>N/A</files>
  <action>
Run the game client and test the beacon beam rendering:

1. Start the game with `./gradlew runClient`
2. Enter a world and take lethal damage to enter downed state
3. Observe:
   - Red beacon beam appears at downed location
   - Beam extends upward into the sky
   - Existing red particles still render
   - Downed body still renders
4. Have another player (or use /revive command if available) revive you
5. Verify beacon beam disappears on revival

If no /revive command exists, test by:
- Observing the beacon appears on death
- Observing it's visible from far away
- Reloading world (which should clear downed state)
  </action>
  <verify>
Manual verification: beacon beam renders at downed player location and disappears on state change
  </verify>
  <done>Red beacon beam visible at downed player location, disappears on revival or state clear</done>
</task>

</tasks>

<verification>
- [ ] BeaconBeamHelper.java compiles and provides renderBeam() method
- [ ] DownedBodyRenderer.kt calls beacon rendering in AFTER_ENTITIES
- [ ] Red beacon beam visible at downed player locations
- [ ] Beam disappears when player is revived
- [ ] Existing particle effects and body rendering still work
</verification>

<success_criteria>
- Downed players have a red beacon light beam at their location
- Beam is visible from significant distance (purpose is long-range visibility)
- Beam disappears on revival
- No performance regression from beacon rendering
</success_criteria>

<output>
After completion, create `.planning/quick/004-when-a-player-is-downed-on-top-of-the-re/004-SUMMARY.md`
</output>
