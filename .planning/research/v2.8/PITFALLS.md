# Villager Overhaul Pitfalls

**Milestone:** v2.8 Villager Overhaul
**Researched:** 2026-01-30
**Confidence:** MEDIUM (WebSearch + existing THC patterns verified)

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: Trade NBT Persistence After Modification

**What goes wrong:** Removing trades via MerchantOffers.removeIf() at runtime does NOT remove them from villagers that already exist. Trade offers are stored in entity NBT and persist across world saves.

**Why it happens:** The trade list is generated once when a villager first levels up, then stored in the entity's Offers NBT tag. Any runtime filtering only affects the displayed list, not the persistent storage.

**Consequences:**
- Existing villagers keep "blocked" trades forever
- Players see inconsistent behavior between old and new villagers
- Trade cycling implementations break for existing villagers

**Prevention:**
1. For the "no shields/bells/saddles" pattern THC already uses (AbstractVillagerMixin.getOffers), this is fine for DISPLAY filtering - but understand it's client-presentation only
2. For complete trade replacement, intercept trade GENERATION (when villager levels up), not trade DISPLAY
3. Target the `updateTrades()` method or equivalent in Villager class
4. Consider clearing existing villager offers on first interaction post-mod-install

**Detection:** Test with a villager that existed before mod installation - do they still have the old trades?

**Which phase should address:** Phase 1 (Trade System Foundation) - need to decide early if this is display-filtering or generation-replacement

---

### Pitfall 2: Profession Change via Zombie Cure Bypass

**What goes wrong:** Players can bypass profession restrictions by zombie-curing villagers. A villager that was traded with keeps its profession forever, but an un-traded zombie villager can change profession when cured.

**Why it happens:** Cured villagers that haven't been traded with yet pick a new profession based on available workstations. They don't check profession restrictions during this process.

**Consequences:**
- Players can get armorers, weaponsmiths, etc. by strategic zombie curing
- Profession restriction system becomes trivially bypassed

**Prevention:**
1. Intercept profession assignment after zombie cure (ZombieVillager conversion event or mixin)
2. Check if the assigned profession is in the allowed list
3. If not allowed, either force to NONE or pick random allowed profession
4. Consider: should zombie villagers even spawn with restricted professions?

**Detection:** Cure a zombie villager near a blacksmith station - does it become a blacksmith?

**Which phase should address:** Phase 2 (Profession Restriction) - must handle cure flow, not just job block claims

---

### Pitfall 3: Client-Server Trade Desync

**What goes wrong:** Client shows different trade options than server has, causing failed trades or UI glitches. Players see "phantom trades" that can't be completed.

**Why it happens:**
- Server generates trades, sends to client on GUI open
- If mod modifies trades on only one side, desync occurs
- Custom trade cycling that updates server but doesn't resync client

**Consequences:**
- Trades appear available but fail when clicked
- Trade cycling appears to work but doesn't persist
- "Can't interact with villager" state requiring relog

**Prevention:**
1. ALL trade modifications must happen server-side
2. After any trade modification, call `villager.updateTrades()` or force resync
3. For trade cycling: close and reopen merchant GUI, or send explicit sync packet
4. Test: modify trades, check both F3+G debug and actual trade execution

**Detection:** Trade cycling appears to work visually but doesn't actually change what you can buy

**Which phase should address:** Phase 3 (Trade Cycling) - cycling must include sync step

---

### Pitfall 4: XP/Level Threshold Mismatch

**What goes wrong:** Custom leveling system conflicts with vanilla XP thresholds. Villagers gain XP from trades but manual level-up gates them, causing "stuck" state where villager has XP but can't unlock trades.

**Why it happens:** Vanilla villager levels are:
| Level | Name | XP Required |
|-------|------|-------------|
| 1 | Novice | 0 |
| 2 | Apprentice | 10 |
| 3 | Journeyman | 70 |
| 4 | Expert | 150 |
| 5 | Master | 250 |

If you gate levels with manual advancement but trades still reward XP, the villager's internal XP can exceed thresholds without leveling.

**Consequences:**
- Villagers with 70+ XP still at level 1
- When manually leveled, they might skip levels entirely
- XP-based behaviors (levelup particles, etc.) fire at wrong times

**Prevention:**
1. OPTION A: Disable XP gain from trades entirely (set rewardExp = false, xp = 0)
2. OPTION B: Intercept XP gain, don't let it exceed current level threshold
3. OPTION C: Let XP accumulate but only level when manual + XP threshold met
4. Document chosen approach clearly - affects trade offer generation

**Detection:** Trade many times with novice, then manually level - do they skip to journeyman?

**Which phase should address:** Phase 4 (Manual Leveling) - XP handling integral to leveling design

---

