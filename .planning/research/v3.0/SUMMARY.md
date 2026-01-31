# Project Research Summary

**Project:** THC v3.0 Revival System
**Domain:** Minecraft Fabric mod - revival/DBNO (Down But Not Out) mechanics
**Researched:** 2026-01-31
**Confidence:** HIGH

## Executive Summary

The revival system is a well-established pattern in cooperative games with clear implementation approaches for Minecraft. The research reveals that THC's architecture is already ideally suited for this feature - no new dependencies are required. The mod's existing patterns (Fabric Attachment API for state, server tick events for progress tracking, CustomPacketPayload for sync, HUD rendering via Fabric API) map directly to revival system requirements.

The recommended implementation uses Fabric API's `ServerLivingEntityEvents.ALLOW_DEATH` to intercept lethal damage, stores downed state via non-persistent attachments, enforces `Pose.SWIMMING` for visual crawling effect, and modifies mob AI targeting via mixin to exclude downed players. The Support class integration for 2x revival speed leverages the existing class system with minimal additions. The indefinite downed duration (no bleedout timer) and preserved-on-interruption progress are THC-specific design choices that simplify implementation compared to typical revival mods.

Key risks center on death interception race conditions (if health reaches 0 before interception, player dies despite downed state), client-server pose desync (visual mismatch breaks immersion), and incomplete action blocking (downed players exploiting allowed actions). These are all mitigated through established patterns from THC's existing systems (lethal parry pattern for damage interception, explicit sync packets like BucklerStatePayload, event-based action blocking like BasePermissions).

## Key Findings

### Recommended Stack

No new dependencies required. All capabilities exist in the current THC stack (Fabric API 1.21.11, Mixins, Attachments). The key technical decisions leverage existing Fabric API components that are already in the project dependencies.

**Core technologies:**
- **Fabric API `ServerLivingEntityEvents.ALLOW_DEATH`**: Death interception - preferred over mixin for mod compatibility, provides damage source and amount, already part of dependencies
- **Fabric Attachment API**: Downed state persistence - matches existing THC patterns (BUCKLER_POISE, PLAYER_CLASS, MOB_THREAT), non-persistent for session-scoped state
- **Fabric Networking (`ServerPlayNetworking`)**: Client state sync - matches existing BucklerSync pattern with CustomPacketPayload and StreamCodec
- **Fabric HUD API (`HudElementRegistry`)**: Radial progress rendering - matches existing BucklerHudRenderer pattern, requires custom textures for progress ring

**Mixin targets (verified against Mojang mappings 1.21.11):**
- `LivingEntity.canBeSeenAsEnemy()`: Return false for downed players to exclude from mob targeting
- `LivingEntity.isInvulnerableTo(DamageSource)`: Return true for downed players to prevent all damage
- `Player` action methods: Block item use, attacks, movement while downed
- `Entity.setPose(Pose)`: Enforce `Pose.SWIMMING` via tick handler to maintain crawling visual

### Expected Features

Research reveals THC's design is simpler than most implementations, which is appropriate for cooperative PvE. The indefinite downed duration (versus typical 30-60s bleedout timers) and progress preservation on interruption are differentiators that reduce complexity and align with THC's risk/reward philosophy.

**Must have (table stakes):**
- Downed state on lethal damage - intercept death, enter downed instead
- Visual downed indicator - laying pose (Pose.SWIMMING), particles, or outline
- Revival progress feedback - radial bar around crosshair for reviver
- Invulnerability while downed - prevents death loop frustration
- Mob ignore while downed - clear threat, prevent re-targeting
- Action lockout while downed - cannot attack, move, use items (camera control preserved)
- Revival outcome (50% HP, 0 hunger) - restore to playable state
- Completion feedback - green particles on successful revival

**Should have (competitive):**
- Support class 2x revival speed - 1.0 progress/tick vs 0.5/tick base rate
- Progress preserved on interruption - forgiving, cooperative-friendly
- Sneak-to-revive input model - simpler than held-interaction patterns
- Indefinite downed duration - no bleedout timer, focus on teamwork over urgency

**Defer (edge cases for later phases):**
- All players downed handler - delayed death (30-60s) if no living players remain
- Disconnect/rejoin preservation - downed state persists through disconnect (prevent exploit)
- Void damage bypass - instant death from void (cannot be revived in void)
- Downed player visual enhancements - custom rendering, glowing outline for teammates

### Architecture Approach

The revival system integrates cleanly with THC's existing architecture through established patterns. Death interception uses Fabric API events (not mixins) to remain mod-compatible. State storage follows the attachment patterns already used for poise, class, and threat. Progress tracking mirrors the buckler poise system's tick-based updates with delta sync to clients. HUD rendering follows the existing BucklerHudRenderer approach but renders at cursor center rather than status bar position.

