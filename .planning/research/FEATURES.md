# Features Research: Twilight Visual System

**Researched:** 2026-01-20
**Domain:** Minecraft visual time/sky rendering, mechanic decoupling
**Confidence:** MEDIUM
**Target Milestone:** v2.0

## Summary

Perpetual twilight/dusk visual systems in Minecraft mods work by intercepting time-of-day queries at different layers. The key insight is that Minecraft uses `getTimeOfDay()` and `getDayTime()` calls throughout both rendering and game logic code. Client-side mods can override these in `ClientLevel` to affect visuals without impacting server mechanics. However, THC's requirements (bees work, monsters spawn, undead don't burn) require server-side mechanic manipulation in addition to client visual changes.

**Primary recommendation:** Use a hybrid approach - client-side mixin on `ClientLevel.getTimeOfDay()` for visuals (locked at ~13000 ticks for dusk), combined with server-side mixins targeting specific mechanic checks (bee behavior, undead burning, spawn rules).

---

## Visual Effects Expected

### Sky Appearance at Dusk (~13000 ticks)

| Element | Dusk State | Implementation Notes |
|---------|------------|---------------------|
| Sky color | Orange-red gradient on horizon, dark blue above | Tied to `getTimeOfDay()` in sky rendering |
| Sun position | Low on horizon (15-30 degrees) | Celestial angle from time value |
| Moon | Rising opposite sun, partially visible | Same celestial calculation |
| Stars | Beginning to appear | Fade in based on sky darkness |
| Clouds | Rendered with sunset coloring | Affected by ambient light |
| Fog | Warm-tinted distance fog | `getBrightnessDependentFogColor()` |

**Target tick value:** 13000 ticks (approximately 19:00 in-game, sunset period)
- This provides the iconic dusk aesthetic
- Sun still visible but low
- Stars emerging
- Warm ambient lighting

### Ambient Lighting

The game calculates ambient light based on:
1. Sky light level (internal value 0-15)
2. Time of day modifier
3. Weather modifier

At dusk (13000 ticks), the internal sky light at surface is approximately 12 (vs 15 at noon). This creates the characteristic twilight ambiance.

**Evernight mod approach (verified):** Override `getTimeOfDay()` and `getDayTime()` in `ClientLevel` to return a fixed value. This affects all rendering that queries time, including:
- Sky color calculation
- Celestial body positioning
- Fog color
- Ambient light computation

### Weather Interaction

| Weather | Visual Impact | Notes |
|---------|---------------|-------|
| Rain | Sky darkens, light level -3 | Celestial bodies hidden in Java Edition |
| Thunder | Further darkening | Internal sky light treated as 0 for spawns |
| Clear | Normal dusk rendering | Target state |

**Consideration:** Rain/thunder during perpetual dusk may look odd. Options:
- Accept the visual (rain at dusk is atmospheric)
- Override weather rendering separately
- Let rain darken as normal (maintains immersion)

---

## Mechanic Decoupling Patterns

### The Core Problem

Vanilla Minecraft uses time-of-day values for multiple purposes:
1. **Visual rendering** (sky, lighting, fog)
2. **Mob spawning rules** (hostile mobs spawn at certain light levels)
3. **Undead burning** (zombies/skeletons burn when sun is 15+ degrees above horizon)
4. **Bee behavior** (return to nest at dusk, exit at dawn)
5. **Villager schedules** (sleep, work, gather patterns)
6. **Bed usage** (available at certain tick ranges)

THC wants different "time states" for different systems:
| System | Desired Time State | Rationale |
|--------|-------------------|-----------|
| Visual | Dusk (13000) | Aesthetic goal |
| Bees | Daytime | Should work normally |
| Mob spawning | Nighttime rules | Hostile mobs should spawn |
| Undead burning | Disabled | Zombies/skeletons shouldn't burn |
| Villagers | Actual server time | Normal schedules |

### Existing Mod Approaches

**1. Client-Visual-Only (Evernight, Always Day, Client Time)**
- Hook: `ClientLevel.getTimeOfDay()`, `ClientLevel.getDayTime()`
- Scope: Client-side only
- Effect: Changes what player sees, NOT game mechanics
- Server impact: None - server time flows normally
- Multiplayer: Works without server mod

**2. Server-Time-Freeze (Time Control)**
- Hook: Uses `doDaylightCycle` gamerule
- Scope: Server-side
- Effect: Stops all time progression
- Problem: Breaks everything that depends on time progression

**3. Dimension-Specific Effects (Twilight Forest)**
- Hook: Custom dimension with hardcoded effects
- Scope: New dimension entirely
- Effect: Perpetual twilight in that dimension only
- Problem: Requires portal/dimension, not for Overworld

### THC's Required Pattern: Selective Mechanic Override

Each subsystem needs its own mixin to "lie" about time:

**For Visual (Client-Side):**
```java
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void thc$perpetualDusk(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.541666f); // ~13000/24000 = dusk
    }
}
```

