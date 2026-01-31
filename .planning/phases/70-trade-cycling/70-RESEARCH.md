# Phase 70: Trade Cycling - Research

**Researched:** 2026-01-31
**Domain:** Minecraft villager trade reroll system (MC 1.21.11 Fabric)
**Confidence:** HIGH

## Summary

Phase 70 implements trade cycling, allowing players to reroll current-rank trades by right-clicking a villager with an emerald when the villager has 0 XP. This builds directly on Phase 68's deterministic trade tables and integrates with Phase 69's manual leveling system.

The implementation uses the proven UseEntityCallback pattern, checking for 0 XP (Phase 69 passes at 0 XP specifically to allow Phase 70 to handle cycling). The cycling logic removes trades for the current level only, calls CustomTradeTables.getTradesFor() to regenerate them with a fresh random seed (rerolling 50/50 variants), preserves all lower-level trades, and handles the single-trade-pool edge case by blocking cycling without consuming emerald.

**Primary recommendation:** Extend the existing UseEntityCallback registration site with trade cycling logic that fires when emerald in hand + villager XP == 0. Use CustomTradeTables to regenerate current-level trades, preserving earlier ranks. Success feedback via HAPPY_VILLAGER particles + VILLAGER_YES sound; failure via VILLAGER_NO sound (head shake).

## Standard Stack

The established libraries/tools for this domain:

### Core

| Class | Package | Purpose | Why Standard |
|-------|---------|---------|--------------|
| `UseEntityCallback` | `net.fabricmc.fabric.api.event.player` | Entity interaction events | Proven pattern in THC (cow milking), integrates with Phase 69 |
| `Villager` | `net.minecraft.world.entity.npc.villager` | Villager entity with XP and offers | Has `getVillagerXp()` for 0 XP check, `getOffers()` for trade modification |
| `MerchantOffers` | `net.minecraft.world.item.trading` | Trade list (extends ArrayList) | Supports `removeIf()`, `addAll()` for replacing trades |
| `CustomTradeTables` | `thc.villager` | Deterministic trade factory | Phase 68 provides `getTradesFor(profession, level, serverLevel, random)` |
| `VillagerData` | `net.minecraft.world.entity.npc.villager` | Immutable profession/level record | `level()` to determine which trades to cycle |

### Supporting

| Class | Package | Purpose | When to Use |
|-------|---------|---------|-------------|
| `ParticleTypes` | `net.minecraft.core.particles` | Particle constants | HAPPY_VILLAGER for success |
| `SoundEvents` | `net.minecraft.sounds` | Sound event constants | VILLAGER_YES for success, VILLAGER_NO for failure |
| `AllowedProfessions` | `thc.villager` | Profession validation | Phase 67 helper, verify cycling only for allowed professions |

### Trade Counts per Level (from Phase 68)

Understanding trade counts is critical for cycling logic:

| Profession | L1 | L2 | L3 | L4 | L5 | Total |
|------------|----|----|----|----|----|----|
| Librarian | 2 | 2 | 2 | 2 | 1 | 9 |
| Butcher | 2 | 2 | 2 | 1 | 1 | 8 |
| Mason | 4 | 2 | 2 | 1 | 1 | 10 |
| Cartographer | 3 | 3 | 2 | 1 | 1 | 10 |

**Critical insight:** When cycling, we must count trades to know how many to remove from the end of the offers list. Earlier levels' trades are at the start; current level's trades are at the end.

## Architecture Patterns

### Recommended Project Structure

```
src/main/
  kotlin/thc/
    villager/
      TradeCycling.kt         # UseEntityCallback handler for emerald cycling
      VillagerInteraction.kt  # Extended from Phase 69 (if already exists)

  java/thc/
    villager/
      CustomTradeTables.java  # Phase 68 - provides getTradesFor(), getTradeCount()
```

### Pattern 1: UseEntityCallback Integration with Phase 69

**What:** Handle emerald right-click at 0 XP for trade cycling
**When to use:** Core interaction pattern for VCYC-01

Phase 69's handler returns `PASS` when XP == 0, specifically allowing Phase 70's handler to process.

