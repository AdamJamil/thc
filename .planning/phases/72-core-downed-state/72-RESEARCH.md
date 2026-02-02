# Phase 72: Core Downed State - Research

**Researched:** 2026-02-02
**Domain:** Minecraft Fabric death interception, game mode manipulation, player state tracking
**Confidence:** HIGH

## Summary

Phase 72 implements the foundation of the revival system: intercepting player death, switching to spectator mode, and enforcing a 50-block tether to the downed location. The technical approach is straightforward and well-supported by Fabric API.

The standard approach uses:
1. `ServerLivingEntityEvents.ALLOW_DEATH` event to intercept death (Fabric API)
2. `ServerPlayer.setGameMode(GameType.SPECTATOR)` to switch game modes
3. Non-persistent attachment to track downed state and location
4. Server tick hook to enforce the 50-block tether via teleportation

All components are already proven patterns in the THC codebase. The existing `THCAttachments` pattern, `ServerPlayerMixin` tick hook pattern, and `BucklerState` accessor pattern provide templates for implementation.

**Primary recommendation:** Use Fabric API `ServerLivingEntityEvents.ALLOW_DEATH` event (not mixin) for death interception, switch to spectator mode immediately, and store downed location in a non-persistent attachment. Enforce tether via existing tick hook in `ServerPlayerMixin`.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.141.0+1.21.11 | Death event interception | `ServerLivingEntityEvents.ALLOW_DEATH` is the official API for this |
| Minecraft | 1.21.11 | Game mode API | `ServerPlayer.setGameMode()` is the native approach |
| Fabric Attachment API | (included in Fabric API) | State storage | Non-persistent attachments for session-scoped state |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Vec3 | (Minecraft) | Position math | Distance calculations for tether enforcement |
| BlockPos | (Minecraft) | Integer position | Not needed - Vec3 preserves sub-block precision |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Fabric API ALLOW_DEATH | Mixin on LivingEntity.die() | Mixin is more fragile, ALLOW_DEATH is purpose-built |
| Non-persistent attachment | Persistent attachment | Downed state should not survive server restart |
| Vec3 for downed location | BlockPos | Vec3 preserves exact death position for teleport |

**Installation:**
Already included - Fabric API is a project dependency.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── downed/              # New package for downed state
│   ├── DownedState.java        # Attachment accessor (like BucklerState)
│   └── DownedManager.java      # Event registration and tether logic
├── THCAttachments.java         # Add DOWNED_LOCATION attachment
└── mixin/
    └── ServerPlayerMixin.java  # Add tether check in tick()
```

### Pattern 1: Death Interception via Fabric API Event
**What:** Register callback on `ServerLivingEntityEvents.ALLOW_DEATH` to intercept death
**When to use:** Any time you need to prevent or modify death behavior
**Example:**
```java
// Source: Fabric API documentation
ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
    if (entity instanceof ServerPlayer player) {
        // Switch to spectator, store location, cancel death
        player.setGameMode(GameType.SPECTATOR);
        DownedState.setDownedLocation(player, player.position());
        return false; // Cancel death
    }
    return true; // Allow death for non-players
});
```

### Pattern 2: Non-Persistent Attachment for Session State
**What:** Attachment without `.persistent()` - resets on server restart
**When to use:** State that should not survive server restart (downed state, wind charge boost)
**Example:**
```java
// Source: THCAttachments.java (existing pattern)
public static final AttachmentType<Vec3> DOWNED_LOCATION = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "downed_location"),
    builder -> builder.initializer(() -> null)
    // No .persistent() - resets on restart
);
```

### Pattern 3: Tick-Based Tether Enforcement
**What:** Check player position each tick, teleport back if too far
**When to use:** Enforcing spatial constraints on players
**Example:**
```java
// Source: ServerPlayerMixin.java (existing tick hook pattern)
@Inject(method = "tick", at = @At("HEAD"))
private void thc$enforceTether(CallbackInfo ci) {
    ServerPlayer self = (ServerPlayer) (Object) this;
    Vec3 downedLoc = DownedState.getDownedLocation(self);
    if (downedLoc != null && self.isSpectator()) {
        double distSq = self.position().distanceToSqr(downedLoc);
        if (distSq > 50.0 * 50.0) { // 50 block radius
            self.teleportTo(downedLoc.x, downedLoc.y, downedLoc.z);
        }
    }
}
```

### Pattern 4: State Accessor Class
**What:** Static utility class for attachment CRUD operations
**When to use:** Any attachment that needs multiple get/set operations
**Example:**
```java
// Source: BucklerState.java, ThreatManager.java (existing patterns)
public final class DownedState {
    private DownedState() {}

