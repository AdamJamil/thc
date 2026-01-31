# Architecture Patterns: Revival System

**Domain:** Revival/downed state system for Minecraft Fabric mod
**Researched:** 2026-01-31
**Confidence:** HIGH (based on existing codebase patterns + Fabric API docs)

## Executive Summary

The revival system integrates cleanly with THC's existing architecture. The mod already uses:
- **Fabric Attachment API** for entity state (poise, class, threat)
- **Server tick events** for state updates
- **CustomPacketPayload + StreamCodec** for client sync
- **HUD rendering** via Fabric's HudElementRegistry
- **UseEntityCallback** for player-entity interactions

The revival system follows these same patterns. Key architectural decisions:
1. Use `ServerLivingEntityEvents.ALLOW_DEATH` to intercept lethal damage
2. Store downed state via Attachment API (non-persistent)
3. Use `Pose.SWIMMING` for crawling visual (vanilla mechanic)
4. Sync progress to client via new payload for radial HUD rendering
5. Modify mob AI targeting via mixin to ignore downed players

## Integration Points with Existing Components

### Death Interception

**Existing Pattern:** `LivingEntityMixin.thc$applyLethalParry` already intercepts lethal damage at `@At("TAIL")` of `hurtServer` and can set `player.setHealth(1.0F)` to prevent death.

**Revival Integration:**
- Use `ServerLivingEntityEvents.ALLOW_DEATH` (Fabric API) as primary hook
- Return `false` to cancel death, immediately set downed state
- Set health to 1.0 to prevent next-tick death check (`LivingEntity.isDead()` checks `health <= 0`)

**Why ALLOW_DEATH over mixin:** The mod already uses Fabric events where available. ALLOW_DEATH is the canonical way to prevent player death and handles edge cases.

**Sources:** [Fabric API ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html)

### State Storage

**Existing Pattern:** `THCAttachments.java` defines all attachments. Examples:
```java
// Non-persistent (session-scoped)
public static final AttachmentType<Boolean> WIND_CHARGE_BOOSTED = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "wind_charge_boosted"),
    builder -> builder.initializer(() -> Boolean.FALSE)
);

// Persistent (survives death via copyOnDeath)
public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "player_class"),
    builder -> {
        builder.initializer(() -> null);
        builder.persistent(Codec.STRING);
        builder.copyOnDeath();
    }
);
```

**Revival Attachments Needed:**
| Attachment | Type | Persistent | Purpose |
|------------|------|------------|---------|
| `DOWNED` | Boolean | No | Is player in downed state |
| `REVIVAL_PROGRESS` | Double | No | Progress toward revival (0.0-1.0) |
| `REVIVER_UUID` | UUID | No | Who is currently reviving |
| `DOWNS_COUNT` | Integer | No | Number of downs this life (for bleed-out) |

**Note:** State should NOT persist through death. Respawning is fresh. Attachments without `persistent()` are session-only.

### Progress Tracking

**Existing Pattern:** Poise system tracks progress per-tick in `THC.updateBucklerState()`:
```kotlin
ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
    for (player in server.playerList.players) {
        // Read state, update values, sync to client
        BucklerSync.sync(player)
    }
})
```

**Revival Tick Logic:**
```
For each player:
  If player is reviving someone:
    If within 2 blocks AND sneaking AND not moving:
      Increment target's REVIVAL_PROGRESS by rate
      Rate = 1.0/tick if Support class, else 0.5/tick
      Sync progress to client
    Else:
      Clear reviver (but preserve progress)

  If player is downed:
    If REVIVAL_PROGRESS >= 1.0:
      Complete revival (set health, clear state)
    Decrement bleed-out timer (optional)
```

### Client/Server Networking

