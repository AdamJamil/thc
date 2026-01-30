# Phase 62: QoL Fixes - Research

**Researched:** 2026-01-29
**Domain:** Minecraft mod QoL fixes (effect capping, HUD rendering, block entity state, XP economy)
**Confidence:** HIGH

## Summary

This phase addresses four independent quality-of-life fixes across different subsystems. Research examined the existing implementations in the THC codebase and determined the minimal changes required for each fix.

**QOL-01 (Mining Fatigue Cap):** The existing `MiningFatigue.applyFatigue()` method increments the amplifier without a cap. Add a simple conditional to cap at amplifier 9 (displayed as level 10).

**QOL-02 (Poise Meter Scaling):** The existing `BucklerHudRenderer` uses fixed constants for icon size (9px) and spacing (8px). Use `Matrix3x2fStack` transformations via `guiGraphics.pose()` to scale icons ~7-10% smaller while adjusting spacing.

**QOL-03 (Bell Ringing):** The existing `BellHandler` intercepts bell interactions to drop land plots on first ring. Modify to only drop item on first ring but allow ringing behavior always.

**QOL-04 (XP Bottles):** The existing `ExperienceOrbXpMixin` already whitelists `ThrownExperienceBottle` in the call stack check, but it blocks the award entirely for other sources rather than just the non-combat ones. Verify current behavior and fix if needed.

**Primary recommendation:** Four independent, low-complexity changes to existing files. No new files required. Each fix is 1-5 lines of code modification.

## Standard Stack

No new libraries or dependencies required. All fixes use existing Minecraft/Fabric APIs.

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.141.0+1.21.11 | Event callbacks, HUD API | Project standard |
| Minecraft | 1.21.11 | MobEffectInstance, GuiGraphics, BlockEntity | Vanilla APIs |
| Mixin | 0.8.x | Method injection | Fabric standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Matrix3x2fStack | MC 1.21.8+ | 2D HUD transformations | Scaling poise icons |

## Architecture Patterns

### Pattern 1: Effect Amplifier Capping

**What:** Limit the amplifier value when applying/stacking mob effects
**When to use:** Any effect that should have a maximum displayed level
**Example:**
```kotlin
// In MiningFatigue.applyFatigue()
val newAmplifier = if (currentEffect != null) {
    // Stack: increment amplifier, cap at 9 (displayed as level 10)
    minOf(currentEffect.amplifier + 1, 9)
} else {
    0
}
```

**Key insight:** Minecraft displays effect level as `amplifier + 1`, so amplifier 9 = Level 10.

### Pattern 2: Matrix Stack Scaling for HUD

**What:** Scale textures using matrix transformations instead of changing blit parameters
**When to use:** Rendering smaller/larger versions of textures on HUD
**Example:**
```kotlin
// In BucklerHudRenderer.render()
val matrices = guiGraphics.pose()
val iconScale = 0.92f  // 8% smaller

for (i in 0 until totalIcons) {
    val baseX = left + (i * ICON_SPACING_SCALED)

    matrices.pushMatrix()
    matrices.translate(baseX.toFloat(), top.toFloat())
    matrices.scale(iconScale, iconScale)

    guiGraphics.blit(
        RenderPipelines.GUI_TEXTURED,
        icon,
        0,  // x offset now 0 since we translated
        0,  // y offset now 0 since we translated
        0.0f, 0.0f,
        ICON_RENDER_SIZE, ICON_RENDER_SIZE,
        16, 16, 16, 16
    )

    matrices.popMatrix()
}
```

**Key insight:** With MC 1.21.8+, `guiGraphics.pose()` returns `Matrix3x2fStack` (not PoseStack). Methods are `pushMatrix()`/`popMatrix()`, `translate(x, y)`, `scale(x, y)` - no z-axis.

### Pattern 3: Conditional Behavior with State Check

**What:** Execute side effects only on first occurrence while allowing primary behavior always
**When to use:** One-time drops that shouldn't block normal interactions
**Example:**
```kotlin
// In BellHandler
UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
    if (!blockState.is(Blocks.BELL)) return@register InteractionResult.PASS
    if (level.isClientSide) return@register InteractionResult.PASS

    // Drop land plot only on first ring (one-time)
    if (!BellState.isActivated(level, pos)) {
        BellState.setActivated(level, pos, true)
        // Drop land plot book
        val landPlot = ItemStack(THCItems.LAND_PLOT)
        val itemEntity = ItemEntity(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, landPlot)
        level.addFreshEntity(itemEntity)
    }

    // Always allow the ring - return PASS to let vanilla handle
    InteractionResult.PASS
}
```

