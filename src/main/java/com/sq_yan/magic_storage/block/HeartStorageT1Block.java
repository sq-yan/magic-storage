package com.sq_yan.magic_storage.block;

import com.mojang.serialization.MapCodec;
import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.menu.ResonanceConsoleMenu;
import com.sq_yan.magic_storage.registry.MSItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeartStorageT1Block extends HeartStorageBlock {
    public static final MapCodec<HeartStorageT1Block> CODEC = simpleCodec(HeartStorageT1Block::new);
    public static final int MAX_CELLS = 4;

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_HEART = BooleanProperty.create("has_heart");

    public HeartStorageT1Block(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
            .setValue(FILL_LEVEL, 0)
            .setValue(FACING, Direction.NORTH)
            .setValue(HAS_HEART, false));
    }

    @Override
    public int getMaxCells() {
        return MAX_CELLS;
    }

    @Override
    protected @NotNull MapCodec<HeartStorageT1Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HAS_HEART);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(HAS_HEART, false);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                       @NotNull BlockPos pos, @NotNull Player player,
                                                       @NotNull BlockHitResult hit) {
        ItemStack main = player.getMainHandItem();
        boolean hasHeart = state.getValue(HAS_HEART);

        // Reigall's Tuning Fork — open the Resonance Console (network map + per-cell view + defrag).
        if (main.is(MSItems.REIGALLS_TUNING_FORK.get())) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof HeartStorageBlockEntity heart) {
                if (!hasHeart) {
                    sp.displayClientMessage(Component.translatable("message.magic_storage.no_heart"), true);
                } else {
                    heart.refreshConnections();
                    var positions = heart.getConnectedCellPositions();
                    sp.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new ResonanceConsoleMenu(id, inv, heart),
                        Component.translatable("gui.magic_storage.console.title")), buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeVarInt(positions.size());
                        for (BlockPos bp : positions) buf.writeBlockPos(bp);
                    });
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!hasHeart && main.is(MSItems.MAGIC_HEART.get())) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_HEART, true), 3);
                if (level.getBlockEntity(pos) instanceof HeartStorageBlockEntity heart) {
                    heart.refreshConnections();
                }
                if (!player.getAbilities().instabuild) main.shrink(1);
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (!hasHeart) {
            if (main.isEmpty() && !level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                    Component.translatable("message.magic_storage.no_heart"), true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
