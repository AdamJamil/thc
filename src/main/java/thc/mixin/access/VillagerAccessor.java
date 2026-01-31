package thc.mixin.access;

import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to access Villager's private tradingXp field.
 * Used for manual level-up logic to read and reset XP.
 */
@Mixin(Villager.class)
public interface VillagerAccessor {

    @Accessor("tradingXp")
    int getTradingXp();

    @Accessor("tradingXp")
    void setTradingXp(int xp);
}
