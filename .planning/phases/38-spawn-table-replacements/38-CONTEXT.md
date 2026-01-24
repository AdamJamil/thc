# Phase 38: Spawn Table Replacements - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace natural Overworld surface zombie spawns with husks and skeleton spawns with strays. Surface means sky-visible positions. Structure spawners and spider jockeys are exceptions. This phase does NOT add new mob types or change spawn rates — only substitutes existing spawn entries.

</domain>

<decisions>
## Implementation Decisions

### Surface definition
- Sky visibility check determines "surface" (isSkyVisible)
- Underground spawns (caves, mines) continue using vanilla zombie/skeleton
- Y-level is not a factor — only sky visibility matters

### Replacement scope
- Zombie → Husk for all sky-visible natural spawns
- Skeleton → Stray for all sky-visible natural spawns
- Drowned spawns are NOT affected (they spawn naturally in water biomes)
- Only affects NATURAL spawn reason, not spawners or events

### Exception handling
- Structure spawners (dungeons, fortresses, monuments): Keep vanilla mobs
- Spider jockeys on surface: Keep skeleton riders (preserve vanilla mechanic)
- Spider jockeys underground: Can have stray riders (follows cave rules)
- Zombie siege events: Replace with husks (consistent with surface rule)

### Side effects
- Husk → Drowned water conversion takes 90s (vs 45s for zombie) — intentional, document only
- Strays drop slowness arrows — intentional loot table change, no mitigation needed

### Claude's Discretion
- Exact mixin injection point selection
- Whether to use entity replacement vs spawn pool modification
- Error handling for edge cases

</decisions>

<specifics>
## Specific Ideas

- The change should be complete for surface spawns — zero vanilla zombies/skeletons on plains AFK test
- Spider jockey skeleton preservation is specifically about the "jockey" spawn context, not spider location

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 38-spawn-table-replacements*
*Context gathered: 2026-01-24*
