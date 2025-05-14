package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class PacketEncoder<O extends Packet> extends MessageToByteEncoder<O> {
    private final PacketMapping<?, O> mapping;

    public PacketEncoder(Class<O> oClass, PacketMapping<?, O> mapping) {
        super(oClass);
        this.mapping = mapping;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, O packet, ByteBuf outBuf) throws Exception {
        int packetId = mapping.getIdForOutPacket(packet.getClass());

        PacketDataOutput out = new PacketDataBuf(outBuf);
        out.writeUByte(packetId);
        packet.write(out);
    }
}
