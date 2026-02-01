## monster changes

* all mobs are 20% faster by default, with the exception of creepers and baby zombies  
* monsters never drop armor/weapons 

### zombie 

* zombies replaced with husks in all spawn tables  
* baby zombies are the same speed as regular zombies  
* do not drop iron anymore

### skeleton 

* skeletons replaced with strays in all spawn tables

### ghast

* projectile speed \+50%  
* fire rate \-25%  
* fire spread from projectile explosion radius increased by 100%

### enderman 

* endermen have a 50% chance to teleport 3 blocks behind you after you damage them or after they damage you  
* endermen aggro if you are within 3 block radius

### vex

* health reduced to 4 hearts  
  * no longer spawns with iron sword

### phantom 

* phantoms no longer spawn naturally

### illager patrols

* no longer occur when world is in stage 1. can occur again once game is in stage 2 or above

### damage updates

| mob | old damage | new damage |
| :---- | :---- | :---- |
| vex | 13.5 | 4 |
| vindicator | 19.5 | 11.7 |
| evoker (fangs) | 6 | 2.5 |
| blaze (fireball) | 7.5 | 3.8 |
| piglin (arrow) | 4 | 8 |
| magma cube (large) | 9 | 4.7 |
| ghast | 9 | 9.3 |

## passive/neutral mob changes

* iron golems cannot be summoned


## Spec: Regional spawn distributions + partitioned monster caps (JE 1.21.11)

### Objective
Modify **NATURAL** monster spawning to:
1) use region-based **selection-time mixing** between custom mobs and vanilla biome spawn pools, and  
2) enforce **separate monster caps** per Overworld region,  
while preserving:
- vanilla **structure-specific spawns** (unchanged),
- vanilla **per-mob placement rules** (e.g., ghasts can spawn in air),
- existing mod changes already implemented:
  - hostile mobs can spawn at **any light level**,
  - **base chunks** already prevent NATURAL spawns.

Do not modify or revert any of the above preserved behaviors.

---

## Region definitions

### Dimensions
- **Overworld**: 3-way region split (below).
- **Nether**: no custom distribution specified in this spec; keep vanilla behavior unchanged.
- **End**: single region (blanket distribution).

### Overworld regions (evaluated at attempted spawn block `pos`)
- **OW_LOWER_CAVE**: `pos.y < 0 && !isSkyVisible(pos)`
- **OW_UPPER_CAVE**: `pos.y >= 0 && !isSkyVisible(pos)`
- **OW_SURFACE**: `isSkyVisible(pos)` (regardless of y)

`isSkyVisible(pos)` must use the world’s standard sky-visibility check at that block position.

> Note: For Overworld, the “surface vs cave” split is defined strictly by `isSkyVisible(pos)` at the attempted spawn block.

### Structure special spawns (bypass rule)
If vanilla would use a **structure-provided spawn pool** for this attempt (e.g., fortress/outpost/mansion special pools), then **bypass** this entire system:
- no custom mixing,
- no regional cap checks,
- vanilla selection/spawning unmodified for that attempt.

---

## Custom distributions (hardcoded constants)

All probabilities below are **per spawn attempt** at selection time.

### Overworld (exposed to sky) — OW_SURFACE
- 5%: `minecraft:witch`
- 95%: vanilla fallback

### Overworld upper caves (not exposed to sky, y >= 0) — OW_UPPER_CAVE
- 5%: `minecraft:witch`
- 2%: `minecraft:vex`
- 10%: `minecraft:pillager` (RANGED build; see “Pillager variants”)
- 25%: `minecraft:pillager` (MELEE build; see “Pillager variants”)
- 58%: vanilla fallback

### Overworld lower caves (not exposed to sky, y < 0) — OW_LOWER_CAVE
- 8%: `minecraft:blaze`
- 8%: `minecraft:breeze`
- 12%: `minecraft:vindicator`
- 25%: `minecraft:pillager` (MELEE build)
- 2%: `minecraft:evoker`
- 45%: vanilla fallback

### End (blanket) — END
- 25%: `minecraft:endermite`
- 75%: vanilla fallback

### Nether
- No custom distribution in this spec. Keep vanilla spawns and caps unchanged.

#### Validation
- For each region distribution, probabilities must sum to **<= 1.0**. If any sum exceeds 1.0, hard-fail at startup.

---

## Pillager variants (required)
Because vanilla has only `minecraft:pillager`, implement two “variants” via equipment loadout at spawn time:

- **MELEE pillager**: pillager with a melee weapon (e.g., iron sword). Must not have a crossbow.
- **RANGED pillager**: pillager with a crossbow (vanilla-like).

These variants are selected only when the custom distribution chooses “MELEE pillager” or “RANGED pillager”.

---

## Custom pack sizes
For any entity type chosen via the custom distribution (including pillager variants):
- `minPack = 1`, `maxPack = 4`

For vanilla fallback spawns:
- preserve vanilla pack sizes exactly.

---

## Monster caps (partitioned)

### Overworld cap multipliers (relative to vanilla monster cap)
Compute caps exactly the way vanilla does (including scaling with spawnable chunks), then apply:

