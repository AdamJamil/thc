---
phase: 85-scaling
verified: 2026-02-10T16:30:00Z
status: gaps_found
score: 2/3 must-haves verified
gaps:
  - truth: "Slider adjusts the scale factor used by the mob health bar and mob effects renderers"
    status: failed
    reason: "MobHealthBarConfig.getScalePercent() exists but is not used by MobHealthBarRenderer"
    artifacts:
      - path: "src/client/kotlin/thc/client/MobHealthBarRenderer.kt"
        issue: "Uses hardcoded BAR_WIDTH = 1.5f instead of calling MobHealthBarConfig.getScalePercent()"
    missing:
      - "MobHealthBarRenderer.kt must call MobHealthBarConfig.getScalePercent() to compute BAR_WIDTH dynamically"
      - "BAR_WIDTH calculation should use screen width and scale percent: (screenWidth * scalePercent / 100.0)"
---

# Phase 85: Scaling Verification Report

**Phase Goal:** Player can control the size of mob health bars and effect icons through Video Settings
**Verified:** 2026-02-10T16:30:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                                      | Status     | Evidence                                                                      |
| --- | ------------------------------------------------------------------------------------------ | ---------- | ----------------------------------------------------------------------------- |
| 1   | Video Settings menu contains a Mob Health Bar slider                                      | ✓ VERIFIED | VideoSettingsScreenMixin injects mobHealthBarScale option                     |
| 2   | Slider adjusts the scale factor used by the mob health bar and mob effects renderers      | ✗ FAILED   | getScalePercent() exists but not called by MobHealthBarRenderer               |
| 3   | Setting persists across game restarts (saved to config file)                              | ✓ VERIFIED | load() reads from config/thc-mob-health-bar.txt, save() writes on change      |

**Score:** 2/3 truths verified

### Required Artifacts

| Artifact                                                             | Expected                                                                                              | Status      | Details                                                                             |
| -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | ----------- | ----------------------------------------------------------------------------------- |
| `src/client/kotlin/thc/client/MobHealthBarConfig.kt`                | Client config holder with OptionInstance slider and file persistence for mob health bar scaling       | ✓ VERIFIED  | OptionInstance range 2-20%, default 6%, load/save functions, getScalePercent()     |
| `src/client/java/thc/mixin/client/VideoSettingsScreenMixin.java`    | Mixin injecting both Effects GUI and Mob Health Bar sliders into Video Settings                       | ✓ VERIFIED  | thc$addTHCOptions method adds both sliders via list.addSmall()                      |
| `src/client/kotlin/thc/THCClient.kt`                                | Calls MobHealthBarConfig.load() on client init                                                        | ✓ VERIFIED  | Line 37: MobHealthBarConfig.load()                                                  |
| `src/main/resources/assets/thc/lang/en_us.json`                     | Contains translation key "thc.options.mobHealthBarScale"                                              | ✓ VERIFIED  | Line 35: "thc.options.mobHealthBarScale": "Mob Health Bar"                          |

### Key Link Verification

| From                                                              | To                                                    | Via                                              | Status       | Details                                                          |
| ----------------------------------------------------------------- | ----------------------------------------------------- | ------------------------------------------------ | ------------ | ---------------------------------------------------------------- |
| VideoSettingsScreenMixin.java                                    | MobHealthBarConfig.kt                                | MobHealthBarConfig.INSTANCE.getMobHealthBarScale() | ✓ WIRED      | Mixin calls getMobHealthBarScale() to create widget              |
| THCClient.kt                                                      | MobHealthBarConfig.kt                                | MobHealthBarConfig.load()                        | ✓ WIRED      | Called on client initialization                                  |
| MobHealthBarRenderer.kt                                           | MobHealthBarConfig.kt                                | MobHealthBarConfig.getScalePercent()             | ✗ NOT_WIRED  | Renderer uses hardcoded BAR_WIDTH = 1.5f, doesn't call config   |

### Requirements Coverage

| Requirement | Status       | Blocking Issue                                                              |
| ----------- | ------------ | --------------------------------------------------------------------------- |
| SCAL-01     | ✓ SATISFIED  | Slider appears in Video Settings                                            |
| SCAL-02     | ✓ SATISFIED  | Config persists to file                                                     |

**Note:** While both requirements are technically satisfied (slider exists, config persists), the scale value is not actually used by the renderer yet. This was intended to be integrated in phases 83-84.

### Anti-Patterns Found

None. All files are substantive implementations with proper error handling and no placeholder code.

### Human Verification Required

#### 1. Video Settings Slider Visibility

**Test:** Launch game, go to Options > Video Settings, scroll to bottom
**Expected:** Two sliders appear: "Effects GUI Scaling" and "Mob Health Bar"
**Why human:** Visual UI verification requires game launch

#### 2. Slider Value Persistence

**Test:** 
1. Launch game, set Mob Health Bar slider to 15%
2. Exit game completely
3. Check `config/thc-mob-health-bar.txt` contains `mobHealthBarScale:15`
4. Relaunch game, return to Video Settings
**Expected:** Slider shows 15% on restart
**Why human:** Requires full game restart and file system observation

#### 3. Slider Range Validation

**Test:** Move Mob Health Bar slider from minimum to maximum
**Expected:** Values display as 2%, 3%, ..., 19%, 20%
**Why human:** Interactive UI validation

### Gaps Summary

**Critical Gap: Config not wired to renderer**

The phase successfully created MobHealthBarConfig with:
- OptionInstance slider (2-20%, default 6%)
- File persistence to config/thc-mob-health-bar.txt
- Video Settings integration

However, the config value is not consumed by MobHealthBarRenderer. The renderer uses hardcoded constants:
- `BAR_WIDTH = 1.5f` (line 39)
- `frameWorldSize = BAR_HEIGHT * (EffectsHudRenderer.BASE_FRAME_SIZE.toFloat() / TEX_HEIGHT)` (line 209)

**Expected wiring:**
MobHealthBarRenderer should:
1. Get screen width from Minecraft client
2. Call `MobHealthBarConfig.getScalePercent()` to get scale value
3. Compute BAR_WIDTH as: `(screenWidth * scalePercent / 100.0)`
4. Derive BAR_HEIGHT from BAR_WIDTH using aspect ratio
5. Use computed values for both health bar and effect icon rendering

This is likely intentional - phase 85 provides the config infrastructure, phases 83-84 provide the renderers, and integration was expected to happen after all pieces exist. However, without integration, the slider has no visible effect.

**Why this matters:**
- Truth #2 fails: slider doesn't actually adjust renderer scale
- User expectation broken: moving slider does nothing
- Goal not fully achieved: player cannot control size yet

**Root cause:**
Documentation suggests phases 83-84 "can integrate" the scale value, but doesn't mandate it. This is a coordination gap between phases.

---

_Verified: 2026-02-10T16:30:00Z_
_Verifier: Claude (gsd-verifier)_
