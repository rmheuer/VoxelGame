package com.github.rmheuer.voxel.server;

import com.github.rmheuer.azalea.utils.SafeCloseable;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.local.LocalServerChannel;

public final class LocalConnectionHandler implements SafeCloseable {
    private final EventLoopGroup serverGroup;

    public LocalConnectionHandler(GameServer server, LocalAddress addr) throws Exception {
        serverGroup = new MultiThreadIoEventLoopGroup(LocalIoHandler.newFactory());

        boolean success = false;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(serverGroup);
            b.channel(LocalServerChannel.class);
            b.childHandler(new ChannelInitializer<LocalChannel>() {
                @Override
                protected void initChannel(LocalChannel ch) {
                    System.out.println("Local channel connected");

                    // No codec needed, packets can be transferred directly
                    ch.pipeline().addLast(new ClientConnection(server, ch));
                }
            });

            b.bind(addr).sync();
            success = true;
        } finally {
            if (!success) {
                serverGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void close() {
        serverGroup.shutdownGracefully();
    }
}
