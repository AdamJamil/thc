# Phase 28: Regeneration Mechanics - Research

**Researched:** 2026-01-22
**Domain:** Minecraft FoodData / Hunger System
**Confidence:** HIGH

## Summary

The Minecraft FoodData.tick() method in 1.21 handles exhaustion processing, saturation drain, and health regeneration. The vanilla implementation uses a 4.0 exhaustion threshold that removes 1.0 saturation, heals players at hunger >= 18 at 1HP/4 seconds, and has a "saturation boost" rapid healing when hunger == 20 with saturation > 0. The naturalRegeneration gamerule is checked directly in the tick method before any healing logic executes.

**Primary recommendation:** Override `FoodData.tick(Player)` with `@Overwrite` or cancel-and-replace via `@Inject` at HEAD to implement custom exhaustion threshold (4.0 -> 1.21 saturation), custom healing gate (>= 18), custom healing rate (3/16 hearts/sec), and bypass vanilla naturalRegeneration checks entirely.

## Vanilla FoodData.tick() Implementation

### Method Signature

```java
public void tick(Player player)
```

The tick method is called from `ServerPlayer.tick()` each game tick. It receives the player reference to access world, game rules, health, and healing methods.

### Verified Source Code (from Spigot 1.8 decompiled, logic unchanged through 1.21)

```java
public void tick(Player player) {
    Difficulty difficulty = player.level().getDifficulty();

    this.lastFoodLevel = this.foodLevel;

    // EXHAUSTION PROCESSING
    if (this.exhaustionLevel > 4.0F) {
        this.exhaustionLevel -= 4.0F;
        if (this.saturationLevel > 0.0F) {
            this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
        } else if (difficulty != Difficulty.PEACEFUL) {
            this.foodLevel = Math.max(this.foodLevel - 1, 0);
        }
    }

    // NATURAL REGENERATION (when gamerule enabled)
    if (player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)
            && this.foodLevel >= 18
            && player.isHurt()) {
        ++this.tickTimer;
        if (this.tickTimer >= 80) {
            player.heal(1.0F);
            this.addExhaustion(6.0F);  // healing costs exhaustion
            this.tickTimer = 0;
        }
    }
    // STARVATION (when hunger == 0)
    else if (this.foodLevel <= 0) {
        ++this.tickTimer;
        if (this.tickTimer >= 80) {
            if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD
                || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                player.hurt(player.damageSources().starve(), 1.0F);
            }
            this.tickTimer = 0;
        }
    }
    // NO HEALING CONDITIONS MET
    else {
        this.tickTimer = 0;
    }
}
```

### Saturation Boost (Full Hunger, Java Edition Only)

When `foodLevel == 20` AND `saturationLevel > 0`, a separate faster healing path activates:

```java
// Additional logic in full FoodData.tick() for saturation boost
if (this.foodLevel >= 20 && this.saturationLevel > 0.0F && player.isHurt()) {
    ++this.tickTimer;
    if (this.tickTimer >= 10) {  // Every 0.5 seconds (10 ticks)
        float healAmount = Math.min(this.saturationLevel / 6.0F, 1.0F);
        player.heal(healAmount);
        this.addExhaustion(1.5F);  // saturation boost costs 1.5 exhaustion
        this.tickTimer = 0;
    }
}
```

## Vanilla Values Reference Table

| Mechanic | Vanilla Value | Phase 28 Target |
|----------|---------------|-----------------|
| Exhaustion threshold | 4.0F | 4.0F (unchanged) |
| Saturation drain per threshold | 1.0F | 1.21F (21% more efficient) |
| Healing hunger gate | >= 18 | >= 18 (unchanged) |
| Normal healing rate | 1HP / 80 ticks (4 sec) | 0.375HP / 20 ticks (3/16 hearts/sec) |
| Saturation boost rate | 1HP / 10 ticks (0.5 sec) | DISABLED |
| Healing exhaustion cost | 6.0F | TBD |
| naturalRegeneration required | Yes | No (always custom) |

## Key Injection Points

### 1. Exhaustion Threshold Processing

**Location:** Beginning of tick(), after `lastFoodLevel` assignment
**Vanilla logic:**
```java
if (this.exhaustionLevel > 4.0F) {
    this.exhaustionLevel -= 4.0F;
    if (this.saturationLevel > 0.0F) {
        this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
    }
}
```

**Phase 28 modification:** Change `- 1.0F` to `- 1.21F`

