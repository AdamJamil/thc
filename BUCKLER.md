# Buckler Implementation Plan

This document locks in the buckler design and provides a detailed, milestone-based
implementation plan. It is intended to remove ambiguity before coding begins.

## Design Summary (Confirmed)

### Buckler tiers (power in hearts)
| tier | stone | iron | gold | diamond | netherite |
| :---- | :---- | :---- | :---- | :---- | :---- |
| shield power | 2.0 | 3.0 | 3.25 | 4.0 | 4.5 |
| parry window | 0.16s | 0.16s | 0.18s | 0.22s | 0.25s |
| stun duration | 1.5s | 1.5s | 1.5s | 1.5s | 3.0s |
| durability | 150 | 300 | 140 | 660 | 740 |

Durability scaling uses iron = 300 and armor material durability ratios
(gold 7/15, diamond 33/15, netherite 37/15).

### Blocking and parry rules
- Bucklers are **offhand-only**. Mainhand use fails.
- Buckler reduction is applied **before all other mitigation** (first in the
  pipeline). It reduces the incoming damage amount before armor, protection,
  resistance, and absorption. Conversion is 1 heart = 2 damage points.
- Bucklers **do not affect environmental damage**. Environmental exclusions:
  burning (IN_FIRE, ON_FIRE), lava (LAVA), magma block (HOT_FLOOR),
  suffocation (IN_WALL), drowning (DROWN), lightning (LIGHTNING_BOLT),
  falling block (FALLING_BLOCK), cramming (CRAMMING), poison, wither, freezing,
  and fall (FALL). See "Damage source mapping" below for poison handling.
- Parry window is computed from "raising the buckler" and rounded to the nearest tick.
  Example: 0.16s -> 3 ticks, 0.18s -> 4 ticks, 0.22s -> 4 ticks, 0.25s -> 5 ticks.
- If non-environmental damage is taken within the parry window:
  - Damage reduction uses **2.5x shield power** for that hit only.
  - Play the custom parry sound (`thc:buckler_parry`).
  - Stun all monsters (MobCategory.MONSTER) within 2 blocks for the stun duration.
    Apply Slowness VI (amplifier 5) + Weakness XX (amplifier 19).
  - Lethal parry check is separate (see below).
  - Player poise increases by **shield power** (not 2.5x). Clamp to max.
  - Parry resets the raise start time so consecutive hits parry if the buckler stays raised.
- If damage is taken with buckler raised (non-parry):
  - Reduce the **incoming damage** by shield power (hearts -> damage points),
    then let vanilla mitigation proceed, clamp to >= 0.
  - Reduce poise by the amount of **blocked damage** (in hearts).
  - Play the vanilla shield block sound.
- Directional blocking check must match vanilla shield logic (angle check vs source).
- Parry window reset must only happen when checking **non-environmental** damage.
- Lethal parry: after health is updated from non-environmental damage, if the
  buckler was raised within the last 3 ticks and the player would die, break the
  buckler and set health to half a heart (1.0F).

### Durability loss
- On normal blocking (non-parry):
  - Let blockedDamagePoints be the final reduction (damage points).
  - durabilityLoss = floor(blockedDamagePoints / 5)
    + (randomUniformInt(1..5) <= (blockedDamagePoints % 5) ? 1 : 0)
  - Apply to the buckler item (offhand stack).
- On non-lethal parry: **no durability loss**.
- On lethal parry: buckler breaks.

### Poise
- Poise units are hearts.
- maxPoise = 2 * shieldPower (hearts).
- Poise reduces on non-parry blocks by blocked damage (hearts).
- If poise reaches 0: "shield break" occurs.
  - Buckler cannot be raised until poise is back to 100%.
  - The item remains (unless it broke from lethal parry).
- Poise regen is linear over 4s while buckler is lowered, even if the buckler
  is not currently in the offhand.

### HUD (poise bar)
- Render a new bar **above armor** in the HUD.
- Use `stone_buckler.png` as full, `stone_buckler_half.png` as half,
  and `stone_buckler_empty.png` as empty.
- The bar appears when:
  - Poise is not full, OR
  - Poise is full but has been full for less than 8 seconds.
- Bar length matches current max poise (in hearts), supporting half-heart endings.
- Register a left-side status bar height provider so vanilla bars keep their
  layout when the poise bar appears.

### Items and recipes
- New items: `stone_buckler`, `iron_buckler`, `gold_buckler`, `diamond_buckler`,
  `netherite_buckler`.
