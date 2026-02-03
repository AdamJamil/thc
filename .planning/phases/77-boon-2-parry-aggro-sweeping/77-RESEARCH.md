# Phase 77: Boon 2 - Parry Aggro & Sweeping - Research

**Researched:** 2026-02-03
**Domain:** Combat mechanics (threat propagation, sweeping edge)
**Confidence:** HIGH

## Summary

This phase implements two combat capabilities gated behind Bastion class at Stage 3+:
1. **Parry threat propagation** - When Bastion successfully parries, adds threat to all mobs within 3 blocks
2. **Sweeping edge enablement** - Enables vanilla sweeping edge behavior for Bastion melee attacks

The implementation is straightforward using existing infrastructure. Both features share the same gate condition (Bastion class + boon level >= 3), and both modify existing mixin code paths. The parry threat propagation hooks into the existing `thc$stunNearby` call site in LivingEntityMixin, while sweeping edge modifies the existing `thc$disableSweepAttack` redirect in PlayerAttackMixin.

**Primary recommendation:** Implement both gates within existing mixins, extracting a shared utility method `BoonGate.hasStage3Boon(ServerPlayer)` to avoid duplicating the class + stage check logic.

## Standard Stack

No new libraries or dependencies required. This phase uses existing infrastructure:

### Core
| Component | Location | Purpose |
|-----------|----------|---------|
| ThreatManager | `thc.threat.ThreatManager` | Add threat to mobs |
| ClassManager | `thc.playerclass.ClassManager` | Check player class |
| StageManager | `thc.stage.StageManager` | Check boon level |
| LivingEntityMixin | `thc.mixin.LivingEntityMixin` | Parry handling |
| PlayerAttackMixin | `thc.mixin.PlayerAttackMixin` | Sweeping edge disable |

### Supporting
| Pattern | Example | When to Use |
|---------|---------|-------------|
| Boon gate check | Phase 76 buckler gate | Any class+stage gated feature |
| Threat addition | `ThreatManager.addThreat(mob, playerUuid, amount)` | Aggro redirection |
| Entity iteration | `level.getEntitiesOfClass(Mob.class, bbox, predicate)` | Finding nearby mobs |

## Architecture Patterns

### Recommended Approach: Shared Gate Utility

Extract the common class + stage check into a utility to avoid duplication:

```java
// New file: src/main/java/thc/boon/BoonGate.java
public final class BoonGate {
    private BoonGate() {}

    /**
     * Check if player has Stage 3+ boon (Bastion class with boon level >= 3).
     * Used for: parry threat propagation, sweeping edge.
     */
    public static boolean hasStage3Boon(ServerPlayer player) {
        PlayerClass playerClass = ClassManager.getClass(player);
        if (playerClass != PlayerClass.BASTION) {  // Or TANK if Phase 75 not complete
            return false;
        }
        return StageManager.getBoonLevel(player) >= 3;
    }
}
```

### Pattern 1: Parry Threat Propagation

**What:** After successful parry, add threat to nearby mobs so they target the Bastion.

**Where to modify:** `LivingEntityMixin.thc$stunNearby()` call site (line 85) or create a separate method called alongside.

**Implementation pattern:**
```java
// In LivingEntityMixin, after parry succeeds (line 85):
if (parry) {
    // ... existing poise logic ...
    thc$stunNearby(level, player, stats);

    // NEW: Propagate threat if Bastion Stage 3+
    if (player instanceof ServerPlayer serverPlayer) {
        if (BoonGate.hasStage3Boon(serverPlayer)) {
            thc$propagateParryThreat(level, serverPlayer);
        }
    }

    level.playSound(...);
}

@Unique
private static void thc$propagateParryThreat(ServerLevel level, ServerPlayer player) {
    // Use same 3-block radius as stunNearby
    for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(3.0D),
        entity -> entity.getType().getCategory() == MobCategory.MONSTER)) {
        ThreatManager.addThreat(mob, player.getUUID(), 10.0);  // Same as arrow hit threat
    }
}
```

### Pattern 2: Conditional Sweeping Edge

**What:** Enable vanilla sweeping edge for Bastion at Stage 3+, keep disabled for everyone else.

**Where to modify:** `PlayerAttackMixin.thc$disableSweepAttack()`

