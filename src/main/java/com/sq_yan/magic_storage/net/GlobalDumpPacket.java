package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record GlobalDumpPacket() {
    public static final GlobalDumpPacket INSTANCE = new GlobalDumpPacket();
    public static final double SEARCH_RADIUS = 5.0;

    public static void encode(GlobalDumpPacket msg, FriendlyByteBuf buf) {
    }

    public static GlobalDumpPacket decode(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public static void handle(GlobalDumpPacket msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        HeartStorageBlockEntity heart = HeartStorageBlockEntity.findNearby(level, origin, SEARCH_RADIUS);
        if (heart == null) return;
        heart.dumpPlayerInventory(player);
    }
}
