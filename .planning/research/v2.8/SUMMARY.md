# Project Research Summary

**Project:** THC v2.8 Villager Overhaul
**Domain:** Minecraft villager trading system modification
**Researched:** 2026-01-30
**Confidence:** HIGH

## Executive Summary

THC v2.8 transforms vanilla villager trading from an automatic XP-based system into a stage-gated, player-controlled progression mechanic. The overhaul restricts villagers to 4 useful professions (mason, librarian, butcher, cartographer), replaces automatic leveling with manual emerald-cost advancement, and provides deterministic custom trades. The implementation primarily uses targeted mixins at key interception points (profession assignment, trade generation, leveling logic) combined with Fabric's UseEntityCallback pattern already proven in THC's cow milking system.

The recommended approach leverages THC's existing architecture extensively. The StageManager provides ready-made level gates (Stage N required for villager level N+1). The POI blocking system from v2.6 extends naturally to disable disallowed job blocks. The UseEntityCallback pattern from copper bucket milking handles both manual leveling (shift+emerald) and trade cycling (emerald). All modifications stay at the data/behavior layer (VillagerData, MerchantOffers, Brain POI filtering) rather than replacing core systems, minimizing complexity and mod compatibility issues.

The critical risk is NBT persistence of existing villagers. Pre-mod villagers retain their professions and trades in entity NBT, requiring either migration logic or graceful degradation. Secondary risks include client-server trade desync during cycling, zombie cure profession bypass, and restocking conflicts with THC's perpetual night schedule. Prevention strategies are well-documented across similar mods (Trade Cycling, VillagerConfig, Liberty's Villagers) and implementation paths are verified against decompiled MC 1.21.11 sources.

## Key Findings

### Recommended Stack

The villager system in MC 1.21.11 uses a Brain-based AI for job acquisition and a static `VillagerTrades.TRADES` map for offer generation. THC already has foundational mixins (`VillagerMixin`, `AbstractVillagerMixin`, `BrainPoiMemoryMixin`) that interact safely with this system. The overhaul requires targeted mixins at specific interception points, with most complexity in trade table replacement and manual leveling handlers.

**Core technologies:**
- **Mixin injection at VillagerData.setVillagerData()**: Single chokepoint for all profession changes (AI, NBT, commands) - blocks disallowed professions at assignment time
- **UseEntityCallback.EVENT (Fabric API)**: Already proven in THC cow milking - handles emerald-based leveling/cycling without custom entity interaction mixins
- **TradeOfferHelper (Fabric API)**: Clean API for replacing vanilla ItemListings with custom deterministic trade tables per profession/level
- **VillagerData record (MC 1.21.11)**: Immutable data holder for profession/level/type with builder methods (withProfession, withLevel) - no custom state storage needed
- **StageManager integration**: Existing THC system provides level gates via getCurrentStage() - exactly matches villager level requirements

**Key API patterns verified:**
- XP thresholds extracted from bytecode: [0, 10, 70, 150, 250] cumulative
- MerchantOffer constructor supports custom trades with emerald costs, max uses, and XP values
- POI blocking extends existing ServerLevelPoiMixin pattern for disallowed job blocks
- Trade filtering extends existing AbstractVillagerMixin.getOffers() pattern

### Expected Features

The villager overhaul creates a curated, stage-locked trading progression that integrates with THC's existing milestone system. Research validated vanilla mechanics (XP thresholds, restocking rules, profession acquisition timing) and mapped them to THC requirements.

**Must have (table stakes):**
- Restrict to 4 professions (mason, librarian, butcher, cartographer) - block at setVillagerData() to prevent AI/NBT/cure bypass
- Manual leveling via emerald payment - UseEntityCallback with shift-click, gated by StageManager
- Stage gates per level (Stage 2 for Apprentice, Stage 3 for Journeyman, etc.) - 1:1 mapping stage to level
- Deterministic custom trades - replace VillagerTrades entries with fixed tables
- Trade cycling at emerald cost - reroll current rank only when XP = 0