```kotlin
// Source: CONTEXT.md, Phase 69 integration point
object TradeCycling {

    fun register() {
        UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
            // Only handle villagers
            if (entity !is Villager) return@register InteractionResult.PASS

            // Only server-side processing
            if (level.isClientSide) return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)

            // Only emeralds trigger cycling
            if (!stack.`is`(Items.EMERALD)) return@register InteractionResult.PASS

            // Core requirement: XP must be 0 for cycling
            val villager = entity as Villager
            if (villager.villagerXp != 0) return@register InteractionResult.PASS

            // Handle cycling attempt
            return@register handleCycle(player as ServerPlayer, villager, stack, level as ServerLevel)
        }
    }

    private fun handleCycle(
        player: ServerPlayer,
        villager: Villager,
        emeraldStack: ItemStack,
        level: ServerLevel
    ): InteractionResult {
        val data = villager.villagerData
        val currentLevel = data.level()

        // Get profession key for trade table lookup
        val profKey = data.profession().unwrapKey().orElse(null)
            ?: return InteractionResult.PASS // NONE profession, skip

        // Check if profession has custom trades (only allowed professions)
        if (!CustomTradeTables.hasCustomTrades(profKey)) {
            return InteractionResult.PASS
        }

        // Check trade pool size for current level
        val poolSize = CustomTradeTables.getTradePoolSize(profKey, currentLevel)
        if (poolSize <= 1) {
            // Single trade pool - cannot cycle
            playFailureEffects(villager, level)
            return InteractionResult.SUCCESS // Block trade GUI but don't consume emerald
        }

        // Success - cycle trades!
        emeraldStack.shrink(1)

        // Regenerate current-level trades
        cycleCurrentLevelTrades(villager, profKey, currentLevel, level)

        // Play success feedback
        playSuccessEffects(villager, level)

        return InteractionResult.SUCCESS
    }
}
```

### Pattern 2: Trade Cycling Logic

**What:** Remove current-level trades and regenerate from trade table
**When to use:** When cycling is permitted (pool size > 1)

```kotlin
// Source: Phase 68 trade structure, CONTEXT.md cycling behavior
private fun cycleCurrentLevelTrades(
    villager: Villager,
    profKey: ResourceKey<VillagerProfession>,
    currentLevel: Int,
    level: ServerLevel
) {
    val offers = villager.offers

    // Calculate how many trades to remove (current level only)
    val tradesAtCurrentLevel = CustomTradeTables.getTradeCount(profKey, currentLevel)
    val tradesBeforeCurrentLevel = (1 until currentLevel).sumOf {
        CustomTradeTables.getTradeCount(profKey, it)
    }

    // Remove trades from current level (they're at the end)
    val startIndex = tradesBeforeCurrentLevel
    while (offers.size > startIndex) {
        offers.removeAt(offers.size - 1)
    }

    // Regenerate current-level trades with fresh random
    val newTrades = CustomTradeTables.getTradesFor(
        profKey,
        currentLevel,
        level,
        level.random
    )
    offers.addAll(newTrades)
}
```

### Pattern 3: Trade Pool Size Query

**What:** Check if cycling is possible (pool has 2+ options)
**When to use:** Before cycling, to handle single-trade pools

Phase 68's CustomTradeTables needs a method to query pool size. For professions with 50/50 variants, pool size is 2. For deterministic-only slots, pool size is 1.

```java
// Source: Phase 68 extension for Phase 70
// Add to CustomTradeTables.java

/**
 * Returns the effective pool size for a profession at a given level.
 * Pool size > 1 means cycling can produce different results.
 *
 * <p>For librarian with 50/50 variants at every slot, pool = 2.
 * For butcher with deterministic slots only, pool = 1.
 */
public static int getTradePoolSize(ResourceKey<VillagerProfession> profession, int level) {
    // All librarian trades have 50/50 variants
    if (profession.location().equals(VillagerProfession.LIBRARIAN.location())) {
        return 2;  // Every slot has 2 options
    }

    // Mason has 50/50 starting at level 2
    if (profession.location().equals(VillagerProfession.MASON.location())) {
        return level >= 2 ? 2 : 1;  // Level 1 is deterministic, 2+ have variants
    }

    // Butcher and cartographer are fully deterministic
    return 1;
}
```

### Pattern 4: Feedback Effects

**What:** Visual and audio feedback for success/failure
**When to use:** After cycling attempt

