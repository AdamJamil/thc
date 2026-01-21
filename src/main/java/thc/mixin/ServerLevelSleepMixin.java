package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Prevent time advancement when players sleep.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where
 * sleeping in beds sets your spawn point but does NOT skip time.
 * The danger never stops; players must survive rather than skip
 * past the perpetual threat.
 *
 * <p>This mixin intercepts the ADVANCE_TIME gamerule check in the
 * sleep handling code of ServerLevel.tick(). When all players are
 * sleeping and the game would normally advance time to morning,
 * we instead prevent the time skip while still allowing:
 * <ul>
 *   <li>Players to wake up naturally after sleep duration</li>
 *   <li>Spawn points to be set (handled in bed interaction)</li>
 *   <li>Weather to be reset (if ADVANCE_WEATHER gamerule is true)</li>
 *   <li>Insomnia/phantom timer to be cleared</li>
 * </ul>
 *
 * <p>Implementation note: The time skip code in ServerLevel.tick() is:
 * <pre>
 * if (this.sleepStatus.areEnoughSleeping(i) && this.sleepStatus.areEnoughDeepSleeping(i, this.players)) {
 *     if (this.getGameRules().get(GameRules.ADVANCE_TIME)) {
 *         long l = this.levelData.getDayTime() + 24000L;
 *         this.setDayTime(l - l % 24000L);  // Skip to morning
 *     }
 *     this.wakeUpAllPlayers();
 *     ...
 * }
 * </pre>
 * We redirect the GameRules.get(ADVANCE_TIME) call to return false,
 * preventing setDayTime from being called while preserving wakeUpAllPlayers.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepMixin {

	/**
	 * Prevent time skip during sleep by making ADVANCE_TIME appear false.
	 *
	 * <p>This redirect targets the first GameRules.get call in the tick method,
	 * which is specifically the ADVANCE_TIME check in the sleep handling block.
	 * By returning false, we prevent the time skip to morning while still
	 * allowing players to complete their sleep cycle and wake up normally.
	 *
	 * <p>Note: There is another GameRules.get(ADVANCE_TIME) call in tickTime(),
	 * but we only want to block the sleep-triggered time skip, not normal
	 * day/night cycle progression. The ordinal=0 targets specifically the
	 * first call in tick(), which is the sleep block.
	 */
	@Redirect(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;",
			ordinal = 0
		)
	)
	private Object thc$preventSleepTimeSkip(GameRules gameRules, GameRule<?> rule) {
		// If this is the ADVANCE_TIME check in the sleep block, return false
		// to prevent time from being skipped to morning
		if (rule == GameRules.ADVANCE_TIME) {
			return false;
		}

		// For any other gamerule, use the original value
		return gameRules.get(rule);
	}
}
