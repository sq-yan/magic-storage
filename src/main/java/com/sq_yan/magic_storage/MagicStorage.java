package com.sq_yan.magic_storage;

import com.mojang.logging.LogUtils;
import com.sq_yan.magic_storage.book.LoreBookEvents;
import com.sq_yan.magic_storage.net.MSNetwork;
import com.sq_yan.magic_storage.registry.MSBlockEntities;
import com.sq_yan.magic_storage.registry.MSBlocks;
import com.sq_yan.magic_storage.registry.MSCreativeTabs;
import com.sq_yan.magic_storage.registry.MSItems;
import com.sq_yan.magic_storage.registry.MSMenus;
import com.sq_yan.magic_storage.protect.ProtectedSlotsServerSync;
import com.sq_yan.magic_storage.trade.VillagerTradeRegistry;
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
        MSCreativeTabs.REGISTRY.register(bus);

        MSNetwork.init();
        VillagerTradeRegistry.register();
        ProtectedSlotsServerSync.register();
        LoreBookEvents.register();

        LOGGER.info("[magic_storage] mod constructed");
    }
}
