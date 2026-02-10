---
phase: quick-7
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - src/client/kotlin/thc/client/EffectsHudRenderer.kt
  - src/client/kotlin/thc/THCClient.kt
autonomous: true

must_haves:
  truths:
    - "Effect frames scale correctly at any config percentage without tiling or partial display"
    - "Green duration overlay is 20% opacity (barely visible)"
    - "Effects with infinite duration are not shown in the HUD"
    - "Vanilla status effects are hidden from the top-right HUD but still appear in inventory"
  artifacts:
    - path: "src/client/kotlin/thc/client/EffectsHudRenderer.kt"
      provides: "Fixed blit calls, 20% overlay, infinite filter"
    - path: "src/client/kotlin/thc/THCClient.kt"
      provides: "Vanilla status effects HUD removal"
  key_links:
    - from: "src/client/kotlin/thc/THCClient.kt"
      to: "VanillaHudElements.STATUS_EFFECTS"
      via: "HudElementRegistry.removeElement"
      pattern: "removeElement.*STATUS_EFFECTS"
---

<objective>
Fix four issues with the v3.2 effects GUI HUD:

1. **Scaling math** -- All three `blit` calls use the 10-param overload which conflates render size with source region size, causing tiling when frameSize > texture size and partial display when frameSize < texture size
2. **Green overlay transparency** -- Currently 50% alpha (`0x80`), needs to be 20% (`0x33`)
3. **Infinite effects** -- Effects with infinite duration should be hidden from the HUD
4. **Vanilla HUD effects** -- The vanilla status effects in the top-right corner should be removed (inventory GUI effects are a separate system and remain)

Purpose: Make the effects GUI functional and visually correct at all scale settings.
Output: Two modified files, all four issues fixed.
</objective>

<execution_context>
@/home/tack/.claude/get-shit-done/workflows/execute-plan.md
@/home/tack/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@src/client/kotlin/thc/client/EffectsHudRenderer.kt
@src/client/kotlin/thc/THCClient.kt
@net/minecraft/client/gui/GuiGraphics.java (lines 564-578 for blit overloads)
</context>

<tasks>

<task type="auto">
  <name>Task 1: Fix all four effects GUI issues</name>
  <files>src/client/kotlin/thc/client/EffectsHudRenderer.kt, src/client/kotlin/thc/THCClient.kt</files>
  <action>
**In EffectsHudRenderer.kt:**

**Fix 1 - Scaling math (all three blit calls):**

The root cause: the 10-param `blit` overload at GuiGraphics.java:568 delegates to the 12-param version by setting `sourceWidth = renderWidth` and `sourceHeight = renderHeight`. This means the UV source region scales with the render size, which is WRONG -- the source region must always match the actual texture pixel dimensions.

Switch all three `blit` calls to the 12-param overload:
```
blit(pipeline, texture, x, y, uOffset, vOffset, renderWidth, renderHeight, sourceWidth, sourceHeight, textureWidth, textureHeight)
```

The 12-param overload (line 572) computes UVs as:
- u0 = uOffset / textureWidth
- u1 = (uOffset + sourceWidth) / textureWidth
- v0 = vOffset / textureHeight
- v1 = (vOffset + sourceHeight) / textureHeight
And renders at renderWidth x renderHeight pixels on screen.

**Frame blit (line 118-129):** Change from:
```kotlin
guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_TEXTURE,
    startX, y, 0.0f, 0.0f, frameSize, frameSize,
    BASE_FRAME_SIZE, BASE_FRAME_SIZE)
```
To:
```kotlin
guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_TEXTURE,
    startX, y, 0.0f, 0.0f,
    frameSize, frameSize,           // render size (dynamic)
    BASE_FRAME_SIZE, BASE_FRAME_SIZE, // source region (44x44 = full texture)
    BASE_FRAME_SIZE, BASE_FRAME_SIZE) // texture dimensions (44x44)
```

