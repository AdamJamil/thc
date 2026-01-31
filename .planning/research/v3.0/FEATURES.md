# Feature Landscape: Revival System (v3.0)

**Domain:** Cooperative revival/DBNO (Down But Not Out) mechanics
**Researched:** 2026-01-31
**Confidence:** MEDIUM-HIGH
**Target Milestone:** v3.0 Revival System

---

## Executive Summary

Revival/DBNO systems are well-established in multiplayer games with clear patterns. Research reveals THC's design (invulnerable, indefinite duration, sneak-to-revive) is simpler than most implementations, which is appropriate for a cooperative PvE mod. Key considerations are edge case handling (all players downed, environmental hazards, disconnect scenarios) and integration with existing THC systems (class multiplier for Support, threat system interaction, base protection zones).

**Key findings:**
- Invulnerability + mob ignore is standard for cooperative games (prevents frustrating death loops)
- Indefinite downed duration is unusual (most games use bleedout timers) but valid for cooperative play
- Progress preservation on interruption is a differentiator (most games reset progress)
- Proximity-based revival (sneak within 2 blocks) is simpler than held-interaction patterns
- Support class 2x revival speed is meaningful differentiation

---

## Table Stakes

Features players expect from any revival system. Missing these makes the system feel broken.

| Feature | Why Expected | Complexity | Dependencies | Notes |
|---------|--------------|------------|--------------|-------|
| Downed state on lethal damage | Core mechanic | **MEDIUM** | Damage intercept | Must intercept death, not just damage |
| Visual downed indicator | Players need to see who needs help | **LOW** | Client sync | Laying pose, particles, or outline |
| Revival progress feedback | Reviver needs to know it's working | **MEDIUM** | Client networking | Radial bar per spec |
| Invulnerability while downed | Prevents death loop frustration | **LOW** | Damage cancellation | Standard in cooperative games |
| Mob ignore while downed | Mobs shouldn't pile on helpless player | **MEDIUM** | Threat/targeting hook | Clear threat, prevent re-targeting |
| Action lockout while downed | Cannot attack, move, use items | **MEDIUM** | Input interception | Preserve camera control only |
| Revival outcome (HP/hunger) | Must restore to playable state | **LOW** | Health/food API | 50% HP, 0 hunger per spec |
| Completion feedback | Clear signal revival succeeded | **LOW** | Particles/sound | Green particles per spec |

### Downed State Trigger (MEDIUM Complexity)

**Expected behavior:** When player would die (HP reaches 0), enter downed state instead of death.

**Implementation approach:**
- Hook into damage calculation at the point where death would occur
- Check if player is already downed (prevent re-triggering)
- Cancel death, set HP to small positive value (0.5 hearts or similar)
- Apply downed state via attachment
- Clear existing mob targets/threat on this player

**Edge cases:**
- Void damage: Should this bypass downed state? (Recommendation: YES - void is unrevivable)
- `/kill` command: Should this bypass? (Recommendation: YES - explicit death request)
- Already downed: Cannot be downed again (invulnerable)
- Totem of Undying: THC replaces with Blast Totem - verify no interaction

