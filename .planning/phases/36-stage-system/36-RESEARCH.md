# Phase 36: Stage System - Research

**Researched:** 2026-01-23
**Domain:** Server-wide persistent state, per-player tracking, command permissions, broadcast messaging
**Confidence:** HIGH

## Summary

The stage system requires implementing server-wide stage progression (1→5) with per-player boon level tracking that persists across restarts. Research identifies four core technical domains: (1) SavedData with Codec for server-wide stage storage following the established ClaimData pattern, (2) persistent integer attachment for per-player boon level with copyOnDeath() to survive respawns, (3) operator-only command using `.requires(source -> source.hasPermissionLevel(2))` for `/advanceStage`, and (4) actionbar broadcast by iterating `server.playerList.players` and calling `displayClientMessage()` with `true` for actionbar mode.

The standard approach mirrors the existing class system architecture: ServerLifecycleEvents.JOIN for late-joiner boon initialization, AttachmentType with Codec.INT for player state, SavedData stored in overworld's DataStorage for server state, and CommandRegistrationCallback for command registration. Stage advancement increments all online players' boon levels and broadcasts a red actionbar message.

**Primary recommendation:** Create StageData (SavedData with single integer field for current stage), BOON_LEVEL attachment (persistent integer), AdvanceStageCommand (op-level 2 required), and StageManager static utility following ThreatManager/ClassManager patterns.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric Attachment API | 0.141.0+1.21.11 | Per-player boon level persistence | Official Fabric API, used for PLAYER_CLASS |
| SavedData + Codec | 1.21.11 | Server-wide stage persistence | Vanilla system, used for ClaimData |
| Fabric Command API | 0.141.0+1.21.11 | Command registration with permissions | Built into Fabric API, used for SelectClassCommand |
| ServerPlayConnectionEvents | 0.141.0+1.21.11 | Late-joiner boon initialization | Fabric Networking API, event-driven pattern |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Codec API | (bundled) | Serialization for SavedData and attachments | Required for persistence with type safety |
| Brigadier | (bundled) | Command parsing and validation | Vanilla command system, bundled with Minecraft |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| SavedData | Level-based gamerule | Gamerules don't support integer values well, harder to query |
| Attachment API | ServerPlayerMixin with NBT | Manual NBT handling, loses copyOnDeath() convenience |
| displayClientMessage | Custom packet | Reinventing the wheel, displayClientMessage handles actionbar natively |

**Installation:**
No additional dependencies needed - all components are part of Fabric API 0.141.0+1.21.11 already in the project.

## Architecture Patterns

### Recommended Project Structure
```
src/main/
├── java/thc/
│   ├── THCAttachments.java          # Add BOON_LEVEL attachment here
│   └── stage/                        # New package for stage system
│       ├── StageManager.java        # Static utility for stage/boon CRUD
│       └── AdvanceStageCommand.java # Op-only command
└── kotlin/thc/
    └── stage/
        └── StageData.kt             # SavedData for server-wide stage
```

### Pattern 1: SavedData for Server-Wide State
**What:** Single integer field stored in overworld's DataStorage, mirrors ClaimData pattern
**When to use:** Server-wide state that must persist across restarts and be accessible from any context
**Example:**
```kotlin
// Following ClaimData.kt pattern exactly
class StageData(currentStage: Int = 1) : SavedData() {
    var currentStage: Int = currentStage
        private set

    fun advanceStage(): Boolean {
        if (currentStage >= 5) return false
        currentStage++
        setDirty()
        return true
    }

    companion object {
        private const val DATA_NAME = "thc_stage"

        private val CODEC: Codec<StageData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("current_stage").forGetter { it.currentStage }
            ).apply(instance, ::StageData)
        }

        val TYPE: SavedDataType<StageData> = SavedDataType(
            DATA_NAME,
            ::StageData,
            CODEC,
            DataFixTypes.LEVEL
        )

        @JvmStatic
        fun getServerState(server: MinecraftServer): StageData {
            val overworld = server.getLevel(Level.OVERWORLD)
                ?: throw IllegalStateException("Overworld not loaded")
            return overworld.dataStorage.computeIfAbsent(TYPE)
        }
    }
}
```

### Pattern 2: Persistent Integer Attachment
**What:** Store boon level as persistent integer attachment with copyOnDeath()
**When to use:** Per-player numeric state that must survive logout and death
**Example:**
```java
// In THCAttachments.java, following PLAYER_CLASS pattern
public static final AttachmentType<Integer> BOON_LEVEL = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "boon_level"),
    builder -> {
        builder.initializer(() -> 0);  // Default boon level 0
        builder.persistent(Codec.INT);
        builder.copyOnDeath();
    }
);
```

