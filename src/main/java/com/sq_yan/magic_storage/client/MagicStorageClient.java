package com.sq_yan.magic_storage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sq_yan.magic_storage.MagicStorage;
import com.sq_yan.magic_storage.net.GlobalDumpPacket;
import com.sq_yan.magic_storage.net.MSNetwork;
import com.sq_yan.magic_storage.registry.MSMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MagicStorage.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MagicStorageClient {

    public static final KeyMapping QUICK_DUMP = new KeyMapping(
        "key.magic_storage.quick_dump",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_INSERT,
        KeyMapping.Category.INVENTORY
    );

    public static final KeyMapping TOGGLE_PROTECT = new KeyMapping(
        "key.magic_storage.toggle_protect",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        KeyMapping.Category.INVENTORY
    );

    static {
        RegisterKeyMappingsEvent.BUS.addListener(MagicStorageClient::onRegisterKeyMappings);
        TickEvent.ClientTickEvent.Post.BUS.addListener(MagicStorageClient::onClientTick);
        ProtectedScreenOverlay.register();
    }

    private MagicStorageClient() {}

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(QUICK_DUMP);
        event.register(TOGGLE_PROTECT);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (QUICK_DUMP.consumeClick()) {
            MSNetwork.CHANNEL.send(GlobalDumpPacket.INSTANCE, PacketDistributor.SERVER.noArg());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MSMenus.MAGIC_STORAGE.get(), MagicStorageScreen::new));
    }
}
