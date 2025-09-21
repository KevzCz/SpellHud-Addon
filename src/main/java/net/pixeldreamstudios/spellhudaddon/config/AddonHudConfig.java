package net.pixeldreamstudios.spellhudaddon.config;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.ConfigData;
import net.spell_engine.client.gui.HudElement;
import net.spell_engine.config.HudConfig;
import net.minecraft.util.math.Vec2f;

@Config(name = "spellhud-addon")
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
        HORIZONTAL("spellhud.layout.horizontal"),
        VERTICAL_UP("spellhud.layout.vertical_up"),
        VERTICAL_DOWN("spellhud.layout.vertical_down"),
        GRID_3x3_UP("spellhud.layout.grid_3x3_up"),
        GRID_3x3_DOWN("spellhud.layout.grid_3x3_down"),
        CIRCULAR_CLOCKWISE("spellhud.layout.circular_clockwise"),
        CIRCULAR_COUNTERCLOCKWISE("spellhud.layout.circular_counterclockwise"),
        CENTERED_HORIZONTAL_ABOVE_HOTBAR("spellhud.layout.centered_horizontal_above_hotbar"),
        ROTATING_CORNER_RING("spellhud.layout.rotating_corner_ring");

        public final String translationKey;

        LayoutStyle(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
