package thc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client-side mixin to hide the lapis cost display in the enchanting table UI.
 * Our book-slot enchanting doesn't use lapis, so we hide the cost indicators.
 */
@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenMixin {

    /**
     * Intercept blitSprite calls to skip rendering the lapis cost sprites.
     * The vanilla UI uses level_1, level_2, level_3 sprites to show lapis costs.
     */
    @Redirect(method = "renderBg",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void thc$skipLapisCostSprite(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        // Skip rendering lapis cost sprites (level_1, level_2, level_3)
        String path = sprite.getPath();
        if (path.contains("level_1") || path.contains("level_2") || path.contains("level_3")) {
            return; // Don't render
        }
        // Render all other sprites normally
        guiGraphics.blitSprite(pipeline, sprite, x, y, width, height);
    }
}
