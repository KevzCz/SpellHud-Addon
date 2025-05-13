// net.pixeldreamstudios.spellhudaddon.mixin.ConfigMenuScreenMixin.java
package net.pixeldreamstudios.spellhudaddon.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.spell_engine.client.gui.ConfigMenuScreen;
import net.pixeldreamstudios.spellhudaddon.config.AddonHudConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.Screen;

@Mixin(ConfigMenuScreen.class)
public abstract class ConfigMenuScreenMixin extends Screen {
    protected ConfigMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void spellhudaddon$addLayoutButton(CallbackInfo ci) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("SpellHUD Layout"), button -> {
            this.client.setScreen(new AddonHudConfigScreen((Screen)(Object)this));
        }).dimensions(centerX - 60, centerY + 60, 120, 20).build());
    }
}
