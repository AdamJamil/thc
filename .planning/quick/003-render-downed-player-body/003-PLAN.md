# Quick Task 003: Render Downed Player Body

## Goal

Make downed players visible to all nearby players by rendering their body lying on the ground with red particles for visibility.

## Current State

- Downed players are in spectator mode (invisible)
- `RevivalSync` only sends to players within 2 blocks who are looking at the target (for HUD progress ring)
- No world-space rendering of downed bodies exists

## Approach

Create a separate broadcast sync for downed player locations, distinct from the existing revival HUD sync. The HUD sync remains unchanged (for active revivers only). The new broadcast sync sends ALL downed players within range to ALL clients for body rendering.

## Tasks

### Task 1: Create DownedPlayersSync (Server-Side Broadcast)

**File:** `src/main/java/thc/network/DownedPlayersSync.java`

Create a new sync system that broadcasts all downed player locations to all clients:
- Each tick, for each online player, send list of all downed players within 50 blocks
- Payload contains: list of (UUID, x, y, z, playerName)
- Delta sync: only send when list changes
- Separate from RevivalSync (which handles HUD progress for active revivers)

**File:** `src/main/java/thc/network/DownedPlayersPayload.java`

New payload with:
- List of downed player entries (UUID, x, y, z, name)
- Stream codec for list serialization

**File:** `src/main/java/thc/THCNetworking.java`

Register new payload type.

**File:** `src/main/kotlin/thc/THC.kt`

Call `DownedPlayersSync.sync()` in the END_SERVER_TICK handler alongside existing `RevivalSync.sync()`.

### Task 2: Create DownedPlayersClientState (Client Cache)

**File:** `src/client/java/thc/client/DownedPlayersClientState.java`

Client-side cache for all downed players:
- `Map<UUID, DownedPlayerInfo>` where info contains x, y, z, name
- `update(List<DownedPlayerEntry> players)` - replaces entire cache
- `getDownedPlayers()` - returns current map
- `clear()` - empties cache

**File:** `src/client/kotlin/thc/THCClient.kt`

Register receiver for `DownedPlayersPayload.TYPE`:
- On receive, call `DownedPlayersClientState.update(payload.entries())`

### Task 3: Create DownedBodyRenderer (World Rendering)

**File:** `src/client/kotlin/thc/client/DownedBodyRenderer.kt`

World-space renderer for downed player bodies:

1. **Register** with `WorldRenderEvents.AFTER_ENTITIES` (Fabric API)

2. **For each downed player in DownedPlayersClientState:**
   - Skip if player is local player (don't render own body)
   - Get skin texture from UUID using `DefaultPlayerSkin.get(uuid)` or fetch from session service
   - Use PoseStack to position at downed location
   - Rotate model to lie flat (90-degree X rotation)
   - Render using `PlayerRenderer` or direct model rendering

3. **Particle spawning:**
   - Spawn red DUST particles around the body each frame
   - Use `Minecraft.getInstance().particleEngine.add()`
   - Position: random offset within 0.5 blocks of body center
   - Color: red (1.0, 0.0, 0.0)
   - Rate: ~2-3 particles per frame for visibility without spam

**Pose:** Use X-axis rotation to lay the model flat (similar to sleeping), facing up.

**Skin handling:**
- Use `AbstractClientPlayer` texture loading system
- `PlayerSkin.load(uuid)` for async skin fetch
- Fall back to default Steve/Alex skin while loading

---

## Files Modified

| File | Change |
|------|--------|
| `src/main/java/thc/network/DownedPlayersPayload.java` | NEW - Broadcast payload |
| `src/main/java/thc/network/DownedPlayersSync.java` | NEW - Server broadcast logic |
| `src/main/java/thc/THCNetworking.java` | Register new payload |
| `src/main/kotlin/thc/THC.kt` | Call DownedPlayersSync in tick |
| `src/client/java/thc/client/DownedPlayersClientState.java` | NEW - Client cache |
| `src/client/kotlin/thc/THCClient.kt` | Register payload receiver |
| `src/client/kotlin/thc/client/DownedBodyRenderer.kt` | NEW - World renderer |

## Testing

1. Start server with 2+ players
2. Kill Player A (enters downed state)
3. Player B should see:
   - Player A's body lying on ground at death location
   - Red particles floating around the body
4. Walk 50+ blocks away - body should disappear
5. Walk back - body should reappear
6. Revive Player A - body should disappear

---

*Plan created: 2026-02-02*
