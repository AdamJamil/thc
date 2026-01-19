package thc.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Global XP blocking mixin - blocks XP orbs from non-combat sources.
 *
 * <p>THC enforces that XP should only come from combat (killing mobs) or
 * experience bottles. This mixin checks the call stack to determine the
 * source and blocks breeding, fishing, trading, smelting, and ore XP.
 *
 * <p>Sources allowed: Mob deaths (LivingEntity.dropExperience), ThrownExperienceBottle
 * Sources blocked: Everything else (breeding, fishing, trading, furnace, ores)
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbXpMixin {

    /**
     * Intercepts ExperienceOrb.award and blocks non-combat XP sources.
     *
     * <p>Checks the call stack to determine the XP source. Only allows XP from:
     * - LivingEntity.dropExperience (mob deaths)
     * - ThrownExperienceBottle (experience bottles)
     *
     * @param level The server level
     * @param pos The position
     * @param amount The XP amount
     * @param ci Callback for cancellation
     */
    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private static void thc$blockNonCombatXp(ServerLevel level, Vec3 pos, int amount, CallbackInfo ci) {
        // Check call stack to determine XP source
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            String methodName = element.getMethodName();

            // Allow: Mob death XP (dropExperience in LivingEntity or subclasses)
            if (methodName.contains("dropExperience") || methodName.contains("dropAllDeathLoot")) {
                return; // Allow
            }

            // Allow: Experience bottles
            if (className.contains("ThrownExperienceBottle") || className.contains("ExperienceBottle")) {
                return; // Allow
            }

            // Block: Breeding (Animal, finalizeSpawnChildFromBreeding, spawnChildFromBreeding)
            if (className.contains("Animal") && (methodName.contains("Breed") || methodName.contains("breed"))) {
                ci.cancel();
                return;
            }

            // Block: Fishing (FishingHook, retrieve)
            if (className.contains("FishingHook") || className.contains("FishingBobber")) {
                ci.cancel();
                return;
            }

            // Block: Trading (AbstractVillager, rewardTradeXp)
            if ((className.contains("Villager") || className.contains("Merchant")) &&
                (methodName.contains("Trade") || methodName.contains("trade"))) {
                ci.cancel();
                return;
            }

            // Block: Furnace (AbstractFurnaceBlockEntity, createExperience)
            if (className.contains("Furnace") && methodName.contains("Experience")) {
                ci.cancel();
                return;
            }

            // Block: Ore mining (Block.popExperience)
            if (className.contains("Block") && methodName.contains("popExperience")) {
                ci.cancel();
                return;
            }
        }

        // Default: allow (covers mob deaths and any edge cases)
    }
}
