# Phase 69: Manual Leveling - Research

**Researched:** 2026-01-31
**Domain:** Minecraft villager leveling system override (MC 1.21.11 Fabric)
**Confidence:** HIGH

## Summary

Phase 69 implements manual villager leveling where players right-click villagers with emerald to level them up, replacing vanilla automatic leveling. The implementation requires three key components: (1) blocking automatic leveling, (2) handling manual emerald-based level-up via UseEntityCallback, and (3) modifying XP mechanics to support the 2/3/4/5 trades-per-level requirement.

The CONTEXT.md decisions establish that right-click with emerald (not shift+click) triggers level-up, with specific feedback messages for failure conditions, stage gates matching level requirements (Novice->Apprentice needs Stage 2, etc.), and custom XP thresholds where 2/3/4/5 trades fill the XP bar at each level. The 0 XP case (no message) is reserved for Phase 70's trade cycling feature.

**Primary recommendation:** Create a `VillagerLevelingMixin.java` that blocks `shouldIncreaseLevel()` to prevent auto-leveling, and extend the existing cow milking pattern in `VillagerInteraction.kt` to handle emerald right-click for manual level-up with stage validation and vanilla particle/sound feedback.

## Standard Stack

The established libraries/tools for this domain:

### Core

| Class | Package | Purpose | Why Standard |
|-------|---------|---------|--------------|
| `Villager` | `net.minecraft.world.entity.npc.villager` | Villager entity with XP and level state | Has `tradingXp` field and level methods |
| `VillagerData` | `net.minecraft.world.entity.npc.villager` | Immutable record with profession/level/type | `withLevel(int)` creates copy with new level |
| `UseEntityCallback` | `net.fabricmc.fabric.api.event.player` | Entity interaction events | Already used for cow milking, same pattern |
| `StageManager` | `thc.stage` | Server-wide stage state | `getCurrentStage(server)` for stage gates |

### Supporting

| Class | Package | Purpose | When to Use |
|-------|---------|---------|-------------|
| `ParticleTypes` | `net.minecraft.core.particles` | Particle type constants | `HAPPY_VILLAGER` for level-up effect |
| `SoundEvents` | `net.minecraft.sounds` | Sound event constants | `VILLAGER_YES` or `EXPERIENCE_ORB_PICKUP` for level-up sound |
| `Component` | `net.minecraft.network.chat` | Text messages | Action bar feedback messages |
| `InteractionResult` | `net.minecraft.world` | Interaction outcomes | SUCCESS blocks trade GUI, PASS opens it |

### XP/Level Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| Vanilla Level 2 XP | 10 | Apprentice threshold |
| Vanilla Level 3 XP | 70 | Journeyman threshold |
| Vanilla Level 4 XP | 150 | Expert threshold |
| Vanilla Level 5 XP | 250 | Master threshold |
| THC XP per trade | 5 | Uniform XP gain |
| THC Level 2 threshold | 10 | 2 trades (2*5=10) |
| THC Level 3 threshold | 15 | 3 trades (3*5=15) |
| THC Level 4 threshold | 20 | 4 trades (4*5=20) |
| THC Level 5 threshold | 25 | 5 trades (5*5=25) |

**Installation:**
No new dependencies - uses existing THC infrastructure and MC core APIs.

## Architecture Patterns

### Recommended Project Structure

```
src/main/
  java/thc/
    mixin/
      VillagerLevelingMixin.java     # Block auto-leveling, cap XP
    villager/
      VillagerXpConfig.java          # XP thresholds per level

  kotlin/thc/
    villager/
      VillagerInteraction.kt         # UseEntityCallback handler for emerald interactions
```

### Pattern 1: Block Automatic Leveling

**What:** Prevent villagers from auto-leveling when they reach XP thresholds
**When to use:** Always - core requirement VLEV-01

