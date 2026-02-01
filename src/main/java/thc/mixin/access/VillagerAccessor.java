package thc.mixin.access;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to invoke Villager's protected methods.
 */
@Mixin(Villager.class)
public interface VillagerAccessor {

    /**
     * Invoke the protected updateTrades(ServerLevel) method.
     * This generates trades for the villager's current level.
     */
    @Invoker("updateTrades")
    void invokeUpdateTrades(ServerLevel serverLevel);
}
