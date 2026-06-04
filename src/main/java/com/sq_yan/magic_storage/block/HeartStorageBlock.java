package com.sq_yan.magic_storage.block;

import com.mojang.serialization.MapCodec;
import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.net.CellNetwork;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeartStorageBlock extends BaseEntityBlock {
    public static final MapCodec<HeartStorageBlock> CODEC = simpleCodec(HeartStorageBlock::new);

    public static final IntegerProperty FILL_LEVEL = IntegerProperty.create("fill_level", 0, 3);

    public HeartStorageBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FILL_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FILL_LEVEL);
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
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level,
                                                                            @NotNull BlockState state,
                                                                            @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type != MSBlockEntities.HEART_STORAGE.get()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof HeartStorageBlockEntity heart) heart.serverTick();
        };
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
