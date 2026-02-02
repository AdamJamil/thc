# Phase 74: Revival UI - Research

**Researched:** 2026-02-02
**Domain:** Client-side HUD rendering, radial progress visualization
**Confidence:** MEDIUM

## Summary

This phase implements a radial progress ring displayed around the cursor when a player looks at a downed location within revival range. The UI must show revival progress filling clockwise from 12 o'clock using two existing textures (empty and filled rings).

Research identified three viable rendering approaches:
1. **Vertex-based radial fill** (recommended) - Draw the filled texture as a pie/triangle-fan, calculating vertex positions based on progress angle
2. **Shader-based masking** - Use a custom fragment shader with atan2 to discard pixels outside the progress angle
3. **Pre-rendered sprite strip** - Create multiple texture frames and select based on progress (requires new textures, against requirements)

The project already has established patterns for HUD rendering (BucklerHudRenderer) and client-server state sync (BucklerClientState/BucklerStatePayload) that should be followed.

**Primary recommendation:** Use vertex-based triangle-fan rendering to draw the filled texture as a radial pie shape, layered over the empty texture. This avoids shader complexity while using only the existing textures.

## Standard Stack

### Core

| Library/API | Version | Purpose | Why Standard |
|-------------|---------|---------|--------------|
| Fabric HudElementRegistry | 0.116+ | Register HUD overlays | Project already uses this for poise bar |
| GuiGraphics | MC 1.21.11 | Texture rendering, matrix transforms | Standard Minecraft GUI rendering |
| BufferBuilder/Tesselator | MC 1.21.11 | Custom vertex geometry | Required for non-rectangular shapes |
| RenderPipelines.GUI_TEXTURED | MC 1.21.11 | Texture rendering pipeline | Standard for GUI textures |

### Supporting

| Library/API | Version | Purpose | When to Use |
|-------------|---------|---------|-------------|
| Minecraft.getInstance() | MC 1.21.11 | Access crosshairTarget for raycast | Determine what player is looking at |
| ClientPlayNetworking | Fabric API | Receive state from server | Sync downed player locations and progress |
| VertexFormat.POSITION_TEX | MC 1.21.11 | Texture-mapped vertices | For custom radial geometry |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Vertex-based radial | Custom shader | Shader more elegant but harder to maintain, debug |
| HudElementRegistry | HudRenderCallback | HudRenderCallback deprecated since Fabric API 0.116 |
| Triangle fan geometry | Sprite sheet animation | Would require creating new textures (violates requirements) |

## Architecture Patterns

### Recommended Structure

```
src/client/
├── kotlin/thc/client/
│   ├── RevivalProgressRenderer.kt    # HUD rendering logic
│   └── RevivalClientState.kt         # Client-side state cache
src/main/
├── kotlin/thc/network/
│   └── RevivalStatePayload.kt        # Server→client state sync
```

### Pattern 1: Client State Cache (Existing Pattern)

**What:** Static singleton holding server-synced state for client rendering
**When to use:** When server state needs to drive client-side rendering
**Example (from existing code):**

```kotlin
// Source: thc/client/BucklerClientState.java (existing pattern)
object RevivalClientState {
    private var targetDownedUUID: UUID? = null
    private var progress: Float = 0f

    fun update(uuid: UUID?, newProgress: Float) {
        targetDownedUUID = uuid
        progress = newProgress
    }

    fun getProgress(): Float = progress
    fun getTargetUUID(): UUID? = targetDownedUUID
}
```

### Pattern 2: HUD Element Registration (Existing Pattern)

**What:** Register HUD elements using Fabric's HudElementRegistry
**When to use:** All HUD overlays
**Example (from existing code):**

```kotlin
// Source: thc/THCClient.kt (existing pattern)
HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, REVIVAL_PROGRESS_ID) { guiGraphics, _ ->
    RevivalProgressRenderer.render(guiGraphics)
}
```

### Pattern 3: Vertex-Based Radial Fill

**What:** Draw a texture as a pie/fan shape using custom vertices
**When to use:** Radial progress indicators with textures
**Example:**

