package com.sq_yan.magic_storage.blockentity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.sq_yan.magic_storage.block.HeartStorageBlock;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.net.AggregatedItemHandler;
import com.sq_yan.magic_storage.net.CellNetwork;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

public class HeartStorageBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TICK_INTERVAL = 20;
    private static final String NBT_CONNECTED = "Connected";

    private int tickCounter = 0;
    private final LinkedHashSet<BlockPos> connected = new LinkedHashSet<>();
    private boolean overflowAnnounced = false;

    public HeartStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MSBlockEntities.HEART_STORAGE.get(), pos, state);
    }

    public int getMaxCells() {
        return getBlockState().getBlock() instanceof HeartStorageBlock h ? h.getMaxCells() : 0;
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
        return buildAggregated().dumpPlayerInventoryMain(player);
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
        List<StorageCellBlockEntity> reachable = CellNetwork.collectReachable(this);
        Set<BlockPos> reachableSet = new HashSet<>(reachable.size());
        for (StorageCellBlockEntity cell : reachable) reachableSet.add(cell.getBlockPos());

        connected.removeIf(pos -> !reachableSet.contains(pos));

        int max = getMaxCells();
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
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        connected.clear();
        long[] packed = input.read(NBT_CONNECTED, Codec.LONG_STREAM)
            .map(LongStream::toArray)
            .orElse(new long[0]);
        for (long l : packed) connected.add(BlockPos.of(l));
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        long[] packed = new long[connected.size()];
        int i = 0;
        for (BlockPos p : connected) packed[i++] = p.asLong();
        output.store(NBT_CONNECTED, Codec.LONG_STREAM, LongStream.of(packed));
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
