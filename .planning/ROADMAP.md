# THC Roadmap

## Completed Milestones

- [v1.0](milestones/v1.0-ROADMAP.md) — Base Claiming & Crafting Foundation (phases 1-13)
- [v1.1](milestones/v1.1-ROADMAP.md) — Combat Polish & Drowning (phases 14-17)
- [v1.2](milestones/v1.2-ROADMAP.md) — XP Economy & Tiered Arrows (phases 18-22)
- [v1.3](milestones/v1.3-ROADMAP.md) — Wind Charges, Threats & World Difficulty (phases 23-28)
- [v2.0](milestones/v2.0-ROADMAP.md) — Twilight System (phases 29-36)
- [v2.1](milestones/v2.1-ROADMAP.md) — Blast Totem & Structure Protection (phases 29-32)
- [v2.2](milestones/v2.2-ROADMAP.md) — Food Economy & Class System (phases 33-36)
- [v2.3](milestones/v2.3-ROADMAP.md) — Monster Overhaul (phases 37-45)
- [v2.4](milestones/v2.4-ROADMAP.md) — Extra Features Batch 8 (phases 46-52)

## Current Milestone

**v2.5: Enchantment Overhaul** (phases 53-56)

Transform enchanting from tedious grind into stage-gated progression with meaningful choices. Removes 12 enchantments completely, enforces single-level enchantments across all types, introduces lectern enchanting for early-stage books (unlimited use), overhauls enchanting tables for stage 3+ books (book-determined outcomes), and adds mob-drop sources for specific stage 3+ enchantments while removing them from Overworld chests.

### Phase 53: Enforcement Foundation ✓

**Goal**: All 12 removed enchantments are purged from the game and all enchantments display/function as single-level

**Depends on**: Nothing (first phase)

**Requirements**: REM-01, REM-02, REM-03, LVL-01, LVL-02, LVL-04, LVL-05

**Note**: REM-04 and LVL-03 (legacy item correction) explicitly NOT implemented per CONTEXT.md decision. Legacy items from before the update keep their enchantments.

**Success Criteria** (what must be TRUE):
1. ✓ Removed enchantments (loyalty, impaling, riptide, infinity, knockback, punch, quick charge, lunge, thorns, wind burst, multishot, density) do not appear on any items in loot or mob spawns
2. ✓ Enchantment books for removed enchantments do not appear in any chest loot
3. ✓ All enchantments display without level suffix (no I/II/III)
4. ✓ Flame enchantment sets targets on fire for exactly 6 seconds (1 dmg/s)
5. ✓ Fire Aspect enchantment sets targets on fire for exactly 6 seconds (1.5 dmg/s)

**Plans**: 3/3 complete

Plans:
- [x] 53-01-PLAN.md — Core enchantment enforcement (loot filtering + mob equipment correction)
- [x] 53-02-PLAN.md — Display override (hide level suffix in tooltips)
- [x] 53-03-PLAN.md — Fire damage customization (Flame 6HP, Fire Aspect 9HP)

---

### Phase 54: Lectern Enchanting ✓

**Goal**: Players can use lecterns to apply stage 1-2 enchantments repeatedly without consuming books

**Depends on**: Phase 53 (single-level enforcement must be active)

**Requirements**: LEC-01, LEC-02, LEC-03, LEC-04, LEC-05, LEC-06

**Stage 1-2 Enchantments**: mending, unbreaking, efficiency, fortune, silk_touch

**Success Criteria** (what must be TRUE):
1. ✓ Stage 1-2 enchantment books can be placed on empty lecterns by right-clicking
2. ✓ Right-clicking a lectern holding a book while holding compatible gear applies the enchantment
3. ✓ Enchantment book remains on lectern after use (unlimited applications)
4. ✓ Lectern enchanting requires player level 10 minimum and costs 3 levels
5. ✓ Players below level 10 see action bar message "You must be level 10!" when attempting to enchant
6. ✓ Stage 3+ books rejected with "This enchantment requires an enchanting table!"

**Plans**: 1/1 complete

Plans:
- [x] 54-01-PLAN.md — Lectern enchanting handler (stage 1-2 set, UseBlockCallback, book placement, gear enchanting)

---

### Phase 55: Enchanting Table Overhaul ✓

**Goal**: Enchanting tables use book-slot mechanic for deterministic stage 3+ enchanting

**Depends on**: Phase 53 (single-level enforcement must be active)

**Requirements**: TBL-01, TBL-02, TBL-03, TBL-04, TBL-05, TBL-06

**Success Criteria** (what must be TRUE):
1. ✓ Enchanting tables require new recipe (ISI/SBS/ISI with iron blocks, soul dust, book)
2. ✓ Enchanting tables require 15 bookshelves to function (vanilla max detectable, lower counts show disabled UI)
3. ✓ Lapis slot replaced with enchanted book slot in enchanting table GUI
4. ✓ Book placed in slot determines exact enchantment applied (no RNG)
5. ✓ Stage 3 enchantments require level 20 minimum and cost 3 levels
6. ✓ Stage 4-5 enchantments require level 30 minimum and cost 3 levels

**Plans**: 3/3 complete

Plans:
- [x] 55-01-PLAN.md — Soul Dust item + stage classification (level requirements)
- [x] 55-02-PLAN.md — New recipe (ISI/SBS/ISI) + vanilla recipe removal
- [x] 55-03-PLAN.md — EnchantmentMenuMixin (deterministic book-slot enchanting)

---

### Phase 56: Acquisition Gating

**Goal**: Stage 3+ enchantments are only obtainable through specific mob drops, not Overworld chests

**Depends on**: Phase 53 (enchantment system foundation must be stable)

**Requirements**: ACQ-01, ACQ-02, ACQ-03, ACQ-04, ACQ-05, ACQ-06, ACQ-07, ACQ-08, ACQ-09

**Success Criteria** (what must be TRUE):
1. Stage 3+ enchantment books and enchanted items do not appear in any Overworld chest loot
2. Drowned drop aqua affinity, depth strider, frost walker, respiration books at 2.5% each (stage 3+ only)
3. Spiders drop bane of arthropods book at 2.5% (stage 3+ only)
4. Husks and strays drop smite book at 2.5% (stage 3+ only)
5. Blazes drop fire protection book at 2.5% (stage 3+ only)
6. Magma cubes drop flame and fire aspect books at 5% each (stage 3+ only)
7. Fishing lure and luck of the sea enchantments only obtainable at stage 3+

**Plans**: TBD

Plans:
- [ ] 56-01: TBD

---

## Progress

**Execution Order:** Phases execute in numeric order: 53 → 54 → 55 → 56

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 53. Enforcement Foundation | 3/3 | ✓ Complete | 2026-01-27 |
| 54. Lectern Enchanting | 1/1 | ✓ Complete | 2026-01-27 |
| 55. Enchanting Table Overhaul | 3/3 | ✓ Complete | 2026-01-28 |
| 56. Acquisition Gating | 0/TBD | Not started | - |

---

*Roadmap last updated: 2026-01-28*
