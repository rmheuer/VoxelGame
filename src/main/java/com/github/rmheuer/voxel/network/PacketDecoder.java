package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.util.List;

public final class PacketDecoder<I extends Packet> extends ReplayingDecoder<Void> {
    private final PacketMapping<I, ?> mapping;
    private I packet;

    public PacketDecoder(PacketMapping<I, ?> mapping) {
        this.mapping = mapping;
        packet = null;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        if (packet == null) {
            int packetId = buf.readUnsignedByte();
            packet = mapping.createInPacket(packetId);
            if (packet == null)
                throw new IOException("Received invalid packet with id " + packetId);
            checkpoint();
        }

        if (packet != null) {
            PacketDataInput in = new PacketDataBuf(buf);
            packet.read(in);
            checkpoint();

            out.add(packet);
            packet = null;
        }
    }
}
