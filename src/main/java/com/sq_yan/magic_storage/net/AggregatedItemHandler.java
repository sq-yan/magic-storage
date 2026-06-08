package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AggregatedItemHandler implements IItemHandlerModifiable {
    private final List<StorageCellBlockEntity> cells;

    public AggregatedItemHandler(List<StorageCellBlockEntity> cells) {
        this.cells = new ArrayList<>(cells);
    }

    /** Build from a fixed list of positions, resolving each to its current BE (skipping missing/removed). */
    public static AggregatedItemHandler fromPositions(Level level, List<BlockPos> positions) {
        List<StorageCellBlockEntity> cells = new ArrayList<>(positions.size());
        for (BlockPos p : positions) {
            if (level.getBlockEntity(p) instanceof StorageCellBlockEntity cell && !cell.isRemoved()) {
                cells.add(cell);
            }
        }
        return new AggregatedItemHandler(cells);
    }

    public void rebuild(List<StorageCellBlockEntity> fresh) {
        this.cells.clear();
        this.cells.addAll(fresh);
    }

    public int cellCount() {
        return cells.size();
    }

    public List<BlockPos> cellPositions() {
        List<BlockPos> out = new ArrayList<>(cells.size());
        for (StorageCellBlockEntity cell : cells) out.add(cell.getBlockPos().immutable());
        return out;
    }

    @Override
    public int getSlots() {
        return cells.size() * StorageCellBlockEntity.SIZE;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        ItemStackHandler h = handlerFor(slot);
        return h == null ? ItemStack.EMPTY : h.getStackInSlot(localSlot(slot));
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        ItemStackHandler h = handlerFor(slot);
        if (h != null) h.setStackInSlot(localSlot(slot), stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStackHandler h = handlerFor(slot);
        return h == null ? stack : h.insertItem(localSlot(slot), stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStackHandler h = handlerFor(slot);
        return h == null ? ItemStack.EMPTY : h.extractItem(localSlot(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        ItemStackHandler h = handlerFor(slot);
        return h == null ? 0 : h.getSlotLimit(localSlot(slot));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        ItemStackHandler h = handlerFor(slot);
        return h != null && h.isItemValid(localSlot(slot), stack);
    }

    /**
     * Merge-then-place distribution: phase 1 fills existing matching stacks,
     * phase 2 places into first empty slots. Mutates {@code stack} count via shrink().
     * <p>
     * Critical: Forge {@link ItemStackHandler#insertItem} stores the SAME reference
     * (not a copy) into an empty slot when the entire stack fits. Pass a copy and
     * reconcile via shrink() — see also pitfall #27 in vault.
     */
    public boolean distribute(ItemStack stack) {
        int slots = getSlots();
        int before = stack.getCount();
        if (stack.isStackable()) {
            for (int i = 0; i < slots && !stack.isEmpty(); i++) {
                ItemStack existing = getStackInSlot(i);
                if (existing.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(stack, existing)) continue;
                insertCopyAndShrink(i, stack);
            }
        }
        for (int i = 0; i < slots && !stack.isEmpty(); i++) {
            if (!getStackInSlot(i).isEmpty()) continue;
            insertCopyAndShrink(i, stack);
        }
        return stack.getCount() != before;
    }

    private void insertCopyAndShrink(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        ItemStack remaining = insertItem(slot, copy, false);
        int inserted = copy.getCount() - remaining.getCount();
        if (inserted > 0) stack.shrink(inserted);
    }

    /**
     * Pulls main player inventory (slots 9..35, no hotbar) into storage.
     * Returns true if at least one item moved.
     */
    public boolean dumpPlayerInventoryMain(Player player) {
        var inv = player.getInventory();
        boolean any = false;
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            int before = s.getCount();
            distribute(s);
            if (s.getCount() != before) any = true;
            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
        }
        if (any) inv.setChanged();
        return any;
    }

    private @Nullable ItemStackHandler handlerFor(int slot) {
        int cellIdx = slot / StorageCellBlockEntity.SIZE;
        if (cellIdx < 0 || cellIdx >= cells.size()) return null;
        StorageCellBlockEntity cell = cells.get(cellIdx);
        if (cell.isRemoved()) return null;
        return cell.getItems();
    }

    private static int localSlot(int slot) {
        return slot % StorageCellBlockEntity.SIZE;
    }
}
