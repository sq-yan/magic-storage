package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class CellNetwork {
    private static final int[][] NEIGHBOR_OFFSETS = buildNeighborOffsets();

    private CellNetwork() {}

    /**
     * BFS all reachable Storage Cells from the heart through 26-neighbor connectivity
     * (faces + edges + corners). Order is breadth-first: cells closer to the heart
     * appear first. Callers apply their own connection cap.
     */
    public static List<StorageCellBlockEntity> collectReachable(HeartStorageBlockEntity heart) {
        Level level = heart.getLevel();
        if (level == null) return List.of();

        BlockPos start = heart.getBlockPos();
        List<StorageCellBlockEntity> result = new ArrayList<>();
        HashSet<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        for (int[] o : NEIGHBOR_OFFSETS) queue.add(start.offset(o[0], o[1], o[2]));

        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof StorageCellBlockEntity cell) {
                result.add(cell);
                for (int[] o : NEIGHBOR_OFFSETS) {
                    BlockPos n = p.offset(o[0], o[1], o[2]);
                    if (!visited.contains(n)) queue.add(n);
                }
            }
        }
        return result;
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
