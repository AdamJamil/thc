# Healing Mechanics Research - Minecraft 1.21.11 Fabric

**Researched:** 2026-01-22
**Domain:** Minecraft Food/Saturation/Healing System
**Confidence:** HIGH - Verified against decompiled 1.21.11 source with Mojang mappings

## Summary

Minecraft's healing system is managed primarily through `FoodData.tick()` called from `ServerPlayer.tick()`. The system has two healing modes: "saturation boost" (fast healing when hunger is full) and "slow healing" (when hunger >= 18). All healing checks the `naturalRegeneration` gamerule. Eating duration is controlled through the `Consumable` data component.

**Primary findings:**
- FoodData handles exhaustion, saturation, and regeneration in a single `tick()` method
- Default eating duration is 1.6 seconds (32 ticks), configured via `Consumable.consumeSeconds`
- Exhaustion threshold is 4.0, removes 1.0 saturation (or 1 hunger if saturation depleted)
- Healing requires hunger >= 18 for slow healing, or hunger == 20 for fast saturation boost

## Vanilla Healing System

### FoodData Class Overview

**Location:** `net.minecraft.world.food.FoodData`

**Fields:**
| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `foodLevel` | int | 20 | Current hunger (0-20) |
| `saturationLevel` | float | 5.0 | Current saturation (0-foodLevel) |
| `exhaustionLevel` | float | 0.0 | Accumulated exhaustion |
| `tickTimer` | int | 0 | Timer for healing/starvation |

**Key Methods:**
| Method | Signature | Purpose |
|--------|-----------|---------|
| `tick` | `void tick(ServerPlayer)` | Per-tick processing of exhaustion, healing, starvation |
| `eat` | `void eat(int nutrition, float saturationModifier)` | Apply food consumption |
| `eat` | `void eat(FoodProperties foodProperties)` | Apply food from properties |
| `addExhaustion` | `void addExhaustion(float f)` | Add exhaustion (capped at 40.0) |

### Natural Regeneration Logic

The `FoodData.tick(ServerPlayer)` method runs every server tick and handles:

1. **Exhaustion processing** (always runs first)
2. **Saturation boost healing** (foodLevel == 20, saturation > 0)
3. **Slow healing** (foodLevel >= 18)
4. **Starvation damage** (foodLevel == 0)

**Vanilla tick() pseudocode:**
```java
void tick(ServerPlayer player) {
    // 1. Process exhaustion
    if (exhaustionLevel > 4.0F) {
        exhaustionLevel -= 4.0F;
        if (saturationLevel > 0.0F) {
            saturationLevel = max(saturationLevel - 1.0F, 0.0F);
        } else if (difficulty != PEACEFUL) {
            foodLevel = max(foodLevel - 1, 0);
        }
    }

    boolean naturalRegen = gameRules.get(NATURAL_HEALTH_REGENERATION);

    // 2. Saturation boost (fast healing) - full hunger
    if (naturalRegen && saturationLevel > 0.0F && player.isHurt() && foodLevel >= 20) {
        tickTimer++;
        if (tickTimer >= 10) {  // Every 0.5 seconds
            float healAmount = min(saturationLevel, 6.0F) / 6.0F;  // Max 1 HP
            player.heal(healAmount);
            addExhaustion(min(saturationLevel, 6.0F));  // Costs saturation
            tickTimer = 0;
        }
    }
    // 3. Slow healing - hunger >= 18
    else if (naturalRegen && foodLevel >= 18 && player.isHurt()) {
        tickTimer++;
        if (tickTimer >= 80) {  // Every 4 seconds
            player.heal(1.0F);
            addExhaustion(6.0F);  // Always costs 6 exhaustion
            tickTimer = 0;
        }
    }
    // 4. Starvation - hunger == 0
    else if (foodLevel <= 0) {
        tickTimer++;
        if (tickTimer >= 80) {
            // Damage depends on difficulty
            if (player.getHealth() > 10.0F || difficulty == HARD ||
                (player.getHealth() > 1.0F && difficulty == NORMAL)) {
                player.hurt(starve(), 1.0F);
            }
            tickTimer = 0;
        }
    }
    // 5. Reset timer when no healing/starving conditions met
    else {
        tickTimer = 0;
    }
}
```

