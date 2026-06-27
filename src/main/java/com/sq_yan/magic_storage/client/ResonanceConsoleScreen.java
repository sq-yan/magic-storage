package com.sq_yan.magic_storage.client;

import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import com.sq_yan.magic_storage.menu.ResonanceConsoleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

/**
 * Reigall's Resonance Console screen. Two modes share one window:
 * <ul>
 *   <li><b>Map</b> — an isometric (X/Y/Z) picture of the whole cell network relative to the Heart,
 *       so vertical stacks read as stacks just like in-world. Each node is coloured by its fill level.
 *       <b>Drag</b> from one cell onto another to move all of its contents; <b>click</b> a cell to open it.</li>
 *   <li><b>Cell</b> — the selected cell's 27 slots as live slots.</li>
 * </ul>
 * Like {@link MagicStorageScreen}, only the visible slots are positioned on-grid; the rest sit
 * off-screen. A Defragment button (available in both modes) compacts the network.
 */
public class ResonanceConsoleScreen extends AbstractContainerScreen<ResonanceConsoleMenu> {

    private static final int CELL_SIZE = StorageCellBlockEntity.SIZE;
    private static final int SLOT_SIZE = 18;
    private static final int OFFSCREEN = -2000;

    private static final int GUI_W = 256;
    private static final int GUI_H = 236;

    private static final int TOP_Y = 4;
    private static final int BACK_X = 8, BACK_W = 48;
    private static final int DEFRAG_W = 54;
    private static final int DEFRAG_X = GUI_W - 8 - DEFRAG_W;

    private static final int MAP_X = 10, MAP_Y = 24, MAP_W = 236, MAP_H = 100;
    private static final int STATUS_Y = MAP_Y + MAP_H + 2;

    private static final int CELL_GRID_X = 47, CELL_GRID_Y = 42;
    private static final int LABEL_Y = 134;

    // Depth nudge (only applied when the network has depth, so flat walls stay a clean grid)
    private static final double DEPTH_H = 0.4;
    private static final double DEPTH_V = -0.2;

    // Palette (mirrors MagicStorageScreen)
    private static final int PANEL_BG = 0xE6121220;
    private static final int PANEL_BORDER = 0xFF8848C0;
    private static final int INNER_PANEL = 0xFF1A1A28;
    private static final int SLOT_BG = 0xFF222236;
    private static final int SLOT_BORDER = 0xFF34344E;
    private static final int LABEL_COLOR = 0xFFE0E0F0;
    private static final int LABEL_MUTED = 0xFFB0B0C8;
    private static final int LABEL_FAINT = 0xFF8484A0;
    private static final int FILL_GREEN = 0xFF40C060;
    private static final int FILL_YELLOW = 0xFFE0C040;
    private static final int FILL_RED = 0xFFD04040;
    private static final int FILL_EMPTY = 0xFF3A3A4A;
    private static final int NODE_BORDER = 0xFF0C0C14;
    private static final int SELECT_BORDER = 0xFFFFFFFF;
    private static final int HEART_COLOR = 0xFFE0506A;
    private static final int HEART_BORDER = 0xFFE0C040;
    private static final int CONNECTOR = 0x55C8C8E0;
    private static final int DRAG_LINE = 0xFFE0B0FF;
    private static final int TARGET_HL = 0xFF40E0A0;

    private static final int GLFW_KEY_ESCAPE = 256;

    private boolean inCell = false;
    private int selectedCell = -1;
    private int dragFrom = -1;

    // Camera-relative ground axes (snapped to a cardinal direction at open time) so the map's
    // left/right matches how the player was facing the build — no skew, no mirroring.
    private int rightX = 1, rightZ = 0; // screen-right world direction
    private int fwdX = 0, fwdZ = 1;     // into-screen world direction

    private DarkSquareButton defragButton;
    private DarkSquareButton backButton;

    public ResonanceConsoleScreen(ResonanceConsoleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = LABEL_Y;
        this.inventoryLabelX = 8;
        this.titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        computeViewAxes();

        this.defragButton = new DarkSquareButton(
            this.leftPos + DEFRAG_X, this.topPos + TOP_Y, DEFRAG_W, 14,
            Component.translatable("gui.magic_storage.console.defrag"),
            Component.translatable("gui.magic_storage.console.defrag.tooltip"),
            this::triggerDefrag);
        this.addRenderableWidget(this.defragButton);

        this.backButton = new DarkSquareButton(
            this.leftPos + BACK_X, this.topPos + TOP_Y, BACK_W, 14,
            Component.translatable("gui.magic_storage.console.back"),
            Component.translatable("gui.magic_storage.console.back"),
            this::backToMap);
        this.addRenderableWidget(this.backButton);

        clampSelection();
        applyVisibility();
        rebuildLayout();
    }