```java
// Source: THC v2.8 STACK.md, MC 1.21.11 Villager class
@Mixin(Villager.class)
public abstract class VillagerLevelingMixin {

    // Block shouldIncreaseLevel() to prevent automatic leveling
    @Inject(method = "shouldIncreaseLevel", at = @At("HEAD"), cancellable = true)
    private void thc$blockAutoLeveling(CallbackInfoReturnable<Boolean> cir) {
        // Always return false - leveling is manual only
        cir.setReturnValue(false);
    }
}
```

**Why this works:**
- `shouldIncreaseLevel()` is checked by villager AI tick
- Returning false prevents the `increaseMerchantCareer()` call
- Villagers accumulate XP but never auto-level

### Pattern 2: XP Capping at Level Max

**What:** Prevent XP from exceeding the threshold for current level
**When to use:** VLEV-05 requires XP cap at current level max

```java
// Source: CONTEXT.md "XP caps at max for current level (no overflow)"
@Mixin(Villager.class)
public abstract class VillagerLevelingMixin {

    @Shadow public abstract VillagerData getVillagerData();
    @Shadow public int tradingXp;

    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void thc$capXpAtLevelMax(MerchantOffer offer, CallbackInfo ci) {
        int currentLevel = this.getVillagerData().level();
        int maxXp = VillagerXpConfig.getMaxXpForLevel(currentLevel);

        if (this.tradingXp > maxXp) {
            this.tradingXp = maxXp;
        }
    }
}
```

### Pattern 3: Custom XP Thresholds

**What:** Define XP thresholds for 2/3/4/5 trades per level
**When to use:** VLEV-05 specifies custom trade counts

```java
// Source: CONTEXT.md XP Thresholds section
public final class VillagerXpConfig {

    // XP per trade (uniform across all trades)
    public static final int XP_PER_TRADE = 5;

    // Thresholds for "max XP" at each level (trades * XP_PER_TRADE)
    private static final int[] MAX_XP_PER_LEVEL = {
        0,   // Level 0 (invalid)
        10,  // Level 1 (Novice): 2 trades * 5 = 10
        15,  // Level 2 (Apprentice): 3 trades * 5 = 15
        20,  // Level 3 (Journeyman): 4 trades * 5 = 20
        25,  // Level 4 (Expert): 5 trades * 5 = 25
        0    // Level 5 (Master): no more leveling
    };

    public static int getMaxXpForLevel(int level) {
        if (level < 1 || level > 5) return 0;
        return MAX_XP_PER_LEVEL[level];
    }

    public static boolean isAtMaxXp(int level, int currentXp) {
        return currentXp >= getMaxXpForLevel(level);
    }
}
```

### Pattern 4: Manual Level-Up via UseEntityCallback

**What:** Handle emerald right-click for manual level up
**When to use:** Core interaction pattern for VLEV-03, VLEV-04

