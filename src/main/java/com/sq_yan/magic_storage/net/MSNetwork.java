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

        CHANNEL.build();
    }
}
