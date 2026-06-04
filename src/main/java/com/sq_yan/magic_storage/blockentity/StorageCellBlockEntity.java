package com.sq_yan.magic_storage.blockentity;

import com.mojang.logging.LogUtils;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class StorageCellBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SIZE = 27;
    private static final String NBT_ITEMS = "Items";

    private final ItemStackHandler items = new ItemStackHandler(SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                BlockState st = getBlockState();
                level.sendBlockUpdated(getBlockPos(), st, st, 3);
            }
        }
    };

    private LazyOptional<IItemHandler> itemsCap = LazyOptional.of(() -> items);

    public StorageCellBlockEntity(BlockPos pos, BlockState state) {
        super(MSBlockEntities.STORAGE_CELL.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemsCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemsCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemsCap = LazyOptional.of(() -> items);
    }

    private int countNonEmpty() {
        int n = 0;
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) n++;
        }
        return n;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider provider) {
        return saveCustomOnly(provider);
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        var maybeTag = input.read(NBT_ITEMS, CompoundTag.CODEC);
        if (maybeTag.isPresent()) {
            items.deserializeNBT(input.lookup(), maybeTag.get());
            if (level == null || !level.isClientSide()) {
                LOGGER.info("[magic_storage] cell loaded @{}: {} non-empty slots", getBlockPos(), countNonEmpty());
            }
        } else if (level == null || !level.isClientSide()) {
            LOGGER.info("[magic_storage] cell loaded @{}: NO Items tag in NBT (fresh place or save was empty)", getBlockPos());
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        if (level != null) {
            output.store(NBT_ITEMS, CompoundTag.CODEC, items.serializeNBT(level.registryAccess()));
        } else {
            LOGGER.warn("[magic_storage] cell save SKIPPED @{}: level is null", getBlockPos());
        }
    }
}
