package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public abstract class Connection<I extends Packet, O extends Packet> extends ChannelInboundHandlerAdapter {
    private final Channel channel;

    public Connection(Channel channel) {
        this.channel = channel;
    }

    protected abstract void dispatchPacket(I packet);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        @SuppressWarnings("unchecked")
        I packet = (I) msg;

        System.out.println("Received packet: " + packet);
        dispatchPacket(packet);
    }

    public void sendPacket(O packet) {
//        System.out.println("Sent packet: " + packet);
        channel.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public boolean isConnected() {
        return channel.isOpen() && channel.isActive();
    }

    public void close() {
        if (channel.isOpen())
            channel.close();
    }
}
