package thc.mixin;

import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Make villagers behave as if it's always night for schedule purposes.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where monsters
 * spawn 24/7 and the visual environment appears as dusk. Villagers should
 * match this by always behaving as if it's night — seeking shelter and beds
 * continuously. This creates a thematically consistent world where daylight
 * doesn't mean safety.
 *
 * <p>In Minecraft 1.21, villagers use a Brain AI system with scheduled
 * Activities. The schedule maps tick ranges to activities:
 * <ul>
 *   <li>00010-12000 ticks: Various work/wander activities</li>
 *   <li>12000-23999 ticks: REST/SLEEP activity (night behavior)</li>
 * </ul>
 *
 * <p>By redirecting the time parameter in
 * {@code Brain.updateActivityFromSchedule} to always be 13000 (mid-night),
 * villagers will:
 * <ul>
 *   <li>Always try to sleep/rest</li>
 *   <li>Always seek shelter</li>
 *   <li>Still trade when directly interacted with (interaction overrides schedule)</li>
 * </ul>
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {

	/**
	 * Redirect the time parameter passed to Brain's schedule update to always
	 * return a night-time value.
	 *
	 * <p>The Villager's customServerAiStep method calls
	 * {@code brain.updateActivityFromSchedule(envSystem, dayTime, pos)} where dayTime
	 * determines which scheduled activity is active. By always passing 13000L,
	 * the villager's Brain will always select the night-time activity (REST).
	 *
	 * <p>This does NOT prevent trading — player interactions trigger
	 * MEET_PLAYER activity which overrides the schedule temporarily.
	 */
	@Redirect(
		method = "registerBrainGoals",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/Brain;updateActivityFromSchedule(Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;JLnet/minecraft/world/phys/Vec3;)V"
		)
	)
	private void thc$perpetualNightSchedule(Brain<?> brain, EnvironmentAttributeSystem envSystem, long dayTime, Vec3 pos) {
		// Always use 13000L (mid-night) for schedule determination
		// This makes villagers always select their night-time REST activity
		brain.updateActivityFromSchedule(envSystem, 13000L, pos);
	}
}