### Saturation Boost Mechanics

"Saturation boost" is the fast healing that occurs when hunger bar is completely full (20):
- Heals every **10 ticks** (0.5 seconds)
- Heals **saturation/6** HP per tick (max 1 HP, since saturation capped at 6 per tick)
- Consumes saturation equal to amount used for healing calculation
- Only active when `player.isHurt()` returns true (health < maxHealth)

### Peaceful Mode Regeneration

`ServerPlayer.tickRegeneration()` provides additional healing on Peaceful difficulty:
- Heals 1 HP every 20 ticks (1 second) if damaged
- Regenerates saturation by 1 every 20 ticks (up to 20)
- Regenerates hunger by 1 every 10 ticks if hungry
- Only active when `naturalRegeneration` gamerule is true

## Exhaustion Processing

### How Exhaustion Accumulates

**Source:** `Player.causeFoodExhaustion(float)` called from various actions.

**Exhaustion costs (from FoodConstants):**
| Action | Exhaustion Cost |
|--------|-----------------|
| Walking | 0.0 |
| Crouching | 0.0 |
| Swimming | 0.01 per meter |
| Sprinting | 0.1 per meter |
| Jumping | 0.05 |
| Sprint-jumping | 0.2 |
| Mining blocks | 0.005 per block |
| Attacking | 0.1 per attack |
| Healing (via food) | 6.0 per HP healed |

### The 4.0 Threshold Processing

**Location:** Beginning of `FoodData.tick()`

```java
if (this.exhaustionLevel > 4.0F) {
    this.exhaustionLevel -= 4.0F;
    if (this.saturationLevel > 0.0F) {
        this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
    } else if (difficulty != Difficulty.PEACEFUL) {
        this.foodLevel = Math.max(this.foodLevel - 1, 0);
    }
}
```

**Key insight:** Exhaustion is processed **before** healing logic. If you modify exhaustion processing, healing behavior in the same tick is affected.

### Method to Intercept

**Target:** `FoodData.tick(ServerPlayer)`

**Injection point for modified exhaustion (1.21 saturation loss):**
```java
@Redirect(
    method = "tick",
    at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0)
)
private float thc$modifyExhaustionSaturationLoss(float a, float b) {
    // Vanilla: Math.max(saturationLevel - 1.0F, 0.0F)
    // THC: Math.max(saturationLevel - 1.21F, 0.0F)
    return Math.max(this.saturationLevel - 1.21F, 0.0F);
}
```

**Alternative: Shadow field approach:**
```java
@Shadow private float saturationLevel;
@Shadow private float exhaustionLevel;

@Inject(method = "tick", at = @At("HEAD"))
private void thc$processExhaustion(ServerPlayer player, CallbackInfo ci) {
    if (this.exhaustionLevel > 4.0F) {
        this.exhaustionLevel -= 4.0F;
        if (this.saturationLevel > 0.0F) {
            this.saturationLevel = Math.max(this.saturationLevel - 1.21F, 0.0F);
        }
        // Cancel vanilla processing somehow...
    }
}
```

## Eating Mechanics

### Food Consumption Flow

1. `Player.use()` -> `Consumable.startConsuming()` -> `player.startUsingItem(hand)`
2. `LivingEntity.tick()` decrements `useItemRemaining` each tick
3. When `useItemRemaining == 0` -> `LivingEntity.completeUsingItem()`
4. `Item.finishUsingItem()` -> `Consumable.onConsume()`
5. `ConsumableListener.onConsume()` processes food effects
6. `FoodData.eat(FoodProperties)` applies nutrition and saturation

### Where Saturation is Applied

**Location:** `FoodData.eat(FoodProperties)` -> `FoodData.add(int nutrition, float saturation)`

```java
private void add(int nutrition, float saturation) {
    this.foodLevel = Mth.clamp(nutrition + this.foodLevel, 0, 20);
    this.saturationLevel = Mth.clamp(saturation + this.saturationLevel, 0.0F, (float)this.foodLevel);
}
```

