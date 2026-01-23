---
phase: 35-class-system
plan: 01
subsystem: player-progression
tags: [class-system, attachments, commands, player-data]
dependency-graph:
  requires: [23-base-chunks, 05-health-system]
  provides: [player-class-attachment, class-selection-command, class-enum]
  affects: [35-02-damage-multipliers]
tech-stack:
  added: []
  patterns: [attachment-persistence, command-registration, static-utility-class]
decisions:
  - id: DEC-35-01-01
    title: "Class selection is permanent"
    choice: "Classes can only be set once per player, enforced in ClassManager.setClass"
    rationale: "Prevents class swapping exploits and encourages meaningful choice"
  - id: DEC-35-01-02
    title: "Base chunk restriction for class selection"
    choice: "Players can only select class while in a claimed base chunk"
    rationale: "Forces players to establish base before class commitment, creates safe selection environment"
  - id: DEC-35-01-03
    title: "Immediate health modification on class selection"
    choice: "Health bonus applied immediately via ServerPlayerHealthAccess interface"
    rationale: "Consistent with existing health modification pattern, provides instant feedback"
key-files:
  created:
    - src/main/java/thc/playerclass/PlayerClass.java
    - src/main/java/thc/playerclass/ClassManager.java
    - src/main/java/thc/playerclass/SelectClassCommand.java
  modified:
    - src/main/java/thc/THCAttachments.java
    - src/main/kotlin/thc/THC.kt
    - src/main/java/thc/mixin/PlayerAttackMixin.java
metrics:
  duration: 10min
  completed: 2026-01-23
---

# Phase 35 Plan 01: Class System Foundation Summary

**One-liner:** Persistent player class selection with TANK/MELEE/RANGED/SUPPORT roles, health bonuses, and base-restricted /selectClass command

## What Was Built

### Core Data Structures

**PlayerClass Enum** (`PlayerClass.java`)
- Four classes with distinct stat multipliers:
  - **TANK**: +1 heart (2.0 HP), 2.5x melee, 1x ranged
  - **MELEE**: +0.5 hearts (1.0 HP), 4x melee, 1x ranged
  - **RANGED**: no health change, 1x melee, 5x ranged
  - **SUPPORT**: no health change, 1x melee, 3x ranged
- Case-insensitive `fromString()` parser for command input
- Damage multipliers ready for future integration (plan 35-02)

**PLAYER_CLASS Attachment** (`THCAttachments.java`)
- Type: `AttachmentType<String>`
- Stores class name as string
- `persistent(Codec.STRING)` - survives logout/restart
- `copyOnDeath()` - permanent across deaths
- Initializer: `null` (no class by default)

**ClassManager Static Utility** (`ClassManager.java`)
- `getClass(ServerPlayer)` - retrieve player's class or null
- `hasClass(ServerPlayer)` - check if player has selected class
- `setClass(ServerPlayer, PlayerClass)` - one-time class assignment
- `applyHealthModifier()` - uses existing `ServerPlayerHealthAccess` interface
- Follows established pattern from `ThreatManager`

### Command Interface

**SelectClassCommand** (`SelectClassCommand.java`)
- Command: `/selectClass <class>`
- Tab completion: tank, melee, ranged, support
- Validation chain:
  1. Invalid class name → actionbar error
  2. Already has class → actionbar error
  3. Not in base → actionbar error
  4. Success → title screen + chat message
- Base chunk check: `ClaimManager.INSTANCE.isInBase(server, player.blockPosition())`
- Success feedback:
  - Title: Class name in GOLD
  - Subtitle: "Class Selected" in YELLOW
  - Chat: "You are now a [class]!" in GREEN

### Integration

- Command registered in `THC.kt` onInitialize
- Integrates with existing base claim system
- Uses existing health modification infrastructure

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed PlayerAttackMixin for MC 1.21.11 compatibility**

- **Found during:** Task 2 smoke test
- **Issue:** Mixin injection failure blocking server startup - `getSweepingDamageRatio()` method no longer exists in MC 1.21.11
- **Root cause:** Minecraft version upgrade changed sweeping damage from EnchantmentHelper method to Attributes system
- **Fix:**
  - Replaced `@Redirect` on `EnchantmentHelper.getSweepingDamageRatio()` (removed method)
  - Added `@Redirect` on `Player.getAttributeValue()` in `doSweepAttack()` method
  - Returns `0.0` when attribute is `Attributes.SWEEPING_DAMAGE_RATIO`
  - Preserves original behavior: sweeping edge damage remains disabled (as per critical balance value in CLAUDE.md)
