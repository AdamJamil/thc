# Phase 56: Acquisition Gating - Context

**Gathered:** 2026-01-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Gate stage 3+ enchantment books and enchanted items to specific mob drops. Remove stage 3+ enchantments from all chest loot, fishing loot, and other acquisition sources. Mob drops are the exclusive source for the specified enchantments.

</domain>

<decisions>
## Implementation Decisions

### Chest filtering scope
- Remove stage 3+ enchantment books from ALL chests (Overworld, Nether, End)
- Remove pre-enchanted items with stage 3+ enchantments from ALL chests
- Remove stage 3+ enchantment books from fishing loot
- Villager trades are OUT OF SCOPE for this phase (future modification)
- No other sources preserved — mob drops are exclusive for specified enchantments

### Loot table strategy
- Book drops are ADDED to existing mob loot (not replacing anything)
- Looting enchantment adds +1% total (any Looting level gives flat +1% boost)
  - 2.5% base → 3.5% with Looting
  - 5% base → 6% with Looting
- Magma Cubes: two separate 5% rolls (flame AND fire aspect independently)
- Drowned: four separate 2.5% rolls (can drop multiple books)
- Adults only — baby mobs do not drop enchantment books
- Any death source triggers drops (not player-kill restricted)
- Husks AND Strays both drop smite at 2.5% (confirmed independent sources)

### Stage classification update
- Stage 1-2 enchantments (updated): mending, unbreaking, efficiency, fortune, silk_touch, **lure**, **luck_of_the_sea**
- Lure and luck_of_the_sea must be added to STAGE_1_2_ENCHANTMENTS for lectern compatibility
- Stage 3+ = everything else (minus the 12 removed enchantments)
- Non-mob-dropped stage 3+ enchantments (sharpness, protection, power, etc.) remain enchanting-table-only for now

### Claude's Discretion
- Implementation approach for loot table modification (data pack vs mixin)
- Specific loot table files to modify
- Handling edge cases for enchanted item detection

</decisions>

<specifics>
## Specific Ideas

- Mob drop assignments (per requirements):
  - Drowned: aqua_affinity, depth_strider, frost_walker, respiration (2.5% each)
  - Spiders: bane_of_arthropods (2.5%)
  - Husks: smite (2.5%)
  - Strays: smite (2.5%)
  - Blazes: fire_protection (2.5%)
  - Magma Cubes: flame (5%), fire_aspect (5%)

</specifics>

<deferred>
## Deferred Ideas

- Villager librarian trade modifications — future phase
- Additional mob sources for other stage 3+ enchantments — future phase

</deferred>

---

*Phase: 56-acquisition-gating*
*Context gathered: 2026-01-28*
