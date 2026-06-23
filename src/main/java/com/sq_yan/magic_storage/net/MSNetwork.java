package com.sq_yan.magic_storage.net;

import com.sq_yan.magic_storage.MagicStorage;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public final class MSNetwork {
    public static final int PROTOCOL_VERSION = 1;
    public static SimpleChannel CHANNEL;

    private MSNetwork() {}

    public static void init() {
        CHANNEL = ChannelBuilder.named(Identifier.parse(MagicStorage.MODID + ":main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

        CHANNEL.messageBuilder(GlobalDumpPacket.class, 0)
            .direction(PacketFlow.SERVERBOUND)
            .encoder(GlobalDumpPacket::encode)
            .decoder(GlobalDumpPacket::decode)
            .consumerMainThread(GlobalDumpPacket::handle)
            .add();

        CHANNEL.messageBuilder(UpdateProtectedSlotsPacket.class, 1)
            .direction(PacketFlow.SERVERBOUND)
            .encoder(UpdateProtectedSlotsPacket::encode)
            .decoder(UpdateProtectedSlotsPacket::decode)
            .consumerMainThread(UpdateProtectedSlotsPacket::handle)
            .add();

        CHANNEL.messageBuilder(ProtectedSlotsSyncPacket.class, 2)
            .direction(PacketFlow.CLIENTBOUND)
            .encoder(ProtectedSlotsSyncPacket::encode)
            .decoder(ProtectedSlotsSyncPacket::decode)
            .consumerMainThread(ProtectedSlotsSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(SetHotbarProtectionPacket.class, 3)
            .direction(PacketFlow.SERVERBOUND)
            .encoder(SetHotbarProtectionPacket::encode)
            .decoder(SetHotbarProtectionPacket::decode)
            .consumerMainThread(SetHotbarProtectionPacket::handle)
            .add();

        CHANNEL.build();
    }
}
