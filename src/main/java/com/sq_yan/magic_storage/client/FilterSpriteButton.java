package com.sq_yan.magic_storage.client;

import com.sq_yan.magic_storage.menu.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FilterSpriteButton extends AbstractButton {
    private static final int BG = 0xFF14141C;
    private static final int BORDER_IDLE = 0xFF2E2E42;
    private static final int BORDER_HOVER = 0xFFB0B0C0;
    private static final int BORDER_ACTIVE = 0xFF8848C0;
    private static final int GLOW = 0x408848C0;

    private final FilterMode mode;
    private final @Nullable ItemStack icon;
    private final Consumer<FilterMode> onSelect;
    private final Supplier<FilterMode> activeProvider;

    public FilterSpriteButton(int x, int y,
                              FilterMode mode,
                              @Nullable ItemStack icon,
                              Supplier<FilterMode> activeProvider,
                              Consumer<FilterMode> onSelect) {
        super(x, y, 18, 18, Component.translatable("gui.magic_storage.filter." + mode.name().toLowerCase()));
        this.mode = mode;
        this.icon = icon;
        this.activeProvider = activeProvider;
        this.onSelect = onSelect;
        this.setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    public void onPress(@NotNull InputWithModifiers input) {
        onSelect.accept(mode);
    }

    @Override
    protected void renderContents(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        boolean active = activeProvider.get() == mode;

        gfx.fill(x, y, x + 18, y + 18, BG);

        int border = active ? BORDER_ACTIVE : (isHovered() ? BORDER_HOVER : BORDER_IDLE);
        gfx.fill(x, y, x + 18, y + 1, border);
        gfx.fill(x, y + 17, x + 18, y + 18, border);
        gfx.fill(x, y, x + 1, y + 18, border);
        gfx.fill(x + 17, y, x + 18, y + 18, border);

        if (active) {
            gfx.fill(x + 1, y + 1, x + 17, y + 17, GLOW);
        }

        if (icon != null && !icon.isEmpty()) {
            gfx.renderItem(icon, x + 1, y + 1);
        } else {
            Minecraft mc = Minecraft.getInstance();
            String glyph = "✱";
            int tw = mc.font.width(glyph);
            int color = active ? 0xFFE6CCFF : (isHovered() ? 0xFFFFFFFF : 0xFFA0A0B0);
            gfx.drawString(mc.font, glyph, x + (18 - tw) / 2, y + 5, color, false);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {
        this.defaultButtonNarrationText(out);
    }
}
