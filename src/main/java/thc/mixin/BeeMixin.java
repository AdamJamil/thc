package thc.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Make bees work 24/7 regardless of time of day or weather.
 *
 * <p>Part of twilight hardcore — a perpetually hostile world where the
 * visual environment appears darker. Without this mixin, bees would
 * refuse to work due to perceived "night" conditions, making farms
 * unproductive.
 *
 * <p>In Minecraft 1.21+, bee behavior is controlled by the
 * {@code BEES_STAY_IN_HIVE} environment attribute, which normally
 * returns true at night or during rain. By redirecting this check
 * to always return false, bees:
 * <ul>
 *   <li>Continue pollinating and collecting nectar at any server time</li>
 *   <li>Continue working during rain</li>
 *   <li>Still return to hive when nectar-full (hasNectar check remains)</li>
 * </ul>
 */
@Mixin(Bee.class)
public abstract class BeeMixin {

	/**
	 * Redirect BEES_STAY_IN_HIVE environment attribute check to always return false.
	 *
	 * <p>The {@code wantsToEnterHive} method checks this attribute to determine
	 * if environmental conditions (night/rain) should make bees return home.
	 * By returning false, we signal that conditions are always favorable for
	 * bee work, enabling 24/7 productivity.
	 */
	@Redirect(
		method = "wantsToEnterHive",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/attribute/EnvironmentAttributeReader;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Object;"
		)
	)
	private Object thc$alwaysWork(EnvironmentAttributeReader reader, EnvironmentAttribute<?> attribute, Vec3 pos) {
		if (attribute == EnvironmentAttributes.BEES_STAY_IN_HIVE) {
			return Boolean.FALSE;
		}
		return reader.getValue(attribute, pos);
	}
}
