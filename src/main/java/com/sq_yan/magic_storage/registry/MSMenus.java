package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MSMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, MagicStorage.MODID);

    public static final RegistryObject<MenuType<MagicStorageMenu>> MAGIC_STORAGE =
        REGISTRY.register("magic_storage",
            () -> IForgeMenuType.create(MagicStorageMenu::new));

    private MSMenus() {}
}