```kotlin
// Pseudocode for radial fill approach
fun renderRadialFill(graphics: GuiGraphics, centerX: Int, centerY: Int, progress: Float) {
    // 1. Render empty ring as background (full texture)
    graphics.blit(RenderPipelines.GUI_TEXTURED, EMPTY_TEXTURE, ...)

    // 2. Render filled ring as pie based on progress
    val angle = progress * 2 * PI  // 0 to 2PI
    val segments = calculatePieSegments(centerX, centerY, radius, 0, angle)
    renderTexturedPie(graphics, FULL_TEXTURE, segments)
}
```

### Anti-Patterns to Avoid

- **Using HudRenderCallback:** Deprecated since Fabric API 0.116, use HudElementRegistry instead
- **Raycast every frame:** crosshairTarget is already computed by Minecraft, use it directly
- **Server-side rendering logic:** All UI calculations must happen client-side based on synced state

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| What player is looking at | Custom raycast | `Minecraft.getInstance().crosshairTarget` | Already computed each frame |
| HUD overlay registration | Custom render hooks | HudElementRegistry | Proper layering, deprecation-safe |
| Texture rendering | OpenGL calls | GuiGraphics.blit() | Handles render state, pipelines |

**Key insight:** Minecraft already computes the crosshair target every frame. Using `crosshairTarget` is free; doing a custom raycast is expensive and redundant.

## Common Pitfalls

### Pitfall 1: Rendering Outside Extraction Phase

**What goes wrong:** Attempting to render during wrong phase causes blank output or crashes
**Why it happens:** MC 1.21.6+ separates extraction (prepare data) from drawing (submit to GPU)
**How to avoid:** Use HudElementRegistry callbacks which are invoked during correct phase
**Warning signs:** Elements render blank or throw OpenGL errors

### Pitfall 2: Wrong Vertex Format Order

**What goes wrong:** Garbled or invisible geometry
**Why it happens:** BufferBuilder methods must be called in exact order matching VertexFormat
**How to avoid:** For POSITION_TEX: always `.addVertex(matrix, x, y, z).setUv(u, v)` in that order
**Warning signs:** Vertices appear at wrong positions, textures look wrong

### Pitfall 3: Crosshair Target Type Confusion

**What goes wrong:** NullPointerException or ClassCastException
**Why it happens:** crosshairTarget can be BlockHitResult, EntityHitResult, or miss
**How to avoid:** Always check `hit.getType()` before casting
**Warning signs:** Crashes when looking at sky or different target types

### Pitfall 4: Coordinate System Confusion

**What goes wrong:** Elements appear at wrong screen position
**Why it happens:** Screen center is (width/2, height/2), not (0, 0)
**How to avoid:** Calculate center from `guiGraphics.guiWidth()` and `guiGraphics.guiHeight()`
**Warning signs:** Element in corner instead of center

### Pitfall 5: Z-Fighting with Crosshair

**What goes wrong:** Progress ring flickers or is obscured
**Why it happens:** Same z-depth as vanilla crosshair
**How to avoid:** Use `pose().translate(0, 0, z)` to set appropriate depth, or register with correct layer ordering
**Warning signs:** Flickering, partial visibility

## Code Examples

### Registering HUD Element

```kotlin
// Source: Based on existing THCClient.kt pattern
HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, REVIVAL_PROGRESS_ID) { guiGraphics, _ ->
    RevivalProgressRenderer.render(guiGraphics)
}
```

### Getting Crosshair Target

```kotlin
// Source: Fabric Wiki pixel_raycast + MC source
val client = Minecraft.getInstance()
val hit = client.crosshairTarget ?: return

when (hit.type) {
    HitResult.Type.MISS -> { /* nothing targeted */ }
    HitResult.Type.BLOCK -> {
        val blockHit = hit as BlockHitResult
        val pos = blockHit.blockPos
        // Check if pos matches a downed player location
    }
    HitResult.Type.ENTITY -> {
        // Not used - downed players are in spectator, tracked by position
    }
}
```

### Rendering Centered Texture

```kotlin
// Source: Based on existing BucklerHudRenderer.kt
fun render(guiGraphics: GuiGraphics) {
    val client = Minecraft.getInstance()
    if (client.options.hideGui || client.player?.isSpectator == true) return

    val centerX = guiGraphics.guiWidth() / 2
    val centerY = guiGraphics.guiHeight() / 2
    val size = 32  // Texture is 32x32
    val halfSize = size / 2

    guiGraphics.blit(
        RenderPipelines.GUI_TEXTURED,
        TEXTURE_ID,
        centerX - halfSize,  // x
        centerY - halfSize,  // y
        0.0f, 0.0f,          // u, v
        size, size,           // width, height
        32, 32                // texture dimensions
    )
}
```

