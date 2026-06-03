package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class CellNetwork {
    public static final int MAX_CELLS = 128;

    private CellNetwork() {}

    public static List<StorageCellBlockEntity> collect(HeartStorageBlockEntity heart) {
        Level level = heart.getLevel();
        if (level == null) return List.of();

        BlockPos start = heart.getBlockPos();
        List<StorageCellBlockEntity> result = new ArrayList<>();
        HashSet<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        for (Direction d : Direction.values()) queue.add(start.relative(d));

        while (!queue.isEmpty() && result.size() < MAX_CELLS) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof StorageCellBlockEntity cell) {
                result.add(cell);
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    if (!visited.contains(n)) queue.add(n);
                }
            }
        }
        return result;
    }
}
