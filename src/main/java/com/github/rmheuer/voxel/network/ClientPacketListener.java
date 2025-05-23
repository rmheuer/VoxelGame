package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.ClientPlayerIdPacket;
import com.github.rmheuer.voxel.network.packet.ClientSetBlockPacket;

public interface ClientPacketListener extends BidiPacketListener {
    void onPlayerId(ClientPlayerIdPacket packet);

    void onSetBlock(ClientSetBlockPacket packet);
}
