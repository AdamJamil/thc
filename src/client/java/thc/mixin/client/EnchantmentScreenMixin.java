package thc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client-side mixin to show cost of 3 in the enchanting table UI.
 * Our book-slot enchanting always costs 3 levels, so we show level_3 sprite.
 */
@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {

    private static final Identifier LEVEL_3_SPRITE = Identifier.withDefaultNamespace("container/enchanting_table/level_3");

    /**
     * Intercept blitSprite calls to replace level_1/level_2 with level_3.
     * This shows "3" as the cost since our enchanting always costs 3 levels.
     */
    @Redirect(method = "renderBg",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void thc$showCostOfThree(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        String path = sprite.getPath();
        // Replace level_1 or level_2 with level_3 to show cost of 3
        if (path.contains("level_1") || path.contains("level_2")) {
            guiGraphics.blitSprite(pipeline, LEVEL_3_SPRITE, x, y, width, height);
            return;
        }
        // Render all other sprites normally (including level_3)
        guiGraphics.blitSprite(pipeline, sprite, x, y, width, height);
    }
}