## Moderate Pitfalls

Mistakes that cause delays or technical debt.

### Pitfall 5: POI Race Condition on Profession Block Placement

**What goes wrong:** THC already blocks POI in claimed chunks (ServerLevelPoiMixin, BrainPoiMemoryMixin). But profession restriction needs to work IN villages too, and POI blocking happens before profession filtering.

**Why it happens:**
- THC blocks all POI in claimed chunks (good for player bases)
- Villages are NOT claimed chunks
- Village villagers need to claim their workstations (mason table, lectern, etc.)
- But restricted workstations (blacksmith, armorer tables) should not be claimable

**Consequences:**
- In villages, restricted professions can still be obtained
- In bases, no professions work at all (intended, but documented?)

**Prevention:**
1. For village villagers: filter profession assignment at AssignProfessionFromJobSite, not POI level
2. Allow POI for allowed professions, block only restricted ones
3. Or: block restricted POI types from even being registered (more invasive)
4. Consider removing restricted workstations from village structure spawns via datapack

**Detection:** Place smithing table in unclaimed village - can a villager become armorer?

**Which phase should address:** Phase 2 (Profession Restriction) - needs clear village vs base behavior

---

### Pitfall 6: Trade Pool Empty State Crashes

**What goes wrong:** If a villager tries to generate trades from an empty pool, game crashes or villager becomes untradeable.

**Why it happens:** Custom trade tables might not have entries for all levels, or entries might fail to generate (null items, missing resources). VillagerTrades.ItemListing can return null.

**Consequences:**
- NPE crashes on villager levelup
- Villagers stuck with 0 trades at a level
- Inconsistent trade counts

**Prevention:**
1. ALWAYS have at least one guaranteed trade per level
2. Handle null returns from ItemListing.getOffer()
3. Use FailureItemListing as fallback (vanilla pattern)
4. Test: level a villager to master, verify trade count at each level

**Detection:** Crash on villager interaction, or villager GUI shows no trades

**Which phase should address:** Phase 5 (Custom Trade Tables) - trade table structure must be robust

---

### Pitfall 7: Structure Locator Map Generation Timing

**What goes wrong:** TreasureMapForEmeralds-style trades generate maps on trade execution. If structure isn't in loaded chunks, map generation can lag or fail.

**Why it happens:** Structure locator uses `ServerLevel.findNearestMapStructure()` which may need to search many chunks. This is synchronous on the server thread.

**Consequences:**
- Server freezes for seconds when buying structure map
- Map might fail to locate if structure is very far
- Players can cheese by buying maps, then knowing where not to explore

**Prevention:**
1. Cap search radius for structure locator (vanilla uses 100 chunks typically)
2. Consider async structure search with "map generating..." feedback
3. Or: pre-generate maps for nearby structures on villager spawn/levelup
4. Test with fresh world where target structure is far away

**Detection:** Buy structure map in fresh world, observe freeze time

**Which phase should address:** Phase 6 (Structure Locator Items) - must handle edge cases

---

### Pitfall 8: Gossip System Interference

**What goes wrong:** Curing zombie villagers normally gives discounts via the gossip system. Custom trade tables might not respect gossip-based price modifiers, or gossip might override custom prices.

**Why it happens:** Villager gossip affects trade prices through reputation. Custom fixed-price trades might ignore this, confusing players. Or gossip stacks with already-cheap trades making them too cheap.

**Consequences:**
- Cured villagers have same prices as normal villagers (breaks player expectations)
- Or: prices become 1 emerald for everything after cure
- Iron golem spawning affected by gossip changes

**Prevention:**
1. Decide: do custom trades respect gossip modifiers?
2. If YES: ensure price multiplier logic preserved
3. If NO: document this clearly, maybe disable gossip entirely for custom professions
4. Consider: gossip affects iron golem spawning - changes here cascade

**Detection:** Cure a villager multiple times, check if prices decrease

**Which phase should address:** Phase 5 (Custom Trade Tables) - pricing policy decision needed

---

### Pitfall 9: Workstation Claiming by Wrong Villager

**What goes wrong:** Villager A places claim on lectern, then Villager B steals it. Villager A can't restock, becomes angry/confused.

**Why it happens:** Vanilla workstation claiming is proximity-based and can be "stolen" when villagers move around. This is especially problematic when restricting to 4 professions - fewer valid workstations means more competition.

**Consequences:**
- Villagers can't restock because their workstation was stolen
- Multiple villagers fighting over same workstation
- Trades lock permanently

**Prevention:**
1. In claimed chunks: POI already blocked, not an issue
2. In villages: ensure enough workstations per villager, or...
3. Consider: lock workstation claim once established (needs mixin)
4. Or: don't care, this is vanilla behavior and accepted