**Implementation pattern:**
```java
@Redirect(
    method = "attack",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;isSweepAttack(ZZZ)Z"
    )
)
private boolean thc$disableSweepAttack(Player instance, boolean bl, boolean bl2, boolean bl3) {
    // Allow sweeping for Bastion Stage 3+
    if (instance instanceof ServerPlayer serverPlayer) {
        if (BoonGate.hasStage3Boon(serverPlayer)) {
            return instance.isSweepAttack(bl, bl2, bl3);  // Call original method
        }
    }
    return false;  // Default: sweeping disabled
}
```

**Note:** The redirect intercepts the call and can either return false (disabled) or call the original method to get vanilla behavior. This requires accessing the original method, which a redirect can do.

### Anti-Patterns to Avoid

- **Duplicating gate logic:** Don't copy-paste the class + stage check. Use shared utility.
- **Modifying vanilla sweep damage values:** Requirements say to use vanilla behavior. Don't scale damage.
- **Adding threat to the parried attacker only:** Requirements say "nearby mobs", not just the attacker.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Finding nearby mobs | Custom entity search | `level.getEntitiesOfClass(Mob.class, bbox.inflate(radius), predicate)` | Existing pattern in `thc$stunNearby` |
| Threat addition | Direct attachment manipulation | `ThreatManager.addThreat()` | Handles null checks, initialization |
| Class check | Manual attachment read | `ClassManager.getClass()` | Type-safe enum conversion |
| Stage check | Manual attachment read | `StageManager.getBoonLevel()` | Handles null with default 0 |

## Common Pitfalls

### Pitfall 1: Wrong Mob Filter for Threat Propagation

**What goes wrong:** Adding threat to neutral mobs (like wolves, iron golems) that shouldn't be affected.

**Why it happens:** Using a broad filter in `getEntitiesOfClass`.

**How to avoid:** Use `MobCategory.MONSTER` filter, same as `thc$stunNearby`:
```java
entity -> entity.getType().getCategory() == MobCategory.MONSTER
```

**Warning signs:** Wolves/iron golems start targeting the parrying player.

### Pitfall 2: Redirect Not Calling Original Method

**What goes wrong:** The @Redirect completely replaces the method call, so you can't easily "call through" to the original.

**Why it happens:** Redirects intercept the call, not wrap it.

**How to avoid:** For enabling sweeping edge conditionally, we need to invoke `isSweepAttack` on the instance. Fortunately, the redirect receives the `instance` parameter (the Player), so we can call `instance.isSweepAttack(bl, bl2, bl3)` directly.

**Note:** This works because `isSweepAttack` is a private method but the redirect happens at the call site within the same class, so access is valid.

### Pitfall 3: Client-Side Gate Checks

**What goes wrong:** Checking boon gate on client side throws errors or has no effect.

**Why it happens:** `ClassManager.getClass()` and `StageManager.getBoonLevel()` use attachments that may not be synced to client.

**How to avoid:** Both features are server-side:
- `LivingEntityMixin.hurtServer` only runs server-side
- `Player.attack()` is called on both sides but the sweep damage application is server-only

The checks should still verify `instanceof ServerPlayer` before casting.

### Pitfall 4: Threat Amount Too Low

**What goes wrong:** Mobs don't switch target to the Bastion because threat is below threshold.

**Why it happens:** MIN_THREAT is 5.0 in ThreatTargetGoal.

**How to avoid:** Use 10.0 threat (same as arrow hit bonus), which is above the 5.0 threshold and meaningful for aggro control.

## Code Examples

### Existing Threat Addition Pattern

```java
// From AbstractArrowMixin.java line 86
ThreatManager.addThreat(mob, player.getUUID(), 10.0);
```

### Existing 3-Block Radius Pattern

```java
// From LivingEntityMixin.java line 198-199
for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(3.0D),
    entity -> entity.getType().getCategory() == MobCategory.MONSTER)) {
```

### Existing Boon Gate Pattern (from Phase 76 research)

```java
// Pattern from Phase 76 BucklerItem gate
if (player instanceof ServerPlayer serverPlayer) {
    PlayerClass playerClass = ClassManager.getClass(serverPlayer);
    int boonLevel = StageManager.getBoonLevel(serverPlayer);
    if (playerClass != PlayerClass.TANK || boonLevel < 2) {  // Stage 2+
        // ... reject
    }
}
```

### Vanilla isSweepAttack Conditions

```java
// From Player.java line 1036-1046
private boolean isSweepAttack(boolean bl, boolean bl2, boolean bl3) {
    if (bl && !bl2 && !bl3 && this.onGround()) {  // bl=charged, bl2=??, bl3=sprint
        double d = this.getKnownMovement().horizontalDistanceSqr();
        double e = this.getSpeed() * 2.5;
        if (d < Mth.square(e)) {
            return this.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS);
        }
    }
    return false;
}
```