### Pattern 3: Static Manager Utility
**What:** Static utility class for stage/boon CRUD operations, follows ThreatManager/ClassManager pattern
**When to use:** Centralized API for state manipulation across the mod
**Example:**
```java
// StageManager.java - static utility like ClassManager
public final class StageManager {
    private StageManager() {}

    public static int getCurrentStage(MinecraftServer server) {
        return StageData.getServerState(server).getCurrentStage();
    }

    public static boolean advanceStage(MinecraftServer server) {
        StageData state = StageData.getServerState(server);
        if (!state.advanceStage()) return false;

        // Increment all online players' boon levels
        int newStage = state.getCurrentStage();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            incrementBoonLevel(player);
        }

        // Broadcast to all players
        Component message = Component.literal("Trial complete. The world has advanced to Stage " + newStage + ".")
            .withStyle(ChatFormatting.RED);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, true); // true = actionbar
        }

        return true;
    }

    public static int getBoonLevel(ServerPlayer player) {
        Integer level = player.getAttached(THCAttachments.BOON_LEVEL);
        return level != null ? level : 0;
    }

    public static void incrementBoonLevel(ServerPlayer player) {
        int current = getBoonLevel(player);
        player.setAttached(THCAttachments.BOON_LEVEL, current + 1);
    }

    public static void setBoonLevel(ServerPlayer player, int level) {
        player.setAttached(THCAttachments.BOON_LEVEL, level);
    }
}
```

### Pattern 4: Operator-Only Command
**What:** Brigadier command with permission level 2 requirement (operator only)
**When to use:** Administrative commands that modify server-wide state
**Example:**
```java
// AdvanceStageCommand.java - follows SelectClassCommand pattern
public final class AdvanceStageCommand {
    private AdvanceStageCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("advanceStage")
                .requires(source -> source.hasPermissionLevel(2)) // Op level 2+
                .executes(AdvanceStageCommand::execute));
        });
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();

        int currentStage = StageManager.getCurrentStage(server);
        if (currentStage >= 5) {
            context.getSource().sendFailure(
                Component.literal("Already at maximum stage (5)!").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        if (StageManager.advanceStage(server)) {
            context.getSource().sendSuccess(
                () -> Component.literal("Advanced to Stage " + StageManager.getCurrentStage(server))
                    .withStyle(ChatFormatting.GREEN),
                true // broadcast to ops
            );
            return 1;
        }

        return 0;
    }
}
```

### Pattern 5: Late-Joiner Boon Initialization
**What:** On player join, set boon level to match current stage if not already set
**When to use:** Ensuring new players joining at stage 3+ get appropriate boon level
**Example:**
```java
// In StageManager.java or separate event registration
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayer player = handler.getPlayer();

    // If player has no class yet (new player), set boon to current stage
    // If player already has class, their boon level persists from attachment
    if (!ClassManager.hasClass(player)) {
        int currentStage = StageManager.getCurrentStage(server);
        StageManager.setBoonLevel(player, currentStage);
    }
});
```

### Pattern 6: Actionbar Broadcast
**What:** Iterate all online players and send actionbar message with red formatting
**When to use:** Server-wide announcements that should be visible but not intrusive
**Example:**
```java
// In StageManager.advanceStage() - already shown in Pattern 3
Component message = Component.literal("Trial complete. The world has advanced to Stage " + newStage + ".")
    .withStyle(ChatFormatting.RED);
for (ServerPlayer player : server.getPlayerList().getPlayers()) {
    player.displayClientMessage(message, true); // true = actionbar
}
```

### Anti-Patterns to Avoid
- **Using gamerule for stage**: Gamerules are boolean/integer but not designed for complex state persistence
- **Storing stage in player data**: Stage is server-wide, not per-player - use SavedData not attachments
- **Forgetting copyOnDeath() on BOON_LEVEL**: Boon level must survive death like class does
- **Broadcasting to chat instead of actionbar**: Actionbar is non-intrusive, chat is permanent
- **Not checking stage bounds**: Always validate stage ≤ 5 before advancing

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Server-wide persistent state | Custom file storage or config | SavedData with Codec | Vanilla system, automatic serialization, tied to world save |
| Player integer storage | NBT manipulation in mixin | Attachment with Codec.INT | Type-safe, persistent, copyOnDeath() built-in |
| Operator permission check | Manual player.hasPermission() | `.requires(source -> source.hasPermissionLevel(2))` | Brigadier native, handles command blocks correctly |
| Broadcasting to all players | Custom packet system | Iterate playerList + displayClientMessage | Vanilla system, handles actionbar vs chat natively |
| Late-joiner handling | Tick-based scanning | ServerPlayConnectionEvents.JOIN | Event-driven, no performance overhead |

