# Phase 35: Class System - Research

**Researched:** 2026-01-22
**Domain:** Persistent player data, command registration, attribute modification
**Confidence:** HIGH

## Summary

The class system requires implementing a permanent class selection command that modifies player health and damage based on role. Research identifies three core technical domains: (1) Fabric command registration with Brigadier for `/selectClass <class>` with custom suggestions, (2) persistent player data storage using Fabric attachments with Codec serialization, and (3) damage modification via mixin injection points since vanilla AttributeModifiers only affect melee damage, not projectile damage.

The standard approach uses CommandRegistrationCallback for command registration, AttachmentType with persistent codecs for cross-session storage, and existing mixin patterns (PlayerAttackMixin for melee, AbstractArrowMixin for ranged) already established in the codebase. Health modification uses AttributeModifier.Operation.ADD_VALUE on the MAX_HEALTH attribute with permanent modifiers.

**Primary recommendation:** Store class as persistent String attachment, apply health modifier on player join/respawn using addPermanentModifier(), and multiply damage in existing damage mixins by checking the player's class attachment.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric Command API | 0.141.0+1.21.11 | Command registration with Brigadier | Built into Fabric API, official command system |
| Fabric Attachment API | 0.141.0+1.21.11 | Persistent entity data storage | Official Fabric API for entity attachments with NBT persistence |
| Minecraft Attributes | 1.21.11 | Health/damage modification via AttributeModifiers | Vanilla system for entity stat modification |
| Mojang Brigadier | (bundled) | Command parsing and suggestions | Vanilla command system used by Minecraft |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Mixin | (bundled with Fabric) | Bytecode injection for damage modification | When vanilla attributes don't cover use case (projectile damage) |
| Codec API | (bundled) | Serialization for persistent data | Attachment persistence with type safety |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Attachment API | Cardinal Components | More features but external dependency, Attachments are official |
| Brigadier | Custom command parsing | Reinventing the wheel, loses tab completion integration |
| AttributeModifiers | Direct health manipulation | Loses persistence, conflicts with vanilla systems |

**Installation:**
No additional dependencies needed - all components are part of Fabric API 0.141.0+1.21.11 already in the project.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── THCAttachments.java          # Attachment registry (add class attachment here)
├── class/                        # New package for class system
│   ├── PlayerClass.java         # Enum for class types
│   ├── ClassManager.java        # Static utility for class CRUD operations
│   └── SelectClassCommand.java  # Command registration
└── mixin/
    ├── PlayerAttackMixin.java   # Modify for class-based melee damage
    ├── AbstractArrowMixin.java  # Modify for class-based ranged damage
    └── ServerPlayerMixin.java   # Add health modifier application on join
```

### Pattern 1: Persistent Attachment with Enum
**What:** Store player class as persistent String attachment, parse to enum on retrieval
**When to use:** Permanent player state that must survive logout/death
**Example:**
```java
// In THCAttachments.java
public static final AttachmentType<String> PLAYER_CLASS = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("thc", "player_class"),
    builder -> {
        builder.initializer(() -> null);  // null = no class selected
        builder.persistent(Codec.STRING);
        builder.copyOnDeath();
    }
);

// In ClassManager.java (static utility pattern like ThreatManager)
public static PlayerClass getClass(ServerPlayer player) {
    String className = player.getAttached(THCAttachments.PLAYER_CLASS);
    if (className == null) return null;
    return PlayerClass.valueOf(className);
}