**Verified patterns:**
- [Down But Not Out mod](https://modrinth.com/mod/down-but-not-out): "Instead of dying, players drop to half a heart and crawl"
- [Hardcore Revival mod](https://modrinth.com/mod/hardcore-revival): "When a player dies, instead of being gone for good, they enter a K.O. state"

**Confidence:** HIGH (well-established pattern)

---

### Mob Ignore Behavior (MEDIUM Complexity)

**Expected behavior:** Mobs stop targeting downed players, do not re-acquire them as targets.

**Implementation approach:**
- When player enters downed state, clear all threat on all mobs for this player
- Set player's threat generation to 0 while downed
- Optionally: mark player as "invalid target" for AI targeting checks

**THC-specific integration:**
- Existing threat system tracks per-mob threat maps
- On downed: iterate all loaded mobs, clear this player from threat maps
- While downed: intercept threat propagation to exclude downed players

**Edge cases:**
- Mobs mid-attack when player downed: Should complete current attack? (Recommendation: YES, then disengage)
- AoE damage: Should AoE hit downed players? (Spec says invulnerable, so NO)
- Newly spawned mobs: Should never target downed players

**Verified patterns:**
- [Guild Wars 2](https://wiki.guildwars2.com/wiki/Aggro): "Status effects on the player, such as invincibility, sleep, and death, which make them more or less favorable to attack are factors that influence hate/aggro"
- [Revive Me mod](https://modrinth.com/mod/revive-me): "Fallen players now become invulnerable to stop mobs from targeting them"

**Confidence:** HIGH (existing threat system makes this straightforward)

---

### Revival Progress Feedback (MEDIUM Complexity)

**Expected behavior:** Reviver sees radial progress ring around cursor showing revival percentage.

**Implementation approach:**
- Server tracks revival progress per downed player (attachment)
- Client renders radial progress around crosshair when in revival range
- Progress value synced via networking (only to reviver, not globally)
- Custom textures for empty/full progress ring

**UI pattern research:**
- [UX research](https://usersnap.com/blog/progress-indicators/): "Use a progress indicator for any action that takes longer than about 1 second"
- Determinate progress (shows completion percentage) is appropriate here
- Radial/circular indicators are common for action progress in games

**Edge cases:**
- Multiple revivers: Each sees their own contribution? Or combined? (Spec unclear - recommend: each sees own progress)
- Progress while moving: Should update smoothly, not jump
- Downed player's view: Should they see who's reviving them? (Recommendation: YES, show reviver nameplate or indicator)

**Confidence:** MEDIUM (requires client rendering, texture assets)

---

## Differentiators

Features that set THC apart from typical revival systems. These are design choices, not expectations.

| Feature | Value Proposition | Complexity | Dependencies | Notes |
|---------|-------------------|------------|--------------|-------|
| Indefinite downed duration | No bleedout pressure | **LOW** | Timer removal | Most games have 30-60s bleedout |
| Progress preserved on interruption | Forgiving, cooperative-friendly | **LOW** | State persistence | Most games reset progress |
| Sneak-to-revive (no held key) | Simpler input model | **MEDIUM** | Sneak detection | Unusual - most use held interaction |
| Support class 2x speed | Class differentiation | **LOW** | Existing class system | 1.0/tick vs 0.5/tick |
| No crawling/limited movement | Simpler state machine | **LOW** | Movement lock | Many games allow crawling |
| Proximity-based (2 blocks) | Spatial awareness required | **LOW** | Distance check | Standard range |

### Indefinite Duration (LOW Complexity)

**Value proposition:** Removes time pressure, focuses on teamwork over urgency. Downed player waits patiently, reviver can handle threats first.

**Contrast with other games:**
- [Down But Not Out](https://modrinth.com/mod/down-but-not-out): "45 seconds by default" bleedout timer
- [Hardcore Revival](https://modrinth.com/mod/hardcore-revival): Timer-based with "consecutiveKnockoutThresholdSeconds" config
- [Project Lazarus](https://project-lazarus.fandom.com/wiki/Downed_Players): "45 seconds" bleedout

**THC rationale:**
- THC is about risk/reward, not time pressure
- Indefinite duration means the decision is "can we clear threats and revive" not "can we revive fast enough"
- Fits cooperative philosophy - no penalty for cautious play

**Implementation:** Simply don't implement a decay timer. Downed state persists until revived.

**Edge cases:**
- AFK downed player: Should they be kicked after long period? (Recommendation: NO - server handles AFK separately)
- All players downed: See "All Players Downed" edge case below

**Confidence:** HIGH (simpler than timed alternative)

---

### Progress Preserved on Interruption (LOW Complexity)

**Value proposition:** Reviver can start, deal with threat, return without losing work. Reduces frustration.

**Contrast with other games:**
- Most games reset progress to 0 when reviver stops (punishes interruption)
- Some games have partial decay (progress decreases slowly)

**THC specification:** "preserved on interruption" - progress stays at current value.

**Implementation:**
- Store progress as attachment on downed player (not reviver)
- When reviver leaves range or stops sneaking, progress stays
- Any player can continue from current progress
- Progress only resets on successful revival

**Edge cases:**
- Multiple revivers sequentially: Second reviver continues from first's progress (intentional)
- Reviver downed during revival: Progress preserved for other revivers

**Confidence:** HIGH (simpler than decay systems)

---

### Sneak-to-Revive Input Model (MEDIUM Complexity)

**Value proposition:** Simpler than "hold right-click" pattern. Player sneaks within range and stays still.

**Specification details:**
- Must be within 2 blocks of downed player
- Must be sneaking
- Must stay still (no movement keys)

**Implementation approach:**
- Tick handler checks: is player sneaking? is player still? is player in range of downed player?
- If all conditions met: increment progress by rate (0.5/tick normal, 1.0/tick Support)
- If any condition fails: stop incrementing (but preserve progress)

**Comparison with hold-interaction:**
- Hold right-click: Explicit action, can conflict with other interactions
- Sneak + still: Implicit action, less prone to conflicts, but less explicit feedback

**Edge cases:**
- Player sneaking for other reasons: Only triggers if in range of downed player
- Sneaking while holding weapon: Should this work? (Recommendation: YES - no item requirement)
- Looking away from downed player: Should this work? (Recommendation: YES - proximity only)

**Confidence:** MEDIUM (unusual pattern, may need iteration)

---

### Support Class Revival Speed (LOW Complexity)

**Value proposition:** Gives Support class unique utility beyond damage multipliers.

**Specification:**
- Normal revival: 0.5 progress/tick
- Support class: 1.0 progress/tick (2x speed)

**Implementation:**
- Check reviver's class via existing ClassManager
- If SUPPORT, use enhanced rate
- Progress to 100 (so at 0.5/tick = 200 ticks = 10 seconds; at 1.0/tick = 100 ticks = 5 seconds)

**Existing dependency:** PlayerClass.SUPPORT already defined with class bonuses.

**Edge cases:**
- Multiple revivers with different classes: Each contributes their own rate? Or only one reviver at a time? (Spec unclear - recommend: only one reviver, Support takes priority if multiple)

**Confidence:** HIGH (simple rate check, existing class system)

---

## Anti-Features

Features to explicitly NOT build. Common in other revival systems but wrong for THC.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Bleedout timer | Adds time pressure, conflicts with risk/reward philosophy | Indefinite duration per spec |
| Crawling while downed | Adds complexity, allows cheese (crawl to safety) | Full immobility per spec |
| Self-revive capability | Undermines cooperative requirement | Require another player always |
| Actions while downed | Undermines "helpless" state | Complete action lockout |
| Downed player damage contribution | Undermines threat clearing incentive | No weapons/abilities while downed |
| Revive items (medkits, defibs) | Adds item economy complexity | Sneak-proximity only |
| Reviver vulnerability (slow movement) | Punishes helping teammates | Normal movement while reviving |
| Knockdown shield equivalent | Adds item tier complexity | Invulnerability is automatic |
| Death message on downed | Confusing - player isn't dead | Custom "downed" message or none |

### Why NOT Crawling

**The trap:** Many games allow downed players to crawl slowly to safety or toward teammates.

**Why wrong for THC:**
- Undermines the "helpless" state - player has agency
- Allows crawling behind cover, reducing urgency for teammates
- Adds animation/movement complexity
- Conflicts with "laying on ground" visual

**THC approach:** Downed player is completely immobile. They can only look around (camera control preserved).

---

### Why NOT Self-Revive

**The trap:** Apex Legends had Gold Knockdown Shield self-revive. Some games allow self-revive items.

**Why wrong for THC:**
- Undermines cooperative requirement - can solo the system
- [Apex removed self-revive](https://www.pcgamesn.com/apex-legends/self-revive-removed) due to competitive issues
- THC philosophy is "you need teammates" - self-revive contradicts this

**THC approach:** Revival always requires another player. No solo option.

---

### Why NOT Actions While Downed

**The trap:** Apex Legends allows secondary weapon fire while downed. Some games allow abilities.

**Why wrong for THC:**
- Downed should mean "helpless, dependent on teammates"
- Actions while downed reduce penalty for being downed
- Complicates mob ignore logic (should mobs re-target if downed player attacks?)

**THC approach:** Complete action lockout. Downed player can only observe and wait.

---

## Edge Cases

Critical scenarios that need explicit handling.

### All Players Downed

**Scenario:** Every player on the server is in downed state. No one can revive anyone.

**Options:**
1. **Instant death for all** - Game over, everyone dies
2. **Delayed death for all** - After N seconds with no revival possible, all die
3. **Last player gets second chance** - Final player to be downed gets brief window to recover
4. **Nothing special** - Players stay downed indefinitely until someone logs in

**Recommendation:** Option 2 - Delayed death (30-60 seconds). If all players downed and no new player joins to help, all die. This:
- Provides clear endpoint
- Allows time for friend to log in
- Prevents infinite limbo state

**Implementation:**
- Track count of downed vs alive players
- When alive count reaches 0, start server-wide timer
- If any player revives (or new player joins), cancel timer
- On timer expiry, all downed players die

**Verified patterns:**
- [Dawn of War II](https://www.gamedev.net/forums/topic/537640-respawn-in-multiplayer-coop-games/): "If every hero is downed, the level ends with everyone getting an emergency teleport"

**Confidence:** MEDIUM (design decision needed)

---

### Player Disconnect While Downed

**Scenario:** Downed player disconnects or crashes mid-revival.

**Options:**
1. **Die on disconnect** - Treat as giving up
2. **Preserve state** - Rejoin still downed, progress preserved
3. **Auto-revive on rejoin** - Rejoin at revival outcome state

**Recommendation:** Option 2 - Preserve state. Player rejoins still downed at same location with same revival progress.

**Implementation:**
- Downed state stored as attachment with `copyOnDeath()` disabled but `persistent()` enabled
- On player join, check if they have downed attachment
- If downed, restore downed state (pose, invulnerability, action lock)

**Verified issues:**
- [Hardcore Revival bug](https://github.com/txnimc/Spice-of-Life-Apple-Pie/issues/19): "if you are knocked down with Hardcore Revival and leave the game, you can bypass the revive system and respawn with half a heart"

**THC must explicitly handle this to prevent bypass.**

**Confidence:** HIGH (must address to prevent exploit)

---

### Downed in Hazardous Location

**Scenario:** Player downed while in lava, water, or other environmental hazard.

**Sub-cases:**

**Lava:**
- Player is invulnerable, so lava damage ignored
- Iron boat usage: Can downed player stay in iron boat? (Recommendation: NO - ejected on downed)
- Visual: Player sinks into lava, may be hard to see

**Water (Drowning):**
- Player is invulnerable, so drowning ignored
- Air meter: Freeze at current value? Or ignore entirely?
- Visual: Player floating/sinking in water

**Void:**
- Recommendation: Void bypasses downed state, causes instant death
- Rationale: Cannot be revived in void, infinite fall is not meaningful state

**Fire:**
- Player is invulnerable, so fire damage ignored
- Visual: Player may still show fire animation (cosmetic only)

**Recommendation:**
- Invulnerability handles damage from all sources except void
- Void damage causes instant death (bypasses downed)
- Visual hazards (fire, in lava) remain but are cosmetic

**Confidence:** MEDIUM (void bypass is design decision)

---

### Downed Near Mob Spawner/High Threat Area

**Scenario:** Player downed in area where mobs continuously spawn. Reviver enters danger to help.

**Design consideration:** This is working as intended. The risk/reward is:
- Clear mobs first, then revive safely (slow but safe)
- Revive quickly while taking damage (fast but risky)

**Reviver is NOT invulnerable.** They can be downed too, leading to "All Players Downed" scenario.

**No special handling needed** - this is emergent gameplay from the system.

---

### Revival Progress at 99% When Reviver Downed

**Scenario:** Reviver at 99% progress, gets hit and downed themselves.

**Behavior:** Progress preserved at 99%. Another player can complete the final 1%.

**Edge case within edge case:** If only two players and reviver downed at 99%, see "All Players Downed."

---

### Downed in Claimed Base Chunk

**Scenario:** Player downed inside their claimed base (where combat is normally blocked).

**THC context:** Bases have "No violence indoors!" - combat blocked.

**Questions:**
- Can players BE downed in bases? (Combat blocked means damage blocked?)
- If somehow downed in base, does revival work normally?

**Recommendation:**
- Damage that WOULD down the player is blocked in bases (existing combat block)
- If player is downed OUTSIDE and walks into base... they're already downed, state persists
- Revival works normally in bases (it's cooperative, not combat)

**Confidence:** MEDIUM (needs verification with existing base protection logic)

---

### Multiple Revivers on Same Target

**Scenario:** Two or more players try to revive the same downed player simultaneously.

**Options:**
1. **Fastest wins** - First to start gets exclusive revival
2. **Combined progress** - Both contribute, revival is faster
3. **Independent attempts** - Each tracks their own progress, first to 100% wins

**Recommendation:** Option 1 - First reviver has exclusivity. Simpler implementation, clearer feedback.

**Implementation:**
- When revival starts, store reviver UUID on downed player
- Other players in range see "Being revived by [name]" instead of progress
- If active reviver leaves/stops, lock releases, next player can take over

**Confidence:** MEDIUM (design decision, affects UI)

---

### Downed Player Receives Damage While Being Revived

**Scenario:** AoE attack or stray projectile hits downed player during revival.

**Behavior:** Downed player is invulnerable. Damage is ignored. Revival continues.

**Edge case:** What if the damage is from the reviver? (Friendly fire)
- Still ignored. Invulnerability is absolute while downed.

---

## Feature Dependencies

```
Existing THC Systems
    |
    +-- Class System (ClassManager, PlayerClass.SUPPORT)
    |       |
    |       v
    |   Support Revival Speed Bonus
    |
    +-- Threat System (ThreatManager, MOB_THREAT attachment)
    |       |
    |       v
    |   Clear Threat on Downed
    |   Prevent Threat Generation While Downed
    |
    +-- Attachment System (THCAttachments)
    |       |
    |       v
    |   Downed State Attachment
    |   Revival Progress Attachment
    |
    +-- Base Protection (claimed chunks, combat block)
            |
            v
        Interaction with Downed State in Bases

New Revival System
    |
    +-- Core: Downed State Management
    |       |
    |       +-- Damage Intercept (death -> downed)
    |       +-- Action Lock (no movement, no actions)
    |       +-- Invulnerability (damage ignored)
    |       +-- Visual State (laying pose)
    |
    +-- Core: Revival Mechanic
    |       |
    |       +-- Proximity Detection (2 blocks)
    |       +-- Sneak + Still Detection
    |       +-- Progress Tracking (per downed player)
    |       +-- Class-Based Speed
    |
    +-- Core: Revival Outcome
    |       |
    |       +-- State Restoration (standing, action unlock)
    |       +-- Health Setting (50% max HP)
    |       +-- Hunger Setting (0)
    |       +-- Visual Feedback (green particles)
    |
    +-- UI: Progress Display
    |       |
    |       +-- Client Networking (progress sync)
    |       +-- Radial Renderer (custom textures)
    |       +-- Downed Player Indicator
    |
    +-- Edge Cases
            |
            +-- All Players Downed Handler
            +-- Disconnect/Rejoin Handler
            +-- Void Death Bypass
```

---

## MVP Recommendation

For MVP implementation, prioritize in this order:

**Phase 1 - Core State (must have for any testing):**
1. Downed state attachment and management
2. Death intercept -> downed transition
3. Invulnerability while downed
4. Action lockout while downed
5. Basic visual indicator (can be particles, formal pose later)

**Phase 2 - Revival Mechanic:**
6. Proximity + sneak + still detection
7. Progress tracking and increment
8. Revival completion and state restoration
9. Health/hunger outcome application
10. Green particle completion feedback

**Phase 3 - Integration:**
11. Threat clearing on downed
12. Threat generation prevention while downed
13. Support class speed bonus
14. Progress preservation on interruption

**Phase 4 - UI/Polish:**
15. Radial progress bar (client rendering)
16. Custom textures for progress ring
17. Downed player indicator for teammates

**Phase 5 - Edge Cases:**
18. All players downed handler
19. Disconnect/rejoin preservation
20. Void death bypass

---

## Complexity Summary

| Complexity | Features |
|------------|----------|
| **LOW** | Invulnerability, action lockout, indefinite duration, progress preservation, Support speed bonus, revival outcome, completion particles |
| **MEDIUM** | Downed state trigger, mob ignore/threat clear, sneak+still detection, progress tracking, disconnect handling |
| **HIGH** | Radial UI rendering, laying pose animation, all players downed handler |

---

## Sources

### Primary (HIGH Confidence)
- [Down But Not Out - Modrinth](https://modrinth.com/mod/down-but-not-out) - Fabric revival mod with detailed mechanics
- [Hardcore Revival - Modrinth](https://modrinth.com/mod/hardcore-revival) - Alternative revival mod for comparison
- [Guild Wars 2 Aggro Wiki](https://wiki.guildwars2.com/wiki/Aggro) - Aggro/targeting mechanics with downed state
- Existing THC codebase (THCAttachments, ClassManager, ThreatManager)

### Secondary (MEDIUM Confidence)
- [Oreate AI - Understanding DBNO](https://www.oreateai.com/blog/understanding-dbno-the-down-but-not-out-status-in-gaming/b928ab691510dacc847c4c738d251e77) - DBNO pattern overview
- [Epic Games - DBNO Devices](https://dev.epicgames.com/documentation/en-us/fortnite/using-down-but-not-out-devices-in-fortnite-creative) - Fortnite DBNO documentation
- [TV Tropes - Combat Resuscitation](https://tvtropes.org/pmwiki/pmwiki.php/Main/CombatResuscitation) - Pattern catalog
- [Game Wisdom - Cooperative Game Design](https://game-wisdom.com/critical/cooperative-game-design) - Cooperative design challenges

### Tertiary (LOW Confidence - WebSearch patterns)
- [ResetEra DBNO Discussion](https://www.resetera.com/threads/is-the-down-but-not-out-dbno-last-stand-mechanic-the-best-addition-to-multiplayer-games-in-the-last-ten-or-so-years.1354924/) - Community perspectives
- [Apex Legends Self-Revive Removal](https://www.pcgamesn.com/apex-legends/self-revive-removed) - Why self-revive was removed
- [GameDev.net Respawn Discussion](https://www.gamedev.net/forums/topic/537640-respawn-in-multiplayer-coop-games/) - Multiplayer respawn patterns

---

## Open Questions for Roadmap

1. **All players downed behavior:** Confirm delayed death (30-60s) or alternative approach
2. **Multiple revivers:** Confirm first-reviver exclusivity or combined progress
3. **Void bypass:** Confirm void damage causes instant death (bypasses downed)
4. **Base chunk interaction:** Verify downed state interaction with combat blocking
5. **Progress display for downed player:** Should downed player see their own revival progress?
6. **Laying pose implementation:** Mixin vs entity state vs custom rendering?

---

## Metadata

**Research type:** Feature Landscape
**Research date:** 2026-01-31
**Valid until:** ~90 days (stable Minecraft 1.21.x)
**Downstream consumer:** Roadmap creation for v3.0 milestone