**Key insight:** The combination of SavedData (server state) and Attachment (player state) provides a clean separation of concerns. Stage is server-wide and advances for everyone; boon level is per-player and accumulates individually.

## Common Pitfalls

### Pitfall 1: Stage Stored in Attachment Instead of SavedData
**What goes wrong:** Each player has their own stage value, causing desync
**Why it happens:** Attachments are per-entity, but stage is server-wide state
**How to avoid:** Use SavedData for server-wide stage, Attachment only for per-player boon level
**Warning signs:** Players see different stage values, "/advanceStage" only affects command executor

### Pitfall 2: Forgetting to Mark SavedData Dirty
**What goes wrong:** Stage advances but doesn't persist across server restart
**Why it happens:** SavedData only saves when `setDirty()` is called
**How to avoid:** Always call `setDirty()` after modifying SavedData fields
**Warning signs:** Stage resets to 1 after server restart despite successful advancement

### Pitfall 3: Late Joiners Don't Get Boon Level
**What goes wrong:** Player joins at stage 3 server but has boon level 0
**Why it happens:** Attachment initializer always returns 0, no JOIN event handler to sync with stage
**How to avoid:** Implement ServerPlayConnectionEvents.JOIN to set boon level = current stage for new players
**Warning signs:** Existing players have boon level 3, new joiners have boon level 0

### Pitfall 4: Boon Level Doesn't Survive Death
**What goes wrong:** Player dies and respawns with boon level reset to 0
**Why it happens:** Forgot to add `.copyOnDeath()` in attachment builder
**How to avoid:** Always add `.copyOnDeath()` for attachments that should persist through death
**Warning signs:** Boon level survives logout but resets on death

### Pitfall 5: Command Fails Silently for Non-Ops
**What goes wrong:** Non-op players type `/advanceStage`, nothing happens, no error message
**Why it happens:** `.requires()` predicate fails silently when false
**How to avoid:** This is expected behavior - Brigadier hides commands players can't use. No action needed.
**Warning signs:** None - this is correct behavior for permission-restricted commands

### Pitfall 6: Actionbar Message Shows in Chat
**What goes wrong:** Broadcast message appears in chat log instead of above hotbar
**Why it happens:** `displayClientMessage(message, false)` instead of `true`
**How to avoid:** Always pass `true` as second parameter for actionbar messages
**Warning signs:** Message persists in chat history, can't see actionbar effect

## Code Examples

Verified patterns from official sources and existing codebase:

### SavedData Accessor Pattern
```kotlin
// Source: ClaimData.kt in existing codebase
// Pattern: computeIfAbsent ensures lazy initialization, no manual registration needed
companion object {
    @JvmStatic
    fun getServerState(server: MinecraftServer): StageData {
        val overworld = server.getLevel(Level.OVERWORLD)
            ?: throw IllegalStateException("Overworld not loaded")
        return overworld.dataStorage.computeIfAbsent(TYPE)
    }
}
```

### Command Registration with Permission Level
```java
// Source: Fabric Wiki - Creating Commands
// https://wiki.fabricmc.net/tutorial:commands
dispatcher.register(Commands.literal("advanceStage")
    .requires(source -> source.hasPermissionLevel(2)) // Op level 2+
    .executes(context -> {
        // Command logic
        return 1;
    }));
```

### Actionbar Broadcast Pattern
```java
// Source: BasePermissions.kt pattern, adapted for broadcast
// Iterate all players and send actionbar message
Component message = Component.literal("Trial complete. The world has advanced to Stage " + stage + ".")
    .withStyle(ChatFormatting.RED);
for (ServerPlayer player : server.getPlayerList().getPlayers()) {
    player.displayClientMessage(message, true); // true = actionbar
}
```

### Late-Joiner Boon Initialization
```java
// Source: Fabric Networking API - ServerPlayConnectionEvents
// Set boon level to match current stage for new players
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayer player = handler.getPlayer();

    // New players (no class) get boon level matching current stage
    if (!ClassManager.hasClass(player)) {
        int currentStage = StageManager.getCurrentStage(server);
        StageManager.setBoonLevel(player, currentStage);
    }
});
```