**For Undead Burning (Server-Side):**
The burn check happens in `Zombie.aiStep()` (and similar for Skeleton). It checks:
1. Is it daytime? (`level.isDay()`)
2. Is sky light >= 12?
3. Is mob not under solid block?

Mixin target: The sun-brightness check method or inject into the burning logic.

**For Bee Behavior (Server-Side):**
Bees check `level.isDay()` to decide when to work. In Nether/End, they work continuously because there's no day/night cycle.

Options:
1. Make bees think it's always day (they work continuously)
2. Let server time control bees (they follow normal schedule)

**For Mob Spawning (Server-Side):**
Spawning rules already exist in `NaturalSpawner`. Light level checks use actual light values, not time directly. However, sky light varies with time of day.

If visual is dusk but server time is noon, sky light remains 15 and hostile mobs may not spawn in exposed areas. Options:
1. Override sky light calculations for spawn checks
2. Accept that spawning follows actual server time
3. Force spawn light checks to use "night" values

---

## Multiplayer Considerations

### Client-Side Only (Visual Changes)

| Aspect | Behavior | Notes |
|--------|----------|-------|
| Server mod required | NO | Client renders independently |
| Other players see | Their own time | Each client renders its own view |
| Server validation | N/A | No gameplay impact |
| Compatibility | HIGH | Works on any server |

**Implication:** If THC only ships client-side visual changes, players can join vanilla servers and see perpetual dusk while server time flows normally.

### Server-Side Only (Mechanic Changes)

| Aspect | Behavior | Notes |
|--------|----------|-------|
| Server mod required | YES | Mechanics are server-authoritative |
| Client mod required | NO for mechanics | Server controls burning/spawning |
| Visual sync | Clients see normal time | Unless client mod also installed |

**Implication:** Server can prevent undead burning and force spawn rules without client mod, but players see normal sky unless they also have client mod.

### Hybrid (THC's Requirement)

| Component | Side | Distribution |
|-----------|------|--------------|
| Dusk visual | Client | Required for intended experience |
| Undead burning disable | Server | Required for gameplay |
| Bee daytime override | Server | Required for gameplay |
| Spawn rule tweaks | Server | Optional (depends on design) |

**Recommended distribution:**
- THC mod is REQUIRED on server (mechanics)
- THC mod is REQUIRED on client (visuals)
- Same JAR contains both client and server mixins
- Use `@Environment(EnvType.CLIENT)` for client-only code

---

## Edge Cases

### Dimension Transitions

| Transition | Behavior | Handling |
|------------|----------|----------|
| Overworld -> Nether | Nether has no sky | Nether rendering unaffected |
| Overworld -> End | End has fixed sky | End rendering unaffected |
| Nether/End -> Overworld | Return to dusk visual | Client mixin re-applies |

**Implementation note:** Condition the client mixin on dimension being Overworld:
```java
if (this.dimensionType().hasSkyLight()) {
    // Apply dusk override
}
```

### Weather Effects

| Scenario | Visual | Mechanic |
|----------|--------|----------|
| Rain at dusk | Darker dusk, no sun visible | Spawn rules use rain modifiers |
| Thunder at dusk | Very dark | Hostile mobs spawn anywhere (vanilla behavior) |
| Rain clears | Return to dusk | Normal dusk lighting |

**Design decision needed:** Does rain during perpetual dusk darken sky further or maintain dusk brightness?

### Shader Compatibility

Client-side time mods (like Evernight) report compatibility with:
- Iris shaders
- Oculus shaders
- Sodium rendering

**Key:** Override happens at `getTimeOfDay()` level, which shaders also query for their sky rendering.

**Risk:** Some shaders may query time differently or cache values. Testing required.

### Sleep Mechanics

Bed availability is time-gated (available after tick 12542 in clear weather). Options:
1. Let server time control sleep availability (players can sleep at vanilla times)
2. Disable sleep entirely (perpetual twilight = eternal awakeness)
3. Make beds always usable (override availability check)

### Time Commands

`/time set` and `/time add` commands would:
- Affect server time (mechanics)
- NOT affect client visual (locked to dusk)

This may be confusing to admins. Document clearly.

### New Player Experience

