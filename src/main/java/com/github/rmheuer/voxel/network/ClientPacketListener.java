package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.BidiChatMessagePacket;
import com.github.rmheuer.voxel.network.packet.BidiPlayerPositionPacket;
import com.github.rmheuer.voxel.network.packet.ClientPlayerIdPacket;
import com.github.rmheuer.voxel.network.packet.ClientSetBlockPacket;

public interface ClientPacketListener {
    void onPlayerId(ClientPlayerIdPacket packet);

    void onSetBlock(ClientSetBlockPacket packet);

    void onPlayerPosition(BidiPlayerPositionPacket packet);

    void onChatMessage(BidiChatMessagePacket packet);
}