### Integer Attachment with Null-Safe Getter
```java
// Source: THCAttachments.java pattern, adapted for integers
// Integer attachments can be null, always provide default
public static int getBoonLevel(ServerPlayer player) {
    Integer level = player.getAttached(THCAttachments.BOON_LEVEL);
    return level != null ? level : 0;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| WorldSavedData | SavedData with SavedDataType | Minecraft 1.20+ | Use SavedDataType factory with Codec, not direct WorldSavedData |
| Manual NBT read/write | Codec-based serialization | Minecraft 1.20+ | RecordCodecBuilder handles all serialization automatically |
| getOnlinePlayers() returns array | getPlayerList().getPlayers() returns list | Minecraft 1.7+ | Use server.getPlayerList().getPlayers() for modern code |
| PermissionAPI for op checks | hasPermissionLevel() | Always vanilla | hasPermissionLevel() is built-in, no external API needed |

**Deprecated/outdated:**
- `WorldSavedData`: Use `SavedData` with `SavedDataType` in 1.21+
- Manual `read(CompoundTag)` and `save(CompoundTag)`: Use Codec serialization instead
- `getOnlinePlayers()` array iteration: Use `getPlayerList().getPlayers()` list
- Custom permission systems for basic op checks: Use `.hasPermissionLevel(2)` built-in

## Open Questions

Things that couldn't be fully resolved:

1. **Should late-joiner boon initialization happen for players who already have a class?**
   - What we know: Context says "late joiners get boon level matching current stage", attachment persists with copyOnDeath()
   - What's unclear: If returning player (has class, has boon level 2) joins stage 3 server, should boon become 3?
   - Recommendation: No - "late joiner" means new players only. Returning players keep their accumulated boon level. Check `!ClassManager.hasClass(player)` before setting boon to current stage.

2. **Should /advanceStage provide operator feedback beyond the broadcast?**
   - What we know: Context says broadcast to all players, operator feedback is "Claude's discretion"
   - What's unclear: Should operator who runs command get additional confirmation beyond the red actionbar?
   - Recommendation: Yes - use `context.getSource().sendSuccess()` to give operator green chat message "Advanced to Stage X" in addition to the red actionbar broadcast to all players.

3. **How to handle stage advancement when no players are online?**
   - What we know: Stage advances increment all connected players' boon levels
   - What's unclear: Can operator run `/advanceStage` from console when server is empty?
   - Recommendation: Yes - command should work from console. Empty player list is valid (no boon increments happen, but stage still advances). Late joiners will catch up via JOIN event.

4. **Should boon level be visible to players?**
   - What we know: Boon level is tracked but has no gameplay effect yet (scaffolding)
   - What's unclear: Should there be a command or UI to check boon level?
   - Recommendation: Not in this phase - boon level is invisible infrastructure for future phases. Phase 36 is "scaffolding only" per context.

## Sources

### Primary (HIGH confidence)
- [Fabric Wiki - Creating Commands](https://wiki.fabricmc.net/tutorial:commands) - Command registration, permission levels
- [Minecraft Wiki - Permission level](https://minecraft.wiki/w/Permission_level) - Operator permission levels (0-4)
- Existing codebase - ClaimData.kt (SavedData pattern), THCAttachments.java (Attachment pattern), ClassManager.java (static utility pattern), SelectClassCommand.java (command pattern), BasePermissions.kt (displayClientMessage actionbar pattern)

### Secondary (MEDIUM confidence)
- [Fabric API - ServerPlayConnectionEvents](https://maven.fabricmc.net/docs/fabric-api-0.58.5+1.19.1/net/fabricmc/fabric/api/message/v1/ServerMessageEvents.html) - Player join events
- [Minecraft Wiki - Action bar](https://minecraft.wiki/w/Action_bar) - Actionbar message mechanics
- Phase 35 Research (35-RESEARCH.md) - Class system patterns directly applicable to stage system

### Tertiary (LOW confidence)
- Various Spigot/Paper documentation on broadcasting - Not directly applicable (different API) but confirms displayClientMessage is standard approach

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All components exist in codebase (SavedData, Attachment, Command API)
- Architecture: HIGH - Direct analogs exist (ClaimData for server state, PLAYER_CLASS for player state, SelectClassCommand for op commands)
- Pitfalls: MEDIUM - Mix of existing codebase validation (HIGH) and inferred from Minecraft mechanics (MEDIUM)

**Research date:** 2026-01-23
**Valid until:** ~30 days (stable domain, Minecraft 1.21.11 unlikely to change)