**Should have (competitive):**
- POI blocking for disallowed job blocks - prevents profession acquisition entirely
- Clear visual/message feedback - "Stage X required" messages via displayClientMessage
- Restocking compatibility with perpetual night - verify workstation access during REST activity
- Reduced trades-per-level (2/3/4/5 trades for levels 2-5) - achieved via 5 XP per trade with XP reset on levelup

**Defer (v2+):**
- Gossip system modifications - keep or disable reputation pricing, but document behavior
- Multi-villager trade syncing - each villager independently managed
- Trade history tracking - no need to log which trades players used
- Villager breeding restrictions - out of scope for trading overhaul
- Dynamic pricing - use fixed emerald costs, ignore demand system

**Critical open questions:**
1. **Restocking in twilight:** Does perpetual night schedule (VillagerMixin forces 13000L = REST) break workstation restocking? Needs in-game testing.
2. **Existing villager migration:** What happens to pre-mod villagers with disallowed professions? Recommend: allow trading but block leveling.
3. **Librarian trades post-lectern-enchanting:** With v2.5 lectern system, what utility trades does librarian offer? Suggest: paper/books/bookshelves.

### Architecture Approach

The villager overhaul integrates with THC's existing systems via four clean boundaries: profession restriction (mixin), manual leveling (UseEntityCallback), POI filtering (mixin extension), and trade tables (data structures). This approach reuses proven THC patterns extensively, minimizing new code surface area and maintaining consistency with v2.0-v2.7 systems.

**Major components:**

1. **VillagerProfessionRestriction** - Mixin at `Villager.setVillagerData()` blocks profession changes to disallowed types; extends existing POI blocking (ServerLevelPoiMixin) to disable disallowed job blocks at placement time

2. **VillagerLeveling** - UseEntityCallback handler for shift+emerald interaction; checks StageManager.getCurrentStage() for gates, consumes emerald, increments VillagerData.level, triggers trade table update

3. **VillagerTradeCycling** - Same UseEntityCallback (non-shift emerald); validates XP = 0, regenerates current-level trades from deterministic table, preserves lower-level trades

4. **VillagerTradeOverrides** - TradeOfferHelper registration replaces vanilla ItemListings; organized as Map<Profession, Map<Level, List<ItemListing>>> with guaranteed entries per level to avoid empty pool crashes

5. **JobBlockPOI** - Extends ServerLevelPoiMixin.updatePOIOnBlockStateChange with profession filter; blocks POI registration for disallowed job blocks (composter, brewing stand, smithing table, etc.)

**Data flow for manual leveling:**
```
Player shift+right-click villager with emerald
  -> UseEntityCallback checks entity instanceof Villager
  -> Check StageManager.getCurrentStage() >= villagerLevel + 1
  -> Consume emerald, call villager.setVillagerData(data.withLevel(level+1))
  -> Trade table updated via TradeOfferHelper for new level
  -> GUI sync via villager.updateTrades()
```

**Reuse of existing THC patterns:**
- StageManager.getCurrentStage() (PatrolSpawnerMixin pattern)
- UseEntityCallback + item check (cow milking in THC.kt)
- ServerLevelPoiMixin extension (village deregistration)
- AbstractVillagerMixin.getOffers() filtering (shield removal)
- Component.literal + displayClientMessage (lectern enchanting feedback)

### Critical Pitfalls

Based on research across similar mods and verified THC patterns, five critical pitfalls require explicit prevention during implementation.

1. **Trade NBT Persistence** - Pre-mod villagers store trades in entity NBT; runtime filtering (getOffers mixin) only affects display, not storage. Prevention: Intercept updateTrades() for generation replacement, not getOffers() for display filtering. Clear existing offers on first post-mod interaction or accept inconsistency for legacy villagers.

2. **Profession Change via Zombie Cure** - Cured villagers bypass profession restrictions if uncured zombie is assigned profession during conversion. Prevention: Mixin ZombieVillager conversion to validate profession against allowlist, force to NONE if disallowed. Test: cure zombie villager near smithing table.

3. **Client-Server Trade Desync** - Trade cycling that updates server offers without GUI resync causes phantom trades. Prevention: After MerchantOffers modification, close/reopen GUI or send explicit sync packet. All trade logic must be server-side only with proper sync.