public static boolean setClass(ServerPlayer player, PlayerClass playerClass) {
    if (getClass(player) != null) return false; // Already has class
    player.setAttached(THCAttachments.PLAYER_CLASS, playerClass.name());
    return true;
}
```

### Pattern 2: Command with Custom Suggestions
**What:** Brigadier command with StringArgumentType and custom SuggestionProvider
**When to use:** Commands with fixed set of valid values (tank, melee, ranged, support)
**Example:**
```java
// Source: https://docs.fabricmc.net/develop/commands/suggestions
CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    dispatcher.register(Commands.literal("selectClass")
        .then(Commands.argument("class", StringArgumentType.string())
            .suggests((context, builder) -> {
                builder.suggest("tank");
                builder.suggest("melee");
                builder.suggest("ranged");
                builder.suggest("support");
                return builder.buildFuture();
            })
            .executes(context -> {
                String className = StringArgumentType.getString(context, "class");
                ServerPlayer player = context.getSource().getPlayer();
                // Command logic here
                return 1;
            })));
});
```

### Pattern 3: Permanent AttributeModifier Application
**What:** Add permanent health modifier on player join/death using AttributeInstance
**When to use:** Persistent stat changes that should survive death and sessions
**Example:**
```java
// Source: https://docs.fabricmc.net/develop/entities/attributes
// In ServerPlayerMixin or join event
AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
if (maxHealth != null) {
    // Remove any existing class health modifier
    maxHealth.removeModifier(Identifier.fromNamespaceAndPath("thc", "class_health"));

    // Add new modifier based on class
    double healthBonus = getHealthBonus(playerClass); // +2.0 for tank, +1.0 for melee, 0.0 otherwise
    if (healthBonus != 0.0) {
        maxHealth.addPermanentModifier(
            new AttributeModifier(
                Identifier.fromNamespaceAndPath("thc", "class_health"),
                healthBonus,
                AttributeModifier.Operation.ADD_VALUE
            )
        );
    }
}
```

### Pattern 4: Damage Multiplier via Mixin
**What:** Multiply damage in existing mixins based on player's class
**When to use:** Damage modification that vanilla attributes don't support (projectile damage)
**Example:**
```java
// In PlayerAttackMixin.java - modify existing thc$reduceMeleeDamage
@ModifyVariable(method = "attack", at = @At(value = "STORE"), ordinal = 0)
private float thc$reduceMeleeDamage(float originalDamage) {
    float baseDamage = originalDamage * 0.1875f; // Existing 75% reduction

    Player self = (Player) (Object) this;
    if (self instanceof ServerPlayer serverPlayer) {
        PlayerClass playerClass = ClassManager.getClass(serverPlayer);
        if (playerClass != null) {
            baseDamage *= playerClass.getMeleeDamageMultiplier();
        }
    }
    return baseDamage;
}

// In AbstractArrowMixin.java - modify existing damage reduction
@Inject(method = "onHitEntity", at = @At("HEAD"))
private void thc$applyArrowHitEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
    // ... existing code ...

    // Apply class-based damage multiplier
    if (owner instanceof ServerPlayer player) {
        PlayerClass playerClass = ClassManager.getClass(player);
        if (playerClass != null) {
            baseDamage = thc$originalBaseDamage * 0.13 * playerClass.getRangedDamageMultiplier();
        } else {
            baseDamage = thc$originalBaseDamage * 0.13;
        }
    }
}
```

### Pattern 5: Feedback Messages
**What:** Different feedback methods for different message types
**When to use:** User feedback from commands
**Example:**
```java
// Source: https://minecraft.wiki/w/Action_bar and existing BasePermissions.kt
// Success: Title + chat
player.displayTitle(Title.title(
    Component.literal(className.toUpperCase()).withStyle(ChatFormatting.GOLD),
    Component.literal("Class Selected").withStyle(ChatFormatting.YELLOW),
    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
));
player.sendSystemMessage(Component.literal("You are now a " + className + "!"));

