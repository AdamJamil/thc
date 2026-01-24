package thc.mixin.access;

import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to invoke EnderMan's private teleport method.
 */
@Mixin(EnderMan.class)
public interface EnderManAccessor {

    @Invoker("teleport")
    boolean invokerTeleport(double x, double y, double z);
}