4. **XP/Level Threshold Mismatch** - Vanilla XP accumulates from trades but manual leveling gates progression, causing "stuck" villagers with high XP at low level. Prevention: Recommended Option A - set all trades to 5 XP, reset to 0 on levelup, use per-level thresholds 10/15/20/25. Alternative Option B - disable XP gain entirely (rewardExp = false).

5. **POI Race Condition in Villages** - THC blocks all POI in claimed chunks (good for bases) but villages are unclaimed, allowing restricted professions. Prevention: Profession restriction at setVillagerData() layer, not POI layer. POI blocking supplements but doesn't replace profession filtering. Villages need profession validation, claimed chunks get full POI block.

**Moderate pitfalls requiring attention:**
- Trade pool empty state crashes (ensure guaranteed trades per level)
- Structure locator map generation lag (cap search radius to 100 chunks)
- Gossip system interference with custom pricing (decide if discounts apply)
- Workstation claiming competition with only 4 professions (test in villages)

## Implications for Roadmap

Based on dependency analysis and THC's architectural patterns, recommend a 6-phase structure that builds incrementally from foundation to full feature set.

### Phase 1: Job Block POI Restriction
**Rationale:** Zero-dependency foundation. Extends existing ServerLevelPoiMixin with additional block type filtering. Prevents disallowed professions from being acquirable in the first place.

**Delivers:** Disallowed job blocks (composter, brewing stand, smithing table, blast furnace, fletching table, cauldron, barrel, grindstone, loom) do not register POI, making associated professions unacquirable.

**Addresses:** Profession restriction (FEATURES.md must-have), reuses v2.6 POI blocking pattern (ARCHITECTURE.md)