// Error: Action bar (temporary, non-intrusive)
player.displayClientMessage(
    Component.literal("You must be in a base to select a class!").withStyle(ChatFormatting.RED),
    true  // actionbar = true
);
```

### Anti-Patterns to Avoid
- **Using AttributeModifiers for projectile damage**: Vanilla `generic.attack_damage` only affects melee damage on ranged weapons, not arrows fired. Use mixins for projectile damage.
- **Forgetting copyOnDeath()**: Class selection is permanent, must survive death
- **Duplicate AttributeModifier UUIDs**: In 1.21+, use resource location Identifiers, not raw UUIDs. Same identifier = won't stack.
- **Applying health modifier without removing old**: Always remove existing modifier before applying new one to avoid stacking
- **Direct base value manipulation**: Use AttributeModifiers for persistence, not setAttribute()

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Command parsing | String splitting and manual parsing | Brigadier via CommandRegistrationCallback | Tab completion, error handling, argument validation all built-in |
| Persistent player data | Manual NBT reading/writing | Fabric Attachment API with Codec | Type-safe, automatic persistence, copyOnDeath() handling |
| Tab completion | Manual suggestion lists | SuggestionProvider with builder.suggest() | Integrates with vanilla tab complete, filters by input |
| Player feedback | Custom packet system | Component + displayClientMessage/sendSystemMessage | Vanilla formatting, localization support, actionbar integration |
| Damage tracking | Custom event system | Existing mixin injection points | Already established in codebase (PlayerAttackMixin, AbstractArrowMixin) |

**Key insight:** Minecraft 1.21's attribute and command systems are mature and well-integrated. Custom implementations lose vanilla compatibility (tab complete, F3+N debug, /attribute command integration) and create maintenance burden when vanilla updates.

## Common Pitfalls

### Pitfall 1: AttributeModifier UUID Collisions
**What goes wrong:** Multiple AttributeModifiers with same identifier don't stack, only most recent applies
**Why it happens:** In Minecraft 1.21, AttributeModifiers transitioned from UUIDs to resource locations. If you use the same `Identifier.fromNamespaceAndPath("thc", "modifier")` for different modifiers on the same attribute, they overwrite each other.
**How to avoid:**
- Use unique identifiers per modifier purpose: `"class_health"` vs `"class_damage"`
- For same-purpose modifiers, explicitly remove old before adding new: `attribute.removeModifier(identifier)` then `attribute.addPermanentModifier()`
**Warning signs:** Player health changes but then resets, damage multipliers don't apply consistently

### Pitfall 2: Projectile Damage with AttributeModifiers
**What goes wrong:** Setting `generic.attack_damage` AttributeModifier on player doesn't affect arrow damage
**Why it happens:** Vanilla `generic.attack_damage` only affects melee damage. When arrows/projectiles deal damage, they use AbstractArrow.baseDamage field, not the shooter's attack_damage attribute.
**How to avoid:** Use mixin injection in AbstractArrow.onHitEntity to multiply baseDamage field directly (already established pattern in AbstractArrowMixin.java)
**Warning signs:** Melee damage changes work but ranged damage doesn't respond to class selection

### Pitfall 3: Command Context Player Extraction
**What goes wrong:** `context.getSource().getPlayer()` throws exception when run from console/command block
**Why it happens:** CommandSource can be server console, command block, RCON, not just players. getPlayer() throws if source isn't a player.
**How to avoid:**
- Always use try-catch or null check: `context.getSource().getEntity() instanceof ServerPlayer`
- Or use `.requires()` in command builder to enforce player-only: `.requires(source -> source.getEntity() instanceof ServerPlayer)`
**Warning signs:** Command works when player runs it, crashes when console tests it

### Pitfall 4: Attachment Persistence Without Codec
**What goes wrong:** Attachment data lost on logout/rejoin
**Why it happens:** Attachments are transient by default. Must explicitly use `.persistent(Codec.TYPE)` in builder to save to NBT.
**How to avoid:** Always specify `.persistent(Codec.STRING)` or appropriate codec when creating AttachmentType for cross-session data
**Warning signs:** Class selection works until player logs out, then resets to null

### Pitfall 5: Health Modifier Timing
**What goes wrong:** Health modifier applied but player's current health doesn't adjust, causing "overheal" or instant death
**Why it happens:** Changing MAX_HEALTH attribute doesn't automatically clamp current health. If current health > new max, player appears to have more hearts than possible. If new max < current, player isn't healed to match.
**How to avoid:** After applying modifier, explicitly check and clamp: `if (player.getHealth() > maxHealth) player.setHealth(maxHealth)`
**Warning signs:** Player shows 10/8 hearts, or selecting tank class doesn't show extra hearts until damage taken

### Pitfall 6: Command Suggestions Case Sensitivity
**What goes wrong:** Player types `/selectClass Tank` (capital T), command fails with "invalid class"
**Why it happens:** String comparison is case-sensitive by default, but suggestions show lowercase
**How to avoid:** Either toLowerCase() the input before parsing, or use enum valueOf() in try-catch to handle gracefully
**Warning signs:** Tab completion works, but typing suggestion exactly still fails

## Code Examples

Verified patterns from official sources:

### Player Join Event for Attribute Application
```java
// Source: Fabric Networking API - ServerPlayConnectionEvents
// https://maven.fabricmc.net/docs/fabric-api-0.102.0+1.21/net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.Join.html

ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayer player = handler.getPlayer();
    PlayerClass playerClass = ClassManager.getClass(player);

    if (playerClass != null) {
        // Apply health modifier silently (no notification on join per context decisions)
        applyHealthModifier(player, playerClass);
    }
});
```

### Checking Base Location
```java
// Source: Existing BasePermissions.kt pattern
// https://github.com/your-repo/thc/src/main/kotlin/thc/base/BasePermissions.kt

ServerPlayer player = context.getSource().getPlayer();
MinecraftServer server = player.getServer();
if (server != null && ClaimManager.isInBase(server, player.blockPosition())) {
    // Player is in base, allow class selection
} else {
    // Show error via actionbar
    player.displayClientMessage(
        Component.literal("You must be in a base to select a class!").withStyle(ChatFormatting.RED),
        true  // actionbar
    );
    return 0; // Command failure
}
```

### Title Announcement
```java
// Source: Minecraft Wiki - Title command and Component API
// https://minecraft.wiki/w/Commands/title