    private void clampSelection() {
        if (selectedCell >= menu.getCellCount()) {
            selectedCell = -1;
            inCell = false;
        }
    }

    private void applyVisibility() {
        this.backButton.visible = inCell;
    }

    private void backToMap() {
        this.inCell = false;
        applyVisibility();
        rebuildLayout();
    }

    private void selectCell(int idx) {
        this.selectedCell = idx;
        this.inCell = true;
        applyVisibility();
        rebuildLayout();
    }

    private void triggerDefrag() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResonanceConsoleMenu.BUTTON_DEFRAG);
        }
    }

    private void sendTransfer(int from, int to) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            int id = ResonanceConsoleMenu.TRANSFER_BASE + from * 100 + to;
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ---- slot layout -------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        clampSelection();
        applyVisibility();
        rebuildLayout();
    }

    private void rebuildLayout() {
        int total = menu.getAggregatedSlotCount();
        for (int i = 0; i < total; i++) {
            Slot s = menu.slots.get(i);
            SlotAccess.setPos(s, OFFSCREEN, OFFSCREEN);
        }
        if (!inCell || selectedCell < 0) return;
        int base = selectedCell * CELL_SIZE;
        for (int j = 0; j < CELL_SIZE && base + j < total; j++) {
            Slot s = menu.slots.get(base + j);
            SlotAccess.setPos(s, CELL_GRID_X + (j % 9) * SLOT_SIZE, CELL_GRID_Y + (j / 9) * SLOT_SIZE);
        }
    }

    // ---- isometric map layout ----------------------------------------------

    /** Screen-space node centres + node size for the current network. Index {@code n} is the heart. */
    private record Layout(int[] cx, int[] cy, int size, int heartX, int heartY) {}

    /** Snap the player's facing to a cardinal and derive screen-right / into-screen world axes. */
    private void computeViewAxes() {
        float yaw = (this.minecraft != null && this.minecraft.player != null)
            ? this.minecraft.player.getYRot() : 0f;
        int card = Math.floorMod(Math.round(yaw / 90f), 4);
        int[][] looks = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}}; // yaw 0=south(+Z), 90=west, 180=north, 270=east
        int lx = looks[card][0], lz = looks[card][1];
        fwdX = lx; fwdZ = lz;
        rightX = -lz; rightZ = lx; // screen-right = forward rotated so it matches the camera
    }

    private Layout layout() {
        java.util.List<BlockPos> cells = menu.getCellPositions();
        BlockPos heart = menu.getHeartPos();
        int n = cells.size();

        // Pre-pass: vertical and depth spans decide projection mode + whether to nudge for depth.
        int minDy = 0, maxDy = 0, minDepth = 0, maxDepth = 0;
        for (BlockPos p : cells) {
            int dx = p.getX() - heart.getX();
            int dy = p.getY() - heart.getY();
            int dz = p.getZ() - heart.getZ();
            int depth = dx * fwdX + dz * fwdZ;
            minDy = Math.min(minDy, dy); maxDy = Math.max(maxDy, dy);
            minDepth = Math.min(minDepth, depth); maxDepth = Math.max(maxDepth, depth);
        }
        boolean elevation = (maxDy - minDy) > 0;          // has vertical structure → front elevation
        double nudge = (maxDepth - minDepth) > 0 ? 1.0 : 0.0;

        double[] ux = new double[n + 1];
        double[] uy = new double[n + 1];
        double minUx = 0, maxUx = 0, minUy = 0, maxUy = 0; // heart at origin always included
        for (int i = 0; i < n; i++) {
            BlockPos p = cells.get(i);
            int dx = p.getX() - heart.getX();
            int dy = p.getY() - heart.getY();
            int dz = p.getZ() - heart.getZ();
            int h = dx * rightX + dz * rightZ;
            int depth = dx * fwdX + dz * fwdZ;
            if (elevation) {
                ux[i] = h + depth * DEPTH_H * nudge;
                uy[i] = -dy + depth * DEPTH_V * nudge;
            } else {
                ux[i] = h;
                uy[i] = -depth; // top-down, oriented to camera (further away = higher)
            }
            minUx = Math.min(minUx, ux[i]); maxUx = Math.max(maxUx, ux[i]);
            minUy = Math.min(minUy, uy[i]); maxUy = Math.max(maxUy, uy[i]);
        }
        // heart
        ux[n] = 0; uy[n] = 0;

        double spanX = maxUx - minUx;
        double spanY = maxUy - minUy;
        int margin = 16;
        double availW = MAP_W - margin * 2;
        double availH = MAP_H - margin * 2;
        double pxPerUnit = Math.min(
            spanX > 0 ? availW / spanX : 1e9,
            spanY > 0 ? availH / spanY : 1e9);
        pxPerUnit = Math.max(10, Math.min(pxPerUnit, 26));
        int size = (int) Math.max(7, Math.min(pxPerUnit * 0.42, 14));

        double midUx = (minUx + maxUx) / 2;
        double midUy = (minUy + maxUy) / 2;
        int cxc = this.leftPos + MAP_X + MAP_W / 2;
        int cyc = this.topPos + MAP_Y + MAP_H / 2;

        int[] cx = new int[n];
        int[] cy = new int[n];
        for (int i = 0; i < n; i++) {
            cx[i] = (int) Math.round(cxc + (ux[i] - midUx) * pxPerUnit);
            cy[i] = (int) Math.round(cyc + (uy[i] - midUy) * pxPerUnit);
        }
        int hx = (int) Math.round(cxc + (0 - midUx) * pxPerUnit);
        int hy = (int) Math.round(cyc + (0 - midUy) * pxPerUnit);
        return new Layout(cx, cy, size, hx, hy);
    }

    private int fillColor(int cellIdx) {
        int used = menu.usedInCell(cellIdx);
        if (used == 0) return FILL_EMPTY;
        double ratio = (double) used / CELL_SIZE;
        return ratio <= 0.5 ? FILL_GREEN : (ratio <= 2.0 / 3.0 ? FILL_YELLOW : FILL_RED);
    }

    /** Index of the cell whose node contains the cursor (topmost first), or -1. */
    private int cellAt(double mx, double my) {
        Layout lo = layout();
        int half = lo.size / 2 + 1;
        for (int i = lo.cx.length - 1; i >= 0; i--) {
            if (mx >= lo.cx[i] - half && mx <= lo.cx[i] + half
                && my >= lo.cy[i] - half && my <= lo.cy[i] + half) return i;
        }
        return -1;
    }

    // ---- input -------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW_KEY_ESCAPE) {
            if (dragFrom >= 0) { dragFrom = -1; return true; }
            if (inCell) { backToMap(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!inCell && button == 0) {
            int idx = cellAt(mx, my);
            if (idx >= 0) {
                dragFrom = idx; // resolved on release: same node = open, other node = transfer
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragFrom >= 0) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragFrom >= 0 && button == 0) {
            int target = cellAt(mx, my);
            int from = dragFrom;
            dragFrom = -1;
            if (target == from) {
                selectCell(from);
            } else if (target >= 0) {
                sendTransfer(from, target);
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    // ---- rendering ---------------------------------------------------------

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos, y0 = this.topPos;
        gfx.fill(x0, y0, x0 + GUI_W, y0 + GUI_H, PANEL_BG);
        drawBorder(gfx, x0, y0, x0 + GUI_W, y0 + GUI_H, PANEL_BORDER);

        if (inCell) renderCellView(gfx, x0, y0);
        else renderMapView(gfx, x0, y0, mouseX, mouseY);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBg(gfx, x0 + ResonanceConsoleMenu.PLAYER_INV_X + c * SLOT_SIZE,
                    y0 + ResonanceConsoleMenu.PLAYER_INV_Y + r * SLOT_SIZE);
            }
        }
        for (int c = 0; c < 9; c++) {
            drawSlotBg(gfx, x0 + ResonanceConsoleMenu.PLAYER_INV_X + c * SLOT_SIZE,
                y0 + ResonanceConsoleMenu.HOTBAR_Y);
        }
    }

    private void renderMapView(GuiGraphics gfx, int x0, int y0, int mouseX, int mouseY) {
        int px0 = x0 + MAP_X, py0 = y0 + MAP_Y;
        int px1 = px0 + MAP_W, py1 = py0 + MAP_H;
        gfx.fill(px0, py0, px1, py1, INNER_PANEL);
        drawBorder(gfx, px0, py0, px1, py1, SLOT_BORDER);

        java.util.List<BlockPos> cells = menu.getCellPositions();
        BlockPos heart = menu.getHeartPos();
        Layout lo = layout();
        int n = cells.size();
        int half = lo.size / 2;

        // Structure connectors (face-adjacent links), drawn under the nodes.
        for (int i = 0; i < n; i++) {
            BlockPos a = cells.get(i);
            if (a.distManhattan(heart) == 1) {
                drawLine(gfx, lo.cx[i], lo.cy[i], lo.heartX, lo.heartY, CONNECTOR);
            }
            for (int j = i + 1; j < n; j++) {
                if (a.distManhattan(cells.get(j)) == 1) {
                    drawLine(gfx, lo.cx[i], lo.cy[i], lo.cx[j], lo.cy[j], CONNECTOR);
                }
            }
        }

        // Heart marker
        drawNode(gfx, lo.heartX, lo.heartY, half, HEART_COLOR, HEART_BORDER);

        // Cell nodes
        int hovered = cellAt(mouseX, mouseY);
        for (int i = 0; i < n; i++) {
            int border = NODE_BORDER;
            if (i == selectedCell) border = SELECT_BORDER;
            if (dragFrom >= 0 && i == hovered && i != dragFrom) border = TARGET_HL;
            drawNode(gfx, lo.cx[i], lo.cy[i], half, fillColor(i), border);
        }

        // Drag line from source to cursor
        if (dragFrom >= 0 && dragFrom < n) {
            drawLine(gfx, lo.cx[dragFrom], lo.cy[dragFrom], mouseX, mouseY, DRAG_LINE);
        }

        String status = Component.translatable("gui.magic_storage.console.cells", n).getString();
        gfx.drawString(this.font, status, px0 + 2, y0 + STATUS_Y, LABEL_FAINT, false);

        Component title = Component.translatable("gui.magic_storage.console.title");
        gfx.drawString(this.font, title, x0 + (GUI_W - this.font.width(title)) / 2, y0 + TOP_Y + 3, LABEL_COLOR, false);
    }

    private void renderCellView(GuiGraphics gfx, int x0, int y0) {
        java.util.List<BlockPos> cells = menu.getCellPositions();
        BlockPos heart = menu.getHeartPos();
        if (selectedCell >= 0 && selectedCell < cells.size()) {
            BlockPos p = cells.get(selectedCell);
            String coords = Component.translatable("gui.magic_storage.console.cell",
                p.getX() - heart.getX(), p.getY() - heart.getY(), p.getZ() - heart.getZ()).getString();
            gfx.drawString(this.font, coords, x0 + BACK_X + BACK_W + 6, y0 + TOP_Y + 3, LABEL_MUTED, false);
            String used = menu.usedInCell(selectedCell) + " / " + CELL_SIZE;
            gfx.drawString(this.font, used, x0 + DEFRAG_X - 8 - this.font.width(used), y0 + TOP_Y + 3, LABEL_COLOR, false);
        }
        for (int j = 0; j < CELL_SIZE; j++) {
            drawSlotBg(gfx, x0 + CELL_GRID_X + (j % 9) * SLOT_SIZE, y0 + CELL_GRID_Y + (j / 9) * SLOT_SIZE);
        }
        gfx.drawString(this.font, Component.translatable("gui.magic_storage.console.cell.hint"),
            x0 + CELL_GRID_X, y0 + CELL_GRID_Y + 3 * SLOT_SIZE + 4, LABEL_FAINT, false);
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
        if (!inCell && dragFrom < 0) {
            int idx = cellAt(mouseX, mouseY);
            if (idx >= 0) renderNodeTooltip(gfx, idx, mouseX, mouseY);
        }
    }

    private void renderNodeTooltip(GuiGraphics gfx, int cellIdx, int mouseX, int mouseY) {
        BlockPos heart = menu.getHeartPos();
        BlockPos p = menu.getCellPositions().get(cellIdx);
        String l1 = Component.translatable("gui.magic_storage.console.cell",
            p.getX() - heart.getX(), p.getY() - heart.getY(), p.getZ() - heart.getZ()).getString();
        String l2 = menu.usedInCell(cellIdx) + " / " + CELL_SIZE;
        int w = Math.max(this.font.width(l1), this.font.width(l2));
        int bx0 = mouseX + 10, by0 = mouseY - 4;
        int bx1 = bx0 + w + 6, by1 = by0 + this.font.lineHeight * 2 + 6;
        gfx.fill(bx0, by0, bx1, by1, 0xF0101018);
        drawBorder(gfx, bx0, by0, bx1, by1, PANEL_BORDER);
        gfx.drawString(this.font, l1, bx0 + 3, by0 + 3, LABEL_COLOR, false);
        gfx.drawString(this.font, l2, bx0 + 3, by0 + 3 + this.font.lineHeight, LABEL_MUTED, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    private static void drawNode(GuiGraphics gfx, int cx, int cy, int half, int fill, int border) {
        int x0 = cx - half, y0 = cy - half, x1 = cx + half + 1, y1 = cy + half + 1;
        gfx.fill(x0, y0, x1, y1, fill);
        drawBorder(gfx, x0, y0, x1, y1, border);
    }

    /** Thin line via Bresenham (GuiGraphics has no line primitive); fine for the few short links here. */
    private static void drawLine(GuiGraphics gfx, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            gfx.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private static void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG);
        drawBorder(gfx, x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BORDER);
    }

    private static void drawBorder(GuiGraphics gfx, int x0, int y0, int x1, int y1, int color) {
        gfx.fill(x0, y0, x1, y0 + 1, color);
        gfx.fill(x0, y1 - 1, x1, y1, color);
        gfx.fill(x0, y0, x0 + 1, y1, color);
        gfx.fill(x1 - 1, y0, x1, y1, color);
    }
}
