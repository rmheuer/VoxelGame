package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.ServerPacketListener;

public interface ServerPacket extends Packet {
    void handleServer(ServerPacketListener listener);
}
