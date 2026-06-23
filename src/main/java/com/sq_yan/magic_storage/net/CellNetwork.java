package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.block.CrystalExpanderBlock;
import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class CellNetwork {
    private static final int[][] NEIGHBOR_OFFSETS = buildNeighborOffsets();

    private CellNetwork() {}

    public record NetworkResult(List<StorageCellBlockEntity> cells, List<BlockPos> activeExpanders) {
        public static final NetworkResult EMPTY = new NetworkResult(List.of(), List.of());
    }

    /**
     * BFS the heart's network through 26-neighbor connectivity. Walks through both
     * Storage Cells and Crystal Expanders with a crystal inserted — empty expanders
     * are not part of the network. Cells and active expanders are returned separately;
     * callers cap cells with their own connection limit.
     */
    public static NetworkResult collect(HeartStorageBlockEntity heart) {
        Level level = heart.getLevel();
        if (level == null) return NetworkResult.EMPTY;

        BlockPos start = heart.getBlockPos();
        List<StorageCellBlockEntity> cells = new ArrayList<>();
        List<BlockPos> expanders = new ArrayList<>();
        HashSet<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        for (int[] o : NEIGHBOR_OFFSETS) queue.add(start.offset(o[0], o[1], o[2]));

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) continue;

            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof StorageCellBlockEntity cell) {
                cells.add(cell);
                enqueueNeighbors(queue, visited, p);
                continue;
            }

            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CrystalExpanderBlock
                && s.hasProperty(CrystalExpanderBlock.HAS_CRYSTAL)
                && s.getValue(CrystalExpanderBlock.HAS_CRYSTAL)
                && p.distManhattan(start) == 1) {
                expanders.add(p.immutable());
                enqueueNeighbors(queue, visited, p);
            }
        }
        return new NetworkResult(cells, expanders);
    }

    private static void enqueueNeighbors(ArrayDeque<BlockPos> queue, HashSet<BlockPos> visited, BlockPos from) {
        for (int[] o : NEIGHBOR_OFFSETS) {
            BlockPos n = from.offset(o[0], o[1], o[2]);
            if (!visited.contains(n)) queue.add(n);
        }
    }

    private static int[][] buildNeighborOffsets() {
        int[][] result = new int[26][3];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    result[i][0] = dx;
                    result[i][1] = dy;
                    result[i][2] = dz;
                    i++;
                }
            }
        }
        return result;
    }
}