Sweeping requires: charged attack, on ground, not sprinting, low movement, holding a sword.

## Threat Value Analysis

**Existing threat values in codebase:**

| Source | Amount | Reference |
|--------|--------|-----------|
| Arrow hit bonus | 10.0 | AbstractArrowMixin.java:86 |
| Snowball/projectile hit | 10.0 | ProjectileEntityMixin.java:49 |
| Proximity threat (damage) | ceil(damage/4) | MobDamageThreatMixin.java:50 |
| Minimum for targeting | 5.0 | ThreatTargetGoal.java:22 |
| Decay rate | 1.0/second | ThreatManager.java:82 |

**Recommendation for parry threat:** Use 10.0, same as arrow/projectile hit. This is:
- Above the 5.0 minimum threshold (immediate aggro)
- Consistent with other "instant aggro" events
- Takes 10 seconds to decay fully
- Meaningful for tanking (mobs will switch to Bastion)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Sweeping always disabled | Conditionally enabled for Bastion | This phase | Bastion gets cleave attacks |
| Parry only stuns | Parry stuns + propagates threat | This phase | Bastion actively tanks |

**Existing behavior preserved:**
- Non-Bastion: sweeping disabled, no parry threat
- Bastion Stage 1-2: sweeping disabled, no parry threat
- Parry stun: always happens (not gated)

## Open Questions

### Resolved

1. **Q: Should threat go to all mobs or just the attacker?**
   A: All mobs in 3-block radius per CONTEXT.md "propagates threat to nearby mobs"

2. **Q: What threat amount?**
   A: 10.0, consistent with arrow hit and above MIN_THREAT threshold

3. **Q: Should we extract a utility for the gate check?**
   A: Yes, recommended to avoid duplication since both features use same gate

### None Remaining

All implementation details are resolved by existing patterns and explicit decisions in CONTEXT.md.

## Files to Modify

| File | Change |
|------|--------|
| `src/main/java/thc/boon/BoonGate.java` | NEW: Shared utility for Stage 3+ gate check |
| `src/main/java/thc/mixin/LivingEntityMixin.java` | Add threat propagation after parry |
| `src/main/java/thc/mixin/PlayerAttackMixin.java` | Conditionally enable sweeping edge |

## Testing Strategy

### Manual Tests

1. **Non-Bastion parry does NOT propagate threat**
   - Select any non-Bastion class
   - Parry an attack (successful parry sound plays)
   - Nearby mobs should NOT switch target
   - Expect: Stun happens, but no aggro redirect

2. **Bastion Stage 2 parry does NOT propagate threat**
   - Select Bastion, advance to Stage 2
   - Parry an attack
   - Expect: Stun happens, but no aggro redirect

3. **Bastion Stage 3+ parry DOES propagate threat**
   - Select Bastion, advance to Stage 3
   - Parry an attack with multiple mobs nearby
   - Expect: Nearby mobs switch target to Bastion

4. **Non-Bastion cannot sweep**
   - Select any non-Bastion class
   - Full charge attack with sword on ground
   - Expect: No sweep particles, no AoE damage

5. **Bastion Stage 2 cannot sweep**
   - Select Bastion, advance to Stage 2
   - Full charge attack with sword on ground
   - Expect: No sweep particles, no AoE damage

6. **Bastion Stage 3+ CAN sweep**
   - Select Bastion, advance to Stage 3
   - Full charge attack with sword on ground near multiple mobs
   - Expect: Sweep particles, AoE damage to nearby mobs

## Sources

### Primary (HIGH confidence)
- Existing codebase patterns (ThreatManager, ClassManager, StageManager)
- LivingEntityMixin.java - parry handling with stunNearby
- PlayerAttackMixin.java - sweeping edge disable redirect
- Phase 76 RESEARCH.md - boon gate pattern

### Secondary (MEDIUM confidence)
- Vanilla Player.java decompilation - isSweepAttack conditions

## Metadata

**Confidence breakdown:**
- Threat propagation: HIGH - exact pattern exists in codebase (arrow hit)
- Sweeping edge gate: HIGH - redirect pattern exists, vanilla method callable
- Gate utility: HIGH - straightforward extraction of existing pattern

**Research date:** 2026-02-03
**Valid until:** No expiration (uses internal codebase patterns)

---

*Research confidence: HIGH - all information sourced from existing codebase patterns*