```kotlin
// Source: THC.kt cow milking pattern, CONTEXT.md Level-up Interaction
object VillagerInteraction {

    fun register() {
        UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
            // Only handle villagers
            if (entity !is Villager) return@register InteractionResult.PASS

            // Only server-side processing
            if (level.isClientSide) return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)

            // Emerald in hand = special action
            if (!stack.`is`(Items.EMERALD)) return@register InteractionResult.PASS

            // Block must be individual emeralds (not emerald blocks)
            if (!stack.`is`(Items.EMERALD)) return@register InteractionResult.PASS

            // Handle level up attempt
            return@register handleLevelUp(player as ServerPlayer, entity, stack, level as ServerLevel)
        }
    }

    private fun handleLevelUp(
        player: ServerPlayer,
        villager: Villager,
        emeraldStack: ItemStack,
        level: ServerLevel
    ): InteractionResult {
        val data = villager.villagerData
        val currentLevel = data.level()
        val currentXp = villager.tradingXp

        // Case 1: Already at master level (5)
        if (currentLevel >= 5) {
            player.displayClientMessage(
                Component.literal("Already at master!"),
                true
            )
            return InteractionResult.FAIL
        }

        // Case 2: Not enough XP (0 XP = Phase 70 cycling case, no message)
        val maxXp = VillagerXpConfig.getMaxXpForLevel(currentLevel)
        if (currentXp < maxXp) {
            if (currentXp > 0) {
                player.displayClientMessage(
                    Component.literal("Not enough experience to level up!"),
                    true
                )
            }
            // 0 XP case: PASS to allow Phase 70 cycling logic
            return if (currentXp == 0) InteractionResult.PASS else InteractionResult.FAIL
        }

        // Case 3: Check stage requirement
        val targetLevel = currentLevel + 1
        val requiredStage = targetLevel  // Stage 2 for Apprentice, Stage 3 for Journeyman, etc.
        val currentStage = StageManager.getCurrentStage(player.server)

        if (currentStage < requiredStage) {
            player.displayClientMessage(
                Component.literal("Complete the next trial!"),
                true
            )
            // Emerald NOT consumed when stage not met
            return InteractionResult.FAIL
        }

        // Case 4: Success - level up!
        // Consume emerald
        emeraldStack.shrink(1)

        // Increment level
        villager.villagerData = data.withLevel(targetLevel)

        // Reset XP to 0 for new level
        villager.tradingXp = 0

        // Trigger trade table update for new level
        // Note: Phase 68's VillagerTradesMixin will handle custom trades
        villager.updateTrades(level)

        // Play vanilla level-up effects
        playLevelUpEffects(villager, level)

        return InteractionResult.SUCCESS
    }

    private fun playLevelUpEffects(villager: Villager, level: ServerLevel) {
        // Green happy particles (same as vanilla villager level up)
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            villager.x,
            villager.y + 1.0,
            villager.z,
            10,  // count
            0.5, 0.5, 0.5,  // spread
            0.0  // speed
        )

        // Level up sound
        level.playSound(
            null,
            villager.x,
            villager.y,
            villager.z,
            SoundEvents.VILLAGER_YES,
            villager.soundSource,
            1.0f,
            1.0f
        )
    }
}
```

### Pattern 5: Trade XP Modification

**What:** Set all trades to give 5 XP uniformly
**When to use:** VLEV-05 requires uniform XP per trade

```java
// Source: Phase 68 VillagerTradesMixin, MerchantOffer constructor
// In CustomTradeTables.java (Phase 68), modify trade creation:
public static MerchantOffer createTrade(
        Item costItem, int costCount,
        Item resultItem, int resultCount) {
    return new MerchantOffer(
        new ItemCost(costItem, costCount),
        Optional.empty(),
        new ItemStack(resultItem, resultCount),
        0,                    // Current uses
        Integer.MAX_VALUE,    // Max uses (unlimited)
        5,                    // XP to villager = 5 (VLEV-05)
        0.05f                 // Price multiplier
    );
}
```

**Note:** Phase 68's trade table implementation already sets XP. This phase needs to verify the value is 5 for all trades.

### Anti-Patterns to Avoid

- **Cancelling increaseMerchantCareer() instead of shouldIncreaseLevel():** The former is called less frequently but the latter is the proper gate. Using shouldIncreaseLevel() is cleaner.

- **Modifying trades in getOffers():** This only affects display, not the actual offers stored in NBT. Level-up trades must be set via updateTrades().

- **Using shift+click instead of plain click:** CONTEXT.md explicitly specifies right-click with emerald (no shift), so anything else opens trade GUI. This matches player expectations for "special item interaction."

- **Forgetting to reset XP on level up:** Without XP reset, the villager would immediately have "max XP" for the next level too, requiring only 1 emerald for all 5 levels.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Stage checking | Custom stage tracking | StageManager.getCurrentStage() | Already handles server state persistence |
| Entity interaction | Mixin on mobInteract | UseEntityCallback | Proven pattern from cow milking |
| Level modification | Direct field access | VillagerData.withLevel() | Immutable record pattern, persists correctly |
| Particle effects | Manual packet sending | ServerLevel.sendParticles() | Built-in synchronization |

