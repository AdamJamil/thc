# Phase 73: Revival Mechanics - Research

**Researched:** 2026-02-02
**Domain:** Server-side tick processing, proximity detection, player state modification, particle effects
**Confidence:** HIGH

## Summary

Phase 73 implements the revival mechanics for downed players. The core pattern is a server tick processor that:
1. Iterates all players to find sneaking revivers near downed locations
2. Accumulates progress on the downed player's attachment
3. Completes revival when progress reaches 100% (200 ticks base, 100 ticks for Support)

The existing codebase patterns (BucklerState, ClassManager, ServerTickEvents) provide a proven template. Minecraft's single-threaded server tick eliminates thread-safety concerns for multi-reviver progress accumulation.

**Primary recommendation:** Store revival progress on the downed player's attachment (not the reviver), accumulate progress from all nearby sneaking players each tick, complete revival when progress >= 1.0.

## Standard Stack

The existing codebase provides all necessary patterns:

### Core
| Component | Location | Purpose | Why Standard |
|-----------|----------|---------|--------------|
| THCAttachments | `THCAttachments.java` | Attachment registration | Established pattern for all player/entity state |
| ServerTickEvents | `THC.kt` | Tick processing | Proven pattern for per-tick state updates |
| ClassManager | `ClassManager.java` | Class lookup for Support bonus | Existing integration point |
| BucklerSync pattern | `BucklerSync.java` | State sync to client | Template for RevivalSync (Phase 74) |

### Supporting
| Component | Purpose | When to Use |
|-----------|---------|-------------|
| ServerLevel.sendParticles | Green particles on completion | Revival completion effect |
| FoodDataAccessor pattern | Set food level | Revival outcome (6 hunger) |
| Vec3.distanceToSqr | Proximity check | 2-block radius detection |

### No New Dependencies Required
All functionality is achievable with existing Fabric API + Minecraft vanilla APIs.

## Architecture Patterns

### Recommended Data Structure

Store revival progress on the **downed player**, not the reviver:

```
Downed Player Attachment:
- downedLocation: Vec3 (from Phase 72)
- revivalProgress: Double (0.0 to 1.0)
```

**Why on downed player:**
- Multiple revivers contribute to same progress value
- Progress naturally persists with the downed player
- No need to track reviver->downed mappings
- Simpler state cleanup on revival completion

### Recommended Project Structure
```
src/main/java/thc/
├── revival/
│   ├── RevivalState.java        # State accessors (like BucklerState)
│   └── RevivalManager.java      # Tick processing logic
├── THCAttachments.java          # Add REVIVAL_PROGRESS attachment
└── THC.kt                       # Register tick handler
```

### Pattern 1: Server Tick Processing for Revival
**What:** Each server tick, iterate players and accumulate revival progress
**When to use:** Core revival mechanic
**Example:**
```java
// In THC.kt ServerTickEvents.END_SERVER_TICK
for (ServerPlayer reviver : server.getPlayerList().getPlayers()) {
    if (!reviver.isShiftKeyDown()) continue;  // Must be sneaking

    // Find nearby downed players
    for (ServerPlayer downed : server.getPlayerList().getPlayers()) {
        if (!RevivalState.isDowned(downed)) continue;

        Vec3 downedLoc = RevivalState.getDownedLocation(downed);
        double distSq = reviver.position().distanceToSqr(downedLoc);
        if (distSq > 4.0) continue;  // 2 blocks = 4.0 squared

        // Accumulate progress
        double rate = ClassManager.getClass(reviver) == PlayerClass.SUPPORT
            ? 1.0 / 100.0    // 100 ticks = 5 seconds
            : 0.5 / 100.0;   // 200 ticks = 10 seconds (0.5 progress/tick scaled to 0-1)
        RevivalState.addProgress(downed, rate);

        // Check completion
        if (RevivalState.getProgress(downed) >= 1.0) {
            completeRevival(downed, downedLoc);
        }
    }
}
```

### Pattern 2: Progress Accumulation (Multi-Reviver Support)
**What:** Multiple revivers naturally stack by each adding progress per tick
**When to use:** Atomic progress addition
**Example:**
```java
// RevivalState.java
public static void addProgress(ServerPlayer downed, double amount) {
    double current = getProgress(downed);
    setProgress(downed, Math.min(1.0, current + amount));
}
```

