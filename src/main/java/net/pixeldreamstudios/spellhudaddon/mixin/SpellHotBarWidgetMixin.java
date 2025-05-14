package net.pixeldreamstudios.spellhudaddon.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.pixeldreamstudios.spellhudaddon.config.AddonHudConfig;
import net.spell_engine.client.gui.Drawable;
import net.spell_engine.client.gui.HudKeyVisuals;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget.SpellViewModel;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget.ViewModel;
import net.spell_engine.client.util.Rect;
import net.spell_engine.client.util.TextureFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpellHotBarWidget.class)
public class SpellHotBarWidgetMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void spellhudaddon$overrideLayout(DrawContext context, int screenWidth, int screenHeight, ViewModel viewModel, CallbackInfo ci) {
        var addonConfig = AutoConfig.getConfigHolder(AddonHudConfig.class).getConfig();

        if (addonConfig.layout != AddonHudConfig.LayoutStyle.HORIZONTAL) {
            var engineOffset = net.spell_engine.client.SpellEngineClient.hudConfig.value.hotbar.offset;
            addonConfig.hotbar.offset = engineOffset;

            if (addonConfig.layout == AddonHudConfig.LayoutStyle.CIRCULAR_CLOCKWISE ||
                    addonConfig.layout == AddonHudConfig.LayoutStyle.CIRCULAR_COUNTERCLOCKWISE) {
                renderCircularLayout(context, screenWidth, screenHeight, viewModel, addonConfig.layout);
            } else {
                renderCustomLayout(context, screenWidth, screenHeight, viewModel, addonConfig.layout);
            }

            ci.cancel();
        }
    }

    private static void renderCircularLayout(DrawContext context, int screenWidth, int screenHeight, ViewModel viewModel, AddonHudConfig.LayoutStyle layout) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        var config = AutoConfig.getConfigHolder(AddonHudConfig.class).getConfig();

        List<SpellViewModel> spells = viewModel.spells();
        if (spells.isEmpty()) return;

        Vec2f center = config.hotbar.origin.getPoint(screenWidth, screenHeight).add(config.hotbar.offset);
        int radius = 50;
        int slotSize = 22;
        int iconSize = 16;
        Vec2f iconOffset = new Vec2f(3, 3);
        TextureFile background = new TextureFile(Identifier.of("textures/gui/sprites/hud/hotbar.png"), 182, 22);

        SpellHotBarWidget.lastRendered = new Rect(
                center.add(new Vec2f(-radius - slotSize, -radius - slotSize)),
                center.add(new Vec2f(radius + slotSize, radius + slotSize))
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < spells.size(); i++) {
            SpellViewModel spell = spells.get(i);
            int slotX, slotY;

            if (i == 0) {
                slotX = (int) center.x - slotSize / 2;
                slotY = (int) center.y - slotSize / 2;
            } else {
                float angleStep = (float)(2 * Math.PI / (spells.size() - 1));
                float angle = (i - 1) * angleStep;
                if (layout == AddonHudConfig.LayoutStyle.CIRCULAR_COUNTERCLOCKWISE) {
                    angle = -angle;
                }

                float xOffset = (float) Math.cos(angle) * radius;
                float yOffset = (float) Math.sin(angle) * radius;

                slotX = (int) (center.x + xOffset - slotSize / 2);
                slotY = (int) (center.y + yOffset - slotSize / 2);
            }

            context.drawTexture(background.id(), slotX, slotY, 0, 0, 6, slotSize, background.width(), background.height());
            context.drawTexture(background.id(), slotX + 6, slotY, 10, 0, 10, slotSize, background.width(), background.height());
            context.drawTexture(background.id(), slotX + 10, slotY, 170, 0, 11, slotSize, background.width(), background.height());

            int iconX = slotX + (int) iconOffset.x;
            int iconY = slotY + (int) iconOffset.y;
            if (spell.iconId() != null) {
                context.drawTexture(spell.iconId(), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            } else if (spell.itemStack() != null) {
                context.drawItem(spell.itemStack(), iconX, iconY);
            }

            if (spell.cooldown() > 0) {
                int k = iconY + (int) (iconSize * (1.0f - spell.cooldown()));
                int l = iconY + iconSize;
                context.fill(RenderLayer.getGuiOverlay(), iconX, k, iconX + iconSize, l, Integer.MAX_VALUE);
            }

            var kb = spell.keybinding();
            var mod = spell.modifier();
            int keyX = slotX + slotSize / 2;
            int keyY = slotY + 8;

            if (kb != null) {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 200);
                if (mod != null) {
                    int spacing = 1;
                    int modWidth = mod.width(textRenderer);
                    int keyWidth = kb.width(textRenderer);
                    int total = modWidth + keyWidth + spacing;
                    int left = keyX - (total / 2);

                    drawKeybinding(context, textRenderer, mod, left, keyY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                    drawKeybinding(context, textRenderer, kb, left + modWidth + spacing, keyY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                } else {
                    drawKeybinding(context, textRenderer, kb, keyX, keyY, Drawable.Anchor.CENTER, Drawable.Anchor.TRAILING);
                }
                context.getMatrices().pop();
            }
        }

        RenderSystem.disableBlend();
        context.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static void renderCustomLayout(DrawContext context, int screenWidth, int screenHeight, ViewModel viewModel, AddonHudConfig.LayoutStyle layout) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        var config = AutoConfig.getConfigHolder(AddonHudConfig.class).getConfig();

        List<SpellViewModel> spells = viewModel.spells();
        if (spells.isEmpty()) return;

        int leftWidth = 6;
        int centerWidth = 10;
        int rightWidth = 11;
        int slotWidth = leftWidth + centerWidth + rightWidth - 6;
        int slotHeight = 22;
        int iconSize = 16;
        Vec2f base = config.hotbar.origin.getPoint(screenWidth, screenHeight);
        Vec2f origin = base.add(config.hotbar.offset);
        Vec2f iconOffset = new Vec2f(3, 3);

        int columns = layout == AddonHudConfig.LayoutStyle.GRID_3x3_UP || layout == AddonHudConfig.LayoutStyle.GRID_3x3_DOWN ? 3 : 1;
        int rows = (int) Math.ceil((float) spells.size() / columns);
        float totalWidth = columns * slotWidth;
        float totalHeight = rows * slotHeight;

        Vec2f originOffset = new Vec2f(0, 0);
        if (layout == AddonHudConfig.LayoutStyle.VERTICAL_UP || layout == AddonHudConfig.LayoutStyle.GRID_3x3_UP) {
            originOffset = new Vec2f(0, -totalHeight);
        }

        Vec2f drawOrigin = origin.add(originOffset);
        Vec2f topLeft = drawOrigin;
        Vec2f bottomRight = drawOrigin.add(new Vec2f(totalWidth, totalHeight));
        SpellHotBarWidget.lastRendered = new Rect(topLeft, bottomRight);

        TextureFile background = new TextureFile(Identifier.of("textures/gui/sprites/hud/hotbar.png"), 182, 22);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < spells.size(); i++) {
            SpellViewModel spell = spells.get(i);
            int row = i / columns;
            int col = i % columns;

            int slotX = (int) drawOrigin.x + col * slotWidth;
            int slotY;
            switch (layout) {
                case VERTICAL_UP, GRID_3x3_UP -> slotY = (int) drawOrigin.y - row * slotHeight;
                case VERTICAL_DOWN, GRID_3x3_DOWN -> slotY = (int) drawOrigin.y + row * slotHeight;
                default -> slotY = (int) drawOrigin.y + row * slotHeight;
            }

            context.drawTexture(background.id(), slotX, slotY, 0, 0, leftWidth, slotHeight, background.width(), background.height());
            context.drawTexture(background.id(), slotX + leftWidth, slotY, 10, 0, centerWidth, slotHeight, background.width(), background.height());
            context.drawTexture(background.id(), slotX + leftWidth + centerWidth - 6, slotY, 170, 0, rightWidth, slotHeight, background.width(), background.height());

            int iconX = slotX + (int) iconOffset.x;
            int iconY = slotY + (int) iconOffset.y;
            if (spell.iconId() != null) {
                context.drawTexture(spell.iconId(), iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            } else if (spell.itemStack() != null) {
                context.drawItem(spell.itemStack(), iconX, iconY);
            }

            if (spell.cooldown() > 0) {
                int k = iconY + (int) (iconSize * (1.0f - spell.cooldown()));
                int l = iconY + iconSize;
                context.fill(RenderLayer.getGuiOverlay(), iconX, k, iconX + iconSize, l, Integer.MAX_VALUE);
            }

            var kb = spell.keybinding();
            var mod = spell.modifier();
            int keyX, keyY;

            if (layout == AddonHudConfig.LayoutStyle.GRID_3x3_UP || layout == AddonHudConfig.LayoutStyle.GRID_3x3_DOWN) {
                keyX = slotX + (slotWidth / 2 + 1);
                keyY = slotY + 8;
            } else {
                keyX = slotX + (slotWidth / 2 + 1) - 3;
                keyY = slotY + 8;
                if (i == 0) keyX += -1;
            }

            if (kb != null) {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 200);
                if (mod != null) {
                    int spacing = 1;
                    int modWidth = mod.width(textRenderer);
                    int keyWidth = kb.width(textRenderer);
                    int total = modWidth + keyWidth + spacing;
                    int left = keyX - (total / 2);

                    drawKeybinding(context, textRenderer, mod, left, keyY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                    drawKeybinding(context, textRenderer, kb, left + modWidth + spacing, keyY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                } else {
                    drawKeybinding(context, textRenderer, kb, keyX, keyY,
                            layout == AddonHudConfig.LayoutStyle.GRID_3x3_UP || layout == AddonHudConfig.LayoutStyle.GRID_3x3_DOWN
                                    ? Drawable.Anchor.CENTER : Drawable.Anchor.LEADING,
                            Drawable.Anchor.TRAILING);
                }
                context.getMatrices().pop();
            }
        }

        RenderSystem.disableBlend();
        context.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static void drawKeybinding(DrawContext context, TextRenderer textRenderer, SpellHotBarWidget.KeyBindingViewModel keybinding, int x, int y, Drawable.Anchor horizontalAnchor, Drawable.Anchor verticalAnchor) {
        if (keybinding.drawable() != null) {
            keybinding.drawable().draw(context, x, y, horizontalAnchor, verticalAnchor);
        } else {
            String label = keybinding.label();
            int width = textRenderer.getWidth(label);
            int xOffset = switch (horizontalAnchor) {
                case LEADING -> width / 2;
                case TRAILING -> -width / 2;
                case CENTER -> 0;
            };
            x += xOffset;

            HudKeyVisuals.buttonLeading.draw(context, x - (width / 2), y, Drawable.Anchor.TRAILING, verticalAnchor);
            HudKeyVisuals.buttonCenter.drawFlexibleWidth(context, x - (width / 2), y, width, verticalAnchor);
            HudKeyVisuals.buttonTrailing.draw(context, x + (width / 2), y, Drawable.Anchor.LEADING, verticalAnchor);
            context.drawCenteredTextWithShadow(textRenderer, label, x, y - 10, 0xFFFFFF);
        }
    }
}
