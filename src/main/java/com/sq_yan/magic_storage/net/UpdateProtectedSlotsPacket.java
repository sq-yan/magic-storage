package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.protect.ProtectedSlots;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.LinkedHashSet;
import java.util.Set;

public record UpdateProtectedSlotsPacket(int[] slots) {

    public static void encode(UpdateProtectedSlotsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slots.length);
        for (int s : msg.slots) buf.writeVarInt(s);
    }

    public static UpdateProtectedSlotsPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = buf.readVarInt();
        return new UpdateProtectedSlotsPacket(arr);
    }

    public static void handle(UpdateProtectedSlotsPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        Set<Integer> set = new LinkedHashSet<>();
        for (int s : msg.slots) {
            if (s >= 0 && s < ProtectedSlots.PLAYER_INV_SLOTS) set.add(s);
        }
        ProtectedSlots.write(player, set);
        MSNetwork.CHANNEL.send(
            new ProtectedSlotsSyncPacket(ProtectedSlots.toArray(set), ProtectedSlots.isHotbarProtected(player)),
            net.minecraftforge.network.PacketDistributor.PLAYER.with(player)
        );
    }
}
