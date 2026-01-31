package thc.villager;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/**
 * Constants and validation for allowed villager professions.
 *
 * <p>THC restricts villagers to a subset of professions:
 * <ul>
 *   <li>MASON - stonecutter trades</li>
 *   <li>LIBRARIAN - enchanted books</li>
 *   <li>BUTCHER - food trades</li>
 *   <li>CARTOGRAPHER - maps and locators</li>
 * </ul>
 *
 * <p>NONE and NITWIT are always allowed as they represent jobless states.
 * All other professions (armorer, cleric, farmer, fisherman, fletcher,
 * leatherworker, shepherd, toolsmith, weaponsmith) are blocked.
 */
public final class AllowedProfessions {

    private AllowedProfessions() {
    }

    /**
     * Professions that villagers are allowed to have.
     * Includes the 4 gameplay-relevant professions plus NONE and NITWIT.
     */
    private static final Set<ResourceKey<VillagerProfession>> ALLOWED = Set.of(
        VillagerProfession.MASON,
        VillagerProfession.LIBRARIAN,
        VillagerProfession.BUTCHER,
        VillagerProfession.CARTOGRAPHER,
        VillagerProfession.NONE,
        VillagerProfession.NITWIT
    );

    /**
     * Job blocks that grant disallowed professions.
     * Used for POI blocking to prevent profession acquisition.
     */
    private static final Set<Block> DISALLOWED_JOB_BLOCKS = Set.of(
        Blocks.COMPOSTER,            // farmer
        Blocks.BREWING_STAND,        // cleric
        Blocks.SMITHING_TABLE,       // toolsmith
        Blocks.BLAST_FURNACE,        // armorer
        Blocks.FLETCHING_TABLE,      // fletcher
        Blocks.CAULDRON,             // leatherworker
        Blocks.WATER_CAULDRON,       // leatherworker variant
        Blocks.LAVA_CAULDRON,        // leatherworker variant
        Blocks.POWDER_SNOW_CAULDRON, // leatherworker variant
        Blocks.BARREL,               // fisherman
        Blocks.GRINDSTONE,           // weaponsmith
        Blocks.LOOM                  // shepherd
    );

    /**
     * Check if a profession is allowed.
     *
     * @param profKey the profession ResourceKey to check
     * @return true if allowed (null is treated as allowed, representing NONE)
     */
    public static boolean isAllowed(ResourceKey<VillagerProfession> profKey) {
        if (profKey == null) {
            return true; // null = NONE, allowed
        }
        return ALLOWED.contains(profKey);
    }

    /**
     * Check if a block grants a disallowed profession.
     *
     * @param block the block to check
     * @return true if the block is a job site for a disallowed profession
     */
    public static boolean isDisallowedJobBlock(Block block) {
        return DISALLOWED_JOB_BLOCKS.contains(block);
    }

    /**
     * Get the Holder for NONE profession from the registry.
     * Used to reset villagers to jobless state when disallowed profession detected.
     *
     * @param registryAccess the registry access from the villager's level
     * @return Holder for VillagerProfession.NONE
     */
    public static Holder<VillagerProfession> getNoneHolder(RegistryAccess registryAccess) {
        return registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION)
            .getOrThrow(VillagerProfession.NONE);
    }
}
