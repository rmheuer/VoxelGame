package com.github.rmheuer.voxel.server;

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

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class GameServer {
    private final Queue<String> consoleInputQueue;
    private final ConsoleInputThread consoleThread;

    private boolean running;

    public GameServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, int port) throws Exception {
        consoleInputQueue = new ConcurrentLinkedQueue<>();
        consoleThread = new ConsoleInputThread(consoleInputQueue);

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
                p.addLast(new ClientConnection(ch));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, 16);
        b.childOption(ChannelOption.SO_KEEPALIVE, true);
        b.childOption(ChannelOption.TCP_NODELAY, true);

        ChannelFuture f = b.bind(port).sync();
        System.out.println("Server open on port " + port);
    }

    private void handleConsoleCommand(String command) {
        if (command.equals("stop")) {
            running = false;
        }
    }

    private void run() {
        consoleThread.start();

        running = true;
        while (running) {
            String consoleCommand;
            while ((consoleCommand = consoleInputQueue.poll()) != null) {
                handleConsoleCommand(consoleCommand);
            }

            // Nothing to do...
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {}
        }

        System.out.println("Server shutting down");
        consoleThread.close();
    }

    public static void main(String[] args) throws Exception {
        int port = 25565;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);

        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            new GameServer(bossGroup, workerGroup, port).run();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
