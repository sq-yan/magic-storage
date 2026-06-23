package com.sq_yan.magic_storage.menu;

import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import com.sq_yan.magic_storage.net.AggregatedItemHandler;
import com.sq_yan.magic_storage.net.CellNetwork;
import com.sq_yan.magic_storage.registry.MSBlocks;
import com.sq_yan.magic_storage.registry.MSMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MagicStorageMenu extends AbstractContainerMenu {

    public static final int GRID_COLS = 12;
    public static final int GRID_ROWS = 6;
    public static final int GRID_SIZE = GRID_COLS * GRID_ROWS;
    public static final int PLAYER_INV_SIZE = 36;
    public static final int PLAYER_INV_X = 47;
    public static final int PLAYER_INV_Y = 150;
    public static final int HOTBAR_Y = 210;

    public static final int BUTTON_QUICK_DUMP = 1;

    private static final int OFFSCREEN = -2000;
    private static final int REBUILD_INTERVAL_TICKS = 10;

    private final ContainerLevelAccess access;
    private final AggregatedItemHandler aggregated;
    private final int aggregatedSlotCount;
    private final @Nullable HeartStorageBlockEntity heart;
    private final List<BlockPos> initialCellPositions;

    private int rebuildCounter = 0;

    public MagicStorageMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveHeart(inv, buf.readBlockPos()), readPositions(buf));
    }

    public MagicStorageMenu(int id, Inventory inv, @Nullable HeartStorageBlockEntity heart) {
        this(id, inv, heart, heart != null ? heart.getConnectedCellPositions() : List.of());
    }

    private MagicStorageMenu(int id, Inventory inv, @Nullable HeartStorageBlockEntity heart, List<BlockPos> cellPositions) {
        super(MSMenus.MAGIC_STORAGE.get(), id);
        this.heart = heart;
        if (heart != null && heart.getLevel() != null && !heart.getLevel().isClientSide()) {
            this.aggregated = heart.buildAggregated();
        } else if (heart != null && heart.getLevel() != null) {
            this.aggregated = AggregatedItemHandler.fromPositions(heart.getLevel(), cellPositions);
        } else {
            this.aggregated = new AggregatedItemHandler(List.of());
        }
        this.access = (heart != null && heart.getLevel() != null)
            ? ContainerLevelAccess.create(heart.getLevel(), heart.getBlockPos())
            : ContainerLevelAccess.NULL;
        this.aggregatedSlotCount = aggregated.getSlots();
        this.initialCellPositions = aggregated.cellPositions();

        for (int i = 0; i < aggregatedSlotCount; i++) {
            addSlot(new SlotItemHandler(aggregated, i, OFFSCREEN, OFFSCREEN));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    public int getAggregatedSlotCount() {
        return aggregatedSlotCount;
    }

    public int countUsedSlots() {
        int used = 0;
        for (int i = 0; i < aggregatedSlotCount; i++) {
            if (!this.slots.get(i).getItem().isEmpty()) used++;
        }
        return used;
    }

    // openMenu = единственная точка истины для slot count.
    // Здесь только освежаем BE-ссылки в aggregated handler на случай unload/reload chunk —
    // если число подключённых cells поменялось, игрок должен переоткрыть GUI.
    private void refreshCellReferencesIfStable() {
        if (heart == null || heart.getLevel() == null || heart.getLevel().isClientSide()) return;
        if (heart.isRemoved()) return;
        List<StorageCellBlockEntity> fresh = heart.getConnectedCells();
        if (fresh.size() * StorageCellBlockEntity.SIZE != aggregatedSlotCount) {
            return;
        }
        aggregated.rebuild(fresh);
    }

    @Override
    public void broadcastChanges() {
        if (++rebuildCounter >= REBUILD_INTERVAL_TICKS) {
            rebuildCounter = 0;
            refreshCellReferencesIfStable();
        }
        super.broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int idx) {
        Slot slot = this.slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        int playerStart = aggregatedSlotCount;
        int playerEnd = aggregatedSlotCount + PLAYER_INV_SIZE;

        if (idx < playerStart) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!aggregated.distribute(stack)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_QUICK_DUMP) {
            if (!player.level().isClientSide()) {
                var protectedSlots = com.sq_yan.magic_storage.protect.ProtectedSlots.cleanEmpty(player);
                aggregated.dumpPlayerInventoryMain(player, protectedSlots);
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (!stillValid(access, player, MSBlocks.HEART_STORAGE.get())) return false;
        if (heart == null || heart.getLevel() == null) return true;
        var level = heart.getLevel();
        if (level.isClientSide()) return true;
        for (BlockPos pos : initialCellPositions) {
            if (!(level.getBlockEntity(pos) instanceof StorageCellBlockEntity cell) || cell.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable HeartStorageBlockEntity resolveHeart(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof HeartStorageBlockEntity h) return h;
        return null;
    }

    private static List<BlockPos> readPositions(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<BlockPos> result = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) result.add(buf.readBlockPos());
        return result;
    }
}
