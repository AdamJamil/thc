# Phase 50: Elytra Flight Changes - Context

**Gathered:** 2026-01-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Rebalance elytra flight to reward skill-based diving instead of firework spam. Remove firework rocket boost during flight, add pitch-based speed multipliers that amplify natural glide physics.

</domain>

<decisions>
## Implementation Decisions

### Firework Boost
- Firework rockets do not boost player speed during elytra flight
- Fireworks may still fire visually, just no velocity change

### Pitch-Based Speed Multipliers
- Multipliers apply to the **delta** (speed change per tick), not absolute velocity
- When diving naturally adds speed → that gain is multiplied by 2x
- When climbing naturally costs speed → that loss is multiplied by 1.8x
- No discrete pitch thresholds — sign of pitch determines which multiplier applies
- Multipliers apply whenever the gliding flag is set (including water/ground contact)

### Riptide Trident
- No changes — riptide works normally during elytra flight

### Claude's Discretion
- Exact injection point for modifying elytra physics
- How to intercept firework boost cleanly
- Neutral zone handling (if any)

</decisions>

<specifics>
## Specific Ideas

- "This is just about how the code adds or subtracts speed from the player. Depending on the sign, multiply by the right constant."
- The intent is amplifying natural glide physics, not overriding them

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 50-elytra-flight-changes*
*Context gathered: 2026-01-25*