**No special multi-reviver code needed:** Server tick is single-threaded, so concurrent modification is impossible. Two revivers = two `addProgress()` calls per tick = 2x progress rate.

### Pattern 3: Revival Completion
**What:** Restore player to survival mode with specified state
**When to use:** When progress reaches 100%
**Example:**
```java
private static void completeRevival(ServerPlayer player, Vec3 location) {
    // Clear downed state
    RevivalState.clearDowned(player);

    // Restore game mode
    player.setGameMode(GameType.SURVIVAL);

    // Teleport to downed location
    player.teleportTo(location.x, location.y, location.z);

    // Set health to 50% of max
    float maxHealth = player.getMaxHealth();
    player.setHealth(maxHealth * 0.5f);

    // Set food level to 6 (CONTEXT.md override)
    player.getFoodData().setFoodLevel(6);

    // Spawn green particles
    ServerLevel level = player.serverLevel();
    level.sendParticles(
        ParticleTypes.HAPPY_VILLAGER,
        location.x, location.y + 1.0, location.z,
        30,      // count
        0.5, 0.5, 0.5,  // spread
        0.0      // speed
    );
}
```

### Anti-Patterns to Avoid
- **Storing progress on reviver:** Creates complexity with multiple revivers and reviver switching
- **Using AtomicDouble:** Unnecessary complexity; server tick is single-threaded
- **Custom networking for progress sync:** Phase 74 handles UI sync separately

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Proximity detection | Custom distance math | `Vec3.distanceToSqr()` | Pre-optimized, avoids sqrt |
| Player sneaking check | Keyboard state tracking | `player.isShiftKeyDown()` | Built-in, always synced |
| Food level modification | Custom FoodData mixin | `FoodData.setFoodLevel(int)` | Public setter exists |
| Particle spawning | Custom packet | `ServerLevel.sendParticles()` | Handles client sync automatically |
| Progress state | Map-based tracker | Attachment on downed player | Follows codebase pattern |

**Key insight:** The existing codebase patterns (attachments, tick handlers, state accessors) handle all the infrastructure. Revival mechanics are just a new application of proven patterns.

## Common Pitfalls

### Pitfall 1: Thread Safety Panic for Multi-Reviver
**What goes wrong:** Over-engineering with AtomicDouble or locks for progress accumulation
**Why it happens:** Assumption that concurrent modification is possible
**How to avoid:** Understand that `ServerTickEvents.END_SERVER_TICK` runs on the main server thread; all progress updates are sequential within the same tick
**Warning signs:** Code using synchronized blocks or atomic operations for simple addition

### Pitfall 2: Progress on Reviver Instead of Downed
**What goes wrong:** Complex state management when reviver switches targets or multiple revivers exist
**Why it happens:** Intuition that "the reviver is doing the action"
**How to avoid:** Store progress on the downed player; this is "how much have I been revived" not "how much have I revived"
**Warning signs:** Need for reviver->downed mapping, progress loss on reviver death

### Pitfall 3: Forgetting FoodData.setFoodLevel is Public
**What goes wrong:** Creating accessor mixin for food level modification
**Why it happens:** Saturation requires accessor (private field), assuming hunger does too
**How to avoid:** Check vanilla API first; `FoodData.setFoodLevel(int)` is a public method
**Warning signs:** Creating FoodDataAccessor.setFoodLevel when it's not needed

### Pitfall 4: Using distanceTo Instead of distanceToSqr
**What goes wrong:** Unnecessary sqrt operation every tick for every player pair
**Why it happens:** More intuitive to think in blocks than squared distance
**How to avoid:** Compare against 4.0 (2 blocks squared) instead of 2.0
**Warning signs:** `distanceTo()` in tight loops

### Pitfall 5: Particle Type Confusion
**What goes wrong:** Using COMPOSTER particles (designed for composting) instead of HAPPY_VILLAGER
**Why it happens:** Both are "green particles"
**How to avoid:** HAPPY_VILLAGER is the canonical "positive event" green particle (trading, breeding, bone meal)
**Warning signs:** Particles look wrong visually

## Code Examples

Verified patterns from existing codebase:

