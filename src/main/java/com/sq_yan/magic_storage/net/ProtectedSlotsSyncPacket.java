package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.client.ProtectedSlotsCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ProtectedSlotsSyncPacket(int[] slots, boolean hotbarProtected) {

    public static void encode(ProtectedSlotsSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slots.length);
        for (int s : msg.slots) buf.writeVarInt(s);
        buf.writeBoolean(msg.hotbarProtected);
    }

    public static ProtectedSlotsSyncPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = buf.readVarInt();
        boolean hotbar = buf.readBoolean();
        return new ProtectedSlotsSyncPacket(arr, hotbar);
    }

    public static void handle(ProtectedSlotsSyncPacket msg, CustomPayloadEvent.Context ctx) {
        ProtectedSlotsCache.replace(msg.slots, msg.hotbarProtected);
    }
}
