# Domain Pitfalls: Revival System

**Domain:** Minecraft revival/downed state system
**Project:** THC (True Hardcore)
**Researched:** 2026-01-31

---

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

---

### Pitfall 1: Death Prevention Race Conditions

**What goes wrong:** Intercepting lethal damage at the wrong point in the damage pipeline causes the player to die anyway, or causes state corruption where the player is both "downed" and "dead."

**Why it happens:** Minecraft's damage pipeline has multiple stages (hurt, hurtServer, die). If you cancel damage too late (after health reaches 0), death events may already be triggered. If you cancel too early (before armor/effects apply), damage calculations are wrong.

**Consequences:**
- Player dies despite having downed state available
- Death screen appears but player is still in world (shaking/frozen state)
- Stats (deaths) increment incorrectly
- Respawn anchor/bed logic fires when it shouldn't

**Warning signs:**
- Testing shows intermittent deaths that should be downs
- Death messages appearing for downed players
- Player health shows 0 but entity still exists

**Prevention:**
1. Use `@ModifyVariable` on the damage amount in `hurtServer` to reduce lethal damage to 0 (THC already does this for lethal parry)
2. Set player health to a small positive value (0.01F or 1F) before returning from damage hook
3. Never let health actually reach 0 - intercept before that point
4. Test with void damage, /kill command, and high burst damage

**THC Integration:** The existing `LivingEntityMixin.thc$applyLethalParry` pattern shows the correct approach - intercept at TAIL of hurtServer, check if health is 0, then restore to positive. Extend this pattern for downed state entry.

**Phase:** Damage interception (first implementation phase)

---

### Pitfall 2: Client-Server Pose Desync

**What goes wrong:** The player's visual pose (lying down) desyncs between server and clients, causing downed players to appear standing on other clients, or the downed player to see themselves standing while others see them down.

**Why it happens:**
- `Pose` is tracked via entity data tracker but requires proper synchronization
- Some pose changes only affect the local player model
- Vanilla poses (SWIMMING, DYING) have built-in behaviors that may conflict

**Consequences:**
- Other players cannot see who is downed
- Hitboxes don't match visual representation
- Revival targeting becomes unreliable
- Immersion breaks completely

**Warning signs:**
- Pose changes work in singleplayer but not multiplayer
- Third-person view shows different pose than first-person
- Other players report seeing downed player standing

