package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

public final class ServerPingPacket implements ServerPacket {
    public static final ServerPingPacket INSTANCE = new ServerPingPacket();

    // No data

    private ServerPingPacket() {}

    @Override
    public void read(PacketDataInput in) {}

    @Override
    public void write(PacketDataOutput out) {}

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onPing();
    }

    @Override
    public String toString() {
        return "ServerPingPacket{}";
    }
}
