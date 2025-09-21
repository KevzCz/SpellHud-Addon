package net.pixeldreamstudios.spellhudaddon.mixin;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.pixeldreamstudios.spellhudaddon.SpellHudState;
import net.pixeldreamstudios.spellhudaddon.config.AddonHudConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    private boolean shouldShiftHud() {
        var config = AutoConfig.getConfigHolder(AddonHudConfig.class).getConfig();
        return config.layout == AddonHudConfig.LayoutStyle.CENTERED_HORIZONTAL_ABOVE_HOTBAR && SpellHudState.hasVisibleSpells;
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void shiftXPBar(DrawContext context, int x, CallbackInfo ci) {
        if (shouldShiftHud()) {
            context.getMatrices().translate(0, -24, 0);
        }
    }
    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"))
    private void shiftHeldItemTooltipUp(DrawContext context, CallbackInfo ci) {
        if (shouldShiftHud()) {
            context.getMatrices().translate(0, -3, 0); // Move up 12 pixels
        }
    }
}
