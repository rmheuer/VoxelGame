package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.BidiChatMessagePacket;
import com.github.rmheuer.voxel.network.packet.BidiPlayerPositionPacket;
import com.github.rmheuer.voxel.network.cpe.packet.BidiExtEntryPacket;
import com.github.rmheuer.voxel.network.cpe.packet.BidiExtInfoPacket;

public interface BidiPacketListener {
    void onExtInfo(BidiExtInfoPacket packet);

    void onExtEntry(BidiExtEntryPacket packet);

    void onPlayerPosition(BidiPlayerPositionPacket packet);

    void onChatMessage(BidiChatMessagePacket packet);
}
