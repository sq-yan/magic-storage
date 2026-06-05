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
        Slot slot = this.slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        int playerStart = aggregatedSlotCount;
        int playerEnd = aggregatedSlotCount + PLAYER_INV_SIZE;

        if (idx < playerStart) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!distributeIntoStorage(stack)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    /**
     * Player -> storage shift-click: phase 1 merge into existing matching stacks,
     * phase 2 place into first empty slots. Mutates {@code stack} count via shrink().
     * <p>
     * Critical: Forge {@link net.minecraftforge.items.ItemStackHandler#insertItem}
     * stores the SAME reference (not a copy) into the slot when the slot is empty
     * and the entire stack fits. Passing our caller's stack directly would alias
     * the slot's contents — any later mutation (e.g. setCount(0)) would zero out
     * the items we just inserted. We pass a copy and reconcile via {@link ItemStack#shrink}.
     */
    private boolean distributeIntoStorage(ItemStack stack) {
        int before = stack.getCount();
        if (stack.isStackable()) {
            for (int i = 0; i < aggregatedSlotCount && !stack.isEmpty(); i++) {
                ItemStack existing = aggregated.getStackInSlot(i);
                if (existing.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(stack, existing)) continue;
                insertCopyAndShrink(i, stack);
            }
        }
        for (int i = 0; i < aggregatedSlotCount && !stack.isEmpty(); i++) {
            if (!aggregated.getStackInSlot(i).isEmpty()) continue;
            insertCopyAndShrink(i, stack);
        }
        return stack.getCount() != before;
    }

    private void insertCopyAndShrink(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        ItemStack remaining = aggregated.insertItem(slot, copy, false);
        int inserted = copy.getCount() - remaining.getCount();
        if (inserted > 0) stack.shrink(inserted);
    }

    /**
     * Drop entire player main inventory (slots 9..35, no hotbar) into storage
     * using the same merge-then-place distribution as shift-click.
     */
    public void quickDump(Player player) {
        var inv = player.getInventory();
        boolean any = false;
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            int before = s.getCount();
            distributeIntoStorage(s);
            if (s.getCount() != before) any = true;
            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
        }
        if (any) inv.setChanged();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BUTTON_QUICK_DUMP) {
            if (!player.level().isClientSide()) quickDump(player);
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
                LOGGER.info("[magic_storage] menu closing — cell at {} no longer present", pos);
                return false;
            }
        }
        return true;
    }

    private static @Nullable HeartStorageBlockEntity resolveHeart(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof HeartStorageBlockEntity h) return h;
        return null;
    }
}
