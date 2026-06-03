package com.sq_yan.magic_storage.block;

import com.mojang.serialization.MapCodec;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StorageCellBlock extends BaseEntityBlock {
    public static final MapCodec<StorageCellBlock> CODEC = simpleCodec(StorageCellBlock::new);

    public StorageCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<StorageCellBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new StorageCellBlockEntity(pos, state);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos,
                                                 @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageCellBlockEntity cell) {
            cell.dropContents(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
