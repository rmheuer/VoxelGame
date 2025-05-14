package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public final class Connection<I extends Packet, O extends Packet> {
    private final Socket socket;
    private final PacketDataInput socketIn;
    private final PacketDataOutput socketOut;

    private final PacketMapping<I, O> mapping;

    public Connection(Socket socket, PacketMapping<I, O> mapping) throws IOException {
        this.socket = socket;
        this.mapping = mapping;

        socketIn = new PacketDataInput(new DataInputStream(socket.getInputStream()));
        socketOut = new PacketDataOutput(new DataOutputStream(socket.getOutputStream()));
    }

    public boolean packetAvailable() throws IOException {
        return socketIn.available() > 0;
    }

    public I readPacket() throws IOException {
        int packetId = socketIn.readUByte();
        I packet = mapping.createInPacket(packetId);
        if (packet == null)
            throw new IOException("Received unknown packet of id " + packetId);

        packet.read(socketIn);
        return packet;
    }

    public void writePacket(O outPacket) throws IOException {
        int packetId = mapping.getIdForOutPacket(outPacket.getClass());
        socketOut.writeUByte(packetId);
        outPacket.write(socketOut);
    }

    public boolean isConnected() {
        return !socket.isClosed() && socket.isConnected();
    }

    public void disconnect() throws IOException {
        socket.close();
    }
}
