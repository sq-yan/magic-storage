package com.sq_yan.magic_storage.client;

import com.sq_yan.magic_storage.menu.FilterMode;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.menu.SortMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MagicStorageScreen extends AbstractContainerScreen<MagicStorageMenu> {

    // Layout constants — match MagicStorageMenu for player inv coordinates.
    private static final int GRID_COLS = MagicStorageMenu.GRID_COLS;
    private static final int GRID_ROWS = MagicStorageMenu.GRID_ROWS;
    private static final int GRID_SIZE = MagicStorageMenu.GRID_SIZE;
    private static final int SLOT_SIZE = 18;
    private static final int OFFSCREEN = -2000;

    private static final int GUI_W = 256;
    private static final int GUI_H = 236;

    private static final int SEARCH_X = 8, SEARCH_Y = 4, SEARCH_W = 140, SEARCH_H = 12;
    private static final int DUMP_X = 152, SORT_X = 172, TOP_BUTTON_Y = 3, TOP_BUTTON_SIZE = 18;

    private static final int FILTER_X = 4, FILTER_Y = 22, FILTER_SIZE = 18;

    private static final int GRID_X = 24, GRID_Y = 22;
    private static final int SCROLLBAR_X = 242, SCROLLBAR_Y = 22, SCROLLBAR_W = 6;
    private static final int SCROLLBAR_H = GRID_ROWS * SLOT_SIZE;

    private static final int LABEL_Y = 134;

    // Palette
    private static final int PANEL_BG = 0xE6121220;        // dark matte glass, alpha ~90%
    private static final int PANEL_BORDER = 0xFF8848C0;     // outer purple frame
    private static final int INNER_PANEL = 0xFF1A1A28;      // search/scrollbar track bg
    private static final int SLOT_BG = 0xFF222236;
    private static final int SLOT_BORDER = 0xFF34344E;
    private static final int SCROLLBAR_THUMB = 0xFF8848C0;
    private static final int SCROLLBAR_THUMB_HL = 0xFFB078E8;
    private static final int LABEL_COLOR = 0xFFE0E0F0;
    private static final int FILL_BAR_BG = 0xFF101018;
    private static final int FILL_GREEN = 0xFF40C060;
    private static final int FILL_YELLOW = 0xFFE0C040;
    private static final int FILL_RED = 0xFFD04040;

    private static final int GLFW_KEY_ESCAPE = 256;

    private SortMode sortMode = SortMode.NONE;
    private FilterMode filterMode = FilterMode.NONE;
    private String searchText = "";
    private int scrollOffset = 0;
    private int[] orderedIndices = new int[0];

    private EditBox searchBox;
    private DarkSquareButton sortButton;
    private DarkSquareButton dumpButton;
    private boolean draggingScrollbar = false;

    public MagicStorageScreen(MagicStorageMenu menu, Inventory inv, Component title) {
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

        this.searchBox = new EditBox(this.font, this.leftPos + SEARCH_X + 2, this.topPos + SEARCH_Y + 2,
            SEARCH_W - 4, SEARCH_H - 4,
            Component.translatable("gui.magic_storage.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(0xFFE0E0F0);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.dumpButton = new DarkSquareButton(
            this.leftPos + DUMP_X, this.topPos + TOP_BUTTON_Y, TOP_BUTTON_SIZE,
            Component.literal("⇩"), dumpTooltip(), this::triggerQuickDump);
        this.addRenderableWidget(this.dumpButton);

        this.sortButton = new DarkSquareButton(
            this.leftPos + SORT_X, this.topPos + TOP_BUTTON_Y, TOP_BUTTON_SIZE,
            sortLabel(), sortTooltip(), this::onSortClicked);
        this.addRenderableWidget(this.sortButton);

        addFilterButton(0, FilterMode.NONE, null);
        addFilterButton(1, FilterMode.POTIONS, new ItemStack(Items.POTION));
        addFilterButton(2, FilterMode.ARMOR, new ItemStack(Items.IRON_CHESTPLATE));
        addFilterButton(3, FilterMode.TOOLS, new ItemStack(Items.IRON_PICKAXE));
        addFilterButton(4, FilterMode.FOOD, new ItemStack(Items.BREAD));
        addFilterButton(5, FilterMode.BLOCKS, new ItemStack(Items.COBBLESTONE));

        rebuildLayout();
    }

    private void addFilterButton(int slot, FilterMode mode, ItemStack icon) {
        this.addRenderableWidget(new FilterSpriteButton(
            this.leftPos + FILTER_X,
            this.topPos + FILTER_Y + slot * FILTER_SIZE,
            mode, icon,
            () -> this.filterMode,
            this::onFilterSelected));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        rebuildLayout();
    }

    private void onSearchChanged(String text) {
        if (!text.equals(this.searchText)) {
            this.searchText = text;
            this.scrollOffset = 0;
            rebuildLayout();
        }
    }

    private void onSortClicked() {
        this.sortMode = this.sortMode.next();
        this.sortButton.setGlyph(sortLabel());
        this.sortButton.setTooltipText(sortTooltip());
        this.scrollOffset = 0;
        rebuildLayout();
    }

    private void onFilterSelected(FilterMode mode) {
        if (this.filterMode != mode) {
            this.filterMode = mode;
            this.scrollOffset = 0;
            rebuildLayout();
        }
    }

    private void triggerQuickDump() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MagicStorageMenu.BUTTON_QUICK_DUMP);
        }
    }

    private Component sortLabel() {
        return Component.literal(switch (this.sortMode) {
            case NONE -> "·";
            case NAME -> "A";
            case COUNT -> "#";
            case MOD -> "M";
        });
    }

    private Component sortTooltip() {
        return Component.translatable("gui.magic_storage.sort." + sortMode.name().toLowerCase());
    }

    private Component dumpTooltip() {
        return Component.translatable("gui.magic_storage.dump",
            MagicStorageClient.QUICK_DUMP.getTranslatedKeyMessage());
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (MagicStorageClient.QUICK_DUMP.matches(event)) {
            triggerQuickDump();
            return true;
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (event.key() == GLFW_KEY_ESCAPE) {
                this.searchBox.setValue("");
                this.searchBox.setFocused(false);
                this.setFocused(null);
                return true;
            }
            if (this.searchBox.keyPressed(event)) return true;
            if (this.searchBox.canConsumeInput()) return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (super.mouseScrolled(mx, my, dx, dy)) return true;
        if (isInGridArea(mx, my) && maxScrollOffset() > 0) {
            this.scrollOffset = clampScroll(this.scrollOffset - (int) Math.signum(dy));
            rebuildLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOnScrollbar(event.x(), event.y())) {
            this.draggingScrollbar = true;
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (event.button() == 0) this.draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double ddx, double ddy) {
        if (this.draggingScrollbar) {
            updateScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, ddx, ddy);
    }

    private void updateScrollFromMouse(double my) {
        int max = maxScrollOffset();
        if (max <= 0) return;
        double relative = (my - (this.topPos + SCROLLBAR_Y)) / SCROLLBAR_H;
        this.scrollOffset = clampScroll((int) Math.round(relative * max));
        rebuildLayout();
    }

    private boolean isInGridArea(double mx, double my) {
        return mx >= this.leftPos + GRID_X
            && mx <= this.leftPos + GRID_X + GRID_COLS * SLOT_SIZE
            && my >= this.topPos + GRID_Y
            && my <= this.topPos + GRID_Y + GRID_ROWS * SLOT_SIZE;
    }

    private boolean isOnScrollbar(double mx, double my) {
        return mx >= this.leftPos + SCROLLBAR_X
            && mx <= this.leftPos + SCROLLBAR_X + SCROLLBAR_W
            && my >= this.topPos + SCROLLBAR_Y
            && my <= this.topPos + SCROLLBAR_Y + SCROLLBAR_H;
    }

    private int maxScrollOffset() {
        if (orderedIndices.length <= GRID_SIZE) return 0;
        int rows = (orderedIndices.length + GRID_COLS - 1) / GRID_COLS;
        return Math.max(0, rows - GRID_ROWS);
    }

    private int clampScroll(int s) {
        return Math.max(0, Math.min(s, maxScrollOffset()));
    }

    private void rebuildLayout() {
        int total = menu.getAggregatedSlotCount();
        List<Integer> visible = new ArrayList<>();
        List<Integer> empties = new ArrayList<>();
        String q = searchText.trim().toLowerCase();
        boolean filterActive = filterMode != FilterMode.NONE;

        for (int i = 0; i < total; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
                if (q.isEmpty() && !filterActive) empties.add(i);
                continue;
            }
            if (filterActive && !filterMode.accepts(stack)) continue;
            if (!q.isEmpty() && !stack.getHoverName().getString().toLowerCase().contains(q)) continue;
            visible.add(i);
        }
        if (sortMode != SortMode.NONE) visible.sort(comparator(sortMode));
        visible.addAll(empties);

        this.orderedIndices = visible.stream().mapToInt(Integer::intValue).toArray();
        this.scrollOffset = clampScroll(this.scrollOffset);

        for (int i = 0; i < total; i++) {
            Slot s = menu.slots.get(i);
            s.x = OFFSCREEN;
            s.y = OFFSCREEN;
        }
        int start = this.scrollOffset * GRID_COLS;
        for (int d = 0; d < GRID_SIZE; d++) {
            int vIdx = start + d;
            if (vIdx >= orderedIndices.length) break;
            int aggIdx = orderedIndices[vIdx];
            Slot s = menu.slots.get(aggIdx);
            s.x = GRID_X + (d % GRID_COLS) * SLOT_SIZE;
            s.y = GRID_Y + (d / GRID_COLS) * SLOT_SIZE;
        }
    }

    private Comparator<Integer> comparator(SortMode mode) {
        return (a, b) -> {
            ItemStack sa = menu.slots.get(a).getItem();
            ItemStack sb = menu.slots.get(b).getItem();
            return switch (mode) {
                case NAME -> sa.getHoverName().getString().compareToIgnoreCase(sb.getHoverName().getString());
                case COUNT -> Integer.compare(sb.getCount(), sa.getCount());
                case MOD -> modOf(sa).compareTo(modOf(sb));
                case NONE -> 0;
            };
        };
    }

    private String modOf(ItemStack stack) {
        Identifier id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.getNamespace();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int x1 = x0 + GUI_W;
        int y1 = y0 + GUI_H;

        // Main dark matte glass panel
        gfx.fill(x0, y0, x1, y1, PANEL_BG);
        drawBorder(gfx, x0, y0, x1, y1, PANEL_BORDER);

        // Search bar background + border
        int sx0 = x0 + SEARCH_X, sy0 = y0 + SEARCH_Y;
        int sx1 = sx0 + SEARCH_W, sy1 = sy0 + SEARCH_H;
        gfx.fill(sx0, sy0, sx1, sy1, INNER_PANEL);
        drawBorder(gfx, sx0, sy0, sx1, sy1, SLOT_BORDER);

        // Storage grid slot backgrounds
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                int sx = x0 + GRID_X + c * SLOT_SIZE;
                int sy = y0 + GRID_Y + r * SLOT_SIZE;
                drawSlotBg(gfx, sx, sy);
            }
        }

        // Mark empty grid cells beyond orderedIndices (filtered view smaller than 72)
        int start = this.scrollOffset * GRID_COLS;
        for (int d = 0; d < GRID_SIZE; d++) {
            int vIdx = start + d;
            if (vIdx < orderedIndices.length) continue;
            int sx = x0 + GRID_X + (d % GRID_COLS) * SLOT_SIZE;
            int sy = y0 + GRID_Y + (d / GRID_COLS) * SLOT_SIZE;
            gfx.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x401A1A28);
        }

        // Player inventory slot backgrounds (3 rows + hotbar)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = x0 + MagicStorageMenu.PLAYER_INV_X + c * SLOT_SIZE;
                int sy = y0 + MagicStorageMenu.PLAYER_INV_Y + r * SLOT_SIZE;
                drawSlotBg(gfx, sx, sy);
            }
        }
        for (int c = 0; c < 9; c++) {
            int sx = x0 + MagicStorageMenu.PLAYER_INV_X + c * SLOT_SIZE;
            int sy = y0 + MagicStorageMenu.HOTBAR_Y;
            drawSlotBg(gfx, sx, sy);
        }

        // Scrollbar track + thumb
        int trackX = x0 + SCROLLBAR_X;
        int trackY = y0 + SCROLLBAR_Y;
        gfx.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + SCROLLBAR_H, INNER_PANEL);
        drawBorder(gfx, trackX, trackY, trackX + SCROLLBAR_W, trackY + SCROLLBAR_H, SLOT_BORDER);

        int max = maxScrollOffset();
        if (max > 0) {
            int totalRows = (orderedIndices.length + GRID_COLS - 1) / GRID_COLS;
            int thumbH = Math.max(12, SCROLLBAR_H * GRID_ROWS / Math.max(1, totalRows));
            int thumbY = trackY + (SCROLLBAR_H - thumbH) * scrollOffset / max;
            gfx.fill(trackX + 1, thumbY, trackX + SCROLLBAR_W - 1, thumbY + thumbH, SCROLLBAR_THUMB);
            gfx.fill(trackX + 1, thumbY, trackX + SCROLLBAR_W - 1, thumbY + 1, SCROLLBAR_THUMB_HL);
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

    @Override
    protected void renderLabels(@NotNull GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        String indicator = menu.countUsedSlots() + " / " + menu.getAggregatedSlotCount();
        int indicatorX = GUI_W - 8 - this.font.width(indicator);
        gfx.drawString(this.font, indicator, indicatorX, this.inventoryLabelY, LABEL_COLOR, false);

        int barX0 = this.inventoryLabelX + this.font.width(this.playerInventoryTitle) + 8;
        int barX1 = indicatorX - 8;
        if (barX1 > barX0 + 12) renderFillBar(gfx, barX0, this.inventoryLabelY + 1, barX1, this.inventoryLabelY + 7);
    }

    private void renderFillBar(GuiGraphics gfx, int x0, int y0, int x1, int y1) {
        gfx.fill(x0, y0, x1, y1, FILL_BAR_BG);
        drawBorder(gfx, x0, y0, x1, y1, SLOT_BORDER);
        int total = menu.getAggregatedSlotCount();
        if (total <= 0) return;
        double ratio = (double) menu.countUsedSlots() / total;
        if (ratio <= 0) return;
        int fillColor = ratio <= 0.5 ? FILL_GREEN : (ratio <= 2.0 / 3.0 ? FILL_YELLOW : FILL_RED);
        int innerW = x1 - x0 - 2;
        int fillW = (int) Math.round(innerW * Math.min(1.0, ratio));
        if (fillW > 0) {
            gfx.fill(x0 + 1, y0 + 1, x0 + 1 + fillW, y1 - 1, fillColor);
        }
    }
}
