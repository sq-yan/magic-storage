package com.sq_yan.magic_storage.client;

import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.registry.MSMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MagicStorage.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MagicStorageClient {
    private MagicStorageClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MSMenus.MAGIC_STORAGE.get(), MagicStorageScreen::new));
    }
}
