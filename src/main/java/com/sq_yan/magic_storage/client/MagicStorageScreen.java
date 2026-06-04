package com.sq_yan.magic_storage.client;

import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.menu.SortMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MagicStorageScreen extends AbstractContainerScreen<MagicStorageMenu> {
    private static final Identifier BG =
        Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int GRID_COLS = MagicStorageMenu.GRID_COLS;
    private static final int GRID_ROWS = MagicStorageMenu.GRID_ROWS;
    private static final int GRID_SIZE = MagicStorageMenu.GRID_SIZE;
    private static final int OFFSCREEN = -2000;
    private static final int LABEL_COLOR = 0xFF404040;

    private static final int SCROLLBAR_X = 170;
    private static final int SCROLLBAR_Y = 18;
    private static final int SCROLLBAR_W = 5;
    private static final int SCROLLBAR_H = GRID_ROWS * 18 - 2;

    private static final int GLFW_KEY_ESCAPE = 256;

    private SortMode sortMode = SortMode.NONE;
    private String searchText = "";
    private int scrollOffset = 0;
    private int[] orderedIndices = new int[0];

    private EditBox searchBox;
    private Button sortButton;
    private boolean draggingScrollbar = false;

    public MagicStorageScreen(MagicStorageMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.leftPos + 55, this.topPos + 4, 90, 12,
            Component.translatable("gui.magic_storage.search"));
        this.searchBox.setBordered(true);
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.sortButton = Button.builder(sortLabel(), btn -> onSortClicked())
            .bounds(this.leftPos + 152, this.topPos + 3, 18, 14).build();
        this.sortButton.setTooltip(Tooltip.create(sortTooltip()));
        this.addRenderableWidget(this.sortButton);

        this.setInitialFocus(this.searchBox);
        rebuildLayout();
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
        this.sortButton.setMessage(sortLabel());
        this.sortButton.setTooltip(Tooltip.create(sortTooltip()));
        this.scrollOffset = 0;
        rebuildLayout();
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

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (event.key() == GLFW_KEY_ESCAPE) {
                this.searchBox.setFocused(false);
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
        return mx >= this.leftPos + 7 && mx <= this.leftPos + 169
            && my >= this.topPos + 17 && my <= this.topPos + 125;
    }

    private boolean isOnScrollbar(double mx, double my) {
        return mx >= this.leftPos + SCROLLBAR_X && mx <= this.leftPos + SCROLLBAR_X + SCROLLBAR_W
            && my >= this.topPos + SCROLLBAR_Y && my <= this.topPos + SCROLLBAR_Y + SCROLLBAR_H;
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

        for (int i = 0; i < total; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
                if (q.isEmpty()) empties.add(i);
                continue;
            }
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
            s.x = 8 + (d % GRID_COLS) * 18;
            s.y = 18 + (d / GRID_COLS) * 18;
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
        gfx.blit(RenderPipelines.GUI_TEXTURED, BG,
            this.leftPos, this.topPos, 0f, 0f,
            this.imageWidth, this.imageHeight, 256, 256);

        // Paint over slot holes that have no aggregated slot in this view —
        // generic_54.png draws 9×6 holes statically, but our active grid may be smaller.
        int start = this.scrollOffset * GRID_COLS;
        for (int d = 0; d < GRID_SIZE; d++) {
            int vIdx = start + d;
            if (vIdx < orderedIndices.length) continue;
            int col = d % GRID_COLS;
            int row = d / GRID_COLS;
            int x = this.leftPos + 7 + col * 18;
            int y = this.topPos + 17 + row * 18;
            gfx.fill(x, y, x + 18, y + 18, 0xFFC6C6C6);
        }

        int max = maxScrollOffset();
        int trackTop = this.topPos + SCROLLBAR_Y;
        int trackX = this.leftPos + SCROLLBAR_X;
        gfx.fill(trackX, trackTop, trackX + SCROLLBAR_W, trackTop + SCROLLBAR_H, 0xFF2A2A2A);
        if (max > 0) {
            int totalRows = (orderedIndices.length + GRID_COLS - 1) / GRID_COLS;
            int thumbH = Math.max(12, SCROLLBAR_H * GRID_ROWS / Math.max(1, totalRows));
            int thumbY = trackTop + (SCROLLBAR_H - thumbH) * scrollOffset / max;
            gfx.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, 0xFFAAAAAA);
            gfx.fill(trackX, thumbY, trackX + SCROLLBAR_W - 1, thumbY + 1, 0xFFFFFFFF);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gfx, int mouseX, int mouseY) {
        String indicator = menu.countUsedSlots() + " / " + menu.getAggregatedSlotCount();
        gfx.drawString(this.font, indicator, 8, 6, LABEL_COLOR, false);
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }
}
