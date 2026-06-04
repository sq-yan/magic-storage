package com.sq_yan.magic_storage.menu;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

import java.util.List;

public class MagicStorageMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int GRID_COLS = 9;
    public static final int GRID_ROWS = 6;
    public static final int GRID_SIZE = GRID_COLS * GRID_ROWS;
    public static final int PLAYER_INV_SIZE = 36;

    private static final int OFFSCREEN = -2000;
    private static final int REBUILD_INTERVAL_TICKS = 10;

    private final ContainerLevelAccess access;
    private final AggregatedItemHandler aggregated;
    private final int aggregatedSlotCount;
    private final @Nullable HeartStorageBlockEntity heart;

    private int rebuildCounter = 0;

    public MagicStorageMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolveHeart(inv, buf.readBlockPos()));
    }

    public MagicStorageMenu(int id, Inventory inv, @Nullable HeartStorageBlockEntity heart) {
        super(MSMenus.MAGIC_STORAGE.get(), id);
        this.heart = heart;
        this.aggregated = heart != null ? heart.buildAggregated() : new AggregatedItemHandler(List.of());
        this.access = (heart != null && heart.getLevel() != null)
            ? ContainerLevelAccess.create(heart.getLevel(), heart.getBlockPos())
            : ContainerLevelAccess.NULL;
        this.aggregatedSlotCount = aggregated.getSlots();

        for (int i = 0; i < aggregatedSlotCount; i++) {
            addSlot(new SlotItemHandler(aggregated, i, OFFSCREEN, OFFSCREEN));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 198));
        }

        boolean isServer = heart != null && heart.getLevel() != null && !heart.getLevel().isClientSide();
        if (isServer) {
            LOGGER.info("[magic_storage] menu opened (server): cells={}, slots={}",
                aggregated.cellCount(), aggregatedSlotCount);
        } else {
            LOGGER.info("[magic_storage] menu opened (client): heart={}, slots={}",
                heart == null ? "null" : "found", aggregatedSlotCount);
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

    private void rebuildNetworkIfNeeded() {
        if (heart == null || heart.getLevel() == null || heart.getLevel().isClientSide()) return;
        if (heart.isRemoved()) return;
        List<StorageCellBlockEntity> fresh = CellNetwork.collect(heart);
        if (fresh.size() * StorageCellBlockEntity.SIZE != aggregatedSlotCount) {
            LOGGER.warn("[magic_storage] cell count changed since menu opened: was={}, now={}. Slots will not resize until reopen.",
                aggregated.cellCount(), fresh.size());
            return;
        }
        aggregated.rebuild(fresh);
    }

    @Override
    public void broadcastChanges() {
        if (++rebuildCounter >= REBUILD_INTERVAL_TICKS) {
            rebuildCounter = 0;
            rebuildNetworkIfNeeded();
        }
        super.broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int idx) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(idx);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int playerStart = aggregatedSlotCount;
            int playerEnd = aggregatedSlotCount + PLAYER_INV_SIZE;
            if (idx < playerStart) {
                if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, playerStart, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, MSBlocks.HEART_STORAGE.get());
    }

    private static @Nullable HeartStorageBlockEntity resolveHeart(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof HeartStorageBlockEntity h) return h;
        return null;
    }
}
