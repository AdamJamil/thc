package thc.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
	@Inject(
		method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
		at = @At("RETURN"),
		cancellable = true
	)
	private void thc$removeShieldRecipe(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<RecipeMap> cir) {
		RecipeMap recipes = cir.getReturnValue();
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("shield"));
		Collection<RecipeHolder<?>> values = recipes.values();
		List<RecipeHolder<?>> filtered = new ArrayList<>(values.size());
		for (RecipeHolder<?> holder : values) {
			if (!holder.id().equals(key)) {
				filtered.add(holder);
			}
		}
		if (filtered.size() != values.size()) {
			cir.setReturnValue(RecipeMap.create(filtered));
		}
	}
}
