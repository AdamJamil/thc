---
phase: 08-projectile-combat
verified: 2026-01-18T14:00:00Z
status: passed
score: 5/5 must-haves verified
---

# Phase 8: Projectile Combat Verification Report

**Phase Goal:** Player projectiles create danger and have enhanced physics
**Verified:** 2026-01-18
**Status:** PASSED
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Player projectile hit applies Speed II to target mob for 6 seconds | VERIFIED | `MobEffects.SPEED, THC_EFFECT_DURATION_TICKS, 1` (amplifier 1 = level II, 120 ticks = 6 sec) at line 41 |
| 2 | Player projectile hit applies Glowing to target mob for 6 seconds | VERIFIED | `MobEffects.GLOWING, THC_EFFECT_DURATION_TICKS, 0` (120 ticks = 6 sec) at line 42 |
| 3 | Player projectile hit redirects mob aggro to the shooter | VERIFIED | `mob.setTarget(player)` at line 45, conditioned on `target instanceof Mob` |
| 4 | Player projectiles travel 20% faster initially (velocity boost on launch) | VERIFIED | `velocity.scale(1.2)` at line 68, in shoot TAIL injection |
| 5 | Player projectiles experience increased gravity after traveling 8 blocks | VERIFIED | `distance >= 8.0` check at line 92, quadratic gravity formula at lines 95-102 |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/thc/mixin/ProjectileEntityMixin.java` | Projectile hit effects and physics injection | VERIFIED | 105 lines, substantive implementation with 3 injections |
| `src/main/resources/thc.mixins.json` | Mixin registration | VERIFIED | ProjectileEntityMixin registered at line 12 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| ProjectileEntityMixin | Projectile.onHitEntity | @Inject at HEAD | WIRED | Line 27: `@Inject(method = "onHitEntity", at = @At("HEAD"))` |
| ProjectileEntityMixin | LivingEntity.addEffect | MobEffectInstance creation | WIRED | Lines 41-42: Effects applied with player attribution |
| ProjectileEntityMixin | Mob.setTarget | Aggro redirect | WIRED | Line 45: `mob.setTarget(player)` with instanceof check |
| ProjectileEntityMixin | Projectile.shoot | @Inject at TAIL | WIRED | Line 49: `@Inject(method = "shoot", at = @At("TAIL"))` |
| ProjectileEntityMixin | Projectile.tick | @Inject at HEAD | WIRED | Line 71: `@Inject(method = "tick", at = @At("HEAD"))` |
| ProjectileEntityMixin | spawn position tracking | @Unique fields | WIRED | Lines 22-25: thc$spawnX/Y/Z fields, recorded in shoot injection |

### Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| PROJ-01 (Speed II effect) | SATISFIED | MobEffects.SPEED with amplifier 1, 120 ticks |
| PROJ-02 (Glowing effect) | SATISFIED | MobEffects.GLOWING, 120 ticks |
| PROJ-03 (Aggro redirect) | SATISFIED | mob.setTarget(player) call |
| PROJ-04 (20% velocity boost) | SATISFIED | velocity.scale(1.2) in shoot injection |
| PROJ-05 (Quadratic gravity) | SATISFIED | distance >= 8.0 check with quadratic formula |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| - | - | None found | - | - |

No TODO, FIXME, placeholder, or stub patterns detected.

### Build Verification

```
./gradlew build
BUILD SUCCESSFUL in 5s
11 actionable tasks: 11 up-to-date
```

No mixin warnings or compilation errors.

### Human Verification Required

While all structural verification passes, the following should be tested in-game:

#### 1. Projectile Hit Effects Test
**Test:** Shoot a mob with a bow/arrow from within 8 blocks
**Expected:** 
- Mob gains Speed II effect (visible speed boost, particles)
- Mob gains Glowing effect (visible through walls)
- Mob immediately turns and chases the player
**Why human:** Visual effects and AI behavior require runtime observation

#### 2. Velocity Boost Test
**Test:** Shoot arrows with and without the mod (or compare to vanilla expectations)
**Expected:** Arrows travel noticeably faster initially
**Why human:** Velocity difference requires visual comparison

#### 3. Gravity Drop Test
**Test:** Shoot arrows at targets at 5 blocks, 10 blocks, 15 blocks, 20+ blocks
**Expected:**
- 5 blocks: Normal arrow behavior
- 10 blocks: Slight additional drop
- 15 blocks: Noticeable arc/drop
- 20+ blocks: Significant drop requiring aim compensation
**Why human:** Gravity curve requires visual observation at varying distances

#### 4. Player-Only Effects Test
**Test:** Let a skeleton shoot another mob
**Expected:** No Speed/Glowing effects, no aggro redirect (effects only apply to player projectiles)
**Why human:** Requires spawning mobs and observing behavior

### Implementation Analysis

**Code Quality:**
- Clean separation of concerns: hit effects, velocity boost, and gravity are in distinct methods
- Proper owner checks (`instanceof ServerPlayer`) ensure only player projectiles are affected
- Constant `THC_EFFECT_DURATION_TICKS = 120` centralizes duration (6 seconds = 120 ticks at 20 TPS)
- @Unique fields properly track spawn position across ticks
- Quadratic formula with cap (0.1 max) prevents extreme drop values

**Edge Case Handling:**
- `Double.isNaN(thc$spawnX)` check prevents calculation before spawn recorded
- `thc$spawnRecorded` flag ensures spawn is only recorded once
- `instanceof` checks throughout ensure type safety

---

*Verified: 2026-01-18*
*Verifier: Claude (gsd-verifier)*
