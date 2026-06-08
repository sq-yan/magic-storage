package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class MSCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicStorage.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = REGISTRY.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.magic_storage"))
            .icon(() -> new ItemStack(MSItems.HEART_STORAGE.get()))
            .displayItems((params, output) -> {
                output.accept(MSItems.HEART_STORAGE.get());
                output.accept(MSItems.HEART_STORAGE_T2.get());
                output.accept(MSItems.STORAGE_CELL.get());
            })
            .build());

    private MSCreativeTabs() {}
}
