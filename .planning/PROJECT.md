# THC (True Hardcore)

## What This Is

A Minecraft mod that creates a "true hardcore" experience where players must always take meaningful risks to achieve anything. The mod replaces tedium-as-difficulty with risk-as-progression through multiple interconnected systems including combat overhaul (buckler replacing shields) and territorial base claiming mechanics.

## Core Value

Risk must be required for progress. No tedious grinding to avoid challenge - players face meaningful choices where reward demands exposure to danger.

## Requirements

### Validated

- ✓ Buckler combat system — existing
  - Five tiers (stone through netherite) with poise mechanics
  - Parry windows, damage reduction, durability system
  - HUD overlay showing poise bar above armor
  - Environmental damage exclusions
  - Lethal parry save mechanism
- ✓ Shield replacement — existing
  - Shields removed from all loot tables
  - Bucklers registered and craftable
- ✓ World mechanics — existing
  - Time locked to night
- ✓ Testing infrastructure — existing
  - Smoke test system (100 tick validation)
  - Game tests for buckler mechanics
  - Debug output gated by tags

### Active

- [ ] Base claiming system (land plots via bells)
  - First bell ring drops "land plot" book
  - Land plot consumed to claim chunks with terrain restrictions
  - Base chunks allow unrestricted building
  - Village chunk protection (blocks cannot be destroyed except ores/allowlist)
  - Restricted block placement outside bases (allowlist only)
  - Placement separation rules (blocks cannot be adjacent)
  - No violence within base chunks
  - Adjacent chunk claiming support

### Out of Scope

- Vanilla hardcore mode integration — THC defines its own hardcore ruleset independent of vanilla permadeath
- Multiplayer territory conflict resolution — focus is single-player or cooperative server experience for now
- Buckler visual effects beyond existing implementation — parry system complete as-is

## Context

**Technical Environment:**
- Minecraft 1.21.11
- Fabric Loader 0.18.4+
- Fabric API + Fabric Language Kotlin
- Java 21+
- Existing codebase with working buckler system

**Design Philosophy:**
- Risk/reward over tedium/grind
- Multiple interconnected systems (combat, territory, future additions)
- Implementation and design execute in parallel - new systems added iteratively as designed
- Game tests preferred for verification, manual testing as fallback

**Existing Architecture:**
- Kotlin + Java mixed codebase
- Mixins for vanilla behavior modification (LivingEntityMixin, ServerPlayerMixin, etc.)
- Attachment API for player state (THCAttachments.java)
- Client/server networking for state sync (BucklerStatePayload)
- Fabric GameTest framework integration
- Data generation for recipes/models

## Constraints

- **Minecraft API conventions**: Research best practices frequently - Fabric/Minecraft conventions change between versions and documentation is incomplete. Avoid failed iteration cycles by validating approaches before implementation.
- **Testing**: Implement game tests where possible. Run existing smoke tests before claiming implementation complete.
- **Performance**: Chunk checking for base/village status happens frequently during gameplay. Needs efficient lookup.
- **Compatibility**: Must work with Minecraft 1.21.11 Fabric - no version drift.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Buckler replaces shields entirely | Supports risk/reward philosophy - active defense (parry) vs passive (shield block) | ✓ Good - completed and tested |
| Night-only world | Forces exposure to hostile mobs, eliminates safe periods | — Pending gameplay validation |
| Iterative system design | Allows parallel design/implementation, faster iteration | — Pending - first iteration (base claiming) in progress |
| Attachments for player state | Fabric API standard for entity data persistence | ✓ Good - clean sync mechanism |
| GameTest over manual testing | Automated verification prevents regressions | ✓ Good - catches bugs early |

---
*Last updated: 2026-01-15 after initialization*
