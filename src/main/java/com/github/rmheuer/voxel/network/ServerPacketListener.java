package com.github.rmheuer.voxel.network;

import com.github.rmheuer.voxel.network.packet.*;

public interface ServerPacketListener {
    void onServerId(ServerIdPacket packet);

    void onPing();

    void onLevelInit();

    void onLevelDataChunk(ServerLevelDataChunkPacket packet);

    void onLevelFinalize(ServerLevelFinalizePacket packet);

    void onSetBlock(ServerSetBlockPacket packet);

    void onSpawnPlayer(ServerSpawnPlayerPacket packet);

    void onPlayerPosition(BidiPlayerPositionPacket packet);

    void onRelativeMoveAndLook(ServerRelativeMoveAndLookPacket packet);

    void onRelativeMove(ServerRelativeMovePacket packet);

    void onLook(ServerLookPacket packet);

    void onDespawnPlayer(ServerDespawnPlayerPacket packet);

    void onChatMessage(BidiChatMessagePacket packet);

    void onDisconnect(ServerDisconnectPacket packet);

    void onUpdateOp(ServerUpdateOpPacket packet);
}
