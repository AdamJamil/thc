package thc.mixin.client;

import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thc.client.EffectsGuiConfig;

/**
 * Injects the "Effects GUI Scaling" slider into the Video Settings screen.
 * Appends after all vanilla options at the bottom of the list.
 */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {

    protected VideoSettingsScreenMixin(Screen screen, Options options, Component component) {
        super(screen, options, component);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void thc$addEffectsGuiScale(CallbackInfo ci) {
        if (this.list != null) {
            this.list.addSmall(EffectsGuiConfig.INSTANCE.getEffectsGuiScale());
        }
    }
}
