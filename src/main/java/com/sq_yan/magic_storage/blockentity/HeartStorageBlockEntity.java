package com.sq_yan.magic_storage.blockentity;

import com.sq_yan.magic_storage.block.HeartStorageBlock;
import com.sq_yan.magic_storage.block.HeartStorageT1Block;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.net.AggregatedItemHandler;
import com.sq_yan.magic_storage.net.CellNetwork;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HeartStorageBlockEntity extends BlockEntity implements MenuProvider {
    private static final int TICK_INTERVAL = 20;
    private static final String NBT_CONNECTED = "Connected";
    private static final String NBT_EXPANDERS = "Expanders";
    public static final int EXPANDER_BONUS_CELLS = 4;

    private int tickCounter = 0;
    private final LinkedHashSet<BlockPos> connected = new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> connectedExpanders = new LinkedHashSet<>();
    private final Set<BlockPos> lastReachable = new HashSet<>();
    private boolean overflowAnnounced = false;

    public HeartStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MSBlockEntities.HEART_STORAGE.get(), pos, state);
    }

    public int getMaxCells() {
        BlockState state = getBlockState();
        if (state.hasProperty(HeartStorageT1Block.HAS_HEART) && !state.getValue(HeartStorageT1Block.HAS_HEART)) {
            return 0;
        }
        int base = state.getBlock() instanceof HeartStorageBlock h ? h.getMaxCells() : 0;
        return base + EXPANDER_BONUS_CELLS * connectedExpanders.size();
    }

    public int getActiveExpanderCount() {
        return connectedExpanders.size();
    }

    /** Snapshot of connected positions (server-side state). */
    public List<BlockPos> getConnectedCellPositions() {
        return new ArrayList<>(connected);
    }

    /** Force a connection refresh outside the tick schedule (e.g. before opening menu). */
    public void refreshConnections() {
        if (level == null || level.isClientSide()) return;
        updateConnections();
    }

    /** Mark every cell this heart last managed as disconnected (grey) — called when the heart is removed. */
    public void releaseAllCells() {
        if (level == null) return;
        for (BlockPos p : lastReachable) {
            if (level.getBlockEntity(p) instanceof StorageCellBlockEntity c) {
                c.setConnectedState(false);
            }
        }
    }

    /** Live connected cells, in connection order, filtered to non-removed BEs. */
    public List<StorageCellBlockEntity> getConnectedCells() {
        if (level == null) return List.of();
        List<StorageCellBlockEntity> result = new ArrayList<>(connected.size());
        for (BlockPos pos : connected) {
            if (level.getBlockEntity(pos) instanceof StorageCellBlockEntity cell && !cell.isRemoved()) {
                result.add(cell);
            }
        }
        return result;
    }

    public AggregatedItemHandler buildAggregated() {
        return new AggregatedItemHandler(getConnectedCells());
    }

    public boolean dumpPlayerInventory(Player player) {
        var protectedSlots = com.sq_yan.magic_storage.protect.ProtectedSlots.cleanEmpty(player);
        return buildAggregated().dumpPlayerInventoryMain(player, protectedSlots);
    }

    /**
     * Compact the whole network: merge partial stacks of the same item and re-pack everything
     * into the fewest cells (filling cell-by-cell in connection order). Trailing cells are emptied,
     * so they show as free and can be safely removed / relocated. Returns true if anything moved.
     */
    public boolean defragment() {
        if (level == null || level.isClientSide()) return false;
        refreshConnections();
        List<StorageCellBlockEntity> cells = getConnectedCells();
        if (cells.isEmpty()) return false;

        // 1. Pull every non-empty stack out, remembering the original layout signature.
        List<ItemStack> merged = new ArrayList<>();
        List<ItemStack> originalOrder = new ArrayList<>();
        for (StorageCellBlockEntity cell : cells) {
            ItemStackHandler h = cell.getItems();
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty()) originalOrder.add(s.copy());
            }
        }
        if (originalOrder.isEmpty()) return false;

        // 2. Merge same item+tags up to max stack size.
        for (ItemStack s : originalOrder) {
            ItemStack stack = s.copy();
            for (ItemStack m : merged) {
                if (stack.isEmpty()) break;
                if (m.getCount() >= m.getMaxStackSize()) continue;
                if (!ItemStack.isSameItemSameTags(m, stack)) continue;
                int move = Math.min(stack.getCount(), m.getMaxStackSize() - m.getCount());
                m.grow(move);
                stack.shrink(move);
            }
            if (!stack.isEmpty()) merged.add(stack);
        }

        // 3. Skip work if already fully compacted (same stack count and no partials to fold).
        if (merged.size() == originalOrder.size() && merged.size() == countLeadingFilled(cells)) {
            return false;
        }

        // 4. Clear all cells.
        for (StorageCellBlockEntity cell : cells) {
            ItemStackHandler h = cell.getItems();
            for (int i = 0; i < h.getSlots(); i++) h.setStackInSlot(i, ItemStack.EMPTY);
        }

        // 5. Re-place sequentially, filling cell-by-cell from the first.
        int idx = 0;
        outer:
        for (StorageCellBlockEntity cell : cells) {
            ItemStackHandler h = cell.getItems();
            for (int i = 0; i < h.getSlots(); i++) {
                if (idx >= merged.size()) break outer;
                h.setStackInSlot(i, merged.get(idx++));
            }
        }
        return true;
    }

    /** How many stacks currently sit contiguously from the very first slot with no gaps. */
    private int countLeadingFilled(List<StorageCellBlockEntity> cells) {
        int count = 0;
        for (StorageCellBlockEntity cell : cells) {
            ItemStackHandler h = cell.getItems();
            for (int i = 0; i < h.getSlots(); i++) {
                if (h.getStackInSlot(i).isEmpty()) return count;
                count++;
            }
        }
        return count;
    }

    public static @Nullable HeartStorageBlockEntity findNearby(Level level, BlockPos from, double radius) {
        double minSq = radius * radius;
        HeartStorageBlockEntity closest = null;
        int chunkRadius = ((int) Math.ceil(radius) + 15) >> 4;
        int cx = from.getX() >> 4;
        int cz = from.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof HeartStorageBlockEntity heart)) continue;
                    if (heart.isRemoved()) continue;
                    double sq = heart.getBlockPos().distSqr(from);
                    if (sq <= minSq) {
                        minSq = sq;
                        closest = heart;
                    }
                }
            }
        }
        return closest;
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        updateConnections();
        updateFillLevel();
    }

    /**
     * Reconcile the persistent connection set with the live reachable cells.
     * Connected cells that are still reachable keep their slot — we never evict
     * a cell that currently holds items just because a closer one appeared.
     * Cells lose their slot only when they become unreachable (broken / cut off).
     */
    private void updateConnections() {
        CellNetwork.NetworkResult result = CellNetwork.collect(this);

        // Update expanders first — getMaxCells() depends on this set.
        connectedExpanders.clear();
        connectedExpanders.addAll(result.activeExpanders());

        List<StorageCellBlockEntity> reachable = result.cells();
        Set<BlockPos> reachableSet = new HashSet<>(reachable.size());
        for (StorageCellBlockEntity cell : reachable) reachableSet.add(cell.getBlockPos());

        connected.removeIf(pos -> !reachableSet.contains(pos));

        int max = getMaxCells();

        // If max shrank (crystal/expander removed) — drop newest entries first
        // so the original early-connected cells keep their slots.
        if (connected.size() > max) {
            List<BlockPos> ordered = new ArrayList<>(connected);
            connected.clear();
            for (int i = 0; i < max; i++) connected.add(ordered.get(i));
        }

        for (StorageCellBlockEntity cell : reachable) {
            if (connected.size() >= max) break;
            connected.add(cell.getBlockPos().immutable());
        }

        boolean overflow = reachable.size() > connected.size();
        if (overflow && !overflowAnnounced) {
            announceOverflow();
            overflowAnnounced = true;
        } else if (!overflow) {
            overflowAnnounced = false;
        }

        // Push the visual connected-state to each cell: coloured if it made the cap, grey if it's
        // reachable-but-overflow. Cells that dropped out of reach since last tick (chain cut / removed)
        // are reset to grey too, so a no-longer-served cell never stays lit.
        for (StorageCellBlockEntity cell : reachable) {
            cell.setConnectedState(connected.contains(cell.getBlockPos()));
        }
        for (BlockPos p : lastReachable) {
            if (!reachableSet.contains(p) && level.getBlockEntity(p) instanceof StorageCellBlockEntity c) {
                c.setConnectedState(false);
            }
        }
        lastReachable.clear();
        lastReachable.addAll(reachableSet);

        setChanged();
    }

    private void announceOverflow() {
        if (level == null) return;
        BlockPos pos = getBlockPos();
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            16.0, false);
        if (nearest == null) return;
        nearest.displayClientMessage(
            Component.translatable("message.magic_storage.heart_full", getMaxCells()),
            true);
    }

    private void updateFillLevel() {
        int total = connected.size() * StorageCellBlockEntity.SIZE;
        int used = 0;
        for (StorageCellBlockEntity cell : getConnectedCells()) {
            ItemStackHandler items = cell.getItems();
            for (int i = 0; i < items.getSlots(); i++) {
                if (!items.getStackInSlot(i).isEmpty()) used++;
            }
        }
        int newLevel = computeFillLevel(used, total);
        BlockState state = getBlockState();
        if (state.hasProperty(HeartStorageBlock.FILL_LEVEL)
            && state.getValue(HeartStorageBlock.FILL_LEVEL) != newLevel) {
            level.setBlock(getBlockPos(), state.setValue(HeartStorageBlock.FILL_LEVEL, newLevel), 3);
        }
    }

    private static int computeFillLevel(int used, int total) {
        if (total == 0) return 0;
        double ratio = (double) used / total;
        if (ratio <= 0.5) return 1;
        if (ratio <= 2.0 / 3.0) return 2;
        return 3;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        connected.clear();
        for (long l : tag.getLongArray(NBT_CONNECTED)) connected.add(BlockPos.of(l));

        connectedExpanders.clear();
        for (long l : tag.getLongArray(NBT_EXPANDERS)) connectedExpanders.add(BlockPos.of(l));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        long[] packed = new long[connected.size()];
        int i = 0;
        for (BlockPos p : connected) packed[i++] = p.asLong();
        tag.putLongArray(NBT_CONNECTED, packed);

        long[] expandersPacked = new long[connectedExpanders.size()];
        int j = 0;
        for (BlockPos p : connectedExpanders) expandersPacked[j++] = p.asLong();
        tag.putLongArray(NBT_EXPANDERS, expandersPacked);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.magic_storage.heart_storage");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new MagicStorageMenu(id, inv, this);
    }
}
