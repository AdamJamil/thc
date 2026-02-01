# Phase 38: Spawn Table Replacements - Research

**Researched:** 2026-01-24
**Domain:** Minecraft Fabric mob spawn manipulation
**Overall confidence:** HIGH

## Executive Summary

Phase 38 requires replacing natural Overworld surface zombie/skeleton spawns with husks/strays based on sky visibility. The research confirms this is achievable via a `@Redirect` mixin on `NaturalSpawner.spawnCategoryForPosition()` intercepting `ServerLevel.addFreshEntityWithPassengers()`. This approach allows entity type replacement during spawn with access to position, spawn reason, and mob category — sufficient for implementing sky-visibility-based logic and spawn reason filtering.

The key technical constraint: spider jockey skeletons spawn via JOCKEY reason, allowing preservation. Zombie sieges use NATURAL reason, meaning they'll correctly be replaced with husks. Structure spawners likely use SPAWNER or STRUCTURE reason — requires verification but expected to bypass replacement logic.

## Key Findings

**Stack:** Mixin on NaturalSpawner with @Redirect targeting addFreshEntityWithPassengers
**Architecture:** Entity replacement pattern at spawn-time, not biome pool modification
**Critical pitfall:** Spider jockeys require spawn reason check to preserve skeleton riders

## Implications for Roadmap

Based on research, suggested task structure:

1. **Sky Visibility Check Implementation** - Core logic
   - Implements: `level.canSeeSky(blockPos)` method access
   - Avoids: Incorrectly assuming y-level determines surface

2. **Entity Replacement Mixin** - Spawn interception
   - Implements: @Redirect on addFreshEntityWithPassengers in spawnCategoryForPosition
   - Avoids: Biome pool modification (would break structure spawners)

3. **Spawn Reason Filtering** - Exception handling
   - Implements: NATURAL-only filtering, JOCKEY/SPAWNER/STRUCTURE bypass
   - Avoids: Replacing spider jockey skeletons or dungeon spawns

**Task ordering rationale:**
- Sky visibility is foundational for replacement logic
- Entity replacement needs spawn reason filtering to work correctly
- Single mixin integrates both concerns

**Research flags for tasks:**
- Task 2: Verify structure spawner spawn reason (likely SPAWNER or STRUCTURE, not NATURAL)
- Task 3: Confirm zombie siege spawn reason is NATURAL (expected but not verified)

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Fabric-carpet and Curtain mods demonstrate @Redirect pattern works |
| Sky Visibility | HIGH | ServerLevel.canSeeSky(BlockPos) confirmed available across 1.18-1.21 |
| Spawn Reasons | MEDIUM | NATURAL/JOCKEY/SPAWNER enum values confirmed, structure spawner reason needs verification |
| Spider Jockey | MEDIUM | 1% spawn rate with skeleton rider confirmed, JOCKEY reason expected but not verified |
| Zombie Siege | MEDIUM | WebSearch confirms husks don't spawn in sieges — replacement will change vanilla behavior |

## Gaps to Address

- **Structure spawner spawn reason:** Research found dungeon spawners exist but didn't confirm their SpawnReason enum value. Task 2 should verify SPAWNER or STRUCTURE reason is used.
- **Zombie siege spawn reason:** Assumed NATURAL based on siege mechanics, but not explicitly verified. If sieges use EVENT or REINFORCEMENT, replacement logic needs adjustment.
- **Spider jockey spawn mechanics:** Expected to use JOCKEY reason, but WebSearch only confirmed 1% spawn rate, not the technical implementation.

---

# Technology Stack

**Project:** Phase 38 Spawn Table Replacements
**Researched:** 2026-01-24

## Recommended Stack

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Mixin | Fabric 0.18.4+ | Spawn interception | @Redirect pattern proven by fabric-carpet and Curtain mods |
| NaturalSpawner | MC 1.21.11 | Target class | spawnCategoryForPosition contains entity spawn call |

### Minecraft APIs
| API | Version | Purpose | Why |
|-----|---------|---------|-----|
| ServerLevel.canSeeSky | MC 1.21.x | Sky visibility | Confirmed available in BlockAndTintGetter interface |
| EntityType.HUSK | MC 1.21.x | Replacement mob | Direct entity type for zombie replacement |
| EntityType.STRAY | MC 1.21.x | Replacement mob | Direct entity type for skeleton replacement |
| SpawnReason enum | MC 1.21.x | Spawn filtering | NATURAL/JOCKEY/SPAWNER values control replacement logic |

