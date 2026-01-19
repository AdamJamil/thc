# HUD overlay bar research (Minecraft 1.21.x / Fabric)

This note captures what the current Fabric docs say for 1.21.11 and how that lines up with 1.21.x HUD API changes.

## What the official guidance says (1.21.11)

- Fabric’s HUD docs for 1.21.11 explicitly discourage `HudRenderCallback` because HUD rendering changed and the callback is now “extremely limited.” The docs instead direct modders to the HUD API. citeturn0search4
- The HUD API is built around registering layers via `HudElementRegistry`, which provides ordered insertion points relative to vanilla HUD elements. citeturn0search4turn1search2
- `VanillaHudElements` exposes IDs for the built‑in bars (health, armor, food, air, etc.), which lets you place a custom bar above/below specific vanilla bars. citeturn1search4

## What changed in 1.21.x (and how stable it is)

- Fabric API 0.116.0 (for 1.21.5 snapshots) introduced `HudLayerRegistrationCallback` to replace the old event‑based rendering flow. citeturn1search1
- In 1.21.6, Fabric rewrote the HUD API and positioned `HudElementRegistry` as the primary entry point, with examples using `addLast` or `attachElementBefore` for ordering. citeturn1search3

**Conclusion:** The “new” HUD layering system is the expected approach for 1.21.5+ and remains the current guidance for 1.21.11. The online docs are consistent with the 1.21.11 version banner, so the approach has not changed since those updates. citeturn0search4turn1search1turn1search3

## Recommended approach for a custom status bar (poise)

1. **Register a HUD element** with `HudElementRegistry` rather than `HudRenderCallback`.
2. **Choose a layer placement** using `VanillaHudElements` (for example, insert after `ARMOR_BAR` or before `FOOD_BAR`) depending on where you want the poise bar to show relative to armor/health.
3. **Render using `DrawContext` + `RenderTickCounter`** in your element’s `render()` method (the HUD API surfaces these to each layer).
4. **Respect vanilla hide‑HUD rules** by attaching relative to a vanilla layer. The registry docs note that elements inserted relative to vanilla inherit that layer’s render conditions (like HUD hidden). citeturn1search2

## Ordering and placement guidance

Use the Fabric registry’s documented injection points to position your bar:

- `attachElementAfter(VanillaHudElements.ARMOR_BAR, ...)` to render above armor.
- `attachElementBefore(VanillaHudElements.FOOD_BAR, ...)` to render between armor and food.
- `addLast(...)` to render after all vanilla HUD layers (usually not desired for status bars). citeturn1search2turn1search4turn1search3

The registry javadoc also highlights common “global” insertion points (before/after `MISC_OVERLAYS`, `BOSS_BAR`, `DEMO_TIMER`, `CHAT`, `SUBTITLES`) which can be used when you want the overlay above or below most HUD elements. citeturn1search2

## Confirmed documentation alignment through 1.21.11

Fabric’s official HUD documentation is explicitly labeled “written for version 1.21.11,” and it emphasizes the HUD API over `HudRenderCallback`. The 1.21.5 and 1.21.6 Fabric changelogs describe the new HUD layer system and the `HudElementRegistry` rewrite, which matches the 1.21.11 docs. This indicates that the standard approach for HUD overlays in 1.21.x is to register layered HUD elements via the registry and avoid `HudRenderCallback`. citeturn0search4turn1search1turn1search3

