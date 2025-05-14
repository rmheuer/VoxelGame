package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.PacketDataInput;
import com.github.rmheuer.voxel.network.PacketDataOutput;
import com.github.rmheuer.voxel.network.ServerPacketListener;

public final class ServerLevelInitPacket implements ServerPacket {
    public static final ServerLevelInitPacket INSTANCE = new ServerLevelInitPacket();

    // No data

    private ServerLevelInitPacket() {}

    @Override
    public void read(PacketDataInput in) {}

    @Override
    public void write(PacketDataOutput out) {}

    @Override
    public void handleServer(ServerPacketListener listener) {
        listener.onLevelInit();
    }

    @Override
    public String toString() {
        return "ServerLevelInitPacket{}";
    }
}
