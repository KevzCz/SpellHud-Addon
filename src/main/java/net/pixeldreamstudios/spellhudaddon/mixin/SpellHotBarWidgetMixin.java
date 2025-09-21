package net.pixeldreamstudios.spellhudaddon.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.pixeldreamstudios.spellhudaddon.SpellHudState;
import net.pixeldreamstudios.spellhudaddon.config.AddonHudConfig;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.Drawable;
import net.spell_engine.client.gui.HudElement;
import net.spell_engine.client.gui.HudKeyVisuals;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget.SpellViewModel;
import net.spell_engine.client.gui.HudRenderHelper.SpellHotBarWidget.ViewModel;
import net.spell_engine.client.input.SpellHotbar;
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
        SpellHudState.hasVisibleSpells = false;

        if (addonConfig.layout == AddonHudConfig.LayoutStyle.CENTERED_HORIZONTAL_ABOVE_HOTBAR) {
            List<SpellViewModel> spells = viewModel.spells();
            if (spells.isEmpty()) return;
            SpellHudState.hasVisibleSpells = true;
            renderCenteredHorizontalLayout(context, screenWidth, screenHeight, viewModel);
            ci.cancel();
            return;
        }

        if (addonConfig.layout == AddonHudConfig.LayoutStyle.HORIZONTAL) return;
        var engineOffset = SpellEngineClient.hudConfig.value.hotbar.offset;

        addonConfig.hotbar.offset = engineOffset;

        List<SpellViewModel> spells = viewModel.spells();
        if (spells.isEmpty()) return;

        SpellHudState.hasVisibleSpells = true;

        if (addonConfig.layout == AddonHudConfig.LayoutStyle.CIRCULAR_CLOCKWISE ||
                addonConfig.layout == AddonHudConfig.LayoutStyle.CIRCULAR_COUNTERCLOCKWISE) {
            renderCircularLayout(context, screenWidth, screenHeight, viewModel, addonConfig.layout);
            ci.cancel();
            return;
        }

        if (addonConfig.layout == AddonHudConfig.LayoutStyle.ROTATING_CORNER_RING) {
            renderRotatingCornerHalves(context, screenWidth, screenHeight, viewModel);
            ci.cancel();
            return;
        }

        renderCustomLayout(context, screenWidth, screenHeight, viewModel, addonConfig.layout);
        ci.cancel();
    }

    private static void renderCenteredHorizontalLayout(DrawContext context, int screenWidth, int screenHeight, ViewModel viewModel) {
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
        int totalWidth = spells.size() * slotWidth;
        boolean isCreative = MinecraftClient.getInstance().interactionManager != null &&
                MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isCreative();

        int creativeOffset = isCreative ? 24 : 0;
        int baseY = screenHeight - slotHeight - creativeOffset;

        Vec2f origin = new Vec2f((screenWidth - totalWidth) / 2f, baseY);
        Vec2f iconOffset = new Vec2f(3, 3);
        TextureFile background = new TextureFile(Identifier.of("textures/gui/sprites/hud/hotbar.png"), 182, 22);

        Rect bounds = new Rect(origin, origin.add(new Vec2f(totalWidth, slotHeight)));
        SpellHotBarWidget.lastRendered = bounds;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < spells.size(); i++) {
            SpellViewModel spell = spells.get(i);
            int slotX = (int) origin.x + i * slotWidth;
            int slotY = (int) origin.y;

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
            int keyX = slotX + (slotWidth / 2);
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

            int iconX = slotX + 3;
            int iconY = slotY + 3;
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
            originOffset = new Vec2f(0, -slotHeight);
        }

        Vec2f drawOrigin = origin.add(originOffset);
        Vec2f topLeft = drawOrigin;
        Vec2f bottomRight = drawOrigin.add(new Vec2f(totalWidth, totalHeight));

        if (layout == AddonHudConfig.LayoutStyle.VERTICAL_UP || layout == AddonHudConfig.LayoutStyle.GRID_3x3_UP) {
            topLeft = topLeft.add(new Vec2f(0, -totalHeight + 22));
            bottomRight = bottomRight.add(new Vec2f(0, -totalHeight + 22));
        }

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

    private static void renderRotatingCornerHalves(DrawContext context, int screenWidth, int screenHeight, ViewModel viewModel) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        var config = AutoConfig.getConfigHolder(AddonHudConfig.class).getConfig();

        List<SpellViewModel> spells = viewModel.spells();
        if (spells.isEmpty()) return;

        SpellHudState.ensureCapacity(spells.size());

        Vec2f center = config.hotbar.origin.getPoint(screenWidth, screenHeight).add(config.hotbar.offset);
        float r = SpellHudState.radius;
        int slotSize = 22;
        int iconSize = 16;
        int leftWidth = 6, midWidth = 10, rightWidth = 11;
        TextureFile background = new TextureFile(Identifier.of("textures/gui/sprites/hud/hotbar.png"), 182, 22);

        int anchor = anchorCornerIndex(config.hotbar.origin, center, screenWidth, screenHeight);
        SpellHudState.setAnchorCorner(anchor);

        int pressedGroup = detectPressedGroup(client);
        if (pressedGroup != -1 && pressedGroup != SpellHudState.visibleGroup) {
            SpellHudState.setVisibleGroup(pressedGroup);
        }
        int cdGroup = detectCooldownFiredGroup(spells);
        if (cdGroup != -1 && cdGroup != SpellHudState.visibleGroup) {
            SpellHudState.setVisibleGroup(cdGroup);
        }

        SpellHudState.tickRotation();


        Rect bounds = new Rect(
                center.add(new Vec2f(-r - slotSize, -r - slotSize)),
                center.add(new Vec2f(r + slotSize,  r + slotSize))
        );
        SpellHotBarWidget.lastRendered = bounds;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (spells.size() > 0) {
            int cx = (int) center.x - slotSize / 2;
            int cy = (int) center.y - slotSize / 2;
            drawSlot(context, background, cx, cy, leftWidth, midWidth, rightWidth, slotSize);
            drawIconAndCooldown(context, spells.get(0), cx + 3, cy + 3, iconSize);
            drawKeybind(context, textRenderer, spells.get(0), cx + slotSize / 2, cy + 8, true);
        }

        float baseCorner = cornerAngleForAnchor(anchor);

        float angleA = baseCorner + SpellHudState.rotationRad;
        float angleB = angleA + (float)Math.PI;

        int[] groupA = new int[]{1,2,3,4};
        int[] groupB = new int[]{5,6,7,8};

        float[] offsetsDeg = new float[]{-25f, -8f, 8f, 25f};

        {
            float cos = (float)Math.cos(angleA);
            float sin = (float)Math.sin(angleA);
            float dirX = cos, dirY = sin;
            float tanX = -sin, tanY = cos;

            float step = slotSize + SpellHudState.tangentGap;
            float centerIdx = (groupA.length - 1) * 0.5f;

            for (int j = 0; j < groupA.length; j++) {
                int idx = groupA[j];
                if (idx >= spells.size()) continue;

                float t = (j - centerIdx) * step;
                int sx = Math.round(center.x + dirX * r + tanX * t) - slotSize / 2;
                int sy = Math.round(center.y + dirY * r + tanY * t) - slotSize / 2;

                drawSlot(context, background, sx, sy, leftWidth, midWidth, rightWidth, slotSize);
                drawIconAndCooldown(context, spells.get(idx), sx + 3, sy + 3, iconSize);
                drawKeybind(context, textRenderer, spells.get(idx), sx + slotSize / 2, sy + 8, false);
            }
        }

        {
            float cos = (float)Math.cos(angleB);
            float sin = (float)Math.sin(angleB);
            float dirX = cos, dirY = sin;
            float tanX = -sin, tanY = cos;

            float step = slotSize + SpellHudState.tangentGap;
            float centerIdx = (groupB.length - 1) * 0.5f;

            for (int j = 0; j < groupB.length; j++) {
                int idx = groupB[j];
                if (idx >= spells.size()) continue;

                float t = (j - centerIdx) * step;
                int sx = Math.round(center.x + dirX * r + tanX * t) - slotSize / 2;
                int sy = Math.round(center.y + dirY * r + tanY * t) - slotSize / 2;

                drawSlot(context, background, sx, sy, leftWidth, midWidth, rightWidth, slotSize);
                drawIconAndCooldown(context, spells.get(idx), sx + 3, sy + 3, iconSize);
                drawKeybind(context, textRenderer, spells.get(idx), sx + slotSize / 2, sy + 8, false);
            }
        }

        RenderSystem.disableBlend();
        context.setShaderColor(1F, 1F, 1F, 1F);

        for (int i = 0; i < spells.size() && i < SpellHudState.lastCooldowns.length; i++) {
            SpellHudState.lastCooldowns[i] = spells.get(i).cooldown();
        }
    }


    private static int detectPressedGroup(MinecraftClient client) {
        int size = SpellHotbar.INSTANCE.slots.size();
        for (int i = 0; i < size && i < SpellHudState.lastKeyDown.length; i++) {
            var slot = SpellHotbar.INSTANCE.slots.get(i);
            KeyBinding kb = slot.getKeyBinding(client.options);
            boolean down = kb != null && kb.isPressed();

            boolean wasDown = SpellHudState.lastKeyDown[i];
            SpellHudState.lastKeyDown[i] = down;

            if (down && !wasDown) {
                if (i >= 1 && i <= 4) return 0;
                if (i >= 5 && i <= 8) return 1;
            }
        }
        return -1;
    }

    private static int detectCooldownFiredGroup(List<SpellViewModel> spells) {
        for (int i = 1; i < spells.size() && i < SpellHudState.lastCooldowns.length; i++) {
            float prev = SpellHudState.lastCooldowns[i];
            float now = spells.get(i).cooldown();
            if (prev < 0.1f && now > 0.3f) {
                if (i <= 4) return 0;
                if (i <= 8) return 1;
            }
        }
        return -1;
    }

    private static int anchorCornerIndex(HudElement.Origin origin, Vec2f pos, int sw, int sh) {
        if (origin == HudElement.Origin.TOP_LEFT) return 1;
        if (origin == HudElement.Origin.TOP_RIGHT) return 0;
        if (origin == HudElement.Origin.BOTTOM_LEFT) return 2;
        if (origin == HudElement.Origin.BOTTOM_RIGHT) return 3;
        boolean left = pos.x < sw * 0.5f;
        boolean top = pos.y < sh * 0.5f;
        if (top && !left) return 0;
        if (top && left) return 1;
        if (!top && left) return 2;
        return 3;
    }

    private static float cornerAngleForAnchor(int anchor) {

        if (anchor == 0) return (float) Math.toRadians(135);
        if (anchor == 1) return (float) Math.toRadians(45);
        if (anchor == 2) return (float) Math.toRadians(315);
        return (float) Math.toRadians(225);
    }



    private static void drawSlot(DrawContext ctx, TextureFile bg, int x, int y, int leftW, int midW, int rightW, int h) {
        ctx.drawTexture(bg.id(), x, y, 0, 0, leftW, h, bg.width(), bg.height());
        ctx.drawTexture(bg.id(), x + leftW, y, 10, 0, midW, h, bg.width(), bg.height());
        ctx.drawTexture(bg.id(), x + leftW + midW - 6, y, 170, 0, rightW, h, bg.width(), bg.height());
    }

    private static void drawIconAndCooldown(DrawContext ctx, SpellViewModel spell, int ix, int iy, int iconSize) {
        if (spell.iconId() != null) {
            ctx.drawTexture(spell.iconId(), ix, iy, 0, 0, iconSize, iconSize, iconSize, iconSize);
        } else if (spell.itemStack() != null) {
            ctx.drawItem(spell.itemStack(), ix, iy);
        }
        float cd = spell.cooldown();
        if (cd > 0f) {
            int k = iy + MathHelper.floor(iconSize * (1.0F - cd));
            int l = iy + MathHelper.ceil(iconSize * cd);
            ctx.fill(RenderLayer.getGuiOverlay(), ix, k, ix + iconSize, l, Integer.MAX_VALUE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void drawKeybind(DrawContext context, TextRenderer textRenderer, SpellViewModel spell, int x, int y, boolean centerAnchor) {
        var kb = spell.keybinding();
        var mod = spell.modifier();
        if (kb == null) return;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200);

        if (mod != null) {
            int spacing = 1;
            int modWidth = mod.width(textRenderer);
            int keyWidth = kb.width(textRenderer);
            int total = modWidth + keyWidth + spacing;
            int left = x - (total / 2);
            drawKeybinding(context, textRenderer, mod, left, y, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
            drawKeybinding(context, textRenderer, kb, left + modWidth + spacing, y, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
        } else {
            drawKeybinding(context, textRenderer, kb, x, y, centerAnchor ? Drawable.Anchor.CENTER : Drawable.Anchor.CENTER, Drawable.Anchor.TRAILING);
        }

        context.getMatrices().pop();
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
