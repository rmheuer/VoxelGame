package com.github.rmheuer.voxel.network.packet;

import com.github.rmheuer.voxel.network.BidiPacketListener;
import com.github.rmheuer.voxel.network.ClientPacketListener;
import com.github.rmheuer.voxel.network.ServerPacketListener;

public interface BidiPacket extends ClientPacket, ServerPacket {
    void handle(BidiPacketListener listener);

    @Override
    default void handleClient(ClientPacketListener listener) {
        handle(listener);
    }

    @Override
    default void handleServer(ServerPacketListener listener) {
        handle(listener);
    }
}
