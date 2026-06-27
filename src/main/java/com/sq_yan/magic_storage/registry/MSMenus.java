package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.menu.MagicStorageMenu;
import com.sq_yan.magic_storage.menu.ResonanceConsoleMenu;
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

    public static final RegistryObject<MenuType<ResonanceConsoleMenu>> RESONANCE_CONSOLE =
        REGISTRY.register("resonance_console",
            () -> IForgeMenuType.create(ResonanceConsoleMenu::new));

    private MSMenus() {}
}
