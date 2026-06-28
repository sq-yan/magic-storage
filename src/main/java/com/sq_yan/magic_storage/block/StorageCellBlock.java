package com.sq_yan.magic_storage.block;

import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StorageCellBlock extends BaseEntityBlock {

    /** Visual fill indicator: 0 = green (≤50% / empty), 1 = yellow (≤67%), 2 = red (>67%). */
    public static final IntegerProperty FILL_LEVEL = IntegerProperty.create("fill_level", 0, 2);

    /** Whether this cell is an active member of a heart's network (false → rendered grey / "not working"). */
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public StorageCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FILL_LEVEL, 0).setValue(CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL_LEVEL, CONNECTED);
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
    public void playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos,
                                  @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageCellBlockEntity cell) {
            cell.dropContents(level, pos);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
