package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Allow players to use beds at any time of day.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where danger
 * never stops. In this environment, players need to set spawn points via
 * beds regardless of the time of day. The visual time is always dusk, but
 * the server time still advances normally for game mechanics.
 *
 * <p>This mixin intercepts the BedRule lookup when a player attempts to
 * sleep and replaces the default "can only sleep when dark" rule with a
 * rule that always allows sleeping. All other bed checks are preserved:
 * <ul>
 *   <li>Monsters nearby check (NOT_SAFE) - still active</li>
 *   <li>Bed obstruction check (OBSTRUCTED) - still active</li>
 *   <li>Distance check (TOO_FAR_AWAY) - still active</li>
 *   <li>Dimension-specific behavior (explodes in Nether/End) - still active</li>
 * </ul>
 *
 * <p>The spawn point is still set when using the bed successfully, which
 * is the primary purpose of beds in twilight hardcore.
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerSleepMixin {

	/**
	 * Redirect the BedRule lookup to always allow sleeping.
	 *
	 * <p>The original code gets a BedRule from environment attributes which
	 * typically returns CAN_SLEEP_WHEN_DARK (only allows sleep at night).
	 * We replace this with a custom BedRule that:
	 * <ul>
	 *   <li>canSleep = ALWAYS (no time restriction)</li>
	 *   <li>canSetSpawn = ALWAYS (spawn point always set)</li>
	 *   <li>explodes = false (beds work normally)</li>
	 *   <li>errorMessage = empty (no custom error needed)</li>
	 * </ul>
	 *
	 * <p>This preserves all other sleep checks (monsters nearby, obstruction,
	 * distance) while only removing the time-of-day restriction.
	 */
	@Redirect(
		method = "startSleepInBed",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;"
		)
	)
	private Object thc$allowSleepAnytime(
		EnvironmentAttributeSystem system,
		net.minecraft.world.attribute.EnvironmentAttribute<?> attribute,
		Vec3 pos
	) {
		// Only intercept BED_RULE attribute lookups
		if (attribute == EnvironmentAttributes.BED_RULE) {
			// Return a BedRule that always allows sleeping
			// canSleep = ALWAYS, canSetSpawn = ALWAYS, explodes = false
			return new BedRule(
				BedRule.Rule.ALWAYS,
				BedRule.Rule.ALWAYS,
				false,
				Optional.empty()
			);
		}

		// For any other attribute, use the original system lookup
		return system.getValue(attribute, pos);
	}
}
