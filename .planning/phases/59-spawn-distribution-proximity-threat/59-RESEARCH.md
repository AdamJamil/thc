# Phase 59: Spawn Distribution & Proximity Threat - Research

**Researched:** 2026-01-28
**Domain:** Minecraft 1.21.11 regional spawn weight modification, proximity-based threat propagation
**Confidence:** HIGH

## Summary

Phase 59 implements two independent subsystems: (1) rebalancing the deepslate region (OW_LOWER_CAVE) spawn distribution to include wither skeletons, and (2) adding proximity threat propagation when dealing damage. Both subsystems build on well-established THC infrastructure - the regional spawn system (Phase 42) and the threat system (v1.3).

The spawn distribution change is a trivial weight modification in `SpawnDistributions.java`. The proximity threat change requires modifying `MobDamageThreatMixin.java` to add proximity-based threat propagation centered on the PLAYER (not the target), with explicit exclusion of the direct damage target from receiving this bonus.

**Primary recommendation:** Modify existing `SpawnDistributions.java` OW_LOWER_CAVE table (wither skeleton 15%, pillager 25%->20%, vanilla 45%->35%). Add new proximity threat injection in `MobDamageThreatMixin.java` using `ceil(damage/4)` formula and 5-block radius from player position.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.119.2+ | Attachment API for threat storage | Already used in THC for MOB_THREAT attachment |
| Mixin | 0.8.5+ | Bytecode injection framework | Required for damage interception |
| Minecraft | 1.21.11 | Target game version | Project constraint |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| AABB | Minecraft | Area-based entity queries | Finding mobs within 5 blocks of player |
| EntityType | Minecraft | Wither skeleton spawn creation | Custom spawn distribution |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Modify existing mixin | New separate mixin | Single mixin is cleaner, all threat propagation in one place |
| Math.ceil() | Inline rounding | Math.ceil() is clearer for "ceil(X/4)" requirement |
| Player-centered AABB | Target-centered AABB | Player-centered matches spec "within 5 blocks of player" |

**Installation:**
Already in project - no additional dependencies needed.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/thc/
├── spawn/
│   └── SpawnDistributions.java    # MODIFY: add wither skeleton, adjust weights
├── mixin/
│   └── MobDamageThreatMixin.java  # MODIFY: add proximity threat propagation
└── threat/
    └── ThreatManager.java         # EXISTING: no changes needed
```

### Pattern 1: Spawn Distribution Weight Modification

**What:** Adjust existing spawn distribution table weights to add new mob type and rebalance.

**When to use:** Adding wither skeletons to deepslate region.

**Example:**
```java
// Source: Existing SpawnDistributions.java pattern
// Current OW_LOWER_CAVE: 8% blaze, 8% breeze, 12% vindicator, 25% pillager, 2% evoker, 45% vanilla
// New OW_LOWER_CAVE: 15% wither skeleton, 8% blaze, 8% breeze, 12% vindicator, 20% pillager, 2% evoker, 35% vanilla

List<WeightedEntry> lowerCave = new ArrayList<>();
lowerCave.add(new WeightedEntry(EntityType.WITHER_SKELETON, 15));  // NEW: 15%
lowerCave.add(new WeightedEntry(EntityType.BLAZE, 8));
lowerCave.add(new WeightedEntry(EntityType.BREEZE, 8));
lowerCave.add(new WeightedEntry(EntityType.VINDICATOR, 12));
lowerCave.add(new WeightedEntry(EntityType.PILLAGER, "MELEE", 20)); // CHANGED: 25% -> 20%
lowerCave.add(new WeightedEntry(EntityType.EVOKER, 2));
lowerCave.add(new WeightedEntry(null, null, 35));                   // CHANGED: 45% -> 35%
TABLES.put("OW_LOWER_CAVE", lowerCave);
// Total: 15 + 8 + 8 + 12 + 20 + 2 + 35 = 100%
```

**Key insight:** Wither skeletons bypass vanilla fortress requirement just like blazes/breezes already bypass vanilla spawn conditions. This is by design - custom spawns in THC ignore vanilla spawn restrictions.

### Pattern 2: Proximity Threat Propagation (Player-Centered)

**What:** When player deals damage, add threat to nearby mobs based on damage dealt, centered on PLAYER position.

**When to use:** Damage propagation for threat aggro system.

**Example:**
```java
// Source: MobDamageThreatMixin.java pattern with modifications
// THRT-01: Dealing X damage adds ceil(X/4) threat to mobs within 5 blocks of PLAYER
// THRT-02: Direct damage target does NOT receive the bonus

