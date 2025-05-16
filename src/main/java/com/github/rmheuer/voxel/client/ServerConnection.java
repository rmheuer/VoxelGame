package com.github.rmheuer.voxel.client;

import com.github.rmheuer.voxel.network.*;
import com.github.rmheuer.voxel.network.packet.BidiPlayerPositionPacket;
import com.github.rmheuer.voxel.network.packet.ClientPacket;
import com.github.rmheuer.voxel.network.packet.ServerPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public final class ServerConnection extends Connection<ServerPacket, ClientPacket> {
    private static final int TIMEOUT_MS = 10000;

    public static ChannelFuture connectToServer(EventLoopGroup eventLoop, InetSocketAddress address) {
        PacketMapping<ServerPacket, ClientPacket> mapping = PacketRegistry.getClientMapping();

        Bootstrap b = new Bootstrap();
        b.group(eventLoop);
        b.channel(NioSocketChannel.class);
        b.remoteAddress(address);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_MS);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new PacketEncoder<>(ClientPacket.class, mapping));
                p.addLast(new PacketDecoder<>(mapping));
                p.addLast(new ServerConnection(ch));
            }
        });

        return b.connect();
    }

    private ServerPacketListener listener;

    public ServerConnection(Channel channel) {
        super(channel);
    }

    @Override
    protected void dispatchPacket(ServerPacket packet) {
        packet.handleServer(listener);
    }

    public void setPacketListener(ServerPacketListener listener) {
        this.listener = listener;
    }
}
