# Phase 44: Damage Rebalancing - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Tune damage output for 6 specific mobs to match THC balance. Melee damage via attribute modifiers, projectile damage via method interception. All mobs affected regardless of spawn source.

**Note:** Ghast fireball damage change (9→9.3) removed from scope. Ghast behavior changes (fire radius, velocity, fire rate) from Phase 40 remain intact.

</domain>

<decisions>
## Implementation Decisions

### Melee damage (Vex, Vindicator, Magma Cube)
- Use ATTACK_DAMAGE attribute modifier on ENTITY_LOAD
- Apply to ALL instances (natural spawns, spawners, summoned)
- Magma Cube: preserve size ratios (same multiplier to all sizes)
- Target exact damage values using precise multipliers

### Projectile damage (Blaze, Piglin)
- Blaze fireballs: intercept SmallFireball.onHitEntity to modify damage
- Piglin arrows: set Arrow.baseDamage at shoot time
- Only crossbow-wielding Piglins affected (ranged Piglins, not Brutes)

### Evoker fangs
- Intercept EvokerFangs.dealDamageTo method
- Apply to ALL fangs regardless of summoner (Evoker, commands, mods)

### Claude's Discretion
- Difficulty scaling behavior (fixed vs scaled)
- Exact multiplier calculation approach
- Mixin injection point specifics

</decisions>

<specifics>
## Specific Ideas

Target damage values (from ROADMAP.md):
- Vex: ~4 damage (down from 13.5)
- Vindicator: ~11.7 damage (down from 19.5)
- Evoker fangs: ~2.5 damage (down from 6)
- Blaze fireball: ~3.8 damage (down from 7.5)
- Piglin arrow: ~8 damage (up from 4)
- Large magma cube: ~4.7 damage (down from 9)

</specifics>

<deferred>
## Deferred Ideas

- Ghast fireball damage tuning — too complex for minimal change (9→9.3), behavior changes sufficient
- Difficulty-based damage scaling — not requested, use fixed values

</deferred>

---

*Phase: 44-damage-rebalancing*
*Context gathered: 2026-01-24*
