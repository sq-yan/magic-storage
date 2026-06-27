package com.sq_yan.magic_storage.protect;

import com.sq_yan.magic_storage.net.MSNetwork;
import com.sq_yan.magic_storage.net.ProtectedSlotsSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

public final class ProtectedSlotsServerSync {
    private ProtectedSlotsServerSync() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ProtectedSlotsServerSync::onLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(ProtectedSlotsServerSync::onRespawn);
        MinecraftForge.EVENT_BUS.addListener(ProtectedSlotsServerSync::onChangedDimension);
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent e) { sync(e.getEntity()); }
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { sync(e.getEntity()); }
    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { sync(e.getEntity()); }

    private static void sync(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        var slots = ProtectedSlots.cleanEmpty(sp);
        MSNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> sp),
            new ProtectedSlotsSyncPacket(ProtectedSlots.toArray(slots), ProtectedSlots.isHotbarProtected(sp))
        );
    }
}
