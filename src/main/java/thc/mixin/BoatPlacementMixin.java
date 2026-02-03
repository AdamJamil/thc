package thc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thc.mixin.access.ItemAccessor;
import thc.playerclass.ClassManager;
import thc.playerclass.PlayerClass;
import thc.stage.StageManager;

/**
 * Mixin to gate land boat placement to Bastion class at Stage 5+.
 * Water placement unchanged for all players.
 */
@Mixin(BoatItem.class)
public abstract class BoatPlacementMixin {

    @Shadow @Final private EntityType<? extends AbstractBoat> entityType;

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void thc$gateLandPlacement(Level level, Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);

        // Ray trace - include both fluids and blocks
        BlockHitResult hitResult = ItemAccessor.invokeGetPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return; // Miss - let vanilla handle
        }

        BlockPos hitPos = hitResult.getBlockPos();

        // Check if hitting water - let vanilla handle water placement for everyone
        if (level.getFluidState(hitPos).is(FluidTags.WATER) ||
            level.getFluidState(hitPos.above()).is(FluidTags.WATER)) {
            return; // Water placement - vanilla handles this
        }

        // Solid ground hit - gate on Bastion + Stage 5+
        if (!level.getBlockState(hitPos).isSolid()) {
            return; // Not solid - let vanilla handle (will likely fail)
        }

        // This is a land placement attempt
        if (player instanceof ServerPlayer sp) {
            PlayerClass playerClass = ClassManager.getClass(sp);
            int boonLevel = StageManager.getBoonLevel(sp);

            if (playerClass != PlayerClass.BASTION || boonLevel < 5) {
                // Not qualified - show message and block
                sp.displayClientMessage(
                    Component.literal("Boat Mastery requires Bastion at Stage 5+"),
                    true
                );
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            // Qualified - spawn boat on top of solid block
            BlockPos spawnPos = hitPos.above();
            double x = spawnPos.getX() + 0.5;
            double y = spawnPos.getY();
            double z = spawnPos.getZ() + 0.5;

            // Create boat using the entity type
            AbstractBoat boat = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (boat == null) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            boat.setInitialPos(x, y, z);
            boat.setYRot(player.getYRot());

            // Apply default stack config (handles custom name, etc.) - server-side only
            if (level instanceof ServerLevel serverLevel) {
                EntityType.createDefaultStackConfig(serverLevel, stack, player).accept(boat);
            }

            // Check collision
            if (!level.noCollision(boat, boat.getBoundingBox())) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }

            // Spawn server-side only
            if (!level.isClientSide()) {
                level.addFreshEntity(boat);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
                stack.consume(1, player);
            }

            player.awardStat(Stats.ITEM_USED.get((Item)(Object)this));
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
