package thc.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
	@Unique
	private static final Set<String> REMOVED_RECIPE_PATHS = Set.of(
		"shield",
		"wooden_spear",
		"stone_spear",
		"copper_spear",
		"iron_spear",
		"golden_spear",
		"diamond_spear",
		"netherite_spear_smithing",
		"snow_block",
		"furnace",
		"blast_furnace",
		"smoker",
		"bone_meal"
	);

	@Inject(
		method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
		at = @At("RETURN"),
		cancellable = true
	)
	private void thc$removeDisabledRecipes(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<RecipeMap> cir) {
		RecipeMap recipes = cir.getReturnValue();
		Collection<RecipeHolder<?>> values = recipes.values();
		List<RecipeHolder<?>> filtered = new ArrayList<>(values.size());
		for (RecipeHolder<?> holder : values) {
			if (!REMOVED_RECIPE_PATHS.contains(holder.id().identifier().getPath())) {
				filtered.add(holder);
			}
		}
		if (filtered.size() != values.size()) {
			cir.setReturnValue(RecipeMap.create(filtered));
		}
	}
}
