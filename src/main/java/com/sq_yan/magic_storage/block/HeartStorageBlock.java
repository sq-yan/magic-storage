package com.sq_yan.magic_storage.block;

import com.mojang.serialization.MapCodec;
import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.net.CellNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeartStorageBlock extends BaseEntityBlock {
    public static final MapCodec<HeartStorageBlock> CODEC = simpleCodec(HeartStorageBlock::new);

    public HeartStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<HeartStorageBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new HeartStorageBlockEntity(pos, state);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                        @NotNull BlockPos pos, @NotNull Player player,
                                                        @NotNull BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        if (!(level.getBlockEntity(pos) instanceof HeartStorageBlockEntity heart)) return InteractionResult.CONSUME;
        if (CellNetwork.collect(heart).isEmpty()) {
            sp.sendSystemMessage(Component.translatable("message.magic_storage.no_cells"));
            return InteractionResult.CONSUME;
        }
        sp.openMenu(heart, pos);
        return InteractionResult.CONSUME;
    }
}