**Existing Pattern:** `BucklerStatePayload.java` + `BucklerSync.java`:
```java
// Payload definition
public record BucklerStatePayload(double poise, double maxPoise, boolean broken, long lastFullTick)
    implements CustomPacketPayload {
    public static final Type<BucklerStatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath("thc", "buckler_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BucklerStatePayload> STREAM_CODEC =
        StreamCodec.ofMember(BucklerStatePayload::write, BucklerStatePayload::new);
}

// Registration in THC.onInitialize()
PayloadTypeRegistry.playS2C().register(BucklerStatePayload.TYPE, BucklerStatePayload.STREAM_CODEC)

// Client receiver in THCClient.onInitializeClient()
ClientPlayNetworking.registerGlobalReceiver(BucklerStatePayload.TYPE) { payload, context ->
    context.client().execute {
        BucklerClientState.update(...)
    }
}
```

**Revival Payloads Needed:**
| Payload | Direction | Data | Purpose |
|---------|-----------|------|---------|
| `RevivalProgressPayload` | S2C | progress: Double, targetId: Int | Show radial on reviver's HUD |
| `DownedStatePayload` | S2C | isDowned: Boolean, downedPlayerId: Int | Render downed players differently |

### HUD Rendering

**Existing Pattern:** `BucklerHudRenderer.kt` uses Fabric's HUD API:
```kotlin
// Registration in THCClient
HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, BucklerHudRenderer.POISE_ID) { guiGraphics, _ ->
    BucklerHudRenderer.render(guiGraphics)
}
```

**Revival HUD:**
- Radial progress ring around crosshair (not status bar position)
- Custom textures: `revival_progress_empty.png`, `revival_progress_full.png`
- Only visible when actively reviving
- Render in center of screen, not via HudElementRegistry (different location)

**Rendering approach:** Direct render in HUD callback, checking client state for active revival.

### Class Integration (Support Bonus)

**Existing Pattern:** `ClassManager.java` + `PlayerClass.java`:
```java
public enum PlayerClass {
    SUPPORT(0.0, 1.0, 3.0);  // no health change, x1 melee, x3 ranged
    // ...
}

// Usage
PlayerClass playerClass = ClassManager.getClass(serverPlayer);
if (playerClass != null) {
    // Apply class-specific logic
}
```

**Revival Integration:**
```kotlin
val reviver = /* player doing the reviving */
val baseRate = 0.5 / 20.0  // 0.5 progress per tick at 20 TPS
val rate = if (ClassManager.getClass(reviver) == PlayerClass.SUPPORT) {
    1.0 / 20.0  // Support gets 2x revival speed
} else {
    baseRate
}
```

### Mob AI Targeting

**Existing Pattern:** `MonsterThreatGoalMixin.java` modifies mob targeting based on threat values. The mod already hooks into mob targeting.

**Revival Targeting:**
- Downed players should be ignored by mobs
- Options:
  1. **Mixin to NearestAttackableTargetGoal** - Check if target is downed, skip
  2. **Set player invisible** - Makes vanilla AI ignore, but affects rendering
  3. **Modify targeting predicate** - Cleanest, add downed check to target selection

**Recommended:** Mixin to targeting predicates used by hostile mobs. Check `target.getAttached(THCAttachments.DOWNED) == true`, return false from targeting check.

## New Components Needed

### Server-Side

| Component | Type | Purpose |
|-----------|------|---------|
| `RevivalAttachments` | Java class | Define DOWNED, REVIVAL_PROGRESS, REVIVER_UUID attachments |
| `RevivalManager` | Kotlin object | Static utilities for downed state CRUD |
| `RevivalTick` | Kotlin object | Server tick handler for progress updates |
| `RevivalEvents` | Kotlin object | ALLOW_DEATH listener, particle spawning |
| `RevivalPayload` | Java record | Network payload for progress sync |
| `RevivalSync` | Java class | Delta-sync handler (like BucklerSync) |
| `DownedMobTargetMixin` | Java mixin | Prevent mobs targeting downed players |
| `DownedActionMixin` | Java mixin | Block actions while downed (movement, items) |

### Client-Side