**Key insight:** The manual leveling system reuses three existing THC patterns:
1. UseEntityCallback for entity interactions (cow milking)
2. StageManager for stage gates (patrol spawning)
3. VillagerData manipulation (Phase 67 profession restriction)

## Common Pitfalls

### Pitfall 1: XP Overflow on Level-Up

**What goes wrong:** If XP isn't reset to 0 on level-up, the villager starts the new level with leftover XP or immediately has "max XP" again.
**Why it happens:** Vanilla auto-leveling handles XP reset internally, but manual leveling bypasses this.
**How to avoid:** Explicitly set `villager.tradingXp = 0` after level increment.
**Warning signs:** Villagers level up multiple times with single emerald click.

### Pitfall 2: Trade GUI Opening After Level-Up

**What goes wrong:** After successfully leveling up, the trade GUI also opens, which is disorienting.
**Why it happens:** UseEntityCallback returns SUCCESS but the vanilla interaction still fires.
**How to avoid:** Return `InteractionResult.SUCCESS` which should consume the interaction. If GUI still opens, may need to mixin `mobInteract()` with HEAD cancellation.
**Warning signs:** Trade GUI opens immediately after level-up particles.

### Pitfall 3: Client-Server Desync on Level Change

**What goes wrong:** Client shows old level in trade GUI until player reopens it.
**Why it happens:** Level change on server doesn't automatically resync the GUI.
**How to avoid:** The `updateTrades()` call should trigger resync. If not, may need explicit packet or GUI close/reopen.
**Warning signs:** Trade GUI shows old-level trades after level-up.

### Pitfall 4: Stage Gate Off-by-One

**What goes wrong:** Stage requirements are off by one level (e.g., Stage 1 unlocks Apprentice instead of Stage 2).
**Why it happens:** Confusion between "current level" and "target level" in the requirement calculation.
**How to avoid:** CONTEXT.md is explicit: Novice->Apprentice needs Stage 2, not Stage 1. Target level equals required stage.
**Warning signs:** Players can level villagers one stage earlier than expected.

### Pitfall 5: 0 XP Case Consuming Emerald

**What goes wrong:** Clicking with emerald at 0 XP consumes the emerald but does nothing (should be reserved for Phase 70).
**Why it happens:** Not checking XP before consuming emerald.
**How to avoid:** At 0 XP, return `InteractionResult.PASS` to allow Phase 70's cycling logic. Never consume emerald at 0 XP.
**Warning signs:** Emeralds disappear when clicking villager with 0 XP.

### Pitfall 6: Emerald Block vs Emerald Item

**What goes wrong:** Emerald blocks also trigger level-up logic.
**Why it happens:** Not checking for `Items.EMERALD` specifically (emerald blocks are a different item).
**How to avoid:** Check `stack.is(Items.EMERALD)` - this is naturally correct since emerald blocks are `Items.EMERALD_BLOCK`.
**Warning signs:** Right-clicking with emerald block causes level-up.

## Code Examples

Verified patterns from official sources:

### Complete Mixin Structure

```java
// Source: THC mixin patterns, MC 1.21.11 Villager class
package thc.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.villager.VillagerXpConfig;

@Mixin(Villager.class)
public abstract class VillagerLevelingMixin {

    @Shadow public abstract VillagerData getVillagerData();
    @Shadow public int tradingXp;

    /**
     * Block automatic leveling - villagers only level via manual emerald payment.
     * VLEV-01: Villagers cannot level up automatically
     */
    @Inject(method = "shouldIncreaseLevel", at = @At("HEAD"), cancellable = true)
    private void thc$blockAutoLeveling(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Cap XP at the maximum for current level to prevent overflow.
     * VLEV-05: XP caps at max for current level (no overflow)
     */
    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void thc$capXpAtLevelMax(MerchantOffer offer, CallbackInfo ci) {
        int currentLevel = this.getVillagerData().level();
        int maxXp = VillagerXpConfig.getMaxXpForLevel(currentLevel);

        if (maxXp > 0 && this.tradingXp > maxXp) {
            this.tradingXp = maxXp;
        }
    }
}
```

