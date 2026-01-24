# Phase 41: NBT Spawn Origin Tagging - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Tag every naturally-spawned monster with its spawn region for downstream cap counting. Two attachments: SPAWN_REGION (where it spawned) and SPAWN_COUNTED (whether it counts toward regional caps). This is infrastructure for Phase 42 (regional spawns) and Phase 43 (cap partitioning).

</domain>

<decisions>
## Implementation Decisions

### Region boundaries
- Y=0 (sea level) is the boundary between upper and lower cave
- Surface detection via heightmap comparison: `pos.getY() >= world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ())`
- This handles overhangs correctly — mob under cliff counts as cave
- Region determined once at spawn time, never recalculated

### Region values
- OW_SURFACE: Overworld, Y >= heightmap at X/Z
- OW_UPPER_CAVE: Overworld, Y < heightmap AND Y >= 0
- OW_LOWER_CAVE: Overworld, Y < heightmap AND Y < 0

### What gets tagged
- Only mobs processed by THC's spawn system get tagged
- Spawner mobs (dungeon, fortress): No tags at all
- Summoned/converted/reinforcement mobs: Retain default behavior (no special handling)
- SPAWN_COUNTED = true only for: NATURAL spawn + MONSTER category + THC-processed

### Nether/End handling
- Claude's discretion on whether to tag other dimensions
- If tagging simplifies the code (unified path), tag as NETHER/END
- If skipping is simpler, skip — no functional impact since caps are Overworld-only
- No modded dimensions to consider

### Claude's Discretion
- Whether to tag Nether/End mobs (whatever makes code cleaner)
- Byte encoding vs string attachment (roadmap suggests byte for efficiency)
- Exact attachment registration pattern

</decisions>

<specifics>
## Specific Ideas

- Heightmap-based surface detection: `pos.getY() >= world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ())`
- This is cleaner than canSeeSky and handles edge cases better

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 41-nbt-spawn-origin-tagging*
*Context gathered: 2026-01-24*