- **OW_SURFACE_CAP** = `0.30 * vanillaCap`
- **OW_UPPER_CAVE_CAP** = `0.40 * vanillaCap`
- **OW_LOWER_CAVE_CAP** = `0.50 * vanillaCap`

These are intentionally > 100% total (120%).

### Nether/End caps
- Keep vanilla monster cap behavior unchanged (no partitioning, no multiplier changes).

### Global + local caps
Apply **both** vanilla cap mechanisms:
- vanilla **global** monster cap, and
- vanilla **local** monster cap,
but with counts restricted to the relevant Overworld region (see “Counting rule”).
Use vanilla’s local-cap definition/radius/logic exactly; only partition counts by region.

### Counting rule (what contributes to regional caps)
Regional cap counts include **only** monsters where:
- SpawnGroup is `MONSTER`, and
- NBT marker `spawnSystem.counted == true`.

Mobs without `spawnSystem.counted == true` do **not** count toward any regional cap.

---

## Spawn-origin tagging (persistent NBT)

### NBT schema
Write on entities:
- `spawnSystem.region` (string enum)
- `spawnSystem.counted` (boolean)

Region enum values:
- Overworld: `OW_SURFACE`, `OW_UPPER_CAVE`, `OW_LOWER_CAVE`
- Nether: `NETHER`
- End: `END`
- Other dimensions: `OTHER`

### When to set NBT
- **On all spawns** (any SpawnReason): set `spawnSystem.region` based on spawn position + dimension using the region rules above.
- Set `spawnSystem.counted = true` **only** when ALL are true:
  - `SpawnReason == NATURAL`,
  - SpawnGroup is `MONSTER`,
  - NOT a structure special spawn attempt,
  - the spawn is being processed by this system (i.e., cap-gated + mixing applied).
- Otherwise set `spawnSystem.counted = false`.

### Carryover
Do not implement explicit carryover/update behavior for transformations. Ignore carryover.

---

## Core spawning behavior (must be exact)

This system applies only to **SpawnReason.NATURAL** and SpawnGroup == **MONSTER**.

Given a candidate spawn position `pos`:

1) **Base chunk filter (already implemented)**
If existing “base chunk” logic denies NATURAL spawns here, fail this attempt.

2) **Structure bypass**
If this attempt uses a structure-provided spawn pool, run vanilla logic unmodified (no mixing, no regional caps). Do not set `spawnSystem.counted=true` due to this system for these entities.

3) **Determine region key**
- Overworld: `region = regionOverworld(pos)`
- Nether: `region = NETHER`
- End: `region = END`
- Other: `region = OTHER`

4) **Regional cap gate (Overworld only)**
If Overworld:
- Check vanilla global+local caps, but using the **region-specific counts** and **region-specific cap** for this `region`.
- If either cap would reject, fail the attempt (no fallback).

If Nether/End/Other:
- Use vanilla caps unchanged.

5) **Selection-time mixing**
Let `dist = distributionFor(region)`. Let `S = sum(p_i in dist)`.

- Draw `r ~ Uniform(0, 1)`.
- If `r < S`:
  - Choose the custom entry by cumulative probability.
  - Attempt to spawn that entity type using vanilla placement rules.
  - Use pack size random in `[1, 4]` with vanilla pack mechanics (same type for the pack).
  - On each spawned entity:
    - set `spawnSystem.region = region`,
    - set `spawnSystem.counted = true`,
    - apply pillager variant loadout if selected.
- Else (`r >= S`):
  - Use vanilla biome spawn-entry selection for `pos` (NOT structure pool; already bypassed).
  - Spawn with vanilla rules and vanilla pack sizes.
  - On each spawned entity:
    - set `spawnSystem.region = region`,
    - set `spawnSystem.counted = true`.

6) **Failure semantics**
If the chosen mob (custom or vanilla) cannot spawn at the location due to standard checks (collision/space/placement/etc.), the attempt fails.
Do not re-roll. Do not fall back.

---

## Cap enforcement implementation requirements

### Counting
When vanilla builds spawn info / counts mobs for cap enforcement, also tally:
- Overworld MONSTER counts per region among entities with `spawnSystem.counted == true`.

Do not scan chunks manually. Reuse vanilla’s entity iteration used for cap accounting and partition by the NBT marker.

### Applying caps
At the point vanilla compares MONSTER counts to caps (global + local):
- For Overworld NATURAL MONSTER spawning attempts processed by this system:
  - replace the single MONSTER count with the count for the candidate position’s Overworld region,
  - compare against that region’s cap (scaled vanilla cap * multiplier).
- For structure special spawns and for Nether/End:
  - leave vanilla cap logic unchanged.

---

## Non-goals / invariants
- Do not change or reintroduce light-level constraints (they are already removed).
- Do not alter base-chunk no-spawn behavior (already implemented).
- Do not change vanilla placement predicates per entity (air/ground rules remain vanilla).
- Do not create cooldowns or per-mob limits.
- Do not add finite lifetime to naturally spawned vexes.
- Do not add tests, config UIs, or debug tools unless required for correctness.

