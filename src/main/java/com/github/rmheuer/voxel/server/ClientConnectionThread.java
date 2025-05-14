package com.github.rmheuer.voxel.server;

import com.github.rmheuer.voxel.network.Connection;
import com.github.rmheuer.voxel.network.PacketRegistry;
import com.github.rmheuer.voxel.network.packet.ClientPacket;
import com.github.rmheuer.voxel.network.packet.ServerPacket;

import java.io.IOException;
import java.net.Socket;

public final class ClientConnectionThread extends Thread {
    private static int count = 0;

    private final Socket socket;

    public ClientConnectionThread(Socket socket) {
        super("Client Handler Thread #" + ++count);
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            Connection<ClientPacket, ServerPacket> conn = new Connection<>(socket, PacketRegistry.getServerMapping());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
