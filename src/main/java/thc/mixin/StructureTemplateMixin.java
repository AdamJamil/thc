package thc.mixin;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Filters furnace, blast furnace, smoker, and brewing stand blocks from structure generation.
 *
 * <p>Villages generate with furnaces by default. THC requires players to craft
 * furnaces with blaze powder, gating smelting behind Nether access. This mixin
 * intercepts structure block placement and skips furnaces and brewing stands.
 */
@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {

	/**
	 * Blocks to filter from structure generation.
	 * Furnaces require blaze powder, smokers require iron to craft in THC.
	 * Brewing stands removed entirely from game economy.
	 */
	@Unique
	private static final Set<Block> FILTERED_STRUCTURE_BLOCKS = Set.of(
		Blocks.FURNACE,
		Blocks.BLAST_FURNACE,
		Blocks.SMOKER,
		Blocks.BREWING_STAND
	);

	/**
	 * Redirect setBlock calls to skip furnace placement.
	 *
	 * <p>When a structure template places blocks, this intercepts each setBlock
	 * call. If the block is a furnace or blast furnace, we return true (pretending
	 * it was placed) but don't actually place it.
	 */
	@Redirect(
		method = "placeInWorld",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
		)
	)
	private boolean thc$filterFurnaceBlocks(
			ServerLevelAccessor level,
			BlockPos pos,
			BlockState state,
			int flags) {
		if (FILTERED_STRUCTURE_BLOCKS.contains(state.getBlock())) {
			// Skip placement, pretend success
			return true;
		}
		return level.setBlock(pos, state, flags);
	}
}