- **Files modified:** `src/main/java/thc/mixin/PlayerAttackMixin.java`
- **Commit:** 5dc0afa
- **Impact:** Restored server functionality, maintained sweeping edge disable requirement

**Why auto-fixed:** This is exactly the scenario described in CLAUDE.md: "Run gradle runClient immediately after updating MC version to catch mixin breakages while the upgrade is the focus. Mixin injection targets change between versions and errors are much cheaper to fix with fresh context than weeks later."

The fix was necessary to proceed with task verification and maintains the critical balance value (sweeping edge disabled) specified in CLAUDE.md.

## Known Limitations

**Unresolved Pre-existing Mixin Breakages**

During execution, additional mixin errors were discovered but NOT fixed:

1. **PlayerSleepMixin** - `thc$allowSleepAnytime` redirect failure
   - Injection target likely changed in MC 1.21.11
   - Does not block compilation, only runtime
   - Outside scope of current plan

2. **Other potential mixins** - Full smoke test could not complete due to cascading mixin errors

**Smoke test status:** Build successful (`./gradlew build`), runtime verification incomplete due to pre-existing mixin breakages.

**Recommendation:** Create separate bug-fix task for comprehensive mixin update audit across all mixins after MC version upgrade.

## Key Implementation Patterns

### Attachment Pattern
```java
public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "player_class"),
    builder -> {
        builder.initializer(() -> null);
        builder.persistent(Codec.STRING);
        builder.copyOnDeath();
    }
);
```

### Static Utility Pattern
```java
public final class ClassManager {
    public static PlayerClass getClass(ServerPlayer player) {
        String className = player.getAttached(THCAttachments.PLAYER_CLASS);
        if (className == null) return null;
        return PlayerClass.fromString(className);
    }

    public static boolean setClass(ServerPlayer player, PlayerClass playerClass) {
        if (hasClass(player)) return false; // One-time only
        player.setAttached(THCAttachments.PLAYER_CLASS, playerClass.name());
        applyHealthModifier(player, playerClass);
        return true;
    }
}
```

### Command Registration Pattern
```java
CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    dispatcher.register(Commands.literal("selectClass")
        .then(Commands.argument("class", StringArgumentType.string())
            .suggests((context, builder) -> {
                // Tab completion suggestions
            })
            .executes(SelectClassCommand::execute)));
});
```

### Base Chunk Validation Pattern
```java
MinecraftServer server = player.level().getServer();
if (!ClaimManager.INSTANCE.isInBase(server, player.blockPosition())) {
    // Actionbar error
    return 0;
}
```

## Next Phase Readiness

### For Plan 35-02 (Damage Multipliers)

**Provided:**
- `PlayerClass` enum with `getMeleeMultiplier()` and `getRangedMultiplier()` methods
- `ClassManager.getClass(ServerPlayer)` for damage mixin integration
- Health bonuses already applied, no further action needed

**Integration points:**
- `PlayerAttackMixin` - multiply damage by `getClass(player).getMeleeMultiplier()`
- Arrow/projectile mixin - multiply damage by `getClass(player).getRangedMultiplier()`

**Blockers:** None

**Concerns:**
- Pre-existing mixin breakages should be resolved before adding new mixins
- Consider creating mixin update audit task before 35-02

## Testing Notes

**Compilation:** ✅ Successful (`./gradlew build`)

**Code verification:**
- ✅ `PLAYER_CLASS` attachment in `THCAttachments.java`
- ✅ `selectClass` command registered in `SelectClassCommand.java`
- ✅ `isInBase` check in `SelectClassCommand.java`
- ✅ `ClassManager` provides getClass/hasClass/setClass methods
- ✅ All 4 classes defined with correct multipliers

**Runtime verification:**
- ⚠️ Incomplete - pre-existing mixin breakages prevent server startup
- Build successful, code compiles correctly
- Manual in-game testing required after mixin fixes

**What to test in-game (when runtime is fixed):**
1. `/selectClass tank` in base → success with title/chat
2. `/selectClass invalid` → actionbar error
3. `/selectClass melee` after already selecting tank → actionbar error
4. `/selectClass ranged` outside base → actionbar error
5. Logout/login → class persists
6. Death → class persists
7. TANK class → max health increases by 1 heart
8. MELEE class → max health increases by 0.5 hearts

## Metrics

- **Tasks completed:** 2/2 + 1 deviation fix
- **Commits:** 3 (2 feature, 1 bug fix)
- **Files created:** 3
- **Files modified:** 3
- **Duration:** 10 minutes
- **Lines of code:** ~200 (excluding comments)