- Recipes: 1 stick in the center + 8 of tier material around it.
  - stone -> `minecraft:stone`
  - iron -> `minecraft:iron_ingot`
  - gold -> `minecraft:gold_ingot`
  - diamond -> `minecraft:diamond`
  - netherite -> `minecraft:netherite_ingot`
- Repairable with the corresponding tier material.
- Items appear in `ItemGroups.COMBAT`.

### Models and textures
- Textures (already present):
  `stone_buckler.png`, `iron_buckler.png`, `gold_buckler.png`,
  `diamond_buckler.png`, `netherite_buckler.png`,
  `stone_buckler_half.png`, `stone_buckler_empty.png`.
- Item models use the 1.21+ item model system:
  - `assets/thc/items/<buckler>.json` switches model on `minecraft:using_item`
    (like vanilla `items/shield.json`).
  - Provide both normal and blocking models:
    `assets/thc/models/item/<buckler>.json`
    `assets/thc/models/item/<buckler>_blocking.json`
  - Use a thin extruded block-model for 3D look with the 16x16 texture on faces.
  - Base model inherits `minecraft:item/handheld` transforms.
  - Blocking model overrides first-person transforms to keep the buckler visible.
  - Side faces sample a small UV region of the outer rim to avoid transparency.

## Damage Source Mapping

Environmental damage is excluded from buckler effects. The direct mapping to
DamageTypes:
- burning: IN_FIRE, ON_FIRE
- lava: LAVA
- magma block: HOT_FLOOR
- suffocation: IN_WALL
- drowning: DROWN
- lightning: LIGHTNING_BOLT
- falling block: FALLING_BLOCK
- cramming: CRAMMING
- freezing: FREEZE
- fall: FALL
- wither: WITHER
- poison: Poison damage in 1.21 uses DamageTypes.MAGIC in vanilla.
  To avoid disabling all "magic" damage, treat MAGIC as environmental only
  when the victim currently has the Poison effect.

If this mapping needs adjustment, update the environmental check helper.

## Milestones

### Milestone 1 - Data + assets scaffolding
Status: Completed

1) Add item model JSONs:
   - `assets/thc/items/{stone,iron,gold,diamond,netherite}_buckler.json`
     - Use `minecraft:condition` with `property: minecraft:using_item`
       and `on_false`/`on_true` models.
2) Populate `assets/thc/models/item/*_buckler.json`
   and `*_buckler_blocking.json`:
   - Use `parent: minecraft:item/handheld` for base + custom elements to
     extrude a thin 3D plate with the buckler texture on both sides.
   - Override blocking model transforms for first-person views only.
3) Add `assets/thc/lang/en_us.json` with buckler display names.
4) Add recipes under `data/thc/recipes/` for each tier.

### Milestone 2 - Buckler item class + registry
Status: Completed

1) Create `thc.item.BucklerItem`:
   - Overrides `use` to allow only offhand use.
   - Starts using item (`startUsingItem`) with `InteractionResult.CONSUME`.
   - `getUseAnimation` returns `ItemUseAnimation.BLOCK`.
   - `getUseDuration` returns a large value (like shields).
2) Add registry class (e.g., `thc.THCBucklers`):
   - Register five bucklers with durability, repair ingredient, and combat group.
   - Hook init from `thc.THC.onInitialize`.

### Milestone 3 - Buckler model rim and blocking alignment
Status: Completed

1) Update buckler model side faces to sample a compact UV region from the outer rim
   of the front texture so the sides stay opaque and match the rim color.
2) Adjust blocking model transforms to rotate the buckler orthogonal to the hand
   (first-person), so the face points outward instead of parallel to the arm.

### Milestone 4 - Buckler player state (attachments)
Status: Completed

1) Extend `thc.THCAttachments` with:
   - `BUCKLER_POISE` (double, hearts)
   - `BUCKLER_RAISE_TICK` (long; -1 when not raised)
   - `BUCKLER_BROKEN` (boolean)
   - `BUCKLER_LAST_FULL_TICK` (long; used by HUD)
   - `BUCKLER_MAX_POISE` (double; last known max poise for regen)
2) Decide persistence:
   - Poise should be non-persistent (reset on relog/death).
   - Raise tick is transient.
   - Broken resets on death.
3) Add helper access class for server-side buckler state.

### Milestone 5 - Poise regen + raise state tracking
Status: Completed

1) Add server tick hook:
   - If player is **not using** a buckler (regardless of offhand):
     - Regen poise at rate `maxPoise / 4s`.
     - When poise reaches max, set `BUCKLER_LAST_FULL_TICK = now`.
   - HUD rendering uses the last synced max poise; an offhand buckler is only
     required as a fallback to discover max poise on the client.