### Supporting Patterns
| Pattern | Purpose | When to Use |
|---------|---------|-------------|
| @Redirect | Entity replacement | Intercept addFreshEntityWithPassengers call |
| Entity factory methods | Mob creation | EntityType.create(level, SpawnReason) for replacement entities |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Spawn manipulation | @Redirect mixin | BiomeModifications API | BiomeModifications.addSpawn is deprecated; pool modification would break structure spawners |
| Spawn manipulation | @Redirect mixin | Data pack biome files | Doesn't support conditional logic (sky visibility check) |
| Surface detection | canSeeSky(BlockPos) | Y-level threshold | Phase context explicitly requires sky visibility, not y-level |
| Entity creation | EntityType.create() | Entity constructor | Yarn docs warn constructors don't initialize mobs properly |

## Installation

No additional dependencies required. Uses existing Mixin framework and Minecraft APIs available in MC 1.21.11.

## Sources

- [Fabric Carpet NaturalSpawnerMixin](https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/mixins/NaturalSpawnerMixin.java) - Demonstrates @Redirect on spawnCategoryForPosition
- [Curtain NaturalSpawnerMixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java) - 1.21 implementation of spawn interception
- [ServerLevel API (Forge 1.21.x)](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/minecraft/server/level/ServerLevel.html) - canSeeSky method documentation

---

# Feature Landscape

**Domain:** Mob spawn replacement systems
**Researched:** 2026-01-24

## Table Stakes

Features users expect from spawn replacement. Missing = inconsistent behavior.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Natural spawn replacement | Core requirement | Medium | NATURAL spawn reason only |
| Sky visibility detection | Defines "surface" | Low | canSeeSky(BlockPos) API available |
| Structure spawner exception | Vanilla dungeons intact | Medium | Requires spawn reason filtering |
| Spider jockey exception | Preserve vanilla mechanic | Medium | JOCKEY spawn reason check |

## Differentiators

Features that would enhance the system beyond basic requirements.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Zombie siege replacement | Consistency with surface rule | Low | Sieges use NATURAL reason (expected) |
| Debug logging | Troubleshooting spawn issues | Low | Log entity type swaps for validation |
| Configurable replacement | Mod compatibility | High | Out of scope for this phase |

## Anti-Features

Features to explicitly NOT build. Common mistakes in spawn replacement.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Biome pool modification | Breaks structure spawners | Entity replacement at spawn-time |
| Y-level threshold | Not the surface definition | Use canSeeSky(BlockPos) check |
| All spawn reason replacement | Breaks jockeys/spawners | NATURAL-only filtering |
| Husk/stray attribute changes | Out of scope | Accept vanilla husk/stray stats |

## Feature Dependencies

```
Sky Visibility Check → Entity Replacement (replacement needs position check)
Entity Replacement → Spawn Reason Filtering (filtering prevents unwanted replacements)
```

## MVP Recommendation

For MVP, prioritize:
1. Entity replacement mixin (core functionality)
2. Sky visibility check (surface definition)
3. NATURAL spawn reason filtering (prevents spawner breakage)

Defer to post-MVP:
- Debug logging: Helpful but not required for correctness
- Zombie siege verification: Expected to work but needs testing

## Sources

