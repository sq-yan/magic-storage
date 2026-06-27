package com.sq_yan.magic_storage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class DarkSquareButton extends AbstractButton {
    private static final int BG = 0xFF14141C;
    private static final int BORDER_IDLE = 0xFF2E2E42;
    private static final int BORDER_HOVER = 0xFFB0B0C0;
    private static final int TEXT_IDLE = 0xFFD0D0E0;
    private static final int TEXT_HOVER = 0xFFFFFFFF;

    private Component glyph;
    private final Runnable onClick;

    public DarkSquareButton(int x, int y, int size, Component glyph, Component tooltip, Runnable onClick) {
        this(x, y, size, size, glyph, tooltip, onClick);
    }

    public DarkSquareButton(int x, int y, int width, int height, Component glyph, Component tooltip, Runnable onClick) {
        super(x, y, width, height, tooltip);
        this.glyph = glyph;
        this.onClick = onClick;
        this.setTooltip(Tooltip.create(tooltip));
    }

    public void setGlyph(Component glyph) {
        this.glyph = glyph;
    }

    public void setTooltipText(Component tooltip) {
        this.setTooltip(Tooltip.create(tooltip));
        this.setMessage(tooltip);
    }

    @Override
    public void onPress() {
        onClick.run();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        gfx.fill(x, y, x + w, y + h, BG);

        int border = isHovered() ? BORDER_HOVER : BORDER_IDLE;
        gfx.fill(x, y, x + w, y + 1, border);
        gfx.fill(x, y + h - 1, x + w, y + h, border);
        gfx.fill(x, y, x + 1, y + h, border);
        gfx.fill(x + w - 1, y, x + w, y + h, border);

        Minecraft mc = Minecraft.getInstance();
        String text = glyph.getString();
        int tw = mc.font.width(text);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - mc.font.lineHeight) / 2 + 1;
        gfx.drawString(mc.font, text, tx, ty, isHovered() ? TEXT_HOVER : TEXT_IDLE, false);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {
        this.defaultButtonNarrationText(out);
    }
}