### Vanilla Particle/Sound References

```kotlin
// Source: MC ParticleTypes, SoundEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents

// Happy villager particles (green sparkles)
ParticleTypes.HAPPY_VILLAGER

// Villager agreement sound
SoundEvents.VILLAGER_YES

// Alternative: experience orb pickup sound (ding)
SoundEvents.EXPERIENCE_ORB_PICKUP

// Sending particles from server
serverLevel.sendParticles(
    ParticleTypes.HAPPY_VILLAGER,
    x, y, z,
    count,       // Number of particles
    xSpread, ySpread, zSpread,  // Spread
    speed        // Particle speed
)

// Playing sound from server
serverLevel.playSound(
    null,        // Player to exclude (null = play for all)
    x, y, z,
    SoundEvents.VILLAGER_YES,
    SoundSource.NEUTRAL,
    volume,
    pitch
)
```

### Integration Point for Phase 70

```kotlin
// Source: CONTEXT.md "At 0 XP, no message - that's the cycling case for Phase 70"
// In handleLevelUp():

val currentXp = villager.tradingXp

if (currentXp < maxXp) {
    if (currentXp > 0) {
        // Has some XP but not enough - show message
        player.displayClientMessage(
            Component.literal("Not enough experience to level up!"),
            true
        )
        return InteractionResult.FAIL
    }
    // 0 XP case: PASS allows Phase 70 to handle cycling
    // Phase 70 will add its own handler that fires when:
    // - emerald in hand
    // - XP == 0
    // - returns SUCCESS (blocks this handler)
    return InteractionResult.PASS
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Auto-level on XP threshold | Manual emerald payment | THC v2.8 | Players control progression timing |
| Random trade generation | Deterministic tables (Phase 68) | THC v2.8 | Level-up adds specific trades |
| Vanilla XP thresholds (10/70/150/250) | Custom (10/15/20/25) | THC v2.8 | 2/3/4/5 trades per level |

**Deprecated/outdated:**
- `increaseMerchantCareer()` - still exists but never called due to `shouldIncreaseLevel()` always returning false
- Vanilla XP thresholds - replaced by VillagerXpConfig values

## Open Questions

Things that couldn't be fully resolved:

1. **Trade GUI Behavior After Level-Up**
   - What we know: UseEntityCallback.SUCCESS should block further interaction
   - What's unclear: Whether vanilla still opens trade GUI after our handler
   - Recommendation: Test and add mobInteract mixin if needed to block GUI on emerald+level-up

2. **Particle Type for Level-Up**
   - What we know: HAPPY_VILLAGER is the green sparkle particle used for villager happiness
   - What's unclear: Whether this is the exact particle vanilla uses for level-up
   - Recommendation: Use HAPPY_VILLAGER + VILLAGER_YES sound, verify visually matches vanilla

## Sources

### Primary (HIGH confidence)
- THC codebase: THC.kt (UseEntityCallback for cow milking), StageManager.java (stage API)
- THC v2.8 research: STACK.md (VillagerData API, XP thresholds), ARCHITECTURE.md (interaction patterns)
- CONTEXT.md: All implementation decisions locked in

### Secondary (MEDIUM confidence)
- THC v2.8 research: PITFALLS.md (XP/Level threshold mismatch, trade persistence)
- Phase 68 research: Trade table structure, MerchantOffer creation patterns

### Tertiary (LOW confidence)
- Vanilla particle/sound identification: Based on standard Minecraft knowledge, should verify

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All APIs verified via THC codebase and v2.8 research
- Architecture: HIGH - Direct reuse of proven patterns (UseEntityCallback, StageManager)
- Pitfalls: HIGH - XP behavior and edge cases thoroughly documented in PITFALLS.md
- Particles/sounds: MEDIUM - Standard MC identifiers, verify visually

**Research date:** 2026-01-31
**Valid until:** Until MC version upgrade changes Villager/VillagerData APIs (likely 6+ months)
