package thc.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.villager.AllowedProfessions;

/**
 * Restrict villagers to allowed professions only.
 *
 * <p>This mixin intercepts all profession changes at the data layer by
 * injecting into {@link Villager#setVillagerData(VillagerData)}. This is
 * the single chokepoint for all profession assignments including:
 * <ul>
 *   <li>AI job acquisition (villager walks to job block)</li>
 *   <li>NBT loading (naturally spawned villagers, structure villagers)</li>
 *   <li>Commands (/data modify)</li>
 *   <li>Zombie villager cure completion</li>
 * </ul>
 *
 * <p>When a disallowed profession is detected, the villager is set to NONE
 * profession instead, making them jobless and unable to trade. This forces
 * players to find or create villagers with allowed professions.
 *
 * <p>Allowed professions: MASON, LIBRARIAN, BUTCHER, CARTOGRAPHER, NONE, NITWIT
 */
@Mixin(Villager.class)
public abstract class VillagerProfessionMixin {

    @Shadow
    public abstract VillagerData getVillagerData();

    /**
     * Intercept profession changes and reject disallowed professions.
     *
     * <p>If the new data contains a disallowed profession, we:
     * <ol>
     *   <li>Get the current villager data</li>
     *   <li>Create fixed data with NONE profession</li>
     *   <li>Set the fixed data via setVillagerData</li>
     *   <li>Cancel the original call to prevent setting disallowed profession</li>
     * </ol>
     */
    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void thc$restrictProfession(VillagerData newData, CallbackInfo ci) {
        Holder<VillagerProfession> newProf = newData.profession();

        // Extract profession key for comparison
        ResourceKey<VillagerProfession> profKey = newProf.unwrapKey().orElse(null);

        if (!AllowedProfessions.isAllowed(profKey)) {
            // For disallowed profession, force to NONE instead
            Villager self = (Villager) (Object) this;
            VillagerData current = this.getVillagerData();
            Holder<VillagerProfession> noneHolder = AllowedProfessions.getNoneHolder(
                self.level().registryAccess()
            );
            VillagerData fixed = current.withProfession(noneHolder);

            // Set the fixed data and cancel original call
            self.setVillagerData(fixed);
            ci.cancel();
        }
    }
}
