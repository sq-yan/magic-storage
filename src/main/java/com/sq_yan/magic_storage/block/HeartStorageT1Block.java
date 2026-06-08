package com.sq_yan.magic_storage.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

public class HeartStorageT1Block extends HeartStorageBlock {
    public static final MapCodec<HeartStorageT1Block> CODEC = simpleCodec(HeartStorageT1Block::new);
    public static final int MAX_CELLS = 4;

    public HeartStorageT1Block(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCells() {
        return MAX_CELLS;
    }

    @Override
    protected @NotNull MapCodec<HeartStorageT1Block> codec() {
        return CODEC;
    }
}
