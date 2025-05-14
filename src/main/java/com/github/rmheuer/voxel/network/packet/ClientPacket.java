package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.ClientPacketListener;

public interface ClientPacket extends Packet {
    void handleClient(ClientPacketListener listener);
}