**Icon blit (line 142-153):** Change from:
```kotlin
guiGraphics.blit(RenderPipelines.GUI_TEXTURED, iconTexture,
    startX + iconOffset, y + iconOffset, 0.0f, 0.0f,
    iconRenderSize, iconRenderSize,
    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE)
```
To:
```kotlin
guiGraphics.blit(RenderPipelines.GUI_TEXTURED, iconTexture,
    startX + iconOffset, y + iconOffset, 0.0f, 0.0f,
    iconRenderSize, iconRenderSize,       // render size (dynamic)
    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE,   // source region (18x18 = full texture)
    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE)   // texture dimensions (18x18)
```

**Numeral blit (line 230-241):** Change from:
```kotlin
guiGraphics.blit(RenderPipelines.GUI_TEXTURED, NUMERALS_TEXTURE,
    numeralX, numeralY, 0.0f,
    (amplifier * NUMERAL_SRC_HEIGHT).toFloat(),
    numeralWidth, numeralHeight,
    NUMERAL_SRC_WIDTH, NUMERAL_SHEET_HEIGHT)
```
To:
```kotlin
guiGraphics.blit(RenderPipelines.GUI_TEXTURED, NUMERALS_TEXTURE,
    numeralX, numeralY, 0.0f,
    (amplifier * NUMERAL_SRC_HEIGHT).toFloat(),
    numeralWidth, numeralHeight,                 // render size (dynamic)
    NUMERAL_SRC_WIDTH, NUMERAL_SRC_HEIGHT,       // source region (13x9 = one numeral)
    NUMERAL_SRC_WIDTH, NUMERAL_SHEET_HEIGHT)     // texture dimensions (13x90 = full sheet)
```

**Fix 2 - Green overlay transparency:**
Change `OVERLAY_COLOR` from `0x8000FF00.toInt()` (50% alpha) to `0x3300FF00.toInt()` (20% alpha).
Update the comment from "50% alpha" to "20% alpha".

**Fix 3 - Hide infinite effects:**
In the `render` function, after sorting, filter out infinite effects BEFORE the rendering loop. Add a filter after line 93 (`val sorted = ...`):
```kotlin
val sorted = activeEffects
    .filter { !it.isInfiniteDuration }
    .sortedWith(effectComparator)
```
Also update the `originalDurations` cleanup to track keys from the filtered list. Since infinite effects are filtered out, they won't appear in `activeKeys` and will naturally be cleaned from `originalDurations`.

Also in `renderDurationOverlay`, the `isInfiniteDuration` branch (returning ratio 1.0f) is now dead code since infinite effects are filtered out. Remove it for clarity -- just handle the finite case directly.

**Fix 4 - Disable vanilla HUD effects:**
In `THCClient.kt`, add after the existing `HudElementRegistry.attachElementAfter(...)` call for the effects HUD (line 76-78):
```kotlin
// Remove vanilla status effects from HUD (inventory GUI effects remain)
HudElementRegistry.removeElement(VanillaHudElements.STATUS_EFFECTS)
```
  </action>
  <verify>Run `./gradlew build` to confirm compilation. If Gradle is already running, inform the user.</verify>
  <done>All four fixes applied: blit calls use 12-param overload with correct source dimensions, overlay is 20% alpha, infinite effects are filtered out before rendering, vanilla HUD effects are removed via Fabric API.</done>
</task>

</tasks>

<verification>
- `./gradlew build` compiles successfully
- In-game: effects display at correct scale without tiling at any slider value (2%-20%)
- In-game: green overlay is barely visible (20% opacity)
- In-game: infinite effects (e.g., from beacons) do not appear in bottom-left HUD
- In-game: no vanilla effect icons in top-right corner
- In-game: inventory screen still shows effect icons on the left side
</verification>

<success_criteria>
All four effects GUI issues are fixed in a single task touching two files.
Build compiles cleanly. No regressions to existing HUD rendering.
</success_criteria>

<output>
After completion, create `.planning/quick/7-fix-v3-2-effects-gui-scaling-math-green-/7-SUMMARY.md`
</output>
