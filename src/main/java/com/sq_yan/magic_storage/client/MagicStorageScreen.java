package com.sq_yan.magic_storage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sq_yan.magic_storage.menu.FilterMode;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.menu.SortMode;
import com.sq_yan.magic_storage.net.MSNetwork;
import com.sq_yan.magic_storage.net.UpdateProtectedSlotsPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;
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

    private static final int SEARCH_X = 8, SEARCH_Y = 4, SEARCH_W = 124, SEARCH_H = 12;
    private static final int SETTINGS_X = 136, DUMP_X = 156, SORT_X = 176, TOP_BUTTON_Y = 3, TOP_BUTTON_SIZE = 18;

    private static final int FILTER_X = 4, FILTER_Y = 22, FILTER_SIZE = 18;

    private static final int GRID_X = 24, GRID_Y = 22;
    private static final int SCROLLBAR_X = 242, SCROLLBAR_Y = 22, SCROLLBAR_W = 6;
    private static final int SCROLLBAR_H = GRID_ROWS * SLOT_SIZE;

    private static final int LABEL_Y = 134;

    // Settings panel layout — occupies grid area when active
    private static final int SETTINGS_X0 = GRID_X;
    private static final int SETTINGS_Y0 = GRID_Y;
    private static final int SETTINGS_W = GRID_COLS * SLOT_SIZE;
    private static final int SETTINGS_H = GRID_ROWS * SLOT_SIZE;

    // Palette
    private static final int PANEL_BG = 0xE6121220;
    private static final int PANEL_BORDER = 0xFF8848C0;
    private static final int INNER_PANEL = 0xFF1A1A28;
    private static final int SLOT_BG = 0xFF222236;
    private static final int SLOT_BORDER = 0xFF34344E;
    private static final int SCROLLBAR_THUMB = 0xFF8848C0;
    private static final int SCROLLBAR_THUMB_HL = 0xFFB078E8;
    private static final int LABEL_COLOR = 0xFFE0E0F0;
    private static final int FILL_BAR_BG = 0xFF101018;
    private static final int FILL_GREEN = 0xFF40C060;
    private static final int FILL_YELLOW = 0xFFE0C040;
    private static final int FILL_RED = 0xFFD04040;
    private static final int PROTECTED_BORDER = 0xFFC050FF;
    private static final int PROTECTED_OUTER = 0xFF6A2E9E;
    private static final int PROTECTED_BORDER_DIM = 0x66C050FF;
    private static final int LABEL_MUTED = 0xFFB0B0C8;
    private static final int LABEL_FAINT = 0xFF8484A0;

    private static final int GLFW_KEY_ESCAPE = 256;

    private SortMode sortMode = SortMode.NONE;
    private FilterMode filterMode = FilterMode.NONE;
    private String searchText = "";
    private int scrollOffset = 0;
    private int[] orderedIndices = new int[0];

    private EditBox searchBox;
    private DarkSquareButton sortButton;
    private DarkSquareButton dumpButton;
    private DarkSquareButton settingsButton;
    private DarkSquareButton rebindButton;
    private DarkSquareButton protectRebindButton;
    private DarkSquareButton hotbarToggleButton;
    private DarkSquareButton markToggleButton;
    private DarkSquareButton backButton;
    private final List<AbstractWidget> gridModeWidgets = new ArrayList<>();
    private final List<AbstractWidget> settingsModeWidgets = new ArrayList<>();
    private boolean draggingScrollbar = false;

    private boolean inSettings = false;
    private KeyMapping awaitingRebind = null;
    private DarkSquareButton awaitingButton = null;

    private static final int SUGGEST_X = SEARCH_X;
    private static final int SUGGEST_Y = SEARCH_Y + SEARCH_H + 1;
    private static final int SUGGEST_W = SEARCH_W;
    private static final int SUGGEST_ROW_H = 12;
    private static final int SUGGEST_MAX = 6;
    private List<String> suggestions = new ArrayList<>();
    private int suggestionsHover = -1;

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
        gridModeWidgets.clear();
        settingsModeWidgets.clear();

        this.searchBox = new EditBox(this.font, this.leftPos + SEARCH_X + 2, this.topPos + SEARCH_Y + 2,
            SEARCH_W - 4, SEARCH_H - 4,
            Component.translatable("gui.magic_storage.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(0xFFE0E0F0);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
        gridModeWidgets.add(this.searchBox);

        this.settingsButton = new DarkSquareButton(
            this.leftPos + SETTINGS_X, this.topPos + TOP_BUTTON_Y, TOP_BUTTON_SIZE,
            Component.literal("⚙"),
            Component.translatable("gui.magic_storage.settings"),
            this::openSettings);
        this.addRenderableWidget(this.settingsButton);

        this.dumpButton = new DarkSquareButton(
            this.leftPos + DUMP_X, this.topPos + TOP_BUTTON_Y, TOP_BUTTON_SIZE,
            Component.literal("⇩"), dumpTooltip(), this::triggerQuickDump);
        this.addRenderableWidget(this.dumpButton);
        gridModeWidgets.add(this.dumpButton);

        this.sortButton = new DarkSquareButton(
            this.leftPos + SORT_X, this.topPos + TOP_BUTTON_Y, TOP_BUTTON_SIZE,
            sortLabel(), sortTooltip(), this::onSortClicked);
        this.addRenderableWidget(this.sortButton);
        gridModeWidgets.add(this.sortButton);

        addFilterButton(0, FilterMode.NONE, null);
        addFilterButton(1, FilterMode.POTIONS, new ItemStack(Items.POTION));
        addFilterButton(2, FilterMode.ARMOR, new ItemStack(Items.IRON_CHESTPLATE));
        addFilterButton(3, FilterMode.TOOLS, new ItemStack(Items.IRON_PICKAXE));
        addFilterButton(4, FilterMode.FOOD, new ItemStack(Items.BREAD));
        addFilterButton(5, FilterMode.BLOCKS, new ItemStack(Items.COBBLESTONE));

        int pX = this.leftPos + SETTINGS_X0;
        int pY = this.topPos + SETTINGS_Y0;

        // Row 1 — quick dump key rebind (label at left, button at right)
        this.rebindButton = new DarkSquareButton(
            pX + 116, pY + 17, 92, 14,
            keyLabel(MagicStorageClient.QUICK_DUMP),
            Component.translatable("gui.magic_storage.rebind.tooltip"),
            () -> startRebind(MagicStorageClient.QUICK_DUMP, this.rebindButton));
        this.addRenderableWidget(this.rebindButton);
        settingsModeWidgets.add(this.rebindButton);

        // Row 2 — mark/protect key rebind
        this.protectRebindButton = new DarkSquareButton(
            pX + 116, pY + 33, 92, 14,
            keyLabel(MagicStorageClient.TOGGLE_PROTECT),
            Component.translatable("gui.magic_storage.rebind.protect.tooltip"),
            () -> startRebind(MagicStorageClient.TOGGLE_PROTECT, this.protectRebindButton));
        this.addRenderableWidget(this.protectRebindButton);
        settingsModeWidgets.add(this.protectRebindButton);

        // Row 3 — hotbar protection toggle
        this.hotbarToggleButton = new DarkSquareButton(
            pX + 170, pY + 49, 38, 14,
            hotbarToggleLabel(),
            Component.translatable("gui.magic_storage.hotbar.tooltip"),
            this::onHotbarToggle);
        this.addRenderableWidget(this.hotbarToggleButton);
        settingsModeWidgets.add(this.hotbarToggleButton);

        // Row 4 — slot marking (self-labelled)
        this.markToggleButton = new DarkSquareButton(
            pX + 10, pY + 65, 140, 14,
            markToggleLabel(),
            Component.translatable("gui.magic_storage.protect.tooltip"),
            this::onMarkToggle);
        this.addRenderableWidget(this.markToggleButton);
        settingsModeWidgets.add(this.markToggleButton);

        this.backButton = new DarkSquareButton(
            pX + SETTINGS_W - 44, pY + 4, 40, 12,
            Component.translatable("gui.magic_storage.back"),
            Component.translatable("gui.magic_storage.back"),
            this::closeSettings);
        this.addRenderableWidget(this.backButton);
        settingsModeWidgets.add(this.backButton);

        applyModeVisibility();
        rebuildLayout();
    }

    private void addFilterButton(int slot, FilterMode mode, ItemStack icon) {
        FilterSpriteButton btn = new FilterSpriteButton(
            this.leftPos + FILTER_X,
            this.topPos + FILTER_Y + slot * FILTER_SIZE,
            mode, icon,
            () -> this.filterMode,
            this::onFilterSelected);
        this.addRenderableWidget(btn);
        gridModeWidgets.add(btn);
    }

    private void applyModeVisibility() {
        boolean grid = !inSettings;
        for (AbstractWidget w : gridModeWidgets) w.visible = grid;
        for (AbstractWidget w : settingsModeWidgets) w.visible = !grid;
    }

    private void openSettings() {
        this.inSettings = true;
        clearRebind();
        ProtectedSlotsCache.setMarkMode(false);
        this.rebindButton.setGlyph(keyLabel(MagicStorageClient.QUICK_DUMP));
        this.protectRebindButton.setGlyph(keyLabel(MagicStorageClient.TOGGLE_PROTECT));
        this.markToggleButton.setGlyph(markToggleLabel());
        this.hotbarToggleButton.setGlyph(hotbarToggleLabel());
        applyModeVisibility();
    }

    private void closeSettings() {
        this.inSettings = false;
        clearRebind();
        ProtectedSlotsCache.setMarkMode(false);
        applyModeVisibility();
    }

    private void startRebind(KeyMapping target, DarkSquareButton button) {
        this.awaitingRebind = target;
        this.awaitingButton = button;
        ProtectedScreenOverlay.setSuppressHotkey(true);
        button.setGlyph(Component.translatable("gui.magic_storage.rebind.press"));
    }

    /** Restore the awaiting button's glyph to its current binding and stop awaiting. */
    private void clearRebind() {
        if (this.awaitingButton != null && this.awaitingRebind != null) {
            this.awaitingButton.setGlyph(keyLabel(this.awaitingRebind));
        }
        this.awaitingRebind = null;
        this.awaitingButton = null;
        ProtectedScreenOverlay.setSuppressHotkey(false);
    }

    private void onMarkToggle() {
        ProtectedSlotsCache.toggleMarkMode();
        this.markToggleButton.setGlyph(markToggleLabel());
    }

    private Component hotbarToggleLabel() {
        return Component.translatable(ProtectedSlotsCache.isHotbarProtected()
            ? "gui.magic_storage.hotbar.on" : "gui.magic_storage.hotbar.off");
    }

    private void onHotbarToggle() {
        boolean next = !ProtectedSlotsCache.isHotbarProtected();
        ProtectedSlotsCache.setHotbarProtected(next);
        MSNetwork.CHANNEL.sendToServer(new com.sq_yan.magic_storage.net.SetHotbarProtectionPacket(next));
        this.hotbarToggleButton.setGlyph(hotbarToggleLabel());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        rebuildLayout();
        if (inSettings && markToggleButton != null) {
            markToggleButton.setGlyph(markToggleLabel());
            hotbarToggleButton.setGlyph(hotbarToggleLabel());
        }
    }

    private void onSearchChanged(String text) {
        if (!text.equals(this.searchText)) {
            this.searchText = text;
            this.scrollOffset = 0;
            rebuildLayout();
            recomputeSuggestions();
        }
    }

    private void recomputeSuggestions() {
        String q = searchText.trim().toLowerCase();
        suggestions.clear();
        suggestionsHover = -1;
        if (q.isEmpty() || inSettings) return;
        int total = menu.getAggregatedSlotCount();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        java.util.List<String> prefix = new ArrayList<>();
        java.util.List<String> contains = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            String lower = name.toLowerCase();
            if (!seen.add(lower)) continue;
            if (lower.equals(q)) continue;
            if (lower.startsWith(q)) prefix.add(name);
            else if (lower.contains(q)) contains.add(name);
        }
        prefix.sort(String.CASE_INSENSITIVE_ORDER);
        contains.sort(String.CASE_INSENSITIVE_ORDER);
        for (String s : prefix) { if (suggestions.size() >= SUGGEST_MAX) break; suggestions.add(s); }
        for (String s : contains) { if (suggestions.size() >= SUGGEST_MAX) break; suggestions.add(s); }
    }

    private boolean suggestionsVisible() {
        return !inSettings && this.searchBox != null && this.searchBox.isFocused() && !suggestions.isEmpty();
    }

    private int suggestionIndexAt(double mx, double my) {
        if (!suggestionsVisible()) return -1;
        int x0 = this.leftPos + SUGGEST_X;
        int y0 = this.topPos + SUGGEST_Y;
        if (mx < x0 || mx >= x0 + SUGGEST_W) return -1;
        int rel = (int) ((my - y0) / SUGGEST_ROW_H);
        if (rel < 0 || rel >= suggestions.size()) return -1;
        return rel;
    }

    private void applySuggestion(String text) {
        if (this.searchBox == null) return;
        this.searchBox.setValue(text);
        this.searchBox.moveCursorToEnd();
        this.suggestions.clear();
        this.suggestionsHover = -1;
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

    private Component keyLabel(KeyMapping km) {
        return Component.literal("[").append(km.getTranslatedKeyMessage()).append("]");
    }

    private Component markToggleLabel() {
        int n = ProtectedSlotsCache.get().size();
        return Component.translatable(ProtectedSlotsCache.isMarkMode() ? "gui.magic_storage.protect.done" : "gui.magic_storage.protect.add", n);
    }

    private static final int GLFW_KEY_TAB = 258;
    private static final int GLFW_KEY_ENTER = 257;
    private static final int GLFW_KEY_DOWN = 264;
    private static final int GLFW_KEY_UP = 265;

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (awaitingRebind != null) {
            if (keyCode == GLFW_KEY_ESCAPE) {
                clearRebind();
                return true;
            }
            awaitingRebind.setKey(InputConstants.getKey(keyCode, scanCode));
            if (this.minecraft != null) this.minecraft.options.save();
            KeyMapping.resetMapping();
            if (awaitingRebind == MagicStorageClient.QUICK_DUMP) {
                this.dumpButton.setTooltipText(dumpTooltip());
            }
            clearRebind();
            return true;
        }
        if (ProtectedSlotsCache.isMarkMode() && keyCode == GLFW_KEY_ESCAPE) {
            ProtectedSlotsCache.setMarkMode(false);
            this.markToggleButton.setGlyph(markToggleLabel());
            return true;
        }
        if (inSettings && keyCode == GLFW_KEY_ESCAPE) {
            closeSettings();
            return true;
        }
        if (MagicStorageClient.QUICK_DUMP.matches(keyCode, scanCode)) {
            triggerQuickDump();
            return true;
        }
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == GLFW_KEY_ESCAPE) {
                if (!suggestions.isEmpty()) {
                    suggestions.clear();
                    suggestionsHover = -1;
                    return true;
                }
                this.searchBox.setValue("");
                this.searchBox.setFocused(false);
                this.setFocused(null);
                return true;
            }
            if (suggestionsVisible()) {
                if (keyCode == GLFW_KEY_TAB) {
                    int idx = suggestionsHover < 0 ? 0 : suggestionsHover;
                    applySuggestion(suggestions.get(idx));
                    return true;
                }
                if (keyCode == GLFW_KEY_DOWN) {
                    suggestionsHover = Math.min(suggestions.size() - 1, suggestionsHover + 1);
                    return true;
                }
                if (keyCode == GLFW_KEY_UP) {
                    suggestionsHover = Math.max(0, suggestionsHover - 1);
                    return true;
                }
                if (keyCode == GLFW_KEY_ENTER) {
                    if (suggestionsHover >= 0) {
                        applySuggestion(suggestions.get(suggestionsHover));
                        return true;
                    }
                    suggestions.clear();
                    return true;
                }
            }
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (this.searchBox.canConsumeInput()) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (super.mouseScrolled(mx, my, delta)) return true;
        if (!inSettings && isInGridArea(mx, my) && maxScrollOffset() > 0) {
            this.scrollOffset = clampScroll(this.scrollOffset - (int) Math.signum(delta));
            rebuildLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && suggestionsVisible()) {
            int idx = suggestionIndexAt(mx, my);
            if (idx >= 0) {
                applySuggestion(suggestions.get(idx));
                return true;
            }
        }
        if (ProtectedSlotsCache.isMarkMode() && button == 0) {
            Slot s = findHoveredPlayerInvSlot(mx, my);
            if (s != null) {
                ProtectedScreenOverlay.attemptToggle(s.getContainerSlot());
                this.markToggleButton.setGlyph(markToggleLabel());
                return true;
            }
        }
        if (!inSettings && button == 0 && isOnScrollbar(mx, my)) {
            this.draggingScrollbar = true;
            updateScrollFromMouse(my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) this.draggingScrollbar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (this.draggingScrollbar) {
            updateScrollFromMouse(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, ddx, ddy);
    }

    private Slot findHoveredPlayerInvSlot(double mx, double my) {
        int start = menu.getAggregatedSlotCount();
        int end = start + MagicStorageMenu.PLAYER_INV_SIZE;
        for (int i = start; i < end; i++) {
            Slot s = menu.slots.get(i);
            int sx = this.leftPos + s.x;
            int sy = this.topPos + s.y;
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) return s;
        }
        return null;
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
        if (inSettings) {
            for (int i = 0; i < total; i++) {
                Slot s = menu.slots.get(i);
                SlotAccess.setPos(s, OFFSCREEN, OFFSCREEN);
            }
            this.orderedIndices = new int[0];
            return;
        }
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
            SlotAccess.setPos(s, OFFSCREEN, OFFSCREEN);
        }
        int start = this.scrollOffset * GRID_COLS;
        for (int d = 0; d < GRID_SIZE; d++) {
            int vIdx = start + d;
            if (vIdx >= orderedIndices.length) break;
            int aggIdx = orderedIndices[vIdx];
            Slot s = menu.slots.get(aggIdx);
            SlotAccess.setPos(s, GRID_X + (d % GRID_COLS) * SLOT_SIZE, GRID_Y + (d / GRID_COLS) * SLOT_SIZE);
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
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.getNamespace();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int x1 = x0 + GUI_W;
        int y1 = y0 + GUI_H;

        gfx.fill(x0, y0, x1, y1, PANEL_BG);
        drawBorder(gfx, x0, y0, x1, y1, PANEL_BORDER);

        // Search bar bg + border (always drawn, but search edit-box hides in settings mode)
        int sx0 = x0 + SEARCH_X, sy0 = y0 + SEARCH_Y;
        int sx1 = sx0 + SEARCH_W, sy1 = sy0 + SEARCH_H;
        gfx.fill(sx0, sy0, sx1, sy1, INNER_PANEL);
        drawBorder(gfx, sx0, sy0, sx1, sy1, SLOT_BORDER);

        if (inSettings) {
            renderSettingsPanel(gfx, x0, y0);
        } else {
            renderGridArea(gfx, x0, y0);
        }

        // Player inventory slots — always rendered
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
    }

    private void renderGridArea(GuiGraphics gfx, int x0, int y0) {
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                int sx = x0 + GRID_X + c * SLOT_SIZE;
                int sy = y0 + GRID_Y + r * SLOT_SIZE;
                drawSlotBg(gfx, sx, sy);
            }
        }
        int start = this.scrollOffset * GRID_COLS;
        for (int d = 0; d < GRID_SIZE; d++) {
            int vIdx = start + d;
            if (vIdx < orderedIndices.length) continue;
            int sx = x0 + GRID_X + (d % GRID_COLS) * SLOT_SIZE;
            int sy = y0 + GRID_Y + (d / GRID_COLS) * SLOT_SIZE;
            gfx.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x401A1A28);
        }

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

    private void renderSettingsPanel(GuiGraphics gfx, int x0, int y0) {
        int px0 = x0 + SETTINGS_X0;
        int py0 = y0 + SETTINGS_Y0;
        int px1 = px0 + SETTINGS_W;
        int py1 = py0 + SETTINGS_H;
        gfx.fill(px0, py0, px1, py1, INNER_PANEL);
        drawBorder(gfx, px0, py0, px1, py1, SLOT_BORDER);

        // Title + separator line
        gfx.drawString(this.font, Component.translatable("gui.magic_storage.settings.title"),
            px0 + 8, py0 + 5, LABEL_COLOR, false);
        gfx.fill(px0 + 6, py0 + 16, px1 - 6, py0 + 17, SLOT_BORDER);

        // Row labels (controls sit to the right)
        gfx.drawString(this.font, Component.translatable("gui.magic_storage.rebind"),
            px0 + 10, py0 + 20, LABEL_MUTED, false);
        gfx.drawString(this.font, Component.translatable("gui.magic_storage.rebind.protect"),
            px0 + 10, py0 + 36, LABEL_MUTED, false);
        gfx.drawString(this.font, Component.translatable("gui.magic_storage.hotbar.label"),
            px0 + 10, py0 + 52, LABEL_MUTED, false);

        // Hint at the bottom
        gfx.drawString(this.font, Component.translatable("gui.magic_storage.settings.hint1"),
            px0 + 10, py0 + 82, LABEL_FAINT, false);
        gfx.drawString(this.font,
            Component.translatable("gui.magic_storage.settings.hint2", MagicStorageClient.TOGGLE_PROTECT.getTranslatedKeyMessage()),
            px0 + 10, py0 + 92, LABEL_FAINT, false);
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
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderProtectedOverlays(gfx);
        if (suggestionsVisible()) renderSuggestions(gfx, mouseX, mouseY);
        if (ProtectedSlotsCache.isMarkMode()) renderMarkCursor(gfx, mouseX, mouseY);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    private void renderSuggestions(GuiGraphics gfx, int mouseX, int mouseY) {
        int x0 = this.leftPos + SUGGEST_X;
        int y0 = this.topPos + SUGGEST_Y;
        int rows = suggestions.size();
        int h = rows * SUGGEST_ROW_H;
        gfx.fill(x0, y0, x0 + SUGGEST_W, y0 + h, 0xF0101018);
        drawBorder(gfx, x0, y0, x0 + SUGGEST_W, y0 + h, PANEL_BORDER);
        int hoverIdx = suggestionIndexAt(mouseX, mouseY);
        for (int i = 0; i < rows; i++) {
            int rowY = y0 + i * SUGGEST_ROW_H;
            boolean isActive = (i == hoverIdx) || (i == suggestionsHover);
            if (isActive) {
                gfx.fill(x0 + 1, rowY + 1, x0 + SUGGEST_W - 1, rowY + SUGGEST_ROW_H - 1, 0x408848C0);
            }
            String name = suggestions.get(i);
            int maxW = SUGGEST_W - 8;
            String shown = name;
            if (this.font.width(shown) > maxW) {
                while (shown.length() > 1 && this.font.width(shown + "…") > maxW) {
                    shown = shown.substring(0, shown.length() - 1);
                }
                shown = shown + "…";
            }
            gfx.drawString(this.font, shown, x0 + 4, rowY + 2, isActive ? 0xFFFFFFFF : LABEL_COLOR, false);
        }
    }

    private void renderProtectedOverlays(GuiGraphics gfx) {
        var protectedSet = ProtectedSlotsCache.get();
        if (protectedSet.isEmpty()) return;
        int start = menu.getAggregatedSlotCount();
        int end = start + MagicStorageMenu.PLAYER_INV_SIZE;
        for (int i = start; i < end; i++) {
            Slot s = menu.slots.get(i);
            int slotIdx = s.getContainerSlot();
            if (!protectedSet.contains(slotIdx)) continue;
            int sx = this.leftPos + s.x - 1;
            int sy = this.topPos + s.y - 1;
            int sw = SLOT_SIZE + 2;
            if (s.hasItem()) {
                drawBorder(gfx, sx, sy, sx + sw, sy + sw, PROTECTED_OUTER);
                drawBorder(gfx, sx + 1, sy + 1, sx + sw - 1, sy + sw - 1, PROTECTED_BORDER);
            } else {
                drawBorder(gfx, sx, sy, sx + sw, sy + sw, PROTECTED_BORDER_DIM);
            }
        }
    }

    private void renderMarkCursor(GuiGraphics gfx, int mouseX, int mouseY) {
        String star = "★";
        int w = this.font.width(star);
        int bx0 = mouseX + 8, by0 = mouseY - 4;
        int bx1 = bx0 + w + 4, by1 = by0 + this.font.lineHeight + 3;
        gfx.fill(bx0, by0, bx1, by1, 0xE61A1024);
        drawBorder(gfx, bx0, by0, bx1, by1, PROTECTED_BORDER);
        gfx.drawString(this.font, star, bx0 + 2, by0 + 2, 0xFFE0B0FF, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
        if (inSettings) return;
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