Players joining see dusk immediately. If server time is noon, this creates disconnect between visual and actual mechanics (sun looks setting but it's midday). Consider:
- Informing players that visual is aesthetic only
- Ensuring key mechanics (farming, mob spawning) work as expected

---

## Reference Implementations

### Primary References (MEDIUM-HIGH Confidence)

**1. Evernight Mod**
- Platform: Forge and Fabric
- Technique: `ClientLevel.getTimeOfDay()` and `getDayTime()` override
- Config: Set target tick value and affected dimensions
- Compatibility: Works with shaders
- Source: Closed source, but documented behavior
- URL: https://www.curseforge.com/minecraft/mc-mods/evernight

**2. Twilight Forest**
- Platform: Forge (NeoForge), Fabric port exists
- Technique: Custom dimension with hardcoded perpetual twilight
- Sky: Custom `TFSkyRenderer` class
- Source: Open source on GitHub
- URL: https://github.com/TeamTwilight/twilightforest
- Limitation: Dimension-specific, not Overworld modification

**3. Always Day / Client Time Mods**
- Platform: Fabric
- Technique: Lock visual time to specific value
- Scope: Client-side only
- Source: Various implementations on Modrinth
- URL: https://modrinth.com/mod/always-day

### Secondary References (MEDIUM Confidence)

**4. Daylight Mobs Reborn**
- Platform: Forge/Fabric
- Technique: Prevents undead burning, allows daytime spawning
- Relevance: Shows mechanic override patterns
- URL: https://www.curseforge.com/minecraft/mc-mods/daylight-mobs-reborn

**5. Mob Sunscreen**
- Platform: Various
- Technique: Block-specific mobs from burning
- Config: By mob resource location
- URL: https://www.curseforge.com/minecraft/mc-mods/mob-sunscreen

**6. Nuit (formerly FabricSkyboxes)**
- Platform: Fabric, NeoForge
- Technique: Custom skybox rendering with JSON config
- Relevance: Shows sky rendering customization patterns
- URL: https://modrinth.com/mod/nuit

---

## Technical Implementation Notes

### Key Classes to Target (Fabric/Yarn Mappings)

| Purpose | Class | Method | Side |
|---------|-------|--------|------|
| Visual time | `ClientLevel` | `getTimeOfDay()`, `getDayTime()` | Client |
| Sky rendering | `LevelRenderer` | `renderSky()` | Client |
| Ambient light | `LightTexture` | Various | Client |
| Undead burning | `Zombie`, `Skeleton`, etc. | `aiStep()` or burn check | Server |
| Bee behavior | `Bee` | `wantsToEnterHive()` | Server |
| Spawn rules | `NaturalSpawner` | `isValidSpawnPostitionForType()` | Server |

### Tick Values Reference

| Time | Ticks | Visual State |
|------|-------|--------------|
| Sunrise | 0 | Dawn, pink sky |
| Morning | 1000 | Bright day beginning |
| Noon | 6000 | Maximum brightness |
| Afternoon | 9000 | Sun descending |
| **Sunset/Dusk** | **12000-13000** | **TARGET: Orange sky, low sun** |
| Night begin | 13000 | Stars appearing |
| Midnight | 18000 | Darkest |
| Night end | 23000 | Dawn approaching |

**Recommended target: 13000 ticks** - provides iconic dusk aesthetic with sun visible but low.

### Burn/Spawn Thresholds

- Undead burn when: Sun 15+ degrees above horizon AND sky light >= 12
- Burning stops at: Tick 12542 (sun too low)
- Hostile spawn in open: Block light 0, sky light check randomized
- Bees enter hive at: Tick 12542 (dusk)

---

## Open Questions

1. **Should rain darken the perpetual dusk further?** Visual consistency vs atmospheric effect.

2. **Should bees work 24/7 or follow server time?** Continuous work is simpler but may feel unnatural.

3. **Should villagers follow visual time or server time?** Server time recommended for normal schedules.

4. **How to communicate the time disconnect to players?** HUD indicator? Config option for actual time display?

5. **Should Nether/End dimensions also show dusk?** Probably not - they have their own aesthetics.

---

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Visual approach | HIGH | Multiple working mods demonstrate `ClientLevel` override |
| Undead burning override | MEDIUM | Known mechanic, specific mixin target needs verification |
| Bee behavior override | MEDIUM | Behavior documented, implementation needs verification |
| Spawn rule interaction | LOW | Complex interaction between light/time/weather |
| Shader compatibility | MEDIUM | Reported working, but shader-dependent |
| Multiplayer sync | HIGH | Well-understood client/server separation |

---

## Sources

### Primary (HIGH Confidence)
- Minecraft Wiki - Daylight Cycle: https://minecraft.wiki/w/Daylight_cycle
- Minecraft Wiki - Effect (dimension): https://minecraft.wiki/w/Effect_(dimension)
- Fabric Wiki - Mixin Examples: https://fabricmc.net/wiki/tutorial:mixin_examples

### Secondary (MEDIUM Confidence)
- Evernight Mod (CurseForge): https://www.curseforge.com/minecraft/mc-mods/evernight
- Twilight Forest GitHub: https://github.com/TeamTwilight/twilightforest
- Always Day Mod (Modrinth): https://modrinth.com/mod/always-day
- Daylight Mobs Reborn (CurseForge): https://www.curseforge.com/minecraft/mc-mods/daylight-mobs-reborn

### Tertiary (LOW Confidence - WebSearch Only)
- DaylightChangerStruggle (Modrinth): https://modrinth.com/mod/daylightchangerstruggle
- AlwaysDayMod GitHub: https://github.com/vincentmetevelis/AlwaysDayMod
- Nuit/FabricSkyboxes (Modrinth): https://modrinth.com/mod/nuit

---

## Metadata

**Research type:** Ecosystem + Implementation
**Research date:** 2026-01-20
**Valid until:** ~60 days (stable Minecraft rendering patterns)
**Downstream consumer:** /gsd:define-requirements for v2.0 twilight milestone
