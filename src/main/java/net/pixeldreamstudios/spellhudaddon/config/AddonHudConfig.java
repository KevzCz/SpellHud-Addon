package net.pixeldreamstudios.spellhudaddon.config;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.ConfigData;
import net.spell_engine.client.gui.HudElement;
import net.spell_engine.config.HudConfig;
import net.minecraft.util.math.Vec2f;

@Config(name = "spellhud-addon") // <== REQUIRED
public class AddonHudConfig extends HudConfig implements ConfigData {
    public LayoutStyle layout = LayoutStyle.HORIZONTAL;

    public AddonHudConfig() {
        this.castbar = new CastBar(
                new HudElement(HudElement.Origin.BOTTOM, new Vec2f(0, -27)),
                new Part(true, new Vec2f(0, -12)),
                new Part(true, new Vec2f(-8, -25)),
                172
        );
        this.hotbar = defaultHotBar();
        this.error_message = defaultErrorMessage();
    }

    public enum LayoutStyle {
        HORIZONTAL,
        VERTICAL,
        GRID_3x3
    }
}
