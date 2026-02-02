package thc.downed;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Manages the downed state system.
 * Intercepts player death and converts it to downed state (spectator mode).
 */
public final class DownedManager {
	private DownedManager() {
	}

	/**
	 * Registers the death interception event.
	 * Called during mod initialization.
	 */
	public static void register() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return true; // Allow non-player deaths
			}

			// Already downed - shouldn't happen but be safe
			if (DownedState.isDowned(player)) {
				return false;
			}

			// Store location BEFORE switching mode (exact death position)
			Vec3 deathLocation = player.position();

			// Switch to spectator (makes player invulnerable, invisible to mobs)
			player.setGameMode(GameType.SPECTATOR);

			// Store downed location in attachment
			DownedState.setDownedLocation(player, deathLocation);

			// Cancel death
			return false;
		});
	}
}