**THC requirement:** `saturation = max(food_sat, current_sat)` instead of `current_sat + food_sat`

**Mixin approach:**
```java
@Redirect(
    method = "add",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F")
)
private float thc$maxSaturation(float value, float min, float max) {
    // Instead of adding, take the max
    float newSaturation = /* calculate based on context */;
    return Mth.clamp(newSaturation, min, max);
}
```

### Eating Duration Control

**Location:** `Item.getUseDuration(ItemStack, LivingEntity)` returns `Consumable.consumeTicks()`

**Consumable.consumeTicks():**
```java
public int consumeTicks() {
    return (int)(this.consumeSeconds * 20.0F);
}
```

**Default:** 1.6 seconds = 32 ticks

**THC requirement:** 64 ticks (3.2 seconds)

**Approaches:**

1. **Per-item component modification** (data-driven):
   - Modify all food items to have custom Consumable with `consumeSeconds(3.2F)`
   - Requires data generation or runtime modification

2. **Global mixin on getUseDuration** (recommended):
```java
@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void thc$doubleEatingDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (stack.has(DataComponents.FOOD)) {
            cir.setReturnValue(cir.getReturnValue() * 2);  // 32 -> 64 ticks
        }
    }
}
```

3. **Mixin on Consumable.consumeTicks():**
```java
@Mixin(Consumable.class)
public class ConsumableMixin {
    @Inject(method = "consumeTicks", at = @At("RETURN"), cancellable = true)
    private void thc$doubleConsumeTicks(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() * 2);
    }
}
```

## Mixin Strategy

### Recommended Injection Points

| Feature | Target Class | Method | Injection Type | Priority |
|---------|--------------|--------|----------------|----------|
| Disable vanilla regen | FoodData | tick | @Redirect or @Overwrite | 1 |
| Custom exhaustion (1.21) | FoodData | tick | @ModifyVariable or @Redirect | 2 |
| Max saturation on eat | FoodData | add | @Redirect or @ModifyVariable | 3 |
| Double eating duration | Item | getUseDuration | @ModifyReturnValue | 4 |
| Custom healing tiers | ServerPlayer | tick | @Inject(TAIL) | 5 |

### Order of Implementation

1. **Disable vanilla natural regeneration**
   - Simplest approach: Redirect the `serverPlayer.heal()` calls in `FoodData.tick()` to no-op
   - Or: Check a custom flag/gamerule before allowing vanilla healing

2. **Modify exhaustion processing**
   - Must happen before healing logic
   - Use `@ModifyVariable` on `saturationLevel` assignment or `@Redirect` on `Math.max`

3. **Implement max saturation on eat**
   - Intercept `FoodData.add()` method
   - Replace additive saturation with max comparison

4. **Implement custom healing tick**
   - Add `@Inject` at end of `ServerPlayer.tick()` (after `foodData.tick()`)
   - Implement tiered healing based on saturation thresholds
   - Requires hunger >= 18 check

5. **Extend eating duration**
   - Global modification via `Item.getUseDuration()` mixin
   - Apply 2x multiplier for food items

### Potential Conflicts

**Existing FoodDataMixin:**
The project already has a `FoodDataMixin` that modifies the heal amount:
```java
@ModifyArg(
    method = "tick",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"),
    index = 0
)
private float thc$halveNaturalRegen(float amount) {
    return amount * 0.5F;
}
```

This will need to be replaced with complete healing override.

**Peaceful mode:**
`ServerPlayer.tickRegeneration()` provides separate healing in Peaceful mode. May need additional mixin to disable if full healing override is desired.

**Regeneration status effect:**
The Regeneration potion effect (`MobEffects.REGENERATION`) applies healing through `LivingEntity.tickEffects()`, separate from food system. This is unaffected by FoodData changes.

**Absorption:**
Golden apples grant absorption hearts via status effect, not FoodData. No conflict.

## FoodConstants Reference

**Location:** `net.minecraft.world.food.FoodConstants`