**Key insight:** Returning `InteractionResult.PASS` instead of `SUCCESS` allows vanilla bell ringing to proceed.

### Anti-Patterns to Avoid
- **Blocking interaction result on already-activated bells:** Current implementation returns SUCCESS which may prevent the actual bell ring sound/animation
- **Hardcoding HUD dimensions:** Use constants that can be adjusted together (icon size + spacing should be coordinated)
- **Stack trace inspection without all cases:** XP mixin checks specific class names but may miss edge cases

## Don't Hand-Roll

No complex solutions needed for this phase. All fixes are straightforward modifications.

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Scaling textures | Custom UV math | Matrix3x2fStack.scale() | Built-in, handles all edge cases |
| Effect level caps | Post-processing | Cap in applyFatigue() | Simpler, single source of truth |

## Common Pitfalls

### Pitfall 1: InteractionResult Confusion
**What goes wrong:** Returning SUCCESS blocks vanilla behavior; returning CONSUME blocks client sync
**Why it happens:** InteractionResult semantics vary by context
**How to avoid:**
- Return PASS to let vanilla handle the interaction after your side effects
- Return SUCCESS only if you're fully handling the interaction yourself
**Warning signs:** Bell doesn't animate/ring after land plot obtained

### Pitfall 2: Matrix Stack Leaks
**What goes wrong:** Forgetting to pop the matrix stack causes all subsequent HUD rendering to be affected
**Why it happens:** Easy to miss popMatrix() call, especially with early returns
**How to avoid:**
- Always pair pushMatrix()/popMatrix()
- Use try/finally if there are multiple return paths
**Warning signs:** Other HUD elements (health, armor) rendered wrong after poise bar

### Pitfall 3: Off-by-One in Effect Levels
**What goes wrong:** Capping at 10 instead of 9 gives level 11 display
**Why it happens:** Confusion between amplifier (0-indexed) and displayed level (1-indexed)
**How to avoid:** Remember: displayed level = amplifier + 1
**Warning signs:** Mining Fatigue shows "XI" instead of "X"

### Pitfall 4: XP Mixin Order-Dependent Logic
**What goes wrong:** Stack trace order may vary; early returns prevent later checks
**Why it happens:** Different code paths have different call stacks
**How to avoid:** Check all allow-list conditions before any block conditions
**Warning signs:** XP bottles work in some contexts but not others

## Code Examples

### Mining Fatigue Cap (QOL-01)
```kotlin
// Source: MiningFatigue.kt - applyFatigue() modification
private fun applyFatigue(player: ServerPlayer) {
    val currentEffect = player.getEffect(MobEffects.MINING_FATIGUE)

    val newAmplifier = if (currentEffect != null) {
        // Stack: increment amplifier, cap at 9 (displayed as level 10)
        minOf(currentEffect.amplifier + 1, MAX_AMPLIFIER)
    } else {
        0
    }

    // ... rest unchanged
}

companion object {
    private const val MAX_AMPLIFIER = 9  // Level 10 display
}
```

### Poise Meter Scaling (QOL-02)
```kotlin
// Source: BucklerHudRenderer.kt - render() modification
private const val ICON_RENDER_SIZE = 9
private const val ICON_SCALE = 0.92f  // ~8% smaller
private const val ICON_SPACING = 9    // Slightly more than scaled size for visible gap

fun render(guiGraphics: GuiGraphics) {
    // ... existing setup ...

    val matrices = guiGraphics.pose()

    for (i in 0 until totalIcons) {
        val baseX = left + (i * ICON_SPACING)

        matrices.pushMatrix()
        matrices.translate(baseX.toFloat(), top.toFloat())
        matrices.scale(ICON_SCALE, ICON_SCALE)

        val icon = /* ... existing icon selection ... */
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            icon,
            0, 0,  // Position handled by translate
            0.0f, 0.0f,
            ICON_RENDER_SIZE, ICON_RENDER_SIZE,
            16, 16, 16, 16
        )

        matrices.popMatrix()
    }
}
```

