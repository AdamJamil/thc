# Technology Stack: Revival System

**Project:** THC v3.0 Revival System
**Researched:** 2026-01-31
**Confidence:** HIGH (verified against existing codebase patterns and Fabric API docs)

## Executive Summary

The revival system requires no new dependencies. All capabilities exist within the current THC stack (Fabric API 1.21.11, Mixins, Attachments). The key technical decisions involve:

1. **Death interception** via Fabric API `ServerLivingEntityEvents.ALLOW_DEATH` event (not mixin)
2. **Pose manipulation** via `Entity.setPose(Pose.SWIMMING)` with tick enforcement
3. **Mob exclusion** via `LivingEntity.canBeSeenAsEnemy()` mixin override
4. **Progress tracking** via existing attachment patterns with per-player maps
5. **Client rendering** via existing HUD API patterns with trigonometric circle drawing

## Recommended Stack Additions

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Fabric API `ServerLivingEntityEvents` | Current | Death interception | Official API, mod-compatible, already in project dependencies |
| Fabric Attachment API | Current | Downed state persistence | Matches existing THC patterns (BUCKLER_POISE, PLAYER_CLASS, etc.) |
| Fabric Networking | Current | Client state sync | Matches existing BucklerSync pattern |

### No New Dependencies Required

All required capabilities exist in the current stack:
- `net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents`
- `net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry`
- `net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking`
- `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry`

## Mixin Targets (Mojang Mappings, MC 1.21.11)

### 1. Mob Targeting Exclusion

**Target:** `net.minecraft.world.entity.LivingEntity`
**Method:** `canBeSeenAsEnemy()`
**Injection:** `@Inject` at `HEAD`, return `false` for downed players

```java
@Mixin(LivingEntity.class)
public abstract class DownedPlayerTargetingMixin {
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void thc$hideDownedFromMobs(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            if (DownedState.isDowned(player)) {
                cir.setReturnValue(false);
            }
        }
    }
}
```

**Rationale:** `canBeSeenAsEnemy()` is the standard Minecraft method for mob target validation. Returning `false` makes the entity invisible to all mob AI targeting goals without modifying each mob type individually. Verified in NeoForge 1.21.1 docs.

### 2. Pose Enforcement

**Target:** `net.minecraft.world.entity.Entity`
**Method:** `setPose(Pose)`
**Injection:** `@Inject` at `HEAD` to block pose changes while downed

