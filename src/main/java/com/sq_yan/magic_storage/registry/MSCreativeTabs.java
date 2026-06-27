package com.sq_yan.magic_storage.registry;

import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.book.LoreBooks;
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
                output.accept(MSItems.CRYSTAL_EXPANDER.get());
                output.accept(MSItems.STORAGE_CELL.get());
                output.accept(MSItems.MAGIC_HEART.get());
                output.accept(MSItems.MAGIC_CRYSTAL.get());
                output.accept(MSItems.REIGALLS_TUNING_FORK.get());
                output.accept(LoreBooks.book1());
                output.accept(LoreBooks.book2());
                output.accept(LoreBooks.book3());
            })
            .build());

    private MSCreativeTabs() {}
}
