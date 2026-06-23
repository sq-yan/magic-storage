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

    public static final RegistryObject<Item> CRYSTAL_EXPANDER = REGISTRY.register("crystal_expander",
        () -> new BlockItem(MSBlocks.CRYSTAL_EXPANDER.get(),
            new Item.Properties().setId(REGISTRY.key("crystal_expander")))
    );

    public static final RegistryObject<Item> STORAGE_CELL = REGISTRY.register("storage_cell",
        () -> new BlockItem(MSBlocks.STORAGE_CELL.get(),
            new Item.Properties().setId(REGISTRY.key("storage_cell")))
    );

    public static final RegistryObject<Item> MAGIC_HEART = REGISTRY.register("magic_heart",
        () -> new Item(new Item.Properties().setId(REGISTRY.key("magic_heart")).stacksTo(16))
    );

    public static final RegistryObject<Item> MAGIC_CRYSTAL = REGISTRY.register("magic_crystal",
        () -> new Item(new Item.Properties().setId(REGISTRY.key("magic_crystal")).stacksTo(16))
    );

    public static final RegistryObject<Item> REIGALLS_TUNING_FORK = REGISTRY.register("reigalls_tuning_fork",
        () -> new Item(new Item.Properties().setId(REGISTRY.key("reigalls_tuning_fork")).stacksTo(1))
    );

    private MSItems() {}
}
