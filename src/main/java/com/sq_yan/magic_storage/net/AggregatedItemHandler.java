package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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