**Prevention:**
1. Set pose on SERVER side only - let data tracker sync to clients
2. Use `player.setPose(Pose.SWIMMING)` for prone position (it's the closest built-in lying pose)
3. Send explicit pose sync packets if built-in sync is unreliable
4. Test in multiplayer with 2+ clients from the start
5. Consider custom pose override via mixin if vanilla poses have unwanted behaviors

**THC Integration:** THC already handles client-server sync for buckler state. Create similar `DownedStatePayload` packet pattern to sync downed state explicitly rather than relying solely on pose sync.

**Phase:** Downed state rendering (early phase, affects all subsequent work)

---

### Pitfall 3: Mob AI Continues Targeting Downed Players

**What goes wrong:** Hostile mobs continue to path toward and attempt to attack downed players, even if attacks don't deal damage. This looks wrong and wastes mob AI cycles.

**Why it happens:**
- Mob targeting goals run independently of damage immunity
- `NearestAttackableTargetGoal` doesn't have built-in "is downed" checks
- Making player invulnerable doesn't affect mob perception

**Consequences:**
- Mobs crowd around downed player instead of attacking revivers
- Visual confusion (mobs attacking invulnerable body)
- Revivers have unfair advantage (mobs ignore them)
- AI thrashing if mobs constantly re-target then fail to damage

**Warning signs:**
- Mobs pathfind to downed player
- Attack animations play against downed player
- Revivers can work uninterrupted by nearby hostiles

**Prevention:**
1. Modify targeting predicates to exclude downed players
2. Use `LivingEntity.isInvisibleTo()` override or similar exclusion
3. Force mobs to forget downed players and re-acquire targets
4. Consider: Should mobs attack revivers instead? (gameplay decision)

**THC Integration:** THC has `MonsterThreatGoalMixin` that modifies target selection. Extend this to check downed state - if target is downed and threat map has other players, switch to them. Clear threat for downed player.

**Phase:** Downed state behavior (must be done alongside invulnerability)

---

### Pitfall 4: Player Actions Not Fully Disabled

**What goes wrong:** Downed players can still perform some actions (opening inventory, using items, chat commands, etc.) that break immersion or allow exploits.

**Why it happens:**
- No single "disable player" flag in Minecraft
- Different actions are handled by different systems (ServerGamePacketListener, PlayerInventory, etc.)
- Easy to forget edge cases like right-clicking containers, dropping items, swapping hands

**Consequences:**
- Downed players open chests and loot mid-combat
- Item dropping to allies as a "death drop" mechanic
- Hotbar switching allows item juggling
- Self-revival exploits via consumables

**Warning signs:**
- Testing reveals any action that works while downed
- Players report being able to do X while down

**Prevention:**
1. Create comprehensive list of actions to block:
   - Movement (already handled by pose/immobility)
   - Attacks (melee, bow, crossbow)
   - Item use (right-click)
   - Block interaction (containers, doors)
   - Inventory access
   - Item dropping
   - Hotbar/offhand swapping
2. Consider using `PlayerInteractCallback` and similar events to cancel
3. Mixin into packet handler to reject disallowed packets from downed players
4. Test with automation: try every possible action while downed

**THC Integration:** THC already has action-blocking patterns (base combat blocking via `BasePermissions`). Consider similar event-based blocking for downed state, but server-side packet rejection is more secure.

**Phase:** Downed state behavior (comprehensive action blocking)

---

### Pitfall 5: Revival Progress Lost on Chunk Unload/Rejoin

**What goes wrong:** Revival progress is stored only in memory and is lost when the downed player's chunk unloads, they disconnect, or the reviver disconnects.

**Why it happens:**
- Attachment data defaults to non-persistent
- Revival progress may be stored on wrong entity (reviver vs downed)
- Chunk unload callbacks are easy to forget

**Consequences:**
- 80% revived player loses all progress when chunk unloads
- Disconnect/reconnect griefing (reviver leaves to deny revival)
- Solo players in downed state when chunk unloads = permanent death

**Warning signs:**
- Progress resets when walking far from downed player
- Rejoining shows 0% progress despite prior work

**Prevention:**
1. Store progress on the DOWNED player (not reviver) - they're the entity that persists
2. Make progress attachment PERSISTENT (use `builder.persistent(Codec.FLOAT)`)
3. Consider: Should progress decay over time? (design decision for balance)
4. Handle edge cases:
   - Reviver disconnects: progress stays, another player can continue
   - Downed player disconnects: save progress, restore on rejoin OR force death
   - Chunk unloads with downed player: progress persists in chunk data

**THC Integration:** Use same pattern as `PLAYER_CLASS` attachment which has `persistent(Codec.STRING)` and `copyOnDeath()`. Revival progress should persist but NOT copy on death.

**Phase:** Revival mechanics (core progress system)

---

## Moderate Pitfalls

Mistakes that cause delays or technical debt.

---

### Pitfall 6: Radial Progress HUD Positioning Issues

**What goes wrong:** The radial progress bar around the cursor doesn't account for GUI scale, aspect ratio, or conflicts with other HUD elements.

**Why it happens:**
- Cursor position calculation differs from HUD element positioning
- Minecraft GUI scale affects element sizes non-linearly
- Crosshair is not always centered (bobbing, shaders)

**Consequences:**
- Progress bar off-center at different GUI scales
- Overlap with crosshair makes both unreadable
- Wide/ultrawide monitors show distorted circles
- Shader mods move crosshair but not progress bar

**Warning signs:**
- Testing at GUI scale 1 works, but scale 4 is broken
- Circle looks elliptical on widescreen
- Bar doesn't follow crosshair in third-person view

**Prevention:**
1. Calculate cursor position dynamically: `screenWidth/2`, `screenHeight/2`
2. Use fixed pixel radius, not percentage of screen
3. Test at all GUI scale settings (1, 2, 3, 4, Auto)
4. Consider hiding in third-person (or not showing at all - debatable)
5. Z-order: render AFTER crosshair to overlay correctly

**THC Integration:** THC's `BucklerHudRenderer` uses `HudElementRegistry.attachElementAfter()` for ordering. Revival HUD should similarly attach after relevant elements. Use `guiGraphics.guiWidth()` and `guiHeight()` for centering.

**Phase:** HUD implementation (client rendering phase)

---

### Pitfall 7: Support Class Bonus Not Applied Correctly

**What goes wrong:** The Support class 2x revival speed (1.0/tick vs 0.5/tick) is applied inconsistently or calculated wrong.

**Why it happens:**
- Class check happens on wrong player (downed vs reviver)
- Class attachment not synced to client for prediction
- Edge case: multiple revivers with different classes

**Consequences:**
- Support class feels no different
- Wrong player gets speed bonus
- Inconsistent experience between plays

**Warning signs:**
- Stopwatch timing shows same duration regardless of class
- Debug logging shows wrong class being checked

**Prevention:**
1. Always check REVIVER's class (they do the reviving work)
2. Single reviver model simplifies: only one player can revive at a time
3. Server calculates progress increment: `baseRate * classMultiplier`
4. Client gets progress value via packet, doesn't calculate independently

**THC Integration:** THC has `ClassManager.getClass(ServerPlayer)` pattern. Use this on the reviver during progress tick calculation.

**Phase:** Revival mechanics (class integration)

---

### Pitfall 8: Invulnerability Bypass via Special Damage Sources

**What goes wrong:** Certain damage types bypass the downed player's invulnerability (void, /kill, bypass_invul tagged damage).

**Why it happens:**
- Invulnerability checks happen at different points for different damage types
- Void damage and /kill use special code paths
- Some mods add damage types with bypass flags

**Consequences:**
- Downed player in void dies anyway
- Admin /kill bypasses downed state (may be intended or not)
- Mod compatibility issues

**Warning signs:**
- Player dies while downed from non-obvious sources
- /kill doesn't put player in downed state, just kills them

**Prevention:**
1. Decide on design intent: should ANY damage be fatal while downed?
2. For void damage: either prevent falling into void while downed, or accept death
3. For /kill: probably should bypass (admin override)
4. Use `source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)` to detect and handle

**THC Integration:** THC's `LivingEntityMixin.thc$isEnvironmentalDamage` shows pattern for damage source checking. Create similar `thc$shouldBypassDownedState(DamageSource)` check.

**Phase:** Downed state behavior (damage handling)

---

### Pitfall 9: Revival Visuals Not Visible to Others

**What goes wrong:** The reviving player sees the progress bar, but the downed player and spectators don't see any indication that revival is happening.

**Why it happens:**
- HUD rendering is local to the reviving player
- Particles/effects need to be spawned on server for all clients
- No feedback loop for downed player

**Consequences:**
- Downed player has no hope indicator
- Spectators can't tell if revival is in progress
- Multiple revivers might both try (if allowed)

**Warning signs:**
- Downed player reports seeing nothing happen
- Third-party observers can't tell revival is occurring

**Prevention:**
1. Server spawns particles around downed player during revival
2. Send progress to downed player's client for their own HUD
3. Consider sound effect loop during revival
4. Green particles on completion (specified in requirements)

**THC Integration:** Server particle spawning is straightforward: `serverLevel.sendParticles()`. Add to revival tick processing.

**Phase:** Revival feedback (visual polish phase)

---

### Pitfall 10: Bleedout Timer Implementation Confusion

**What goes wrong:** Deciding whether to have a bleedout timer (die after N seconds if not revived) creates design ambiguity and implementation complexity.

**Why it happens:**
- Spec says "downed state" but doesn't specify duration limit
- Other revival mods typically have bleedout timers
- Solo play becomes impossible with infinite downed state

**Consequences:**
- Design confusion during implementation
- Feature creep adding timer mid-implementation
- Solo players stuck forever if no timer

**Warning signs:**
- Unsure whether to add timer or not
- Solo play testing reveals permanent stuck state

**Prevention:**
1. Make design decision BEFORE implementation:
   - Option A: No timer (infinite downed state, rely on eventual mob cleanup)
   - Option B: Fixed timer (e.g., 60 seconds then die)
   - Option C: Configurable timer
2. For THC context: Consider that this is hardcore-inspired. Infinite downed state may be too forgiving.
3. If adding timer: show countdown on downed player's HUD

**THC Integration:** This is a DESIGN DECISION for the roadmap, not a technical pitfall. Recommend addressing in requirements before implementation begins.

**Phase:** Requirements clarification (before implementation)

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

---

### Pitfall 11: Revival Range Check Edge Cases

**What goes wrong:** The 2-block revival range creates edge cases (diagonal distance, Y-axis, moving during revival).

**Why it happens:**
- 2 blocks Euclidean vs Manhattan distance
- Vertical distance (downed player on different Y level)
- Both entities moving slightly (reviver crouching = slow movement)

**Consequences:**
- Revival cancels unexpectedly
- Players unsure if they're in range
- Reviver can "walk while crouching" out of range

**Prevention:**
1. Use `player.distanceTo(downedPlayer) <= 2.0` (Euclidean, all axes)
2. Check range every tick during revival
3. Cancel revival if range exceeded (don't just pause)
4. Consider visual range indicator (optional polish)

**Phase:** Revival mechanics (core range checking)

---

### Pitfall 12: Downed State Persists After Revival

**What goes wrong:** State flags (isDowned, isInvulnerable, pose) don't fully reset after revival, causing residual issues.

**Why it happens:**
- Forgetting to reset all state variables
- Event-based state changes not firing correctly
- Client-side state not synced after revival

**Consequences:**
- Player still looks lying down after revival
- Invulnerability persists (player can't take damage)
- Progress bar still visible

**Prevention:**
1. Create explicit `clearDownedState()` method that resets ALL related state:
   - `setDowned(false)`
   - `setInvulnerable(false)`
   - `setPose(Pose.STANDING)`
   - `setRevivalProgress(0)`
   - Clear any visual effects
2. Send explicit sync packet after revival
3. Test state after revival thoroughly

**Phase:** Revival mechanics (completion handling)

---

### Pitfall 13: Hunger Value on Revival

**What goes wrong:** Setting hunger to 0 on revival (as per spec) may interact strangely with THC's saturation-tiered healing system.

**Why it happens:**
- THC requires hunger >= 18 to heal naturally
- 0 hunger means no healing possible immediately
- Saturation mechanics are custom

**Consequences:**
- Revived player can't heal at all
- Must eat before any recovery begins
- May be too punishing (or intended?)

**Warning signs:**
- Players revive with 4 hearts but can't heal
- Confusion about why healing doesn't work

**Prevention:**
1. Design decision: Is 0 hunger intentional for difficulty?
   - If yes: document clearly, this is a feature
   - If no: set to a non-zero value (e.g., 6 hunger bars = 12 points)
2. Consider giving small grace period of healing after revival
3. Test with THC's `FoodDataMixin` to ensure no conflicts

**THC Integration:** THC's healing requires hunger >= 18. Setting hunger to 0 on revival means revived players MUST eat before healing. This may be the intended design for "risk in recovery."

**Phase:** Revival mechanics (revival outcome)

---

### Pitfall 14: Attachment Initialization Timing

**What goes wrong:** Downed state attachment accessed before initialization causes null pointer or default value issues.

**Why it happens:**
- Attachments initialize lazily on first access
- Early game ticks might access state before player fully loads
- `copyOnDeath()` behavior with new attachment types

**Consequences:**
- NullPointerException on first downed check
- Player incorrectly treated as downed on login
- State corruption after death/respawn

**Warning signs:**
- Crashes on first damage
- Players spawn in downed state

**Prevention:**
1. Use `getAttachedOrDefault()` pattern instead of raw `getAttached()`
2. Initialize with safe default (e.g., `isDowned = false`)
3. DO NOT use `copyOnDeath()` for downed state (death should clear it)
4. Test first-login and respawn scenarios

**THC Integration:** Follow THC's existing patterns. `WIND_CHARGE_BOOSTED` shows non-persistent boolean pattern. Downed state should be similar: non-persistent, defaults to false.

**Phase:** Attachment implementation (foundation phase)

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|----------------|------------|
| Damage interception | Race conditions (#1) | Use existing lethal parry pattern, test void/kill |
| Downed state entry | Pose desync (#2) | Server-side pose, explicit sync packet |
| Invulnerability | Mob targeting (#3) | Modify threat system, clear threat on down |
| Action blocking | Incomplete blocking (#4) | Comprehensive list, packet-level rejection |
| Revival progress | Persistence (#5) | Store on downed player, make persistent |
| HUD rendering | Positioning (#6) | Dynamic centering, test all GUI scales |
| Class integration | Wrong player check (#7) | Check reviver's class, not downed player |
| Revival completion | State residue (#12) | Explicit clear method, sync packet |
| Testing | Solo play | Design decision on bleedout timer (#10) |

---

## THC-Specific Integration Notes

### Existing Patterns to Reuse

1. **Attachment Pattern:** `THCAttachments` shows both persistent (`PLAYER_CLASS`) and non-persistent (`WIND_CHARGE_BOOSTED`) patterns
2. **Client State Pattern:** `BucklerClientState` + `BucklerStatePayload` + `BucklerSync` for server->client state sync
3. **HUD Pattern:** `BucklerHudRenderer` with `HudElementRegistry` for proper HUD integration
4. **Damage Interception:** `LivingEntityMixin.thc$applyLethalParry` at TAIL of hurtServer
5. **Action Blocking:** `BasePermissions` event-based blocking pattern

### New Patterns Needed

1. **Pose Override:** Need mixin for custom pose behavior (lying down without swimming mechanics)
2. **Multi-Player State:** Revival involves two players - new pattern for relationship state
3. **Radial HUD:** New cursor-centered rendering (different from bottom-bar poise meter)
4. **Progress Sync:** Bi-directional: reviver sees progress, downed player sees being revived

### Compatibility Concerns

1. **Buckler System:** Can downed players raise buckler? (No - action blocking)
2. **Threat System:** Clear threat on downed player? (Yes - see Pitfall #3)
3. **Class System:** Support class bonus on revival (check reviver's class)
4. **Saturation Healing:** 0 hunger = no healing until eating (intentional?)

---

## Sources

- [PlayerRevive Mod](https://www.curseforge.com/minecraft/mc-mods/playerrevive) - Reference implementation, common patterns
- [Incapacitated Mod](https://modrinth.com/mod/incapacitated) - Down limit configuration, instant-kill causes
- [Down But Not Out](https://modrinth.com/mod/down-but-not-out) - Edge case handling (lava, solo play, log-out penalties)
- [Hardcore Revival](https://modrinth.com/mod/hardcore-revival) - Hardcore mode integration patterns
- [Fabric Wiki - Persistent State](https://wiki.fabricmc.net/tutorial:persistent_states) - Attachment persistence patterns
- [Fabric Wiki - Events](https://docs.fabricmc.net/develop/events) - Event-based action blocking
- [Fabric Wiki - Damage Types](https://docs.fabricmc.net/develop/entities/damage-types) - Damage source handling
- [Technical Minecraft Wiki - Client/Server Desync](https://technical-minecraft.fandom.com/wiki/Client/server_desynchronization) - Sync issues
- THC codebase analysis: `LivingEntityMixin`, `THCAttachments`, `BucklerSync`, `BucklerHudRenderer`
