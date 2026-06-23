package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.block.CrystalExpanderBlock;
import com.sq_yan.magic_storage.block.HeartStorageT1Block;
import com.sq_yan.magic_storage.block.StorageCellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MSBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, MagicStorage.MODID);

    public static final RegistryObject<HeartStorageT1Block> HEART_STORAGE = REGISTRY.register("heart_storage",
        () -> new HeartStorageT1Block(BlockBehaviour.Properties.of()
            .setId(REGISTRY.key("heart_storage"))
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(3.0f, 4.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<CrystalExpanderBlock> CRYSTAL_EXPANDER = REGISTRY.register("crystal_expander",
        () -> new CrystalExpanderBlock(BlockBehaviour.Properties.of()
            .setId(REGISTRY.key("crystal_expander"))
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(2.5f, 3.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
        )
    );

    public static final RegistryObject<StorageCellBlock> STORAGE_CELL = REGISTRY.register("storage_cell",
        () -> new StorageCellBlock(BlockBehaviour.Properties.of()
            .setId(REGISTRY.key("storage_cell"))
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .sound(SoundType.WOOD)
        )
    );

    private MSBlocks() {}
}
