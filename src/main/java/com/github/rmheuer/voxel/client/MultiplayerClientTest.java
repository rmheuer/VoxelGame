package com.github.rmheuer.voxel.client;

import com.github.rmheuer.voxel.network.packet.ClientPlayerIdPacket;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;

import java.net.InetSocketAddress;

public final class MultiplayerClientTest {
    public static void main(String[] args) throws Exception {
        InetSocketAddress address = new InetSocketAddress("localhost", 25565);

        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            ChannelFuture f = ServerConnection.connectToServer(workerGroup, address);
            f.sync();

            ServerConnection conn = f.channel().pipeline().get(ServerConnection.class);
            conn.sendPacket(new ClientPlayerIdPacket(
                    (short) 7,
                    "TestPlayer",
                    ""
            ));

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
