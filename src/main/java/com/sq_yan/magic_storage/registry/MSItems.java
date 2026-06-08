package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MSItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MagicStorage.MODID);

    public static final RegistryObject<Item> HEART_STORAGE = REGISTRY.register("heart_storage",
        () -> new BlockItem(MSBlocks.HEART_STORAGE.get(),
            new Item.Properties().setId(REGISTRY.key("heart_storage")))
    );

    public static final RegistryObject<Item> HEART_STORAGE_T2 = REGISTRY.register("heart_storage_t2",
        () -> new BlockItem(MSBlocks.HEART_STORAGE_T2.get(),
            new Item.Properties().setId(REGISTRY.key("heart_storage_t2")))
    );

    public static final RegistryObject<Item> STORAGE_CELL = REGISTRY.register("storage_cell",
        () -> new BlockItem(MSBlocks.STORAGE_CELL.get(),
            new Item.Properties().setId(REGISTRY.key("storage_cell")))
    );

    private MSItems() {}
}