**Detection:** Watch village villagers over time - do librarians stop restocking?

**Which phase should address:** Phase 2 (Profession Restriction) - fewer professions may exacerbate this

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

### Pitfall 10: Wandering Trader Confusion

**What goes wrong:** Wandering traders use AbstractVillager but have completely different trade mechanics. Profession restrictions might accidentally apply to them.

**Why it happens:** VillagerMixin and AbstractVillagerMixin both exist. Wandering trader extends AbstractVillager but not Villager.

**Consequences:**
- Wandering trader gets profession restrictions (makes no sense)
- Trade filtering accidentally removes wandering trader items
- Players confused about what restrictions apply where

**Prevention:**
1. Ensure villager-specific logic uses Villager.class, not AbstractVillager.class
2. Review existing AbstractVillagerMixin.getOffers() - does it affect wandering traders?
3. Add instanceof Villager checks if needed

**Detection:** Check wandering trader trades - are they filtered incorrectly?

**Which phase should address:** Phase 1 (Trade System Foundation) - baseline mixin audit

---

### Pitfall 11: NBT Tag Format Changes Between MC Versions

**What goes wrong:** Trade NBT structure changes between Minecraft versions. What works in 1.21.11 might break in 1.21.12.

**Why it happens:** Mojang refactors NBT structure occasionally. VillagerData, Offers, etc. have changed format over time.

**Consequences:**
- World upgrades break existing villagers
- DataFix not applied to custom trades

**Prevention:**
1. Use Minecraft's accessor methods, not raw NBT manipulation
2. Test world upgrades if planning to support version updates
3. Document MC version this is built for

**Detection:** Load old world in new MC version - are villagers broken?

**Which phase should address:** All phases - general hygiene

---

### Pitfall 12: Missing rewardExp Tag on Custom Trades

**What goes wrong:** Custom trades don't give villager XP, so villagers can't level up naturally (if that's intended behavior).

**Why it happens:** When creating trades programmatically, easy to forget the rewardExp and xp fields. Defaults may not match expectations.

**Consequences:**
- Villagers never level up from trading
- Or: villagers level up too fast

**Prevention:**
1. Explicitly set rewardExp: false if using manual leveling
2. Explicitly set xp: 0 if XP should be blocked
3. Review THC design: is XP from trades disabled or not?

**Detection:** Trade 100 times with novice, check if they gained XP

**Which phase should address:** Phase 5 (Custom Trade Tables) - trade definition format

---

## THC-Specific Considerations

### Existing Pattern Interactions

| THC Feature | Interaction with Villager Overhaul |
|-------------|-----------------------------------|
| Village POI blocking (ServerLevelPoiMixin) | Works in claimed chunks; villages unaffected - need separate handling |
| Brain memory blocking (BrainPoiMemoryMixin) | Same as above - claimed chunk specific |
| Villager schedule override (VillagerMixin) | Uses registerBrainGoals; profession changes shouldn't affect |
| AbstractVillager trade filtering | Already removes shields/bells/saddles - extend this pattern |
| Stage system | Will gate villager levels - need level unlock at specific stages |

### Recommended Testing Checklist

Before shipping each phase:

- [ ] Fresh villager gains correct profession (allowed only)
- [ ] Existing villager (pre-mod) handles gracefully
- [ ] Zombie cure doesn't bypass restrictions
- [ ] Trade cycling syncs client/server
- [ ] Manual level-up works at all stages
- [ ] Structure locator maps generate without freeze
- [ ] Wandering trader unaffected by villager restrictions
- [ ] Village villagers vs base villagers behave correctly
- [ ] XP gain matches leveling design
- [ ] All 4 professions have trades at all 5 levels

## Sources

### Verified (MEDIUM confidence)
- [Minecraft Wiki - Trading](https://minecraft.wiki/w/Trading) - XP thresholds, restock mechanics
- [FabricMC/fabric Issue #4456](https://github.com/FabricMC/fabric/issues/4456) - Trade mixin null handling
- [Liberty's Villagers](https://modrinth.com/mod/libertyvillagers) - POI configuration patterns
- [VillagerConfig](https://github.com/DrexHD/VillagerConfig) - Trade customization via datapack patterns

### Partially Verified (LOW confidence)
- Minecraft Forum discussions on trade modification
- CurseForge mod descriptions for trade replacement approaches
- WebSearch results on gossip/reputation systems

### THC Codebase (HIGH confidence)
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/AbstractVillagerMixin.java` - existing trade filtering
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/VillagerMixin.java` - schedule override pattern
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/ServerLevelPoiMixin.java` - POI blocking in claims
- `/mnt/c/home/code/thc/src/main/java/thc/mixin/BrainPoiMemoryMixin.java` - Memory blocking in claims