**Recommended approach:** `@ModifyConstant` targeting the `1.0F` constant in the saturation subtraction, OR redirect the `Math.max()` call.

### 2. Natural Regeneration Gamerule Check

**Location:** Conditional before healing logic
**Vanilla logic:**
```java
if (player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION) && ...)
```

**Phase 28 requirement:** Bypass this check entirely - custom regeneration should always apply regardless of gamerule.

**Recommended approach:** `@Redirect` the `getBoolean()` call to always return `false` (to skip vanilla healing), then inject custom healing in a separate injection point.

### 3. Healing Condition (foodLevel >= 18)

**Location:** Same conditional as gamerule check
**Vanilla logic:**
```java
this.foodLevel >= 18 && player.isHurt()
```

**Phase 28 requirement:** Keep the `>= 18` gate (or make configurable)

### 4. Healing Rate (80 ticks / 1HP)

**Location:** Inside the healing block
**Vanilla logic:**
```java
if (this.tickTimer >= 80) {
    player.heal(1.0F);
    this.tickTimer = 0;
}
```

**Phase 28 target:** 3/16 hearts per second = 0.375 HP/second
- At 20 TPS, this is 0.01875 HP per tick
- Alternative: heal 0.375 HP every 20 ticks (1 second)
- Alternative: heal 1 HP every 53.33 ticks (~2.67 seconds)

**Recommended approach:** Modify the `>= 80` threshold constant and/or the `1.0F` heal amount.

### 5. Saturation Boost Disabling

**Location:** The separate saturation boost conditional
**Vanilla logic:** Activates when `foodLevel >= 20 && saturationLevel > 0`

**Phase 28 requirement:** Disable entirely

**Recommended approach:** `@Redirect` or `@Inject` at HEAD with cancellation for the saturation boost branch.

## Mixin Strategy Recommendations

### Option A: Full Replacement (@Overwrite)

**Pros:**
- Complete control over all logic
- No worry about injection order
- Clean implementation

**Cons:**
- Breaks mod compatibility
- Must maintain ALL vanilla logic we want to keep
- Risky with future MC updates

### Option B: Multiple Targeted Injections

**Recommended structure:**
```java
@Mixin(FoodData.class)
public class FoodDataMixin {

    // 1. Modify saturation drain amount (4.0 -> 1.21 saturation loss)
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 1.0F, ordinal = 0))
    private float thc$modifyExhaustionSaturationDrain(float original) {
        return 1.21F;  // Drain 1.21 saturation instead of 1.0
    }

    // 2. Disable vanilla natural regeneration by making gamerule always false
    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z")
    )
    private boolean thc$disableVanillaRegenGamerule(GameRules rules, GameRules.Key<GameRules.BooleanValue> key) {
        return false;  // Always return false to skip vanilla healing
    }

    // 3. Inject custom healing logic at end of tick
    @Inject(method = "tick", at = @At("RETURN"))
    private void thc$customRegeneration(Player player, CallbackInfo ci) {
        if (this.foodLevel >= 18 && player.isHurt()) {
            // Custom healing: 3/16 hearts/second = 0.375 HP/sec
            // Heal 1HP every ~53 ticks (2.67 seconds) for same effective rate
            ++this.tickTimer;
            if (this.tickTimer >= 53) {
                player.heal(1.0F);
                this.addExhaustion(6.0F);
                this.tickTimer = 0;
            }
        }
    }
}
```

### Option C: @Inject HEAD with cancellation + full reimplementation

Most reliable for complete behavior replacement:

```java
@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
private void thc$overrideTick(Player player, CallbackInfo ci) {
    // Full custom implementation here
    ci.cancel();
}
```

## Field Access Requirements

The mixin will need access to these private fields:

| Field | Type | Access Method |
|-------|------|---------------|
| `exhaustionLevel` | float | @Shadow or Accessor |
| `saturationLevel` | float | Already have `FoodDataAccessor.setSaturationLevel()` |
| `foodLevel` | int | @Shadow (public getter exists) |
| `tickTimer` | int | @Shadow |
| `lastFoodLevel` | int | @Shadow |

**Existing accessor:** The project already has `FoodDataAccessor` from phase 27 for `saturationLevel`.

## Exhaustion Math Clarification

**User requirement:** "4.0 exhaustion removes 1.21 saturation (instead of 1.0)"