    private static AttachmentTarget target(ServerPlayer player) {
        return (AttachmentTarget) player;
    }

    public static Vec3 getDownedLocation(ServerPlayer player) {
        return target(player).getAttached(THCAttachments.DOWNED_LOCATION);
    }

    public static void setDownedLocation(ServerPlayer player, Vec3 location) {
        target(player).setAttached(THCAttachments.DOWNED_LOCATION, location);
    }

    public static boolean isDowned(ServerPlayer player) {
        return getDownedLocation(player) != null && player.isSpectator();
    }

    public static void clearDowned(ServerPlayer player) {
        target(player).setAttached(THCAttachments.DOWNED_LOCATION, null);
    }
}
```

### Anti-Patterns to Avoid
- **Mixin on LivingEntity.die():** Fabric API provides ALLOW_DEATH event specifically for this; mixins are more fragile and harder to maintain
- **Persistent attachment for downed state:** If server restarts, downed players would be stuck in spectator mode with no way to revive - non-persistent is correct
- **BlockPos for downed location:** Loses sub-block precision; player could fall into block if teleported to BlockPos center
- **Checking health <= 0 repeatedly:** ALLOW_DEATH event already handles this; don't re-implement detection

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Death detection | Mixin on damage/health checks | `ServerLivingEntityEvents.ALLOW_DEATH` | Handles all edge cases, includes damage source info |
| Game mode change | Manual ability flag setting | `ServerPlayer.setGameMode()` | Handles client sync, ability updates automatically |
| Distance calculation | Manual coordinate subtraction | `Vec3.distanceToSqr()` | Already optimized, avoids sqrt until needed |
| Position storage | Three separate attachments (x,y,z) | Single Vec3 attachment | Vec3 serializes automatically via Codec |

**Key insight:** The Fabric API event system is designed exactly for death interception. The documentation explicitly mentions "minigame mod teleporting the player" and "switching the player over to the mod's play-mode" as intended use cases.

## Common Pitfalls

### Pitfall 1: Not Handling Subsequent Ticks
**What goes wrong:** Canceling death in ALLOW_DEATH but not restoring health causes death next tick
**Why it happens:** Vanilla checks `health <= 0` each tick and kills if true
**How to avoid:** Switch to spectator mode immediately - spectators cannot die
**Warning signs:** Player dies one tick after cancel

### Pitfall 2: Using GameMode Instead of GameType
**What goes wrong:** Compilation error or wrong import
**Why it happens:** Fabric (Yarn mappings) uses `GameMode`, Mojang mappings use `GameType`
**How to avoid:** This project uses Mojang mappings - use `GameType.SPECTATOR`
**Warning signs:** Import from `net.minecraft.world.level.GameType`

### Pitfall 3: Forgetting Client Sync for Game Mode
**What goes wrong:** Client shows wrong game mode, abilities don't update
**Why it happens:** Directly setting abilities without proper sync
**How to avoid:** Use `ServerPlayer.setGameMode()` which handles sync automatically
**Warning signs:** Client-side issues, incorrect HUD display

### Pitfall 4: Storing BlockPos Instead of Vec3
**What goes wrong:** Player teleports into block or falls when revived
**Why it happens:** BlockPos loses fractional position; center of block might be inside floor
**How to avoid:** Store exact Vec3 from `player.position()` at death
**Warning signs:** Player takes fall damage or suffocates on revival

### Pitfall 5: Teleporting Before Game Mode Change
**What goes wrong:** Player dies during teleport
**Why it happens:** Survival player in void/lava still takes damage during teleport
**How to avoid:** Set spectator mode FIRST, then store location
**Warning signs:** Void death loops

## Code Examples

Verified patterns from official sources and existing codebase:

### Death Interception Event Registration
```java
// Source: Fabric API ServerLivingEntityEvents, THC.kt registration pattern
public static void register() {
    ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
        if (!(entity instanceof ServerPlayer player)) {
            return true; // Allow non-player deaths
        }

        // Store location BEFORE switching mode (for tether reference point)
        Vec3 deathLocation = player.position();

        // Switch to spectator (makes player invulnerable, invisible to mobs)
        player.setGameMode(GameType.SPECTATOR);

        // Store downed location in attachment
        DownedState.setDownedLocation(player, deathLocation);

        // Cancel death
        return false;
    });
}
```

### Tether Enforcement in Tick
```java
// Source: ServerPlayerMixin.java existing tick pattern
@Inject(method = "tick", at = @At("HEAD"))
private void thc$enforceTether(CallbackInfo ci) {
    ServerPlayer self = (ServerPlayer) (Object) this;
    Vec3 downedLoc = DownedState.getDownedLocation(self);

    // Only enforce tether for downed players
    if (downedLoc == null) {
        return;
    }

    // 50 block tether radius (squared to avoid sqrt)
    double distSq = self.position().distanceToSqr(downedLoc);
    if (distSq > 2500.0) { // 50 * 50 = 2500
        self.teleportTo(downedLoc.x, downedLoc.y, downedLoc.z);
    }
}
```

### Non-Persistent Vec3 Attachment
```java
// Source: THCAttachments.java pattern + Vec3 codec
public static final AttachmentType<Vec3> DOWNED_LOCATION = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "downed_location"),
    builder -> builder.initializer(() -> null)
    // Intentionally non-persistent - downed state should not survive restart
);
```

### isDowned State Check
```java
// Source: DownedState accessor pattern (like BucklerState.isBroken())
public static boolean isDowned(ServerPlayer player) {
    Vec3 loc = getDownedLocation(player);
    // Downed = has location AND in spectator mode
    return loc != null && player.isSpectator();
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ServerPlayerEvents.ALLOW_DEATH` | `ServerLivingEntityEvents.ALLOW_DEATH` | Fabric API 0.75+ | Old API deprecated, new one works for all living entities |
| Manual ability flags | `setGameMode()` | Always preferred | Automatic client sync, proper ability handling |

**Deprecated/outdated:**
- `ServerPlayerEvents.ALLOW_DEATH`: Deprecated in favor of `ServerLivingEntityEvents.ALLOW_DEATH` with instanceof check

## Open Questions

Things that couldn't be fully resolved:

1. **Vec3 Codec for Attachment**
   - What we know: Minecraft has `Vec3.CODEC` for serialization
   - What's unclear: Whether non-persistent attachments even need codecs (they shouldn't serialize)
   - Recommendation: Start without codec since attachment is non-persistent; add if compilation requires it

2. **Spectator Teleport Behavior**
   - What we know: `teleportTo()` works for spectators
   - What's unclear: Whether spectator camera smoothly transitions or jumps
   - Recommendation: Implement with basic teleport; smooth transition can be added in Phase 74 if needed

## Sources

### Primary (HIGH confidence)
- [ServerLivingEntityEvents Fabric API 0.119.2+1.21.5](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html) - ALLOW_DEATH event documentation
- [Fabric Wiki Event Index](https://wiki.fabricmc.net/tutorial:event_index) - Event listing and patterns
- THCAttachments.java - Existing non-persistent attachment pattern (FIRE_SOURCE, WIND_CHARGE_BOOSTED)
- ServerPlayerMixin.java - Existing tick hook pattern
- BucklerState.java - Existing accessor class pattern
- THCBucklerGameTests.java - `setGameMode(GameType.SURVIVAL)` usage proving API works

### Secondary (MEDIUM confidence)
- [ServerPlayerInteractionManager Yarn API](https://maven.fabricmc.net/docs/yarn-22w11a+build.2/net/minecraft/server/network/ServerPlayerInteractionManager.html) - Game mode change mechanics
- Minecraft Wiki Spectator - Spectator mode behavior

### Tertiary (LOW confidence)
- None - all critical claims verified with primary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Fabric API documentation is explicit about ALLOW_DEATH use case
- Architecture: HIGH - All patterns already exist in THC codebase
- Pitfalls: HIGH - Based on Fabric API docs warning and existing codebase patterns

**Research date:** 2026-02-02
**Valid until:** 60 days (stable APIs, Minecraft version locked)