| Component | Type | Purpose |
|-----------|------|---------|
| `RevivalClientState` | Java class | Store current revival progress for HUD |
| `RevivalHudRenderer` | Kotlin object | Radial progress ring around crosshair |
| `DownedClientState` | Java class | Track which players are downed |
| `DownedPlayerRenderer` | Kotlin object | Visual modifications for downed players |

### Mixins

| Mixin | Target | Purpose |
|-------|--------|---------|
| `PlayerMovementMixin` | Player | Block movement inputs while downed |
| `PlayerActionMixin` | Player | Block item use, attacks while downed |
| `MobTargetMixin` | NearestAttackableTargetGoal | Skip downed players |
| `PlayerPoseMixin` | Player | Force SWIMMING pose while downed |

## Data Flow

```
LETHAL DAMAGE
     |
     v
ServerLivingEntityEvents.ALLOW_DEATH
     |
     +--> return false (cancel death)
     +--> set player.health = 1.0
     +--> RevivalManager.setDowned(player, true)
     +--> sync DownedStatePayload to all clients
     |
     v
SERVER TICK (EndTick)
     |
     +--> For each downed player:
     |      Check if being revived
     |      Update progress
     |      Check bleed-out timer
     |
     +--> For each reviving player:
     |      Validate: within 2 blocks, sneaking, not moving
     |      Calculate rate (Support = 1.0, others = 0.5)
     |      Increment target's progress
     |      Sync RevivalProgressPayload to reviver
     |
     v
REVIVAL COMPLETE (progress >= 1.0)
     |
     +--> RevivalManager.revive(player)
     +--> player.setHealth(maxHealth * 0.5)
     +--> player.foodData.setFoodLevel(0)
     +--> clear DOWNED, REVIVAL_PROGRESS
     +--> spawn green particles
     +--> sync cleared state to clients
```

## Component Boundaries

```
+------------------+     +------------------+     +------------------+
|   THCAttachments |     |  RevivalManager  |     |   RevivalTick    |
|------------------|     |------------------|     |------------------|
| DOWNED           |<----|  setDowned()     |<----|  per-tick update |
| REVIVAL_PROGRESS |     |  isReviving()    |     |  progress calc   |
| REVIVER_UUID     |     |  revive()        |     |  validation      |
+------------------+     +------------------+     +------------------+
                                  ^
                                  |
+------------------+     +------------------+     +------------------+
|  RevivalEvents   |     |   RevivalSync    |     |  RevivalPayload  |
|------------------|     |------------------|     |------------------|
| ALLOW_DEATH hook |---->|  delta sync      |---->|  S2C network     |
| particle spawn   |     |  dirty tracking  |     |  progress data   |
+------------------+     +------------------+     +------------------+
                                  |
                                  v
                         +------------------+
                         |    CLIENT        |
                         |------------------|
                         | RevivalClientState
                         | RevivalHudRenderer
                         | DownedPlayerRenderer
                         +------------------+
```

## Suggested Build Order

Based on component dependencies:

### Phase 1: Core State
1. Add attachments to `THCAttachments.java` (DOWNED, REVIVAL_PROGRESS, REVIVER_UUID)
2. Create `RevivalManager` with basic CRUD operations
3. Hook `ServerLivingEntityEvents.ALLOW_DEATH` to trigger downed state

**Validates:** Player enters downed state instead of dying, stays alive

### Phase 2: Action Blocking
4. Create mixins to block movement while downed
5. Create mixins to block item use/attacks while downed
6. Force Pose.SWIMMING for crawling visual

**Validates:** Downed player cannot act, shows crawling pose

### Phase 3: Revival Mechanics
7. Implement revival detection (UseEntityCallback or tick-based proximity check)
8. Add `RevivalTick` for progress calculation
9. Integrate Support class bonus

**Validates:** Sneaking near downed player accumulates progress

### Phase 4: Client Sync
10. Create `RevivalPayload` and `RevivalSync`
11. Register payload in THC/THCClient
12. Create `RevivalClientState` to store received progress

**Validates:** Client knows about revival progress

