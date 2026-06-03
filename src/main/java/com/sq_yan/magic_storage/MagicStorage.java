package com.sq_yan.magic_storage;

import com.mojang.logging.LogUtils;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import com.sq_yan.magic_storage.registry.MSBlocks;
import com.sq_yan.magic_storage.registry.MSItems;
import com.sq_yan.magic_storage.registry.MSMenus;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MagicStorage.MODID)
public final class MagicStorage {
    public static final String MODID = "magic_storage";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MagicStorage(FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();

        MSBlocks.REGISTRY.register(bus);
        MSItems.REGISTRY.register(bus);
        MSBlockEntities.REGISTRY.register(bus);
        MSMenus.REGISTRY.register(bus);

        BuildCreativeModeTabContentsEvent.BUS.addListener(MagicStorage::addCreative);

        LOGGER.info("[magic_storage] mod constructed");
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(MSItems.HEART_STORAGE.get());
            event.accept(MSItems.STORAGE_CELL.get());
        }
    }
}
