package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.utils.SafeCloseable;
import com.github.rmheuer.voxel.network.PacketDecoder;
import com.github.rmheuer.voxel.network.PacketEncoder;
import com.github.rmheuer.voxel.network.PacketMapping;
import com.github.rmheuer.voxel.network.PacketRegistry;
import com.github.rmheuer.voxel.network.packet.ClientPacket;
import com.github.rmheuer.voxel.network.packet.ServerPacket;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public final class NetworkConnectionHandler implements SafeCloseable {
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    public NetworkConnectionHandler(GameServer server, int port) throws Exception {
        bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        boolean success = false;
        try {
            PacketMapping<ClientPacket, ServerPacket> mapping = PacketRegistry.getServerMapping();

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    System.out.println("Client has connected");

                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new PacketEncoder<>(ServerPacket.class, mapping));
                    p.addLast(new PacketDecoder<>(mapping));
                    p.addLast(new ClientConnection(server, ch));
                }
            });
            b.option(ChannelOption.SO_BACKLOG, 16);
            b.childOption(ChannelOption.SO_KEEPALIVE, true);
            b.childOption(ChannelOption.TCP_NODELAY, true);

            b.bind(port).sync();

            success = true;
            System.out.println("Server open on port " + port);
        } finally {
            if (!success) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
