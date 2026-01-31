# Feature Landscape: Villager Overhaul

**Domain:** Minecraft villager trading system modification
**Researched:** 2026-01-30
**Confidence:** HIGH (verified against official wiki)

## Executive Summary

THC v2.8 overhauls vanilla villager trading to create a stage-gated, player-controlled progression system. This document maps vanilla mechanics against THC requirements, identifying modification points and edge cases.

---

## Part 1: Vanilla Mechanics Reference

### Villager XP Thresholds

Villagers accumulate XP through trades and level up at specific thresholds.

| Level | Badge | Total XP Required | XP to Next Level |
|-------|-------|-------------------|------------------|
| Novice | Stone | 0 | 10 |
| Apprentice | Iron | 10 | 60 |
| Journeyman | Gold | 70 | 80 |
| Expert | Emerald | 150 | 100 |
| Master | Diamond | 250 | N/A |

**Source:** [Minecraft Wiki - Trading](https://minecraft.wiki/w/Trading)

### XP Per Trade (Villager receives)

XP awarded to the villager varies by trade level:

| Trade Level | XP Range | Typical Value |
|-------------|----------|---------------|
| Novice trades | 1-2 XP | 2 XP |
| Apprentice trades | 5-10 XP | 10 XP |
| Journeyman trades | 10-20 XP | 20 XP |
| Expert trades | 15-30 XP | 20-30 XP |
| Master trades | 30 XP | 30 XP |

**Implication:** Vanilla leveling is fast at lower levels (5 Novice trades = 10 XP = Apprentice) but progressively slower.

### Trades Per Level (Vanilla)

To reach each level from the previous:

| From | To | XP Needed | Trades at Previous Level |
|------|-----|-----------|-------------------------|
| Novice | Apprentice | 10 | ~5 Novice trades (2 XP each) |
| Apprentice | Journeyman | 60 | ~6 Apprentice trades (10 XP each) |
| Journeyman | Expert | 80 | ~4-8 Journeyman trades (10-20 XP each) |
| Expert | Master | 100 | ~3-5 Expert trades (20-30 XP each) |

### Trade Restocking

- **Frequency:** Twice per Minecraft day (at 2000 and 9000 ticks)
- **Requirements (Java):** Villager must pathfind to and "reach" their workstation
- **Requirements (Bedrock):** Also requires linked bed nearby
- **Reach definition:** Within 8 adjacent/diagonal blocks horizontally at feet level, or 9 blocks below
- **Uses restored:** Trades unlock based on use history

**Source:** [Minecraft Wiki - Villager](https://minecraft.wiki/w/Villager)

### Profession Acquisition

1. **Unemployed villager** searches for unclaimed workstation (48-block sphere)
2. **Provisional claim** made when villager pathfinds toward workstation
3. **Full claim** when within 2-block radius of workstation center
4. **Profession locked** after first trade with player
5. **Pre-trade reset:** Breaking workstation before trading = villager becomes unemployed again

**Critical timing:** Only during work hours (2000-9000 ticks)

### Trade Locking

- Each trade has limited stock (3-16 uses depending on trade)
- Locked trades restock when villager works at their workstation
- Profession and trades permanently lock after first trade

---

## Part 2: THC Custom System Requirements

### Allowed Professions

THC restricts villagers to 4 useful professions:

| Profession | Workstation | Rationale |
|------------|-------------|-----------|
| **Mason** | Stonecutter | Building materials |
| **Librarian** | Lectern | Enchanted books (but note: THC uses lectern enchanting, so this may need custom trades) |
| **Butcher** | Smoker | Food economy |
| **Cartographer** | Cartography Table | Structure locator items |

**Blocked professions:** All others (armorer, cleric, farmer, fisherman, fletcher, leatherworker, shepherd, toolsmith, weaponsmith, nitwit, none)

### Manual Leveling with Stage Gates

THC replaces automatic XP-based leveling with player-controlled progression.

| Villager Level | Required Player Stage | Emerald Cost |
|----------------|----------------------|--------------|
| Novice | Stage 1 | N/A (starting level) |
| Apprentice | Stage 2 | 1 emerald |
| Journeyman | Stage 3 | 1 emerald |
| Expert | Stage 4 | 1 emerald |
| Master | Stage 5 | 1 emerald |

**Interaction:** Right-click villager with emerald when:
1. Villager has max XP for current level
2. Player has required stage
3. Player holds emerald

### Reduced XP Requirements

THC dramatically reduces trades needed per level:

| To Level | Vanilla Trades | THC Trades | Reduction |
|----------|---------------|------------|-----------|
| Apprentice | ~5 | 2 | 60% |
| Journeyman | ~6 | 3 | 50% |
| Expert | ~4-8 | 4 | 0-50% |
| Master | ~3-5 | 5 | 0-40% |

**Implementation approach:** Modify XP per trade or modify thresholds.

**Option A - Modify XP per trade (RECOMMENDED):**
- All trades award 5 XP (constant)
- XP resets to 0 on each level-up
- Per-level thresholds: 10, 15, 20, 25
- Achieves exactly 2/3/4/5 trade pattern

**Option B - Modify thresholds:**
- Keep vanilla XP per trade (2/10/20/30)
- Modify thresholds to: 4, 34, 94, 174
- More complex, trades-per-level varies by which trade used

**Recommendation:** Option A is simpler and more predictable for players.

### Trade Cycling

Right-click with emerald at 0 XP to reroll current rank trades.

**Mechanics:**
- Only available at 0 XP (no trades completed at current level)
- Consumes 1 emerald
- Rerolls only current rank's trades (not lower ranks)
- Previous rank trades preserved

**Purpose:** Players can seek specific trades without grinding.

### Deterministic Trade Tables

THC uses user-specified exact trades per profession/rank rather than vanilla randomization.

**Structure:**
- Each profession has defined trades per level
- No random selection from pools
- Same trades for all villagers of that profession/level

---

## Part 3: Player Interaction Flows

### Flow 1: Acquiring a Villager

```
1. Player finds unemployed villager in village
2. Player places allowed workstation (stonecutter/lectern/smoker/cartography table)
3. Villager claims workstation, gains profession
4. Villager spawns with Novice trades

VALIDATION:
- If player places disallowed workstation: Villager ignores it (stays unemployed)
- If villager already has disallowed profession: [DECISION NEEDED]
  - Option A: Allow trading but no leveling
  - Option B: Block all interaction
  - Option C: Force unemployment when player interacts
```

### Flow 2: Trading and XP Accumulation

```
1. Player opens trade GUI
2. Player completes trade
3. Villager gains XP (THC: 5 XP per trade)
4. If XP reaches threshold:
   - Visual indicator (green particles? badge change?)
   - Villager ready for manual level-up
5. Player continues trading or exits

VALIDATION:
- Trade stock limits still apply
- Restocking still requires workstation access
```

### Flow 3: Manual Level-Up

```
1. Villager has max XP for current level
2. Player has required stage (e.g., Stage 2 for Apprentice)
3. Player holds emerald
4. Player right-clicks villager

SUCCESS:
- Emerald consumed
- Villager level increases
- New trades unlock
- XP resets to 0

FAILURE CONDITIONS:
- Villager not at max XP: "Villager needs more experience" message
- Player below required stage: "You need to reach Stage X first" message
- Player not holding emerald: Normal trade GUI opens
- Villager already Master: Normal trade GUI opens (nothing to level)
```

### Flow 4: Trade Cycling

```
1. Villager has 0 XP at current level (no trades completed this level)
2. Player holds emerald
3. Player right-clicks villager

SUCCESS:
- Emerald consumed
- Current rank trades regenerated (new random or deterministic selection)
- Previous rank trades unchanged
- XP remains 0

FAILURE CONDITIONS:
- Villager has >0 XP: Level-up flow takes precedence (or message)
- Villager is Novice with 0 XP: No previous trades, cycling still works
```

### Flow 5: Disallowed Profession Handling

```
Scenario: Player encounters villager with existing disallowed profession

Option A (Recommended): Profession Lock
- Villager keeps profession but cannot level up
- Trades available but no XP accumulates
- Message on emerald right-click: "This profession cannot advance"

Option B: Force Unemployment
- On first interaction, villager loses profession
- Becomes unemployed, can claim new workstation

Option C: Block Interaction
- Trade GUI does not open
- Message: "This villager's profession is not recognized"
```

---

## Part 4: Edge Cases and Validation

### Edge Case Matrix

| Scenario | Expected Behavior | Validation Required |
|----------|-------------------|---------------------|
| Right-click with emerald, 0 XP | Trade cycling | Check XP = 0 |
| Right-click with emerald, max XP | Level up (if stage met) | Check XP >= threshold AND player stage |
| Right-click with emerald, partial XP | Normal trade GUI | XP > 0 AND XP < threshold |
| Right-click without emerald | Normal trade GUI | Always |
| Level up without emerald | Impossible | Emerald check before level logic |
| Cycle at wrong XP level | Normal trade GUI | XP must be exactly 0 |
| Level up at wrong stage | Failure message | Stage check |
| Master villager + emerald + max XP | Normal trade (nothing to level) | Level check |
| Trading with disallowed profession | Option A: Trade but no XP | Profession check |

### XP Boundary Conditions (Recommended Implementation)

Using 5 XP per trade with XP reset on level-up:

```
Level thresholds (per-level, not cumulative):

Novice → Apprentice: 10 XP threshold (2 trades x 5 XP)
  - At 5 XP: Not ready (1 trade completed)
  - At 10 XP: Ready for level-up (2 trades completed)

Apprentice → Journeyman: 15 XP threshold (3 trades x 5 XP)
  - At 10 XP: Not ready (2 trades completed)
  - At 15 XP: Ready for level-up (3 trades completed)

Journeyman → Expert: 20 XP threshold (4 trades x 5 XP)
  - At 15 XP: Not ready (3 trades completed)
  - At 20 XP: Ready for level-up (4 trades completed)

Expert → Master: 25 XP threshold (5 trades x 5 XP)
  - At 20 XP: Not ready (4 trades completed)
  - At 25 XP: Ready for level-up (5 trades completed)
```

**Key design:** XP resets to 0 after each level-up, so thresholds are per-level, not cumulative. This matches vanilla behavior and simplifies implementation.

### Restocking Considerations

THC has villagers in perpetual "night mode" (v2.0 twilight system).

**Potential issue:** Villagers need work hours to restock, but THC forces night schedule (13000L = REST activity).

**Resolution needed:**
- Option A: Modify restocking to not require work hours
- Option B: Force WORK activity despite night schedule
- Option C: Special "twilight restocking" bypass logic

**Current v2.0 behavior:** VillagerMixin redirects `Brain.updateActivityFromSchedule()` to always pass 13000L (mid-night). This triggers REST activity, which may prevent restocking.

**Recommendation:** Test first; if restocking breaks, add bypass for workstation interaction during REST.

---

## Part 5: Modification Points

### Where to Hook

| Vanilla System | THC Modification | Hook Location |
|----------------|------------------|---------------|
| Profession assignment | Filter to 4 allowed | `Villager.setVillagerData()` or workstation claim |
| XP accumulation | Fixed 5 XP per trade | `Villager.rewardTradeXp()` or trade execution |
| Level thresholds | Custom values 10/15/20/25 | `VillagerData.NEXT_LEVEL_XP_THRESHOLDS` or check override |
| Auto level-up | Block, require manual | `Villager.increaseMerchantCareer()` or equivalent |
| Trade generation | Deterministic tables | Trade registry or `VillagerTrades` |
| Right-click handling | Emerald detection | `Villager.mobInteract()` |

### Key Classes to Investigate

From vanilla Minecraft (names may vary by mapping):
- `Villager` - Main entity class
- `VillagerData` - Level, profession, type storage
- `VillagerTrades` - Trade pool definitions
- `MerchantOffers` / `TradeOfferList` - Active trades
- `AbstractVillager` - Base merchant class

### Existing THC Mixins

Already implemented (from codebase review):
- `VillagerMixin.java` - Forces night schedule (perpetual twilight)
- `AbstractVillagerMixin.java` - Removes shield/bell/saddle trades

---

## Part 6: Anti-Features (What NOT to Build)

### Avoid These Patterns

| Anti-Feature | Why Avoid | THC Alternative |
|--------------|-----------|-----------------|
| Infinite trade cycling | Trivializes progression | Emerald cost per cycle |
| Automatic leveling | No player agency | Manual emerald + stage gate |
| All professions allowed | Bloated, unfocused | 4 curated professions |
| Random trade pools | Unpredictable, frustrating | Deterministic tables |
| XP overflow between levels | Confusing | Reset to 0 on level-up |

### Complexity to Defer

- **Multi-villager sync:** Don't sync trades between villagers
- **Trade history:** Don't track which trades player has used
- **Dynamic pricing:** Use fixed prices, ignore reputation system
- **Villager breeding restrictions:** Out of scope for v2.8

---

## Part 7: Sources and Confidence

### Primary Sources (HIGH confidence)

- [Minecraft Wiki - Trading](https://minecraft.wiki/w/Trading) - XP thresholds, trade structure
- [Minecraft Wiki - Villager](https://minecraft.wiki/w/Villager) - Profession mechanics, restocking

### Secondary Sources (MEDIUM confidence)

- [Modrinth - Trade Cycling](https://modrinth.com/mod/trade-cycling) - Mod implementation reference
- [Modrinth - VillagerConfig](https://modrinth.com/mod/villagerconfig) - Datapack approach reference

### Implementation References

- THC codebase: `/src/main/java/thc/mixin/VillagerMixin.java`
- THC codebase: `/src/main/java/thc/mixin/AbstractVillagerMixin.java`

---

## Part 8: Open Questions for Implementation

1. **Restocking in twilight:** Does perpetual night schedule break restocking? Needs testing.

2. **Profession blocking method:** Block at workstation claim? Block disallowed workstations entirely? Convert existing villagers?

3. **Trade cycling scope:** Reroll all current-level trades? Or offer pick from pool?

4. **Cartographer structure locators:** What structures? Custom items or vanilla explorer maps?

5. **Librarian trades:** With lectern enchanting (v2.5), what does librarian sell? Non-enchantment utility?

6. **Rail recipe changes:** Mentioned in milestone but not villager-related. Separate phase?

---

## Quality Gate Checklist

- [x] Vanilla XP mechanics fully documented
- [x] THC modification points clearly identified
- [x] Player interaction flows for all operations described
- [x] Edge cases and validation requirements listed
- [x] Confidence levels assigned to sources
- [x] Anti-features documented
- [x] Open questions flagged for planning phase