2) When player starts using the buckler:
   - Record `BUCKLER_RAISE_TICK = currentTick` (server side).
3) When player stops using:
   - Reset raise tick to -1.

### Milestone 6 - Damage handling and blocking
Status: Completed

1) Add `LivingEntityMixin` to reduce the raw damage amount at the start of
   `hurtServer` (before vanilla mitigation):
   - If victim is a player with buckler raised in offhand, run buckler logic.
   - Skip entirely for environmental damage.
   - Apply reduction to the incoming `amount` so vanilla mitigation scales it.
2) Ensure direction check matches vanilla:
   - Copy the view-vector vs source-position angle check logic from
     `LivingEntity.applyItemBlocking`.
   - If direction does not block, skip buckler reduction and parry.
3) Parry:
   - parryTicks = round(parryWindowSeconds * 20).
   - If `currentTick - raiseStartTick <= parryTicks`:
     - reduction = shieldPower * 2.5 * 2 (damage points)
     - poise += shieldPower (hearts)
     - raiseStartTick = currentTick (reset window)
     - stun monsters within 2 blocks
4) Non-parry block:
   - reduction = shieldPower * 2 (damage points)
   - poise -= (reduction / 2) (hearts)
   - if poise <= 0 -> broken (cannot raise until full)
5) Lethal parry handling:
   - After health is updated from non-environmental damage, if buckler was
     raised within the last 3 ticks and the player would die, break buckler
     and set health to 1.0F.
   - Ensure no durability loss on non-lethal parries.

### Milestone 7 - Durability loss + break behavior
Status: Completed

1) On non-parry blocks:
   - Compute durability loss with the random remainder rule.
   - Apply to offhand buckler stack.
2) On lethal parry:
   - Break item (damage to max + broadcast break event).
3) Ensure vanilla shield blocking does not apply:
   - Do not attach `DataComponents.BLOCKS_ATTACKS` to bucklers.
   - Blocking is fully custom via `isUsingItem` + offhand check.

### Milestone 8 - Client HUD bar
Status: Completed

1) Register a Fabric HUD render callback and a height provider:
   - Use `HudRenderCallback.EVENT.register(...)` to render the bar.
   - Register a no-op `HudElementRegistry.addLast(...)` for the poise bar ID so
     the height provider is considered registered.
   - Register `HudStatusBarHeightRegistry.addLeft(...)` to reserve vertical space.
   - Only render if maxPoise > 0 and:
     - poise < max, OR
     - (poise == max AND now - lastFullTick <= 160 ticks).
2) Draw icon row:
   - Each icon represents 1 heart of poise.
   - Use full/half/empty textures based on current poise.
   - Use `stone_buckler.png`, `stone_buckler_half.png`,
     `stone_buckler_empty.png`.
3) Sync poise state to client:
   - Create an S2C packet that sends current poise, max poise,
     broken flag, and lastFullTick.
   - Send when values change, on login, and on respawn.

### Milestone 9 - Tests
Status: Completed

Add/extend GameTests:
1) Parry window timing (edge cases around rounding).
2) Environmental damage exclusions (e.g., fall, lava, drowning) do not parry.
3) Poise break/recovery gating.
4) Lethal parry saves at half-heart and breaks buckler.
5) Durability loss distribution for blocked damage.

## Progress (Implementation Notes)

### Milestone 1 (Completed)
- Added item model switchers using `minecraft:using_item` in
  `src/main/resources/assets/thc/items/stone_buckler.json` and tier variants.
- Added 3D extruded buckler models in
  `src/main/resources/assets/thc/models/item/stone_buckler.json` and tier variants.
- Added blocking variants in
  `src/main/resources/assets/thc/models/item/stone_buckler_blocking.json`
  and tier variants with first-person transform overrides.
- Added display names in `src/main/resources/assets/thc/lang/en_us.json`.
- Added crafting recipes in `src/main/resources/data/thc/recipes/stone_buckler.json`
  and tier variants.

### Milestone 2 (Completed)
- Implemented `BucklerItem` with offhand-only use, block animation, long use duration,
  and buckler detection helpers in `src/main/kotlin/thc/item/BucklerItem.kt`.
- Registered items with durability and repair materials in
  `src/main/kotlin/thc/item/THCBucklers.kt`.
- Added bucklers to the combat tab via `ItemGroupEvents.modifyEntriesEvent` and
  `entries.accept(...)`.