@Inject(method = "hurtServer", at = @At("TAIL"))
private void thc$propagateProximityThreat(
    ServerLevel level,
    DamageSource source,
    float amount,
    CallbackInfoReturnable<Boolean> cir
) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (!(self instanceof Mob damagedMob)) {
        return;
    }

    if (!cir.getReturnValue() || amount <= 0) {
        return;
    }

    Entity attacker = source.getEntity();
    if (!(attacker instanceof ServerPlayer player)) {
        return;
    }

    // Calculate proximity threat: ceil(damage / 4)
    double proximityThreat = Math.ceil(amount / 4.0);

    // Find mobs within 5 blocks of PLAYER (not target)
    AABB area = player.getBoundingBox().inflate(5.0);

    for (Mob nearby : level.getEntitiesOfClass(Mob.class, area, this::thc$isHostileOrNeutral)) {
        // THRT-02: Skip the direct damage target
        if (nearby == damagedMob) {
            continue;
        }
        ThreatManager.addThreat(nearby, player.getUUID(), proximityThreat);
    }
}
```

### Pattern 3: Existing Threat vs Proximity Threat Separation

**What:** Keep existing damage-based threat (damage amount to hit mob) separate from new proximity threat (ceil(X/4) to nearby mobs).

**Current behavior:**
- MobDamageThreatMixin adds `damage amount` threat to all mobs within 15 blocks of the TARGET

**New behavior:**
- Remove the 15-block area threat propagation
- Keep direct damage threat to the hit mob (damage amount)
- Add proximity threat (ceil(X/4)) to mobs within 5 blocks of PLAYER
- Direct target excluded from proximity bonus

**Implementation approach:**
```java
// OLD CODE (remove):
AABB area = mob.getBoundingBox().inflate(THC_THREAT_RADIUS); // 15 blocks from target
for (Mob nearby : level.getEntitiesOfClass(Mob.class, area, ...)) {
    ThreatManager.addThreat(nearby, player.getUUID(), amount);
}

// NEW CODE:
// 1. Direct threat to hit mob (unchanged - happens via damage)
// Note: The existing ThreatTargetGoal handles threat-based targeting
// Direct damage already creates threat via the damage event itself

