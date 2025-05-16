package com.github.rmheuer.voxel.server;

import com.github.rmheuer.voxel.network.Connection;
import com.github.rmheuer.voxel.network.packet.ClientPacket;
import com.github.rmheuer.voxel.network.packet.ServerDisconnectPacket;
import com.github.rmheuer.voxel.network.packet.ServerPacket;
import io.netty.channel.Channel;

public final class ClientConnection extends Connection<ClientPacket, ServerPacket> {
    private final ClientHandler handler;

    public ClientConnection(Channel channel) {
        super(channel);
        handler = new ClientHandler(this);
    }

    @Override
    protected void dispatchPacket(ClientPacket packet) {
        packet.handleClient(handler);
    }

    public void kick(String reason) {
        System.out.println("Kicking for: " + reason);
        sendPacket(new ServerDisconnectPacket(reason));
        disconnect();
    }
}