### Radial Fill with Triangle Fan

```kotlin
// Conceptual approach - needs adaptation to MC 1.21.11 rendering API
fun renderRadialProgress(graphics: GuiGraphics, progress: Float, centerX: Int, centerY: Int, radius: Int) {
    // Render empty background
    blitCentered(graphics, EMPTY_TEXTURE, centerX, centerY)

    if (progress <= 0f) return

    val tesselator = Tesselator.getInstance()
    val builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX)
    val matrix = graphics.pose().last().pose()

    // Center vertex
    builder.addVertex(matrix, centerX.toFloat(), centerY.toFloat(), 0f).setUv(0.5f, 0.5f)

    // Arc vertices (clockwise from top)
    val startAngle = -PI / 2  // 12 o'clock
    val endAngle = startAngle + (progress * 2 * PI)
    val steps = (progress * 32).toInt().coerceAtLeast(3)

    for (i in 0..steps) {
        val angle = startAngle + (endAngle - startAngle) * i / steps
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        val u = 0.5f + 0.5f * cos(angle).toFloat()
        val v = 0.5f + 0.5f * sin(angle).toFloat()
        builder.addVertex(matrix, x, y, 0f).setUv(u, v)
    }

    BufferUploader.drawWithShader(builder.buildOrThrow())
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| HudRenderCallback | HudElementRegistry | Fabric API 0.116 | Must migrate to new API |
| Direct OpenGL calls | BufferBuilder | MC 1.17+ | Cannot use glBegin/glEnd |
| tessellator.draw() | BufferRenderer.drawWithGlobalProgram(builder.end()) | MC 1.21+ | API change |
| builder.begin() | tessellator.begin() returns builder | MC 1.21+ | API change |

**Deprecated/outdated:**
- HudRenderCallback: Deprecated, use HudElementRegistry
- Direct Tesselator manipulation in GUI: GuiGraphics handles most cases

## Open Questions

1. **Exact vertex format for radial fill**
   - What we know: POSITION_TEX should work for textured geometry
   - What's unclear: Whether GuiGraphics provides helper methods or if raw BufferBuilder is needed
   - Recommendation: Start with BufferBuilder approach, simplify if helpers exist

2. **Scale tuning**
   - What we know: User wants to iterate on scale value
   - What's unclear: Best initial value
   - Recommendation: Start with 1.0x (32x32 pixels), expose as constant for easy tuning

3. **"Looking at downed location" detection**
   - What we know: Downed players are in spectator mode, tracked by position
   - What's unclear: Whether to check crosshairTarget block position or do distance check from look direction
   - Recommendation: Calculate look direction intersection with downed position's Y-plane, check if within radius

## Sources

### Primary (HIGH confidence)
- Fabric HUD Rendering Documentation: https://docs.fabricmc.net/develop/rendering/hud
- Fabric Drawing to GUI: https://docs.fabricmc.net/develop/rendering/draw-context
- Fabric Basic Rendering Concepts: https://docs.fabricmc.net/develop/rendering/basic-concepts
- Existing codebase: BucklerHudRenderer.kt, THCClient.kt (established patterns)

### Secondary (MEDIUM confidence)
- Fabric Wiki Pixel Raycast: https://wiki.fabricmc.net/tutorial:pixel_raycast
- GuiGraphics NeoForge Javadoc (API reference): https://lexxie.dev/neoforge/1.21.1/net/minecraft/client/gui/GuiGraphics.html

### Tertiary (LOW confidence)
- General GLSL radial shader approaches (for potential future optimization)
- Community discussions on circular GUI rendering

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Project has established patterns, Fabric docs are current
- Architecture: HIGH - Following existing project patterns
- Radial rendering approach: MEDIUM - Concept is sound but exact MC 1.21.11 API needs validation
- Pitfalls: MEDIUM - Based on docs and common modding issues

**Research date:** 2026-02-02
**Valid until:** 30 days (stable domain, established APIs)
