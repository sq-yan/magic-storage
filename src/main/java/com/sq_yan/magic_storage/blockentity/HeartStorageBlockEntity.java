package com.sq_yan.magic_storage.blockentity;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeartStorageBlockEntity extends BlockEntity implements MenuProvider {
    public HeartStorageBlockEntity(BlockPos pos, BlockState state) {
        super(MSBlockEntities.HEART_STORAGE.get(), pos, state);
    }

    public AggregatedItemHandler buildAggregated() {
        return new AggregatedItemHandler(CellNetwork.collect(this));
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
