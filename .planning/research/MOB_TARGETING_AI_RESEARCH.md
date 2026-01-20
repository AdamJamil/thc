# Minecraft Fabric 1.21.4+ Mob Targeting AI Research

**Researched:** 2026-01-19
**Minecraft Version:** 1.21.11 (Fabric with Mojang mappings)
**Confidence:** HIGH (verified with official Yarn/Mojang API docs and Minecraft Wiki)

## Executive Summary

Minecraft uses two distinct AI systems for mobs: the **GoalSelector** system (used by most mobs) and the **Brain** system (used by complex mobs like Villagers, Piglins, Wardens). For your threat system targeting hostile/neutral mobs, you will primarily work with the GoalSelector system.

**Primary recommendation:** Create a custom `ThreatTargetGoal` extending `TargetGoal` (Mojang) / `TrackTargetGoal` (Yarn), inject it at priority 1 (highest) into mob `targetSelector` via mixin, and intercept `setTarget()` to enforce threat-based targeting.

---

## 1. Goal vs Brain Systems

### GoalSelector System (Most Mobs)

**Used by:** Zombies, Skeletons, Creepers, Spiders, Endermen, Iron Golems, Wolves, and most other mobs.

The GoalSelector manages competing goals with a priority system:
- **Lower priority number = higher priority** (priority 1 beats priority 2)
- Goals can run simultaneously if they use different `Goal.Control` flags
- A running goal is replaced by a lower-priority-number goal when `canStart()` returns true
- Same-priority goals cannot replace each other; order of addition breaks ties

**Two separate selectors per mob:**
- `goalSelector` - Movement, actions (wandering, attacking, fleeing)
- `targetSelector` - Target acquisition (who to attack)

**Key Mojang Mappings (1.21.11):**
```java
// In Mob class
protected GoalSelector goalSelector;     // Movement/action goals
protected GoalSelector targetSelector;   // Target selection goals

// GoalSelector methods
void add(int priority, Goal goal);
void remove(Goal goal);
void clear();
Set<WrappedGoal> getAvailableGoals();    // All goals
Stream<WrappedGoal> getRunningGoals();   // Currently active
```

### Brain System (Complex Mobs)

**Used by:** Villager, Piglin, Axolotl, Goat, Tadpole, Frog, Warden, Camel, Allay, Sniffer, Breeze

The Brain system uses:
- **Sensors** - Detect conditions (nearest player, in water, etc.)
- **Memories** - Store data (visible mobs, job site location, cooldowns)
- **Tasks/Activities** - Behaviors triggered by sensor data and memories

**For your threat system:** You do NOT need to handle Brain mobs separately. Hostile mobs (zombies, skeletons, etc.) use GoalSelector. Brain mobs that attack (Piglin, Warden) have specialized targeting you would need separate handling for.

