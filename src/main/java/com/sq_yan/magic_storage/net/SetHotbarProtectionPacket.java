package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.protect.ProtectedSlots;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

public record SetHotbarProtectionPacket(boolean enabled) {

    public static void encode(SetHotbarProtectionPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static SetHotbarProtectionPacket decode(FriendlyByteBuf buf) {
        return new SetHotbarProtectionPacket(buf.readBoolean());
    }

    public static void handle(SetHotbarProtectionPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        ProtectedSlots.setHotbarProtected(player, msg.enabled);
        MSNetwork.CHANNEL.send(
            new ProtectedSlotsSyncPacket(ProtectedSlots.toArray(ProtectedSlots.read(player)), msg.enabled),
            PacketDistributor.PLAYER.with(player)
        );
    }
}