**Avoids:** POI race condition pitfall (PITFALLS.md #5) by blocking at POI registration, not AI layer

**Research flag:** Standard pattern - skip /gsd:research-phase. Direct extension of existing mixin.

### Phase 2: Profession Assignment Restriction
**Rationale:** Depends only on MC core APIs. Single mixin at setVillagerData() catches all profession changes (AI, NBT load, commands, zombie cure). Completes prevention layer started in Phase 1.

**Delivers:** Villagers can only have mason/librarian/butcher/cartographer professions. Existing villagers with disallowed professions gracefully degrade (block leveling, preserve trades).

**Addresses:** Profession allowlist (FEATURES.md must-have), zombie cure bypass prevention (PITFALLS.md #2)

**Avoids:** NBT persistence issues for legacy villagers (PITFALLS.md #1) via graceful degradation

**Research flag:** Standard pattern - skip /gsd:research-phase. VillagerData API verified in STACK.md.

### Phase 3: Custom Trade Tables
**Rationale:** Must exist before leveling/cycling can function. Independent of interaction logic. Defines the "what" before implementing the "how."

**Delivers:** Deterministic trade definitions for 4 professions across 5 levels. TradeOfferHelper registration replaces vanilla random pools with fixed ItemListings.

**Uses:** TradeOfferHelper (Fabric API), ItemListing implementations (STACK.md)

**Implements:** VillagerTradeOverrides component (ARCHITECTURE.md)

**Avoids:** Trade pool empty state crashes (PITFALLS.md #6) via guaranteed entries per level

**Research flag:** NEEDS /gsd:research-phase for trade design. Must define specific trades per profession/level, emerald costs, max uses, XP values. Consult .planning/PROJECT.md for economy balance.

### Phase 4: Manual Leveling System
**Rationale:** Depends on Phase 3 (needs trade tables to unlock). Core mechanic that gates progression. UseEntityCallback pattern already proven in THC.

**Delivers:** Shift+emerald on villager levels them up if: (1) player has required stage, (2) villager at max XP for current level. XP modified to 5 per trade, resets to 0 on levelup, thresholds 10/15/20/25.

**Uses:** StageManager.getCurrentStage() (STACK.md), UseEntityCallback.EVENT (ARCHITECTURE.md)

**Addresses:** Manual leveling (FEATURES.md must-have), stage gates (FEATURES.md must-have), reduced trades-per-level (FEATURES.md should-have)

**Avoids:** XP/level threshold mismatch (PITFALLS.md #4) via explicit XP modification and reset

**Research flag:** Standard pattern - skip /gsd:research-phase. Reuses cow milking UseEntityCallback pattern exactly.

### Phase 5: Trade Cycling System
**Rationale:** Depends on Phase 3 (needs trade tables to reroll). Enhances but doesn't block core progression. Same callback as Phase 4.

**Delivers:** Emerald (non-shift) on villager at XP=0 rerolls current-level trades. Consumes 1 emerald, preserves lower-level trades, regenerates from deterministic table.

**Uses:** Same UseEntityCallback as Phase 4 (dispatch on shift key state)

**Addresses:** Trade cycling (FEATURES.md must-have), emerald economy integration

**Avoids:** Client-server desync (PITFALLS.md #3) via explicit GUI resync after offer modification

**Research flag:** NEEDS /gsd:research-phase for cycling scope. Decide: reroll all current-level trades, or cycle through predefined variations? Affects trade table structure.

### Phase 6: Structure Locator Items
**Rationale:** Cartographer-specific feature. Independent of core systems. Can be deferred if scope creeps.

**Delivers:** Custom structure locator items (not vanilla explorer maps) for THC-specific structures. Cartographer trades at higher levels.

**Addresses:** Cartographer utility (FEATURES.md open question), structure discovery mechanic

**Avoids:** Map generation lag (PITFALLS.md #7) via capped search radius and async generation consideration

**Research flag:** NEEDS /gsd:research-phase. Must define: which structures? Custom items or vanilla maps? Integration with THC structure generation?

### Phase Ordering Rationale

- **Phases 1-2 are prerequisites** for villager behavior - they prevent unwanted professions before adding new mechanics
- **Phase 3 must precede 4-5** because leveling/cycling need trade tables to apply
- **Phases 4-5 can be sequential** (both use UseEntityCallback but distinct logic) or combined
- **Phase 6 is independent** and can be deferred entirely if cartographer trades use standard items

**Dependency graph:**
```
Phase 1 (POI) -----> Phase 2 (Profession) -----> Phase 4 (Leveling) -----> Phase 6 (Structures)
                                           \
                                            ----> Phase 5 (Cycling)
                     Phase 3 (Trades) -----/
```

**Critical path:** 1 → 2 → 3 → 4 (minimum viable overhaul)

**Parallel opportunities:** Phase 1 and Phase 2 can be implemented concurrently (different mixins, no shared state)

### Research Flags

**Phases needing /gsd:research-phase during planning:**

- **Phase 3 (Trade Tables):** Must define exact trades - which items, emerald costs, quantities, max uses. Requires economy balance analysis against THC's existing progression. Check .planning/PROJECT.md for emerald sources/sinks.

- **Phase 5 (Trade Cycling):** Decide cycling scope - full reroll vs predefined variations. If variations, needs larger trade pool design. If full reroll, verify randomization doesn't conflict with "deterministic" requirement.

- **Phase 6 (Structure Locators):** Scope entirely dependent on THC structure generation. May require custom item types, loot table integration, or chunk scanning logic.

**Phases with standard patterns (skip research-phase):**

- **Phase 1 (POI):** Direct extension of ServerLevelPoiMixin, block type list is straightforward
- **Phase 2 (Profession):** VillagerData API fully documented in STACK.md, setVillagerData interception is proven pattern
- **Phase 4 (Leveling):** UseEntityCallback + StageManager exactly matches cow milking + patrol spawning patterns

**Testing requirements per phase:**

Phase 1-2: Test profession acquisition, zombie cure, existing villager migration
Phase 3: Verify trade pool completeness for all profession/level combinations
Phase 4: Test stage gates, XP accumulation, levelup triggers
Phase 5: Test GUI sync, emerald consumption, trade preservation
Phase 6: Test structure search performance, map generation

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All MC 1.21.11 APIs verified via decompiled sources; XP thresholds extracted from bytecode; existing THC mixin compatibility confirmed |
| Features | HIGH | Vanilla mechanics fully documented via official wiki; THC requirements clear from milestone spec; edge cases identified and handled |
| Architecture | HIGH | Direct reuse of 5 existing THC patterns; UseEntityCallback proven in cow milking; POI blocking proven in village deregistration; StageManager proven in patrol spawning |
| Pitfalls | MEDIUM | Trade NBT persistence verified in community mods; zombie cure bypass inferred from vanilla behavior; client-server sync patterns standard but need testing |

**Overall confidence:** HIGH

Research verified against primary sources (MC 1.21.11 decompiled code, official wiki, THC codebase), architectural patterns proven in production (v2.0-v2.7), and implementation paths confirmed via existing mixin inspection. Medium confidence on pitfalls reflects community experience rather than official docs, but multiple mod implementations corroborate findings.

### Gaps to Address

**Must validate during implementation:**

1. **Restocking with perpetual night:** VillagerMixin forces schedule to 13000L (mid-night = REST activity). Vanilla restocking requires "work hours" and workstation pathfinding. TEST: Do villagers restock during REST activity? If not, add workstation interaction bypass during twilight or force WORK activity for restocking window.

2. **Librarian trade utility:** With lectern enchanting (v2.5), enchanted books are redundant. DECIDE: Does librarian sell bookshelves, paper, books for decoration/storage? Or different profession entirely? May affect "4 useful professions" requirement.

3. **Gossip system interaction:** Zombie curing gives discounts via reputation. DECIDE: Do custom fixed-price trades respect gossip multipliers? If yes, ensure price calculation preserved. If no, disable gossip entirely and document. Affects iron golem spawning as side effect.

4. **Trade cycling scope:** "Reroll current rank trades" is ambiguous. CLARIFY: Does cycling regenerate from same deterministic pool (no change)? Or select different variant from larger pool? Or truly random within bounds? Affects trade table design in Phase 3.

**Nice-to-validate but not blockers:**

5. **Wandering trader filtering:** AbstractVillagerMixin.getOffers() affects wandering traders. VERIFY: Are wandering trader trades accidentally filtered? Likely fine but check in-game.

6. **Workstation competition:** With only 4 professions, village villagers may fight over limited workstations. OBSERVE: Does this cause practical issues in natural villages? May need documentation rather than fix.

7. **NBT format stability:** VillagerData, Offers structure assumed stable in 1.21.x. DOCUMENT: This implementation targets MC 1.21.11 specifically. Version updates may require NBT migration.

## Sources

### Primary (HIGH confidence)
- MC 1.21.11 decompiled sources (gradle cache) - Villager.java, VillagerData.java, VillagerTrades.java, MerchantOffer.java, AssignProfessionFromJobSite.java
- Minecraft Official Wiki - Trading mechanics (https://minecraft.wiki/w/Trading), XP thresholds verified
- Minecraft Official Wiki - Villager behavior (https://minecraft.wiki/w/Villager), profession acquisition, restocking rules
- THC codebase - VillagerMixin.java, AbstractVillagerMixin.java, BrainPoiMemoryMixin.java, ServerLevelPoiMixin.java, StageManager.java, THC.kt UseEntityCallback pattern

### Secondary (MEDIUM confidence)
- Trade Cycling mod (Modrinth) - Trade reroll implementation reference, client-server sync patterns
- VillagerConfig mod (Modrinth/GitHub DrexHD) - Datapack-based trade customization, ItemListing patterns
- Liberty's Villagers mod (Modrinth) - POI configuration approach, profession restriction patterns
- FabricMC Issue #4456 - Trade mixin null handling, ItemListing.getOffer() failure modes

### Tertiary (LOW confidence, validated against primary sources)
- Community forum discussions on villager AI behavior - Informed zombie cure bypass testing
- CurseForge mod descriptions for villager overhauls - Informed gossip system interaction considerations

---
**Research completed:** 2026-01-30
**Ready for roadmap:** Yes

**Key recommendation:** Implement incrementally (POI → Profession → Trades → Leveling → Cycling → Structures) to maintain working state at each phase. Leverage existing THC patterns extensively (5 direct reuses identified). Validate restocking in twilight mode early - if broken, adjust in Phase 1 before building dependent features.

**Main risk mitigation:** Handle pre-mod villagers gracefully (allow trading, block leveling for disallowed professions). Test zombie cure flow explicitly in Phase 2. Include GUI resync in Phase 5 trade cycling. Cap structure search in Phase 6 to prevent lag.