Title title = Title.title(
    Component.literal(className.toUpperCase()).withStyle(ChatFormatting.GOLD),
    Component.literal("Class Selected").withStyle(ChatFormatting.YELLOW),
    Title.Times.times(
        Duration.ofMillis(500),   // fade in
        Duration.ofSeconds(3),     // stay
        Duration.ofMillis(500)     // fade out
    )
);
player.showTitle(title);
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| AttributeModifier with UUID | AttributeModifier with ResourceLocation (Identifier) | Minecraft 1.21.0 | Use `Identifier.fromNamespaceAndPath()` not `UUID.randomUUID()` |
| Component.literal() to sendSuccess() | Supplier<Component> to sendSuccess() | Minecraft 1.20+ | Use `() -> Component.literal()` in sendSuccess() first param |
| Cardinal Components API | Fabric Attachment API | Fabric API 0.83.0+ | Attachments are now official, simpler API |
| Manual ServerPlayNetworkHandler hooks | ServerPlayConnectionEvents.JOIN | Fabric API | Event-based, no mixin needed for join detection |

**Deprecated/outdated:**
- UUID-based AttributeModifier constructors: Use Identifier-based constructors in 1.21+
- `new AttributeModifier(UUID, String, double, Operation)`: Use `new AttributeModifier(Identifier, double, Operation)`
- Direct Component in sendSuccess(): Use Supplier<Component> for lazy evaluation
- AttachmentRegistry.Builder.buildAndRegister(): Use AttachmentRegistry.create() with builder lambda

## Open Questions

Things that couldn't be fully resolved:

1. **Do AttributeModifiers on respawn need re-application?**
   - What we know: copyOnDeath() preserves attachment, permanent modifiers should persist
   - What's unclear: Whether respawn clears AttributeModifiers even if attachment persists
   - Recommendation: Hook both JOIN event and restoreFrom() mixin (existing pattern in ServerPlayerMixin) to ensure modifiers always applied

2. **Should class selection be idempotent for same class?**
   - What we know: Context says "cannot change once selected", spec says command fails if already has class
   - What's unclear: If player selects "tank" twice, is that an error or a no-op?
   - Recommendation: Treat as error per spec "already has class" - once selected, command always fails

3. **How to handle death with class health modifiers?**
   - What we know: copyOnDeath() means attachment survives, player respawns at base health
   - What's unclear: Does respawn with +2 hearts mean spawn at 10/10 or 8/10 hearts?
   - Recommendation: Modifier persists so max is 10, but respawn health is vanilla behavior (full heal) so 10/10

## Sources

### Primary (HIGH confidence)
- [Fabric Command API Documentation](https://docs.fabricmc.net/develop/commands/basics) - Command registration, Brigadier patterns
- [Fabric Command Arguments Documentation](https://docs.fabricmc.net/develop/commands/arguments) - Argument types and parsing
- [Fabric Command Suggestions Documentation](https://docs.fabricmc.net/develop/commands/suggestions) - Custom SuggestionProvider
- [Fabric Entity Attributes Documentation](https://docs.fabricmc.net/develop/entities/attributes) - AttributeModifier API, permanent modifiers
- [Minecraft Wiki - Attributes](https://minecraft.wiki/w/Attribute) - Attribute system, operations, 1.21 changes
- [Minecraft Wiki - Action Bar](https://minecraft.wiki/w/Action_bar) - Feedback mechanisms

### Secondary (MEDIUM confidence)
- [ServerPlayConnectionEvents API](https://maven.fabricmc.net/docs/fabric-api-0.102.0+1.21/net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.Join.html) - Player join event
- [Minecraft Wiki - /attribute command](https://minecraft.wiki/w/Commands/attribute) - Attribute operations and identifiers
- [Minecraft Wiki - Damage](https://minecraft.wiki/w/Damage) - Damage calculations, melee vs ranged
- [PaperMC Docs - Command Suggestions](https://docs.papermc.io/paper/dev/command-api/basics/argument-suggestions/) - Brigadier suggestion patterns (same system)

### Tertiary (LOW confidence)
- Various forum discussions on AttributeModifier pitfalls - UUID collisions, persistence issues
- Community mod examples (Projectile Damage Attribute) - Confirms vanilla attributes don't affect projectile damage

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Official Fabric API, well-documented, stable in 1.21.11
- Architecture: HIGH - Existing codebase patterns (ThreatManager, attachments, mixins) directly applicable
- Pitfalls: MEDIUM - Mix of official docs (HIGH) and community reports (LOW), cross-verified where possible

**Research date:** 2026-01-22
**Valid until:** ~30 days (stable domain, Minecraft 1.21.11 unlikely to change)
