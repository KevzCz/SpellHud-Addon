package net.pixeldreamstudios.spellhudaddon.config;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

public class AddonHudConfigScreen extends Screen {
    private final Screen parent;

    public AddonHudConfigScreen(Screen parent) {
        super(Text.literal("SpellHUD Layout Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var config = AutoConfig.getConfigHolder(AddonHudConfig.class).getConfig();
        int centerX = width / 2;
        int centerY = height / 2;

        addDrawableChild(CyclingButtonWidget.<AddonHudConfig.LayoutStyle>builder(
                        layout -> Text.translatable(layout.translationKey)) // <== changed from Text.literal
                .values(AddonHudConfig.LayoutStyle.values())
                .initially(config.layout)
                .build(centerX - 125, centerY - 20, 250, 20, Text.translatable("spellhud.layout.title"), // also changed
                        (button, layout) -> {
                            config.layout = layout;
                            AutoConfig.getConfigHolder(AddonHudConfig.class).save();
                        }));


        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            client.setScreen(parent);
        }).dimensions(centerX - 50, centerY + 20, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("SpellHUD Layout Config"), width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

}