- Wired item initialization in `src/main/kotlin/thc/THC.kt`.
- Fixed 1.21 runtime crash by assigning `Item.Properties.setId(...)` and registering
  via `ResourceKey<Item>` so description IDs are available at construction time.

### Milestone 3 (Completed)
- Updated buckler model side faces to sample a 2x2 UV region from the outer rim
  of the front texture to keep sides opaque without separate textures.
- Rotated blocking transforms in first-person so the buckler face is orthogonal
  to the arm instead of parallel.

### Milestone 4 (Completed)
- Added buckler attachments in `src/main/java/thc/THCAttachments.java`:
  poise, raise tick, broken flag, last-full tick, and max poise.
- Added `thc.buckler.BucklerState` helper in
  `src/main/java/thc/buckler/BucklerState.java` for server-side access.

### Milestone 5 (Completed)
- Added `BucklerStatsRegistry` in `src/main/kotlin/thc/buckler/BucklerStats.kt` for
  shield power, parry window, and stun duration data.
- Added a server tick hook in `src/main/kotlin/thc/THC.kt` to:
  - Track buckler raise start tick.
  - Regen poise when not raised using the last known max poise.
  - Clear broken state once poise fully recovers.

### Milestone 6 (Completed)
- Added `LivingEntityMixin` in `src/main/java/thc/mixin/LivingEntityMixin.java` to
  reduce incoming damage at the start of `hurtServer`, making bucklers first in
  the mitigation pipeline.
- Implemented environmental damage exclusions, vanilla shield direction checks,
  parry window handling, poise updates, and stun application.
- Added `BUCKLER_MAX_POISE` attachment and set it from combat logic.
- Blocked buckler raising when poise is empty or the shield is broken in
  `src/main/kotlin/thc/item/BucklerItem.kt`.
 - Added a post-damage lethal parry check gated by a 3-tick raise window to
  break the buckler and keep the player at half a heart.

### Milestone 7 (Completed)
- Applied buckler durability loss for non-parry blocks using the 5-damage
  random remainder rule in `src/main/java/thc/mixin/LivingEntityMixin.java`.
- Broke the buckler on lethal parries before setting health to half a heart.
- Kept bucklers free of vanilla `BLOCKS_ATTACKS` data components so vanilla
  shield blocking stays disabled.

### Milestone 8 (Completed)
- Added `BucklerSync` and `BucklerStatePayload` in `src/main/java/thc/network/`
  to sync poise state to clients when values change.
- Wired syncing into `src/main/kotlin/thc/THC.kt` and registered client receivers
  in `src/client/kotlin/thc/THCClient.kt`.
- Implemented `BucklerHudRenderer` in `src/client/kotlin/thc/client/` and
  registered it via `HudRenderCallback`, with a `HudStatusBarHeightRegistry`
  provider so the poise bar renders above armor using the buckler textures.
- Removed the offhand-only HUD gate; the bar now renders whenever max poise is
  known on the client and the display rules are met.

### Milestone 9 (Completed)
- Added buckler GameTests in `src/main/java/thc/gametest/THCBucklerGameTests.java`
  to cover parry blocking, environmental damage exclusions, poise break, and lethal
  parry survival.

### Additional Fixes and Adjustments
- Replaced the initial shield-style display transforms with `minecraft:item/handheld`
  transforms to remove skewed GUI icons and odd hand positions.
- Adjusted blocking transforms to keep the buckler visible and orthogonal in first-person.
- Updated third-person blocking transforms to vanilla shield-style rotations with centered
  translations; needs in-game verification for offhand alignment.
- Switched creative tab insertion from `entries.add(...)` to `entries.accept(...)`
  to match the Fabric API mappings used in this project.
- Updated buckler GameTests to force survival/vulnerable mock players so damage
  logic executes during tests.
- Added buckler debug chat output gated by the `thc_buckler_debug` tag to help
  trace parry/reduction calculations.
- Moved buckler reduction to the start of `hurtServer` so it runs before vanilla
  mitigation, and added a 3-tick lethal parry check after health updates.
- Rendered the poise bar via Fabric's HUD element registry to inherit vanilla
  HUD render conditions and anchor the bar above armor.
- Updated `GuiGraphics.blit` usage for 1.21's normalized UV signature.

## Open Questions / Assumptions
- Poison handling: treat DamageTypes.MAGIC as environmental only when the victim
  has the Poison effect, to avoid disabling other magic damage. If you want all
  magic to be excluded, remove the Poison effect check.
- If the player has no buckler equipped, poise regen still proceeds using the
  last known max poise value; the HUD is hidden only when max poise is unknown.
- Half-heart rendering for max poise when tier max is fractional (gold).