### Phase 5: HUD Rendering
13. Create radial progress textures
14. Implement `RevivalHudRenderer` with ring around crosshair
15. Only show when actively reviving

**Validates:** Visual feedback during revival

### Phase 6: Mob AI
16. Create mixin to prevent mob targeting of downed players
17. Verify with hostile mobs in testing

**Validates:** Mobs ignore downed players

### Phase 7: Polish
18. Add green particles on revival completion
19. Set revival outcome (50% HP, 0 hunger)
20. Add sound effects

**Validates:** Complete revival experience

## Patterns to Follow

### Pattern 1: Attachment State Manager
**What:** Centralized static utility class for attachment CRUD
**When:** Any entity state that needs get/set/clear operations
**Example:**
```java
public final class RevivalManager {
    public static boolean isDowned(ServerPlayer player) {
        return Boolean.TRUE.equals(player.getAttached(THCAttachments.DOWNED));
    }

    public static void setDowned(ServerPlayer player, boolean downed) {
        player.setAttached(THCAttachments.DOWNED, downed);
        if (!downed) {
            player.removeAttached(THCAttachments.REVIVAL_PROGRESS);
            player.removeAttached(THCAttachments.REVIVER_UUID);
        }
    }
}
```

### Pattern 2: Delta Sync
**What:** Only send network packets when state changes
**When:** Frequently updated state synced to client
**Example:**
```java
public static void sync(ServerPlayer player) {
    State current = State.fromPlayer(player);
    State previous = LAST_SENT.get(player.getUUID());
    if (current.equals(previous)) return;
    LAST_SENT.put(player.getUUID(), current);
    send(player, current);
}
```

### Pattern 3: Tick-Based Progress
**What:** Accumulate progress in server tick handler
**When:** Continuous actions over time (revival, channel effects)
**Example:**
```kotlin
ServerTickEvents.END_SERVER_TICK.register { server ->
    for (player in server.playerList.players) {
        if (isValidReviveAction(player)) {
            val target = getReviveTarget(player)
            val rate = getRevivalRate(player)
            RevivalManager.addProgress(target, rate)
        }
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Client Authority
**What:** Letting client determine downed state or progress
**Why bad:** Cheats can skip downed state entirely
**Instead:** Server is authoritative for all revival state. Client only renders.

### Anti-Pattern 2: Blocking Death in hurtServer
**What:** Using mixin in `hurtServer` to prevent death
**Why bad:** Misses edge cases, doesn't integrate with vanilla death logic
**Instead:** Use `ServerLivingEntityEvents.ALLOW_DEATH` which fires at the right time.

### Anti-Pattern 3: Persistent Downed State
**What:** Making DOWNED attachment persistent/copyOnDeath
**Why bad:** Player would respawn still downed after timeout
**Instead:** All revival state is session-only. Death resets everything.

### Anti-Pattern 4: Per-Tick Client Sync
**What:** Sending packets every tick regardless of state change
**Why bad:** Network spam, performance impact with many players
**Instead:** Use delta sync pattern - only send when state changes.

## Sources

**HIGH Confidence (Official Documentation):**
- [Fabric API ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html) - ALLOW_DEATH event
- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events) - Event registration patterns

**MEDIUM Confidence (Existing Codebase - Verified Patterns):**
- `THCAttachments.java` - Attachment definition patterns
- `BucklerSync.java` - Network delta sync pattern
- `BucklerHudRenderer.kt` - HUD rendering approach
- `VillagerInteraction.kt` - UseEntityCallback pattern
- `THC.kt` - Server tick event registration

**LOW Confidence (Community Mods - Reference Only):**
- [Down But Not Out](https://modrinth.com/mod/down-but-not-out) - Similar feature set, Fabric
- [Incapacitated](https://modrinth.com/mod/incapacitated) - Crawling state, down counter
- [Hardcore Revival](https://www.curseforge.com/minecraft/mc-mods/hardcore-revival-fabric) - K.O. period, revival
