package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MagicStorage.MODID);

    public static final RegistryObject<BlockEntityType<HeartStorageBlockEntity>> HEART_STORAGE =
        REGISTRY.register("heart_storage", () ->
            BlockEntityType.Builder.of(HeartStorageBlockEntity::new, MSBlocks.HEART_STORAGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<StorageCellBlockEntity>> STORAGE_CELL =
        REGISTRY.register("storage_cell", () ->
            BlockEntityType.Builder.of(StorageCellBlockEntity::new, MSBlocks.STORAGE_CELL.get()).build(null));

    private MSBlockEntities() {}
}
