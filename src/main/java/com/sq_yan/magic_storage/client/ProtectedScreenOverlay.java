package com.sq_yan.magic_storage.client;

import com.sq_yan.magic_storage.net.MSNetwork;
import com.sq_yan.magic_storage.net.UpdateProtectedSlotsPacket;
import com.sq_yan.magic_storage.protect.ProtectedSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;

/**
 * Cross-screen overlay: shows purple borders on protected player-inventory slots in ANY container screen
 * (vanilla inventory, chests, etc.) and handles the TOGGLE_PROTECT hotkey + mark-mode clicks globally.
 */
public final class ProtectedScreenOverlay {
    private static final int PROTECTED_BORDER = 0xFFC050FF;
    private static final int PROTECTED_OUTER = 0xFF6A2E9E;
    private static final int PROTECTED_BORDER_DIM = 0x66C050FF;
    private static final int SLOT_W = 16;

    /** Set while a settings screen is capturing a key rebind, so the mark hotkey doesn't fire instead. */
    private static volatile boolean suppressHotkey = false;

    public static void setSuppressHotkey(boolean v) { suppressHotkey = v; }

    private ProtectedScreenOverlay() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ProtectedScreenOverlay::onRender);
        MinecraftForge.EVENT_BUS.addListener(ProtectedScreenOverlay::onMouseClick);
        MinecraftForge.EVENT_BUS.addListener(ProtectedScreenOverlay::onKeyPressed);
        MinecraftForge.EVENT_BUS.addListener(ProtectedScreenOverlay::onClosing);
    }

    private static void onRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        // Our own MagicStorageScreen already draws its own borders; let it handle them to avoid double-render.
        if (screen instanceof MagicStorageScreen) {
            if (ProtectedSlotsCache.isMarkMode()) renderMarkCursor(e.getGuiGraphics(), e.getMouseX(), e.getMouseY());
            return;
        }
        renderProtectedBorders(screen, e.getGuiGraphics());
        if (ProtectedSlotsCache.isMarkMode()) renderMarkCursor(e.getGuiGraphics(), e.getMouseX(), e.getMouseY());
    }

    private static void renderProtectedBorders(AbstractContainerScreen<?> screen, GuiGraphics gfx) {
        Set<Integer> protectedSet = ProtectedSlotsCache.get();
        if (protectedSet.isEmpty()) return;
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        for (Slot s : screen.getMenu().slots) {
            if (!(s.container instanceof Inventory)) continue;
            int idx = s.getContainerSlot();
            if (!protectedSet.contains(idx)) continue;
            int sx = left + s.x - 1;
            int sy = top + s.y - 1;
            int sx2 = sx + SLOT_W + 2;
            int sy2 = sy + SLOT_W + 2;
            if (s.hasItem()) {
                drawBorder(gfx, sx, sy, sx2, sy2, PROTECTED_OUTER);
                drawBorder(gfx, sx + 1, sy + 1, sx2 - 1, sy2 - 1, PROTECTED_BORDER);
            } else {
                drawBorder(gfx, sx, sy, sx2, sy2, PROTECTED_BORDER_DIM);
            }
        }
    }

    private static void renderMarkCursor(GuiGraphics gfx, int mouseX, int mouseY) {
        String star = "★";
        var font = Minecraft.getInstance().font;
        int w = font.width(star);
        int bx0 = mouseX + 8, by0 = mouseY - 4;
        int bx1 = bx0 + w + 4, by1 = by0 + font.lineHeight + 3;
        gfx.fill(bx0, by0, bx1, by1, 0xE61A1024);
        drawBorder(gfx, bx0, by0, bx1, by1, PROTECTED_BORDER);
        gfx.drawString(font, star, bx0 + 2, by0 + 2, 0xFFE0B0FF, false);
    }

    private static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!ProtectedSlotsCache.isMarkMode()) return;
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (e.getButton() != 0) return;
        Slot hovered = findHovered(screen, e.getMouseX(), e.getMouseY());
        if (hovered == null) return;
        if (!(hovered.container instanceof Inventory)) return;
        int idx = hovered.getContainerSlot();
        if (idx < 0 || idx >= 36) return;
        attemptToggle(idx);
        // Always cancel the click while in mark mode on an inventory slot, even if the toggle was refused,
        // so vanilla slot logic (picking the item up) doesn't fire.
        e.setCanceled(true);
    }

    /**
     * Toggle protection for a player-inventory slot, enforcing the hotbar rule.
     * Hotbar slots (0..8) cannot be marked while hotbar protection is on — they're protected by default.
     * Returns true if the protection set actually changed.
     */
    public static boolean attemptToggle(int idx) {
        if (idx < 0 || idx >= 36) return false;
        if (idx < ProtectedSlots.HOTBAR_SLOTS && ProtectedSlotsCache.isHotbarProtected()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("message.magic_storage.hotbar_protected"), true);
            }
            return false;
        }
        ProtectedSlotsCache.toggle(idx);
        MSNetwork.CHANNEL.sendToServer(new UpdateProtectedSlotsPacket(ProtectedSlotsCache.toArray()));
        return true;
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre e) {
        if (suppressHotkey) return;
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (MagicStorageClient.TOGGLE_PROTECT.matches(e.getKeyCode(), e.getScanCode())) {
            // Quick-mark: hovering a player-inventory slot → toggle just that slot, no mode needed.
            Slot hovered = SlotAccess.hoveredSlot(screen);
            if (hovered != null && hovered.container instanceof Inventory) {
                int idx = hovered.getContainerSlot();
                if (idx >= 0 && idx < 36) {
                    attemptToggle(idx);
                    e.setCanceled(true);
                    return;
                }
            }
            // Otherwise toggle mark mode for click-based marking.
            ProtectedSlotsCache.toggleMarkMode();
            e.setCanceled(true);
        }
    }

    private static void onClosing(ScreenEvent.Closing e) {
        ProtectedSlotsCache.setMarkMode(false);
    }

    private static Slot findHovered(AbstractContainerScreen<?> screen, double mx, double my) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        for (Slot s : screen.getMenu().slots) {
            if (mx >= left + s.x && mx < left + s.x + SLOT_W && my >= top + s.y && my < top + s.y + SLOT_W) return s;
        }
        return null;
    }

    private static void drawBorder(GuiGraphics gfx, int x0, int y0, int x1, int y1, int color) {
        gfx.fill(x0, y0, x1, y0 + 1, color);
        gfx.fill(x0, y1 - 1, x1, y1, color);
        gfx.fill(x0, y0, x0 + 1, y1, color);
        gfx.fill(x1 - 1, y0, x1, y1, color);
    }
}