| Constant | Value | Description |
|----------|-------|-------------|
| MAX_FOOD | 20 | Maximum hunger level |
| MAX_SATURATION | 20.0F | Maximum saturation |
| START_SATURATION | 5.0F | Initial saturation on spawn |
| EXHAUSTION_DROP | 4.0F | Threshold for exhaustion processing |
| HEALTH_TICK_COUNT | 80 | Ticks between slow heals (4 seconds) |
| HEALTH_TICK_COUNT_SATURATED | 10 | Ticks between saturation boost heals (0.5 seconds) |
| HEAL_LEVEL | 18 | Minimum food level for slow healing |
| SPRINT_LEVEL | 6 | Minimum food level for sprinting |
| STARVE_LEVEL | 0 | Food level for starvation damage |
| EXHAUSTION_HEAL | 6.0F | Exhaustion cost per heal |

## Implementation Notes for THC

### Saturation-Tiered Healing System

**THC requirements from PROJECT.md:**
- Healing requires hunger >= 18 (9 bars)
- Base healing: 3/16 hearts/s when hunger >= 18
- Tier bonuses based on saturation:
  - T5 (6.36+): +1 heart/s
  - T4 (2.73+): +0.5 heart/s
  - T3 (1.36+): +3/16 heart/s
  - T2 (0.45+): +1/8 heart/s
  - T1 (0-0.45): +1/16 heart/s

**Implementation approach:**
```java
// In ServerPlayer tick, after foodData.tick() which now does NO healing
if (foodData.getFoodLevel() >= 18 && player.isHurt()) {
    float saturation = foodData.getSaturationLevel();
    float healRate = 3.0F / 16.0F;  // Base rate

    if (saturation >= 6.36F) {
        healRate += 1.0F;        // T5
    } else if (saturation >= 2.73F) {
        healRate += 0.5F;        // T4
    } else if (saturation >= 1.36F) {
        healRate += 3.0F / 16.0F; // T3
    } else if (saturation >= 0.45F) {
        healRate += 1.0F / 8.0F;  // T2
    } else {
        healRate += 1.0F / 16.0F; // T1
    }

    // healRate is in hearts per second
    // Convert to HP per tick: healRate * 2 / 20
    float hpPerTick = healRate * 2.0F / 20.0F;
    player.heal(hpPerTick);

    // Exhaustion cost per HP healed
    foodData.addExhaustion(hpPerTick * 6.0F);  // Match vanilla ratio
}
```

### Key Design Decisions

1. **Disable vanilla completely** rather than modifying - cleaner implementation
2. **Run custom healing AFTER foodData.tick()** - exhaustion still processed normally
3. **Per-tick healing** rather than interval-based - smoother health regeneration
4. **Preserve exhaustion costs** - 6.0 exhaustion per HP matches vanilla

## Sources

### Primary (HIGH confidence)
- Decompiled source: `minecraft-common-6dd721cd7d-1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2-sources.jar`
  - `net/minecraft/world/food/FoodData.java`
  - `net/minecraft/world/food/FoodConstants.java`
  - `net/minecraft/world/item/component/Consumable.java`
  - `net/minecraft/world/item/component/Consumables.java`
  - `net/minecraft/server/level/ServerPlayer.java`
  - `net/minecraft/world/entity/player/Player.java`
  - `net/minecraft/world/item/Item.java`

### Secondary (MEDIUM confidence)
- [Yarn HungerManager API 1.21+build.7](https://maven.fabricmc.net/docs/yarn-1.21+build.7/net/minecraft/entity/player/HungerManager.html)
- [Yarn HungerConstants 1.21.7](https://maven.fabricmc.net/docs/yarn-1.21.7+build.1/net/minecraft/entity/player/HungerConstants.html)
- [Minecraft Wiki - Hunger](https://minecraft.wiki/w/Hunger)
- [Fabric Documentation - Food Items](https://docs.fabricmc.net/develop/items/food)

## Metadata

**Confidence breakdown:**
- Class/method names: HIGH - Verified against 1.21.11 decompiled source
- Injection points: HIGH - Traced through actual code paths
- Timing values: HIGH - Constants from FoodConstants class
- Mixin approaches: MEDIUM - Standard patterns, untested for this specific use case

**Research date:** 2026-01-22
**Valid until:** Estimated stable through 1.21.x series; verify on major version changes