**Major components:**
1. **RevivalManager** - Static utilities for downed state CRUD (isDowned, setDowned, revive), follows existing manager patterns
2. **RevivalTick** - Server tick handler for progress calculation (proximity check, sneak detection, class-based rate, completion check)
3. **RevivalSync** - Delta sync handler for client state (only sends when progress changes, matches BucklerSync pattern)
4. **RevivalPayload** - Network payload for progress data (CustomPacketPayload with StreamCodec, S2C direction)
5. **RevivalHudRenderer** - Client-side radial progress ring (trigonometric vertex calculation for filled arc, custom textures)
6. **DownedMobTargetMixin** - Prevent mob targeting (override canBeSeenAsEnemy to return false for downed players)
7. **DownedActionMixin** - Block player actions (intercept movement, item use, attacks while downed)

### Critical Pitfalls

The research identified 14 pitfalls across critical/moderate/minor severity. The top 5 that could cause rewrites or major issues:

1. **Death prevention race conditions** - Intercepting lethal damage at wrong point in pipeline causes player to die despite downed state. Use Fabric API `ALLOW_DEATH` event and set health > 0 immediately (follows THC's existing lethal parry pattern). Test with void damage, /kill command, high burst damage.

2. **Client-server pose desync** - Visual pose (lying down) desyncs between server and clients, breaking immersion. Set pose server-side only, use explicit sync packets (DownedStatePayload pattern like BucklerStatePayload), test in multiplayer from start.

3. **Mob AI continues targeting downed players** - Mobs crowd invulnerable body instead of attacking revivers. Modify targeting predicates to exclude downed players, clear threat on downed, integrate with existing MonsterThreatGoalMixin.

4. **Player actions not fully disabled** - Downed players can still open inventory, drop items, swap hands, breaking immersion and enabling exploits. Comprehensive action blocking list, packet-level rejection in server handler.

5. **Revival progress lost on chunk unload/rejoin** - Progress stored only in memory, lost on disconnect or chunk unload. Store progress on downed player (not reviver), make attachment persistent, handle disconnect to prevent bypass exploit.

## Implications for Roadmap

Based on research, suggested phase structure follows dependency order and risk mitigation:

### Phase 1: Core Downed State
**Rationale:** Foundation phase that establishes death interception and basic state management. Must work reliably before any other features. Addresses highest risk pitfalls (race conditions, state corruption).

**Delivers:** Players enter downed state instead of dying, basic visual indicator (particles, not formal pose yet), invulnerability active, server-side state management working.

**Addresses:** Table stakes features - downed state trigger, invulnerability, basic indicator

**Avoids:** Pitfall #1 (death race conditions) by using Fabric API ALLOW_DEATH, Pitfall #14 (attachment initialization) by following existing THC patterns

**Research flags:** Standard patterns (well-documented Fabric API). Skip research-phase.

### Phase 2: Action Blocking & Pose
**Rationale:** Downed players must be fully incapacitated before revival mechanics. Visual pose depends on action blocking (movement locked). Addresses immersion and exploit risks.

**Delivers:** Complete action lockout (movement, attacks, items, inventory), Pose.SWIMMING for crawling visual, client-server sync for pose state.

**Addresses:** Table stakes - action lockout, visual downed indicator (pose)

**Avoids:** Pitfall #2 (pose desync) via explicit sync packets, Pitfall #4 (incomplete blocking) via comprehensive packet-level rejection

**Research flags:** Moderate complexity (pose enforcement patterns, packet filtering). Standard Fabric patterns should suffice.

### Phase 3: Mob AI Integration
**Rationale:** Must happen before revival mechanics to prevent mobs crowding downed players. Integrates with existing threat system.

**Delivers:** Mobs ignore downed players, threat cleared on downed, no re-targeting while downed.

**Addresses:** Table stakes - mob ignore behavior

**Avoids:** Pitfall #3 (mob targeting) by modifying canBeSeenAsEnemy and clearing MOB_THREAT attachment

**Uses:** Existing MonsterThreatGoalMixin pattern, THCAttachments.MOB_THREAT

**Research flags:** THC-specific (threat system integration). May need research-phase to understand threat clearing across all mob types.

### Phase 4: Revival Mechanics
**Rationale:** Core revival system. Depends on downed state (Phase 1), action blocking (Phase 2), mob ignore (Phase 3). Progress tracking is complex with multiple edge cases.

**Delivers:** Proximity detection (2 blocks), sneak + still detection, progress tracking per downed player, class-based speed (Support 2x), revival completion (50% HP, 0 hunger), progress preservation on interruption.

**Addresses:** Table stakes - revival progress, outcome, completion feedback (green particles). Competitive features - Support class bonus, progress preservation, sneak-to-revive.

**Avoids:** Pitfall #5 (progress loss) by storing on downed player with persistent attachment, Pitfall #7 (wrong class check) by checking reviver's class, Pitfall #11 (range edge cases) via Euclidean distance check

**Uses:** Existing ClassManager for Support bonus, attachment patterns for progress storage, server tick events for progress calculation

**Implements:** RevivalManager, RevivalTick components

**Research flags:** Complex logic (multi-player state, progress preservation). Standard patterns but needs careful implementation.

### Phase 5: Client Sync & HUD
**Rationale:** Visual feedback for revivers. Depends on revival mechanics producing progress data. Client rendering is isolated from server logic.

**Delivers:** RevivalPayload network sync, RevivalClientState storage, radial progress ring around crosshair, custom textures (revival_progress_empty/full.png).

**Addresses:** Table stakes - revival progress feedback

**Avoids:** Pitfall #6 (HUD positioning) via dynamic centering and GUI scale testing, Pitfall #9 (visuals not visible to others) by server-side particle spawning

**Uses:** Existing BucklerSync/BucklerStatePayload pattern, HudElementRegistry pattern from BucklerHudRenderer

**Implements:** RevivalSync, RevivalPayload, RevivalHudRenderer components

**Research flags:** Client rendering (radial progress via trigonometry). Moderate complexity, may need research-phase for arc rendering techniques.

### Phase 6: Edge Cases & Polish
**Rationale:** Handle uncommon scenarios that didn't block core implementation. Lower priority, can be deferred or handled incrementally.

**Delivers:** All players downed handler (delayed death after 30-60s), disconnect/rejoin preservation, void damage bypass (instant death), special damage source handling (DamageTypeTags.BYPASSES_INVULNERABILITY), downed player HUD (show revival progress from their perspective), sound effects, additional particle effects.

**Addresses:** Deferred features - all players downed, disconnect handling, void bypass, downed player visuals

**Avoids:** Pitfall #8 (invulnerability bypass) via damage source tag checks, Pitfall #12 (state residue) via explicit clearDownedState method

**Research flags:** Edge case testing. Each sub-feature may need design decisions (e.g., all players downed behavior).

### Phase 7: Testing & Integration Validation
**Rationale:** Final validation across all THC systems. Multiplayer testing at scale, mod compatibility verification.

**Delivers:** Solo play testing, multiplayer stress testing, base claiming interaction verification (combat blocked in bases), existing system integration (buckler, threat, class), mod compatibility validation.

**Addresses:** Integration concerns - buckler system (can't raise while downed), threat system (cleared on downed), saturation healing (0 hunger = no healing until eating)

**Avoids:** All pitfalls via comprehensive testing scenarios

**Research flags:** Minimal. Testing phase focuses on execution, not research.

### Phase Ordering Rationale

- **Foundation first (Phase 1-2):** Core state management and action blocking must be rock-solid before any other features. These address the highest-risk pitfalls (race conditions, state corruption).
- **Dependencies clear (Phase 3-4):** Mob AI integration before revival mechanics prevents mobs interfering with revival testing. Revival mechanics depend on downed state working reliably.
- **Client isolated (Phase 5):** HUD rendering can be developed independently once server-side progress tracking exists. Delta sync pattern is well-established in THC.
- **Polish deferred (Phase 6-7):** Edge cases and testing don't block core functionality. Can be addressed incrementally or deferred to later milestones if needed.

This ordering minimizes rework by validating each layer before building on it, and groups related functionality (state management, AI integration, client rendering) to maintain context.

### Research Flags

**Phases likely needing deeper research during planning:**
- **Phase 3 (Mob AI Integration):** THC-specific threat system integration. Need to understand how to clear threat across all mob types, verify MOB_THREAT attachment usage, ensure compatibility with MonsterThreatGoalMixin. May need research-phase to analyze existing threat code.
- **Phase 5 (Client Sync & HUD):** Radial progress ring rendering. While pattern exists (BucklerHudRenderer), the radial arc geometry is new. May need research-phase for trigonometric vertex calculation, texture UV mapping, circle rendering at different GUI scales.
- **Phase 6 (Edge Cases):** Each sub-feature (all players downed, disconnect handling, void bypass) may need design decision research rather than technical research. "How should this work?" rather than "how to implement?"

**Phases with standard patterns (skip research-phase):**
- **Phase 1 (Core Downed State):** Fabric API ALLOW_DEATH is well-documented, attachment patterns established in THC, death interception follows existing lethal parry pattern.
- **Phase 2 (Action Blocking & Pose):** Pose enforcement via setPose is standard Minecraft, packet filtering has examples in THC codebase, sync pattern matches BucklerStatePayload.
- **Phase 4 (Revival Mechanics):** Progress tracking mirrors buckler poise system, class integration uses existing ClassManager, attachment persistence is established pattern.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **HIGH** | All mixin targets verified against Mojang mappings 1.21.11, Fabric API events verified via official docs, no new dependencies needed |
| Features | **HIGH** | Revival/DBNO patterns well-established in cooperative games, multiple Minecraft revival mods provide reference implementations, edge cases identified from existing mods |
| Architecture | **HIGH** | All patterns already exist in THC codebase (attachments, sync, HUD, tick handlers), component boundaries clear, integration points identified |
| Pitfalls | **MEDIUM-HIGH** | Critical pitfalls identified from similar mod issues and Minecraft mechanics knowledge, mitigation strategies verified against THC patterns, some edge cases may emerge during testing |

**Overall confidence:** HIGH

All major technical questions answered with verifiable sources. The implementation approach leverages proven patterns from THC's existing systems. Risk areas (death race conditions, pose sync, mob targeting) have clear mitigation strategies drawn from existing THC code.

### Gaps to Address

Minor design decisions that need resolution during planning or early implementation:

- **All players downed behavior:** Design decision needed - delayed death (30-60s recommended), instant death, or indefinite wait for new player to join. Affects Phase 6 planning.
- **Multiple simultaneous revivers:** Design decision needed - first reviver exclusive (recommended for simpler UI), combined progress (faster but complex), or independent attempts. Affects Phase 4 implementation.
- **Downed player perspective:** Should downed player see their own revival progress? Should they see who's reviving them? Affects Phase 5 UI design.
- **Hunger value on revival:** Spec says 0 hunger. Verify this is intentional given THC's saturation-tiered healing requiring hunger >= 18. May be too punishing or intentionally forcing food consumption. Affects Phase 4 revival outcome.
- **Base chunk interaction:** Clarify if players can be downed in claimed bases (combat is blocked). If yes, does revival work normally? Affects Phase 7 integration testing.

These gaps are minor and can be resolved during phase planning. None block starting implementation.

## Sources

### Primary (HIGH confidence)
- [Fabric API ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html) - ALLOW_DEATH event signature and behavior
- [Fabric HUD Rendering Documentation](https://docs.fabricmc.net/develop/rendering/hud) - HudElementRegistry patterns
- [LivingEntity NeoForge 1.21.1 Javadocs](https://lexxie.dev/neoforge/1.21.1/net/minecraft/world/entity/LivingEntity.html) - canBeSeenAsEnemy, isInvulnerableTo verification
- [Pose Enum Spigot API](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Pose.html) - Pose.SWIMMING for crawling visual
- Existing THC codebase - `THCAttachments.java`, `BucklerSync.java`, `BucklerHudRenderer.kt`, `ClassManager.java`, `VillagerInteraction.kt`, `LivingEntityMixin.java`

### Secondary (MEDIUM confidence)
- [Down But Not Out - Modrinth](https://modrinth.com/mod/down-but-not-out) - Fabric revival mod, reference implementation, edge case handling
- [Hardcore Revival - Modrinth](https://modrinth.com/mod/hardcore-revival) - K.O. state patterns, disconnect exploit prevention
- [Guild Wars 2 Aggro Wiki](https://wiki.guildwars2.com/wiki/Aggro) - Downed state interaction with mob aggro systems
- [Fabric Mappings Wiki](https://wiki.fabricmc.net/tutorial:mappings) - Mojang mapping verification
- [Oreate AI - Understanding DBNO](https://www.oreateai.com/blog/understanding-dbno-the-down-but-not-out-status-in-gaming/b928ab691510dacc847c4c738d251e77) - DBNO pattern overview

### Tertiary (LOW confidence - community patterns)
- [FabPose mod](https://github.com/YukkuriLaboratory/FabPose) - Pose implementation examples
- [Incapacitated mod](https://modrinth.com/mod/incapacitated) - Crawling state, down counter patterns
- [Apex Legends Self-Revive Removal](https://www.pcgamesn.com/apex-legends/self-revive-removed) - Why self-revive was removed (anti-feature justification)
- [GameDev.net Respawn Discussion](https://www.gamedev.net/forums/topic/537640-respawn-in-multiplayer-coop-games/) - Multiplayer respawn patterns

---
*Research completed: 2026-01-31*
*Ready for roadmap: yes*