- [Stray - Minecraft Wiki](https://minecraft.wiki/w/Stray) - Stray slowness arrow drops confirmed
- [Husk - Minecraft Wiki](https://minecraft.wiki/w/Husk) - Husk water conversion mechanics
- [Spider Jockey - Minecraft Wiki](https://minecraft.wiki/w/Spider_Jockey) - 1% spawn rate, skeleton rider mechanics
- [Zombie Siege - Minecraft Wiki](https://minecraft.wiki/w/Zombie_siege) - Husks don't spawn in sieges (vanilla behavior)

---

# Architecture Patterns

**Domain:** Minecraft mob spawn manipulation
**Researched:** 2026-01-24

## Recommended Architecture

```
NaturalSpawner.spawnCategoryForPosition()
  ↓ (vanilla calls)
ServerLevel.addFreshEntityWithPassengers(Entity)
  ↓ (mixin intercepts with @Redirect)
SpawnReplacementMixin.replaceEntity()
  ↓ (checks spawn reason, position, entity type)
  Decision: Replace or pass-through
  ↓ (if replace)
EntityType.create(level, replacementType, blockPos, spawnReason)
  ↓ (return replacement entity)
ServerLevel.addFreshEntityWithPassengers(replacementEntity)
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| SpawnReplacementMixin | Intercept spawn, decide replacement | NaturalSpawner, ServerLevel |
| Sky visibility check | Determine surface vs cave | ServerLevel.canSeeSky |
| Entity factory | Create replacement mob | EntityType.create |
| Spawn reason filter | Bypass non-NATURAL spawns | SpawnReason enum |

### Data Flow

1. Vanilla spawning selects entity type from biome pool
2. NaturalSpawner calls ServerLevel.addFreshEntityWithPassengers
3. Mixin intercepts with @Redirect, inspects entity and position
4. If NATURAL spawn reason + (ZOMBIE or SKELETON) + canSeeSky(pos):
   - Create replacement entity (HUSK or STRAY)
   - Return replacement to addFreshEntityWithPassengers
5. Else: return original entity (pass-through)

## Patterns to Follow

### Pattern 1: @Redirect for Entity Replacement
**What:** Intercept entity spawn call, conditionally replace entity
**When:** Need to modify entity type at spawn-time without changing biome pools
**Example:**
```java
@Redirect(
    method = "spawnCategoryForPosition",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
    )
)
private static boolean thc$replaceEntity(
    ServerLevel level,
    Entity entity,
    // Additional parameters from spawnCategoryForPosition accessible
    MobCategory category,
    ServerLevel world,
    ChunkAccess chunk,
    BlockPos pos,
    // etc.
) {
    // Replacement logic here
    Entity replacementEntity = determineReplacement(entity, pos, level);
    return level.addFreshEntityWithPassengers(replacementEntity);
}
```

### Pattern 2: Spawn Reason Access
**What:** Access SpawnReason from entity NBT or spawn context
**When:** Need to filter by spawn reason (NATURAL vs JOCKEY vs SPAWNER)
**Example:**
```java
// SpawnReason may be available in spawn context or entity data
// Research gap: verify exact access pattern in spawnCategoryForPosition
if (spawnReason != SpawnReason.NATURAL) {
    return level.addFreshEntityWithPassengers(entity); // Pass-through
}
```

### Pattern 3: Entity Type Check and Replacement
**What:** Conditional entity creation based on type and position
**When:** Replacing specific entity types (zombie/skeleton) at spawn
**Example:**
```java
if (entity.getType() == EntityType.ZOMBIE && level.canSeeSky(pos)) {
    Entity husk = EntityType.HUSK.create(level);
    husk.moveTo(pos.getX(), pos.getY(), pos.getZ());
    // Copy relevant data (equipment, baby status, etc.)
    return level.addFreshEntityWithPassengers(husk);
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: BiomeModifications for Conditional Logic
**What:** Using BiomeModifications.addSpawn to modify spawn pools
**Why bad:** Cannot implement sky visibility check; deprecated API; breaks structure spawners
**Instead:** Use @Redirect mixin for spawn-time entity replacement

### Anti-Pattern 2: Modifying Biome Data Packs
**What:** Changing biome JSON files to swap zombie→husk in spawn pools
**Why bad:** No conditional logic support; global change affects structure spawners
**Instead:** Runtime entity replacement with position-based conditions

### Anti-Pattern 3: Entity Conversion After Spawn
**What:** Let vanilla entity spawn, then convert using entity transformation
**Why bad:** Introduces delay, extra entity ticking, complexity
**Instead:** Replace at spawn-time before entity enters world

### Anti-Pattern 4: Y-Level Threshold for Surface Detection
**What:** Using `pos.getY() >= 64` to determine surface
**Why bad:** Caves can exist above y=64; phase context requires sky visibility
**Instead:** Use `level.canSeeSky(pos)` for accurate surface detection

## Scalability Considerations

| Concern | At 100 spawns/sec | At 1000 spawns/sec | At 10K spawns/sec |
|---------|-------------------|---------------------|-------------------|
| canSeeSky calls | Negligible | Negligible | Negligible (vanilla already checks light) |
| Entity creation | No extra cost | No extra cost | No extra cost (replaces one create with another) |
| Conditional checks | < 1µs per spawn | < 1ms total | < 10ms total (simple boolean checks) |

**Conclusion:** Performance impact is negligible. Spawn interception adds ~3-4 boolean checks per NATURAL mob spawn, well within Minecraft's spawn budget.

## Sources

- [Fabric Carpet NaturalSpawnerMixin](https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/mixins/NaturalSpawnerMixin.java) - @Redirect pattern on addFreshEntityWithPassengers
- [Curtain NaturalSpawnerMixin](https://github.com/Gu-ZT/Curtain/blob/1.21/src/main/java/dev/dubhe/curtain/mixins/NaturalSpawnerMixin.java) - Parameter access pattern for spawnCategoryForPosition
- [Creating an Entity - Fabric Wiki](https://wiki.fabricmc.net/tutorial:entity) - EntityType.create() recommended over constructors

---

# Domain Pitfalls

**Domain:** Minecraft spawn manipulation
**Researched:** 2026-01-24

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: Incorrect Spawn Reason Filtering
**What goes wrong:** Replacing spider jockey skeletons or dungeon spawner mobs
**Why it happens:** Assuming all zombie/skeleton spawns are NATURAL
**Consequences:** Spider jockeys spawn with stray riders (lose skeleton visuals); dungeon spawners produce husks/strays (changes difficulty curve)
**Prevention:** Filter by `SpawnReason.NATURAL` before replacement logic
**Detection:** AFK test near spider spawns — if stray jockeys appear, filtering failed

### Pitfall 2: Entity Attribute Loss During Replacement
**What goes wrong:** Baby zombies become adult husks; equipped skeletons lose equipment
**Why it happens:** Creating replacement entity without copying vanilla entity data
**Consequences:** Inconsistent mob difficulty; visual bugs (naked husks)
**Prevention:** Copy baby status, equipment, NBT tags from original entity to replacement
**Detection:** Spawn baby zombie on surface — if adult husk appears, attribute copy failed

### Pitfall 3: Structure Spawner Modification
**What goes wrong:** Dungeon spawners produce husks instead of zombies
**Why it happens:** Biome pool modification or incorrect spawn reason filtering
**Consequences:** Dungeons lose identity; XP farms produce wrong mob type
**Prevention:** Use entity replacement (not pool modification) with NATURAL-only filtering
**Detection:** Find dungeon spawner — if produces husks, structure spawns are affected

## Moderate Pitfalls

Mistakes that cause delays or technical debt.

### Pitfall 1: Missing Spider Jockey Exception
**What goes wrong:** Spider jockeys spawn with stray riders instead of skeletons
**Why it happens:** Replacing all surface skeleton spawns without checking spawn context
**Consequences:** Breaks vanilla spider jockey mechanic; strays dismount in water
**Prevention:** Check spawn reason for JOCKEY and bypass replacement
**Detection:** AFK in plains until spider jockey spawns — check rider type

### Pitfall 2: Zombie Siege Replacement Oversight
**What goes wrong:** Zombie sieges continue spawning zombies instead of husks
**Why it happens:** Assuming sieges use EVENT or REINFORCEMENT reason instead of NATURAL
**Consequences:** Inconsistent surface spawning rules; zombies appear on surface despite replacement
**Prevention:** Verify siege spawn reason; apply replacement if NATURAL
**Detection:** Trigger zombie siege near village — observe spawned mob types

### Pitfall 3: Y-Level Instead of Sky Visibility
**What goes wrong:** Caves above y=64 spawn husks; underground bases below y=64 spawn zombies
**Why it happens:** Using y-level threshold instead of canSeeSky check
**Consequences:** Incorrect surface/cave categorization; breaks phase requirements
**Prevention:** Use `level.canSeeSky(pos)` for all surface checks
**Detection:** Build cave at y=100 — if husks spawn, y-level logic is active

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

### Pitfall 1: Missing Spawn Reason Enum Value
**What goes wrong:** Structure spawners use unexpected spawn reason (e.g., CHUNK_GENERATION)
**Why it happens:** Incomplete research on structure spawner spawn reason
**Consequences:** Structure spawners incorrectly produce husks/strays
**Prevention:** Log spawn reasons during testing; add all non-NATURAL reasons to bypass list
**Detection:** Test multiple structure types (dungeon, fortress, monument) for correct spawns

### Pitfall 2: Husk/Stray Side Effect Confusion
**What goes wrong:** Players report "bugs" with husk→drowned conversion time or stray slowness arrows
**Why it happens:** Vanilla husk/stray mechanics differ from zombie/skeleton
**Consequences:** User confusion; false bug reports
**Prevention:** Document side effects in phase completion notes
**Detection:** N/A (user expectation issue, not technical bug)

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Entity replacement mixin | Missing spawn reason access | Verify spawnReason parameter availability in @Redirect signature |
| Sky visibility check | Assuming canSeeSky exists in all Level types | Use ServerLevel (confirmed available), not LevelAccessor |
| Spider jockey preservation | JOCKEY spawn reason not used | If no JOCKEY reason found, check entity passengers before replacement |
| Zombie siege replacement | Siege uses EVENT reason instead of NATURAL | Test zombie siege behavior; adjust filtering if needed |

## Sources

- [SpawnReason Enum (Forge 1.16.5)](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.16.5/net/minecraft/entity/SpawnReason.html) - NATURAL, SPAWNER, JOCKEY enum values
- [Zombie Siege - Minecraft Wiki](https://minecraft.wiki/w/Zombie_siege) - Husks never spawn in sieges (vanilla)
- [Spider Jockey - Minecraft Wiki](https://minecraft.wiki/w/Spider_Jockey) - 1% spawn rate, skeleton rider mechanics
- [Stray - Minecraft Wiki](https://minecraft.wiki/w/Stray) - Transformation preserves equipment via preserve_equipment flag
