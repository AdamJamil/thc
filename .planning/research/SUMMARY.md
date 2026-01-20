# Research Summary: v2.0 Twilight Hardcore

**Researched:** 2026-01-20
**Milestone:** v2.0
**Overall Confidence:** HIGH

## Executive Summary

The v2.0 Twilight Hardcore system is technically feasible with clean isolation between subsystems. Each of the five target features (twilight visuals, undead immunity, bee AI, spawn bypass, time unlock) has a dedicated interception point with no cascading effects.

**Key insight:** Minecraft's time/light systems are decentralized. Client rendering queries `ClientLevel`, mob burning queries `isSunBurnTick()`, bee AI queries `wantsToEnterHive()`, and spawning queries `checkMonsterSpawnRules()`. Modifying one does not affect the others.

**Risk assessment:** Client-side rendering is new territory for this codebase. All other features follow established THC mixin patterns.

---

## Key Findings by Dimension

### Stack (Mixin Targets)

| Feature | Target | Method | Confidence |
|---------|--------|--------|------------|
| Twilight visuals | `ClientLevel` | `getDayTime()` | HIGH |
| Undead immunity | `Mob` | `isSunBurnTick()` | HIGH |
| Bee always-work | `Bee` | `wantsToEnterHive()` | HIGH |
| Spawn bypass | `Monster` | `checkMonsterSpawnRules()` | HIGH |
| Remove night lock | Delete existing mixin | — | HIGH |

All methods verified via Mojang mappings. Client mixin goes in `thc.client.mixins.json`, server mixins in `thc.mixins.json`.

### Features (Expected Behavior)

- **Twilight visuals:** Sky locked to dusk (~13000 ticks), sun low on horizon, stars emerging, warm ambient lighting
- **Bees:** Work 24/7 regardless of time/weather, only return when nectar-full
- **Undead:** Never burn in sunlight (fire aspect and lava still work)
- **Spawning:** Monsters spawn regardless of sky light (block light still matters for spawn density)
- **Time:** Server time flows normally, villagers/crops/beds use real time

### Architecture (Isolation)

```
            Level (shared queries)
                    |
    +-------+-------+-------+-------+
    |       |       |       |       |
ClientLevel Mob   Bee   Monster  ServerLevel
getDayTime  isSun wants  check    getDifficulty
(visuals)   Burn  ToEnter Spawn   (existing)
            Tick  Hive    Rules
```

Each branch is independent. Modifying `isSunBurnTick` does not affect spawning or bee AI.

### Pitfalls (Critical)

1. **SKY-06: Client code in common mixin** — Server crash. Use separate mixin configs.
2. **COMPAT-01: Mixin nesting** — Use `@WrapOperation` not `@Redirect` for chaining compatibility.
3. **BURN-04: Multiple fire sources** — Target `isSunBurnTick()` specifically, not general fire.
4. **BEE-01: Brain vs Goal AI** — Bees use Brain system for complex behavior; verify target.

---

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Remove Night Lock
- **Rationale:** Prerequisite — current night lock conflicts with flowing time goal
- **Implementation:** Delete or disable the existing time-lock mixin
- **Risk:** Low — removal only

### Phase 2: Client Twilight Visuals
- **Rationale:** Highest risk, establishes client mixin patterns
- **Implementation:** `ClientLevel.getDayTime()` → return 13000L
- **Addresses:** Visual dusk requirement
- **Avoids:** SKY-06 (separate mixin config), COMPAT-01 (use @Inject not @Redirect)
- **Uses:** New client mixin infrastructure

### Phase 3: Undead Sun Immunity
- **Rationale:** Clear injection point, isolated from other systems
- **Implementation:** `Mob.isSunBurnTick()` → return false
- **Addresses:** Undead never burn
- **Avoids:** BURN-04 (target sun-specific method only)
- **Uses:** Existing server mixin pattern

### Phase 4: Hostile Spawn Bypass
- **Rationale:** Extends existing `NaturalSpawnerMixin` pattern
- **Implementation:** `Monster.checkMonsterSpawnRules()` → return true
- **Addresses:** Monsters spawn in daylight
- **Avoids:** SPAWN-01 (correct scope), SPAWN-03 (verified signature)
- **Uses:** Pattern from existing spawn blocking code

### Phase 5: Bee Always-Work
- **Rationale:** Depends on understanding AI system (Brain vs Goal)
- **Implementation:** `Bee.wantsToEnterHive()` → return false when no nectar
- **Addresses:** Bees work 24/7
- **Avoids:** BEE-01 (verify target method controls behavior)
- **Uses:** Goal-level interception

**Phase ordering rationale:**
1. Night lock removal first — prerequisite for everything else
2. Client visuals second — establishes new (client) mixin patterns, highest uncertainty
3. Server mechanics last — follow established patterns, lower risk
4. Bee AI last — most likely to need investigation if Brain system is involved

**Research flags for phases:**
- Phase 2 (Client Twilight): May need deeper research on shader compatibility if using Iris
- Phase 5 (Bee AI): May need Brain system investigation if `wantsToEnterHive()` doesn't fully control behavior
- Phases 3-4: Standard patterns, unlikely to need additional research

---

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Client time override | HIGH | Multiple working mods demonstrate pattern (Evernight, Always Day) |
| `isSunBurnTick` injection | HIGH | Single method entry point, verified in mappings |
| Bee `wantsToEnterHive` | HIGH | Clear method, verified in mappings |
| Spawn rule override | HIGH | Existing THC pattern validates approach |
| System isolation | HIGH | Independent call chains, no shared mutable state |

---

## Open Questions (Resolved)

| Question | Resolution |
|----------|------------|
| Rain during dusk? | Let rain darken naturally (atmospheric) |
| Bees: 24/7 or server time? | 24/7 (simpler, requested) |
| Shader compatibility? | Use @Inject for chaining, test with Iris |
| Phantom burning? | Covered by `Mob.isSunBurnTick()` (Phantom inherits from FlyingMob → Mob) |

---

## Files Generated

| File | Contents |
|------|----------|
| `STACK.md` | Mixin targets, method signatures, injection strategies |
| `FEATURES.md` | Expected visual behavior, mechanic decoupling patterns |
| `ARCHITECTURE.md` | Call chains, interception points, isolation analysis |
| `PITFALLS.md` | 20+ cataloged pitfalls with prevention strategies |

---

## Next Steps

1. **Define requirements** — `/gsd:define-requirements` to formalize acceptance criteria
2. **Create roadmap** — `/gsd:create-roadmap` with 5 phases as outlined above
3. **Plan phases** — Start with night lock removal, then client visuals

---

*Research completed: 2026-01-20*
*Valid until: ~30 days (Minecraft 1.21.x stable)*
