# Phase 10: XP Economy Restriction - Context

**Gathered:** 2026-01-19
**Status:** Ready for planning (plan exists, needs update)

<vision>
## How This Should Work

XP orbs should only come from combat — killing mobs. All passive XP grinding methods are blocked cleanly: mining ores, breeding animals, fishing, trading with villagers, and taking items from furnaces.

When a player performs a blocked action (mining diamond ore, breeding cows, etc.), **nothing happens** related to XP. No orbs spawn, no visual feedback. The action completes normally otherwise — you still get the diamond, the baby animal is born — but no XP appears. Clean cancellation, not zero-value ghost orbs.

This reinforces the core philosophy: risk must be required for progress. Want to enchant your gear? Go fight something.

</vision>

<essential>
## What Must Be Nailed

- **Complete cancellation** — Blocked sources spawn absolutely nothing, no zero-value orbs
- **Combat XP untouched** — Mob kills work exactly as vanilla
- **Experience bottles stay functional** — They're rare/expensive and represent earned gameplay, not passive grinding

</essential>

<specifics>
## Specific Ideas

- Experience bottles are the ONE exception — keep them working normally
- All other non-combat sources blocked: ores, breeding, fishing, trading, furnaces
- Visual consistency: either XP appears (combat/bottles) or nothing happens (everything else)

</specifics>

<notes>
## Additional Context

The original plan had bottles being cancelled entirely. Updated to keep bottles working since they're already gated behind meaningful gameplay (expensive trades, rare loot).

Plan 10-01-PLAN.md exists but needs update to:
1. Use complete cancellation (not zero-value redirects) for all blocked sources
2. Remove the ExperienceBottleXpMixin entirely — bottles stay functional

</notes>

---

*Phase: 10-xp-economy*
*Context gathered: 2026-01-19*