### Attachment Registration
```java
// Source: THCAttachments.java
public static final AttachmentType<Double> REVIVAL_PROGRESS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "revival_progress"),
    builder -> builder.initializer(() -> 0.0D)
    // Non-persistent: revival state is session-scoped
);
```

### State Accessor Pattern
```java
// Source: BucklerState.java pattern
public final class RevivalState {
    private RevivalState() {}

    private static AttachmentTarget target(LivingEntity entity) {
        return (AttachmentTarget) entity;
    }

    public static double getProgress(ServerPlayer player) {
        Double value = target(player).getAttachedOrCreate(THCAttachments.REVIVAL_PROGRESS);
        return value == null ? 0.0D : value;
    }

    public static void setProgress(ServerPlayer player, double value) {
        target(player).setAttached(THCAttachments.REVIVAL_PROGRESS, value);
    }

    public static void addProgress(ServerPlayer player, double amount) {
        setProgress(player, Math.min(1.0, getProgress(player) + amount));
    }
}
```

### Class Bonus Check
```java
// Source: ClassManager.java
PlayerClass playerClass = ClassManager.getClass(reviver);
double progressRate = (playerClass == PlayerClass.SUPPORT) ? 0.01 : 0.005;
// 0.01 per tick = 100 ticks = 5 seconds
// 0.005 per tick = 200 ticks = 10 seconds
```

### Sneaking + Proximity Check
```java
// Source: Existing patterns in codebase
if (!reviver.isShiftKeyDown()) continue;

Vec3 reviverPos = reviver.position();
Vec3 downedLoc = RevivalState.getDownedLocation(downed);
double distSq = reviverPos.distanceToSqr(downedLoc);
if (distSq <= 4.0) {  // 2 blocks = 4.0 squared
    // In range, accumulate progress
}
```

### Particle Spawning
```java
// Source: Verified Minecraft API pattern
ServerLevel level = player.serverLevel();
level.sendParticles(
    ParticleTypes.HAPPY_VILLAGER,  // Green sparkle particles
    location.x,
    location.y + 1.0,  // Slightly above ground
    location.z,
    30,                // particle count
    0.5, 0.5, 0.5,     // x/y/z spread
    0.0                // speed
);
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Thread-per-world | Single-threaded tick | Always vanilla | No thread safety needed for attachments |
| Custom packets for particles | ServerLevel.sendParticles | MC 1.8+ | Automatic client sync |
| Manual sync for entity state | Fabric Attachment API | Fabric 0.70+ | Standardized persistence |

**Current for Minecraft 1.21.11:**
- `player.isShiftKeyDown()` for sneaking (not `isCrouching()`)
- `player.position()` returns Vec3 (not `getPosition()`)
- `player.blockPosition()` for BlockPos
- `player.setGameMode(GameType.SURVIVAL)` for mode change
- `ServerLevel.sendParticles()` for server-side particle spawning

## Open Questions

All questions resolved:

1. **Particle type:** HAPPY_VILLAGER is the standard green "positive event" particle
2. **Thread safety:** Server tick is single-threaded; no atomic operations needed
3. **Progress data structure:** Store on downed player attachment
4. **Food level API:** `FoodData.setFoodLevel(int)` is public, no accessor needed

## Sources

### Primary (HIGH confidence)
- Existing codebase patterns (BucklerState, ClassManager, THCAttachments, THC.kt)
- Phase 72 CONTEXT.md (downed state design)
- Phase 73 CONTEXT.md (revival mechanics design)
- Minecraft API (verified via existing codebase grep results)

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Particles](https://minecraft.wiki/w/Particles_(Java_Edition)) - Particle types and HAPPY_VILLAGER description
- [Fabric Documentation - Events](https://docs.fabricmc.net/develop/events) - ServerTickEvents usage
- [Forge Documentation - Particles](https://docs.minecraftforge.net/en/1.18.x/gameeffects/particles/) - ServerLevel.sendParticles pattern

### Tertiary (LOW confidence)
- None - all critical claims verified with codebase or official sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All patterns exist in codebase
- Architecture: HIGH - Direct application of existing BucklerState/ClassManager patterns
- Pitfalls: HIGH - Based on codebase analysis and Minecraft threading model

**Research date:** 2026-02-02
**Valid until:** Indefinite (patterns are stable within Minecraft 1.21.x)
