package com.sq_yan.magic_storage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.registry.MSMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MagicStorage.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MagicStorageClient {

    public static final KeyMapping QUICK_DUMP = new KeyMapping(
        "key.magic_storage.quick_dump",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        KeyMapping.Category.INVENTORY
    );

    static {
        RegisterKeyMappingsEvent.BUS.addListener(MagicStorageClient::onRegisterKeyMappings);
    }

    private MagicStorageClient() {}

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(QUICK_DUMP);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MSMenus.MAGIC_STORAGE.get(), MagicStorageScreen::new));
    }
}
