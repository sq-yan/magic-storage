package com.sq_yan.magic_storage.blockentity;

import com.sq_yan.magic_storage.block.HeartStorageBlock;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.net.AggregatedItemHandler;
import com.sq_yan.magic_storage.net.CellNetwork;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HeartStorageBlockEntity extends BlockEntity implements MenuProvider {
    private static final int TICK_INTERVAL = 20;

    private int tickCounter = 0;

    public HeartStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MSBlockEntities.HEART_STORAGE.get(), pos, state);
    }

    public AggregatedItemHandler buildAggregated() {
        return new AggregatedItemHandler(CellNetwork.collect(this));
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        List<StorageCellBlockEntity> cells = CellNetwork.collect(this);
        int total = cells.size() * StorageCellBlockEntity.SIZE;
        int used = 0;
        for (StorageCellBlockEntity cell : cells) {
            ItemStackHandler items = cell.getItems();
            for (int i = 0; i < items.getSlots(); i++) {
                if (!items.getStackInSlot(i).isEmpty()) used++;
            }
        }
        int newLevel = computeFillLevel(used, total);
        BlockState state = getBlockState();
        if (state.hasProperty(HeartStorageBlock.FILL_LEVEL)
            && state.getValue(HeartStorageBlock.FILL_LEVEL) != newLevel) {
            level.setBlock(getBlockPos(), state.setValue(HeartStorageBlock.FILL_LEVEL, newLevel), 3);
        }
    }

    private static int computeFillLevel(int used, int total) {
        if (total == 0) return 0;
        double ratio = (double) used / total;
        if (ratio <= 0.5) return 1;
        if (ratio <= 2.0 / 3.0) return 2;
        return 3;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.magic_storage.heart_storage");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new MagicStorageMenu(id, inv, this);
    }
}