// 2. Proximity threat to nearby mobs (5 blocks from player)
AABB area = player.getBoundingBox().inflate(5.0);
double proximityThreat = Math.ceil(amount / 4.0);
for (Mob nearby : level.getEntitiesOfClass(Mob.class, area, ...)) {
    if (nearby != damagedMob) {  // Exclude direct target
        ThreatManager.addThreat(nearby, player.getUUID(), proximityThreat);
    }
}
```

### Anti-Patterns to Avoid

- **Target-centered proximity calculation:** The spec says "within 5 blocks of player", not "within 5 blocks of target". Use player position.

- **Adding proximity threat to direct target:** THRT-02 explicitly states direct target does NOT receive the proximity bonus. The direct target gets threat from the damage itself, not the proximity system.

- **Keeping the old 15-block propagation:** The old system adds full damage threat to all mobs in 15 blocks. Remove this entirely - it's being replaced by the proximity system.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Ceiling division | Manual integer math | Math.ceil(amount / 4.0) | Handles edge cases (0 damage, fractional) |
| Area entity query | Manual distance checks | AABB.inflate() + getEntitiesOfClass() | Optimized spatial query |
| Mob category filtering | Custom instanceof checks | Existing thc$isHostileOrNeutral() | Already handles MONSTER + CREATURE categories |
| Threat storage | Custom data structures | ThreatManager + MOB_THREAT attachment | Already implemented, session-scoped |

**Key insight:** Both subsystems leverage existing infrastructure. No new classes or utilities needed.

## Common Pitfalls

### Pitfall 1: Using Target Position Instead of Player Position

**What goes wrong:** Proximity threat calculates from damaged mob's position instead of attacking player's position.

**Why it happens:** The existing MobDamageThreatMixin uses `mob.getBoundingBox().inflate()` - easy to copy this pattern.

**How to avoid:** Use `player.getBoundingBox().inflate(5.0)` explicitly. The spec says "mobs within 5 blocks of player".

**Warning signs:**
- Proximity threat affects mobs near the target, not near the attacker
- Ranged attacks affect mobs far from player

### Pitfall 2: Double-Threatening the Direct Target

**What goes wrong:** Direct damage target receives both direct damage threat AND proximity threat.

**Why it happens:** Not filtering the damaged mob from the proximity query.

**How to avoid:** Explicit check: `if (nearby != damagedMob) continue;`

**Warning signs:**
- Direct target gets more threat than expected
- Multiplayer threat calculations seem wrong

### Pitfall 3: Wither Skeleton Spawn Validation

**What goes wrong:** Adding wither skeletons to spawn table but they fail to spawn due to vanilla conditions.

**Why it happens:** Forgetting that THC custom spawns bypass vanilla conditions.

**How to avoid:** Wither skeletons in THC spawn via `EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.NATURAL)` - same pattern as blazes/breezes. No fortress requirement.

**Warning signs:**
- Wither skeletons never appearing in deepslate
- Spawn logs showing creation but no entities in world

### Pitfall 4: Weight Sum Validation

**What goes wrong:** New weights don't sum to 100%, causing spawn system assertion failure.

**Why it happens:** Math error when adjusting multiple weights simultaneously.

**How to avoid:** The existing static initializer validates: `if (total != 100) throw IllegalStateException`. Trust it. Calculate: 15 + 8 + 8 + 12 + 20 + 2 + 35 = 100.

**Warning signs:**
- Server crash on startup with "Distribution sums to X, expected 100"

## Code Examples

Verified patterns from existing codebase:

### Wither Skeleton Spawn Entry
```java
// Source: SpawnDistributions.java pattern
// Wither skeletons are simple mobs with no variants (like blaze, breeze)
lowerCave.add(new WeightedEntry(EntityType.WITHER_SKELETON, 15));
```

### Proximity Threat Calculation
```java
// Source: Math.ceil() for ceiling behavior per THRT-01
// Example: 7 damage -> ceil(7/4) = ceil(1.75) = 2 threat
// Example: 4 damage -> ceil(4/4) = ceil(1.0) = 1 threat
// Example: 1 damage -> ceil(1/4) = ceil(0.25) = 1 threat
double proximityThreat = Math.ceil(amount / 4.0);
```

### Player-Centered Area Query
```java
// Source: AABB pattern from existing THC code
AABB playerArea = player.getBoundingBox().inflate(5.0);
List<Mob> nearbyMobs = level.getEntitiesOfClass(
    Mob.class,
    playerArea,
    MobDamageThreatMixin::thc$isHostileOrNeutral
);
```

### Mob Filtering (Existing)
```java
// Source: MobDamageThreatMixin.java - reuse existing filter
@Unique
private static boolean thc$isHostileOrNeutral(Mob mob) {
    MobCategory category = mob.getType().getCategory();
    return category == MobCategory.MONSTER || category == MobCategory.CREATURE;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 15-block target-centered threat | 5-block player-centered proximity | Phase 59 (v2.6) | More tactical - only mobs near you get alerted |
| Full damage as threat to area | ceil(X/4) threat to area | Phase 59 (v2.6) | Reduced area threat, encourages focused damage |
| No wither skeletons in Overworld | 15% in deepslate region | Phase 59 (v2.6) | New challenge in deep caves |

**Deprecated/outdated:**
- **15-block threat radius:** Being replaced with 5-block player-centered proximity
- **Full damage threat propagation:** Being replaced with ceil(X/4) formula

## Open Questions

Things that couldn't be fully resolved:

1. **Wither Skeleton Equipment Retention**
   - What we know: THC custom spawns don't call populateDefaultEquipmentSlots for simple mobs
   - What's unclear: Whether wither skeletons need special handling for stone swords
   - Recommendation: Wither skeletons get default equipment via finalizeSpawn() - should work automatically

2. **Interaction with Arrow Threat**
   - What we know: ProjectileEntityMixin adds +10 threat to struck mob, MobDamageThreatMixin adds damage-based threat
   - What's unclear: Whether proximity threat should also apply to arrow hits
   - Recommendation: Yes - MobDamageThreatMixin triggers on all damage including arrows, so proximity threat will apply

## Sources

### Primary (HIGH confidence)
- Existing THC codebase:
  - `/mnt/c/home/code/thc/src/main/java/thc/spawn/SpawnDistributions.java` - Spawn weight tables
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/MobDamageThreatMixin.java` - Damage threat propagation
  - `/mnt/c/home/code/thc/src/main/java/thc/threat/ThreatManager.java` - Threat CRUD operations
  - `/mnt/c/home/code/thc/src/main/java/thc/spawn/RegionDetector.java` - Region detection
  - `/mnt/c/home/code/thc/src/main/java/thc/mixin/SpawnReplacementMixin.java` - Custom spawn creation
- [Minecraft Wiki - Wither Skeleton](https://minecraft.wiki/w/Wither_Skeleton) - Entity type and properties

### Secondary (MEDIUM confidence)
- Phase 42 Research (`.planning/phases/42-regional-spawn-system/42-RESEARCH.md`) - Spawn system architecture

### Tertiary (LOW confidence)
- None - all findings verified against existing codebase

## Metadata

**Confidence breakdown:**
- Spawn distribution modification: HIGH - Trivial weight change to existing table
- Proximity threat implementation: HIGH - Clear spec, existing patterns to follow
- Wither skeleton spawning: HIGH - Same pattern as blaze/breeze, already proven

**Research date:** 2026-01-28
**Valid until:** Minecraft 1.21.x (method signatures stable within minor versions)

**Notes for planner:**
1. Both requirements (spawn + threat) are independent - can be implemented in any order
2. Spawn change is a single-file, ~5-line modification to SpawnDistributions.java
3. Threat change requires rewriting the area query logic in MobDamageThreatMixin.java
4. No new files, classes, or dependencies needed
5. Existing ThreatManager.addThreat() handles all threat storage/retrieval
6. Test by dealing damage and verifying nearby (but not targeted) mobs gain threat