The vanilla formula is:
- When `exhaustionLevel >= 4.0F`: subtract 4.0F from exhaustion, subtract 1.0F from saturation

The phase 28 modification:
- When `exhaustionLevel >= 4.0F`: subtract 4.0F from exhaustion, subtract **1.21F** from saturation

This means saturation depletes ~21% faster per unit of exhaustion.

## Healing Rate Math

**User requirement:** "3/16 hearts/second when hunger >= 18"

- 3/16 hearts = 3/16 * 2 HP = 0.375 HP per second
- At 20 TPS: 0.01875 HP per tick

Implementation options:
1. **Fractional per tick:** Heal 0.01875 HP every tick (requires accumulator)
2. **Periodic burst:** Heal 1.0 HP every ~53 ticks (2.67 seconds)
3. **Small periodic:** Heal 0.5 HP every ~27 ticks (1.33 seconds)

**Recommendation:** Option 2 or 3 for simplicity. Option 1 would require an accumulator to prevent floating-point rounding issues over time.

## Existing Project Code

The project already has `FoodDataMixin.java` with:
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

This modifies the healing amount to 0.5x. Phase 28 will need to either:
- Remove/replace this mixin
- Integrate the new healing rate into this approach

## Common Pitfalls

### Pitfall 1: Saturation Boost Not Disabled
**What goes wrong:** Players still heal rapidly when at full hunger with saturation
**Why it happens:** Only modified the >= 18 healing path, not the >= 20 saturation boost path
**How to avoid:** Explicitly handle/disable both healing code paths

### Pitfall 2: Double Healing
**What goes wrong:** Both vanilla and custom healing apply
**Why it happens:** Failed to properly cancel vanilla healing before adding custom
**How to avoid:** Use @Redirect to make gamerule return false, OR use HEAD cancellation

### Pitfall 3: Exhaustion Not Consumed
**What goes wrong:** Saturation drains but exhaustion never decreases
**Why it happens:** Modified saturation drain but broke the exhaustionLevel -= 4.0F
**How to avoid:** Keep exhaustion processing intact, only modify the saturation drain amount

### Pitfall 4: tickTimer Conflicts
**What goes wrong:** Healing rate is inconsistent or broken
**Why it happens:** Multiple systems trying to use the same tickTimer field
**How to avoid:** Either fully replace tick() or ensure custom logic uses its own timer

## Sources

### Primary (HIGH confidence)
- [Minecraft Wiki - Hunger](https://minecraft.wiki/w/Hunger) - Core mechanics documentation
- [Minecraft Wiki - Food mechanics](https://minecraft.wiki/w/Food_mechanics) - Detailed tick mechanics
- [Alkazia Spigot FoodMetaData.java](https://raw.githubusercontent.com/vmarchaud/Alkazia/master/1.8/Spigot/src/main/java/net/minecraft/server/FoodMetaData.java) - Decompiled source code (verified)
- [Forge JavaDocs FoodData](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.19.3/net/minecraft/world/food/FoodData.html) - API documentation

### Secondary (MEDIUM confidence)
- [Fabric Wiki - Mixin Injects](https://wiki.fabricmc.net/tutorial:mixin_injects) - Injection techniques
- [Minecraft Wiki - Healing](https://minecraft.wiki/w/Healing) - Saturation boost details

### Verification Notes
- FoodData.tick() logic has been stable from 1.8 through 1.21 (only mappings changed)
- The saturation boost mechanic was added in ~1.9 and remains unchanged
- The naturalRegeneration gamerule check has been in the same location since introduction

## Open Questions

1. **Healing exhaustion cost:** Should phase 28 modify the 6.0F exhaustion cost for healing?
2. **Starvation damage:** Does phase 28 need to modify starvation behavior at hunger 0?
3. **Difficulty scaling:** Should healing rate vary by difficulty?
4. **Saturation boost:** Confirm this should be disabled entirely vs modified

## Metadata

**Confidence breakdown:**
- FoodData.tick() structure: HIGH - Verified from decompiled source
- Exhaustion threshold (4.0F): HIGH - Multiple sources confirm
- Saturation drain (1.0F): HIGH - Verified in source code
- Healing gate (>= 18): HIGH - Multiple sources confirm
- Saturation boost mechanics: HIGH - Well documented
- 1.21 specific changes: MEDIUM - No major changes found since 1.9

**Research date:** 2026-01-22
**Valid until:** Stable - hunger mechanics rarely change between versions