Alternative approach: Override in tick handler (preferred for THC's patterns)

**Rationale:** Downed players need SWIMMING pose to appear "laying down". Minecraft will attempt to reset pose based on movement/water/etc. Must enforce pose each tick or block setPose calls.

**Recommended Pattern (tick-based):**
```java
// In ServerTickEvents.END_SERVER_TICK handler
for (ServerPlayer player : server.playerList.players) {
    if (DownedState.isDowned(player)) {
        player.setPose(Pose.SWIMMING);
    }
}
```

### 3. Action Blocking

**Target:** Multiple methods need blocking for downed players

| Action | Target Class | Method | Approach |
|--------|--------------|--------|----------|
| Item use | `LivingEntity` | `startUsingItem` | Inject HEAD, cancel |
| Attack | `Player` | `attack` | Inject HEAD, cancel |
| Movement | `ServerPlayer` | `move` (in tick) | Set delta to zero |
| Interaction | Various | `UseEntityCallback` | Event return FAIL |

**Rationale:** Downed players should be completely incapacitated. Block at earliest injection points to prevent any action processing.

### 4. Invulnerability

**Target:** `net.minecraft.world.entity.LivingEntity`
**Method:** `isInvulnerableTo(DamageSource)`
**Injection:** `@Inject` at `HEAD`, return `true` for downed players

```java
@Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
private void thc$downedInvulnerable(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof ServerPlayer player) {
        if (DownedState.isDowned(player)) {
            cir.setReturnValue(true);
        }
    }
}
```

**Rationale:** Downed players must be immune to all damage. `isInvulnerableTo` is checked before any damage application.

## Fabric API Events (NOT Mixins)

### Death Interception

**DO NOT USE MIXIN FOR THIS.** Use Fabric API event.

```java
// In THC.onInitialize()
ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
    if (entity instanceof ServerPlayer player) {
        // Check if already downed (die from bleedout, etc.)
        if (DownedState.isDowned(player)) {
            return true; // Allow actual death
        }

        // Enter downed state instead of dying
        DownedState.enterDowned(player);
        player.setHealth(1.0f); // Prevent isDead() check next tick
        return false; // Cancel death
    }
    return true; // Non-players die normally
});
```

**Method Signature (verified):**
```java
boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount)
```

**Rationale:**
1. Fabric API event is mod-compatible (other mods can also hook death)
2. Cleaner than mixin for this use case
3. Provides damage source and amount for logging/debugging
4. Already part of Fabric API dependencies in THC

**Critical Implementation Note:** The Fabric docs explicitly state:
> "Vanilla checks for entity health <= 0 each tick (with isDead()), and kills if true - so the entity will still die next tick if this event is cancelled."

Must set player health > 0 when entering downed state to prevent immediate death.

## Attachment Patterns

### New Attachments Required

```java
// In THCAttachments.java
public static final AttachmentType<Boolean> PLAYER_DOWNED = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "player_downed"),
    builder -> {
        builder.initializer(() -> Boolean.FALSE);
        // NOT persistent - death resets downed state
    }
);

public static final AttachmentType<Long> DOWNED_TICK = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "downed_tick"),
    builder -> builder.initializer(() -> 0L)
);

// Revival progress: Map<UUID, Float> where UUID is reviver, Float is progress (0.0-1.0)
public static final AttachmentType<Map<UUID, Float>> REVIVAL_PROGRESS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "revival_progress"),
    builder -> builder.initializer(HashMap::new)
);
```

**Pattern Match:** Follows existing THC attachment patterns:
- `MOB_THREAT` uses `Map<UUID, Double>` for per-entity state
- `BUCKLER_POISE`, `WIND_CHARGE_BOOSTED` use simple types
- Non-persistent attachments for session-scoped state (like `FIRE_SOURCE`)

## Client Rendering

### Radial Progress Bar

**API:** `HudElementRegistry` (Fabric API 1.21.8+)
**Pattern:** Match existing `BucklerHudRenderer`

```kotlin
object RevivalProgressRenderer {
    fun render(graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        val target = RevivalClientState.getTarget() ?: return
        val progress = RevivalClientState.getProgress()

        // Center of screen
        val centerX = graphics.guiWidth() / 2
        val centerY = graphics.guiHeight() / 2

        // Draw radial progress using TRIANGLE_FAN
        drawRadialProgress(graphics, centerX, centerY, progress)
    }

    private fun drawRadialProgress(graphics: GuiGraphics, x: Int, y: Int, progress: Float) {
        val radius = 24f
        val innerRadius = 20f
        val segments = 32
        val angle = progress * 2f * Math.PI.toFloat()

        // Use BufferBuilder with POSITION_COLOR for filled segments
        val tessellator = Tessellator.getInstance()
        // ... trigonometric vertex calculation
    }
}
```

**Rendering Approach:**
1. Calculate arc angle from progress (0.0-1.0 maps to 0-360 degrees)
2. Use `TRIANGLE_STRIP` or multiple triangles for filled arc
3. Vertices at (centerX + cos(theta) * radius, centerY + sin(theta) * radius)
4. Inner and outer radius for ring appearance

**Alternative:** Use textured sprites with UV manipulation (simpler but less flexible)

### Client State Sync

**Pattern:** Match existing `BucklerSync` / `BucklerStatePayload`

```java
// RevivalStatePayload.java
public record RevivalStatePayload(
    boolean reviving,           // Currently reviving someone
    @Nullable UUID targetUuid,  // Who is being revived
    float progress              // 0.0-1.0
) implements CustomPacketPayload {
    public static final Type<RevivalStatePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("thc", "revival_state"));
    // ... STREAM_CODEC, write, type methods
}
```

## Integration with Existing THC Systems

### Class System Integration

**Support class bonus:** Already defined in `PlayerClass.java`
```java
SUPPORT(0.0, 1.0, 3.0);  // no health change, x1 melee, x3 ranged
```

Revival speed multiplier should be added as new property or computed from class:
```java
// Option 1: Add to PlayerClass enum
private final double revivalMultiplier;
SUPPORT(0.0, 1.0, 3.0, 2.0);  // 2x revival speed

// Option 2: Check class at runtime
if (PlayerClass.fromString(className) == PlayerClass.SUPPORT) {
    progressPerTick = 1.0f;  // Double rate
} else {
    progressPerTick = 0.5f;  // Base rate
}
```

**Recommendation:** Option 2 (runtime check) is simpler and matches existing patterns.

### Threat System Integration

Downed players should NOT generate or hold threat:
- In `ThreatTargetGoal.canContinueToUse()`: Check `DownedState.isDowned(target)`
- Already handled by `canBeSeenAsEnemy()` returning false

### Base Claiming Integration

No special handling needed - revival works anywhere.

## Pose Values Reference (Mojang Mappings)

From `net.minecraft.world.entity.Pose`:
| Pose | Description | Use for Revival |
|------|-------------|-----------------|
| `STANDING` | Normal upright | Post-revival |
| `SWIMMING` | Horizontal, flat | **Downed state** |
| `DYING` | Death animation | Not usable (triggers death) |
| `SLEEPING` | In bed | Could work but implies rest |
| `SNEAKING` | Crouched | Reviver must be sneaking |

**Recommendation:** Use `SWIMMING` for downed state. It displays the player horizontally without triggering any death-related systems.

## Verification Checklist

- [x] Mixin targets verified via NeoForge 1.21.1 javadocs and Fabric yarn mappings
- [x] `canBeSeenAsEnemy()` exists in `LivingEntity` (verified)
- [x] `isInvulnerableTo(DamageSource)` exists in `LivingEntity` (verified)
- [x] `setPose(Pose)` exists in `Entity` (verified)
- [x] `Pose.SWIMMING` available in MC 1.21 (verified)
- [x] `ServerLivingEntityEvents.ALLOW_DEATH` signature verified
- [x] Attachment patterns match existing THC code
- [x] Network payload patterns match existing THC code
- [x] HUD rendering patterns match existing THC code

## Sources

**HIGH Confidence (Official Docs):**
- [ServerLivingEntityEvents Fabric API 1.21.5](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html)
- [Fabric HUD Rendering Documentation](https://docs.fabricmc.net/develop/rendering/hud)
- [LivingEntity NeoForge 1.21.1](https://lexxie.dev/neoforge/1.21.1/net/minecraft/world/entity/LivingEntity.html)
- [Pose Enum Spigot API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Pose.html)

**MEDIUM Confidence (Verified Patterns):**
- Existing THC codebase (`THCAttachments.java`, `BucklerSync.java`, `BucklerHudRenderer.kt`)
- [Fabric Mappings Wiki](https://wiki.fabricmc.net/tutorial:mappings)

**LOW Confidence (Community):**
- [FabPose mod](https://github.com/YukkuriLaboratory/FabPose) for pose implementation patterns
