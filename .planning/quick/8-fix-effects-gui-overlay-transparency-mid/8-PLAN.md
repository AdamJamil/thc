---
phase: quick-8
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - src/client/kotlin/thc/client/EffectsHudRenderer.kt
autonomous: true

must_haves:
  truths:
    - "Green overlay alpha is 0x5A (~35%), midpoint between quick-7's 0x33 and original 0x80"
    - "Green overlay is centered over the effect icon within the frame"
    - "Numeral renders at 5px right, 5px down from frame top-left in texture space, properly scaled to render space"
  artifacts:
    - path: "src/client/kotlin/thc/client/EffectsHudRenderer.kt"
      provides: "Corrected overlay color, overlay position, numeral position"
      contains: "0x5A00FF00"
  key_links: []
---

<objective>
Fix three visual positioning/transparency issues in the effects HUD overlay that remain after quick-7.

Purpose: The overlay transparency, overlay position, and numeral position all need fine-tuning for correct visual appearance.
Output: Updated EffectsHudRenderer.kt with all three fixes.
</objective>

<execution_context>
@/home/tack/.claude/get-shit-done/workflows/execute-plan.md
@/home/tack/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@src/client/kotlin/thc/client/EffectsHudRenderer.kt
@.planning/quick/7-fix-v3-2-effects-gui-scaling-math-green-/7-SUMMARY.md
</context>

<tasks>

<task type="auto">
  <name>Task 1: Fix overlay alpha, overlay positioning, and numeral positioning</name>
  <files>src/client/kotlin/thc/client/EffectsHudRenderer.kt</files>
  <action>
Three targeted fixes in EffectsHudRenderer.kt:

**Fix 1 — Green overlay transparency midpoint:**
- Change `OVERLAY_COLOR` from `0x3300FF00` to `0x5A00FF00`
- This is the midpoint: (0x33 + 0x80) / 2 = (51 + 128) / 2 = 89.5 ~ 90 = 0x5A
- Update the comment to say "~35% alpha" instead of "20% alpha"

**Fix 2 — Green overlay positioning (slightly up and to the left):**
- The overlay currently fills using `iconX`/`iconY` which is `frameX + iconOffset` / `frameY + iconOffset`
- The `fill()` method uses screen coordinates, same as blit x/y params, so the coordinate space should be consistent
- The issue is the overlay fills over the ICON area, but the icon is rendered inside the frame with `iconOffset`. If the overlay appears shifted up-left from where it should be, investigate whether the icon blit's visual center doesn't match where fill() draws
- Check if the overlay should be covering the full frame area (frameX, frameY, frameX+frameSize, frameY+frameSize) rather than just the icon area. The user may want the green to fill the whole frame interior, not just the smaller icon region
- If that's the case, change `renderDurationOverlay` to accept `frameSize` instead of `iconRenderSize`/`iconOffset`, and fill from `(frameX, frameY + frameSize - overlayHeight)` to `(frameX + frameSize, frameY + frameSize)`
- IMPORTANT: Consult the user's description — "green overlay appears slightly up and to the left of where it should be". This means we need to shift it right and down. The most likely fix: the overlay should use the frame bounds, not the icon bounds. Pass `frameSize` to `renderDurationOverlay` and use frame coordinates for the fill rect

**Fix 3 — Numeral positioning (slightly up and to the left):**
- Currently `numeralX = frameX + numeralOffsetX` and `numeralY = frameY + numeralOffsetY`
- The offset ratios are `5.0/44.0` which should produce correct scaled offsets
- The user says "if the effect frame is 44x44, the numeral should start 5 pixels right and 5 pixels down from the top-left of the frame" — the ratios already encode this
- The likely issue is the same as the overlay: verify that `frameX`/`frameY` and the offset math are producing the correct screen position
- Check if the blit's x,y position for the numeral needs adjustment. Since the frame blit at (startX, y) renders correctly, and the numeral is at (startX + numeralOffsetX, y + numeralOffsetY), this should be correct IF the offsets are non-zero at runtime
- Debug approach: add temporary logging of frameSize, numeralOffsetX, numeralOffsetY to verify they're non-trivial values. For a typical frameSize of ~40-50px, offset should be ~4-5px
- If the offsets compute to 0 due to integer truncation at small frame sizes, consider using `Math.round()` or `coerceAtLeast(1)` to ensure minimum 1px offset

IMPLEMENTATION APPROACH:
1. Update OVERLAY_COLOR constant
2. Modify `renderDurationOverlay` signature to accept `frameSize: Int` parameter instead of `iconRenderSize`/`iconOffset`. Compute overlay using frame bounds: x1=frameX, y1=frameY+(frameSize-overlayHeight), x2=frameX+frameSize, y2=frameY+frameSize. The `overlayHeight` should be `(frameSize * ratio).toInt()` (based on frameSize, not iconRenderSize)
3. Update the call site in `render()` to pass `frameSize` instead of `iconRenderSize`/`iconOffset`
4. For numerals: the ratios and offset math look correct. If the numeral still appears offset after the overlay fix lands, the issue may be a visual perception thing with the overlay fixed. Leave numeral math as-is unless testing reveals it's actually wrong
  </action>
  <verify>
Run `./gradlew build` (or `./gradlew classes` if faster). The build must succeed with no compilation errors.

Manual in-game verification needed: apply a timed effect (e.g., potion of speed) and confirm:
- Green overlay is noticeably more opaque than quick-7 but less than the original 50%
- Green overlay fills correctly within the frame (not shifted up/left)
- Numeral (for amplifier 2+ effects) is positioned correctly at ~5px offset from frame top-left
  </verify>
  <done>
OVERLAY_COLOR is 0x5A00FF00. Green overlay fills using frame bounds (not icon bounds). Numeral position verified or adjusted. Build passes.
  </done>
</task>

</tasks>

<verification>
- `./gradlew build` passes
- OVERLAY_COLOR constant is `0x5A00FF00`
- renderDurationOverlay uses frame-relative coordinates for fill()
- No regressions in frame or icon rendering
</verification>

<success_criteria>
- Green overlay at ~35% alpha (0x5A)
- Overlay rectangle aligned to frame, not shifted
- Numeral positioned correctly relative to frame
- Clean build
</success_criteria>

<output>
After completion, create `.planning/quick/8-fix-effects-gui-overlay-transparency-mid/8-SUMMARY.md`
</output>