```kotlin
// Source: MC ParticleTypes, SoundEvents, CONTEXT.md feedback decisions
private fun playSuccessEffects(villager: Villager, level: ServerLevel) {
    // Happy villager particles (green sparkles)
    level.sendParticles(
        ParticleTypes.HAPPY_VILLAGER,
        villager.x,
        villager.y + 1.0,
        villager.z,
        10,           // count
        0.5, 0.5, 0.5, // spread
        0.0           // speed
    )

    // Villager agreement sound
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

private fun playFailureEffects(villager: Villager, level: ServerLevel) {
    // Villager "no" sound (head shake)
    level.playSound(
        null,
        villager.x,
        villager.y,
        villager.z,
        SoundEvents.VILLAGER_NO,
        villager.soundSource,
        1.0f,
        1.0f
    )

    // Optional: angry villager particles (red swirl)
    level.sendParticles(
        ParticleTypes.ANGRY_VILLAGER,
        villager.x,
        villager.y + 1.5,
        villager.z,
        3,            // fewer particles for failure
        0.3, 0.3, 0.3,
        0.0
    )
}
```

### Anti-Patterns to Avoid

- **Clearing all trades:** Only current-level trades should be removed. Earlier levels must be preserved (VCYC-02).

- **Using same random seed:** The cycling must produce different results. Use `level.random` for fresh randomness, not a fixed seed.

- **Consuming emerald on failure:** When pool size is 1, emerald should NOT be consumed (CONTEXT.md decision).

- **Missing GUI sync:** After modifying offers, the client must see changes. Since GUI is closed during cycling (right-click, not trade screen), reopening will naturally sync. However, verify this works correctly.

- **Forgetting to check 0 XP:** The 0 XP check is the integration point with Phase 69. Without it, cycling could fire at wrong times.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Trade generation | Manual offer creation | CustomTradeTables.getTradesFor() | Phase 68 already has the deterministic table |
| Pool size check | Counting 50/50 slots manually | CustomTradeTables.getTradePoolSize() | Centralized in Phase 68 extension |
| Profession validation | Manual key comparison | AllowedProfessions.isAllowed() | Phase 67 helper already exists |
| Particle/sound feedback | Custom packet handling | ServerLevel.sendParticles/playSound | Built-in server-side methods |

**Key insight:** Cycling is essentially "re-calling Phase 68's trade generation for current level only." The trade table structure supports this directly; cycling just needs to know how many trades to remove before regenerating.

## Common Pitfalls

### Pitfall 1: Breaking Earlier Level Trades

**What goes wrong:** Cycling removes or modifies trades from earlier levels.
**Why it happens:** Incorrect index calculation when removing current-level trades.
**How to avoid:** Calculate `tradesBeforeCurrentLevel` by summing trade counts for levels 1 through (currentLevel-1). Only remove trades at indices >= this sum.
**Warning signs:** After cycling, Level 1 trades are different or missing.

### Pitfall 2: Same Trade After Cycling

**What goes wrong:** Cycling produces the exact same trade, wasting emerald.
**Why it happens:** Random seed not refreshed, or pool size is actually 1.
**How to avoid:** Use `level.random` for fresh randomness. For truly deterministic trades (pool size 1), block cycling entirely.
**Warning signs:** Players report cycling never changes anything.

### Pitfall 3: Client-Server Trade Desync

**What goes wrong:** Client shows old trades after cycling; need to reopen GUI to see changes.
**Why it happens:** Offers modified server-side but client not notified.
**How to avoid:** Since cycling happens with GUI closed (right-click interaction), opening trade GUI after cycling will naturally sync. Test to verify this works. If not, may need explicit packet or villager data mark.
**Warning signs:** Trades look unchanged after cycling until GUI closed/reopened.

### Pitfall 4: Cycling Non-Custom Professions

**What goes wrong:** Attempting to cycle trades for non-allowed professions (armorer, etc.) causes errors or unexpected behavior.
**Why it happens:** Pre-existing villagers or vanilla professions don't have custom trade tables.
**How to avoid:** Check `CustomTradeTables.hasCustomTrades(profKey)` before cycling. Return PASS for non-custom professions.
**Warning signs:** Errors when interacting with non-allowed profession villagers.

### Pitfall 5: Emerald Consumed on Blocked Cycling

