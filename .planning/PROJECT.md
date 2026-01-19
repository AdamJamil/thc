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
- ✓ Base claiming system — v1.0
  - Bell rings drop land plot books (first ring per bell)
  - Land plots claim chunks with terrain flatness validation
  - Base areas allow unrestricted building
  - Combat blocked in bases ("No violence indoors!")
  - Mining fatigue outside bases (1.4^x stacking, 12s decay)
  - Village chunks protected (no breaking except ores/allowlist)
  - Allowlist-only placement outside bases with adjacency rules
  - Bells indestructible (bedrock-like hardness)
- ✓ Crafting tweaks — v1.0
  - Ladder recipe yields 16 (instead of 3)
  - Snowballs stack to 64 (instead of 16)
  - Snow block ↔ 9 snowballs conversion
- ✓ Drowning modification — v1.1
  - Drowning damage ticks every 4 seconds (instead of 1)
- ✓ Spear removal — v1.1
  - Spears removed from crafting, loot tables, and mob drops
- ✓ Projectile combat — v1.1
  - Hit effects: Speed II and Glowing (6s) on target
  - Aggro redirection to shooter
  - Enhanced physics: 20% faster launch, gravity increase after 8 blocks
- ✓ Parry stun improvements — v1.2
  - Stun range increased to 3 blocks
  - ~1 block knockback on stunned enemies
- ✓ XP economy restriction — v1.2
  - XP orbs only from mob deaths and experience bottles
  - Blocked: ores, breeding, fishing, trading, smelting
- ✓ Tiered arrows — v1.2
  - Vanilla arrow renamed to "Flint Arrow" with custom texture
  - Iron Arrow (+1 damage), Diamond Arrow (+2), Netherite Arrow (+3)
  - Anvil crafting: 64 flint arrows + material = 64 tiered arrows

### Active

None - v1.2 complete. Ready for v1.3 planning.

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
| Iterative system design | Allows parallel design/implementation, faster iteration | ✓ Good - v1.0 delivered in 3 days |
| Attachments for player state | Fabric API standard for entity data persistence | ✓ Good - clean sync mechanism |
| GameTest over manual testing | Automated verification prevents regressions | ✓ Good - catches bugs early |

## Current State

**Shipped:** v1.2 Extra Features Batch 2 (2026-01-19)

**In Progress:** None (ready for v1.3 planning)

**Codebase:**
- ~3,000 LOC Kotlin/Java
- Mixed mixin + event-driven architecture
- Accessor mixin pattern for immutable field modification
- XP blocking via HEAD cancellation and @Redirect patterns

**Known issues:** None currently tracked

**Technical debt:** None identified

---
*Last updated: 2026-01-19 after v1.2 milestone complete*