### Bell Behavior (QOL-03)
```kotlin
// Source: BellHandler.kt - register() modification
UseBlockCallback.EVENT.register { player, level, hand, hitResult ->
    val pos = hitResult.blockPos
    val blockState = level.getBlockState(pos)

    if (!blockState.`is`(Blocks.BELL)) {
        return@register InteractionResult.PASS
    }

    // Server-side only for item drop
    if (!level.isClientSide) {
        // Check if already activated (already dropped land plot)
        if (!BellState.isActivated(level, pos)) {
            // Mark as activated
            BellState.setActivated(level, pos, true)

            // Drop land plot book
            val landPlot = ItemStack(THCItems.LAND_PLOT)
            val itemEntity = ItemEntity(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, landPlot)
            level.addFreshEntity(itemEntity)
        }
    }

    // ALWAYS return PASS to allow vanilla bell ringing
    InteractionResult.PASS
}
```

### XP Bottle Fix (QOL-04)
```java
// Source: ExperienceOrbXpMixin.java - verify/fix whitelist
@Inject(method = "award", at = @At("HEAD"), cancellable = true)
private static void thc$blockNonCombatXp(ServerLevel level, Vec3 pos, int amount, CallbackInfo ci) {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();

    for (StackTraceElement element : stack) {
        String className = element.getClassName();
        String methodName = element.getMethodName();

        // ALLOW conditions - check these FIRST before any blocking

        // Allow: Mob death XP
        if (methodName.contains("dropExperience") || methodName.contains("dropAllDeathLoot")) {
            return; // Allow
        }

        // Allow: Experience bottles (expanded check)
        if (className.contains("ThrownExperienceBottle") ||
            className.contains("ExperienceBottle") ||
            className.contains("ThrownExpBottle")) {
            return; // Allow
        }

        // BLOCK conditions - only after all allow conditions checked
        // ... existing block conditions ...
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| PoseStack for HUD | Matrix3x2fStack | MC 1.21.8 | No z-axis methods, 2D-only |
| HudRenderCallback | HudElementRegistry | Fabric API 0.116 | Deprecated old callback |

**Deprecated/outdated:**
- `HudRenderCallback`: Deprecated in Fabric API 0.116, use `HudElementRegistry` (project already uses this)
- `PoseStack` for HUD: Replaced with `Matrix3x2fStack` in 1.21.8+

## Open Questions

1. **XP Bottle Verification Needed**
   - What we know: Mixin checks for "ThrownExperienceBottle" and "ExperienceBottle" in class names
   - What's unclear: Whether the actual class in MC 1.21.11 matches these patterns exactly
   - Recommendation: Test XP bottles in-game first; if not working, add debug logging to see actual class names in stack trace

2. **Poise Icon Visual Tuning**
   - What we know: ~7-10% smaller requested
   - What's unclear: Exact scale factor and spacing that looks best
   - Recommendation: Start with 0.92 scale (8% smaller), adjust based on visual feedback

## Sources

### Primary (HIGH confidence)
- THC codebase analysis: `MiningFatigue.kt`, `BucklerHudRenderer.kt`, `BellHandler.kt`, `ExperienceOrbXpMixin.java`
- Fabric Documentation: HUD rendering, GuiGraphics blit - https://docs.fabricmc.net/develop/rendering/hud
- Minecraft Wiki: Effect amplifier values - https://minecraft.fandom.com/wiki/Effect

### Secondary (MEDIUM confidence)
- Fabric docs on draw context - https://docs.fabricmc.net/develop/rendering/draw-context
- WebSearch results on Matrix3x2fStack changes in 1.21.8+

### Tertiary (LOW confidence)
- None - all critical findings verified with primary sources

## Metadata

**Confidence breakdown:**
- Mining fatigue cap: HIGH - straightforward amplifier cap, well-understood API
- Poise meter scaling: HIGH - Matrix3x2fStack documented, existing rendering code clear
- Bell behavior: HIGH - existing code clear, InteractionResult semantics well-known
- XP bottles: MEDIUM - mixin logic clear but exact class names need verification

**Research date:** 2026-01-29
**Valid until:** 60 days (stable APIs, no expected changes)