**What goes wrong:** Emerald disappears even when cycling was blocked (pool size 1).
**Why it happens:** Emerald shrink happens before pool size check.
**How to avoid:** Check pool size BEFORE consuming emerald. Only shrink on confirmed successful cycle.
**Warning signs:** Players lose emeralds when cycling butcher/cartographer (deterministic professions).

### Pitfall 6: XP Not Actually 0

**What goes wrong:** Cycling fires when villager has traded but XP shows 0 due to UI lag.
**Why it happens:** XP might not update immediately after trade.
**How to avoid:** Use `villager.getVillagerXp()` (not `tradingXp` field directly) for authoritative value. Phase 69's XP capping ensures XP is accurate.
**Warning signs:** Cycling works immediately after trading (shouldn't - XP > 0).

## Code Examples

### Complete Cycling Handler

```kotlin
// Source: THC patterns, Phase 68/69 integration
package thc.villager

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object TradeCycling {

    fun register() {
        UseEntityCallback.EVENT.register { player, level, hand, entity, _ ->
            if (entity !is Villager) return@register InteractionResult.PASS
            if (level.isClientSide) return@register InteractionResult.PASS

            val stack = player.getItemInHand(hand)
            if (!stack.`is`(Items.EMERALD)) return@register InteractionResult.PASS

            val villager = entity as Villager
            // Core Phase 70 gate: 0 XP required for cycling
            if (villager.villagerXp != 0) return@register InteractionResult.PASS

            return@register handleCycle(
                player as ServerPlayer,
                villager,
                stack,
                level as ServerLevel
            )
        }
    }

    private fun handleCycle(
        player: ServerPlayer,
        villager: Villager,
        emeraldStack: ItemStack,
        level: ServerLevel
    ): InteractionResult {
        val data = villager.villagerData
        val currentLevel = data.level()

        val profKey = data.profession().unwrapKey().orElse(null)
            ?: return InteractionResult.PASS

        if (!CustomTradeTables.hasCustomTrades(profKey)) {
            return InteractionResult.PASS
        }

        // Check if cycling is meaningful (pool > 1)
        val poolSize = CustomTradeTables.getTradePoolSize(profKey, currentLevel)
        if (poolSize <= 1) {
            playFailureEffects(villager, level)
            return InteractionResult.SUCCESS // Block GUI, no emerald consumed
        }

        // Successful cycle
        emeraldStack.shrink(1)
        cycleCurrentLevelTrades(villager, profKey, currentLevel, level)
        playSuccessEffects(villager, level)

        return InteractionResult.SUCCESS
    }

    private fun cycleCurrentLevelTrades(
        villager: Villager,
        profKey: ResourceKey<VillagerProfession>,
        currentLevel: Int,
        level: ServerLevel
    ) {
        val offers = villager.offers

        // Calculate trades before current level
        val tradesBeforeCurrentLevel = (1 until currentLevel).sumOf {
            CustomTradeTables.getTradeCount(profKey, it)
        }

        // Remove current level trades (at the end of the list)
        while (offers.size > tradesBeforeCurrentLevel) {
            offers.removeAt(offers.size - 1)
        }

        // Regenerate with fresh random
        val newTrades = CustomTradeTables.getTradesFor(
            profKey,
            currentLevel,
            level,
            level.random
        )
        offers.addAll(newTrades)
    }

    private fun playSuccessEffects(villager: Villager, level: ServerLevel) {
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            villager.x, villager.y + 1.0, villager.z,
            10, 0.5, 0.5, 0.5, 0.0
        )
        level.playSound(
            null,
            villager.x, villager.y, villager.z,
            SoundEvents.VILLAGER_YES,
            villager.soundSource,
            1.0f, 1.0f
        )
    }

    private fun playFailureEffects(villager: Villager, level: ServerLevel) {
        level.playSound(
            null,
            villager.x, villager.y, villager.z,
            SoundEvents.VILLAGER_NO,
            villager.soundSource,
            1.0f, 1.0f
        )
    }
}
```

### CustomTradeTables Extensions (Phase 68)

```java
// Source: Add to existing CustomTradeTables.java from Phase 68
// These methods support Phase 70 cycling

/**
 * Returns the number of trades for a profession at a specific level.
 */
public static int getTradeCount(ResourceKey<VillagerProfession> profession, int level) {
    if (profession.location().equals(VillagerProfession.LIBRARIAN.location())) {
        return switch (level) {
            case 1, 2, 3, 4 -> 2;
            case 5 -> 1;
            default -> 0;
        };
    }
    if (profession.location().equals(VillagerProfession.BUTCHER.location())) {
        return switch (level) {
            case 1, 2, 3 -> 2;
            case 4, 5 -> 1;
            default -> 0;
        };
    }
    if (profession.location().equals(VillagerProfession.MASON.location())) {
        return switch (level) {
            case 1 -> 4;
            case 2, 3 -> 2;
            case 4, 5 -> 1;
            default -> 0;
        };
    }
    if (profession.location().equals(VillagerProfession.CARTOGRAPHER.location())) {
        return switch (level) {
            case 1, 2 -> 3;
            case 3 -> 2;
            case 4, 5 -> 1;
            default -> 0;
        };
    }
    return 0;
}

/**
 * Returns the trade pool size (number of distinct options) for cycling.
 * Pool > 1 means cycling produces different results (50/50 variants).
 * Pool = 1 means deterministic (cycling blocked).
 */
public static int getTradePoolSize(ResourceKey<VillagerProfession> profession, int level) {
    // Librarian: all slots have 50/50 variants
    if (profession.location().equals(VillagerProfession.LIBRARIAN.location())) {
        return 2;
    }
    // Mason: level 1 deterministic, levels 2-5 have 50/50 variants
    if (profession.location().equals(VillagerProfession.MASON.location())) {
        return level >= 2 ? 2 : 1;
    }
    // Butcher and Cartographer: all deterministic
    return 1;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Random trade reroll (vanilla) | Deterministic pool cycling | THC v2.8 | Controlled progression within trade tables |
| Automatic trade restocking | Unlimited trades + manual cycling | THC v2.8 | Player controls trade discovery |
| GUI-based cycling mods | Right-click interaction | THC v2.8 | Consistent with manual leveling pattern |

**Deprecated/outdated:**
- Trade Cycling mod (Modrinth reference) - uses different interaction pattern; THC uses unified emerald right-click for both leveling and cycling
- Vanilla restocking mechanics - THC trades are unlimited (maxUses = Integer.MAX_VALUE), no restocking needed

## Open Questions

Things that couldn't be fully resolved:

1. **GUI Sync After Cycling**
   - What we know: Cycling happens with GUI closed (right-click on villager, not from trade screen)
   - What's unclear: Whether opening trade GUI after cycling shows updated trades immediately
   - Recommendation: Test and verify. If desync occurs, may need `villager.setChanged()` or explicit sync packet

2. **Event Registration Order**
   - What we know: Phase 69 returns PASS at 0 XP, Phase 70 should catch this
   - What's unclear: Whether UseEntityCallback registration order guarantees Phase 69 runs before Phase 70
   - Recommendation: Can register in same event handler with internal branching, or use event priority if available

## Sources

### Primary (HIGH confidence)
- THC codebase: THC.kt (UseEntityCallback for cow milking), Phase 68 CustomTradeTables structure, Phase 69 0 XP integration point
- Phase 68 RESEARCH.md: Trade count summary, MerchantOffer/MerchantOffers API
- Phase 69 RESEARCH.md: UseEntityCallback pattern, success/failure feedback
- CONTEXT.md: All cycling decisions locked (0 XP gate, pool size blocking, feedback)
- MC 1.21.11: MerchantOffers (ArrayList), ParticleTypes, SoundEvents

### Secondary (MEDIUM confidence)
- THC v2.8 research: PITFALLS.md (client-server desync), STACK.md (trade cycling pattern)
- REQUIREMENTS.md: Trade counts per level (TLIB, TBUT, TMAS, TCRT specifications)

### Tertiary (LOW confidence)
- None - all findings verified against primary sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All APIs verified via THC codebase and MC 1.21.11
- Architecture: HIGH - Direct reuse of Phase 69 patterns, Phase 68 trade tables
- Pitfalls: HIGH - Trade desync and index calculation verified against research docs
- Feedback: HIGH - SoundEvents.VILLAGER_YES/NO and ParticleTypes verified via javap

**Research date:** 2026-01-31
**Valid until:** Until Phase 68/69 implementation changes or MC version upgrade (likely 6+ months)
