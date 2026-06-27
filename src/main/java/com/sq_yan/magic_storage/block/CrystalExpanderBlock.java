package com.sq_yan.magic_storage.block;

import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.registry.MSItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public class CrystalExpanderBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_CRYSTAL = BooleanProperty.create("has_crystal");

    private static final double HEART_NOTIFY_RADIUS = 32.0;

    public CrystalExpanderBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HAS_CRYSTAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_CRYSTAL);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(HAS_CRYSTAL, false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        ItemStack main = player.getItemInHand(hand);
        boolean hasCrystal = state.getValue(HAS_CRYSTAL);

        // Insert: empty slot + crystal in hand
        if (!hasCrystal && main.is(MSItems.MAGIC_CRYSTAL.get())) {
            if (!hasFaceAdjacentHeart(level, pos)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                        Component.translatable("message.magic_storage.expander_not_adjacent"), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_CRYSTAL, true), 3);
                if (!player.getAbilities().instabuild) main.shrink(1);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
                notifyNearbyHeart(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        // Extract: crystal in slot + empty hand
        if (hasCrystal && main.isEmpty()) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAS_CRYSTAL, false), 3);
                ItemStack crystal = new ItemStack(MSItems.MAGIC_CRYSTAL.get());
                if (!player.getInventory().add(crystal)) {
                    player.drop(crystal, false);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
                notifyNearbyHeart(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        // Empty slot + empty hand → hint
        if (!hasCrystal && main.isEmpty()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                    Component.translatable("message.magic_storage.no_crystal"), true);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                         @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            notifyNearbyHeart(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void notifyNearbyHeart(Level level, BlockPos pos) {
        HeartStorageBlockEntity heart = HeartStorageBlockEntity.findNearby(level, pos, HEART_NOTIFY_RADIUS);
        if (heart != null) heart.refreshConnections();
    }

    private static boolean hasFaceAdjacentHeart(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(pos.relative(d)) instanceof HeartStorageBlockEntity) {
                return true;
            }
        }
        return false;
    }
}