**Confidence:** HIGH - Verified with [Minecraft Wiki - Mob AI](https://minecraft.wiki/w/Mob_AI)

---

## 2. How setTarget() Works

### Mob.setTarget(LivingEntity target)

**Mojang mapping:** `Mob.setTarget(@Nullable LivingEntity target)`
**Yarn mapping:** `MobEntity.setTarget(@Nullable LivingEntity target)`

**What it does:**
1. Sets the mob's current attack objective
2. Stores the target in an internal field
3. For Brain mobs, also updates the `ATTACK_TARGET` memory
4. Does NOT directly cause attacking - attack goals check `getTarget()` each tick

**Does it persist?**
- The target persists until:
  - `setTarget(null)` is called
  - The target dies or becomes invalid
  - A targeting goal clears it (e.g., target out of range)
  - The mob dies or despawns

**Can goals override it?**
- YES. Targeting goals call `setTarget()` when they find a new target
- Priority matters: higher-priority (lower number) targeting goals run first
- `NearestAttackableTargetGoal` (Mojang) / `ActiveTargetGoal` (Yarn) calls `setTarget()` when it finds a closer/valid target
- `HurtByTargetGoal` (Mojang) / `RevengeGoal` (Yarn) calls `setTarget()` when the mob is hurt

**Key insight:** `setTarget()` is just a setter. The actual targeting behavior comes from goals that call it.

**Confidence:** HIGH - Verified with [Yarn MobEntity 1.21.6 API](https://maven.fabricmc.net/docs/yarn-1.21.6+build.1/net/minecraft/entity/mob/MobEntity.html)

---

## 3. NearestAttackableTargetGoal / ActiveTargetGoal

**Mojang mapping:** `NearestAttackableTargetGoal<T extends LivingEntity>`
**Yarn mapping:** `ActiveTargetGoal<T extends LivingEntity>`
**Package:** `net.minecraft.world.entity.ai.goal.target` (Mojang)

### Class Hierarchy
```
Goal
  -> TargetGoal (Mojang) / TrackTargetGoal (Yarn)
       -> NearestAttackableTargetGoal / ActiveTargetGoal
       -> HurtByTargetGoal / RevengeGoal
       -> OwnerHurtByTargetGoal / TrackOwnerAttackerGoal
       -> OwnerHurtTargetGoal
       -> DefendVillageTargetGoal
```

### How It Works

**Constructor (full form):**
```java
NearestAttackableTargetGoal(
    Mob mob,                        // The mob owning this goal
    Class<T> targetClass,           // Class of entities to target (e.g., Player.class)
    int reciprocalChance,           // 1/N chance to search each tick when idle
    boolean checkVisibility,        // Require line of sight
    boolean checkCanNavigate,       // Require pathfinding possible
    @Nullable Predicate<LivingEntity> targetPredicate  // Additional filter
)
```

**Lifecycle:**
1. `canStart()` - Returns true if:
   - Random chance passes (reciprocalChance)
   - `findTarget()` finds a valid entity
2. `start()` - Calls `mob.setTarget(foundTarget)`
3. `shouldContinue()` - Checks if target is still valid
4. `stop()` - Clears target (calls `mob.setTarget(null)`)

**Target Selection (`findTarget()`):**
1. Gets search area via `getSearchBox(followRange)`
2. Queries world for entities of `targetClass` in the box
3. Filters by `TargetingConditions`:
   - Line of sight (if checkVisibility)
   - Distance within follow range
   - Custom predicate
4. Returns nearest valid target

**Default Priorities (Zombie example):**
| Priority | Goal | Description |
|----------|------|-------------|
| 1 | HurtByTargetGoal | Target whoever hurt me |
| 2 | NearestAttackableTargetGoal<Player> | Target nearest player |
| 3 | NearestAttackableTargetGoal<Villager> | Target nearest villager |
| 4 | NearestAttackableTargetGoal<IronGolem> | Target nearest iron golem |

**Confidence:** HIGH - Verified with [Yarn ActiveTargetGoal 1.21.4 API](https://maven.fabricmc.net/docs/yarn-1.21.4+build.1/net/minecraft/entity/ai/goal/ActiveTargetGoal.html)

---

## 4. Custom Targeting Approaches

### Approach A: Custom Goal with Higher Priority (RECOMMENDED)

**Strategy:** Add a `ThreatTargetGoal` at priority 0 (or 1) to override default targeting.

**Implementation Pattern:**
```java
public class ThreatTargetGoal extends TargetGoal {
    private final Predicate<LivingEntity> targetPredicate;
    private LivingEntity target;

    public ThreatTargetGoal(Mob mob) {
        super(mob, false);  // checkVisibility = false for threat-based
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Check if any player has threat >= 5
        LivingEntity highestThreat = findHighestThreatTarget();
        if (highestThreat != null) {
            this.target = highestThreat;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
    }

    @Override
    public boolean canContinueToUse() {
        // Continue if target still valid and has highest threat
        // Allow switch on revenge or strictly higher threat
        return shouldContinueTargeting(this.target);
    }

    @Override
    public void stop() {
        this.target = null;
        // Don't clear mob target - let lower priority goals take over
    }
}
```

**Injection via Mixin:**
```java
@Mixin(Zombie.class)  // Or specific mob classes
public abstract class ZombieMixin extends Monster {

    protected ZombieMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$addThreatTargeting(CallbackInfo ci) {
        // Priority 0 = highest, will override default targeting
        this.targetSelector.addGoal(0, new ThreatTargetGoal(this));
    }
}
```

**Pros:**
- Clean, follows vanilla patterns
- Easy to understand and debug
- Doesn't break other mods' targeting
- Can coexist with vanilla goals

**Cons:**
- Need to add to each mob type (or use abstract mixin on Monster)
- Doesn't affect Brain-based mobs

### Approach B: Mixin Intercept setTarget()

**Strategy:** Intercept all `setTarget()` calls to enforce threat-based targeting.

```java
@Mixin(Mob.class)
public abstract class MobTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void thc$interceptSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        // Get current highest threat target
        LivingEntity threatTarget = ThreatManager.getHighestThreatTarget(self);

        if (threatTarget != null && target != threatTarget) {
            // Only allow if:
            // 1. This is a revenge switch (mob was just hurt by target)
            // 2. Target has strictly higher threat
            if (!isRevengeSwitch(self, target) && !hasHigherThreat(target, threatTarget)) {
                ci.cancel();  // Reject the target change
                return;
            }
        }
    }

    @Unique
    private boolean isRevengeSwitch(Mob mob, LivingEntity newTarget) {
        return mob.getLastHurtByMob() == newTarget;
    }
}
```

**Pros:**
- Centralized control over ALL targeting
- Works regardless of which goal sets target

**Cons:**
- More invasive, higher conflict risk with other mods
- Harder to debug
- May interfere with Brain-based mobs unexpectedly

### Approach C: Combined (BEST FOR YOUR REQUIREMENTS)

Use both approaches:
1. **Custom ThreatTargetGoal (priority 0)** - Actively selects highest-threat target
2. **setTarget() mixin** - Prevents unwanted target switches

This ensures:
- Threat-based targeting is proactively applied
- Other goals cannot override without meeting your conditions

**Confidence:** MEDIUM - Patterns verified, but specific implementation needs testing

---

## 5. Handling "Last Hurt By" (Revenge) Mechanic

### How Vanilla Revenge Works

**Key methods (Mojang mappings):**
```java
// In LivingEntity
@Nullable LivingEntity getLastHurtByMob();           // Who hurt me
int getLastHurtByMobTimestamp();                      // When (tick count)
void setLastHurtByMob(@Nullable LivingEntity entity); // Set revenge target
```

**Behavior:**
1. When `hurt()` is called, `setLastHurtByMob(attacker)` is set
2. `lastHurtByMobTimestamp` is set to current tick count
3. After 100 ticks (5 seconds) of not being hurt, `lastHurtByMob` is cleared
4. `HurtByTargetGoal` checks `getLastHurtByMob()` in `canUse()`

### HurtByTargetGoal / RevengeGoal

**What it does:**
1. `canUse()` - Returns true if `getLastHurtByMob()` is not null and valid
2. `start()` - Calls `mob.setTarget(lastHurtByMob)` AND alerts nearby same-type mobs
3. `alertOthers()` - Notifies nearby mobs of same type to also target the attacker

**Alert radius:** Typically 10-20 blocks (varies by mob)

### For Your Threat System

**Recommendation:** Let revenge work naturally, but integrate it with threat:

```java
// In your damage listener
public void onMobDamaged(Mob mob, DamageSource source, float amount) {
    if (source.getEntity() instanceof Player player) {
        // Add damage to threat
        double threat = amount / 2.0;  // Half of damage as base threat

        // Check if arrow for +10 bonus
        if (source.getDirectEntity() instanceof Arrow) {
            threat += 10.0;
        }

        // Add threat to THIS mob
        ThreatManager.addThreat(mob, player.getUUID(), threat);

        // Add threat to nearby hostile/neutral mobs in 15 blocks
        AABB area = mob.getBoundingBox().inflate(15.0);
        for (Mob nearby : mob.level().getEntitiesOfClass(Mob.class, area, this::isHostileOrNeutral)) {
            ThreatManager.addThreat(nearby, player.getUUID(), threat);
        }

        // Revenge is automatic via vanilla setLastHurtByMob
        // Your goal should allow targeting switch when getLastHurtByMob() matches
    }
}
```

**Confidence:** HIGH - Verified with [Forge/NeoForge LivingEntity Javadoc](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/LivingEntity.html)

---

## 6. Implementation Architecture for Threat System

### Data Storage (Using Fabric Attachments)

```java
// Attachment for per-mob threat data
public static final AttachmentType<Map<UUID, Double>> MOB_THREAT = AttachmentRegistry.create(
    Identifier.of("thc", "mob_threat"),
    builder -> builder.initializer(HashMap::new)
);

public static final AttachmentType<Long> THREAT_LAST_DECAY = AttachmentRegistry.create(
    Identifier.of("thc", "threat_last_decay"),
    builder -> builder.initializer(() -> 0L)
);
```

### Threat Manager

```java
public class ThreatManager {
    public static void addThreat(Mob mob, UUID playerUuid, double amount) {
        Map<UUID, Double> threats = mob.getAttachedOrCreate(MOB_THREAT);
        double current = threats.getOrDefault(playerUuid, 0.0);
        threats.put(playerUuid, current + amount);
    }

    public static double getThreat(Mob mob, UUID playerUuid) {
        return mob.getAttachedOrCreate(MOB_THREAT).getOrDefault(playerUuid, 0.0);
    }

    public static void decayThreat(Mob mob) {
        long now = mob.level().getGameTime();
        long lastDecay = mob.getAttachedOrGet(THREAT_LAST_DECAY, () -> 0L);

        if (now - lastDecay >= 20) {  // Once per second
            Map<UUID, Double> threats = mob.getAttachedOrCreate(MOB_THREAT);
            threats.replaceAll((uuid, threat) -> Math.max(0.0, threat - 1.0));
            threats.values().removeIf(v -> v <= 0.0);
            mob.setAttached(THREAT_LAST_DECAY, now);
        }
    }

    @Nullable
    public static Player getHighestThreatTarget(Mob mob, double minThreat) {
        Map<UUID, Double> threats = mob.getAttachedOrCreate(MOB_THREAT);

        return threats.entrySet().stream()
            .filter(e -> e.getValue() >= minThreat)
            .max(Map.Entry.comparingByValue())
            .map(e -> mob.level().getPlayerByUUID(e.getKey()))
            .filter(Objects::nonNull)
            .filter(p -> mob.canAttack(p))
            .orElse(null);
    }
}
```

### ThreatTargetGoal

```java
public class ThreatTargetGoal extends TargetGoal {
    private static final double MIN_THREAT = 5.0;
    private Player target;

    public ThreatTargetGoal(Mob mob) {
        super(mob, false);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        ThreatManager.decayThreat(this.mob);

        this.target = ThreatManager.getHighestThreatTarget(this.mob, MIN_THREAT);
        return this.target != null;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }

        // Check for revenge switch
        LivingEntity revenge = this.mob.getLastHurtByMob();
        if (revenge instanceof Player revengePlayer && revenge != this.target) {
            double revengeThreat = ThreatManager.getThreat(this.mob, revengePlayer.getUUID());
            if (revengeThreat >= MIN_THREAT) {
                this.target = revengePlayer;
                this.mob.setTarget(this.target);
            }
        }

        // Check for strictly higher threat
        Player highest = ThreatManager.getHighestThreatTarget(this.mob, MIN_THREAT);
        if (highest != null && highest != this.target) {
            double currentThreat = ThreatManager.getThreat(this.mob, this.target.getUUID());
            double highestThreat = ThreatManager.getThreat(this.mob, highest.getUUID());
            if (highestThreat > currentThreat) {  // Strictly higher
                this.target = highest;
                this.mob.setTarget(this.target);
            }
        }

        return this.mob.canAttack(this.target);
    }

    @Override
    public void stop() {
        this.target = null;
        super.stop();
    }
}
```

**Confidence:** MEDIUM - Architecture pattern is sound, specific API usage needs validation

---

## 7. Mixin Strategy for Adding Goals

### Option A: Target Specific Mob Classes

```java
@Mixin(Zombie.class)
public abstract class ZombieThreatMixin extends Monster {
    protected ZombieThreatMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$addThreatGoal(CallbackInfo ci) {
        this.targetSelector.addGoal(0, new ThreatTargetGoal(this));
    }
}
```

**Mobs to target:**
- `Zombie` (and subtypes via inheritance: Husk, Drowned, ZombieVillager)
- `Skeleton` (and subtypes: Stray, WitherSkeleton)
- `Spider` (and CaveSpider)
- `Creeper`
- `Enderman`
- `Slime` (and MagmaCube)
- `Silverfish`, `Endermite`
- `Phantom`
- `Vindicator`, `Pillager`, `Evoker`, `Ravager`
- `Witch`
- `Guardian`, `ElderGuardian`
- `Blaze`, `Ghast`, `Piglin` (when hostile), `Hoglin`
- `Warden` (uses Brain, may need different approach)

### Option B: Target Monster Base Class

```java
@Mixin(Monster.class)
public abstract class MonsterThreatMixin extends PathfinderMob {
    protected MonsterThreatMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void thc$addThreatGoal(CallbackInfo ci) {
        // Check if this mob has targeting (not all do)
        if (!this.targetSelector.getAvailableGoals().isEmpty()) {
            this.targetSelector.addGoal(0, new ThreatTargetGoal(this));
        }
    }
}
```

**Note:** `Monster` extends `PathfinderMob` extends `Mob`. The `registerGoals()` method is defined in `Mob` but typically overridden.

### Option C: Entity Join World Event

Use Fabric API's `ServerEntityEvents.ENTITY_LOAD` to add goals when mob spawns:

```java
ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
    if (entity instanceof Monster monster) {
        // Check if already has our goal (avoid duplicates)
        boolean hasGoal = monster.targetSelector.getAvailableGoals().stream()
            .anyMatch(g -> g.getGoal() instanceof ThreatTargetGoal);

        if (!hasGoal) {
            monster.targetSelector.addGoal(0, new ThreatTargetGoal(monster));
        }
    }
});
```

**Confidence:** MEDIUM - Patterns are standard, but specific method names need verification in 1.21.11

---

## 8. Mojang vs Yarn Mapping Reference

| Concept | Mojang (your project) | Yarn |
|---------|----------------------|------|
| Base target goal | `TargetGoal` | `TrackTargetGoal` |
| Nearest enemy goal | `NearestAttackableTargetGoal` | `ActiveTargetGoal` |
| Revenge goal | `HurtByTargetGoal` | `RevengeGoal` |
| Goal system | `GoalSelector` | `GoalSelector` |
| Add goal | `addGoal(priority, goal)` | `add(priority, goal)` |
| Get target | `Mob.getTarget()` | `MobEntity.getTarget()` |
| Set target | `Mob.setTarget(entity)` | `MobEntity.setTarget(entity)` |
| Last attacker | `getLastHurtByMob()` | `getLastHurtByMob()` |
| Register goals | `registerGoals()` | `initGoals()` |
| Goal controls | `Goal.Flag` | `Goal.Control` |

**Confidence:** HIGH - Verified with official API documentation

---

## 9. Open Questions / Gaps

1. **Brain-based hostile mobs:** Warden, Piglin (when hostile), Breeze use Brain AI. Need separate research on how to inject threat targeting into Brain tasks.

2. **Neutral mob handling:** Wolves, Iron Golems, Bees have conditional hostility. Need to verify when to apply threat system.

3. **Target switching cooldowns:** Does vanilla have any cooldown on target changes? Need to check if rapid switching causes issues.

4. **Network sync:** If clients need to know threat levels (e.g., for UI), need to add packet sync.

5. **Persistence:** Current design doesn't persist threat across saves. May want to add `persistent(Codec)` for attachments.

---

## Sources

### Primary (HIGH confidence)
- [Yarn MobEntity 1.21.6 API](https://maven.fabricmc.net/docs/yarn-1.21.6+build.1/net/minecraft/entity/mob/MobEntity.html)
- [Yarn ActiveTargetGoal 1.21.4 API](https://maven.fabricmc.net/docs/yarn-1.21.4+build.1/net/minecraft/entity/ai/goal/ActiveTargetGoal.html)
- [Yarn TrackTargetGoal 1.21 API](https://maven.fabricmc.net/docs/yarn-1.21-rc1+build.1/net/minecraft/entity/ai/goal/TrackTargetGoal.html)
- [Yarn GoalSelector 1.19 API](https://maven.fabricmc.net/docs/yarn-1.19+build.1/net/minecraft/entity/ai/goal/GoalSelector.html)
- [Yarn net.minecraft.entity.ai.goal package 1.21.8](https://maven.fabricmc.net/docs/yarn-1.21.8+build.1/net/minecraft/entity/ai/goal/package-summary.html)

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Mob AI](https://minecraft.wiki/w/Mob_AI)
- [Fabric Wiki - Mixin Examples](https://wiki.fabricmc.net/tutorial:mixin_examples)
- [Fabric Wiki - Mixin Injects](https://wiki.fabricmc.net/tutorial:mixin_injects)
- [Fabric Documentation - Entity Attributes](https://docs.fabricmc.net/develop/entities/attributes)
- [Forge JavaDocs LivingEntity 1.18.2](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/LivingEntity.html)
- [Forge JavaDocs NearestAttackableTargetGoal 1.16.5](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.16.5/net/minecraft/entity/ai/goal/NearestAttackableTargetGoal.html)

### Tertiary (LOW confidence - patterns only)
- [McJTY Tutorial Episode 4](https://www.mcjty.eu/docs/1.18/ep4) - Goal implementation patterns
- [TelepathicGrunt Mixin Guide](https://gist.github.com/TelepathicGrunt/3784f8a8b317bac11039474012de5fb4) - Parent class access trick
- [Forge Forums - Change Mob AI](https://forums.minecraftforge.net/topic/92864-1163i-want-to-change-the-ai-of-an-existing-mob/) - GoalSelector modification patterns

---

## Metadata

**Confidence breakdown:**
- GoalSelector vs Brain: HIGH - Official wiki and API docs
- setTarget behavior: HIGH - API docs and code patterns
- NearestAttackableTargetGoal: HIGH - API docs
- Custom goal implementation: MEDIUM - Patterns verified, needs testing
- Mixin injection: MEDIUM - Standard patterns, method names may vary

**Research date:** 2026-01-19
**Valid until:** ~60 days (stable Minecraft version, unlikely to change significantly)
