package thc.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import thc.mixin.access.AbstractBoatAccessor;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mixin to implement hostile mob trapping in boats.
 * Hostile mobs that enter boats are tracked and auto-eject after 80 ticks (4 seconds).
 * When breakout occurs, the boat drops as an item (reusable, not destroyed).
 *
 * Only applies to vanilla wooden boats, not IronBoat.
 */
@Mixin(AbstractBoat.class)
public class BoatTrappingMixin {

    /**
     * Map of trapped hostile mob UUID to the game tick when they boarded.
     */
    @Unique
    private final Map<UUID, Long> thc$trappedSince = new HashMap<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void thc$handleMobTrapping(CallbackInfo ci) {
        AbstractBoat self = (AbstractBoat) (Object) this;

        // Only process on server side
        if (self.level().isClientSide()) return;

        // Only apply to vanilla Boat, not IronBoat or other custom boats
        if (!(self instanceof Boat)) return;

        long currentTick = self.level().getGameTime();

        // Track new hostile passengers
        for (Entity passenger : self.getPassengers()) {
            if (passenger instanceof Mob mob &&
                mob.getType().getCategory() == MobCategory.MONSTER) {
                // Only add if not already tracked
                thc$trappedSince.putIfAbsent(mob.getUUID(), currentTick);
            }
        }

        // Check for breakout (any hostile mob trapped 80+ ticks)
        for (Entity passenger : new ArrayList<>(self.getPassengers())) {
            if (passenger instanceof Mob mob) {
                Long boardTick = thc$trappedSince.get(mob.getUUID());
                if (boardTick != null && currentTick - boardTick >= 80) {
                    // Time's up - mob breaks out
                    // Eject all passengers
                    for (Entity p : new ArrayList<>(self.getPassengers())) {
                        p.stopRiding();
                    }

                    // Drop boat as item
                    thc$dropBoatItem(self);

                    // Remove boat entity
                    self.discard();
                    return; // Exit - boat is gone
                }
            }
        }

        // Clean up tracking for passengers that left (not via breakout)
        thc$trappedSince.keySet().removeIf(uuid ->
            self.getPassengers().stream().noneMatch(p -> p.getUUID().equals(uuid))
        );
    }

    @Unique
    private void thc$dropBoatItem(AbstractBoat boat) {
        // Get the correct item for this boat variant using accessor (getDropItem is protected)
        AbstractBoatAccessor accessor = (AbstractBoatAccessor) boat;
        ItemStack stack = new ItemStack(accessor.invokeGetDropItem());
        ItemEntity itemEntity = new ItemEntity(
            boat.level(),
            boat.getX(),
            boat.getY(),
            boat.getZ(),
            stack
        );
        boat.level().addFreshEntity(itemEntity);
    }
}
